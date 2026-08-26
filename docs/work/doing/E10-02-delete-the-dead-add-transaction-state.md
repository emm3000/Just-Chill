# E10-02 — Delete the dead state on the add-transaction form

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)

## Done when

- [ ] `hasChanges` is gone from `AddTransactionUiState`, and E09's constraint about it — the one
      warning the next writer about a field nothing reads — is removed in the same commit
- [ ] `isEnabled` is gone; `AddTransactionScreen`'s `ctaInteraction` reads `missingField == null`
- [ ] `touched()` and `validate()` are gone, and all eleven call sites drop the suffix
- [ ] `reset()`, `AddTransactionIntent.OnReset` and their three tests are gone
- [ ] `AddTransactionEffect.FocusAmountField`, its `sendEffect` and its test are gone
- [ ] `rg 'hasChanges|OnReset|FocusAmountField' presentation ui-android androidApp` finds nothing
      outside `EditTransaction`, which E10-04 takes
- [ ] `./gradlew qualityGate` is green, with `:androidApp:testDevDebugUnitTest --rerun`

## Context

Each one is provably unread: `hasChanges` has no consumer outside `:presentation`, `OnReset` no
Composable and no Swift caller, and `AddTransactionScreen` handles `FocusAmountField` with `-> Unit`.
`validate()` only ever set `isEnabled`, and `touched()` only ever called `validate()` and set
`hasChanges` — so both vanish with the two fields rather than merging into one.
