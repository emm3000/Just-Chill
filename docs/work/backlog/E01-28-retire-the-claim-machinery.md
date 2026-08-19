# E01-28 — Retire the claim machinery

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** E01-06

## Done when

- [ ] `ClaimLocalDataUseCase`, `ClaimLocalDataRepository` and `DefaultClaimLocalDataRepository` are deleted with their Koin bindings (`AuthModule.kt:30,32`) and their two test files
- [ ] the eight `claimAll:` / `unclaimAll:` / `countUnclaimed:` queries in `accounts.sq`, `categories.sq`, `recurring_movements.sq` and `transactions.sq` STAY — the epic says a future engine plugging into the preserved sync schema needs a catch-up claim pass, and the queries are that pass
- [ ] `qualityGate --rerun-tasks` green, with the iOS compile and link forced separately if it reports 241 tasks

## Context

E01-05 deleted the observer that called `claimAll`; E01-06 removes the last `unclaimAll` caller.
After that the whole subsystem is dead code held alive only by its own tests, which then prove
nothing. It is split out because E01-06 rewrites a destructive path and a subsystem deletion in the
same commit muddies that review.
