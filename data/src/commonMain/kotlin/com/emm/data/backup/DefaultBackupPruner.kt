package com.emm.data.backup

import com.emm.domain.shared.backup.BackupPruneReport
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.RetentionCandidate
import com.emm.domain.shared.backup.SnapshotRetention
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import kotlinx.datetime.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Reads the bucket, works out what retention keeps, and deletes the rest.
 *
 * The decision itself is not here: `SnapshotRetention` is a pure `:domain` object over ids and
 * timestamps, and this class is the half that knows there is a bucket — resolving the prefix,
 * pairing payloads with sidecars, and issuing deletes. Splitting it that way is what lets the slot
 * arithmetic be tested exhaustively with no storage anywhere, and it is the half where an off-by-one
 * deletes real financial history.
 *
 * ### The listing is paged to completion, and completeness is PROVED rather than inferred
 *
 * The bucket is read one page at a time until the server answers with a short one. A short page is
 * proof there is nothing after it; a count that merely came in under the limit is not, and that
 * distinction is the defect this shape closes. The previous version refused any listing that came
 * back at exactly [BACKUP_LIST_PAGE_SIZE] — but it counted the list *after* folder entries had been
 * dropped, so a full page holding `pinned` arrived one short, the refusal never fired, and the prune
 * ran on a truncated listing believing it was whole. That refusal was also terminal rather than a
 * skip: nothing advanced an offset, so a bucket that genuinely reached the limit would have thrown
 * on every run forever, and the only thing that could have brought it back under the limit was the
 * prune itself.
 *
 * ### Reading fails loudly, deleting does not
 *
 * A failure to resolve the prefix or to read **any** page throws, and nothing is deleted — a partial
 * view can decide a snapshot is the newest when it is not, then delete real ones to fill slots
 * already occupied by objects it could not see. A cut can also fall between a payload and its
 * sidecar, and an orphan payload is deleted on sight. A skipped prune costs storage; a prune on a
 * truncated listing costs data. Running past [BACKUP_LIST_MAX_PAGES] is the same answer for the same
 * reason: a server still answering full pages there is an anomaly, not a busy bucket.
 *
 * Individual deletes are independent and none of them can abort the run. An object that fails to
 * delete is a leftover the next prune sees again, and taking the rest of the run down with it would
 * turn one stuck object into a bucket that never gets cleaned. They surface in
 * [BackupPruneReport.failedDeletes] instead — ADR 009 hard constraint 4 does not allow a failure
 * nobody can see, and a swallowed delete reporting nothing would be one.
 *
 * ### The clock and the zone are injected, and neither has a default
 *
 * `docs/DATE_AUDIT.md` rule #7. "Which day is this snapshot from" is a question about the person
 * holding the phone, and the composition root (`hh/di/SharedModule.kt`) is the only place allowed to
 * answer it from the machine. The clock is read **once** per prune, so every bucket boundary in one
 * run is measured against the same instant.
 */
