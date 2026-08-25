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

KMP host-test tasks go `UP-TO-DATE` across sessions — `--rerun` forces a real run, **per-task**. There is
no `:domain:test`, and no instrumented source set outside `:data`.

## Project Layout

- Toolchain, SDK levels and versions: `build-logic/.../BuildConventions.kt` +
  `gradle/libs.versions.toml` — `minSdk` disagrees per module on purpose. Flavors `dev`/`prod` on
  `:androidApp` only; signing, Crashlytics and the absent Analytics: `androidApp/CLAUDE.md`.
- Compose Multiplatform is **gone** — `:ui-android` renders Google's Compose under the BOM;
  `:presentation` still uses JetBrains' `lifecycle-viewmodel`, which has to compile for iOS.

## Architecture

Clean Architecture, five modules, four of them KMP. Dependency direction is top to bottom:

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
with **snapshot backup**; the engine is gone, the sync schema stays. **Read
`docs/work/epics/E01-snapshot-backup.md` before touching `data/src/**/backup/`.**

**Data flow:** `Screen` → `ViewModel` → use case → `Repository` interface → `Default{Entity}Repository` → `LocalDataSource` (SQLDelight). The use case is there **only where there is domain logic** — a pure read goes from `ViewModel` straight to the `Repository` interface. Rationale + the measurement: `docs/CODE_QUALITY.md`.

### Contracts that span modules

- **MVI** — `MviViewModel<S, I, E>` and every ViewModel/UiState/Intent/Effect live in `:presentation`
  (`core/mvi/`); the Screens consuming them live in `:ui-android`.
- **Errors** — sealed `DomainException` (`:domain/shared/error/`) → `SafeCall.kt` translates SQLDelight
  exceptions into it → `DomainExceptionExt.kt` renders the Spanish message. Add failure modes by
  extending `DomainException`, never a new exception type.

## Testing

JUnit4 + MockK + `kotlinx-coroutines-test` as JVM host tests (`androidHostTest`); `:domain` use cases
are the primary surface. Two failure modes have one net each: a missing Koin binding, caught by a host
test (`presentation/CLAUDE.md`), and a missing migration — compiled by the gate, run only on a device.

## Delegation

Tiers, the tiebreaker, the Judgment Day carve-outs and the `model`-passing rule live in
`docs/WORKFLOW.md` `## Model tier policy`. **Read it before delegating anything.**

## Writer conventions (they reach every subagent through this file)

- **Comments climb a ladder, in order: delete → rename → redesign → comment.** In a test file the test
  method name IS the rename rung. A surviving comment names a constraint the code cannot show — one
  comment, one fact. Full rule: `docs/CODE_QUALITY.md`.
- **English for every identifier; Spanish only in user-data VALUES.** `name = "Sueldo"` is data;
  `val sueldo` is a violation — name fixture locals by role (`incomeCategory`).
- **That Spanish addresses the reader as `tú`, never `vos`.** Tuteo is the house register; a voseo
  string is a defect even when it reads well. A test can pin the wrong one — `DeleteCategoryCopyTest`
  did — so grep the expectation, not just the source.

## Gotchas

This is the Opus list (`docs/WORKFLOW.md` model-tier policy) — a Sonnet writer does not write here.

- **`./gradlew qualityGate` is the gate.** One definition — `build-logic/.../QualityGateConventionPlugin.kt`,
  invoked by the pre-push hook and all three workflows: detekt over every module source set holding
  code, the host test suites, dev lint, (macOS) the iOS compile, plus `:build-logic:test` named
  explicitly (an included build is unreachable by task-name matching; its sources are the one code the
  gate runs and never lints). Change the plugin, not the callers. **Never gate on plain
  `./gradlew detekt`** — `NO-SOURCE` on all four KMP modules; it only lints `:androidApp`.
- **Third-party actions in `.github/` are pinned to a commit SHA on purpose** — they hold the signing
  and Play/Firebase credentials, and a floating `@v1` can be repointed upstream. Do not "tidy" them
  into tags; dependabot proposes bumps. GitHub's own `actions/*` stay on tags.
