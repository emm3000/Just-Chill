# E01-34 — Count loans in the verifier

**Epic:** [E01 — Snapshot backup](../epics/E01-snapshot-backup.md)
**Blocks:** flipping `SNAPSHOT_BACKUP_ENABLED`

## Done when

- [ ] `BackupRowCounts` carries `loans` and `loanPayments`, and `DefaultBackupVerifier` builds them
      from the payload like the other four
- [ ] `BackupMessageText.toPhrase()` renders both, tuteo, matching the register of the four phrases
      already there
- [ ] a test proves a verified v4 snapshot reports six table counts, not four

## Context

`ExportPayloadDto` carries loans since E05-05, and `BackupRowCountsDto` in the manifest counts them
— but the domain `BackupRowCounts` the verifier reports to Perfil is a different type and still has
four fields, so an in-app "Verificar" would never say whether the loan ledger reached the file. ADR
009 Decision 4 makes per-table counts part of the ship gate, which is why this blocks the flag.

Unreachable in production today: `Verificar` sits inside the `if (SNAPSHOT_BACKUP_ENABLED)` block.
