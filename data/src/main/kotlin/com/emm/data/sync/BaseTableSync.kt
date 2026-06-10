package com.emm.data.sync

import com.emm.data.shared.safeDbCall
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.Resolution
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/** Composite keyset position used to paginate through a remote table. */
data class PullPageKey(val serverUpdatedAt: String, val pk: String)

/**
 * Shared push/pull algorithm for a single Supabase table.
 *
 * Subclasses supply only the table-specific wiring:
 *   - [transact]              — wraps a lambda in a DB transaction (`{ body -> db.transaction { body() } }`)
 *   - [pkColumn]              — PostgREST column name for the primary key (e.g. "account_id")
 *   - [selectPendingDtos]     — reads locally-pending rows owned by [userId] and maps them to [DTO]
 *   - [pkOf]                  — extracts the primary key String from a [DTO]
 *   - [markSynced]            — marks a single row Synced after a successful push (called inside tx)
 *   - [fetchRemotePage]       — performs the reified decodeList Postgrest call for this table
 *   - [localUpdatedAt]        — reads the local updatedAt for a PK (for LWW conflict resolution)
 *   - [applyRemoteRow]        — two-statement INSERT OR IGNORE + UPDATE; returns false on FK miss
 *   - [markPendingForResync]  — re-flags a local row Pending so a newer local revision re-pushes
 *
 * The algorithm (ordering, user-id filter, cursor math, transaction boundaries, skippedRows
 * semantics, per-row [SQLiteConstraintException] catch, [Resolution.KeepLocal] resync) lives here
 * and is shared verbatim across all four tables.
 *
 * ## Pull pagination
 *
 * Composite keyset `(server_updated_at, pk)` is used to paginate through remote rows.
 *
 * **Why not timestamp-only pagination?** A batch upsert from another device shares ONE
 * `server_updated_at` value (Postgres `now()` is transaction-stable). Paginating with `gt` at a
 * page boundary inside a same-timestamp batch drops rows. Paginating with `gte` when the batch is
 * larger than the page size loops forever on the same page.
 *
 * **Why not offset/range pagination?** Under concurrent remote writes a row updated mid-pagination
 * moves to the end of the result set, shifting later rows left — a row that was about to be fetched
 * is now at a position the cursor has already passed, so it is lost.
 *
 * **Composite keyset is stable**: concurrently-updated rows jump AHEAD of the keyset position and
 * are still seen in the same pass. The tiebreak on `pk` (a UUID) guarantees a deterministic order
 * within rows that share the same `server_updated_at`.
 *
 * Per-page DB transactions are safe because the pull cursor is only persisted by
 * [DefaultSyncRepository] after the whole multi-page pull completes. A process death mid-pagination
 * resets to the old cursor, triggers an overlap re-pull, and idempotent LWW upserts make the
 * re-applied rows a no-op.
 *
 * [MAX_PULL_PAGES] acts as a defensive guard: if a broken keyset causes an infinite loop the pull
 * terminates with `skippedRows = true`, which holds the cursor so the next sync cycle retries from
 * the same position.
 */
