# E06-02 — Give Préstamos a door in Cuentas

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)
**Blocks:** E06-04

## Done when

- [ ] `AccountsScreen` renders a `"Préstamos"` entry row showing the total owed, and tapping it
      pushes `LoansRoute`
- [ ] the total owed comes from `LoanRepository.balancesByPerson()` through `AccountsViewModel`,
      formatted the same way `HomeViewModel` formats it today — the number must match what Home shows
      for the same data
- [ ] `LoansRoute` now has two doors; Home's `LoansCard` stays until E06-04 removes it
- [ ] the row is a real touch target (48dp minimum) and its icon carries a `contentDescription`
- [ ] the account list's own empty state still renders correctly with the new row present
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` both pass

## Context

`LoanRepository` is already injected by four loans ViewModels, so adding it to `AccountsViewModel`
binds nothing new in Koin. The accounts list is a `LazyColumn`, so the row prepends as its own
`item`. Loans are a parallel ledger (ADR 010) and must not be folded into any account balance.
