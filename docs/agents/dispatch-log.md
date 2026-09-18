# Dispatch log

This log records the outcome of every PR per row of the model table, which lives only in `.claude/skills/wave/SKILL.md`; the loop it serves is `docs/agents/multi-session.md`. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why in the skill. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 0 | 0 | 0 | 0 | 0 |
| 2 | sonnet:medium | 5 | 0 | 1 | 3 | 1 |
| 3 | opus:medium | 0 | 0 | 0 | 0 | 0 |
| 4 | opus:high | 0 | 0 | 0 | 0 | 0 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
| #141 | #138 | 3 | opus:medium | FIX FIRST | checklist |
| #143 | #106 | 4 | opus:high | FIX FIRST | checklist |
| #144 | #107 | 4 | opus:high | MERGE | - |
| #145 | #108 | 4 | opus:high | FIX FIRST | spec |
| #146 | #109 | 4 | opus:high | MERGE | - |
| #148 | #147 | 3 | opus:medium | MERGE | - |
| #152 | #151 | 4 | opus:high | MERGE | - |
| #154 | #110 | 4 | opus:high | FIX FIRST | checklist |
| #156 | #153 | 3 | opus:medium | FIX FIRST | judgment |
| #161 | #111 | 4 | opus:high | FIX FIRST | checklist |
| #166 | #163 | 3 | fable:medium | MERGE | - |
| #170 | #167 | 3 | opus:high | FIX FIRST | judgment |
| #171 | #157 | 3 | opus:high | MERGE | - |
| #172 | #112 | 4 | opus:high | FIX FIRST | checklist |
| #176 | #113 | 4 | opus:high | MERGE | - |
| #177 | #175 | 3 | opus:medium | FIX FIRST | checklist |
| #178 | #116 | 4 | opus:high | MERGE | - |
| #181 | #115 | 4 | opus:high | MERGE | - |
| #183 | #117 | 4 | opus:high | MERGE | - |
| #184 | #114 | 4 | opus:high | MERGE | - |
