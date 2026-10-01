# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. Five consecutive MERGE on a row allow one pilot ticket one step down, logged as what actually ran. A pilot that returns MERGE moves the row, a FIX FIRST keeps it. The five MERGE must span at least two kinds of ticket, and a follow-up bug ticket or a revert traced to a merged PR turns that PR's MERGE into a FIX FIRST for this count, which the orchestrator records when it finds one. Rows fold by row plus model:effort, so a pilot or a moved row gets its own Summary line.

The log has a fixed size. The Summary keeps the totals per row forever; Nudges and Review are folded per Summary line from #516 on (cells start at 0 there, `-` means unmeasured); Minutes is read over Recent alone. Recent keeps only the last 20 PRs. The Changes section keeps one dated line per rule change, newest first, at most 10 lines. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec | Review overturned | Nudges |
|---|---|---|---|---|---|---|---|---|
| 1 | sonnet:low | 12 | 10 | 1 | 1 | 0 | 0 | 0 |
| 2 | sonnet:medium | 47 | 22 | 12 | 10 | 3 | - | - |
| 2 | sonnet:high | 10 | 3 | 4 | 2 | 1 | 0 | 0 |
| 2 | opus:medium | 18 | 11 | 4 | 3 | 0 | 0 | 0 |
| 2 | opus:high | 3 | 2 | 0 | 1 | 0 | 0 | 0 |
| 3 | opus:medium | 102 | 69 | 8 | 15 | 10 | 0 | 0 |
| 3 | opus:high | 18 | 7 | 5 | 5 | 1 | 0 | 0 |
| 4 | opus:high | 56 | 30 | 16 | 7 | 3 | 1 | 0 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row. Nudges counts the `@orch` messages the peer needed before the PR URL, 0 when none. Minutes is wall clock from the dispatch to the PR URL. Review is `ok`, or `overturned` when the orchestrator rejected the first-review FIX FIRST as false. Rows up to #515 carry `-` in Review, Nudges and Minutes: not measured.

| PR | Issue | Row | Model:effort | First review | Cause | Review | Nudges | Minutes |
|---|---|---|---|---|---|---|---|---|
| #665 | #657 | 2 | opus:high | MERGE | - | ok | 0 | 16 |
| #666 | #656 | 2 | opus:high | FIX FIRST | checklist | ok | 0 | 24 |
| #667 | #593 | 2 | opus:high | MERGE | - | ok | 0 | 10 |
| #686 | #670 | 1 | sonnet:low | MERGE | - | ok | 0 | 10 |
| #687 | #671 | 2 | opus:high | MERGE | - | ok | 0 | 15 |
| #688 | #668 | 3 | opus:high | FIX FIRST | judgment | ok | 0 | 16 |
| #689 | #673 | 2 | opus:high | MERGE | - | ok | 0 | 8 |
| #690 | #672 | 2 | opus:high | MERGE | - | ok | 0 | 12 |
| #691 | #669 | 2 | opus:high | MERGE | - | ok | 0 | 12 |
| #692 | #676 | 4 | opus:high | FIX FIRST | judgment | ok | 0 | 9 |
| #693 | #675 | 2 | opus:high | MERGE | - | ok | 0 | 17 |
| #694 | #674 | 2 | opus:medium | FIX FIRST | judgment | ok | 0 | 25 |
| #704 | #678 | 2 | opus:high | MERGE | - | ok | 0 | 11 |
| #705 | #679 | 1 | sonnet:low | MERGE | - | ok | 0 | 11 |
| #706 | #677 | 4 | opus:high | FIX FIRST | spec | ok | 0 | 14 |
| #707 | #680 | 4 | opus:high | MERGE | - | ok | 0 | 18 |
| #708 | #681 | 3 | opus:high | MERGE | - | ok | 0 | 30 |
| #709 | #696 | 2 | opus:high | MERGE | - | ok | 0 | 7 |
| #710 | #695 | 3 | opus:high | MERGE | - | ok | 0 | 10 |
| #711 | #699 | 2 | opus:high | MERGE | - | ok | 0 | 17 |

## Changes

- 2026-10-01: row 4 held at opus:high despite two first-review FIX FIRST judgment in Recent (#660; #692 moved a submit-path fix into `launchSubmitting`, the helper the Google path shares, and changed that path untested). High is the top effort and Fable stays reserved for design, so the raise goes into the dispatch: every row-4 dispatch now names it, a fix scoped to one path never moves into a helper another caller shares unless the ticket names that caller.
- 2026-10-01: the row 2 pilot at opus:medium (#674, PR #694) came back FIX FIRST judgment: the PR body cited shots the assets branch did not hold, and a copied beside-or-under rule dropped its source's height guard. Row 2 stays at opus:high.
- 2026-10-01: row 2 at opus:high reached five consecutive MERGE across three kinds of ticket (#667 iOS chrome, #687 and #689 iOS screens, #690 an Android layout rule with tests, #691 an atom move), so one pilot one step down runs on #674 at opus:medium. Its outcome moves the row or keeps it.
- 2026-10-01: row 3 held at opus:high on #688's first-review FIX FIRST judgment, the only one in Recent after the fold (the reviewer said MERGE, the orchestrator promoted its follow-up: the new search atom carried the hand-built field's 16dp glyph against `ui-components.md`'s 20dp inline rule, a default every taking screen inherits). No rule moves; the existing reference-debt line in the skill covers it.
- 2026-10-01: row 3 held at opus:high on #652's first-review FIX FIRST spec: the ticket's Done-when item 1 asked for a 25 ms double tap `axe` cannot send, and the peer tuned a 1 s window to `axe`'s latency. The orchestrator rescoped the item on the issue (a state guard proven by a red unit test). A criterion that names a measurement tool gets its feasibility checked before dispatch.
- 2026-10-01: row 2 opus:medium -> opus:high: two first-review FIX FIRST judgment in Recent (#645 asserted a 1.0 row unchanged against trunk while its own shots moved the amount 1px; #648 added a duplicate TalkBack label neither neighbour field carries, promoted from a reviewer minor). The raise goes one effort step on the same model.
- 2026-10-01: row 3 held at opus:high on a third first-review FIX FIRST judgment in Recent (#644: the reviewer said MERGE, the orchestrator promoted a test that could never fail, presented as a reproduction that fails on trunk). High is the top effort and Fable stays reserved for design. Every row-3 dispatch now also names it: a test offered as proof must go red when the fix is reverted, and the PR body states only what a run showed.
- 2026-09-30: row 3 held at opus:high on a third first-review FIX FIRST judgment in Recent (#627: a new precondition, the main checkout holding `.worktreeinclude`, shipped with no guard and no doc line). #618 and #619 merged clean in between, and Fable stays reserved for design. Every row-3 dispatch now also names it: a precondition the change introduces gets a guard in code and one line in the doc that drives it.
- 2026-09-30: row 3 held at opus:high despite two first-review FIX FIRST judgment in Recent (#599 a false doc claim, #603 a size drift reported as intended): high is the table's top effort and Fable is reserved for design. The raise goes into the dispatch instead: every row-3 dispatch names both errors (check each doc claim with `rg`; any non-zero pixel diff against trunk is drift until the ticket names it).
- 2026-09-30: row 2 sonnet:high -> opus:medium: two first-review FIX FIRST judgment in Recent (#569; #596, the report hero split on a plain space, visible in the peer's own shot). High is the table's top effort, so the raise moves the model. #595 counts as FIX FIRST judgment: the reviewer said MERGE and the orchestrator promoted its minor (an uncaught observed query froze the backup row).
