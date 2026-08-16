# E01-12 — Route backup failure reasons to the UI

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `ProfileViewModel.onBackupEvent` maps `BackupEvent.Failed.cause` to a distinct `ProfileMessage`
      per `BackupFailureReason`, not the single shared `ProfileMessage.BackupFailed`
- [ ] `SerializationError`'s Spanish copy — and every other named reason — is reachable through a
      real failure path, not only its own unit test

## Context

`BackupFailureReason` already distinguishes Serialization/Network/Unauthorized/LocalDatabase/etc.;
`onBackupEvent` throws that distinction away before it reaches the UI.
