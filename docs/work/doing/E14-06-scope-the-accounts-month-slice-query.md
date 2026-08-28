# E14-06 — Scope the accounts month slice to a query

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)

## Done when

- [ ] `accountsMonthSlice` no longer folds the whole `transactions` table in memory per emission:
      the month's rows arrive from a SQLDelight query bounded by `YearMonth.range()`, shaped like
      the aggregate `transactions.sq` already documents as the O(1)-for-the-caller replacement.
- [ ] The `AccountsViewModelTest` month fixtures (boundary exclusion, midnight rollover,
      per-account attribution) pass unchanged — the tests pin behaviour, so the swap is invisible
      to them.
- [ ] `./gradlew qualityGate --rerun-tasks` green.

## Context

E14-03 derived per-account nets by filtering `transactionRepository.all()` —
`transactions.sq` records that exact fold as the shape `observeTotals` was built to replace, and
E14-03 simultaneously dropped this screen's only use of that optimised aggregate. Not a
regression (the pre-image scanned `all()` too), a missed correction.
