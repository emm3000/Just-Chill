# E05-06 — Add the loans ViewModels

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-05

## Done when

- [ ] `:presentation` gains a people-list ViewModel and a person-detail ViewModel, each with its own
  `UiState`/`Intent`/`Effect` triple over `MviViewModel<S, I, E>`
- [ ] `hh/di/LoanModule.kt` exists and both ViewModels are Koin-bound there
- [ ] the new module is added to `appModules` in `AppGraph.kt`
- [ ] `AppGraphKoinTest` passes with both bindings resolvable

## Context

ViewModels cannot touch Compose (`presentation/CLAUDE.md`); `AppGraphKoinTest` only catches a missing
binding reached through `koin.get`, so register it rather than injecting it directly.
