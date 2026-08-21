# E01-31 — Settle the reported sign-out test flake

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `DefaultAuthRepositorySignOutTest > signOut propagates a failing local clear instead of
      reporting LocalOnly` either reproduces under a written procedure, or the report is recorded
      as unreproducible and this ticket dies with `git rm`
- [ ] if it reproduces, the fix removes the race — never a retry, a sleep or a `@Ignore`
- [ ] the closing commit states the run count and the method that settled it

## Context

Reported failing roughly one full-suite run in three or four, with stdout `Skipping session logout
as there is no session available`. Twelve runs (eight isolated, four full-suite) did not reproduce
it. Suspected race between `importSession` and the Auth plugin's own scope on `Dispatchers.Main`
under `UnconfinedTestDispatcher`.
