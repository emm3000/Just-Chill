# E06-07 — Bring the saldo total back onto Cuentas

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)
**Blocks:** the restore drill in E05-11 regaining its strong check

## Done when

- [ ] `AccountsScreen` renders the all-time balance above the Préstamos row, labelled the way Home
      labelled it, and the figure equals what Home's hero showed for the same data
- [ ] it is sourced from the balance `TransactionRepository.observeTotals()` already emits — the
      aggregate, never a fold over the whole table
- [ ] no loan or abono moves the figure by a single cent (ADR 010), even though the Préstamos row
      sits directly beneath it
- [ ] `docs/work/backlog/E05-11-rehearse-the-v6-restore.md` and `docs/RELEASE_CHECKLIST.md` cite this
      figure again instead of the current-month totals they were repointed at
- [ ] `docs/work/epics/E05-loans.md` no longer names the deleted Home screen in its constraint about
      what a loan must not touch
- [ ] `@Preview` coverage for a non-zero, a zero and a negative balance
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Deleting Home in E06-04 took the only surface showing the all-time balance with it; `observeTotals()`
survived only because `SeeTransactionsViewModel` reads its `movementCount`. The domain still computes
the balance and the backup still carries it — it just lost its screen.

This matters beyond the number: E05-11's restore drill used that figure to prove a backup restored
real data intact, and the current-month substitute is a weaker witness on the one device holding the
author's only copy.
