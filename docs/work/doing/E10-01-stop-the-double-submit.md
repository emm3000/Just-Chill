# E10-01 — Stop the double submit on the add-transaction form

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)

## Done when

- [ ] `AddTransactionUiState` carries a saving flag, raised before `createTransaction` and lowered on
      the failure path
- [ ] `AddTransactionScreen` maps that flag to `CtaInteraction.Loading`, never to `Enabled`
- [ ] a host test dispatches `OnSave` twice before the write resolves and asserts
      `CreateTransactionUseCase` ran exactly once

## Context

`addTransaction` never touches the state, so `isEnabled` stays `true` for the whole suspend and
`StickyCTA` keeps its `Modifier.clickable`. `CreateTransactionUseCase` mints a fresh `TransactionId`
per call, so nothing downstream deduplicates. `CtaInteraction.Loading` already exists and is already
used by `AddEditLoanScreen` and `LoanPaymentSheet`.
