# E07-04 — Shorten the long Composable signatures

**Epic:** [E07 — baseline burn-down](../epics/E07-baseline-burndown.md)

## Done when

- [ ] `AccountRow` (`AccountPickerSheet.kt`) and `CategoryRow` (`CategoryPickerSheet.kt`) stop taking
      theme colours as parameters — between them they thread eight (`swatchColor`, `accentColor`,
      `textPrimary`, `textTertiary`, `activeBg`) that `DESIGN_SYSTEM.md` says come from the theme
- [ ] the five remaining offenders each shrink or are named in the closing commit as signatures that
      read better long: `ReportScreen` and `MonthContent` (`ReportScreen.kt`), `CategoryFilterSheet`,
      `DeleteTransactionDialog`, `ProfileRowWithTrailing` (`ProfileScreen.kt`)
- [ ] the six `TooManyFunctions` — `SeeTransactionsScreen`, `RecurringMovementsScreen`,
      `ProfileScreen`, `AddEditRecurringMovementScreen`, `AddCategoryScreen`, `AccountsScreen` — are
      split or kept with a reason; a file-level entry is amnesty at any size (E07)
- [ ] every screen renders identically and no Spanish copy changes
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`ignoreDefaultParameters: true` already freed the twelve design-system components whose extra
parameters carry defaults. The **7** entries left in `baseline-ui-android-main.xml` exceed the
threshold on required parameters alone, so every one of them is real.

`ReportScreen` and `MonthContent` take 8 and 7 callbacks; the MVI core already has `ReportIntent`, so
a single `onIntent: (ReportIntent) -> Unit` is the obvious shape — check it does not just move the
branching into the Screen. `:ui-android` has no Compose test harness, so a rendering regression is
caught by reading the diff and by nothing else.
