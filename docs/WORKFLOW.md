# Workflow — How We Execute Every Unit of Work

> How we execute every unit of work in this repo — iOS slices, the sync redesign, bugfixes, docs,
> anything else. Carved out of the KMP migration's orchestration doc after Slices 1–3 proved the loop,
> that doc is now closed and archived (`archive/kmp/ORCHESTRATION.md` — slice ledger, landmines,
> established patterns), but the loop it proved now applies repo-wide, not just to KMP. This file is
> the one that stays current.

## Roles

- **Orchestrator** (main thread): scopes the unit, delegates, verifies cheaply, keeps its own
  context clean. Does not write non-trivial work inline — see the Model Tier Policy below for the
  bounded-read / mechanical-write exceptions.
- **Writer** (delegated sub-agent, fresh context, full task prompt): does the actual work for one
  unit — the move, the fix, the doc, the slice. Model tier per the policy below.
- **Reviewer** (delegated sub-agent, fresh context, a **different agent instance** — no shared
  context with the writer): adversarial double-check of the writer's commit. Finds breakage or risky
  changes the gate can't catch. **Always Opus, no exception, regardless of blast radius** — full
  rationale in the Model tier policy below. Not to be confused with Judgment Day's judges, a separate,
  on-demand two-panel protocol with its own (narrower) Sonnet carve-out.

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
- If the unit touches the iOS-exported core (`:presentation` commonMain) or crosses a module
  boundary: `rg` for leaks — `import (java|javax|android)\.`, `BuildConfig`, `R.`/
  `stringResource`/`painterResource`/`Font(R.`, `@Preview`/`tooling.preview`, `LocalConfiguration`,
  `koin.androidx`.
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
- A quick, unit-appropriate regression check (e.g. `rg -l 'import (java|javax|android)\.'
  presentation/src/commonMain` for anything touching the iOS-exported core).
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

### 5. Decide
- **SHIP** → report to user, move to the next unit.
- **FIX-FIRST** → send findings back to the writer agent (or a fix agent) and re-review.

## The reinforced gate (every unit)
```bash
./gradlew qualityGate      # the whole gate
./gradlew assembleDevDebug # plus the build, which the gate deliberately excludes
```

**Do not maintain a task list here.** This section used to spell one out, and it drifted: it was
missing `:data:detektAndroidDeviceTestSourceSet`, while the pre-push hook was missing every test
and CI was missing `:ui-android:testAndroidHostTest`. Four hand-written lists, none a superset of
the others. The gate now has exactly one definition —
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt` — and the hook, the
three workflows and this doc all invoke it.

`qualityGate` covers, per module: detekt over every source set that holds code, the JVM host test
suites, Android lint on the dev variant, and the iOS compile. To change what the gate means, edit
the plugin; everything downstream follows.

"Per module" means the five in `settings.gradle.kts`. `build-logic` is an **included build**, not a
module, so task-name matching never reaches it: one task is named rather than matched,
`:build-logic:test`, and the root project applies `justchill.quality.gate` for that line and nothing
else. Tests only — `build-logic` applies no detekt (its build file applies just `kotlin-dsl`; the
detekt entry there is an `implementation` marker so `DetektConventionPlugin` can be *written*), so
its own sources are the one body of code the gate runs and never lints.

Two properties worth knowing:

- **The iOS compile is host-gated.** Kotlin/Native only builds iOS binaries on macOS, so the gate
  adds it there and logs the skip elsewhere — CI on Linux runs everything else. It is the real proof
  the de-JVM worked, and per `docs/adr/003` it is the one thing keeping frozen iOS revivable.
- **iosMain detekt has no type resolution** (Kotlin/Native has none in detekt 2.0), so TR-dependent
  rules do not fire there. Treat it as style and structure, not a deep semantic gate. It is JVM
  analysis, though, so it runs on any host — including CI.

A green gate still does NOT catch a missing Koin binding at the DI graph level; that is what
`presentation/src/androidHostTest/.../core/AppGraphKoinTest.kt` is for, and it is on the gate.

Never gate on plain `./gradlew detekt`: it is **NO-SOURCE on every KMP module** and only ever
linted `:androidApp`.

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
- **The reviewer (step 4 of the loop) is always Opus — no exception, no blast-radius carve-out.** Its
  entire job is the zone where the compiler, the tests, and `qualityGate` catch nothing, so lowering
  its tier removes its reason to exist; it also reads and reports rather than writing, which makes it
  the cheapest agent in the loop to run at the top tier.
- **Judgment Day's two-judge blind panel is Opus, with `model` passed explicitly on every Agent
  call.** This carve-out is scoped to that panel only: Sonnet judges only when blast radius is low
  (UI, formatters, tests, docs) — NOT when the diff is merely small, and NEVER for the reviewer above.
  Diff size does not predict risk.
- **Opus judges are mandatory for Judgment Day** when the change touches: Supabase or SQLDelight
  migrations, auth, the surface exported to iOS (`:presentation` commonMain), or `.github/` (signing
  keys and credentials).
- **The writer never reviews its own work.** The reviewer is always a separate agent with fresh
  context.
- **Haiku needs a strict output contract** or it summarizes wrong: it returns exit code plus failing
  task names verbatim, and nothing interpreted.

### Enforcement note

`~/.claude/agents/jd-judge-a.md:7` and `jd-judge-b.md:7` carry `model: sonnet` in frontmatter. The
Agent tool's `model` parameter overrides frontmatter, and this policy is deliberately repo-scoped, so
the global frontmatter is NOT being changed — which means omitting `model` on an Agent call silently
downgrades a judge to Sonnet.

This is not a hypothetical risk; it already happened. Across this project's session logs there are 16
`jd-judge-a`/`jd-judge-b` invocations total. Only 8 carry `"model":"opus"`, all in one session,
`c793227e` (2026-08-11). The other 8 carry **no `model` key at all**, so they ran on the frontmatter
default — Sonnet — across three separate sessions: `72d07f9b` (×4), `baafe251` (×2), `fc5c810b` (×2).
Session `baafe251`, timestamped 2026-08-12T04:35:56Z, is the judgment day over the composite-primary-
key repair migration — the migration that rewrote production primary keys irreversibly, the exact
class of change this document declares mandatory-Opus. The silent downgrade landed on the
highest-stakes diff judged so far. **Always pass `model` explicitly.**
