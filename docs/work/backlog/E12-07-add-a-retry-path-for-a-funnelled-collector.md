# E12-07 — Add a retry path for a funnelled collector

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [ ] A `DomainException` raised inside `AccountsViewModel`, `SeeTransactionsViewModel`, or
      `LoansViewModel`'s init-time collector no longer permanently freezes that screen: a test
      drives the failure, then proves the list updates again on a later emission — without
      recreating the ViewModel.
- [ ] The design is picked by whoever takes this: `retryWhen` on the upstream flow, a "reintentar"
      action on the error snackbar, or making `launchSafeIn` re-collect after `onError` fires.
      Whichever wins, `MviViewModelTest` pins its policy the way it already pins `launchSafe`'s.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

E12's "a funnelled collector dies with its first error" constraint: `launchSafeIn` catches outside
`collect()`, so the coroutine it ran in has already completed by the time `onError` fires. The three
named ViewModels are the ones known to survive a screen visit as the same instance.
