# E01-11 — Type the storage-error failure reason

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `DomainException` gains a member carrying the failed HTTP status as typed data, replacing the
      string-only `Unknown(this, "$reason The server answered HTTP $statusCode: $error.")` mapping
- [ ] `Throwable.asBackupFailure`'s `RestException` branch (`data/backup/BackupFailures.kt`) raises
      the new type instead of `DomainException.Unknown`
- [ ] the new type maps to its own `BackupFailureReason`, distinct from `Unknown`

## Context

Today a 413 over the bucket ceiling, a refused 415, any 5xx, the `.json` naming defect, and any
unanticipated throwable all collapse into `Unknown` — a `:domain` + `:data` change, not a
presentation one.
