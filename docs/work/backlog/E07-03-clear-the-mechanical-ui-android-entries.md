# E07-03 — Clear the mechanical `:ui-android` entries

**Epic:** [E07 — baseline burn-down](../epics/E07-baseline-burndown.md)

## Done when

- [ ] these 11 entries are gone from `baseline-ui-android-main.xml`, or each survivor is named in
      the closing commit with its reason: `UseOrEmpty` on `RecurringMovementsScreen.kt` and
      `EmmDropDown.kt`; `UnusedVariable` on `AddCategoryScreen.kt` and `AccountsScreen.kt`;
      `UnnecessarySafeCall` on `TopExpensesCard.kt`; `UnnecessaryComposable` on
      `AccountPickerSheet.kt`; `NoNameShadowing` on `AccountsScreen.kt`; `ModifierMissing` on
      `HhBottomBar.kt`; `FunctionSignature` on `IconsAll.kt` and `AuthScreen.kt`;
      `ComposableParamOrder` on `AuthScreen.kt`
- [ ] the three `CyclomaticComplexMethod` — `EmmTextInput.kt`, `EmmButton.kt`,
      `AddTransactionScreen.kt` — are reduced by simplifying the branching, not by extracting a
      function whose only job is to move branches out of the counted one
- [ ] every screen still renders identically: no visual change, no changed Spanish copy
- [ ] no threshold in `config/detekt/detekt.yml` is relaxed, no new `@Suppress`, no baseline
      hand-edited and no entry added
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

The 19 `LongParameterList` and 6 `TooManyFunctions` entries in the same file are **out of scope** —
they are [E07-04](E07-04-shorten-the-long-composable-signatures.md) and
[E01-39](E01-39-decide-ignoredefaultparameters.md). Touch neither here.

`:ui-android` has no Compose test harness, so nothing catches a rendering regression but reading the
diff. `UnusedVariable` and `UnnecessarySafeCall` are the two that can hide one: confirm what the
variable fed and whether the receiver is genuinely non-null before deleting either.
