# E01-21 — Snapshot names stop parsing when the schema version bumps

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] a snapshot written under an older `BACKUP_SCHEMA_VERSION` is still recognised as a snapshot,
  so retention and verification keep seeing it after the constant moves
- [x] a test fails if a future bump makes the already-uploaded generation invisible again

## Context

`SNAPSHOT_PREFIX` (`BackupSnapshotName.kt:28`) interpolates the **live** `BACKUP_SCHEMA_VERSION` and
`SNAPSHOT_NAME` matches on it, so the day v4 lands every `backup-v3-…json` in the bucket stops
parsing. `DefaultBackupPruner` then neither counts nor deletes it — and never calls it an orphan
either, since its sidecar is present — while `DefaultBackupVerifier` reports `NoSnapshots` over a
bucket full of restorable ones. Found while writing E01-02 and deliberately left alone there.
