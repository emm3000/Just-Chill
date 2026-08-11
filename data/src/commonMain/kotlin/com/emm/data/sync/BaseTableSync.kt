package com.emm.data.sync

import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.safeDbCall
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.LocalRevision
import com.emm.domain.sync.Resolution
import com.emm.domain.sync.SyncLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.withContext
import kotlin.time.Instant

/** Composite keyset position used to paginate through a remote table. */
data class PullPageKey(val serverUpdatedAt: String, val pk: String)

/**
 * What one remote row did when the pull tried to apply it.
 *
 * The distinction that matters is [Deferred] versus [Dropped], and it is the difference between a
 * wait and a freeze. A [Deferred] row holds the pull cursor so the next cycle re-pulls it; that is
 * correct only for something that can still resolve on its own. Reporting a row that can NEVER be
 * applied the same way pins the cursor permanently — and because the cursor is shared, it pins it
 * for every table, so nothing remote ever reaches the device again. Silently.
 */
enum class RemoteRowOutcome {
    /** Written locally. */
    Applied,

    /**
     * Not applied YET, and it can be: the FK parent has not been pulled or pushed yet. Holds the
     * cursor so the next cycle retries it once the parent arrives.
     */
    Deferred,

    /**
     * Never applicable: the row carries a value this client cannot represent, and re-pulling it
     * would produce the same answer forever. Dropped locally, and the cursor advances past it. If
     * the server later corrects the row its `server_updated_at` bumps and it comes back on its own.
     *
     * Whoever returns this MUST log why — the base class cannot know the reason, and a skipped row
     * that leaves no trace is a data gap nobody can diagnose after the fact.
     */
    Dropped,
}

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
 *   - [localRevision]         — reads the local updatedAt + syncState for a PK (conflict inputs)
 *   - [applyRemoteRow]        — two-statement INSERT OR IGNORE + UPDATE; reports a [RemoteRowOutcome]
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
    /**
     * Where the silent row skips below become visible. Every skip is a row this device decided not
     * to apply — a data gap the user cannot see and the algorithm never reports upward.
     */
    protected val logger: SyncLogger,
    /**
     * Remote table name, used only to identify this instance in log lines (all four subclasses
     * share this class, so a message without it is unattributable).
     */
    protected val tableName: String,
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

    /**
     * Read the local row's conflict inputs for [pk], or null when the row is unknown locally.
     *
     * Carries `syncState` as well as `updatedAt`: without it the resolver cannot tell an unpushed
     * local edit from an untouched copy of the server's own row, and arbitrates both by clock.
     */
    protected abstract fun localRevision(pk: String): LocalRevision?

    /**
     * Two-statement upsert: INSERT OR IGNORE + UPDATE. Neither statement triggers an implicit
     * DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL) are never fired.
     *
     * Returns [RemoteRowOutcome.Deferred] when a [SQLiteConstraintException] fires (orphan FK —
     * retry next cycle), and [RemoteRowOutcome.Dropped] for a row this client will never be able to
     * apply. See [RemoteRowOutcome] for why those two must not be reported the same way.
     */
    protected abstract fun applyRemoteRow(remote: DTO): RemoteRowOutcome

    /** Re-flag the row with [pk] as Pending so its newer local revision re-pushes next cycle. */
    protected abstract fun markPendingForResync(pk: String)

    // ---------------------------------------------------------------------------
    // Push algorithm
    // ---------------------------------------------------------------------------

    // Intentional broad catch: funnelled to DomainException by DefaultSyncRepository.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun push(userId: String): Unit = withContext(ioDispatcher) {
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
        withContext(ioDispatcher) {
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
                    if (maxInstant == null || inst > maxInstant!!) maxInstant = inst
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

        val mark = CursorHighWaterMark()
        var skipped = false

        safeDbCall {
            transact {
                page.forEach { remote ->
                    // Defence-in-depth against RLS misconfig: never apply another user's row.
                    if (remote.userId != userId) {
                        // A foreign row reaching this device means the server-side policy let it
                        // through. Loud on purpose — the skip below is otherwise invisible. Ids
                        // only: the row belongs to someone else, so none of it may be logged.
                        logger.warn("pull skipped foreign row table=$tableName pk=${pkOf(remote)}")
                        skipped = true
                        return@forEach
                    }

                    val resolution = resolver.resolve(localRevision(pkOf(remote)), remote.updatedAt)
                    when (resolution) {
                        Resolution.ApplyRemote -> when (applyRemoteRow(remote)) {
                            RemoteRowOutcome.Applied -> Unit

                            // FK parent absent locally. Expected transiently; permanent only if
                            // the parent never arrives — which is exactly what this line makes
                            // diagnosable after the fact. Holding the cursor IS the retry.
                            RemoteRowOutcome.Deferred -> {
                                logger.warn("pull skipped fk miss table=$tableName pk=${pkOf(remote)}")
                                skipped = true
                                return@forEach
                            }

                            // Unapplicable forever, so it must NOT hold the cursor: fall through to
                            // the high-water mark below and let the pull move past it. Already
                            // logged, with its reason, by whoever dropped it — only that code knows
                            // why, and a second generic line here would double-count the skip.
                            RemoteRowOutcome.Dropped -> Unit
                        }

                        // Local row is newer than what the server holds: re-flag it Pending so the
                        // newer local revision re-pushes next cycle. Without this, a stale remote
                        // upsert from another device would leave the two replicas diverged forever.
                        Resolution.KeepLocal ->
                            markPendingForResync(pkOf(remote))
                    }

                    if (!mark.offer(remote.serverUpdatedAt)) {
                        // Same class of problem as the null serverUpdatedAt guard below, and just
                        // as invisible: the row applied, but it cannot advance the cursor. Holding
                        // the cursor re-pulls this window next cycle.
                        logger.warn("pull hit unparseable serverUpdatedAt table=$tableName pk=${pkOf(remote)}")
                        skipped = true
                    }
                }
            }
        }

        // Null lastSat is structurally impossible (NOT NULL column + trigger), but guard
        // defensively: hold the cursor so the next cycle re-pulls this window.
        val lastSat = page.last().serverUpdatedAt
        val nullSatGuard = lastSat == null
        if (nullSatGuard) {
            // Reaching this means the remote schema no longer matches what the cursor math assumes,
            // and the cursor will now be held every cycle. Never silently.
            logger.warn("pull hit null serverUpdatedAt table=$tableName pk=${pkOf(page.last())}")
        }
        val isLastPage = page.size < pageSize || nullSatGuard

        // The next page key is always built from the last FETCHED row, even if that row was
        // skipped due to a userId mismatch or an FK miss. Intra-run pagination intentionally
        // advances past skipped rows — they are NOT retried within this run. The `skipped = true`
        // flag holds the cursor in DefaultSyncRepository so the next sync cycle overlap-re-pulls
        // from the same position and retries them. Convergence for rows whose FK parent never
        // arrives is therefore deferred to subsequent cycles by design (same posture as the
        // pre-pagination cursor-hold semantics).
        val nextKey = if (isLastPage) null else PullPageKey(serverUpdatedAt = lastSat!!, pk = pkOf(page.last()))
        return PageResult(mark.value, skipped || nullSatGuard, nextKey, stop = isLastPage)
    }

    /**
     * Highest `server_updated_at` seen while applying a page — the value the pull cursor advances
     * to. Its own type so the parse, the comparison and the unreadable-value case stay out of the
     * row loop, which reads one branch per policy decision and is complexity-capped.
     */
    private class CursorHighWaterMark {

        var value: Instant? = null
            private set

        /**
         * Offers one row's raw `server_updated_at`. Returns false only when a value was present and
         * could not be parsed — an absent one is normal for a row this page did not advance past.
         */
        fun offer(raw: String?): Boolean {
            val parsed = raw?.let(::parseServerInstant)
            if (parsed != null) {
                val current = value
                if (current == null || parsed > current) value = parsed
            }
            return raw == null || parsed != null
        }
    }

    companion object {
        const val DEFAULT_PULL_PAGE_SIZE = 500
        const val MAX_PULL_PAGES = 200
    }
}

// ---------------------------------------------------------------------------
// Shared helpers — top-level to stay within TooManyFunctions budget
// ---------------------------------------------------------------------------

/** The one `syncState` value that means the server already holds this exact revision. */
private const val SYNCED = "Synced"

/**
 * Builds the resolver's input from a local `(updatedAt, syncState)` pair.
 *
 * Anything that is not [SYNCED] counts as an unpushed edit. An unrecognised state therefore
 * protects the local row rather than letting the server overwrite it — the safe direction when
 * the alternative is discarding something the user typed.
 */
internal fun localRevisionOf(updatedAt: Long, syncState: String): LocalRevision =
    LocalRevision(updatedAt = updatedAt, hasUnpushedEdit = syncState != SYNCED)

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
