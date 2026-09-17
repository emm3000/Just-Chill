# Dispatch log

The model-tier table in `.claude/skills/wave/SKILL.md` is tuned from this log; the loop it serves is `docs/agents/multi-session.md`. A row with two or more `judgment` FIX FIRST verdicts moves one step up. A row that stays MERGE across many PRs may move one step down.

The log has a fixed size. The Summary keeps the totals per row forever. Recent keeps only the last 20 PRs. At cycle close the orchestrator appends the new PR to Recent. When Recent passes 20 rows, it adds the oldest rows to the Summary counts and deletes them.

Cause values: `checklist` (a recurring item from the reviewer checklist, the model was fine), `judgment` (a wrong decision the model made), `spec` (the issue was wrong or thin).

## Rows

| Row | Model:effort | Work |
|---|---|---|
| 1 | sonnet:low | `.md` edits, renames, applying a diff already designed |
| 2 | sonnet:medium | code where the compiler or a test catches the error |
| 3 | fable:medium | code on the `## Gotchas` list, where nothing catches the error |
| 4 | fable:high | architecture, ADR content, plans, reviews |

## Summary

Totals of rows already folded out of Recent.

| Row | Model:effort | PRs | MERGE | FIX FIRST checklist | FIX FIRST judgment | FIX FIRST spec |
|---|---|---|---|---|---|---|
| 1 | sonnet:low | 0 | 0 | 0 | 0 | 0 |
| 2 | sonnet:medium | 0 | 0 | 0 | 0 | 0 |
| 3 | fable:medium | 0 | 0 | 0 | 0 | 0 |
| 4 | fable:high | 0 | 0 | 0 | 0 | 0 |

## Recent

Last 20 PRs, oldest first. Model:effort is what actually ran, which may differ from the table row.

| PR | Issue | Row | Model:effort | First review | Cause |
|---|---|---|---|---|---|
