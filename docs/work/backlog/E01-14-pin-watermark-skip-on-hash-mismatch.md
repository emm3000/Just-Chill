# E01-14 — Pin the watermark skip on hash mismatch

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `BackupOrchestratorTest` gains a case that drives a hash-mismatch upload failure (the same
      shape `DefaultBackupUploaderTest` pins as throwing) through the orchestrator and asserts no
      watermark is written
- [ ] the existing generic case (`a failed upload records no watermark and the next trigger
      retries`) is left as-is — this pins the hash-mismatch failure specifically, not a stand-in

## Context

`DefaultBackupUploaderTest` pins the throw at the uploader level; nothing asserts the orchestrator's
watermark behaviour for that specific failure shape.
