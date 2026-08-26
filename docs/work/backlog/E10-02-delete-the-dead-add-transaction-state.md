# E10-02 — Delete the dead state on the add-transaction form

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)

## Done when

- [ ] `hasChanges` is gone from `AddTransactionUiState`, and E09's constraint about it — the one
      warning the next writer about a field nothing reads — is removed in the same commit
- [ ] `isEnabled` is gone; `AddTransactionScreen` reads `missingField == null`
- [ ] `reset()`, `AddTransactionIntent.OnReset` and their three tests are gone
- [ ] `AddTransactionEffect.FocusAmountField`, its `sendEffect` and its test are gone
- [ ] `touched()` and `validate()` are collapsed into at most one helper
- [ ] `rg 'OnReset|FocusAmountField' --type kotlin` returns nothing

## Context

Each one is provably unread: `hasChanges` has no consumer outside `:presentation`, `OnReset` no
Composable and no Swift caller, and `AddTransactionScreen` handles `FocusAmountField` with `-> Unit`.
Deleting them also frees the two detekt ceilings the class is wedged against — eleven class
functions and seven top-level — which E10-03 needs before it can extract anything.
