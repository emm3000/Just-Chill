# E05-10 — Surface loans on Home

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-07

## Done when

- [ ] Home renders a card showing the total owed across all live loans
- [ ] tapping the card pushes the loans route added in E05-07
- [ ] no existing Home total's query (`Saldo total` or the month report) references `loans` or
  `loan_payments`

## Context

The bottom bar is full — a Home card is the entry point, not a fifth tab. `Saldo total` renders in
`HomeScreen.kt`; the new figure is a sibling, not folded into it.
