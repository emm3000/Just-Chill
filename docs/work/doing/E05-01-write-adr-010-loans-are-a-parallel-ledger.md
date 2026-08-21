# E05-01 — Write ADR 010: loans are a parallel ledger

**Epic:** [E05 — Loans](../epics/E05-loans.md)

## Done when

- [x] `docs/adr/010-loans-are-a-parallel-ledger.md` exists, amends nothing, and states the decision
  (a loan never posts to `transactions`, an account balance or the month report) plus the
  alternative rejected (modelling a loan as a Spend/Income pair)
- [x] it cites `PRODUCT_REQUIREMENTS.md` §3 as the acceptance criterion the feature passes
- [x] `docs/work/epics/E05-loans.md`'s `**Decision:**` line resolves to this file

## Context

010 is the next free ADR number — 009 is the latest on disk.
