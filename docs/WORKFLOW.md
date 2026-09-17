# Workflow — How We Execute Every Unit of Work

> One GitHub issue in, one rebased PR out. Every unit — feature slice, bugfix, doc, refactor —
> takes the same path; the model tier policy at the end decides who runs each step.

## The unit

A unit of work is one GitHub issue of `emm3000/Just-Chill` labelled `ready-for-agent`. The label
vocabulary is `docs/agents/triage-labels.md`; the `gh` operations are `docs/agents/issue-tracker.md`;
the open board is `gh issue list --label ready-for-agent`, never a list in a doc.

- The issue IS the work: a `Done when` list of falsifiable conditions plus at most 3 lines of
  context. An epic under `docs/work/epics/` IS the constraints that outlive every issue under it.
  Git plus engram are the chronicle. A fact goes in exactly one of the three.
- New issues come from the `ticket-writer` agent, from the PRD and an epic — never hand-written into
  a doc. `mattpocock-skills:grilling` stress-tests a plan before it is ticketed.
- Citations are by symbol, never by line.

## The loop

```
1. Orchestrator SCOPES the issue (inline, cheap) and picks the dispatch-log row
2. Orchestrator delegates → WRITER        ── worktree, branch, commits, PR that closes the issue
3. CI runs qualityGate on the PR
4. Orchestrator delegates → pr-reviewer   ── MERGE or FIX FIRST (tiered, capped)
5. Orchestrator rebase-merges, appends the dispatch-log row, closes the cycle
```

### 1. Scope (orchestrator, inline)

Map the issue cheaply so the writer prompt is precise, in 1–3 reads plus greps — the whole feature
is what the writer's fresh context is for.

- List the affected files (`fd -e kt` or equivalent), check for existing tests.
- If the unit touches `:presentation` or crosses a module boundary: `rg` for UI leaks —
  `androidx\.compose`, `BuildConfig`, `R\.`/`stringResource`/`painterResource`/`Font(R.`,
  `@Preview`/`tooling.preview`, `LocalConfiguration`, `koin.androidx`. `:presentation` is an Android
  library holding `androidx.lifecycle` and nothing else from androidx; `:domain` needs no grep at
  all, since nothing Android resolves on its classpath.
- If the unit touches DI: read the feature's Koin module — does it bind a `:data` repository, and is
  the binding in the right module.
- Pick the row of `docs/agents/dispatch-log.md` `## Rows` that matches the work; its `Model:effort`
  is the writer's, passed explicitly on the Agent call.
- Bugs with an unknown cause get `mattpocock-skills:diagnosing-bugs` before any writer is dispatched;
  reading legwork goes to `mattpocock-skills:research`.

### 2. Writer

Runs in its own git worktree on a branch, commits, opens a PR whose body says `Closes #<issue>`, and
reports the PR number. Its prompt carries:

- Repo root, the issue number, "own worktree and branch, open a PR, do NOT push to `trunk`".
- Pointers to read: the epic, the relevant ADR(s), `docs/PROGRESS.md`, prior engram memos.
- Exact scope: the file list, the `Done when` list (it wins over the file list), the pre-scoped
  blockers from step 1.
- For a move/refactor unit: a dependency-closure directive — map transitive imports from the
  consuming layer first; co-move non-platform dependencies with it, watching for `internal`→`public`
  visibility flips; hoist platform-specific symbols to callbacks the consuming layer wires in (e.g. the
  nav host).
- The reinforced gate (below), run locally before the PR opens.
- Conventions: English code and docs, Spanish only for UI strings; `rg`/`fd`/`bat`/`sd`; conventional
  commits, **NO Co-Authored-By** (a PreToolUse hook in `.claude/settings.json` blocks it); linear
  history; gotchas saved to engram.
- `mattpocock-skills:tdd` when the slice is a test-first one (a new rule, a bug with a known cause).

### 3. CI

`qualityGate` runs on the PR. The reviewer reads `gh pr checks`; nobody reruns the gate by hand.

### 4. Review (pr-reviewer, always the Opus tier)

The `pr-reviewer` agent takes the PR number, fresh context, adversarial, read-only. It returns
severity-tagged findings and one verdict — **MERGE**, or **FIX FIRST** with the blocking items and
one cause word (`checklist`, `judgment`, `spec`) for the dispatch log. Whether it runs at all is the
review policy below. A diff that is not a PR (a spike, a branch under audit) gets
`mattpocock-skills:code-review` instead.

### 5. Merge (orchestrator)

- **MERGE** → `gh pr merge --rebase`. The repository allows rebase merges only, and `trunk` requires
  linear history: a stale base is rebased by the writer, never merged.
- **FIX FIRST** → findings back to the writer, in the same worktree; cap below.
- Append the PR row to `docs/agents/dispatch-log.md` `## Recent`, fold the oldest into `## Summary`
  when Recent passes 20. `Closes #n` closes the issue on merge. Report to the user.

## Review policy — risk-tiered and capped

**Cap: at most one review + one fix round per unit.**
- MERGE → done. Fixes that follow a MERGE verdict (nits, copy) are covered by the gate, not by another
  review round.
- FIX FIRST → writer fixes → done. One exception: a fix touching a finding the reviewer tagged
  blocking gets one re-review, scoped to that finding only.