- **`versionName` is `git describe --match "v[0-9]*"`, and the filter is load-bearing** — the repo
  carries non-release tags and a bare `describe` returns the nearest one (builds once shipped
  `versionName = "pre-kmp"`). Same filter in `/release`.
- **A tag push does not ship.** `uploadRelease.yml` uploads the AAB to the alpha track as a **draft**; publishing it is manual in Play Console. A green workflow reached no one.
- **`run:` blocks take secrets through `env:`**, never `${{ }}` spliced into the script text. Validate workflow edits with `actionlint` — it catches errors a YAML parse cannot.
- **Every route the nav host can push MUST be `@Serializable`.** Android's reflective
  `rememberNavBackStack(vararg NavKey)` re-resolves each entry via `Class.forName(name).kotlin.serializer()`
  — miss the annotation and the app dies only on process-death restore, invisible to the compiler.
  `RouteSerializationTest` round-trips every sealed `AppRoute` through that serializer.

## Docs contract

Six doc types, one job and a death rule each: **`CLAUDE.md`** — how to work here, ≤150 lines, pointers
only. **`adr/`** — one decision per ADR, target 1 page, ceiling 2 **at writing time**; a published ADR is
never trimmed to fit, only amended by a new ADR.
**One live plan per track** — ONLY what remains; closing a unit removes it from the plan in the same
commit. **`PROGRESS.md`** — the orientation page: where the app stands, how to check it, untaken ideas. **Reference docs**
(`WORKFLOW`, `CODE_QUALITY`, `DESIGN_SYSTEM`) — timeless conventions, zero history. **`archive/`** —
the reasoning of closed work. The chronicle lives in git and engram, never in a live doc; every live
doc has a read-trigger in the map below, and a doc with no trigger is archive.

## Docs map (`docs/`)

- `PROGRESS.md` — where the app stands, how to verify that from a shell, and the ideas nobody has
  taken. It holds no work list: every committed unit is a ticket in `docs/work/`. **Read it first.**
- `work/epics/E01-snapshot-backup.md` — the sync/backup epic: constraints outliving every ticket
  under it, remaining work in `work/backlog/`. **Read before touching backup.**
- `work/epics/E02-migration-coverage.md` — the coverage invariant; `PERSISTENCE.md` — the schema, the
  migration obligation and the test mechanics. **Read both before touching a `.sq`, a `.sqm` or a migration test.**
- `work/README.md` — the board's rules: directory-is-status, immutable IDs, ceilings. **Read before
  opening, taking or closing a ticket.**
- `adr/` — filenames state the decision; each header declares what it amends or supersedes. 009 is
  the one to read first for anything sync-shaped. **Read before changing anything an ADR decided.**
- `PLAY_ADVERTISING_ID.md`, `PLAY_STORE_LISTING.md`, `PRIVACY_POLICY.md` — the store-facing set.
  **Read before a Play submission or a privacy change**; the advertising-ID answer is "No", and a
  still-active release in ANY track can fail it even when the bundle being uploaded is clean.
- `DESIGN_SYSTEM.md` — the criteria: which token to reach for and why, never its value. **Read before adding UI.**
- `RELEASE_CHECKLIST.md` — the ordered gate a release passes, including the ADR 009 restore drill.
  **Read before tagging a release.**
- `CODE_QUALITY.md` — detekt's thresholds and blind spots, what only a reviewer can judge, and the date
  rule (injected `Clock` **and** `TimeZone`, no defaults). **Read before a lint rule, a `@Suppress`, a use case, or a date.**
- `WORKFLOW.md` — the writer/reviewer loop, the reinforced gate, the model-tier policy. **Required
  before any unit of work.** `swiftui/PLAN.md` — the iOS slices that remain.
- `PRODUCT_REQUIREMENTS.md` — the Won't-have rows (ADRs amend them **by row id**), the NFRs and the
  acceptance criterion. **Read before scoping a feature.**
- `archive/` — closed tracks kept for the reasoning. Release state lives in `PROGRESS.md`;
  `pre-kmp` is the rollback point before the KMP migration.
