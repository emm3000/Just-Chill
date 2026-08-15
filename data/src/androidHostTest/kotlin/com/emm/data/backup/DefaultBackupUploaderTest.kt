package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * ADR 009 Phase 2's own gate lives in this file: **an upload whose hash mismatches is not marked
 * successful**, proved on the JVM host suite with no network and no Supabase project.
 *
 * That proof is only possible because [BackupObjectStore] exists. A real bucket returns the bytes it
 * was handed, so a mismatch cannot be provoked against one; behind the seam it is a two-line stub.
 * Everything else here rides on the same fixture, including the ten failure paths that must stay
 * ten different messages — ADR 009 hard constraint 4, the reason the 2026-08-12 outage stayed
 * invisible as long as it did. The other three of the pipeline's thirteen come out of the real store
 * and belong to `SupabaseBackupObjectStoreTest`.
 *
 * The expected messages are written out as literals rather than read from production. That is
 * deliberate and is the same discipline `BackupManifestTest` follows: a test that builds its
 * expectation out of the code under test agrees with any regression it introduces.
 *
 * What this file cannot reach is the `<uid>/` prefix itself — [FakeBackupObjectStore] fabricates it.
 * `SupabaseBackupObjectStoreTest` pins the real one against a real client.
 */
class DefaultBackupUploaderTest {

