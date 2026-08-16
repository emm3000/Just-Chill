# E01-10 — Pin the propagating local clear

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] a `SessionManager` test double whose `deleteSession()` throws is set on `AuthConfig.sessionManager` after `minimalConfig()` runs
- [ ] `DefaultAuthRepositorySignOutTest` asserts `signOut()` propagates that failure instead of returning `SignOutResult.LocalOnly`

## Context

`minimalConfig()`'s in-memory session manager cannot fail, so today's swallow-then-clear shape is
exercised only on the happy path — the propagating branch is unpinned.
