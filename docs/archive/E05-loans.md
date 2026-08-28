# E05 — Loans

**Decision:** [ADR 010](../adr/010-loans-are-a-parallel-ledger.md)

## Why

The author lends money informally and repeatedly, and tracks the outstanding balances in their head
or on paper. Repayments arrive piecemeal, in cash or by transfer. The feature holds the running
per-person balance. `PRODUCT_REQUIREMENTS.md` §1 has no Won't-have row covering lending — this is
unaddressed scope, not rejected scope, so no row moves.

## Constraints

- **A loan is a parallel ledger and never touches money elsewhere.** Lending creates no Spend; an
  abono creates no Income. Account balances, `liveTotals`, the month report and Cuentas' `Saldo
  total` are all unaffected by every row in `loans` and `loan_payments`. This is the product
  decision the whole feature rests on; wiring a loan into `transactions` reverses it silently.
  Recorded in ADR 010.
- **Settled-ness is derived, never stored.** There is no `status` column: a loan is settled when
  `remaining == 0`. A stored status desyncs from its own payments — the loans table deleted in
  `d1d9446a` had one.
- **`totalDue` is written by the domain on every write path**, create and edit alike. It is
  redundant with `principal` and `interestBps` on purpose — the SQL rollup reads it, and
  recomputing the same rounding in both Kotlin and SQLite is two implementations of one rule. A
  write that skips `LoanMath` leaves a loan whose total disagrees with its own inputs.
  `LoanRepository.create`/`update` take a full `Loan`, so that skip is type-legal rather than
  impossible — a ViewModel injecting `LoanRepository` directly is one Koin binding away from it.
  Loans go through the use cases.
- **Deleting a loan must soft-delete its payments in the same write.** The `ON DELETE RESTRICT`
  clause on `loan_payments.loanId` never fires, because this repo soft-deletes and FK clauses only
  act on physical DELETE (`docs/PERSISTENCE.md`). Delete integrity is a use-case obligation, not
  enforced by SQL — but the existing pattern is not uniform: `DeleteAccountUseCase` refuses to
  delete while live dependents exist, and `DeleteCategoryUseCase` deletes unconditionally and lets
  transactions keep pointing at the dead row on purpose. A loan takes a third shape, cascade
  soft-delete, because a payment has no meaning without its loan.
- **A person is a normalized `personKey` column, not a table.** A contacts CRUD before the first
  loan can be recorded is the onboarding friction `PRODUCT_REQUIREMENTS.md` §3 forbids
  ("descubrible: no requiere onboarding ni tutorial"). Accepted cost: two different people whose
  names normalize identically merge into one row.
- **A new table only survives a restore if it is in the backup payload.** `loans` and
  `loan_payments` must round-trip through `ExportPayloadDto`; a table added without that wiring is
  erased by the next restore, on a device holding real data.
