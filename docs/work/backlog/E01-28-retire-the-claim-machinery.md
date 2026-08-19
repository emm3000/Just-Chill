# E01-28 — Retire the claim machinery

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** nothing — E01-06 closed

## Done when

- [ ] `ClaimLocalDataUseCase`, `ClaimLocalDataRepository` and `DefaultClaimLocalDataRepository` are deleted with their Koin bindings (`AuthModule.kt:30,32`) and their two test files
- [ ] the twelve `claimAll:` / `unclaimAll:` / `countUnclaimed:` queries — three in each of `accounts.sq`, `categories.sq`, `recurring_movements.sq` and `transactions.sq` — STAY: the epic says a future engine plugging into the preserved sync schema needs a catch-up claim pass, and the queries are that pass
- [ ] `qualityGate --rerun-tasks` green, plus `:presentation:linkDebugFrameworkIosSimulatorArm64` run explicitly — the gate compiles for iOS and runs SKIE but never links, and its actionable-task count is not a signal either way

## Context

E01-05 deleted the observer that called `claimAll`; E01-06 removes the last `unclaimAll` caller.
After that the whole subsystem is dead code held alive only by its own tests, which then prove
nothing. It is split out because E01-06 rewrites a destructive path and a subsystem deletion in the
same commit muddies that review.
