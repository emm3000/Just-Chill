# E01-13 — Timeout the shared backup mutex

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] the shared mutex's `withLock` call in `DeleteUserAccountUseCase` takes a timeout, so a stuck
      holder cannot block the other caller indefinitely
- [x] `BackupOrchestrator`'s upload critical section acquires the same shared mutex, so account
      deletion and an in-flight backup can no longer run unserialized
- [x] a test pins that a stuck holder times out instead of blocking its counterpart forever

## Context

The mutex is a shared Koin `single` with no timeout today — the engine's old defect under a new
name, and it must close before `SNAPSHOT_BACKUP_ENABLED` flips.
