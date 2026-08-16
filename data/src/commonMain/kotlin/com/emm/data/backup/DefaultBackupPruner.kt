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

class DefaultBackupPruner internal constructor(
    private val store: BackupObjectStore,
    private val clock: Clock,
    private val zone: TimeZone,
) : BackupPruner {

    constructor(client: SupabaseClient, clock: Clock, zone: TimeZone) :
        this(SupabaseBackupObjectStore(client), clock, zone)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun prune(): BackupPruneReport {
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
                // Recorded, never rethrown — one failed delete must not take the rest of the run
                // down with it, and the next prune sees this object again.
                failures += "$name could not be deleted: ${e.message ?: e::class.simpleName}"
            }
        }
        return BackupPruneReport(kept = plan.kept, deleted = deleted, failedDeletes = failures)
    }

    private suspend fun wholeBucket(prefix: String): List<String> {
        val names = mutableListOf<String>()
        var offset = 0
        repeat(BACKUP_LIST_MAX_PAGES) {
            val page: ObjectPage = storageCall(LIST_FAILED) { store.list(prefix, BACKUP_LIST_PAGE_SIZE, offset) }
            names += page.names
            if (page.serverReturned == 0) return names
            offset += page.serverReturned
        }
        throw listingNeverEnded()
    }
}

private class PrunePlan(val kept: Int, val delete: List<String>)

private fun planPrune(names: List<String>, now: Instant, zone: TimeZone): PrunePlan {
    val flat: List<String> = names.filterNot { it.contains('/') }
    val present: Set<String> = flat.toSet()

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

private fun listingNeverEnded(): DomainException {
    val reason = "$PRUNE_FAILED the listing never returned an empty page within " +
        "$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects, so it cannot be read " +
        "completely. Pruning a partial listing can delete a snapshot that is not really the " +
        "oldest, so nothing was deleted."
    return DomainException.Unknown(IllegalStateException(reason), reason)
}

private const val PRUNE_FAILED = "Snapshot retention prune failed:"

private const val PREFIX_UNRESOLVED = "$PRUNE_FAILED the owning prefix could not be resolved."

private const val LIST_FAILED = "$PRUNE_FAILED the bucket could not be listed, so nothing was deleted."
