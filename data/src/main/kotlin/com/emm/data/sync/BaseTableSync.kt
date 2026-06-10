package com.emm.data.sync

import com.emm.data.shared.safeDbCall
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.Resolution
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Shared push/pull algorithm for a single Supabase table.
 *
 * Subclasses supply only the table-specific wiring:
 *   - [transact]            — wraps a lambda in a DB transaction (`{ body -> db.transaction { body() } }`)
 *   - [selectPendingDtos]   — reads locally-pending rows owned by [userId] and maps them to [DTO]
 *   - [pkOf]                — extracts the primary key String from a [DTO]
 *   - [markSynced]          — marks a single row Synced after a successful push (called inside tx)
 *   - [fetchRemoteRows]     — performs the reified decodeList Postgrest call for this table
 *   - [localUpdatedAt]      — reads the local updatedAt for a PK (for LWW conflict resolution)
 *   - [applyRemoteRow]      — two-statement INSERT OR IGNORE + UPDATE; returns false on FK miss
 *   - [markPendingForResync]— re-flags a local row Pending so a newer local revision re-pushes
 *
 * The algorithm (ordering, user-id filter, cursor math, transaction boundaries, skippedRows
 * semantics, per-row [SQLiteConstraintException] catch, [Resolution.KeepLocal] resync) lives here
 * and is shared verbatim across all four tables.
 */
abstract class BaseTableSync<DTO : SyncRowDto>(
    protected val client: SupabaseClient,
    /**
     * Wraps a lambda in a single DB transaction.
     * Pass `{ body -> db.transaction { body() } }`. Keeps the base class free of db coupling.
     */
    private val transact: (() -> Unit) -> Unit,
) : TableSync {

    // ---------------------------------------------------------------------------
    // Push hooks
    // ---------------------------------------------------------------------------

    /**
     * Read locally-pending rows, filter to [userId], and convert them to push-ready DTOs.
     * selectPending already filters userId IS NOT NULL, but we keep the userId filter as
     * defence-in-depth: a row claimed by a different account must never leak to remote.
     */
    protected abstract suspend fun selectPendingDtos(userId: String): List<DTO>

    /** Primary key of [dto], used to call [markSynced] after a successful upsert. */
    protected abstract fun pkOf(dto: DTO): String

    /** Mark a single row Synced in the local DB after it has been pushed. Called inside tx. */
    protected abstract fun markSynced(pk: String, updatedAt: Long)

    /**
     * Upserts [dtos] to the remote table via Postgrest.
     * Implemented in each subclass as an inline/reified call so the kotlinx.serialization
     * encoder can see the concrete DTO type at compile time.
     */
    protected abstract suspend fun upsertDtos(dtos: List<DTO>)

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    /**
     * Fetch remote rows from Postgrest for this table.
     *
     * Filters by [userId]; when [overlapCursor] is non-null, also filters
     * `server_updated_at >= overlapCursor`. Implemented as `inline` in each subclass
     * because [decodeList] requires a reified type parameter.
     */
    protected abstract suspend fun fetchRemoteRows(userId: String, overlapCursor: String?): List<DTO>

    /** Read the local `updatedAt` Long for [pk], or null when the row is unknown locally. */
    protected abstract fun localUpdatedAt(pk: String): Long?

    /**
     * Two-statement upsert: INSERT OR IGNORE + UPDATE. Neither statement triggers an implicit
     * DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL) are never fired.
     * Returns false when a [SQLiteConstraintException] fires (orphan FK — retry next cycle).
     */
    protected abstract fun applyRemoteRow(remote: DTO): Boolean

    /** Re-flag the row with [pk] as Pending so its newer local revision re-pushes next cycle. */
    protected abstract fun markPendingForResync(pk: String)

    // ---------------------------------------------------------------------------
    // Push algorithm
    // ---------------------------------------------------------------------------

    // Intentional broad catch: funnelled to DomainException by DefaultSyncRepository.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun push(userId: String): Unit = withContext(Dispatchers.IO) {
        val dtos = selectPendingDtos(userId)
        if (dtos.isEmpty()) return@withContext

        upsertDtos(dtos)

        safeDbCall {
            transact {
                dtos.forEach { dto -> markSynced(pkOf(dto), dto.updatedAt) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Pull algorithm
    // ---------------------------------------------------------------------------

    @Suppress("TooGenericExceptionCaught")
    override suspend fun pull(userId: String, cursor: String?, resolver: ConflictResolver): PullResult =
        withContext(Dispatchers.IO) {
            val overlapCursor = overlapCursor(cursor)

            val remoteRows = fetchRemoteRows(userId, overlapCursor)

            if (remoteRows.isEmpty()) return@withContext PullResult(maxServerUpdatedAt = null, skippedRows = false)

            var maxInstant: Instant? = null
            var skipped = false

            safeDbCall {
                transact {
                    remoteRows.forEach { remote ->
                        // Defence-in-depth against RLS misconfig: never apply another user's row.
                        if (remote.userId != userId) {
                            skipped = true
                            return@forEach
                        }

                        val localTs = localUpdatedAt(pkOf(remote))

                        val resolution = resolver.resolve(localTs, remote.updatedAt)
                        when (resolution) {
                            Resolution.ApplyRemote ->
                                if (!applyRemoteRow(remote)) {
                                    skipped = true
                                    return@forEach
                                }

                            // Local row is newer than what the server holds: re-flag it Pending so the
                            // newer local revision re-pushes next cycle. Without this, a stale remote
                            // upsert from another device would leave the two replicas diverged forever.
                            Resolution.KeepLocal ->
                                markPendingForResync(pkOf(remote))
                        }

                        remote.serverUpdatedAt?.let { sat ->
                            val inst = parseServerInstant(sat)
                            if (maxInstant == null || inst.isAfter(maxInstant)) maxInstant = inst
                        }
                    }
                }
            }

            PullResult(maxServerUpdatedAt = maxInstant?.toString(), skippedRows = skipped)
        }
}
