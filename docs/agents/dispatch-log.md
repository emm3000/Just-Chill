# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 3 | 3 | 0 | 0 | 0 |
| 2 | sonnet:medium | 14 | 3 | 5 | 4 | 2 |
| 3 | opus:medium | 21 | 13 | 3 | 3 | 2 |
| 4 | opus:high | 27 | 16 | 8 | 1 | 2 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #263 | #239 | 3 | opus:medium | MERGE | - |
| #262 | #256 | 3 | opus:medium | MERGE | - |
| #264 | #243 | 3 | opus:medium | FIX FIRST | checklist |
| #265 | #240 | 2 | sonnet:medium | MERGE | - |
| #266 | #258 | 3 | opus:medium | MERGE | - |
| #267 | #257 | 3 | opus:medium | MERGE | - |
| #268 | #85 | 3 | opus:medium | MERGE | - |
| #269 | #244 | 3 | opus:medium | FIX FIRST | spec |
| #274 | #86 | 4 | opus:high | MERGE | - |
| #275 | #260 | 3 | opus:medium | MERGE | - |
| #280 | #277 | 3 | opus:medium | FIX FIRST | spec |
| #281 | #278 | 3 | opus:medium | FIX FIRST | spec |
| #283 | #279 | 3 | opus:medium | FIX FIRST | judgment |
| #290 | #285 | 2 | sonnet:medium | FIX FIRST | judgment |
| #291 | #286 | 3 | opus:medium | MERGE | - |
| #292 | #287 | 3 | opus:medium | MERGE | - |
| #294 | #288 | 2 | opus:medium | FIX FIRST | judgment |
| #295 | #289 | 4 | opus:high | FIX FIRST | checklist |
| #298 | #245 | 3 | opus:medium | MERGE | - |
| #297 | #272 | 2 | sonnet:medium | FIX FIRST | checklist |
