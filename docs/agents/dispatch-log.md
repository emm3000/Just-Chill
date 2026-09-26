# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 4 | 4 | 0 | 0 | 0 |
| 2 | sonnet:medium | 36 | 18 | 8 | 8 | 2 |
| 3 | opus:medium | 72 | 51 | 5 | 11 | 5 |
| 4 | opus:high | 31 | 19 | 9 | 1 | 2 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #409 | #397 | 2 | sonnet:medium | FIX FIRST | checklist |
| #410 | #401 | 2 | sonnet:medium | FIX FIRST | spec |
| #416 | #414 | 3 | opus:medium | FIX FIRST | spec |
| #417 | #415 | 3 | opus:medium | FIX FIRST | judgment |
| #419 | #418 | 3 | opus:medium | FIX FIRST | spec |
| #422 | #420 | 3 | opus:medium | MERGE | - |
| #424 | #423 | 3 | opus:medium | FIX FIRST | spec |
| #426 | #425 | 3 | opus:medium | FIX FIRST | judgment |
| #428 | #427 | 3 | opus:medium | FIX FIRST | spec |
| #430 | #429 | 3 | opus:medium | MERGE | - |
| #431 | #87 | 2 | sonnet:medium | FIX FIRST | judgment |
| #434 | #89 | 3 | opus:medium | MERGE | - |
| #433 | #101 | 2 | sonnet:medium | FIX FIRST | judgment |
| #437 | #436 | 1 | sonnet:low | MERGE | - |
| #438 | #98 | 3 | opus:medium | MERGE | - |
| #439 | #435 | 1 | sonnet:low | MERGE | - |
| #444 | #440 | 4 | opus:high | MERGE | - |
| #445 | #441 | 3 | opus:medium | MERGE | - |
| #450 | #443 | 2 | sonnet:medium | MERGE | - |
| #451 | #442 | 2 | sonnet:medium | FIX FIRST | checklist |
