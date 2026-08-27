# E12-06 — Land the cheap E12-02 review findings

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [ ] E12 gains two constraint lines: a funnelled collector dying with its first error, and a
      `stateIn`/`shareIn` upstream bypassing the funnel entirely.
- [ ] `LoansViewModelTest` and `PersonLoansViewModelTest` each pin a `ShowError` effect for a
      failed repository read.
- [ ] `EditTransactionViewModel.changeTransactionType`'s stacked error policy is resolved — one
      policy deleted, or the read confirmed and left as designed.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

Follow-up from the 2026-08-27 adversarial review of E12-02 (commits `136a3aba`, `d0789c04`), which
shipped the flow-side error funnel and returned SHIP with four findings. The other two — a retry
path for a funnelled collector, and the repeated `onError` lambdas — leave as their own backlog
tickets, out of this one's scope.
