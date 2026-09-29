# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. Five consecutive MERGE on a row allow one pilot ticket one step down, logged as what actually ran. A pilot that returns MERGE moves the row, a FIX FIRST keeps it. The five MERGE must span at least two kinds of ticket, and a follow-up bug ticket or a revert traced to a merged PR turns that PR's MERGE into a FIX FIRST for this count, which the orchestrator records when it finds one. Rows fold by row plus model:effort, so a pilot or a moved row gets its own Summary line.

The log has a fixed size. The Summary keeps the totals per row forever; Nudges and Review are folded per Summary line from #516 on (cells start at 0 there, `-` means unmeasured); Minutes is read over Recent alone. Recent keeps only the last 20 PRs. The Changes section keeps one dated line per rule change, newest first, at most 10 lines. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec | Review overturned | Nudges |
|---|---|---|---|---|---|---|---|---|
| 1 | sonnet:low | 6 | 6 | 0 | 0 | 0 | - | - |
| 2 | sonnet:medium | 45 | 20 | 12 | 10 | 3 | - | - |
| 2 | sonnet:high | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 3 | opus:medium | 89 | 60 | 6 | 13 | 10 | - | - |
| 4 | opus:high | 35 | 22 | 10 | 1 | 2 | - | - |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row. Nudges counts the `@orch` messages the peer needed before the PR URL, 0 when none. Minutes is wall clock from the dispatch to the PR URL. Review is `ok`, or `overturned` when the orchestrator rejected the first-review FIX FIRST as false. Rows up to #515 carry `-` in Review, Nudges and Minutes: not measured.

| PR | Issue | Row | Model:effort | First review | Cause | Review | Nudges | Minutes |
|---|---|---|---|---|---|---|---|---|
| #473 | #471 | 3 | opus:medium | MERGE | - | - | - | - |
| #475 | #474 | 1 | sonnet:low | MERGE | - | - | - | - |
| #477 | #476 | 3 | opus:medium | FIX FIRST | checklist | - | - | - |
| #494 | #478 | 4 | opus:high | FIX FIRST | judgment | - | - | - |
| #496 | #479 | 4 | opus:high | FIX FIRST | judgment | - | - | - |
| #497 | #495 | 2 | sonnet:medium | MERGE | - | - | - | - |
| #499 | #480 | 4 | opus:high | FIX FIRST | checklist | - | - | - |
| #500 | #481 | 4 | opus:high | MERGE | - | - | - | - |
| #502 | #484 | 3 | opus:medium | FIX FIRST | checklist | - | - | - |
| #503 | #482 | 4 | opus:high | MERGE | - | - | - | - |
| #504 | #485 | 3 | opus:medium | MERGE | - | - | - | - |
| #506 | #483 | 4 | opus:high | FIX FIRST | checklist | - | - | - |
| #505 | #487 | 3 | opus:medium | MERGE | - | - | - | - |
| #508 | #490 | 3 | opus:medium | MERGE | - | - | - | - |
| #507 | #486 | 4 | opus:high | MERGE | - | - | - | - |
| #509 | #488 | 3 | opus:medium | MERGE | - | - | - | - |
| #510 | #491 | 3 | opus:medium | MERGE | - | - | - | - |
| #511 | #492 | 3 | opus:medium | MERGE | - | - | - | - |
| #515 | #514 | 2 | sonnet:medium | MERGE | - | - | - | - |

## Changes

- 2026-09-28: ported Anthropic's Opus 5.5, Sonnet 5.5 and Fable 5.1 prompting guides. Row 2 sonnet:medium -> sonnet:high; autonomy paragraph and reporting rule in every dispatch; Time sentence rows 1-2; @orch nudges capped at 3; Review, Nudges and Minutes columns; five-MERGE downward pilot; fable:low pilot allowed on row 3.
| #516 | #493 | 4 | opus:high | FIX FIRST | checklist | ok | 0 | 30 |
