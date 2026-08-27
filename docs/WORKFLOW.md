# Workflow — How We Execute Every Unit of Work

> How we execute every unit of work in this repo — the sync redesign, bugfixes, docs, anything else.

## Roles

- **Orchestrator** (main thread): scopes the unit, delegates, verifies cheaply, keeps its own
  context clean. Does not write non-trivial work inline — see the Model Tier Policy below for the
  bounded-read / mechanical-write exceptions.
- **Writer** (delegated sub-agent, fresh context, full task prompt): does the actual work for one
  unit — the move, the fix, the doc, the slice. Model tier per the policy below.
- **Reviewer** (delegated sub-agent, fresh context, a **different agent instance** — no shared
  context with the writer): adversarial double-check of the writer's commit. Finds breakage or risky
  changes the gate can't catch. **Always Opus when it runs — but it no longer runs on every unit**:
  the Review policy below decides whether a unit gets one. Not to be confused with Judgment Day's
  judges, a separate, on-demand two-panel protocol with its own (narrower) Sonnet carve-out.

## The loop

```
1. Orchestrator SCOPES (inline, cheap)
2. Orchestrator delegates → WRITER              ── writes + commits the unit
3. Orchestrator CHEAP-VERIFY (inline)            ── git state + a quick, unit-appropriate check
4. Orchestrator delegates → REVIEWER             ── adversarial, fresh context
5. SHIP (reviewer verdict) or FIX-FIRST loop back to writer
```

### 1. Scope (orchestrator, inline)
Before delegating, map the unit cheaply so the writer prompt is precise:
- List the affected files (`fd -e kt` or equivalent), check for existing tests.
- If the unit touches `:presentation` or crosses a module boundary: `rg` for UI leaks —
  `androidx\.compose`, `BuildConfig`, `R\.`/`stringResource`/`painterResource`/`Font(R.`,
  `@Preview`/`tooling.preview`, `LocalConfiguration`, `koin.androidx`. `:presentation` is an Android
  library holding `androidx.lifecycle` and nothing else from androidx; `:domain` needs no grep at
  all, since nothing Android resolves on its classpath.
- If the unit touches DI: read the feature's Koin module — does it bind a `:data` repository, and is
  the binding in the right module.
- This stays inline (1–3 reads + greps). Do NOT read the whole feature — that is what the writer's
  fresh context is for.

### 2. Writer prompt must include
- Repo root, branch, "commit here, do NOT branch/push" (unless the unit is explicitly meant to open
  a PR).
- Pointers to read: the relevant ADR(s), `docs/PROGRESS.md`, prior engram memos, the previous unit's
  commit hash if this continues one.
- Exact scope: the file list / what to change, and the package-path-stability rule where it applies.
- Pre-scoped blockers from step 1.
- For a move/refactor unit: a dependency-closure directive — map transitive imports from the
  consuming layer first; co-move non-platform dependencies with it, watching for `internal`→`public`
  visibility flips; hoist platform-specific symbols to callbacks the consuming layer wires in (e.g. the
  nav host).
- The reinforced gate (below).
- Conventions: English code and docs, Spanish only for UI strings; `rg`/`fd`/`bat`/`sd`; conventional
  commit, **NO Co-Authored-By**; linear history; no push unless told.
- Save gotchas to engram.

### 3. Cheap-verify (orchestrator, inline)
- `git log --oneline` — commit landed.
- A quick, unit-appropriate regression check (e.g. `rg -l 'androidx\.compose' presentation/src/main`
  for anything touching `:presentation`).
- Do NOT re-run the full gate here (expensive) — the reviewer does that.

### 4. Reviewer prompt must include
- "FRESH context, adversarial, do NOT trust the writer's self-report."
- The commit hash + what the unit was supposed to do.
- `docs/CODE_QUALITY.md` — the reviewer owns its second half (SRP, DIP, YAGNI, DRY-over-knowledge,
  the use-case admission rule); detekt cannot see any of it, so a green gate says nothing about it.
- Checklist: diff scope sane; no duplicates/leftovers; DI graph bound exactly once where DI is
  touched (zero = runtime crash the build gate misses, double = also wrong); leak grep where it
  applies; behavior preservation (moved/changed bodies match except documented swaps); cross-module
  same-package gotcha where it applies; **re-run the full gate from scratch** (`--rerun-tasks`).
