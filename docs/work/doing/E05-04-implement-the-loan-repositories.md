# E05-04 — Implement the loan repositories

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-03

## Done when

- [ ] `:data` gains the SQLDelight entities, mappers, local data sources, `DefaultLoanRepository`
  and `DefaultLoanPaymentRepository` implementing the E05-03 ports
- [ ] the delete path soft-deletes a loan's live payments in the same call — no live payment can
  exist under a soft-deleted loan afterward
- [ ] query tests cover: live loans grouped by `personKey`, the remaining-balance rollup, and the
  live-payment sum per loan
- [ ] `./gradlew :data:testAndroidHostTest` passes for the new suite

## Context

Mirror `DefaultCategoryRepository`/`categories.sq` for the soft-delete shape, but note the loan case
differs: `DeleteAccountUseCase` refuses when dependents exist and `DeleteCategoryUseCase` leaves
dependents orphaned on purpose — a loan cascades instead.
