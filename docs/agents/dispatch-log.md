# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. Five consecutive MERGE on a row allow one pilot ticket one step down, logged as what actually ran. A pilot that returns MERGE moves the row, a FIX FIRST keeps it. The five MERGE must span at least two kinds of ticket, and a follow-up bug ticket or a revert traced to a merged PR turns that PR's MERGE into a FIX FIRST for this count, which the orchestrator records when it finds one. Rows fold by row plus model:effort, so a pilot or a moved row gets its own Summary line.

The log has a fixed size. The Summary keeps the totals per row forever; Nudges and Review are folded per Summary line from #516 on (cells start at 0 there, `-` means unmeasured); Minutes is read over Recent alone. Recent keeps only the last 20 PRs. The Changes section keeps one dated line per rule change, newest first, at most 10 lines. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec | Review overturned | Nudges |
|---|---|---|---|---|---|---|---|---|
| 1 | sonnet:low | 9 | 9 | 0 | 0 | 0 | - | - |
| 2 | sonnet:medium | 47 | 22 | 12 | 10 | 3 | - | - |
| 2 | sonnet:high | 10 | 3 | 4 | 2 | 1 | 0 | 0 |
| 2 | opus:medium | 8 | 5 | 2 | 1 | 0 | 0 | 0 |
| 3 | opus:medium | 102 | 69 | 8 | 15 | 10 | 0 | 0 |
| 3 | opus:high | 13 | 6 | 4 | 3 | 0 | 0 | 0 |
| 4 | opus:high | 54 | 30 | 15 | 6 | 3 | 1 | 0 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row. Nudges counts the `@orch` messages the peer needed before the PR URL, 0 when none. Minutes is wall clock from the dispatch to the PR URL. Review is `ok`, or `overturned` when the orchestrator rejected the first-review FIX FIRST as false. Rows up to #515 carry `-` in Review, Nudges and Minutes: not measured.

| PR | Issue | Row | Model:effort | First review | Cause | Review | Nudges | Minutes |
|---|---|---|---|---|---|---|---|---|
| #621 | #612 | 2 | opus:medium | MERGE | - | ok | 0 | 12 |
| #622 | #611 | 2 | opus:medium | FIX FIRST | checklist | ok | 0 | 21 |
| #623 | #614 | 1 | sonnet:low | MERGE | - | ok | 0 | 8 |
| #624 | #615 | 2 | opus:medium | MERGE | - | ok | 0 | 12 |
| #625 | #613 | 2 | opus:medium | MERGE | - | ok | 0 | 399 |
| #627 | #626 | 3 | opus:high | FIX FIRST | judgment | ok | 0 | 7 |
| #642 | #628 | 2 | opus:medium | MERGE | - | ok | 0 | 11 |
| #643 | #629 | 2 | opus:medium | MERGE | - | ok | 0 | 18 |
| #644 | #630 | 3 | opus:high | FIX FIRST | judgment | ok | 0 | 19 |
| #645 | #632 | 2 | opus:medium | FIX FIRST | judgment | ok | 0 | 14 |
| #646 | #631 | 2 | opus:medium | FIX FIRST | checklist | ok | 0 | 15 |
| #647 | #633 | 2 | opus:medium | MERGE | - | ok | 0 | 9 |
| #648 | #634 | 2 | opus:medium | FIX FIRST | judgment | ok | 0 | 12 |
| #649 | #636 | 2 | opus:high | FIX FIRST | judgment | ok | 0 | 19 |
| #650 | #635 | 2 | opus:high | MERGE | - | ok | 0 | 33 |
| #651 | #638 | 1 | sonnet:low | FIX FIRST | checklist | ok | 0 | 9 |
| #652 | #639 | 3 | opus:high | FIX FIRST | spec | ok | 0 | 24 |
| #659 | #653 | 3 | opus:high | FIX FIRST | checklist | ok | 0 | 11 |
| #661 | #592 | 2 | opus:high | MERGE | - | ok | 0 | 19 |
| #660 | #658 | 4 | opus:high | FIX FIRST | judgment | ok | 0 | 18 |

## Changes

- 2026-10-01: row 3 held at opus:high on #652's first-review FIX FIRST spec: the ticket's Done-when item 1 asked for a 25 ms double tap `axe` cannot send, and the peer tuned a 1 s window to `axe`'s latency. The orchestrator rescoped the item on the issue (a state guard proven by a red unit test). A criterion that names a measurement tool gets its feasibility checked before dispatch.
- 2026-10-01: row 2 opus:medium -> opus:high: two first-review FIX FIRST judgment in Recent (#645 asserted a 1.0 row unchanged against trunk while its own shots moved the amount 1px; #648 added a duplicate TalkBack label neither neighbour field carries, promoted from a reviewer minor). The raise goes one effort step on the same model.
- 2026-10-01: row 3 held at opus:high on a third first-review FIX FIRST judgment in Recent (#644: the reviewer said MERGE, the orchestrator promoted a test that could never fail, presented as a reproduction that fails on trunk). High is the top effort and Fable stays reserved for design. Every row-3 dispatch now also names it: a test offered as proof must go red when the fix is reverted, and the PR body states only what a run showed.
- 2026-09-30: row 3 held at opus:high on a third first-review FIX FIRST judgment in Recent (#627: a new precondition, the main checkout holding `.worktreeinclude`, shipped with no guard and no doc line). #618 and #619 merged clean in between, and Fable stays reserved for design. Every row-3 dispatch now also names it: a precondition the change introduces gets a guard in code and one line in the doc that drives it.
- 2026-09-30: row 3 held at opus:high despite two first-review FIX FIRST judgment in Recent (#599 a false doc claim, #603 a size drift reported as intended): high is the table's top effort and Fable is reserved for design. The raise goes into the dispatch instead: every row-3 dispatch names both errors (check each doc claim with `rg`; any non-zero pixel diff against trunk is drift until the ticket names it).
- 2026-09-30: row 2 sonnet:high -> opus:medium: two first-review FIX FIRST judgment in Recent (#569; #596, the report hero split on a plain space, visible in the peer's own shot). High is the table's top effort, so the raise moves the model. #595 counts as FIX FIRST judgment: the reviewer said MERGE and the orchestrator promoted its minor (an uncaught observed query froze the backup row).
- 2026-09-29: row 3 opus:medium -> opus:high: two first-review FIX FIRST judgment in Recent (#519, #552). Row 4 not raised: of its two judgment verdicts, #520 came from an orchestrator relay, not the model.
- 2026-09-28: ported Anthropic's Opus 5.5, Sonnet 5.5 and Fable 5.1 prompting guides. Row 2 sonnet:medium -> sonnet:high; autonomy paragraph and reporting rule in every dispatch; Time sentence rows 1-2; @orch nudges capped at 3; Review, Nudges and Minutes columns; five-MERGE downward pilot; fable:low pilot allowed on row 3.
