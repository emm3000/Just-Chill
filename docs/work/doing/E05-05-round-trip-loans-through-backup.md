# E05-05 — Round-trip loans through backup

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-04

## Done when

- [ ] `BACKUP_SCHEMA_VERSION` is `4`
- [ ] `BackupV3.kt` freezes today's payload shape with a `toCurrent()`, mirroring `BackupV1`/`BackupV2`
- [ ] `ExportPayloadDto` gains `LoanDto`/`LoanPaymentDto` collections
- [ ] `loans.sq` and `loan_payments.sq` each gain `softDeleteAllLive`, `insertOrIgnoreFromBackup`
  and `restoreFromBackup`, mirroring the triple already in `categories.sq`
- [ ] `BackupPayloadDecoder` and the restore repository wire both new tables through
- [ ] a compatibility test proves a v3 export (no loans) restores clean with an empty loans set
- [ ] the restore rehearsal `PERSISTENCE.md` demands per schema bump runs with loans present,
  and its `ImportStats` comparison now counts `loans` and `loanPayments` too

## Context

`BACKUP_SCHEMA_VERSION` is `3` today (`ExportPayloadDto.kt`); the bump is this ticket's, in the same
commit as `BackupV3.kt`.
