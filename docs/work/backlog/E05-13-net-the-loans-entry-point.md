# E05-13 — Net the loans entry point

**Epic:** [E05 — Loans](../epics/E05-loans.md)

## Done when

- [ ] a test in the `qualityGate` suites goes red when Home stops rendering the loans card, or
      when the card stops calling `navigateToLoans`
- [ ] the same test, or a sibling, goes red when any `AppRoute` the nav host declares an entry for
      has no `nav.push` reaching it — that is the general shape of the bug, not the loans instance
- [ ] the mechanism chosen is written down where the next writer meets it, not only in this ticket

## Context

`15b8783a` fixed a deadlock: `LoansCard` was the only push of `LoansRoute` and it rendered behind
`hasLoans`, which needs a loan, which needs the screen the card opens. `qualityGate` stayed green
through the whole of E05 and two blind judges read the diff without seeing it.

`:ui-android` has no UI-test source set — only `androidHostTest`. So this either buys Robolectric
(real composition, real cost) or asserts reachability over the route graph without composing
(cheap, and blind to a card that renders but is never wired). Pick one and say why; if both cost
more than they catch, close the ticket saying so rather than shipping a test that proves nothing.
