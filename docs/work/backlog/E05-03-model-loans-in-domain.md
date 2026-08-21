# E05-03 — Model loans in domain

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-02

## Done when

- [ ] `:domain` gains `Loan`, `LoanInsert`, `LoanUpdate`, `LoanPayment`, `PaymentMethod`, plus the
  `LoanRepository` and `LoanPaymentRepository` ports
- [ ] `LoanMath` computes `totalDue = principal + round_half_up(principal * interestBps / 10_000)`
  and `remaining = totalDue - sum of live payments`, both pinned by tests
- [ ] `personKey` folds diacritics, not only case — `"Juan Pérez"` and `"juan perez"` produce the
  same key, pinned by a test naming an accented surname
- [ ] create and update use cases call `LoanMath` for `totalDue` on every write — no call site
  computes it inline
- [ ] a delete use case exists and its contract is "soft-delete this loan and its live payments in
  one call" (implementation lands in E05-04)
- [ ] new `ValidationCode.PersonRequired`, `InterestOutOfRange`, `PaymentExceedsBalance` each force
  a branch in `DomainExceptionExt.toUserMessage()`'s exhaustive `when`
- [ ] `./gradlew :domain:testAndroidHostTest` passes for the new suite

## Context

`Money(val cents: Long)` is the existing money type. `PaymentMethod` covers Efectivo/Transferencia;
name it in English, render it in Spanish at the UI layer only.