    @Test
    fun `a payload that reads back wrong is refused, deleted, and never given a manifest`() = runTest {
        val store = FakeBackupObjectStore()
        // Stands in for a corrupted transfer: what comes back is not what went up. Nothing else in
        // the pipeline can tell that apart from a truncated or rewritten object, which is the point.
        store.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray()

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MISMATCH, failure.message)
        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
        // The manifest is the receipt: it must not exist beside a payload that failed its check.
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY", "delete $PAYLOAD_KEY"), store.calls)
        assertEquals(emptyList(), store.objects.keys.toList())
    }

    @Test
    fun `a verified snapshot writes the payload, its manifest, and reads both back`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

        // Order, not just membership: a manifest written before the payload's read-back would mean
        // "an upload was attempted" instead of "these bytes were verified", and every later restore
        // check that trusts the manifest would be trusting a receipt written before the goods
        // arrived. The manifest's own read-back is last because it is the receipt being checked, and
        // a receipt corrupted in transit condemns a payload that is perfectly intact.
        assertEquals(
            listOf(
                "upload $PAYLOAD_KEY",
                "download $PAYLOAD_KEY",
                "upload $MANIFEST_KEY",
                "download $MANIFEST_KEY",
            ),
            store.calls,
        )
        assertEquals(listOf(PAYLOAD_KEY, MANIFEST_KEY), store.objects.keys.toList())
    }

    @Test
    fun `the owning prefix is resolved once, not once per object`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

        // Two resolutions would be two reads of the session, so a sign-out landing between them puts
        // the manifest under a different prefix from the payload it describes. The bucket's RLS
        // `with check` refuses whichever key misses the live session, but that is a backstop in a
        // migration; the pair is meant to be consistent before the server ever sees it.
        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `both objects land under the owner prefix and both end in json`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

        // The prefix is what every RLS policy on the bucket is keyed on; the extension is what makes
        // storage-kt send a bare `application/json`, which is the only content type the bucket
        // accepts. A sidecar named `.manifest` would be refused with an HTTP 415 that reads like a
        // server fault — see SupabaseBackupObjectStore.
        store.objects.keys.forEach { key ->
            assertTrue(key.startsWith("$UID/"), "$key is not under the owner prefix")
            assertTrue(key.endsWith(".json"), "$key would not get a bare application/json")
        }
        assertEquals(MANIFEST_KEY, store.objects.keys.last())
    }

    @Test
    fun `the bytes that were hashed are the bytes that were stored`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

        val stored: ByteArray = store.objects.getValue(PAYLOAD_KEY)
        // Byte-for-byte the caller's document. Anything that re-serialised the payload between
        // hashing it and sending it — a reparse, a re-pretty-print, a charset — would still produce
        // a self-consistent manifest and a snapshot whose digest describes something else.
        assertEquals(PAYLOAD, stored.decodeToString())

        val manifest = Json.decodeFromString<BackupManifestDto>(store.objects.getValue(MANIFEST_KEY).decodeToString())
        assertEquals(sha256Hex(stored), manifest.payloadSha256)
        assertEquals(FILE_NAME, manifest.fileName)
    }

    @Test
    fun `a failed upload never reaches the read-back`() = runTest {
        val store = FakeBackupObjectStore()
        store.failUpload = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(UPLOAD_FAILED, failure.message)
        assertEquals(listOf("upload $PAYLOAD_KEY"), store.calls)
    }

    @Test
    fun `a failed read-back leaves the object alone instead of guessing`() = runTest {
        val store = FakeBackupObjectStore()
        store.failDownload = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(READ_BACK_FAILED, failure.message)
        // No delete, deliberately: whatever stopped the read would very likely stop the delete too,
        // and then the precise reason would be replaced by a vague one. The orphan it leaves carries
        // no manifest, so nothing may treat it as a snapshot.
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY"), store.calls)
        assertEquals(listOf(PAYLOAD_KEY), store.objects.keys.toList())
    }

    @Test
    fun `a cleanup that fails after a mismatch reports the cleanup, not the mismatch`() = runTest {
        val store = FakeBackupObjectStore()
        store.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray()
        store.failDelete = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        // Both facts survive in one message — the digest disagreed AND the bad object is still up
        // there. Reporting only the mismatch would leave an unverified blob nobody knows about.
        assertEquals(CLEANUP_FAILED, failure.message)
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY", "delete $PAYLOAD_KEY"), store.calls)
        assertEquals(listOf(PAYLOAD_KEY), store.objects.keys.toList())
    }

    @Test
    fun `a failed manifest upload is not reported as a failed payload upload`() = runTest {
        val store = FakeBackupObjectStore()
        store.failUploadOf = MANIFEST_KEY

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_UPLOAD_FAILED, failure.message)
    }

    @Test
    fun `a manifest that cannot be read back leaves both objects alone`() = runTest {
        val store = FakeBackupObjectStore()
        store.failDownloadOf = MANIFEST_KEY

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_READ_BACK_FAILED, failure.message)
        // Same reasoning as the payload's failed read-back: the transport is the suspect, so a delete
        // over it would replace a precise reason with a vague one. What is left is a verified payload
        // beside a manifest of unknown state, and the next cycle uploads a fresh pair under a fresh
        // timestamp rather than reasoning about this one.
        assertEquals(listOf(PAYLOAD_KEY, MANIFEST_KEY), store.objects.keys.toList())
    }

    @Test
    fun `a manifest that reads back wrong takes the payload down with it`() = runTest {
        val store = FakeBackupObjectStore()
        // A receipt corrupted in transit is the nastiest of these failures: the payload is intact and
        // the manifest now states a digest it will never match, so a later pre-restore check
        // condemns a snapshot that would have restored perfectly.
        store.readBackInstead[MANIFEST_KEY] = "{}".encodeToByteArray()

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_MISMATCH, failure.message)
        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
        // Manifest deleted BEFORE the payload: if the second delete failed, what survives is a
        // payload with no sidecar, which the retention rule already discards. The other order would
        // leave a receipt for goods that are gone.
        assertEquals(
            listOf(
                "upload $PAYLOAD_KEY",
                "download $PAYLOAD_KEY",
                "upload $MANIFEST_KEY",
                "download $MANIFEST_KEY",
                "delete $MANIFEST_KEY",
                "delete $PAYLOAD_KEY",
            ),
            store.calls,
        )
        assertEquals(emptyList(), store.objects.keys.toList())
    }

    @Test
    fun `a manifest mismatch cleanup that deletes the manifest but not the payload orphans it alone`() = runTest {
        val store = FakeBackupObjectStore()
        store.readBackInstead[MANIFEST_KEY] = "{}".encodeToByteArray()
        // The manifest delete (first in the cleanup) succeeds; only the payload delete fails. This is
        // orphan source five (`docs/sync/ADR009_PLAN.md`): a verified payload the manifest-mismatch
        // cleanup could only half-finish, indistinguishable on the wire from "the manifest upload
        // never happened" and handled identically by the retention prune.
        store.failDeleteOf = PAYLOAD_KEY

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_CLEANUP_FAILED, failure.message)
        // The point of this test: the payload survives ALONE. A pair that both failed to delete would
        // still be a "complete-looking pair" (a different, already-covered leftover); a payload with
        // no manifest beside it is what makes this orphan source five rather than a non-event.
        assertEquals(listOf(PAYLOAD_KEY), store.objects.keys.toList())
        assertEquals(
            listOf(
                "upload $PAYLOAD_KEY",
                "download $PAYLOAD_KEY",
                "upload $MANIFEST_KEY",
                "download $MANIFEST_KEY",
                "delete $MANIFEST_KEY",
                "delete $PAYLOAD_KEY",
            ),
            store.calls,
        )
    }

    @Test
    fun `no session stops the snapshot before anything is written`() = runTest {
        val store = FakeBackupObjectStore()
        // The real text and the real type are pinned in SupabaseBackupObjectStoreTest; what this one
        // owns is that the uploader lets it through untouched instead of rewrapping it as "the owning
        // prefix could not be resolved", and that it stops before a blob exists.
        store.failOwnedPrefix = DomainException.Unauthorized("nobody is signed in")

        val failure = assertFailsWith<DomainException.Unauthorized> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals("nobody is signed in", failure.message)
        assertEquals(emptyList(), store.calls)
    }

    @Test
    fun `anything else the prefix step throws is named rather than escaping raw`() = runTest {
        val store = FakeBackupObjectStore()
        store.failOwnedPrefix = IllegalStateException("the session store exploded")

        // The port's headline contract is that EVERY failure is a DomainException. Without the
        // wrapper this step is the one place a raw throwable leaves the pipeline unnamed.
        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(PREFIX_FAILED, failure.message)
        assertEquals(emptyList(), store.calls)
    }

    @Test
    fun `a snapshot for one account is refused when the session has become another's`() = runTest {
        val store = FakeBackupObjectStore()
        // The account switched between the cycle deciding whose ledger this is and this class asking
        // where it goes. Resolved rather than asserted, the prefix would simply BE the new owner's,
        // the bucket's RLS `with check` would accept the key because it matches the live session, and
        // one user's whole ledger would land in another's bucket — with the caller then recording a
        // success for a snapshot it can never see again. Nothing about that is visible server-side.
        store.owner = OTHER_UID

        val failure = assertFailsWith<DomainException.Unauthorized> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(OWNER_CHANGED, failure.message)
        // Refused before the first byte: one session read, and a bucket in exactly the state it was.
        assertEquals(emptyList(), store.calls)
        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `the ten failures reachable through the seam say ten different things`() = runTest {
        // Hard constraint 4, asserted as a property rather than ten tests agreeing by accident.
        // Collapsing any two of these messages in production turns this red even if each individual
        // expectation above were edited to match.
        val messages: List<String?> = FAILURES.map { failure ->
            val store = FakeBackupObjectStore()
            failure.arrange(store)

            assertFailsWith<DomainException>(message = failure.label) {
                DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
            }.message
        }

        assertEquals(FAILURES.size, messages.toSet().size, "two failure paths share a message: $messages")
    }
}

