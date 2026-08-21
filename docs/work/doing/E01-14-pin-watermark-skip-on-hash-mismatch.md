# E01-14 — Pin the watermark skip on hash mismatch

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `BackupOrchestratorTest` gains a case that drives a hash-mismatch upload failure — the
      `ValidationError`/`BackupUploadUnverified` shape `DefaultBackupUploaderTest` pins as thrown —
      through the orchestrator and asserts no watermark is written and no prune runs
- [ ] the same case asserts the failure is booked as `BackupFailureReason.Unverified`, so it cannot
      pass as the generic case with a different exception dropped into it
- [ ] the existing generic case (`a failed upload records no watermark and the next trigger
      retries`) is left as-is — this pins the hash-mismatch failure specifically, not a stand-in

## Context

`DefaultBackupUploaderTest` pins the throw at the uploader level, and `BackupOrchestratorHealthTest`
already drives this exact shape through the orchestrator — but asserts only health there, never the
watermark.
