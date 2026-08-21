# E05-09 — Register an abono

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-08

## Done when

- [ ] a payment sheet exists with monto, método (Efectivo/Transferencia), fecha and nota
- [ ] an abono whose monto exceeds the loan's current `remaining` is refused with
  `ValidationCode.PaymentExceedsBalance` and never persisted
- [ ] a successful abono is reflected in the loan's displayed `remaining` without a manual refresh
