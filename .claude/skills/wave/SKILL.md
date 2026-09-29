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
- `iosApp/` only: two tickets may share a wave when they touch different screen-family folders under `iosApp/JustChill/` and neither edits a shared file (`iosApp/CLAUDE.md`, `project.pbxproj`, any non-screen-family entry under `iosApp/JustChill/`); the list is in `docs/agents/multi-session.md` `## Slicing and waves`.

## Decision Gates

Rows are ordered by blast radius: how much a mistake breaks and whether a gate catches it. A ticket that matches several rows takes the highest-numbered row that matches. The table lives only here; `docs/agents/dispatch-log.md` records the outcomes per row.

| Row | Work | Model:effort | Extra instruction |
|---|---|---|---|
| 1 | `.md` edits, strings, renames, applying a diff already designed; every criterion is a command with empty output | sonnet:low | none: tests and the criteria fail loudly |
| 2 | Code where the compiler or a test catches the error: one screen, a ViewModel rule, a use case | sonnet:high | load `mattpocock-skills:tdd` for behavior; visual check on the peer's pool AVD for a screen |
| 3 | Code on the trap list, where nothing catches the error: a route, a Koin binding, ViewModel purity, an atom default that changes N screens, `.github/` | opus:high | visual check of every affected screen |
| 4 | Migration, backup/restore, auth, DI graph, cross-module architecture | opus:high | the restore drill in `.claude/rules/sqldelight.md` when the schema moves |

Review tiers, `ticket-writer` and design roles are in `docs/agents/multi-session.md` `## Model and effort` and `## Review cycle`.

Every row is a bet; the thresholds that move a row up or down live in `docs/agents/dispatch-log.md`, and each move gets a note here.

- A module-wide sweep that needs judgment per line (which comment survives, which rationale is a duplicate) takes row 3, opus:medium. Wave #129-#133 ran it on sonnet:medium: all 5 PRs came back FIX FIRST, 3 for judgment (partial sweeps, kept history, repeated rationale).
- A dispatch that names a reference file to model the work on inherits that file's debt, so name its known gaps in the same breath. Wave #208-#220 pointed three tickets at `DeleteCategoryDialog`, whose `val type` carries no explicit type: #223 and #224 both came back FIX FIRST on exactly that line. Raising the row would have been the wrong lesson, because the model was not the cause.
- Row 2 runs sonnet:high since 2026-09-28. The Summary showed 10 FIX FIRST judgment on 45 row-2 PRs at sonnet:medium, which the raise rule already required; the raise went one effort step, not to Opus, and the log decides whether it holds.
- Row 3 runs opus:high since 2026-09-29. Recent held two first-review FIX FIRST judgment on row 3 at opus:medium (#519 policy wording, #552 line pitch that ignored each font's own line height), which the raise rule requires; the raise went one effort step, and the log decides whether it holds.

## Execution Steps

1. For each issue run `gh issue view <n> --json title,body,labels,comments`. Read the comments as part of the ticket: one may clarify or rescope the body. Confirm the label and derive a short lowercase pane name from the title (one word, no digits).
2. Classify each ticket with the table. The table binds: deviate only with a one-line reason stated in the plan, never silently. Tell the owner the plan in one line per ticket: `@<name> #<n> <model>:<effort>`, before booting anything.
3. Run `scripts/justchill-wave <name>:<model>:<effort> ...` once with every ticket.
4. Poll `ListAgents` until every pane name is listed, at most 60 seconds.
5. Send each peer one dispatch built from the playbook checklist: issue, docs to read, branch `<type>/<n>-<slug>`, its worktree `../justchill-<name>`, the acceptance-criteria line, the gate (`scripts/justchill-ci` after every push, before `gh pr create`), TDD or visual check per the table, `Closes #<n>`, no merge, reply with the PR URL, the autonomy paragraph and the reporting rule from the playbook checklist, the time sentence for rows 1-2, and every comment that clarifies or rescopes the body, quoted or pointed at. Ask for `notify_when_idle`.
6. Report to the owner in one or two lines: peers booted, tickets dispatched.

## Output Contract

Return the list `@<name> #<n> <model>:<effort>` and nothing else until a peer reports back.

## References

- `docs/agents/multi-session.md` — dispatch checklist, isolation, review cycle.
- `docs/agents/dispatch-log.md` — the outcomes per table row.
- `scripts/justchill-wave` — Warp tab config generator.
- `scripts/justchill-session` — worktree plus `claude` launcher.
