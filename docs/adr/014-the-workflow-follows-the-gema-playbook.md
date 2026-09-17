---
status: accepted
date: 2026-09-17
supersedes: ADR 007 (entirely), ADR 013 point 3
---
# The Claude workflow follows the Gema playbook

ADR 007 fixed a way of working that the repo has since outgrown: an Opus
reviewer on every unit, Haiku running the gate, a main thread that never read
raw output, a `## Gotchas` list in `CLAUDE.md` as the trap list and line
citations into a progress doc. ADR 013 point 3 then made review risk-tiered,
with a gate-only tier that shipped UI, docs and wiring unreviewed. The Gema
repo runs the same writer and reviewer loop on GitHub Issues with a simpler
set of rules, and it has the dispatch history to back them.

## Decision

The Claude workflow is Gema's playbook, adapted to this repo's modules,
`trunk`, `medium_phone` and the `justchill-*` scripts.
`docs/agents/multi-session.md` is the procedure; the model table in
`.claude/skills/wave/SKILL.md` is the only place a model and effort per kind
of work is written, and `docs/agents/dispatch-log.md` tunes it from PR
verdicts.

- Every PR gets a fresh `pr-reviewer`. Mechanical slices (restyles, docs,
  renames) are reviewed by Sonnet medium; screens, logic, migrations,
  backup/restore, auth and the DI graph by Opus high. Post-review fixes run
  Sonnet low. The orchestrator passes `model` on every Agent call.
- The orchestrator reads at most one or two files inline to decide and
  delegates the rest.
- Fable is used for design decisions only, never for reviews, implementation
  or doc checks.

## Consequences

ADR 007 is deleted; its evidence lives in git history. ADR 013 points 1 and 2
stand. A docs-only PR now costs a Sonnet review it did not cost under the
gate-only tier, and a review model is chosen per PR instead of fixed at Opus.
When the playbook and this ADR disagree, the playbook is updated and this ADR
is amended in the same change.
