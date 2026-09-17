# ADR 013 — GitHub Issues and pull requests replace the file board and Judgment Day

- **Status**: Accepted
- **Date**: 2026-09-17
- **Deciders**: Edgardo Muñoz
- **Supersedes in part**: [ADR 007](007-one-way-of-working-writer-reviewer-and-model-tiers.md),
  Decision points 4 and 5 (the Judgment Day two-judge panel and its global judge definitions).
  Decision points 1, 2 and 3 — separate writer and reviewer, the model tiers, the tiebreaker — stand.

> Resumen (es): el tablero de tickets deja de ser una carpeta de archivos y pasa a ser GitHub Issues
> con el vocabulario `ready-for-agent`. Cada issue produce un PR; la única revisión es el agente
> `pr-reviewer`. Judgment Day, el panel de dos jueces del ADR 007, nunca tuvo una implementación
> ejecutable y se retira.

## Context

The board was a directory of ticket files under `docs/work/`, one file per ticket, directory as
status. It duplicated what a tracker already does, and every move was a commit on `trunk`. ADR 007
Decision 4 defined a two-judge blind panel, Judgment Day, over a frozen target; it never got a
runnable implementation in this repo — no skill, no command, only prose in `docs/WORKFLOW.md` and a
carve-out in the tier policy that the enforcement note had to keep correcting. Meanwhile the repo
adopted the Pocock skills (`mattpocock-skills:*`), whose ticket, triage and review flows already
assume a tracker and a pull request.

## Decision

1. **GitHub Issues are the board.** An issue labelled `ready-for-agent` is a unit of work; the label
   vocabulary is `docs/agents/triage-labels.md`, the operations `docs/agents/issue-tracker.md`, the
   index `gh issue list --label ready-for-agent`. No doc holds a work list. Epics under
   `docs/work/epics/` keep only the constraints that outlive their issues.
2. **One PR per issue, rebase-merged.** The writer works in its own worktree and opens a PR whose
   body closes the issue; `trunk` keeps linear history.
3. **`pr-reviewer` is the only review.** Fresh context, read-only, top tier, `blocking|minor`
   findings, one verdict; it runs where the risk-tiered policy (now `docs/agents/multi-session.md`) says so, at most once
   plus one fix round per unit. Judgment Day, its judges and their Sonnet carve-out are retired;
   `mattpocock-skills:code-review` covers a diff that is not a PR. `model` stays explicit on every
   Agent call (ADR 007's lesson survives the panel).

## Consequences

- One number space for tickets and PRs, native labels, comments and `Closes #n`; the file board's
  ID, ceiling and directory-status rules are gone with it.
- The dispatch log (`docs/agents/dispatch-log.md`) is tuned from PR verdicts, not judge verdicts.
- Anything that still names the panel, its judges or the file board outside the ADRs is stale.
- Cost: an issue is off-repo state; a clone without `gh` access cannot see the board.