private class Failure(val label: String, val arrange: (FakeBackupObjectStore) -> Unit)

/** One entry per way an upload can fail, from resolving the prefix to discarding a broken pair. */
private val FAILURES: List<Failure> = listOf(
    Failure("prefix resolution") { it.failOwnedPrefix = IllegalStateException("boom") },
    Failure("the session became another account's") { it.owner = OTHER_UID },
    Failure("payload upload") { it.failUpload = IllegalStateException("boom") },
    Failure("payload read-back") { it.failDownload = IllegalStateException("boom") },
    Failure("payload digest mismatch") { it.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray() },
    Failure("cleanup after a payload mismatch") {
        it.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray()
        it.failDelete = IllegalStateException("boom")
    },
    Failure("manifest upload") { it.failUploadOf = MANIFEST_KEY },
    Failure("manifest read-back") { it.failDownloadOf = MANIFEST_KEY },
    Failure("manifest mismatch") { it.readBackInstead[MANIFEST_KEY] = "{}".encodeToByteArray() },
    Failure("cleanup after a manifest mismatch") {
        it.readBackInstead[MANIFEST_KEY] = "{}".encodeToByteArray()
        it.failDelete = IllegalStateException("boom")
    },
)

/**
 * An in-memory bucket that can be told to misbehave in exactly the ways a real one cannot be asked to.
 *
 * Hand-written rather than a MockK relaxed mock because two of its properties are the substance of
 * these tests and not incidental: [calls] records the ORDER, which is what "the manifest goes last"
 * means, and [readBackInstead] is the corrupted transfer that no real storage API will perform on
 * request. A failure is recorded in [calls] before it is thrown, so "it tried and failed" and "it
 * never got that far" stay distinguishable.
 *
 * Every misbehaviour is addressable by KEY, because the payload and its sidecar now travel the same
 * four operations and a fixture that broke both at once could not tell their failures apart.
 */
