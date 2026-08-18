# E01-12 — Route backup failure reasons to the UI

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `ProfileMessage.BackupFailed` carries the `BackupFailureReason` instead of discarding it, and
  the copy differs per reason — one message type, not one per reason
- [ ] every named reason is reachable through a real failure path, not only through its own copy test

## Context

`BackupEvent.Failed(cause)` reaches `toProfileMessage` (`ProfileViewModel.kt:242`) and collapses to a
`data object`, so all seven reasons say the same sentence. Copy lives in `BackupMessageText`, never a
composable. `ProfileMessage.toText()` measured complexity 16 against a max of 14 before the nested
`Backup` family split it — one branch per reason would breach it again.
