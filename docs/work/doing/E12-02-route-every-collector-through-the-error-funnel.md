# E12-02 — Route every collector through the error funnel

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [x] `MviViewModel` gains a flow-side sibling of `launchSafe` (a `Flow<T>.launchSafeIn(onError)`
      or equivalent) with the same policy: cancellation is never an error, a `DomainException`
      reaches `onError` untouched, anything else arrives wrapped in `DomainException.Unknown` with
      its cause preserved.
- [x] `MviViewModelTest` pins that helper with the same three cases it pins `launchSafe` with.
- [x] `rg 'launchIn\(viewModelScope\)' presentation/src/main/kotlin/com/emm/justchill/hh` returns
      nothing — every collector goes through the helper.
- [x] `rg 'viewModelScope\.launch' presentation/src/main/kotlin/com/emm/justchill/hh` returns
      nothing — `AddTransactionViewModel.init`, `EditTransactionViewModel.loadFrequent` and
      `loadCurrentTransaction`, `AddEditLoanViewModel.loadLoan`, `AddEditRecurringMovementViewModel.loadTemplate`
      go through `launchSafe`. Per site the writer picks the reaction: an error effect, or
      `loadOrNull`'s "a missing row beats a crashed screen".
- [x] `SeeTransactionsViewModel`'s inner `.catch { emit(...) }` branches stay — they recover per
      branch on purpose and its comment says why; the helper wraps the outer collector only.
- [x] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

The funnel exists, but only the suspend side has a door. A `DomainException.DatabaseError` raised by
`safeDbCall`/`catchAsDomainException` inside any of the flows collected with a bare
`launchIn(viewModelScope)` escapes the scope and kills the process instead of reaching a snackbar.