private class FakeBackupObjectStore : BackupObjectStore {

    val objects: LinkedHashMap<String, ByteArray> = linkedMapOf()
    val calls: MutableList<String> = mutableListOf()

    /** How many times the prefix was resolved — one snapshot must mean one read of the session. */
    var prefixResolutions: Int = 0
        private set

    /**
     * Whose session [ownedPrefix] answers for. Settable because an account switch landing mid-cycle
     * is the one thing a real store cannot be asked to perform: the uploader would have to be
     * suspended between resolving the prefix and being constructed, which is not a thing. Here it is
     * one assignment, and it is what makes the owner assertion falsifiable at all.
     */
    var owner: String = UID

    /** Handed back by [download] in place of what is stored, per key. */
    val readBackInstead: MutableMap<String, ByteArray> = mutableMapOf()
    var failOwnedPrefix: Throwable? = null
    var failUpload: Throwable? = null

    /** Fails only the upload of this one key, so payload and manifest can fail independently. */
    var failUploadOf: String? = null
    var failDownload: Throwable? = null

    /** The read-back counterpart of [failUploadOf]. */
    var failDownloadOf: String? = null
    var failDelete: Throwable? = null

    /**
     * Fails only the delete of this one key, so a partial pair-cleanup — one delete landing, the
     * other failing — is expressible. Without this, [failDelete] fails both deletes in the manifest
     * mismatch cleanup indiscriminately, and the state where the manifest is gone but the payload
     * survives alone (orphan source five, `docs/sync/ADR009_PLAN.md`) has no way to occur.
     */
    var failDeleteOf: String? = null

    override suspend fun ownedPrefix(): String {
        prefixResolutions++
        failOwnedPrefix?.let { throw it }
        return "$owner/"
    }

    override suspend fun upload(key: String, bytes: ByteArray) {
        calls += "upload $key"
        failUpload?.let { throw it }
        if (key == failUploadOf) throw IllegalStateException("boom")
        objects[key] = bytes
    }

