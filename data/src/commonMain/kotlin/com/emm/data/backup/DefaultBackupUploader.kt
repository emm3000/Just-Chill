package com.emm.data.backup

import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.github.jan.supabase.SupabaseClient
import kotlin.coroutines.cancellation.CancellationException

class DefaultBackupUploader internal constructor(private val store: BackupObjectStore) : BackupUploader {

    constructor(client: SupabaseClient) : this(SupabaseBackupObjectStore(client))

    override suspend fun upload(userId: String, fileName: String, payload: String) {
        val payloadBytes = payload.encodeToByteArray()
        val manifest = buildBackupManifest(fileName, payloadBytes)
        val manifestBytes = encodeAsDomainException { manifest.encodeToJson() }.encodeToByteArray()

        val prefix = ownedPrefixFor(userId)
        val payloadKey = prefix + fileName
        val manifestKey = prefix + manifestNameFor(fileName)

        remotely("the payload could not be uploaded to $payloadKey") {
            store.upload(payloadKey, payloadBytes)
        }
        val readBack = remotely("the payload could not be read back from $payloadKey") {
            store.download(payloadKey)
        }
        if (sha256Hex(readBack) != manifest.payloadSha256) {
            val cleanup: Throwable? = discarding { store.delete(payloadKey) }
            throw unverified(
                if (cleanup == null) {
                    "${payloadMismatchAt(payloadKey)}; the unverified object was deleted."
                } else {
                    "${payloadMismatchAt(payloadKey)}, and the unverified object could not be deleted."
                },
                cleanup,
            )
        }

        remotely("the manifest could not be uploaded to $manifestKey") {
            store.upload(manifestKey, manifestBytes)
        }
        val manifestReadBack = remotely("the manifest could not be read back from $manifestKey") {
            store.download(manifestKey)
        }
        if (!manifestReadBack.contentEquals(manifestBytes)) {
            val cleanup: Throwable? = discarding {
                store.delete(manifestKey)
                store.delete(payloadKey)
            }
            throw unverified(
                if (cleanup == null) {
                    "${manifestMismatchAt(manifestKey)}; both objects were deleted."
                } else {
                    "${manifestMismatchAt(manifestKey)}, and the pair could not be deleted."
                },
                cleanup,
            )
        }
    }

    /**
     * Converts a cleanup failure into data so the verdict above it survives; nothing is swallowed,
     * the throwable becomes the thrown exception's cause.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun discarding(block: suspend () -> Unit): Throwable? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e
    }

    private suspend fun ownedPrefixFor(userId: String): String {
        val prefix: String = remotely(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val owner: String = ownerPrefixOf(userId)
        if (prefix != owner) throw ownerChanged(owner, prefix)
        return prefix
    }
}

private fun payloadMismatchAt(payloadKey: String): String =
    "the payload read back from $payloadKey does not match the digest its manifest states"

private fun manifestMismatchAt(manifestKey: String): String =
    "the manifest read back from $manifestKey does not match the bytes that were uploaded"

private fun ownerChanged(owner: String, live: String): DomainException = DomainException.Unauthorized(
    "${FAILED}the signed-in account changed while the snapshot was being taken: it belongs under " +
        "$owner and the live session owns $live, so nothing was uploaded.",
)

private fun unverified(reason: String, cleanupFailure: Throwable? = null): DomainException =
    DomainException.ValidationError(
        "$FAILED$reason",
        ValidationCode.BackupUploadUnverified,
        cleanupFailure,
    )

private const val FAILED = "Snapshot backup failed: "

private const val PREFIX_UNRESOLVED = "the owning prefix could not be resolved"

private suspend fun <T> remotely(reason: String, block: suspend () -> T): T = storageCall("$FAILED$reason.", block)