**Tiering: the full adversarial review runs only where nothing else catches the error.**
- **Reviewed (mandatory):** Supabase/SQLDelight schema or migrations, backup/restore correctness,
  auth, DI graph changes, a Compose or UI type reaching `:presentation`, `.github/` — the same list
  that mandates Opus judges for Judgment Day. `:presentation` is on it because ADR 011 Decision 4
  demoted its compose-free purity from a module boundary to a reviewed convention: the import
  compiles, and the reviewer is the only thing left that rejects it.
- **Gate-only (no reviewer):** UI composition, copy, presentation-layer wiring, docs, tests-only
  changes, mechanical refactors. CI's gate plus the orchestrator's read of the PR is the whole check.
- A unit spanning both tiers gets one review scoped to its high-risk part.

Rationale: this app has no third-party users and the author runs the release daily on real data. The
one irreversible failure is data loss. A wrong row in Perfil behind a flag is cheap to fix later;
the review budget belongs to the paths that are not.

## The reinforced gate (every unit)
```bash
./gradlew qualityGate      # the whole gate
./gradlew assembleDevDebug # plus the build, which the gate deliberately excludes
```

**Do not maintain a task list here.** The gate has exactly one definition,
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt`, and the pre-push
hook, the workflows and this doc all invoke it; hand-written copies drift.

`qualityGate` covers, per module: detekt (`detektMain`, `detektTest`), that module's own host test
suite, `:data`'s instrumented compile plus `verifySqlDelightMigration`, and `:androidApp`'s dev
lint. The plugin owns everything but the test suites and lint, which each module names in its own
`build.gradle.kts` — the trap that splits is in the root `CLAUDE.md` under Gotchas.

"Per module" means the five in `settings.gradle.kts`. `build-logic` is an **included build**, not a
module, so task-name matching never reaches it: one task is named rather than matched,
`:build-logic:test`, and the root project applies `justchill.quality.gate` for that line and nothing
else. Tests only — `build-logic` applies no detekt (its build file applies just `kotlin-dsl`; the
detekt entry there is an `implementation` marker so `DetektConventionPlugin` can be *written*), so
its own sources are the one body of code the gate runs and never lints.

The whole gate runs on any host — all three workflows run it on `ubuntu-latest`.

A green gate still does NOT catch a missing Koin binding at the DI graph level; that is what
`presentation/src/test/.../core/AppGraphKoinTest.kt` is for, and it is on the gate.

## Why writer and reviewer are always separate agents

The Android build can be green while DI is broken (missing repository binding → runtime crash only
when the screen opens). A fresh-context reviewer that traces the Koin graph catches what the writer
— and the compiler — miss, and a broken unit merged silently costs more than one review pass. The
writer never reviews its own work.

## Model tier policy

| Tier | Role | Examples |
|---|---|---|
| Haiku | Runs and reports. Zero judgment. | `./gradlew qualityGate`, `test`, `assembleDevDebug`, git plumbing, reading logs |
| Sonnet | Writes from a decision already made. | `.md` files, mechanical refactors, renames, applying a diff already designed, tests from a spec |
| Opus | Decides. | Architecture, ADR content, plans, reviewers, Judgment Day judges, and writers where nothing catches the error |
| Main thread | Decides, delegates, verifies conclusions. Never reads raw tool output. | — |

Tier names are roles, not model ids. The Opus tier is the strongest reasoning model available —
`fable` (Fable 5.1) today, `opus` as the fallback; the Agent call's `model` parameter names it. The
effort that goes with each tier is a row of `docs/agents/dispatch-log.md`, tuned from that log.

- **Tiebreaker for code writers: Sonnet writes where the compiler or a test catches the error. Opus
  writes where nothing catches it.** In this repo the "nothing catches it" list IS the `## Gotchas`
  section of `CLAUDE.md`.
- **The reviewer (step 4 of the loop) is always Opus — the Review policy decides whether it runs,
  never its tier.** Its entire job is the zone where the compiler, the tests, and `qualityGate` catch
  nothing, so lowering its tier removes its reason to exist; it also reads and reports rather than
  writing, which makes it the cheapest agent in the loop to run at the top tier.
- **Judgment Day's two-judge blind panel is Opus, with `model` passed explicitly on every Agent
  call.** This carve-out is scoped to that panel only: Sonnet judges only when blast radius is low
  (UI, formatters, tests, docs) — NOT when the diff is merely small, and NEVER for the reviewer above.
  Diff size does not predict risk.
- **Opus judges are mandatory for Judgment Day** when the change touches: Supabase or SQLDelight
  migrations, auth, `:presentation`'s compose-free purity, or `.github/` (signing keys and
  credentials).
- **ADRs and `CONTEXT.md` are Opus work through `mattpocock-skills:domain-modeling`.**
- **Haiku needs a strict output contract** or it summarizes wrong: it returns exit code plus failing
  task names verbatim, and nothing interpreted.

### Enforcement note

`model` is passed explicitly on every delegation. An agent definition's frontmatter can carry its
own `model:`, and the Agent tool's `model` parameter overrides it — omitting the parameter silently
runs whatever the definition or the session default says. That downgraded a Judgment Day judge to
Sonnet once; the incident chronicle lives in git/engram. Judges are spawned as general-purpose
agents with `model` set on the call; `pr-reviewer` and `ticket-writer` carry `model: opus` in their
frontmatter and the call still names it.
