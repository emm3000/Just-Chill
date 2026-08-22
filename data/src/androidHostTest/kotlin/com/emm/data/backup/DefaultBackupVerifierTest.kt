package com.emm.data.backup

import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class DefaultBackupVerifierTest {

    private val bucket = FakeVerifiableBucket()

    private fun verifier(): DefaultBackupVerifier = DefaultBackupVerifier(bucket)

    @Test
    fun `the newest pair verifies and names the file it read`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seedPair(NEWEST, payloadJson())

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(NEWEST, verified.fileName)
        assertTrue(verified.isNewestPair, "the newest pair verified, so nothing was walked back")
    }

    @Test
    fun `a manifest that is not readable JSON walks back to the older pair, and says that it did`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seed(NEWEST, payloadJson(), manifest = "{ this is not a manifest")

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
        assertFalse(verified.isNewestPair, "reporting a walked-back snapshot as the newest one is a lie")
    }

    @Test
    fun `a manifest of an unknown manifestVersion is not trusted, and the walk back skips its pair`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        val payload = payloadJson()
        bucket.seed(NEWEST, payload, manifest = manifestJson(NEWEST, FUTURE_MANIFEST_VERSION, digestOf(payload)))

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
    }

    @Test
    fun `a payload whose bytes do not match the digest its manifest states walks back`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seed(NEWEST, payloadJson(), manifest = manifestJson(NEWEST, BACKUP_MANIFEST_VERSION, WRONG_DIGEST))

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
    }

    @Test
    fun `a payload that is not a decodable snapshot walks back`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seedPair(NEWEST, "{ \"schemaVersion\": 3 }")

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
    }

    @Test
    fun `a payload with no manifest is skipped, and is never deleted`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.objects[PREFIX + NEWEST] = payloadJson().encodeToByteArray()

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
        assertTrue(PREFIX + NEWEST in bucket.objects, "an orphan payload is the prune's business, not the check's")
    }

    @Test
    fun `an orphan newest payload leaves the pair that verifies a walked-back one, never the newest`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.objects[PREFIX + NEWEST] = payloadJson().encodeToByteArray()

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(OLDER, verified.fileName)
        assertFalse(
            verified.isNewestPair,
            "$NEWEST uploaded and its manifest never landed, so nothing restorable is the newest snapshot",
        )
    }

    @Test
    fun `an empty bucket has no snapshots, which is not the same as none of them verifying`() = runTest {
        assertEquals(BackupVerification.NoSnapshots, verifier().verifyLatest())
    }

    @Test
    fun `a bucket holding only orphan payloads has no snapshots either`() = runTest {
        bucket.objects[PREFIX + NEWEST] = payloadJson().encodeToByteArray()

        assertEquals(BackupVerification.NoSnapshots, verifier().verifyLatest())
    }

    @Test
    fun `when nothing verifies, the answer says how many pairs were inspected`() = runTest {
        listOf(OLDEST, OLDER, NEWEST).forEach { bucket.seed(it, payloadJson(), manifest = "{ broken") }

        assertEquals(BackupVerification.NothingVerified(pairsInspected = 3), verifier().verifyLatest())
    }

    @Test
    fun `the walk back stops at its bound instead of downloading the whole shelf`() = runTest {
        val brokenPairs = 7
        repeat(brokenPairs) { day ->
            bucket.seed(snapshotOnDay(day), payloadJson(), manifest = "{ broken")
        }

        val result = verifier().verifyLatest()

        assertEquals(BackupVerification.NothingVerified(pairsInspected = BACKUP_VERIFY_MAX_PAIRS), result)
        assertEquals(
            BACKUP_VERIFY_MAX_PAIRS,
            bucket.calls.count { it.startsWith("download ") },
            "one download per inspected manifest and not one more: $brokenPairs pairs were reachable",
        )
    }

    @Test
    fun `a bucket that cannot be listed is an error, not a bucket where nothing verified`() = runTest {
        bucket.seedPair(NEWEST, payloadJson())
        bucket.failList = IllegalStateException("the socket died mid-listing")

        val failure = assertFailsWith<DomainException.Unknown> { verifier().verifyLatest() }

        assertEquals("Snapshot backup verification failed: the bucket could not be listed.", failure.message)
    }

    @Test
    fun `an object that cannot be downloaded is an error, not a pair that failed to verify`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seedPair(NEWEST, payloadJson())
        bucket.failDownloadOf = PREFIX + manifestNameFor(NEWEST)

        val failure = assertFailsWith<DomainException.Unknown> { verifier().verifyLatest() }

        assertTrue(
            failure.message.orEmpty().endsWith("${manifestNameFor(NEWEST)} could not be downloaded."),
            failure.message.orEmpty(),
        )
    }

    @Test
    fun `the reported counts are the payload's own, not the ones its manifest states`() = runTest {
        bucket.seedPair(NEWEST, payloadJson(accounts = 1, categories = 23, transactions = 412, recurring = 3))

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(
            BackupRowCounts(accounts = 1, categories = 23, transactions = 412, recurringMovements = 3),
            verified.rowCounts,
        )
    }

    @Test
    fun `a payload declaring an older schema version verifies, and its counts are that file's`() = runTest {
        bucket.seedPair(NEWEST, payloadJson(recurring = 3, schemaVersion = BACKUP_SCHEMA_VERSION_V2))

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(
            0,
            verified.rowCounts.recurringMovements,
            "a v2 file carries no templates, so a restore of it would put none back",
        )
    }

    @Test
    fun `a snapshot named by an older build is still found, so a bump is not an empty bucket`() = runTest {
        val legacy: String = NEWEST.asEarlierGeneration()
        bucket.seedPair(legacy, payloadJson(accounts = 1, schemaVersion = BACKUP_SCHEMA_VERSION_V2))

        val verified = assertIs<BackupVerification.Verified>(verifier().verifyLatest())

        assertEquals(legacy, verified.fileName)
        assertEquals(1, verified.rowCounts.accounts)
        assertTrue(verified.isNewestPair, "it is the only pair in the bucket")
    }

    @Test
    fun `verification writes nothing to the bucket`() = runTest {
        bucket.seedPair(OLDER, payloadJson())
        bucket.seed(NEWEST, payloadJson(), manifest = "{ broken")
        bucket.objects[PREFIX + OLDEST] = payloadJson().encodeToByteArray()
        val before: List<String> = bucket.objects.keys.toList()

        verifier().verifyLatest()

        assertTrue(bucket.calls.none { it.startsWith("delete ") || it.startsWith("upload ") }, "${bucket.calls}")
        assertEquals(before, bucket.objects.keys.toList())
    }
}

