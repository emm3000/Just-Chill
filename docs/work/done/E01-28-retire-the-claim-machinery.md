# E01-28 — Retire the claim machinery

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** nothing — E01-06 closed

## Done when

- [x] `ClaimLocalDataUseCase`, `ClaimLocalDataRepository` and `DefaultClaimLocalDataRepository` are deleted with their Koin bindings (`AuthModule.kt:30,32`) and their two test files — proven: `rg 'ClaimLocalData' --type kotlin` returns nothing; the dead `ImportOrdering` ID for the deleted file is out of `config/detekt/baseline-data-main.xml` too
- [x] the twelve `claimAll:` / `unclaimAll:` / `countUnclaimed:` queries — three in each of `accounts.sq`, `categories.sq`, `recurring_movements.sq` and `transactions.sq` — STAY: the epic says a future engine plugging into the preserved sync schema needs a catch-up claim pass, and the queries are that pass — proven: `rg -c` still counts 3 per file and `git status data/src/commonMain/sqldelight/` is clean
- [x] `qualityGate --rerun-tasks` green, plus `:presentation:linkDebugFrameworkIosSimulatorArm64` run explicitly — the gate compiles for iOS and runs SKIE but never links, and its actionable-task count is not a signal either way — proven: gate 245/245 executed, link 22/22 executed, both `BUILD SUCCESSFUL` (245 here against 241 on the same tree earlier, which is why the count is not a condition)

## Context

E01-05 deleted the observer that called `claimAll`; E01-06 removes the last `unclaimAll` caller.
After that the whole subsystem is dead code held alive only by its own tests, which then prove
nothing. It is split out because E01-06 rewrites a destructive path and a subsystem deletion in the
same commit muddies that review.
