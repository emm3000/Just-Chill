package com.emm.data.backup

import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.domain.shared.backup.BackupVerifier
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

// Well under the 7 + 8 + 12 a shelf can hold: a bucket where nothing verifies must answer without
// downloading every snapshot in it.
internal const val BACKUP_VERIFY_MAX_PAIRS: Int = 5

class DefaultBackupVerifier internal constructor(private val store: BackupObjectStore) : BackupVerifier {

    constructor(client: SupabaseClient) : this(SupabaseBackupObjectStore(client))

    override suspend fun verifyLatest(): BackupVerification {
        val prefix: String = storageCall(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val names: List<String> = store.wholeBucket(prefix, LIST_FAILED, LISTING_NEVER_ENDED)
        val inspected: List<SnapshotPair> = pairsNewestFirst(names).take(BACKUP_VERIFY_MAX_PAIRS)

        val verified: BackupVerification.Verified? = firstThatVerifies(prefix, inspected)
        return when {
            verified != null -> verified
            inspected.isEmpty() -> BackupVerification.NoSnapshots
            else -> BackupVerification.NothingVerified(inspected.size)
        }
    }

    private suspend fun firstThatVerifies(prefix: String, pairs: List<SnapshotPair>): BackupVerification.Verified? {
        for ((index, pair) in pairs.withIndex()) {
            val verified: BackupVerification.Verified? = verify(prefix, pair, isNewestPair = index == 0)
            if (verified != null) return verified
        }
        return null
    }

    private suspend fun verify(
        prefix: String,
        pair: SnapshotPair,
        isNewestPair: Boolean,
    ): BackupVerification.Verified? {
        val manifestKey: String = prefix + manifestNameFor(pair.fileName)
        val manifest = decodeBackupManifestOrNull(read(manifestKey)) ?: return null

        val decoded: DecodedBackup? = decodeIfDigestMatches(manifest, read(prefix + pair.fileName))
        return decoded?.let {
            BackupVerification.Verified(
                fileName = pair.fileName,
                takenAt = pair.takenAt,
                schemaVersion = it.declaredVersion,
                rowCounts = it.payload.rowCounts(),
                isNewestPair = isNewestPair,
            )
        }
    }

    private suspend fun read(key: String): ByteArray = storageCall(unreadable(key)) { store.download(key) }
}

// The digest is checked before the payload is parsed on purpose: a payload that parses is not
// evidence it is the payload the manifest describes, and only the digest can say so.
private fun decodeIfDigestMatches(manifest: BackupManifestDto, payloadBytes: ByteArray): DecodedBackup? {
    if (sha256Hex(payloadBytes) != manifest.payloadSha256) return null
    return decodeBackupPayloadOrNull(payloadBytes.decodeToString())
}

// A payload with no sidecar is not verifiable and is left exactly where it is: an orphan is the
// prune's business, and a check that deletes is not a check.
private fun pairsNewestFirst(names: List<String>): List<SnapshotPair> {
    val flat: List<String> = names.filterNot { it.contains('/') }
    val present: Set<String> = flat.toSet()
    return flat
        .mapNotNull { name -> parseBackupSnapshotTakenAt(name)?.let { SnapshotPair(name, it) } }
        .filter { manifestNameFor(it.fileName) in present }
        .sortedByDescending { it.takenAt }
}

private fun ExportPayloadDto.rowCounts(): BackupRowCounts = BackupRowCounts(
    accounts = accounts.size,
    categories = categories.size,
    transactions = transactions.size,
    recurringMovements = recurringMovements.size,
)

private class SnapshotPair(val fileName: String, val takenAt: Instant)

private fun unreadable(key: String): String = "$VERIFY_FAILED $key could not be downloaded."

private const val VERIFY_FAILED = "Snapshot backup verification failed:"

private const val PREFIX_UNRESOLVED = "$VERIFY_FAILED the owning prefix could not be resolved."

private const val LIST_FAILED = "$VERIFY_FAILED the bucket could not be listed."

private const val LISTING_NEVER_ENDED = "$VERIFY_FAILED the listing never returned an empty page within " +
    "$BACKUP_LIST_MAX_PAGES pages of $BACKUP_LIST_PAGE_SIZE objects, so the newest snapshot may not " +
    "be in it at all — the bucket is listed by name ascending and the newest names sort last."
