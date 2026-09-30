# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. Five consecutive MERGE on a row allow one pilot ticket one step down, logged as what actually ran. A pilot that returns MERGE moves the row, a FIX FIRST keeps it. The five MERGE must span at least two kinds of ticket, and a follow-up bug ticket or a revert traced to a merged PR turns that PR's MERGE into a FIX FIRST for this count, which the orchestrator records when it finds one. Rows fold by row plus model:effort, so a pilot or a moved row gets its own Summary line.

The log has a fixed size. The Summary keeps the totals per row forever; Nudges and Review are folded per Summary line from #516 on (cells start at 0 there, `-` means unmeasured); Minutes is read over Recent alone. Recent keeps only the last 20 PRs. The Changes section keeps one dated line per rule change, newest first, at most 10 lines. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec | Review overturned | Nudges |
|---|---|---|---|---|---|---|---|---|
| 1 | sonnet:low | 8 | 8 | 0 | 0 | 0 | - | - |
| 2 | sonnet:medium | 47 | 22 | 12 | 10 | 3 | - | - |
| 2 | sonnet:high | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| 3 | opus:medium | 102 | 69 | 8 | 15 | 10 | 0 | 0 |
| 3 | opus:high | 1 | 0 | 0 | 1 | 0 | 0 | 0 |
| 4 | opus:high | 50 | 29 | 14 | 5 | 2 | 0 | 0 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row. Nudges counts the `@orch` messages the peer needed before the PR URL, 0 when none. Minutes is wall clock from the dispatch to the PR URL. Review is `ok`, or `overturned` when the orchestrator rejected the first-review FIX FIRST as false. Rows up to #515 carry `-` in Review, Nudges and Minutes: not measured.

| PR | Issue | Row | Model:effort | First review | Cause | Review | Nudges | Minutes |
|---|---|---|---|---|---|---|---|---|
| #562 | #560 | 3 | opus:high | MERGE | - | ok | 0 | 17 |
| #563 | #556 | 3 | opus:high | FIX FIRST | checklist | ok | 0 | 28 |
| #564 | #557 | 3 | opus:high | MERGE | - | ok | 0 | 10 |
| #565 | #558 | 3 | opus:high | MERGE | - | ok | 0 | 20 |
| #566 | #530 | 2 | sonnet:high | FIX FIRST | spec | ok | 0 | 12 |
| #567 | #531 | 3 | opus:high | FIX FIRST | checklist | ok | 0 | 22 |
| #568 | #532 | 2 | sonnet:high | FIX FIRST | checklist | ok | 0 | 12 |
| #569 | #533 | 2 | sonnet:high | FIX FIRST | judgment | ok | 0 | 21 |
| #570 | #534 | 2 | sonnet:high | MERGE | - | ok | 0 | 14 |
| #571 | #529 | 2 | sonnet:high | MERGE | - | ok | 0 | 9 |
| #572 | #535 | 4 | opus:high | FIX FIRST | checklist | ok | 0 | 22 |
| #573 | #536 | 3 | opus:high | FIX FIRST | checklist | ok | 0 | 33 |
| #574 | #537 | 2 | sonnet:high | FIX FIRST | checklist | ok | 0 | 10 |
| #575 | #543 | 2 | sonnet:high | MERGE | - | ok | 0 | 7 |
| #576 | #538 | 2 | sonnet:high | FIX FIRST | checklist | ok | 0 | 23 |
| #577 | #539 | 4 | opus:high | FIX FIRST | spec | overturned | 0 | 27 |
| #578 | #540 | 4 | opus:high | MERGE | - | ok | 0 | 24 |
| #594 | #581 | 3 | opus:high | MERGE | - | ok | 0 | 12 |
| #595 | #579 | 4 | opus:high | FIX FIRST | judgment | ok | 0 | 16 |
| #596 | #580 | 2 | sonnet:high | FIX FIRST | judgment | ok | 0 | 17 |

## Changes

- 2026-09-30: row 2 sonnet:high -> opus:medium: two first-review FIX FIRST judgment in Recent (#569; #596, the report hero split on a plain space, visible in the peer's own shot). High is the table's top effort, so the raise moves the model. #595 counts as FIX FIRST judgment: the reviewer said MERGE and the orchestrator promoted its minor (an uncaught observed query froze the backup row).
- 2026-09-29: row 3 opus:medium -> opus:high: two first-review FIX FIRST judgment in Recent (#519, #552). Row 4 not raised: of its two judgment verdicts, #520 came from an orchestrator relay, not the model.
- 2026-09-28: ported Anthropic's Opus 5.5, Sonnet 5.5 and Fable 5.1 prompting guides. Row 2 sonnet:medium -> sonnet:high; autonomy paragraph and reporting rule in every dispatch; Time sentence rows 1-2; @orch nudges capped at 3; Review, Nudges and Minutes columns; five-MERGE downward pilot; fable:low pilot allowed on row 3.
