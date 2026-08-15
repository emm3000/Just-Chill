package com.emm.domain.shared.backup

/**
 * Deletes the stored snapshots [SnapshotRetention] does not keep, and the leftovers that are not
 * snapshots at all.
 *
 * **A port of its own rather than an operation on [BackupUploader]**, because the two fail
 * differently and are triggered differently. An upload that fails means the ledger is not backed up
 * and the user should hear about it; a prune that fails means the bucket is untidy and the next run
 * will try again. Folding the prune into the uploader would also make every "back up now" carry a
 * delete, which is the one thing this pipeline should do reluctantly.
 *
 * ### What it deletes, and what it refuses to
 *
 * It keys on the **sidecar**, never on a payload name alone. `<name>.manifest.json` exists only
 * because the payload beside it was uploaded, read back, and matched its digest, so its presence is
 * the statement that the snapshot is good. A prune that filled its retention slots from bare names
 * would hand a slot to an unverified leftover and evict a *verified* snapshot to make room — data
 * loss, and invisible, because both are just names in a listing. Only complete pairs are candidates.
 *
 * A payload with no sidecar is an orphan and goes on sight; a sidecar with no payload can only ever
 * state a digest for bytes that are gone, and goes too. Both rules, and the argument for the good
 * bytes the first one occasionally throws away, are in `docs/sync/ADR009_PLAN.md` — that file is
 * what this unit was built from and it is not re-derived here.
 *
 * It never verifies anything. A pair that looks complete and will fail a later check is still kept:
 * falling back to the newest snapshot that VERIFIES is ADR 009 Phase 4's job, and a prune that
 * started deleting suspicious-looking pairs would be deciding it on the wrong side of the network.
 * Anything under the `pinned/` prefix is outside retention entirely and is never even listed.
 *
 * ### Failure
 *
 * **Reading fails loudly; deleting does not.** If the prefix or the listing cannot be read, this
 * throws a `DomainException` naming the step and nothing is deleted — pruning against a listing that
 * may be partial is exactly how a good snapshot disappears. Individual deletes are independent: one
 * failing leaves an object the next run picks up again, and it must not take the rest of the prune
 * with it. Those failures come back in [BackupPruneReport] instead, because ADR 009 hard constraint 4
 * forbids a failure nobody can see and a swallowed delete that reported nothing would be exactly one.
 */
interface BackupPruner {

    /** Runs one prune and reports what it did. See the class KDoc for what throws and what does not. */
    suspend fun prune(): BackupPruneReport
}

/**
 * What one prune did.
 *
 * It exists for [failedDeletes] and would not otherwise be worth its own type: a delete that fails
 * cannot throw — one object must not abort the rest — so this is the *only* channel by which it can
 * ever be noticed. Each entry names the object and what stopped it, which is what separates
 * "yesterday's cleanup was retried today" from "this bucket has been failing to delete the same
 * object for a month".
 */
data class BackupPruneReport(
    /** Snapshots the retention policy kept. Counts complete pairs, not objects. */
    val kept: Int,
    /** Objects actually removed — payloads, sidecars and leftovers together. */
    val deleted: Int,
    /** One diagnostic line per object that could not be removed; empty on a clean run. */
    val failedDeletes: List<String>,
)
