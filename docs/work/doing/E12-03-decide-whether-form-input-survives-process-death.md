# E12-03 — Decide whether form input survives process death

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** —

## Done when — branch B, accept the loss (branch A, `SavedStateHandle`, is rejected)

- [ ] ADR 012 records that typed form input dies with the process, and why.
- [ ] E12 gains one constraint line pointing at the ADR.

## Context

No ViewModel takes a `SavedStateHandle` today (`rg SavedStateHandle presentation ui-android androidApp`
is empty). The sheet flags around the forms do survive, via `rememberSaveable` in `AddTransactionScreen`,
`EditTransaction`, `AddEditLoanScreen`, `ReportScreen` and `SeeTransactionsScreen` — so a restored
screen shows a restored sheet with empty fields. `AuthUiState.password` stays excluded; `AuthScreen`
says why.
