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
 * Everything else here rides on the same fixture, including the four failure paths that must stay
 * four different messages — ADR 009 hard constraint 4, the reason the 2026-08-12 outage stayed
 * invisible as long as it did.
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
        store.readBackInstead = "{}".encodeToByteArray()

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
        }

        assertEquals(MISMATCH, failure.message)
        assertEquals(ValidationCode.BackupFileInvalid, failure.code)
        // The manifest is the receipt: it must not exist beside a payload that failed its check.
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY", "delete $PAYLOAD_KEY"), store.calls)
        assertEquals(emptyList(), store.objects.keys.toList())
    }

    @Test
    fun `a verified snapshot writes the payload first and its manifest second`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)

        // Order, not just membership: a manifest written before the read-back would mean "an upload
        // was attempted" instead of "these bytes were verified", and every later restore check that
        // trusts the manifest would be trusting a receipt written before the goods arrived.
        assertEquals(
            listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY", "upload $MANIFEST_KEY"),
            store.calls,
        )
        assertEquals(listOf(PAYLOAD_KEY, MANIFEST_KEY), store.objects.keys.toList())
    }

    @Test
    fun `both objects land under the owner prefix and both end in json`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)

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

        DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)

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
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
        }

        assertEquals(UPLOAD_FAILED, failure.message)
        assertEquals(listOf("upload $PAYLOAD_KEY"), store.calls)
    }

    @Test
    fun `a failed read-back leaves the object alone instead of guessing`() = runTest {
        val store = FakeBackupObjectStore()
        store.failDownload = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
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
        store.readBackInstead = "{}".encodeToByteArray()
        store.failDelete = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
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
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_UPLOAD_FAILED, failure.message)
    }

    @Test
    fun `no session stops the snapshot before anything is written`() = runTest {
        val store = FakeBackupObjectStore()
        // The real text and the real type are pinned in SupabaseBackupObjectStoreTest; what this one
        // owns is that the uploader lets it through untouched instead of rewrapping it as "the
        // payload could not be uploaded", and that it stops before a blob exists.
        store.failOwnedKey = DomainException.Unauthorized("nobody is signed in")

        val failure = assertFailsWith<DomainException.Unauthorized> {
            DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
        }

        assertEquals("nobody is signed in", failure.message)
        assertEquals(emptyList(), store.calls)
    }

    @Test
    fun `the five failures reachable through the seam say five different things`() = runTest {
        // Hard constraint 4, asserted as a property rather than five tests agreeing by accident.
        // Collapsing any two of these messages in production turns this red even if each individual
        // expectation above were edited to match.
        val messages: List<String?> = FAILURES.map { failure ->
            val store = FakeBackupObjectStore()
            failure.arrange(store)

            assertFailsWith<DomainException>(message = failure.label) {
                DefaultBackupUploader(store).upload(FILE_NAME, PAYLOAD)
            }.message
        }

        assertEquals(FAILURES.size, messages.toSet().size, "two failure paths share a message: $messages")
    }
}

private class Failure(val label: String, val arrange: (FakeBackupObjectStore) -> Unit)

/** One entry per way an upload can fail once the owner prefix has resolved. */
private val FAILURES: List<Failure> = listOf(
    Failure("payload upload") { it.failUpload = IllegalStateException("boom") },
    Failure("read-back") { it.failDownload = IllegalStateException("boom") },
    Failure("digest mismatch") { it.readBackInstead = "{}".encodeToByteArray() },
    Failure("cleanup after a mismatch") {
        it.readBackInstead = "{}".encodeToByteArray()
        it.failDelete = IllegalStateException("boom")
    },
    Failure("manifest upload") { it.failUploadOf = MANIFEST_KEY },
)

/**
 * An in-memory bucket that can be told to misbehave in exactly the ways a real one cannot be asked to.
 *
 * Hand-written rather than a MockK relaxed mock because two of its properties are the substance of
 * these tests and not incidental: [calls] records the ORDER, which is what "the manifest goes last"
 * means, and [readBackInstead] is the corrupted transfer that no real storage API will perform on
 * request. A failure is recorded in [calls] before it is thrown, so "it tried and failed" and "it
 * never got that far" stay distinguishable.
 */
private class FakeBackupObjectStore : BackupObjectStore {

    val objects: LinkedHashMap<String, ByteArray> = linkedMapOf()
    val calls: MutableList<String> = mutableListOf()

    /** Handed back by [download] in place of what is stored. */
    var readBackInstead: ByteArray? = null
    var failOwnedKey: Throwable? = null
    var failUpload: Throwable? = null

    /** Fails only the upload of this one key, so payload and manifest can fail independently. */
    var failUploadOf: String? = null
    var failDownload: Throwable? = null
    var failDelete: Throwable? = null

    override suspend fun ownedKey(fileName: String): String {
        failOwnedKey?.let { throw it }
        return "$UID/$fileName"
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
        return readBackInstead ?: objects.getValue(key)
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        failDelete?.let { throw it }
        objects -= key
    }
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"
private const val FILE_NAME = "backup-v3-2026-08-14T03-00-00.json"
private const val PAYLOAD_KEY = "$UID/$FILE_NAME"
private const val MANIFEST_KEY = "$PAYLOAD_KEY.manifest.json"

private const val FAILED = "Snapshot backup failed: "
private const val MISMATCHED = "the payload read back from $PAYLOAD_KEY does not match the digest its manifest states"

private const val UPLOAD_FAILED = "${FAILED}the payload could not be uploaded to $PAYLOAD_KEY."
private const val READ_BACK_FAILED = "${FAILED}the payload could not be read back from $PAYLOAD_KEY."
private const val MISMATCH = "$FAILED$MISMATCHED; the unverified object was deleted."
private const val CLEANUP_FAILED = "$FAILED$MISMATCHED, and the unverified object could not be deleted."
private const val MANIFEST_UPLOAD_FAILED = "${FAILED}the manifest could not be uploaded to $MANIFEST_KEY."

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