- Output: severity-tagged findings (`CRITICAL`/`WARNING`/`NIT`) + one-line verdict **SHIP** or
  **FIX-FIRST**. Review only — no fixes, no commit.
- **A comment finding has exactly two shapes: DELETE, or KEEP naming the constraint it carries.**
  "Inconsistent with the surrounding style" is not a comment finding — style is what the diff looks
  like, and the policy is about whether the sentence should exist at all. This is not hypothetical:
  3c's review reported a comment as a KDoc-vs-line-comment inconsistency, the orchestrator forwarded
  it as a reformatting instruction, and the round shipped the same sentence in a different syntax
  while the sentence itself was a copy of a rule enforced in another file.

### 5. Decide
- **SHIP** → report to user, move to the next unit.
- **FIX-FIRST** → send findings back to the writer agent (or a fix agent). No re-review by default —
  see the Review policy cap below.

## Review policy — risk-tiered and capped

**Cap: at most one review + one fix round per unit.**
- SHIP → done. Fixes that follow a SHIP verdict (nits, copy) are covered by the gate, not by another
  review round.
- FIX-FIRST → writer fixes → done. One exception: a fix touching a finding the reviewer tagged
  **CRITICAL** gets one re-review, scoped to that finding only.

**Tiering: the full adversarial review runs only where nothing else catches the error.**
- **Reviewed (mandatory):** Supabase/SQLDelight schema or migrations, backup/restore correctness,
  auth, DI graph changes, a Compose or UI type reaching `:presentation`, `.github/` — the same list
  that mandates Opus judges for Judgment Day. `:presentation` is on it because ADR 011 Decision 4
  demoted its compose-free purity from a module boundary to a reviewed convention: the import
  compiles, and the reviewer is the only thing left that rejects it.
- **Gate-only (no reviewer):** UI composition, copy, presentation-layer wiring, docs, tests-only
  changes, mechanical refactors. The reinforced gate plus the orchestrator's cheap-verify is the
  whole check.
- A unit spanning both tiers gets one review scoped to its high-risk part.

Rationale: this app has no third-party users and the author runs the release daily on real data. The
one irreversible failure is data loss. A wrong row in Perfil behind a flag is cheap to fix later;
the review budget belongs to the paths that are not.

## The reinforced gate (every unit)
```bash
./gradlew qualityGate      # the whole gate
./gradlew assembleDevDebug # plus the build, which the gate deliberately excludes
```

**Do not maintain a task list here.** This section used to spell one out, and four hand-written
lists — this doc, the pre-push hook, and CI — drifted apart, none a superset of the others. The gate
has exactly one definition,
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt`, and the hook, the
three workflows and this doc all invoke it.

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
when the screen opens). A fresh-context reviewer that traces the Koin graph and re-runs the gate
catches what the writer — and the compiler — miss. Cost is justified: a broken unit merged silently
costs more than one review pass. Confirmed worth it on Slice 3 of the KMP migration (DI was the
single highest-risk spot; the full ledger and landmines are in `docs/archive/kmp/ORCHESTRATION.md`).

## Model tier policy

| Tier | Role | Examples |
|---|---|---|
| Haiku | Runs and reports. Zero judgment. | `./gradlew qualityGate`, `test`, `assembleDevDebug`, git plumbing, reading logs |
| Sonnet | Writes from a decision already made. | `.md` files, mechanical refactors, renames, applying a diff already designed, tests from a spec |
| Opus | Decides. | Architecture, ADR content, plans, reviewers, Judgment Day judges, and writers where nothing catches the error |
| Main thread | Decides, delegates, verifies conclusions. Never reads raw tool output. | — |

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
- **The writer never reviews its own work.** The reviewer is always a separate agent with fresh
  context.
- **Haiku needs a strict output contract** or it summarizes wrong: it returns exit code plus failing
  task names verbatim, and nothing interpreted.

### Enforcement note

`model` is passed explicitly on every delegation. Agent-file frontmatter (e.g.
`~/.claude/agents/jd-judge-a.md`, `jd-judge-b.md`) carries `model: sonnet` by default, and the Agent
tool's `model` parameter overrides it — omitting it silently downgrades a judge to Sonnet. The
incident chronicle lives in git/engram.
