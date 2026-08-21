# E05-08 — Add the loan form

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-07

## Done when

- [ ] a create/edit loan form exists with person (autocomplete chips of previously used names),
  monto, interés %, fecha and nota fields
- [ ] the percent field is converted to `interestBps` and handed to the E05-03 use case, which
  computes `totalDue` — the form itself never calls `LoanMath`
- [ ] an empty person or an out-of-range interest surfaces its `ValidationCode` message
  (`PersonRequired`, `InterestOutOfRange`), never a raw exception

## Context

Autocomplete chips read from previously used `personKey` values; there is still no contacts table
(E05 constraint).
