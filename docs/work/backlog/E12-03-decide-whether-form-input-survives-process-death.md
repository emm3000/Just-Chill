# E12-03 — Decide whether form input survives process death

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when — one branch; delete the other when taking the ticket

**A — adopt `SavedStateHandle`:**

- [ ] `AddTransactionViewModel`, `EditTransactionViewModel`, `AddEditLoanViewModel`,
      `AddEditRecurringMovementViewModel`, `AddCategoryViewModel` and `AddAccountViewModel` take a
      `SavedStateHandle` and mirror their typed fields into it (`amount`, `description`,
      `personName`, `amountDigits`, `interestPercentText`, `note`, `name`).
- [ ] One host test per ViewModel constructs it over a pre-seeded `SavedStateHandle` and asserts
      `state.value` carries the seeded text.
- [ ] `AppGraphKoinTest` still resolves the whole graph.
- [ ] Emulator drill with "Don't keep activities" on: type into each form, background the app,
      return — every field is still there.

**B — accept the loss:**

- [ ] ADR 012 records that typed form input dies with the process, and why.
- [ ] E12 gains one constraint line pointing at the ADR.

## Context

No ViewModel takes a `SavedStateHandle` today (`rg SavedStateHandle presentation ui-android androidApp`
is empty). The sheet flags around the forms do survive, via `rememberSaveable` in `AddEditLoanScreen`,
`EditTransaction` and `SeeTransactionsScreen` — so a restored screen shows a restored sheet with
empty fields. `AuthUiState.password` stays excluded whichever branch wins; `AuthScreen` says why.
