# E10-04 — Share the category catalog across the three transaction forms

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)
**Blocked by:** E10-03

## Done when

- [ ] `rg 'private val allCategories' --type kotlin` returns nothing
- [ ] one `Category` to `SelectableCategory` mapper survives — `mapToUi`, `toSelectable` and
      `mapCategory` become one name
- [ ] `EditTransactionViewModel` and `AddEditRecurringMovementViewModel` hold zero mutable fields,
      and `EditTransactionUiState.hasChanges` goes with them
- [ ] `rg 'private fun today\(\): LocalDate' --type kotlin` returns nothing; the four call sites take
      `TodayFlow` instead
- [ ] `./gradlew qualityGate --rerun-tasks` is green

## Context

The same `MutableMap<CategoryType, List<SelectableCategory>>` is declared in three files and is the
source of eleven of the repo's thirteen mutable fields. `EditTransactionViewModel` also keeps
`oldTransaction` and `snapshot`, a third copy of data the state already holds.