    override suspend fun download(key: String): ByteArray {
        calls += "download $key"
        failDownload?.let { throw it }
        if (key == failDownloadOf) throw IllegalStateException("boom")
        return readBackInstead[key] ?: objects.getValue(key)
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        failDelete?.let { throw it }
        if (key == failDeleteOf) throw IllegalStateException("boom")
        objects -= key
    }

    /**
     * Answers honestly so a stray call would show up, but nothing here lists anything: [list] belongs
     * to the retention prune, and `DefaultBackupPrunerTest` owns the fixture that exercises it.
     */
    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        calls += "list $prefix from $offset"
        val page = objects.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
            .drop(offset)
            .take(limit)
        return ObjectPage(names = page, serverReturned = page.size)
    }
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"

/** The account the session switches to mid-cycle. A real uid shape, because the prefix is one. */
private const val OTHER_UID = "5f1a2b3c-0000-4000-8000-000000000002"

/**
 * A REAL snapshot name — one `backupSnapshotName` writes and `parseBackupSnapshotTakenAt` reads back.
 *
 * It used to be missing its `Z`, which made it a name the prune classifies as *not a snapshot*
 * (`BackupSnapshotNameTest` pins that exact shape as a non-snapshot). Nothing here failed, because
 * the uploader never parses what it is handed — but this is the more-read of the two files, so what
 * it showed a reader was a format the rest of the pipeline rejects. `BackupNameSeamTest` is what now
 * holds the two ends together.
 */
private const val FILE_NAME = "backup-v3-2026-08-14T03-00-00Z.json"
private const val PAYLOAD_KEY = "$UID/$FILE_NAME"
private const val MANIFEST_KEY = "$PAYLOAD_KEY.manifest.json"

private const val FAILED = "Snapshot backup failed: "
private const val MISMATCHED = "the payload read back from $PAYLOAD_KEY does not match the digest its manifest states"
private const val MANIFEST_MISMATCHED =
    "the manifest read back from $MANIFEST_KEY does not match the bytes that were uploaded"

private const val PREFIX_FAILED = "${FAILED}the owning prefix could not be resolved."
private const val OWNER_CHANGED =
    "${FAILED}the signed-in account changed while the snapshot was being taken: it belongs under " +
        "$UID/ and the live session owns $OTHER_UID/, so nothing was uploaded."
private const val UPLOAD_FAILED = "${FAILED}the payload could not be uploaded to $PAYLOAD_KEY."
private const val READ_BACK_FAILED = "${FAILED}the payload could not be read back from $PAYLOAD_KEY."
private const val MISMATCH = "$FAILED$MISMATCHED; the unverified object was deleted."
private const val CLEANUP_FAILED = "$FAILED$MISMATCHED, and the unverified object could not be deleted."
private const val MANIFEST_UPLOAD_FAILED = "${FAILED}the manifest could not be uploaded to $MANIFEST_KEY."
private const val MANIFEST_READ_BACK_FAILED = "${FAILED}the manifest could not be read back from $MANIFEST_KEY."
private const val MANIFEST_MISMATCH = "$FAILED$MANIFEST_MISMATCHED; both objects were deleted."
private const val MANIFEST_CLEANUP_FAILED = "$FAILED$MANIFEST_MISMATCHED, and the pair could not be deleted."

/**
 * A real snapshot document, small but complete: [buildBackupManifest] decodes it STRICTLY, so an
 * unknown or missing key here fails the manifest before the upload is ever reached.
 *
 * The account name carries accents on purpose — the digest is over UTF-8 bytes, and an ASCII-only
 * fixture would let a one-byte-per-character regression through unnoticed.
 */
private val PAYLOAD: String = """
    {
      "schemaVersion": $BACKUP_SCHEMA_VERSION,
      "exportedAt": 1755000000000,
      "appVersion": "v2.4.0",
      "accounts": [
        { "accountId": "acc-1", "name": "Ahorro año", "type": "Bank", "currency": "PEN" }
      ],
      "categories": [],
      "transactions": [],
      "recurringMovements": []
    }
""".trimIndent()
