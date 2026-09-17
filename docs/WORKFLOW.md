# Workflow — How We Execute Every Unit of Work

> One GitHub issue in, one rebased PR out; every unit takes the same path. Separate writer and
> top-tier reviewer: ADR 007. Issues as the board, `pr-reviewer` as the only review: ADR 013.

## The unit

One GitHub issue of `emm3000/Just-Chill` labelled `ready-for-agent`. The label vocabulary is
`docs/agents/triage-labels.md`, the `gh` operations are `docs/agents/issue-tracker.md`, the open board
is `gh issue list --label ready-for-agent`, never a list in a doc. The issue IS the work: a `Done
when` list of falsifiable conditions plus at most 3 lines of context. An epic under
`docs/work/epics/` IS the constraints that outlive every issue under it. Git plus engram are the
chronicle; a fact goes in exactly one of the three. New issues come from the `ticket-writer` agent,
from the PRD and an epic; `mattpocock-skills:grilling` stress-tests a plan before it is ticketed.

## The loop

```
1. Orchestrator SCOPES the issue (inline, cheap) and picks the dispatch-log row
2. Orchestrator delegates → WRITER        ── worktree, branch, commits, PR that closes the issue
3. CI runs qualityGate on the PR
4. Orchestrator delegates → pr-reviewer   ── MERGE or FIX FIRST (tiered, capped)
5. Orchestrator rebase-merges, appends the dispatch-log row, closes the cycle
```

**1. Scope (orchestrator, inline).** 1–3 reads plus greps, so the writer prompt is precise. List the
affected files (`fd -e kt`) and their tests; `:presentation` or a module boundary touched, run the
leak grep in `.claude/rules/presentation.md`. Pick the `docs/agents/dispatch-log.md` `## Rows` row
that matches; its `Model:effort` is the writer's, passed explicitly on the Agent call (the tiers
behind the table: ADR 007). An unknown-cause bug gets `mattpocock-skills:diagnosing-bugs` first;
reading legwork goes to `mattpocock-skills:research`.

**2. Writer.** Own git worktree on a branch, commits, a PR whose body says `Closes #<issue>`, reports
the PR number. Its prompt carries: the repo root, the issue number, "own worktree and branch, open a
PR, do NOT push to `trunk`"; pointers to the epic, the relevant ADR(s), `docs/PROGRESS.md` and prior
engram memos; the file list, the `Done when` list (it wins over the file list) and the pre-scoped
blockers; for a move/refactor, a dependency-closure directive (transitive imports mapped from the
consuming layer, non-platform dependencies co-moved, platform symbols hoisted to callbacks); the
gate run locally before the PR opens, `./gradlew qualityGate` plus `./gradlew assembleDevDebug` (the
gate excludes the build on purpose; what it is: `.claude/rules/github-workflows.md`); the
conventions: English code and docs, Spanish only for UI strings, `rg`/`fd`/`bat`/`sd`, conventional
commits, **NO Co-Authored-By** (a PreToolUse hook in `.claude/settings.json` blocks it), linear
history, gotchas saved to engram; `mattpocock-skills:tdd` when the slice is test-first.

**3–4. CI, then review.** `qualityGate` runs on the PR; the reviewer reads `gh pr checks`, nobody
reruns it by hand. `pr-reviewer` is always the top tier: fresh context, adversarial, read-only. It
returns `blocking|minor` findings and one verdict, **MERGE** or **FIX FIRST** with the blocking items
and one cause word (`checklist`, `judgment`, `spec`) for the dispatch log. Whether it runs is the
policy below; its tier never moves. A diff that is not a PR gets `mattpocock-skills:code-review`.

**5. Merge (orchestrator).** MERGE → `gh pr merge --rebase`; rebase merges only, linear history on
`trunk`, so a stale base is rebased by the writer, never merged. FIX FIRST → findings back to the
writer, same worktree, cap below. Append the PR row to `docs/agents/dispatch-log.md` `## Recent`,
fold the oldest into `## Summary` when Recent passes 20. `Closes #n` closes the issue on merge.

## Review policy — risk-tiered and capped

**Cap: one review plus one fix round per unit.** MERGE → done; fixes after a MERGE verdict (nits,
copy) are covered by the gate. FIX FIRST → writer fixes → done; one re-review only for a fix
touching a `blocking` finding, scoped to that finding.
- **Reviewed (mandatory):** Supabase/SQLDelight schema or migrations, backup/restore correctness,
  auth, DI graph changes, a Compose or UI type reaching `:presentation` (ADR 011 Decision 4 made its
  purity a reviewed convention: the import compiles), `.github/`.
- **Gate-only (no reviewer):** UI composition, copy, presentation-layer wiring, docs, tests-only
  changes, mechanical refactors. CI's gate plus the orchestrator's read of the PR is the whole check.
- A unit spanning both tiers gets one review scoped to its high-risk part.

Rationale: no third-party users, the author runs the release daily on real data, and the one
irreversible failure is data loss. `model` is explicit on every Agent call: the parameter overrides
an agent's frontmatter `model:`, and omitting it silently runs the definition or session default.
