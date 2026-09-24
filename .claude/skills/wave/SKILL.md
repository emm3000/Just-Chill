---
name: wave
description: "Trigger: /wave, levantar wave, abrir peers, dispatch wave, lanzar sesiones. Boot one Warp pane per ticket and dispatch each ticket to its peer session."
argument-hint: <issue numbers>
allowed-tools: Bash(gh:*) Bash(scripts/justchill-wave:*) Bash(git worktree:*) ListAgents SendMessage Read
license: Apache-2.0
metadata:
  author: "emm3000"
  version: "1.0"
---

## Activation Contract

Run when the owner invokes `/wave` with one or more issue numbers, or when the orchestrator starts the next wave itself after the previous wave is fully merged. Each number becomes one peer session and one dispatch. Stop and report if any number is not an open `ready-for-agent` issue.

## Hard Rules

- Read `docs/agents/multi-session.md` first. Its dispatch checklist, model table and isolation rules bind every dispatch.
- Never queue two tickets on one peer. Never dispatch a ticket while a PR from the same wave is unmerged.
- One explicit model and one explicit effort per ticket, stated to the owner before booting.
- Never touch the owner's main checkout. Peers get their own worktree from `scripts/justchill-session`.
- No wave carries two tickets touching the same module, and a schema change (`.sq` / `.sqm`) is always a wave of one.

## Decision Gates

Rows are ordered by blast radius: how much a mistake breaks and whether a gate catches it. A ticket that matches several rows takes the highest-numbered row that matches. The table lives only here; `docs/agents/dispatch-log.md` records the outcomes per row.

| Row | Work | Model:effort | Extra instruction |
|---|---|---|---|
| 1 | `.md` edits, strings, renames, applying a diff already designed; every criterion is a command with empty output | sonnet:low | none: tests and the criteria fail loudly |
| 2 | Code where the compiler or a test catches the error: one screen, a ViewModel rule, a use case | sonnet:medium | load `mattpocock-skills:tdd` for behavior; visual check on `medium_phone` for a screen |
| 3 | Code on the trap list, where nothing catches the error: a route, a Koin binding, ViewModel purity, an atom default that changes N screens, `.github/` | opus:medium | visual check of every affected screen |
| 4 | Migration, backup/restore, auth, DI graph, cross-module architecture | opus:high | the restore drill in `.claude/rules/sqldelight.md` when the schema moves |

Reviews and other roles keep the playbook rules: a `pr-reviewer` on every PR, restyle/docs/rename reviews sonnet:medium, screen/logic/migration/backup/auth/DI reviews opus:high, post-review fixes sonnet:low, `ticket-writer` opus:high, design fable.

Every row is a bet until `docs/agents/dispatch-log.md` says otherwise. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why here.

- A module-wide sweep that needs judgment per line (which comment survives, which rationale is a duplicate) takes row 3, opus:medium. Wave #129-#133 ran it on sonnet:medium: all 5 PRs came back FIX FIRST, 3 for judgment (partial sweeps, kept history, repeated rationale).
- A dispatch that names a reference file to model the work on inherits that file's debt, so name its known gaps in the same breath. Wave #208-#220 pointed three tickets at `DeleteCategoryDialog`, whose `val type` carries no explicit type: #223 and #224 both came back FIX FIRST on exactly that line. Raising the row would have been the wrong lesson, because the model was not the cause.

## Execution Steps

1. For each issue run `gh issue view <n> --json title,labels,body`. Confirm the label and derive a short lowercase pane name from the title (one word, no digits).
2. Classify each ticket with the table. The table binds: deviate only with a one-line reason stated in the plan, never silently. Tell the owner the plan in one line per ticket: `@<name> #<n> <model>:<effort>`, before booting anything.
3. Run `scripts/justchill-wave <name>:<model>:<effort> ...` once with every ticket.
4. Poll `ListAgents` until every pane name is listed, at most 60 seconds.
5. Send each peer one dispatch built from the playbook checklist: issue, docs to read, branch `<type>/<n>-<slug>`, its worktree `../justchill-<name>`, the acceptance-criteria line, the gate (`scripts/justchill-ci` after every push, before `gh pr create`), TDD or visual check per the table, `Closes #<n>`, no merge, reply with the PR URL. Ask for `notify_when_idle`.
6. Report to the owner in one or two lines: peers booted, tickets dispatched.

## Output Contract

Return the list `@<name> #<n> <model>:<effort>` and nothing else until a peer reports back.

## References

- `docs/agents/multi-session.md` — dispatch checklist, isolation, review cycle.
- `docs/agents/dispatch-log.md` — the outcomes per table row.
- `scripts/justchill-wave` — Warp tab config generator.
- `scripts/justchill-session` — worktree plus `claude` launcher.
