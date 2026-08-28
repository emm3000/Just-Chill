# E12-05 — Move the remaining sheet flags into `UiState`

**Epic:** [E12 — MVI core](../epics/E12-mvi-core.md)
**Blocked by:** E12-04

## Done when

- [x] `AddTransactionScreen` and `EditTransaction` read `showAccountSheet`, `showCategorySheet`,
      `showDateSheet` and `showNoteSheet` from `state`; opening and closing each one is an intent.
- [x] `AddEditLoanScreen`'s `showAmountSheet` and `showDateSheet`, `ReportScreen`'s `showMonthSheet`,
      and `SeeTransactionsScreen`'s `showFilterSheet` and `searchRequested` move the same way.
- [x] Each affected ViewModel test drives open then close through `onIntent` and asserts
      `state.value`.
- [x] `rg rememberSaveable ui-android/src` returns only the out-of-scope drafts below,
      `AppNavHost`'s `rememberSaveableStateHolderNavEntryDecorator`, and `AuthScreen`'s comment.
- [x] `./gradlew qualityGate --rerun-tasks` and `./gradlew :androidApp:testDevDebugUnitTest --rerun`
      pass.

## Context

E12-04 moves `SeeTransactionsScreen`'s `confirmSheetItem` first and is the template for all of these
([ADR 012](../../adr/012-typed-form-input-dies-with-the-process.md): a flag must survive exactly what
the fields inside its sheet survive). Out of scope, because it is a sheet's own scratch space rather
than screen state and is a separate question: `AmountInputSheet.draftDigits`,
`CategoryFilterSheet.segment`/`query`, `CategoryPickerSheet.query`, `MonthPickerSheet.displayYear`,
`ConfirmRecurringSheet.amountCents` and `AddEditLoanScreen.lastEdit`.
