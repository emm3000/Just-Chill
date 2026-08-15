package com.emm.data.backup

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The name the uploader is given, the sidecar it derives from it, and the pair the prune recognises —
 * pinned as one thing, because they are one thing.
 *
 * **Nothing else proves this end to end.** `BackupSnapshotNameTest` says the builder and the parser
 * agree; `DefaultBackupUploaderTest` says the uploader writes two objects; `DefaultBackupPrunerTest`
 * says a complete pair survives. All three can be green while the pipeline is broken, because each
 * one invents its own names — and one of them did: the uploader suite's file-name fixture was missing
 * its `Z`, so it pinned a shape the prune classifies as *not a snapshot*, in the same package as a
 * test asserting exactly that. Two suites disagreeing about the format is a defect nobody can see
 * from either file.
 *
 * So this runs the real builder into the real uploader and the real prune, over one in-memory bucket.
 * The strong assertion is not that the strings match a literal — it is that **the prune keeps what
 * the uploader wrote**, which is only true if the name is parseable, the sidecar suffix is what the
 * prune strips, and the prefix arithmetic agrees on both sides. The control at the end is what stops
 * that from being vacuous.
 */
class BackupNameSeamTest {

    @Test
    fun `the pair the uploader writes is a pair the prune keeps`() = runTest {
        val bucket = InMemoryBucket()

        DefaultBackupUploader(bucket).upload(backupSnapshotName(TAKEN_AT), PAYLOAD)
        val report = prunerOver(bucket).prune()

        // Both objects still there, and the payload counted as a verified snapshot rather than as an
        // orphan — an orphan would have been deleted on sight, which is what a name the prune cannot
        // parse, or a sidecar suffix it does not recognise, would produce.
        assertEquals(1, report.kept)
        assertEquals(0, report.deleted)
        assertEquals(
            listOf(PREFIX + backupSnapshotName(TAKEN_AT), PREFIX + manifestNameFor(backupSnapshotName(TAKEN_AT))),
            bucket.objects.keys.toList(),
        )
    }

    @Test
    fun `and it is the sidecar that makes it one, not the payload name alone`() = runTest {
        // The control. Without it the test above would pass just as happily against a prune that
        // deleted nothing at all, or one that recognised no name in the bucket as a snapshot.
        val bucket = InMemoryBucket()
        DefaultBackupUploader(bucket).upload(backupSnapshotName(TAKEN_AT), PAYLOAD)
        bucket.objects -= PREFIX + manifestNameFor(backupSnapshotName(TAKEN_AT))

        val report = prunerOver(bucket).prune()

        assertEquals(0, report.kept)
        assertEquals(1, report.deleted)
        assertEquals(emptyList(), bucket.objects.keys.toList())
    }
}

private fun prunerOver(bucket: InMemoryBucket): DefaultBackupPruner = DefaultBackupPruner(bucket, SeamClock, LIMA)

/**
 * One bucket both halves of the pipeline talk to, which is the whole point: an uploader fixture and a
 * prune fixture that each invent their own names cannot disagree, and that is how the two suites in
 * this package came to disagree.
 */
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

/** Reads the same instant every time; a bucket boundary can only move when a test moves it. */
private object SeamClock : Clock {
    override fun now(): Instant = AFTERWARDS
}

private const val UID = "5f1a2b3c-0000-4000-8000-000000000002"

private const val PREFIX = "$UID/"

/** A complete snapshot document: `buildBackupManifest` decodes it strictly before anything uploads. */
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

/** After the snapshot, so it is an ordinary candidate rather than one on the future shelf. */
private val AFTERWARDS: Instant = Instant.parse("2026-08-15T00:00:00Z")