private class FakeVerifiableBucket : BackupObjectStore {

    val objects: LinkedHashMap<String, ByteArray> = linkedMapOf()
    val calls: MutableList<String> = mutableListOf()

    var failList: Throwable? = null
    var failDownloadOf: String? = null

    override suspend fun ownedPrefix(): String = PREFIX

    override suspend fun upload(key: String, bytes: ByteArray) {
        calls += "upload $key"
        objects[key] = bytes
    }

    override suspend fun download(key: String): ByteArray {
        calls += "download $key"
        if (key == failDownloadOf) throw IllegalStateException("the socket died")
        return objects.getValue(key)
    }

    override suspend fun delete(key: String) {
        calls += "delete $key"
        objects -= key
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        calls += "list $prefix from $offset"
        failList?.let { throw it }
        val page: List<String> = objects.keys
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .drop(offset)
            .take(limit)
        return ObjectPage(names = page, serverReturned = page.size)
    }
}

private fun FakeVerifiableBucket.seedPair(fileName: String, payload: String) {
    seed(fileName, payload, manifest = manifestJson(fileName, BACKUP_MANIFEST_VERSION, digestOf(payload)))
}

private fun FakeVerifiableBucket.seed(fileName: String, payload: String, manifest: String) {
    objects[PREFIX + fileName] = payload.encodeToByteArray()
    objects[PREFIX + manifestNameFor(fileName)] = manifest.encodeToByteArray()
}