abstract class BaseTableSync<DTO : SyncRowDto>(
    protected val client: SupabaseClient,
    /**
     * Wraps a lambda in a single DB transaction.
     * Pass `{ body -> db.transaction { body() } }`. Keeps the base class free of db coupling.
     */
    private val transact: (() -> Unit) -> Unit,
    /** Number of rows to request per page. Override in tests to use a smaller value. */
    private val pageSize: Int = DEFAULT_PULL_PAGE_SIZE,
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
     * PostgREST column name for the primary key of this table (e.g. "account_id",
     * "category_id", "transaction_id", "id"). Used as the tiebreak column in composite
     * keyset pagination.
     */
    protected abstract val pkColumn: String

    /**
     * Fetch one page of remote rows from PostgREST.
     *
     * Filters applied (in the shared base via [applyPageFilter]):
     *   - Always: `user_id = userId` (defence-in-depth over RLS).
     *   - Page 1 with cursor: `server_updated_at >= overlapCursor`.
     *   - Page 2+: composite keyset `(server_updated_at, pk) > after`.
     *
     * Ordering: `server_updated_at ASC`, then `pkColumn ASC`.
     * Row count: limited to [limit].
     *
     * Implemented as `inline` in each subclass because [decodeList] requires a reified type.
     */
    protected abstract suspend fun fetchRemotePage(
        userId: String,
        overlapCursor: String?,
        after: PullPageKey?,
        limit: Int,
    ): List<DTO>

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
    // Pull algorithm — composite keyset pagination
    // ---------------------------------------------------------------------------

    @Suppress("TooGenericExceptionCaught")
    override suspend fun pull(userId: String, cursor: String?, resolver: ConflictResolver): PullResult =
        withContext(Dispatchers.IO) {
            val overlapCursor = overlapCursor(cursor)
            var pageKey: PullPageKey? = null
            var maxInstant: Instant? = null
            var skipped = false

            var pageIndex = 0
            var stoppedCleanly = false
            while (pageIndex < MAX_PULL_PAGES) {
                val nextKey = fetchAndApplyPage(userId, overlapCursor, pageKey, resolver)
                if (nextKey.skipped) skipped = true
                nextKey.maxInstant?.let { inst ->
                    if (maxInstant == null || inst.isAfter(maxInstant)) maxInstant = inst
                }
                pageKey = nextKey.nextPageKey
                pageIndex++
                if (nextKey.stop) {
                    stoppedCleanly = true
                    break
                }
            }

            // Defensive guard: exhausting the page budget WITHOUT a clean stop signal means a
            // broken keyset may be looping. Hold the cursor so the next cycle resumes from the
            // same position via the overlap window. A pull that completes cleanly on exactly the
            // last budgeted page is NOT penalized.
            if (!stoppedCleanly) skipped = true

            PullResult(maxServerUpdatedAt = maxInstant?.toString(), skippedRows = skipped)
        }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /** Outcome of a single fetch-and-apply iteration. */
    private data class PageResult(
        val maxInstant: Instant?,
        val skipped: Boolean,
        val nextPageKey: PullPageKey?,
        /** True when the caller should stop paginating. */
        val stop: Boolean,
    )

    /**
     * Fetches one page, applies each row inside a DB transaction, and returns a [PageResult].
     *
     * [PageResult.stop] is true when pagination should stop (empty page, incomplete page, or null
     * serverUpdatedAt defensive guard). [PageResult.nextPageKey] carries the keyset for the next
     * page (null when stop=true). [PageResult.skipped] is true when any row was skipped.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAndApplyPage(
        userId: String,
        overlapCursor: String?,
        after: PullPageKey?,
        resolver: ConflictResolver,
    ): PageResult {
        val page = fetchRemotePage(userId, overlapCursor, after, pageSize)
        if (page.isEmpty()) return PageResult(null, false, null, stop = true)

        var maxInstant: Instant? = null
        var skipped = false

        safeDbCall {
            transact {
                page.forEach { remote ->
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

        // Null lastSat is structurally impossible (NOT NULL column + trigger), but guard
        // defensively: hold the cursor so the next cycle re-pulls this window.
        val lastSat = page.last().serverUpdatedAt
        val nullSatGuard = lastSat == null
        val isLastPage = page.size < pageSize || nullSatGuard

        // The next page key is always built from the last FETCHED row, even if that row was
        // skipped due to a userId mismatch or an FK miss. Intra-run pagination intentionally
        // advances past skipped rows — they are NOT retried within this run. The `skipped = true`
        // flag holds the cursor in DefaultSyncRepository so the next sync cycle overlap-re-pulls
        // from the same position and retries them. Convergence for rows whose FK parent never
        // arrives is therefore deferred to subsequent cycles by design (same posture as the
        // pre-pagination cursor-hold semantics).
        val nextKey = if (isLastPage) null else PullPageKey(serverUpdatedAt = lastSat!!, pk = pkOf(page.last()))
        return PageResult(maxInstant, skipped || nullSatGuard, nextKey, stop = isLastPage)
    }

    companion object {
        const val DEFAULT_PULL_PAGE_SIZE = 500
        const val MAX_PULL_PAGES = 200
    }
}

// ---------------------------------------------------------------------------
// Shared filter helper — top-level to stay within TooManyFunctions budget
// ---------------------------------------------------------------------------

/**
 * Applies the standard pull filters to a [PostgrestFilterBuilder].
 *
 * - Always asserts `user_id = userId` (defence-in-depth over RLS).
 * - When [after] is non-null (page 2+): composite keyset strictly dominates, so the
 *   overlapCursor lower bound is intentionally omitted — it was already applied on page 1.
 * - When [after] is null and [overlapCursor] is non-null (page 1): `server_updated_at >= overlapCursor`.
 * - When both are null (full re-pull): only the user_id filter is applied.
 */
internal fun PostgrestFilterBuilder.applyPageFilter(
    userId: String,
    overlapCursor: String?,
    after: PullPageKey?,
    pkCol: String,
) {
    eq("user_id", userId)
    if (after != null) {
        // Composite keyset: rows where (server_updated_at, pk) strictly dominates `after`.
        // Equivalent to: server_updated_at > after.serverUpdatedAt
        //   OR (server_updated_at = after.serverUpdatedAt AND pk > after.pk)
        or {
            gt("server_updated_at", after.serverUpdatedAt)
            and {
                eq("server_updated_at", after.serverUpdatedAt)
                gt(pkCol, after.pk)
            }
        }
    } else if (overlapCursor != null) {
        gte("server_updated_at", overlapCursor)
    }
}
