# E05-14 — Update an abono

**Epic:** [E05 — Loans](../epics/E05-loans.md)

## Done when

- [ ] `loan_payments.sq` carries an `update:` that sets `updatedAt` and `syncState = 'Pending'`
      alongside the edited columns, and `LoanPaymentRepository` exposes it
- [ ] the edit goes through a use case, like every other loan write — `LoanValidation` rejects the
      same amounts it rejects on create
- [ ] `LoanDetailScreen` opens the existing payment form on an abono, prefilled, and a test proves
      the loan's `remaining` follows the edited amount
- [ ] a v4 backup exported after an edit carries the edited amount, not the original

## Context

Abonos are create-and-delete only: there is no `update` in `loan_payments.sq`, in
`LoanPaymentRepository`, in `domain/loan/`, or in `LoanDetailIntent`. A wrong amount costs a delete
and a retype. The loan itself has `UpdateLoanUseCase`, so the asymmetry is scope that was never
taken, not a decision — neither the epic nor ADR 010 declares an abono immutable.

`latestLocalChange` in `backup.sq` reads `max(updatedAt)` across six tables. An `update:` that
skips `updatedAt` leaves a device reporting no local change and never uploading the edit.
