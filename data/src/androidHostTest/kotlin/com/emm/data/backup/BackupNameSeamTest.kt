package com.emm.data.backup

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class BackupNameSeamTest {

    @Test
    fun `the pair the uploader writes is a pair the prune keeps`() = runTest {
        val bucket = InMemoryBucket()

        DefaultBackupUploader(bucket).upload(UID, backupSnapshotName(TAKEN_AT), PAYLOAD)
        val report = prunerOver(bucket).prune()

        assertEquals(1, report.kept)
        assertEquals(0, report.deleted)
        assertEquals(
            listOf(PREFIX + backupSnapshotName(TAKEN_AT), PREFIX + manifestNameFor(backupSnapshotName(TAKEN_AT))),
            bucket.objects.keys.toList(),
        )
    }

    @Test
    fun `and it is the sidecar that makes it one, not the payload name alone`() = runTest {
        val bucket = InMemoryBucket()
        DefaultBackupUploader(bucket).upload(UID, backupSnapshotName(TAKEN_AT), PAYLOAD)
        bucket.objects -= PREFIX + manifestNameFor(backupSnapshotName(TAKEN_AT))

        val report = prunerOver(bucket).prune()

        assertEquals(0, report.kept)
        assertEquals(1, report.deleted)
        assertEquals(emptyList(), bucket.objects.keys.toList())
    }
}

private fun prunerOver(bucket: InMemoryBucket): DefaultBackupPruner = DefaultBackupPruner(bucket, SeamClock, LIMA)

private class InMemoryBucket : BackupObjectStore {

    val objects: LinkedHashMap<String, ByteArray> = linkedMapOf()

    override suspend fun ownedPrefix(): String = PREFIX

    override suspend fun upload(key: String, bytes: ByteArray) {
        objects[key] = bytes
    }

    override suspend fun download(key: String): ByteArray = objects.getValue(key)

    override suspend fun delete(key: String) {
        objects -= key
    }

    override suspend fun list(prefix: String, limit: Int, offset: Int): ObjectPage {
        val page = objects.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
            .drop(offset)
            .take(limit)
        return ObjectPage(names = page, serverReturned = page.size)
    }
}

private object SeamClock : Clock {
    override fun now(): Instant = AFTERWARDS
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000002"

private const val PREFIX = "$UID/"

private val PAYLOAD: String = """
    {
      "schemaVersion": $BACKUP_SCHEMA_VERSION,
      "exportedAt": 1755000000000,
      "appVersion": "v2.4.0",
      "accounts": [],
      "categories": [],
      "transactions": [],
      "recurringMovements": []
    }
""".trimIndent()

private val LIMA: TimeZone = TimeZone.of("America/Lima")

private val TAKEN_AT: Instant = Instant.parse("2026-08-14T03:04:05Z")

private val AFTERWARDS: Instant = Instant.parse("2026-08-15T00:00:00Z")
