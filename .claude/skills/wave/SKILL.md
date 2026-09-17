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

Rows are ordered by blast radius: how much a mistake breaks and whether a gate catches it. A ticket that matches several rows takes the highest-numbered row that matches. The rows are `docs/agents/dispatch-log.md` `## Rows`; the log is what tunes them.

| Row | Work | Model:effort | Extra instruction |
|---|---|---|---|
| 1 | `.md` edits, strings, renames, applying a diff already designed; every criterion is a command with empty output | sonnet:low | none: detekt, tests and the criteria fail loudly |
| 2 | Code where the compiler or a test catches the error: one screen, a ViewModel rule, a use case | sonnet:medium | load `mattpocock-skills:tdd` for behavior; visual check on `medium_phone` for a screen |
| 3 | Code on the trap list, where nothing catches the error: a route, a Koin binding, `:presentation` purity, an atom default that changes N screens, `.github/` | fable:medium | visual check of every affected screen |
| 4 | Migration, backup/restore, auth, DI graph, cross-module architecture | fable:high | the restore drill in `docs/PERSISTENCE.md` when the schema moves |

Reviews keep the playbook rules: `pr-reviewer` is always the top tier and runs only where the risk-tiered policy says so; post-review fixes sonnet:low; `ticket-writer` fable:high.

Every row is a bet until `docs/agents/dispatch-log.md` says otherwise. When a row shows two or more first-review FIX FIRST verdicts for reasons the checklist did not cover, raise it one step and note why there.

## Execution Steps

1. For each issue run `gh issue view <n> --json title,labels,body`. Confirm the label and derive a short lowercase pane name from the title (one word, no digits).
2. Classify each ticket with the table. The table binds: deviate only with a one-line reason stated in the plan, never silently. Tell the owner the plan in one line per ticket: `@<name> #<n> <model>:<effort>`, before booting anything.
3. Run `scripts/justchill-wave <name>:<model>:<effort> ...` once with every ticket.
4. Poll `ListAgents` until every pane name is listed, at most 60 seconds.
5. Send each peer one dispatch built from the playbook checklist: issue, docs to read, branch `<type>/<n>-<slug>`, its worktree `../justchill-<name>`, the acceptance-criteria line, the gates (`./gradlew qualityGate assembleDevDebug`), TDD or visual check per the table, `Closes #<n>`, no merge, reply with the PR URL. Ask for `notify_when_idle`.
6. Report to the owner in one or two lines: peers booted, tickets dispatched.

## Output Contract

Return the list `@<name> #<n> <model>:<effort>` and nothing else until a peer reports back.

## References

- `docs/agents/multi-session.md` — dispatch checklist, isolation, review cycle.
- `docs/agents/dispatch-log.md` — the rows and their outcomes.
- `scripts/justchill-wave` — Warp tab config generator.
- `scripts/justchill-session` — worktree plus `claude` launcher.
