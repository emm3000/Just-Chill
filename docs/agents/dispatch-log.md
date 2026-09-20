# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 3 | 3 | 0 | 0 | 0 |
| 2 | sonnet:medium | 15 | 4 | 5 | 4 | 2 |
| 3 | opus:medium | 32 | 19 | 4 | 4 | 5 |
| 4 | opus:high | 28 | 17 | 8 | 1 | 2 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #290 | #285 | 2 | sonnet:medium | FIX FIRST | judgment |
| #291 | #286 | 3 | opus:medium | MERGE | - |
| #292 | #287 | 3 | opus:medium | MERGE | - |
| #294 | #288 | 2 | opus:medium | FIX FIRST | judgment |
| #295 | #289 | 4 | opus:high | FIX FIRST | checklist |
| #298 | #245 | 3 | opus:medium | MERGE | - |
| #297 | #272 | 2 | sonnet:medium | FIX FIRST | checklist |
| #299 | #259 | 3 | opus:medium | MERGE | - |
| #300 | #271 | 3 | opus:medium | FIX FIRST | judgment |
| #301 | #270 | 3 | opus:medium | MERGE | - |
| #302 | #185 | 3 | opus:medium | MERGE | - |
| #303 | #282 | 3 | opus:medium | FIX FIRST | judgment |
| #308 | #306 | 3 | opus:medium | MERGE | - |
| #309 | #307 | 3 | opus:medium | FIX FIRST | judgment |
| #311 | #293 | 3 | opus:medium | MERGE | - |
| #313 | #284 | 3 | opus:medium | FIX FIRST | judgment |
| #315 | #314 | 2 | sonnet:medium | MERGE | - |
| #316 | #305 | 3 | opus:medium | MERGE | - |
| #317 | #304 | 2 | sonnet:medium | MERGE | - |
| #318 | #312 | 3 | opus:medium | MERGE | - |
