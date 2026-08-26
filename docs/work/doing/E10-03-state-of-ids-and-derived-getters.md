# E10-03 — Rebuild AddTransactionUiState on ids and derived getters

**Epic:** [E10 — ViewModel state hygiene](../epics/E10-viewmodel-state-hygiene.md)
**Blocked by:** E10-02

## Done when

- [ ] `rg 'private var|MutableMap|Job\?' AddTransactionViewModel.kt` returns nothing
- [ ] the state stores `accountId`/`categoryId`; `accountSelected` and `categorySelected` are getters
      that re-resolve against the catalog on every read
- [ ] `frequentCombos` filters by `transactionType` at read time
- [ ] the combos load through `flatMapLatest` over the type, driven from one place
- [ ] a test switches type and asserts the chip row never shows the previous type's combos
- [ ] a test preselects one resolvable id together with one dangling id, and asserts the resolvable
      one still lands
- [ ] `./gradlew qualityGate --rerun-tasks` is green

## Context

A sealed `Catalog` with a `Loading` case answers what the `dataLoaded` boolean answers today, and
`flatMapLatest` cancels what `loadFrequentJob` cancels by hand. The one field that survives into the
state is the preselect-applied flag: E09's constraint that a preselect is consumed once still holds.