// payloadSchemaVersion and rowCounts are deliberately wrong in every fixture: the verifier must read
// both from the payload, and a manifest that agreed with it could not tell the two apart.
private fun manifestJson(fileName: String, manifestVersion: Int, digest: String): String = """
    {
      "manifestVersion": $manifestVersion,
      "fileName": "$fileName",
      "payloadSha256": "$digest",
      "payloadSchemaVersion": 1,
      "rowCounts": { "accounts": 0, "categories": 0, "transactions": 0, "recurringMovements": 0, "loans": 0, "loanPayments": 0 }
    }
""".trimIndent()

private fun digestOf(payload: String): String = sha256Hex(payload.encodeToByteArray())

private fun payloadJson(
    accounts: Int = 0,
    categories: Int = 0,
    transactions: Int = 0,
    recurring: Int = 0,
    schemaVersion: Int = BACKUP_SCHEMA_VERSION,
): String = fixtureJson.encodeToString(
    ExportPayloadDto(
        schemaVersion = schemaVersion,
        exportedAt = 1_755_000_000_000L,
        appVersion = "v2.4.0",
        accounts = List(accounts) { account(it) },
        categories = List(categories) { category(it) },
        transactions = List(transactions) { transaction(it) },
        recurringMovements = List(recurring) { recurringMovement(it) },
        loans = emptyList(),
        loanPayments = emptyList(),
    ),
)

private fun account(index: Int) = AccountDto(accountId = "account-$index", name = "Cuenta $index", type = "Cash")

private fun category(index: Int) = CategoryDto(
    categoryId = "category-$index",
    name = "Categoría $index",
    icon = "icon",
    color = "color",
    categoryType = "Spend",
)

private fun transaction(index: Int) = TransactionDto(
    transactionId = "transaction-$index",
    type = "Spend",
    amountCents = 1_000L,
    description = "Movimiento $index",
    occurredAt = "2026-08-16T10:00:00",
    accountId = "account-0",
    categoryId = null,
)

private fun recurringMovement(index: Int) = RecurringMovementDto(
    recurringMovementId = "recurring-$index",
    name = "Plantilla $index",
    type = "Spend",
    amountCents = 1_000L,
    description = "Plantilla $index",
    categoryId = null,
    accountId = "account-0",
    frequency = "Monthly",
    dayOfMonth = 1,
    isActive = true,
    lastConfirmedPeriod = null,
    createdAt = 0L,
)

private fun snapshotOnDay(day: Int): String = backupSnapshotName(Instant.parse("2026-08-0${day + 1}T14:22:08Z"))

private val fixtureJson = Json { encodeDefaults = true }

private const val UID = "5f1a2b3c-0000-4000-8000-000000000002"

private const val PREFIX = "$UID/"

private const val FUTURE_MANIFEST_VERSION = BACKUP_MANIFEST_VERSION + 1

private const val WRONG_DIGEST = "0000000000000000000000000000000000000000000000000000000000000000"

private val TAKEN_AT_NEWEST: Instant = Instant.parse("2026-08-16T14:22:08Z")

private val TAKEN_AT_OLDER: Instant = Instant.parse("2026-08-15T14:22:08Z")

private val TAKEN_AT_OLDEST: Instant = Instant.parse("2026-08-14T14:22:08Z")

private val NEWEST: String = backupSnapshotName(TAKEN_AT_NEWEST)

private val OLDER: String = backupSnapshotName(TAKEN_AT_OLDER)

private val OLDEST: String = backupSnapshotName(TAKEN_AT_OLDEST)
