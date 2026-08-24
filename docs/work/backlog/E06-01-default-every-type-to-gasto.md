# E06-01 — Default every transaction type to Gasto

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] every initial `TransactionType` in `:presentation` reads `Spend`, not `Income` — the
      add-transaction UiState, the edit-transaction UiState, the report UiState's selected type, and
      the initial state built by `ReportViewModel`
- [ ] `AddEditRecurringMovementUiState` is untouched: it already defaults to `Spend`
- [ ] every test that pinned `Income` as the default is updated to pin `Spend`, and none is deleted to
      make the change pass
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` both pass

## Context

The author records far more spending than income, so an `Income` default costs a tap on nearly every
entry and silently mislabels a transaction saved without touching the toggle. There is no
database-level default for transaction type, so the change is entirely in `:presentation`.
