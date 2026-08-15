package com.emm.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * The builder and the parser are tested together because they are one decision.
 *
 * ADR 009 Phase 2c's retention prune has nothing but names to sort a bucket on, so a generator and a
 * reader that disagree do not fail loudly — the prune quietly stops recognising this app's own
 * snapshots and every one of them becomes an object it leaves alone forever. Round-tripping is the
 * only thing that can say they agree.
 *
 * The expected literals are written out rather than assembled from the production constants, the
 * same discipline `BackupManifestTest` follows: a test that builds its expectation out of the code
 * under test agrees with any regression that code introduces. The one constant read from production
 * is [BACKUP_MANIFEST_SUFFIX], and it is read to prove the two ends of the *pair* agree, never to
 * decide what the pair should be — its literal value is pinned here as well.
 */
class BackupSnapshotNameTest {

    @Test
    fun `a snapshot name is the schema version and a UTC stamp with its colons replaced`() {
        val takenAt = Instant.parse("2026-08-14T03:04:05Z")

        assertEquals("backup-v3-2026-08-14T03-04-05Z.json", backupSnapshotName(takenAt))
    }

    @Test
    fun `every name the builder writes is a name the parser reads back`() {
        // Midnight, a leap day, a year boundary and an ordinary afternoon: the four places a
        // zero-padding or a length assumption breaks.
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
        // Not merely shorter — the round trip must land on the truncated instant, or the prune would
        // read a timestamp the name never carried.
        assertEquals(Instant.parse("2026-08-14T03:04:05Z"), parseBackupSnapshotTakenAt(name))
    }

    @Test
    fun `a name from a future format version is not a v3 snapshot`() {
        // The bucket outlives any single build, so a payload written by a later version WILL be
        // sitting there. It must read as "not a snapshot I know", which is what keeps the prune from
        // giving it a retention slot — and, far more importantly, from deleting it as an orphan.
        assertNull(parseBackupSnapshotTakenAt("backup-v4-2026-08-14T03-04-05Z.json"))
        assertNull(parseBackupSnapshotTakenAt("backup-v2-2026-08-14T03-04-05Z.json"))
    }

    @Test
    fun `the sidecar suffix appends, so a manifest never parses as the snapshot it describes`() {
        val name = backupSnapshotName(Instant.parse("2026-08-14T03:04:05Z"))
        val sidecar = manifestNameFor(name)

        assertEquals(".manifest.json", BACKUP_MANIFEST_SUFFIX)
        assertEquals("backup-v3-2026-08-14T03-04-05Z.json.manifest.json", sidecar)
        // Both halves of what the prune relies on: a sidecar is never mistaken for a payload, and
        // removing the suffix gets back the exact payload name it sits beside. A suffix that drifted
        // from what the uploader appends would make every sidecar look like an orphan, and the prune
        // deletes orphans on sight.
        assertNull(parseBackupSnapshotTakenAt(sidecar))
        assertEquals(name, sidecar.removeSuffix(BACKUP_MANIFEST_SUFFIX))
        assertTrue(sidecar.endsWith(".json"), "the bucket only accepts application/json")
    }

    @Test
    fun `anything that is not a snapshot name reads as null instead of throwing`() {
        // A bucket listing holds whatever anyone ever put there. Every one of these must come back
        // null: null means "not a snapshot", and only a snapshot can ever become a delete target.
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
