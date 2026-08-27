# E12-04 — Move the confirm sheet flag into `SeeTransactionsUiState`

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [ ] `SeeTransactionsEffect.CloseConfirmSheet` is deleted;
      `rg CloseConfirmSheet presentation/src ui-android/src` returns nothing.
- [ ] `SeeTransactionsUiState` carries the sheet visibility and `SeeTransactionsEntries` reads it
      from `state` — the `remember { mutableStateOf(false) }` flag is gone.
- [ ] Opening and closing the sheet are intents, and `SeeTransactionsViewModelTest` covers
      close-after-confirm through `state.value`.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

The one effect in the repo that is state in disguise (E12 constraint: an effect is a navigation or
a transient message). Today the flag lives in a non-saveable `remember`, so it also resets on
rotation while the ViewModel keeps going.