class DefaultBackupPruner internal constructor(
    private val store: BackupObjectStore,
    private val clock: Clock,
    private val zone: TimeZone,
) : BackupPruner {

    /**
     * The production wiring. [store] stays `internal` so the seam cannot leak into `:presentation`,
     * which means Koin needs a constructor it can actually see; this is it — the same shape, for the
     * same reason, as [DefaultBackupUploader]'s.
     */
    constructor(client: SupabaseClient, clock: Clock, zone: TimeZone) :
        this(SupabaseBackupObjectStore(client), clock, zone)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun prune(): BackupPruneReport {
        // Resolved ONCE, like the uploader does it: two calls would be two reads of the session, and
        // a sign-out landing between them would have the second half of this run addressing keys
        // under somebody else's prefix. It is also what reports "nobody is signed in" before any
        // delete is issued rather than after some of them are.
        val prefix: String = storageCall(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val names: List<String> = wholeBucket(prefix)

        val plan: PrunePlan = planPrune(names, clock.now(), zone)

        val failures = mutableListOf<String>()
        var deleted = 0
        plan.delete.forEach { name ->
            try {
                store.delete(prefix + name)
                deleted++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Recorded, never rethrown: the objects after this one are unrelated to it, and the
                // next prune sees this one again. The reason is kept because "the same object has
                // failed to delete for a month" is only visible if the reason travels.
                failures += "$name could not be deleted: ${e.message ?: e::class.simpleName}"
            }
        }
        return BackupPruneReport(kept = plan.kept, deleted = deleted, failedDeletes = failures)
    }

    /**
     * Every object under [prefix], assembled page by page, or a named refusal that deletes nothing.
     *
     * **The comparison is against [ObjectPage.serverReturned], never against the names received.**
     * The store drops folder entries, so a page that was full arrives shorter than the limit — and a
     * pager that stopped there would treat a truncated listing as a complete one, which is the
     * data-loss shape the whole class is written around. The server's own count is the only number
     * that answers "was this page full".
     *
     * A page bigger than the limit is treated as full and paging continues: [BACKUP_LIST_MAX_PAGES]
     * bounds the loop whatever the server does, and over-reading is safe where under-reading is not.
     */
    private suspend fun wholeBucket(prefix: String): List<String> {
        val names = mutableListOf<String>()
        var offset = 0
        repeat(BACKUP_LIST_MAX_PAGES) {
            val page: ObjectPage = storageCall(LIST_FAILED) { store.list(prefix, BACKUP_LIST_PAGE_SIZE, offset) }
            names += page.names
            if (page.serverReturned < BACKUP_LIST_PAGE_SIZE) return names
            offset += BACKUP_LIST_PAGE_SIZE
        }
        throw listingNeverEnded()
    }
}

/** What one prune decided before it touched anything: how many pairs survive, and what goes. */
private class PrunePlan(val kept: Int, val delete: List<String>)

/**
 * Turns a listing into a delete list, with no IO in sight so the whole classification is testable.
 *
 * The order of the checks is the safety argument:
 *
 * 1. **Anything with a `/` in it is dropped before anything else looks at it.** A listing is
 *    supposed to be non-recursive, so `pinned/…` cannot appear — this is the belt to that braces,
 *    and it is stated first because "the prune never scans `pinned/`" is a promise ADR 009 makes to
 *    a user who pinned a snapshot before a schema migration.
 * 2. **Sidecars are recognised by suffix, payloads by parsing.** A sidecar name never parses as a
 *    snapshot — [parseBackupSnapshotTakenAt] matches whole names — so the two sets cannot overlap.
 * 3. **A name that parses as neither is left alone.** Not a snapshot, not a receipt, not this app's
 *    business — and a prune that deleted what it did not recognise would be the worst possible
 *    reading of "tidy up".
 * 4. **Only complete pairs reach the retention policy.** That is the whole reason this keys on the
 *    sidecar: filling slots from bare names would give an unverified leftover a slot and evict a
 *    verified snapshot to make room.
 *
 * Deletion order within an evicted pair is **sidecar first**, matching the uploader's cleanup for
 * the same reason: if the second delete fails, what survives is a payload with no receipt — the
 * leftover the rules already handle — instead of a receipt for bytes that are gone, which is the one
 * shape a pre-restore check cannot catch.
 */
private fun planPrune(names: List<String>, now: Instant, zone: TimeZone): PrunePlan {
    val flat: List<String> = names.filterNot { it.contains('/') }
    val present: Set<String> = flat.toSet()

    // payload name -> the sidecar sitting beside it.
    val sidecars: Map<String, String> = flat
        .filter { it.endsWith(BACKUP_MANIFEST_SUFFIX) }
        .associateBy { it.removeSuffix(BACKUP_MANIFEST_SUFFIX) }

    val snapshots: List<Pair<String, Instant>> = flat.mapNotNull { name ->
        parseBackupSnapshotTakenAt(name)?.let { name to it }
    }
    val (verified, orphanPayloads) = snapshots.partition { (name, _) -> name in sidecars }

    val decision = SnapshotRetention.select(
        verified.map { (name, takenAt) -> RetentionCandidate(name, takenAt.toEpochMilliseconds()) },
        now,
        zone,
    )

    val orphanSidecars: List<String> = sidecars.filterKeys { it !in present }.values.toList()
    val evicted: List<String> = decision.delete.flatMap { listOf(sidecars.getValue(it), it) }

    return PrunePlan(
        kept = decision.keep.size,
        delete = orphanSidecars + orphanPayloads.map { it.first } + evicted,
    )
}

/**
 * The refusal that makes [BACKUP_LIST_MAX_PAGES] a bound rather than a wish.
 *
 * It is [DomainException.Unknown] rather than a `ValidationError` for the same reason
 * [SupabaseBackupObjectStore.upload]'s `.json` check is: nothing about it is user input, and its
 * user-facing text should say so. Both numbers are spelled into the message because "ten pages of a
 * thousand" is what tells a reader this is a server answering full pages forever rather than a
 * bucket that happens to be large.
 */
private fun listingNeverEnded(): DomainException {
    val reason = "$PRUNE_FAILED the bucket was still answering full pages after " +
        "$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects, so the listing cannot be " +
        "read completely. Pruning a partial listing can delete a snapshot that is not really the " +
        "oldest, so nothing was deleted."
    return DomainException.Unknown(IllegalStateException(reason), reason)
}

private const val PRUNE_FAILED = "Snapshot retention prune failed:"

private const val PREFIX_UNRESOLVED = "$PRUNE_FAILED the owning prefix could not be resolved."

private const val LIST_FAILED = "$PRUNE_FAILED the bucket could not be listed, so nothing was deleted."
