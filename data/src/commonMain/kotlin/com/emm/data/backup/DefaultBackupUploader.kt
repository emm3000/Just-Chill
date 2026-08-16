package com.emm.data.backup

import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.github.jan.supabase.SupabaseClient

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
 * ### The prefix is asserted against the caller's account, not merely resolved
 *
 * [BackupUploader.upload] takes the account the snapshot is *for*, and the resolved prefix has to be
 * that account's or nothing is written. The prefix still comes from the live session and never from
 * the parameter — a caller cannot aim a snapshot anywhere — so this adds no destination, only a
 * refusal.
 *
 * What it closes is a race the server cannot see. A caller decides "this is user A's ledger, and A's
 * watermark", then suspends here through an export and four round trips under a 120s
 * `transferTimeout`. An account switch landing inside that window makes this class resolve **B's**
 * prefix, and the bucket's RLS `with check` *accepts* the write, because the key matches whoever is
 * signed in now. A's ledger lands in B's bucket and A is marked backed up. One decision read the
 * session twice — the same shape `core/sync/SyncKillSwitch.kt` blames for the dangling cross-tenant
 * rows that ended row replication.
 *
 * The comparison is against [ownerPrefixOf], which is also what builds the prefix in
 * [SupabaseBackupObjectStore]; the layout is declared once and read here, never respelled. It sits
 * immediately after the single resolution and before the first upload, so a mismatch costs a session
 * read and leaves the bucket untouched.
 *
 * ### Failure reasons
 *
 * **Thirteen paths, thirteen messages**, because ADR 009 hard constraint 4 exists: the 2026-08-12
 * outage was invisible for as long as it was because one path logged nothing. Ten of them originate
 * here — resolving the owning prefix, the session no longer belonging to the account the snapshot is
 * for, uploading the payload, reading it back, the digest disagreeing,
 * failing to discard the object that disagreed, uploading the manifest, reading the manifest back,
 * the manifest disagreeing, failing to discard the pair — and three more come out of
 * [SupabaseBackupObjectStore] intact: nobody is signed in, a session that never finished loading,
 * and a key that does not end in `.json`. That last one is a defect in this app's own naming rather
 * than a transport failure, and it is counted with the rest precisely because whoever reads the
 * message has no way to know that in advance. Collapsing any two would tell whoever is holding the
 * outage only what they already knew. `DefaultBackupUploaderTest` asserts the ten as a property
 * rather than trusting ten expectations to disagree by accident; `SupabaseBackupObjectStoreTest`
 * owns the other three.
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

    override suspend fun upload(userId: String, fileName: String, payload: String) {
        // Encoded ONCE, and everything downstream reads this array: the digest in the manifest, the
        // bytes that travel, and the bytes the read-back is compared against. Re-encoding `payload`
        // for the upload would make the digest a claim about a String rather than about the file, and
        // a mismatch would then be indistinguishable from a corrupted transfer.
        val payloadBytes = payload.encodeToByteArray()
        val manifest = buildBackupManifest(fileName, payloadBytes)
        // Serialised OUTSIDE the upload's failure wrapper: a defect in the manifest's own encoding is
        // not a transport problem, and reporting it as "the manifest could not be uploaded" would
        // send whoever is reading that line to the network. `encodeAsDomainException` — shared with
        // `DefaultBackupRepository.exportToJson` — is what names it instead: without it this escaped
        // as a raw SerializationException, caught only by `BackupOrchestrator.runBackup`'s generic
        // branch and reported as `DomainException.Unknown`.
        val manifestBytes = encodeAsDomainException { manifest.encodeToJson() }.encodeToByteArray()

        // Resolved ONCE, and both keys are built from it. Two calls would be two reads of the session
        // and therefore two possible prefixes; what actually stops a split pair reaching the bucket is
        // the RLS `with check` on `storage.objects`, which refuses whichever key does not match the
        // live session — a backstop in the migration, not in this file. Resolving once is what makes
        // the pair consistent by construction instead of by the server's mercy, and it is also what
        // reports "nobody is signed in" before rather than after a blob exists.
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

    /**
     * The one session read this snapshot gets, refused unless the session is still [userId]'s.
     *
     * **Asserted, never substituted.** The prefix comes from the live session exactly as it always
     * did; [userId] only says which session the caller *decided* this snapshot was for. Resolving
     * without comparing is what let an account switch land one user's ledger under another's key —
     * with the bucket's RLS agreeing, because the key matched whoever was signed in by then.
     *
     * It is one step rather than two lines inside [upload] so that the resolution and its refusal
     * cannot drift apart, and so the "resolved exactly once per snapshot" property
     * [BackupObjectStore.ownedPrefix] asks for has one place to be read off. The comparison uses
     * [ownerPrefixOf], the single declaration of the layout — this file never respells it.
     */
    private suspend fun ownedPrefixFor(userId: String): String {
        val prefix: String = remotely(PREFIX_UNRESOLVED) { store.ownedPrefix() }
        val owner: String = ownerPrefixOf(userId)
        // Before the first byte, so a session that changed mid-cycle costs a read and leaves the
        // bucket exactly as it was.
        if (prefix != owner) throw ownerChanged(owner, prefix)
        return prefix
    }
}

/** Shared by the mismatch itself and by a failure to clean up after it — one event, two endings. */
private fun payloadMismatchAt(payloadKey: String): String =
    "the payload read back from $payloadKey does not match the digest its manifest states"

/** The same pair of endings for the sidecar. Byte equality, not a digest — see the class KDoc. */
private fun manifestMismatchAt(manifestKey: String): String =
    "the manifest read back from $manifestKey does not match the bytes that were uploaded"

/**
 * The account switched under a cycle that had already decided whose ledger this is.
 *
 * [DomainException.Unauthorized] rather than a `ValidationError`: nothing was validated and nothing
 * is wrong with the payload — the live session is simply not the one this operation was authorised
 * for. Both prefixes are named because "the session is someone else's" and "the session is gone" are
 * different outages, and only the text separates them here.
 */
private fun ownerChanged(owner: String, live: String): DomainException = DomainException.Unauthorized(
    "${FAILED}the signed-in account changed while the snapshot was being taken: it belongs under " +
        "$owner and the live session owns $live, so nothing was uploaded.",
)

/** Both mismatches are the same event to the person holding the phone: a backup that is not trustworthy. */
private fun unverified(reason: String): DomainException = DomainException.ValidationError(
    "$FAILED$reason",
    ValidationCode.BackupUploadUnverified,
)

private const val FAILED = "Snapshot backup failed: "

private const val PREFIX_UNRESOLVED = "the owning prefix could not be resolved"

/**
 * One storage call, reported as an upload failure that names [reason].
 *
 * The headline and the trailing full stop are added here rather than by [storageCall], which the
 * retention prune shares: the two operations fail for different reasons and must never borrow each
 * other's opening words. Every one of this file's nine messages is pinned by
 * `DefaultBackupUploaderTest`, so the assembly is not taken on trust.
 */
private suspend fun <T> remotely(reason: String, block: suspend () -> T): T = storageCall("$FAILED$reason.", block)
