# CLAUDE.md — per-module guidance lives in each module's own `CLAUDE.md`

> **KMP everywhere; each platform owns its UI.** Android renders Compose (`:ui-android`); iOS is native
> SwiftUI over the `JustChillKit` framework `:presentation` exports with SKIE (ADR 005; slices in
> `docs/swiftui/PLAN.md`). Keep the iOS compile in every gate run — it is the only thing stopping the
> exported core from silently filling with `java.*`. Android-only *capabilities* may live in
> `:androidApp`; their platform-neutral *logic* stays in the KMP core.
>
> **No third-party users, but the author runs the release daily** on a device holding real accumulated
> data — UI is cheap to redo, a destructive migration is not.

## Build & Development Commands

```bash
./gradlew assembleDevDebug      # dev debug build; prod release is assembleProdRelease
./gradlew qualityGate           # THE gate — see Gotchas
./gradlew test                  # all JVM host tests; per module :<module>:testAndroidHostTest;
                                # the MockK ViewModel suite is :androidApp:testDevDebugUnitTest
./gradlew :presentation:compileKotlinIosSimulatorArm64      # proves zero java.*/android.* leak
./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64 # links JustChillKit + runs SKIE
```

KMP host-test tasks go `UP-TO-DATE` across sessions — `--rerun` forces a real run, **per-task** (it only
forces the task it follows). There is no `:domain:test` and no `connectedDevDebugAndroidTest`.

## Project Layout

- Java toolchain 17 everywhere. `compileSdk = 37`. `minSdk = 28`, except 26 on `:domain` and `:data`.
- `iosApp/` — Xcode project consuming `JustChillKit`. `supabase/` — CLI migrations for the server schema.
- Two `tier` flavors (`dev` / `prod`), `:androidApp` only. Signing, Crashlytics-per-flavor and the
  deliberately-absent Firebase Analytics are in `androidApp/CLAUDE.md`.
- Versions live in `gradle/libs.versions.toml` + `gradle-wrapper.properties`, not here. Compose
  Multiplatform is **gone** — `:ui-android` renders Google's Compose under the BOM; `:presentation`
  still uses JetBrains' multiplatform `lifecycle-viewmodel`, which has to compile for iOS.

## Architecture

Clean Architecture, five KMP modules. Dependency direction is top to bottom:

| Module | Role | Root package |
|---|---|---|
| `:androidApp` | thin Android entry point (Activity, platform Koin module) | `com.emm.justchill.*` |
| `:ui-android` | Android-only Compose UI (screens, nav, theme) | `com.emm.justchill.{hh.<feature>, core, components}` |
| `:presentation` | compose-free MVI core, ViewModels, Koin DI, formatters; exports `JustChillKit` (SKIE) to iOS | same packages as `:ui-android` on purpose |
| `:data` | implements domain interfaces (commonMain/androidMain/iosMain) | `com.emm.data.<entity>` |
| `:domain` | pure Kotlin, no framework deps | `com.emm.domain.<entity>` |

`iosApp/` sits on `:presentation` directly; `:ui-android` sits on it as a Gradle dependency. Same Kotlin
packages across that boundary on purpose — explicit imports are required where same-package symbols
crossed modules. ViewModels cannot touch Compose; conventions around it (ViewModel purity, why
`:presentation` depends on `:data`) are in `presentation/CLAUDE.md`.

The app is **local-first**: SQLDelight on-device is the single source of truth, fully usable with no
account and no network. **Sync is being removed, not repaired**: ADR 009 replaces row replication
with **snapshot backup** and deletes the engine while keeping the sync schema. **Read
`docs/work/epics/E01-snapshot-backup.md` before touching `data/.../sync/`.**

**Data flow:** `Screen` → `ViewModel` → use case → `Repository` interface → `Default{Entity}Repository` → `LocalDataSource` (SQLDelight). The use case is there **only where there is domain logic** — a pure read goes from `ViewModel` straight to the `Repository` interface. Rationale + the measurement: `docs/CODE_QUALITY.md`.

### Contracts that span modules

- **MVI** — `MviViewModel<S, I, E>` and every ViewModel/UiState/Intent/Effect live in `:presentation`
  (`core/mvi/`); the Screens consuming them live in `:ui-android`.
- **Errors** — sealed `DomainException` (`:domain/shared/error/`) → `SafeCall.kt` translates SQLDelight
  exceptions into it → `DomainExceptionExt.kt` renders the Spanish message. Add failure modes by
  extending `DomainException`, never a new exception type.

## Testing

JUnit4 + MockK + `kotlinx-coroutines-test` as JVM host tests (`androidHostTest`); `:domain` use cases
are the primary surface. Two suites are the ONLY net for their failure mode: `AppGraphKoinTest`
(missing Koin binding) and the instrumented `:data` tests (missing migration, off the default gate).

## Delegation

| Tier | Role |
|---|---|
| Haiku | Runs and reports — gate/tests/builds/git, zero judgment |
| Sonnet | Writes from a decision already made — docs, mechanical refactors, tests from a spec |
| Opus | Decides — architecture, ADRs, plans, reviewers, judges, and code where nothing else catches the error |
| Main thread | Decides, delegates, verifies conclusions — never reads raw tool output |

