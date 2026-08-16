package com.emm.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BackupSnapshotNameTest {

    @Test
    fun `a snapshot name is the schema version and a UTC stamp with its colons replaced`() {
        val takenAt = Instant.parse("2026-08-14T03:04:05Z")

        assertEquals("backup-v3-2026-08-14T03-04-05Z.json", backupSnapshotName(takenAt))
    }

    @Test
    fun `every name the builder writes is a name the parser reads back`() {
        val instants = listOf(
            "2026-01-01T00:00:00Z",
            "2028-02-29T23:59:59Z",
            "2026-12-31T23:59:59Z",
            "2026-08-14T15:30:09Z",
        ).map(Instant::parse)

        instants.forEach { takenAt ->
            assertEquals(takenAt, parseBackupSnapshotTakenAt(backupSnapshotName(takenAt)), "$takenAt")
        }
    }

    @Test
    fun `sub-second precision is dropped so one snapshot is always one fixed-width name`() {
        val name = backupSnapshotName(Instant.parse("2026-08-14T03:04:05.678Z"))

        assertEquals("backup-v3-2026-08-14T03-04-05Z.json", name)
        assertEquals(Instant.parse("2026-08-14T03:04:05Z"), parseBackupSnapshotTakenAt(name))
    }

    @Test
    fun `a name from a future format version is not a v3 snapshot`() {
        assertNull(parseBackupSnapshotTakenAt("backup-v4-2026-08-14T03-04-05Z.json"))
        assertNull(parseBackupSnapshotTakenAt("backup-v2-2026-08-14T03-04-05Z.json"))
    }

    @Test
    fun `the sidecar suffix appends, so a manifest never parses as the snapshot it describes`() {
        val name = backupSnapshotName(Instant.parse("2026-08-14T03:04:05Z"))
        val sidecar = manifestNameFor(name)

        assertEquals(".manifest.json", BACKUP_MANIFEST_SUFFIX)
        assertEquals("backup-v3-2026-08-14T03-04-05Z.json.manifest.json", sidecar)
        assertNull(parseBackupSnapshotTakenAt(sidecar))
        assertEquals(name, sidecar.removeSuffix(BACKUP_MANIFEST_SUFFIX))
        assertTrue(sidecar.endsWith(".json"), "the bucket only accepts application/json")
    }

    @Test
    fun `anything that is not a snapshot name reads as null instead of throwing`() {
        val notSnapshots = listOf(
            "",
            "pinned",
            "pinned/backup-v3-2026-08-14T03-04-05Z.json",
            "backup-v3-2026-08-14T03-04-05Z.json.bak",
            "backup-v3-2026-08-14T03-04-05.json",
            "backup-v3-2026-08-14T03:04:05Z.json",
            "backup-v3-2026-8-14T03-04-05Z.json",
            "justchill-backup-2026-08-14.json",
            "backup-v3-2026-13-40T03-04-05Z.json",
            "backup-v3-2026-02-30T03-04-05Z.json",
            "backup-v3-2026-08-14T25-04-05Z.json",
        )

        notSnapshots.forEach { name -> assertNull(parseBackupSnapshotTakenAt(name), name) }
    }
}
