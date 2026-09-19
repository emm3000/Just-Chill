# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 0 | 0 | 0 | 0 | 0 |
| 2 | sonnet:medium | 6 | 0 | 2 | 3 | 1 |
| 3 | opus:medium | 8 | 4 | 2 | 2 | 0 |
| 4 | opus:high | 25 | 15 | 7 | 1 | 2 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #207 | #194 | 2 | sonnet:medium | FIX FIRST | judgment |
| #212 | #193 | 3 | opus:medium | MERGE | - |
| #211 | #158 | 4 | opus:high | MERGE | - |
| #213 | #202 | 2 | sonnet:medium | FIX FIRST | checklist |
| #215 | #214 | 2 | sonnet:medium | MERGE | - |
| #219 | #210 | 3 | opus:medium | MERGE | - |
| #221 | #220 | 2 | sonnet:medium | MERGE | - |
| #222 | #218 | 1 | sonnet:medium | MERGE | - |
| #223 | #208 | 2 | sonnet:medium | FIX FIRST | checklist |
| #224 | #209 | 2 | sonnet:medium | FIX FIRST | checklist |
| #225 | #217 | 3 | opus:medium | FIX FIRST | judgment |
| #227 | #90 | 3 | opus:medium | MERGE | - |
| #228 | #179 | 1 | sonnet:low | MERGE | - |
| #229 | #216 | 3 | opus:medium | MERGE | - |
| #230 | #95 | 4 | opus:high | FIX FIRST | checklist |
| #231 | #168 | 3 | opus:medium | MERGE | - |
| #232 | #182 | 2 | sonnet:medium | MERGE | - |
| #233 | #164 | 3 | opus:medium | FIX FIRST | spec |
| #234 | #93 | 2 | sonnet:low | FIX FIRST | spec |
| #237 | #235 | 3 | opus:medium | FIX FIRST | spec |
