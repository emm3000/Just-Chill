# E01-25 — Type the client-side key guard

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] the `.json` key guard raises something distinguishable from "nobody saw this coming", so a
  persisted streak can tell a programmer defect from an unanticipated one
- [ ] a test pins which reason it becomes

## Context

`SupabaseBackupObjectStore.kt:35` guards the key client-side and throws `DomainException.Unknown`
before any network call, so `asBackupFailure` passes it straight through to
`BackupFailureReason.Unknown`. Unreachable today — both keys come from `BackupSnapshotName` — but a
rename of `BACKUP_MANIFEST_SUFFIX` would fail every cycle and say only "intenta de nuevo".