Tiebreaker: Sonnet writes where the compiler/a test catches the error; Opus where nothing does (that
"nothing" list is `## Gotchas` below). The reviewer is always Opus. `model` passed explicitly every
time, never relying on agent-file frontmatter. Loop + reasoning: `docs/WORKFLOW.md`.

## Writer conventions (they reach every subagent through this file)

- **Comments climb a ladder, in order: delete → rename → redesign → comment.** In a test file the test
  method name IS the rename rung. A surviving comment names a constraint the code cannot show — one
  comment, one fact. Full rule: `docs/CODE_QUALITY.md`.
- **English for every identifier; Spanish only in user-data VALUES.** `name = "Sueldo"` is data;
  `val sueldo` is a violation — name fixture locals by role (`incomeCategory`).

## Gotchas

This is the Opus list (`docs/WORKFLOW.md` model-tier policy) — a Sonnet writer does not write here.

- **`./gradlew qualityGate` is the gate.** One definition — `build-logic/.../QualityGateConventionPlugin.kt`,
  invoked by the pre-push hook and all three workflows: detekt over every module source set holding
  code, the host test suites, dev lint, (macOS) the iOS compile, plus `:build-logic:test` named
  explicitly (an included build is unreachable by task-name matching; its sources are the one code the
  gate runs and never lints). Change the plugin, not the callers. **Never gate on plain
  `./gradlew detekt`** — `NO-SOURCE` on all three KMP modules; it only lints `:androidApp`.
- **Third-party actions in `.github/` are pinned to a commit SHA on purpose** — they hold the signing
  and Play/Firebase credentials, and a floating `@v1` can be repointed upstream. Do not "tidy" them
  into tags; dependabot proposes bumps. GitHub's own `actions/*` stay on tags.
- **`versionName` is `git describe --match "v[0-9]*"`, and the filter is load-bearing** — the repo
  carries non-release tags and a bare `describe` returns the nearest one (builds once shipped
  `versionName = "pre-kmp"`). Same filter in `/release`.
- **A tag push does not ship.** `uploadRelease.yml` uploads the AAB to the alpha track as a **draft**; publishing it is manual in Play Console. A green workflow reached no one.
- **`run:` blocks take secrets through `env:`**, never `${{ }}` spliced into the script text. Validate workflow edits with `actionlint` — it catches errors a YAML parse cannot.
- **Every route the nav host can push MUST be `@Serializable`.** The reflective 1-arg
  `rememberNavBackStack` re-resolves each entry via `Class.forName(name).kotlin.serializer()` — miss
  the annotation and the app dies only on process-death restore, invisible to the compiler.
  `RouteSerializationTest` round-trips every sealed `AppRoute` through that serializer.

## Docs contract

Six doc types, one job and a death rule each: **`CLAUDE.md`** — how to work here, ≤150 lines, pointers
only. **`adr/`** — one decision per ADR, target 1 page, ceiling 2; amended by a new ADR, never edited.
**One live plan per track** — ONLY what remains; closing a unit removes it from the plan in the same
commit. **`PROGRESS.md`** — backlog items of 1–2 lines + pointer, no essays. **Reference docs**
(`WORKFLOW`, `CODE_QUALITY`, `DESIGN_SYSTEM`) — timeless conventions, zero history. **`archive/`** —
the reasoning of closed work. The chronicle lives in git and engram, never in a live doc; every live
doc has a read-trigger in the map below, and a doc with no trigger is archive.

## Docs map (`docs/`)

- `PROGRESS.md` — "where are we now" plus the single open-work checklist; sync debt lives in the
  work epic instead. **Read it first.**
- `work/epics/E01-snapshot-backup.md` — the sync/backup epic: constraints outliving every ticket
  under it, remaining work in `work/backlog/`. **Read before touching sync.** The Phase 0–3
  chronicle, old audit, old plan and old slice plan sit in `archive/sync/`.
- `work/epics/E02-migration-coverage.md` — **Read before touching a `.sqm` or a migration test.**
- `adr/` — filenames state the decision; each header declares what it amends or supersedes. 009 is
  the one to read first for anything sync-shaped. **Read before changing anything an ADR decided.**
- `DATE_AUDIT.md` — the date findings + live rule #7: whatever asks "what day is it" takes an injected
  `Clock` **and** `TimeZone`, neither with a default. **Read before touching dates.**
- `PLAY_ADVERTISING_ID.md` — proof the app does not use the advertising ID. **Read before answering Play's declaration**; the console says "Yes", wrongly.
- `DESIGN_SYSTEM.md` — tokens and components. **Read before adding UI**; paths point at `ui-android/src/androidMain/`, the only source set `:ui-android` has.
- `CODE_QUALITY.md` — the two halves of the convention: detekt's real thresholds and its blind spots, and what only a reviewer can judge. **Read before adding a lint rule, a `@Suppress`, or a use case.**
- `WORKFLOW.md` — the writer/reviewer loop, the reinforced gate, the model-tier policy. **Required
  before any unit of work.** `swiftui/PLAN.md` — the 11 iOS slices and their status.
- `PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `POST_V1_PLAN.md` — the product definition ADR 001
  amends by row id, plus unstarted growth work. **Read before scoping a feature.**
- `archive/` — closed tracks kept for the reasoning.

Latest release tag `v2.4.0`: built but NOT on the alpha track — its Play upload was rejected
(`PLAY_ADVERTISING_ID.md`). `pre-kmp` is the rollback point before the KMP migration.
