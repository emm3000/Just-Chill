# E12-09 — Move the `remember` visibility flags into `UiState`

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)

## Done when

- [ ] `AddEditRecurringMovementScreen`'s `showAccountPicker`, `showCategoryPicker`, `showDaySheet`
      and `showAmountSheet` read from `state`; opening and closing each one is an intent.
- [ ] `EditTransaction`'s `showDeleteDialog`, `ProfileScreen`'s `showDeleteAccountDialog`,
      `BackupSection`'s `showImportDialog` and `LoanPaymentSheet`'s `showAmountSheet` and
      `showDateSheet` move the same way, each into the `UiState` of the ViewModel that owns its
      screen.
- [ ] Each affected ViewModel test drives open then close through `onIntent` and asserts
      `state.value`.
- [ ] `rg 'remember \{ mutable' ui-android/src` returns only the out-of-scope holders below.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

E12-05 moved the `rememberSaveable` half and its acceptance criterion grepped for that word alone,
so the `remember` half of the same defect survived it —
[ADR 012](../../adr/012-typed-form-input-dies-with-the-process.md) Decision 3 rejects both, for
opposite reasons. Out of scope: `AuthScreen`'s `passwordVisible` (ADR 012 Decision 4 keeps it
deliberately) and `focused`, `EmmRowMenu`'s `expanded` (a reusable atom with no ViewModel),
`DayOfMonthSheet`'s `selected` (sheet scratch space), and `AppNavHost`'s and
`PlatformHostActions`' pending-payload holders (nav plumbing, not screen state).
