# E06-03 — Move pending recurring onto Ver

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)
**Blocks:** E06-04

## Done when

- [ ] `SeeTransactionsScreen` renders the `"Pendientes"` section above the transaction list, and it
      disappears when there is nothing pending
- [ ] tapping a pending row opens `ConfirmRecurringSheet`, and confirm, skip and dismiss all behave
      exactly as they do on Home today
- [ ] `SeeTransactionsViewModel` sources the pending list from `GetPendingRecurringMovementsUseCase`
      directly — **not** from `GetHomeDataUseCase`, which E06-04 deletes
- [ ] `PendientesHeader` and `PendingRecurringRow` no longer live in `HomeScreen.kt`; they move to the
      recurring feature package alongside `ConfirmRecurringSheet`
- [ ] confirming a pending movement refreshes the transaction list it just wrote into
- [ ] the pending section survives the search and category filters being active, or is deliberately
      hidden while filtering — whichever, the behaviour is pinned by a test
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` both pass

## Context

`GetPendingRecurringMovementsUseCase` already stands alone in `:domain/recurring` and is already
registered in `RecurringModule`, so `SeeTransactionsViewModel` injects it without a new binding;
`ConfirmRecurringMovementUseCase` and `SkipRecurringMovementUseCase` come across with it. "Ver" is
the start tab, so pendings gain visibility they never had behind Inicio.
