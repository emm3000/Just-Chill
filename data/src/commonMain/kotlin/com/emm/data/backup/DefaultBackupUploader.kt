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
 * Uploads a snapshot, reads it back, and writes the manifest only once the bytes have matched — then
 * reads the manifest back too.
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
 * disappearing rather than the object. The orphan it leaves is the safe leftover again.
 *
 * **The rule that makes an orphan safe lives in `docs/sync/ADR009_PLAN.md`, not here.** ADR 009
 * Phase 2c's retention prune keys on the presence of `<name>.manifest.json` and never on `<name>`
 * alone; a payload without its sidecar counts toward no retention bucket and is deleted on sight.
 * That rule is stated in the plan because that is what 2c is built from, and its full argument —
 * including which orphans it knowingly throws away — is there. It is repeated here only so nobody
 * changes this ordering believing the prune will sort it out.
 *
 * ### What "verify" is, since the obvious reading is wrong
 *
 * For the payload: `sha256Hex(readBackBytes) == manifest.payloadSha256`, raw bytes on both sides, and
 * **not** a comparison of the manifest's row counts. Both are derived from the same array by
 * [buildBackupManifest], so matching counts are a tautology when the digest matches and describe a
 * payload that no longer exists when it does not. [BackupManifestDto.rowCounts]'s own KDoc says so;
 * the counts earn their keep at listing time, not here.
 *
 * **The manifest is verified too, and by plain byte equality**, because trusting its 200 is the exact
 * thing this class exists to refuse. It needs no digest of its own: the array that was uploaded is
 * still in hand, it is a few hundred bytes, and comparing it to what came back is strictly stronger
 * than hashing both. What it prevents is a false alarm on a GOOD snapshot — a manifest corrupted in
 * transit states a digest the payload beside it will never match, so a later pre-restore check
 * condemns intact bytes and the owner is told a perfectly restorable ledger is broken.
 *
 * On a manifest mismatch **both** objects are deleted, in that order. There is no valid receipt, so
 * there is nothing to keep: a payload whose only manifest is known wrong is exactly the orphan the
 * prune discards anyway, and leaving the bad manifest up would leave the false alarm in place. This
 * is also the one cleanup path where the transport is known to be working — the mismatch was
 * measured over bytes that arrived — so the delete is expected to succeed rather than hoped for, and
 * that is what separates it from the read-back failure two paragraphs up.
 *
 * ### Failure reasons
 *
 * **Eleven paths, eleven messages**, because ADR 009 hard constraint 4 exists: the 2026-08-12 outage
 * was invisible for as long as it was because one path logged nothing. Nine of them originate here —
 * resolving the owning prefix, uploading the payload, reading it back, the digest disagreeing,
 * failing to discard the object that disagreed, uploading the manifest, reading the manifest back,
 * the manifest disagreeing, failing to discard the pair — and two more come out of
 * [SupabaseBackupObjectStore.ownedPrefix] intact: nobody is signed in, and a session that never
 * finished loading. Collapsing any two would tell whoever is holding the outage only what they
 * already knew. `DefaultBackupUploaderTest` asserts the nine as a property rather than trusting nine
 * expectations to disagree by accident; `SupabaseBackupObjectStoreTest` owns the other two.
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
        // Serialised OUTSIDE the upload's failure wrapper: a defect in the manifest's own encoding is
        // not a transport problem, and reporting it as "the manifest could not be uploaded" would
        // send whoever is reading that line to the network.
        val manifestBytes = manifest.encodeToJson().encodeToByteArray()

        // Resolved ONCE, and both keys are built from it. Two calls would be two reads of the session
        // and therefore two possible prefixes; what actually stops a split pair reaching the bucket is
        // the RLS `with check` on `storage.objects`, which refuses whichever key does not match the
        // live session — a backstop in the migration, not in this file. Resolving once is what makes
        // the pair consistent by construction instead of by the server's mercy, and it is also what
        // reports "nobody is signed in" before rather than after a blob exists.
        val prefix = remotely(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val payloadKey = prefix + fileName
        val manifestKey = prefix + manifestNameFor(fileName)

        remotely("the payload could not be uploaded to $payloadKey") {
            store.upload(payloadKey, payloadBytes)
        }
        val readBack = remotely("the payload could not be read back from $payloadKey") {
            store.download(payloadKey)
        }
        if (sha256Hex(readBack) != manifest.payloadSha256) {
            remotely("${payloadMismatchAt(payloadKey)}, and the unverified object could not be deleted") {
                store.delete(payloadKey)
            }
            throw unverified("${payloadMismatchAt(payloadKey)}; the unverified object was deleted.")
        }

        remotely("the manifest could not be uploaded to $manifestKey") {
            store.upload(manifestKey, manifestBytes)
        }
        val manifestReadBack = remotely("the manifest could not be read back from $manifestKey") {
            store.download(manifestKey)
        }
        if (!manifestReadBack.contentEquals(manifestBytes)) {
            remotely("${manifestMismatchAt(manifestKey)}, and the pair could not be deleted") {
                // Manifest FIRST. If the second delete fails, what survives is a payload with no
                // sidecar — the leftover the retention rule already knows to throw away. Deleting the
                // payload first and failing would leave the opposite: a receipt for goods that are
                // gone, which is the one shape a pre-restore check has no way to catch.
                store.delete(manifestKey)
                store.delete(payloadKey)
            }
            throw unverified("${manifestMismatchAt(manifestKey)}; both objects were deleted.")
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
private fun payloadMismatchAt(payloadKey: String): String =
    "the payload read back from $payloadKey does not match the digest its manifest states"

/** The same pair of endings for the sidecar. Byte equality, not a digest — see the class KDoc. */
private fun manifestMismatchAt(manifestKey: String): String =
    "the manifest read back from $manifestKey does not match the bytes that were uploaded"

/** Both mismatches are the same event to the person holding the phone: a backup that is not trustworthy. */
private fun unverified(reason: String): DomainException = DomainException.ValidationError(
    "$FAILED$reason",
    ValidationCode.BackupUploadUnverified,
)

private const val FAILED = "Snapshot backup failed: "

private const val PREFIX_UNRESOLVED = "the owning prefix could not be resolved"

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
