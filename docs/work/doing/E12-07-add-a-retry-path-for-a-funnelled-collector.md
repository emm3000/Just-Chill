# E12-07 — Add a retry path for a funnelled collector

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when

- [ ] A `DomainException` raised inside `AccountsViewModel`, `SeeTransactionsViewModel`, or
      `LoansViewModel`'s init-time collector no longer permanently freezes that screen: a test
      drives the failure, then proves the list updates again on a later emission — without
      recreating the ViewModel.
- [ ] Picked: `retryWhen` inside `launchSafeIn` — bounded re-subscription with exponential backoff,
      the error effect firing only once the retries are spent, so four attempts never mean four
      snackbars. `MviViewModelTest` pins that policy the way it already pins `launchSafe`'s, the
      cancellation arm included.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

E12's "a funnelled collector dies with its first error" constraint: `launchSafeIn` catches outside
`collect()`, so the coroutine it ran in has already completed by the time `onError` fires. The three
named ViewModels are the ones known to survive a screen visit as the same instance. What actually
reaches that catch is SQLite lock contention, which re-subscribing heals; a "reintentar" action
would need an effect carrying a lambda, and E12 rules an effect is a navigation or a message.
