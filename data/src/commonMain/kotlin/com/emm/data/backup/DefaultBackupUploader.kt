package com.emm.data.backup

import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Uploads a snapshot, reads it back, and writes the manifest only once the bytes have matched.
 *
 * ### The order is the design, and it is the manifest that carries the meaning
 *
 * Payload, then read-back, then manifest. Written that way round, **the presence of a manifest is
 * itself the statement that the payload beside it was verified** — the one thing a later restore can
 * check without downloading the snapshot. Uploading the manifest first, or both together, keeps the
 * same bytes on the server and destroys that: a manifest would then mean "an upload was attempted",
 * and the pre-restore check would be a receipt written before the goods arrived.
 *
 * The state that ordering can leave behind is a payload with no manifest, and that is the *safe*
 * leftover precisely because the invariant above already tells a reader to ignore it. On a digest
 * mismatch the object is deleted anyway, so a bucket cannot accumulate blobs that are known bad.
 *
 * **A read-back that FAILS deliberately does not delete.** The failure that stopped the read is
 * overwhelmingly the transport, and a delete over the same dead transport would fail too — replacing
 * a precise "could not read it back" with a vague "could not clean up", which is the reason
 * disappearing rather than the object. The orphan it leaves is the safe leftover again, so ADR 009
 * Phase 2c's retention prune must treat a payload with no manifest as prunable rubbish and never as
 * a snapshot.
 *
 * ### What "verify" is, since the obvious reading is wrong
 *
 * `sha256Hex(readBackBytes) == manifest.payloadSha256`, raw bytes on both sides, and **not** a
 * comparison of the manifest's row counts. Both are derived from the same array by
 * [buildBackupManifest], so matching counts are a tautology when the digest matches and describe a
 * payload that no longer exists when it does not. [BackupManifestDto.rowCounts]'s own KDoc says so;
 * the counts earn their keep at listing time, not here.
 *
 * ### Failure reasons
 *
 * Six paths, six messages, because ADR 009 hard constraint 4 exists: the 2026-08-12 outage was
 * invisible for as long as it was because one path logged nothing. Uploading the payload, reading it
 * back, the digest disagreeing, failing to discard the object that disagreed, uploading the manifest
 * and having no session at all are six different things to go and look at, and collapsing them into
 * one "backup failed" would tell whoever is holding the outage only what they already knew.
 *
 * The exception TYPE still comes from the cause, so a caller can still tell a dead network from a
 * refusal; the message is what carries the step. Translation is [asBackupFailure], written here and
 * deliberately NOT delegated to `toSyncDomainException`: that function belongs to the row-replication
 * engine ADR 009 Phase 5 deletes, and reusing it would make backup break when sync is removed.
 * Duplication of text, not of knowledge (`docs/CODE_QUALITY.md`) — the two tables answer to different
 * owners and are meant to drift.
 */
class DefaultBackupUploader internal constructor(private val store: BackupObjectStore) : BackupUploader {

    /**
     * The production wiring. [store] stays `internal` so the seam cannot leak into `:presentation`,
     * which means Koin needs a constructor it can actually see; this is it.
     */
    constructor(client: SupabaseClient) : this(SupabaseBackupObjectStore(client))

    override suspend fun upload(fileName: String, payload: String) {
        // Encoded ONCE, and everything downstream reads this array: the digest in the manifest, the
        // bytes that travel, and the bytes the read-back is compared against. Re-encoding `payload`
        // for the upload would make the digest a claim about a String rather than about the file, and
        // a mismatch would then be indistinguishable from a corrupted transfer.
        val payloadBytes = payload.encodeToByteArray()
        val manifest = buildBackupManifest(fileName, payloadBytes)

        // Both keys resolved before anything is written, so a session that ends mid-upload cannot
        // land the manifest under a different prefix from the payload it describes — and so "nobody
        // is signed in" is reported before, rather than after, a blob exists.
        val payloadKey = store.ownedKey(fileName)
        val manifestKey = store.ownedKey(manifestNameFor(fileName))

        remotely("the payload could not be uploaded to $payloadKey") {
            store.upload(payloadKey, payloadBytes)
        }
        val readBack = remotely("the payload could not be read back from $payloadKey") {
            store.download(payloadKey)
        }

        if (sha256Hex(readBack) != manifest.payloadSha256) {
            remotely("${mismatchAt(payloadKey)}, and the unverified object could not be deleted") {
                store.delete(payloadKey)
            }
            throw DomainException.ValidationError(
                "$FAILED${mismatchAt(payloadKey)}; the unverified object was deleted.",
                ValidationCode.BackupFileInvalid,
            )
        }

        remotely("the manifest could not be uploaded to $manifestKey") {
            store.upload(manifestKey, manifest.encodeToJson().encodeToByteArray())
        }
    }
}

/**
 * The sidecar's name: the payload's, plus a suffix.
 *
 * **It appends rather than replacing the extension**, which looks clumsier and is the point. The
 * bucket only accepts `application/json`, and storage-kt derives that header from the key's
 * extension, so a name that stops ending in `.json` is refused with an HTTP 415 that reads like a
 * server fault. Appending cannot produce one whatever the caller passed; swapping an extension
 * quietly can, for any file name that did not have the extension the rule assumed.
 *
 * It also gives the pair a shape a listing can read without downloading anything: `.manifest.json`
 * is the manifest, everything else is a payload, and no payload name can collide with a sidecar.
 */
private fun manifestNameFor(fileName: String): String = "$fileName.manifest.json"

/** Shared by the mismatch itself and by a failure to clean up after it — one event, two endings. */
private fun mismatchAt(payloadKey: String): String =
    "the payload read back from $payloadKey does not match the digest its manifest states"

private const val FAILED = "Snapshot backup failed: "

/**
 * Runs one storage call and turns anything it throws into a [DomainException] that names [reason].
 *
 * `CancellationException` is rethrown before the generic catch, the same property Phase 0's sign-out
 * fix pinned: a cancelled snapshot is the caller going away, not a backup failure, and reporting it
 * as one would put a phantom outage in front of whoever is reading these messages.
 */
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> remotely(reason: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw e.asBackupFailure("$FAILED$reason.")
}

/**
 * Supabase/Ktor throwable to [DomainException], keeping [reason] as the message.
 *
 * A [DomainException] passes through unchanged — [BackupObjectStore.ownedKey] already throws one for
 * a missing session, and rewrapping it would bury the only failure here that is not a transport
 * problem under a message about the step that merely noticed it.
 *
 * [RestException] carries the server's own status, and it is spelled into the message rather than
 * left in the cause because the two refusals this bucket is configured to produce are unreadable
 * without it: **413** is a payload over the 10 MiB ceiling and **415** is a content type the bucket
 * does not allow. Both are deliberate server-side refusals, not faults, and a message that omitted
 * the code would send a reader hunting for a network problem that is not there.
 */
private fun Throwable.asBackupFailure(reason: String): DomainException = when (this) {
    is DomainException -> this

    is UnauthorizedRestException -> DomainException.Unauthorized(reason, this)

    // Storage is installed with requireValidSession = true, so an unresolved session throws instead
    // of silently downgrading to the anon key and collecting an RLS refusal that explains nothing.
    is SessionRequiredException -> DomainException.Unauthorized(reason, this)

    is RestException -> DomainException.Unknown(this, "$reason The server answered HTTP $statusCode: $error.")

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this, reason)

    else -> DomainException.Unknown(this, reason)
}
