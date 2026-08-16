# E01-01 — Pin the export→import round trip field by field

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] `BackupRoundTripTest` asserts the wipe-first export→import composition field by field, not just row counts
- [x] a second composition covers the restore UPDATE path without a physical wipe — export, stale the live rows, import without wiping, assert the same fields
- [x] `ImportStats` is built with named arguments so swapping two tables can no longer pass
- [x] the fixture's paused recurring template is an Income row so the type assertion can fail

## Context

`importFromJson` never deletes physically, so on a device already holding a row the INSERT is
ignored on PK conflict and `restoreFromBackup` is the only statement that acts. Landed `9b95dae2`
(wipe-first) and `e70006f6` (restore UPDATE path).
