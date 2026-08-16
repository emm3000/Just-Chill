package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultBackupUploaderTest {

    @Test
    fun `a payload that reads back wrong is refused, deleted, and never given a manifest`() = runTest {
        val store = FakeBackupObjectStore()
        store.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray()

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MISMATCH, failure.message)
        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY", "delete $PAYLOAD_KEY"), store.calls)
        assertEquals(emptyList(), store.objects.keys.toList())
    }

    @Test
    fun `a verified snapshot writes the payload, its manifest, and reads both back`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

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

        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `both objects land under the owner prefix and both end in json`() = runTest {
        val store = FakeBackupObjectStore()

        DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)

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
        assertEquals(listOf("upload $PAYLOAD_KEY", "download $PAYLOAD_KEY"), store.calls)
        assertEquals(listOf(PAYLOAD_KEY), store.objects.keys.toList())
    }

    @Test
    fun `a cleanup that fails after a mismatch is still typed as unverified, and says both`() = runTest {
        val store = FakeBackupObjectStore()
        store.readBackInstead[PAYLOAD_KEY] = "{}".encodeToByteArray()
        store.failDelete = IllegalStateException("the socket died")

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
        assertEquals("the socket died", failure.cause?.message)
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
        assertEquals(listOf(PAYLOAD_KEY, MANIFEST_KEY), store.objects.keys.toList())
    }

    @Test
    fun `a manifest that reads back wrong takes the payload down with it`() = runTest {
        val store = FakeBackupObjectStore()
        store.readBackInstead[MANIFEST_KEY] = "{}".encodeToByteArray()

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(MANIFEST_MISMATCH, failure.message)
        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
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
        store.failDeleteOf = PAYLOAD_KEY

        val failure = assertFailsWith<DomainException.ValidationError> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(ValidationCode.BackupUploadUnverified, failure.code)
        assertEquals(MANIFEST_CLEANUP_FAILED, failure.message)
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

        val failure = assertFailsWith<DomainException.Unknown> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(PREFIX_FAILED, failure.message)
        assertEquals(emptyList(), store.calls)
    }

    @Test
    fun `a snapshot for one account is refused when the session has become another's`() = runTest {
        val store = FakeBackupObjectStore()
        store.owner = OTHER_UID

        val failure = assertFailsWith<DomainException.Unauthorized> {
            DefaultBackupUploader(store).upload(UID, FILE_NAME, PAYLOAD)
        }

        assertEquals(OWNER_CHANGED, failure.message)
        assertEquals(emptyList(), store.calls)
        assertEquals(1, store.prefixResolutions)
    }

    @Test
    fun `a payload buildBackupManifest cannot decode surfaces as SerializationError, not BackupFileInvalid`() =
        runTest {
            val store = FakeBackupObjectStore()

            val failure = assertFailsWith<DomainException.SerializationError> {
                DefaultBackupUploader(store).upload(UID, FILE_NAME, MALFORMED_PAYLOAD)
            }

            assertEquals(emptyList(), store.calls)
            assertTrue(failure.cause is SerializationException)
        }

    @Test
    fun `the ten failures reachable through the seam say ten different things`() = runTest {
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

private class FakeBackupObjectStore : BackupObjectStore {

    val objects: LinkedHashMap<String, ByteArray> = linkedMapOf()
    val calls: MutableList<String> = mutableListOf()

    var prefixResolutions: Int = 0
        private set

    var owner: String = UID

    val readBackInstead: MutableMap<String, ByteArray> = mutableMapOf()
    var failOwnedPrefix: Throwable? = null
    var failUpload: Throwable? = null

    var failUploadOf: String? = null
    var failDownload: Throwable? = null

    var failDownloadOf: String? = null
    var failDelete: Throwable? = null

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

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        calls += "list $prefix from $offset"
        val page = objects.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
            .drop(offset)
            .take(limit)
        return ObjectPage(names = page, serverReturned = page.size)
    }
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000001"

private const val OTHER_UID = "5f1a2b3c-0000-4000-8000-000000000002"

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

private val MALFORMED_PAYLOAD: String = """
    {
      "schemaVersion": $BACKUP_SCHEMA_VERSION,
      "exportedAt": 1755000000000,
      "appVersion": "v2.4.0",
      "accounts": "not-a-list",
      "categories": [],
      "transactions": [],
      "recurringMovements": []
    }
""".trimIndent()
