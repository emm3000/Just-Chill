# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 3 | 3 | 0 | 0 | 0 |
| 2 | sonnet:medium | 28 | 12 | 6 | 8 | 2 |
| 3 | opus:medium | 62 | 42 | 5 | 10 | 5 |
| 4 | opus:high | 30 | 18 | 9 | 1 | 2 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #374 | #352 | 2 | sonnet:medium | FIX FIRST | checklist |
| #370 | #353 | 2 | sonnet:medium | FIX FIRST | checklist |
| #375 | #355 | 3 | opus:medium | MERGE | - |
| #376 | #344 | 2 | sonnet:medium | MERGE | - |
| #377 | #356 | 3 | opus:medium | MERGE | - |
| #383 | #379 | 3 | opus:medium | MERGE | - |
| #384 | #382 | 3 | opus:medium | MERGE | - |
| #385 | #380 | 3 | opus:medium | MERGE | - |
| #386 | #381 | 3 | opus:medium | MERGE | - |
| #387 | #378 | 3 | opus:medium | MERGE | - |
| #388 | #201 | 4 | opus:high | MERGE | - |
| #399 | #398 | 1 | sonnet:low | MERGE | - |
| #400 | #396 | 2 | sonnet:medium | MERGE | - |
| #402 | #389 | 3 | opus:medium | MERGE | - |
| #403 | #393 | 2 | sonnet:medium | MERGE | - |
| #404 | #390 | 3 | opus:medium | FIX FIRST | judgment |
| #405 | #395 | 3 | opus:medium | MERGE | - |
| #406 | #392 | 2 | sonnet:medium | MERGE | - |
| #407 | #391 | 2 | sonnet:medium | MERGE | - |
| #408 | #394 | 2 | sonnet:medium | MERGE | - |
