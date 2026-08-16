# E01-02 — Verify backup in-app

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] "Verify backup" downloads the latest snapshot, checks its hash, parses it, and compares per-table counts against local data, without applying it
- [ ] the check walks back to the newest snapshot that verifies, rather than reporting the newest pair as broken when only its manifest is bad or of unknown state
- [ ] the UI names which snapshot verified, not just "OK"

## Context

`ProfileOp.VerifyingBackup` does not exist yet; the concurrent-op guard already emits
`OperationInProgress` once it does.
