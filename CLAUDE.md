# CLAUDE.md

Per-module guidance lives in each module's own `CLAUDE.md`; Claude loads it automatically when working in that module.

> **KMP everywhere; each platform owns its UI.** Android renders Compose (`:ui-android`); iOS is a
> native SwiftUI app over the `JustChillKit` framework `:presentation` exports with SKIE
> ([ADR 005](docs/adr/005-native-swiftui-ios-over-the-kmp-core.md); slices in `docs/swiftui/PLAN.md`).
> Keep the iOS compile in every gate run — it is the only thing stopping the exported core from
> silently filling with `java.*`. iOS runs the same writer+reviewer loop as everything else — see
> `docs/WORKFLOW.md`. Android-only *capabilities* may live in `:androidApp`, but their platform-neutral
> *logic* stays in the KMP core.
>
> **No third-party users on either platform, but the author runs the release build daily** via
> Firebase App Distribution, on a device holding real accumulated data — UI and navigation are cheap
> to redo, a destructive migration is not.

## Build & Development Commands

```bash
./gradlew assembleDevDebug      # dev debug build (most common during dev)
./gradlew assembleProdRelease   # production signed release
./gradlew qualityGate           # THE gate — see Gotchas

# Unit tests (JVM host tests — no device)
./gradlew test                             # everything
./gradlew :domain:testAndroidHostTest      # also :data:, :presentation:, :ui-android:
./gradlew :androidApp:testDevDebugUnitTest # the MockK ViewModel suite lives here
./gradlew :domain:testAndroidHostTest --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"

# iOS
./gradlew :presentation:compileKotlinIosSimulatorArm64      # proves zero java.*/android.* leak
./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64 # links JustChillKit + runs SKIE
open iosApp/iosApp.xcodeproj                                # run the SwiftUI app from Xcode
```

KMP host-test tasks go `UP-TO-DATE` across sessions — `--rerun` forces a real run, and it is **per-task**:
with several tasks in one invocation it forces only the task it follows. There is no `:domain:test` and
no `connectedDevDebugAndroidTest`; both died with the KMP migration.

## Project Layout

- Modules in `settings.gradle.kts`: `:androidApp`, `:ui-android`, `:presentation`, `:domain`, `:data`.
- Java toolchain 17 everywhere. `compileSdk = 37`. `minSdk = 28` (`:androidApp`, `:ui-android`, `:presentation`) / `26` (`:domain`, `:data`).
- `iosApp/` — Xcode project consuming `JustChillKit`. `supabase/` — CLI migrations for the server schema.
- Two `tier` flavors (`dev` / `prod`), `:androidApp` only. Signing, Crashlytics-per-flavor and the
  deliberately-absent Firebase Analytics are in `androidApp/CLAUDE.md`.

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

The app is **local-first**: SQLDelight on-device is the single source of truth and the app is fully
usable with no account and no network. **Sync is switched OFF in production since 2026-08-12**
(`SYNC_TEMPORARILY_DISABLED`, `core/sync/SyncKillSwitch.kt:16`), under redesign as **backup only, one
device at a time** ([ADR 006](docs/adr/006-sync-is-backup-only-one-device-at-a-time.md), superseding
001's multi-device premise). **Read `docs/sync/AUDIT.md` before touching anything under
`data/.../sync/` or `presentation/.../core/sync/`.**

**Data flow:** `Screen` → `ViewModel` → use case → `Repository` interface → `Default{Entity}Repository` → `LocalDataSource` (SQLDelight). The use case is there **only where there is domain logic** — a pure read goes from `ViewModel` straight to the `Repository` interface. Rationale + the measurement: `docs/CODE_QUALITY.md`.

### Contracts that span modules

- **MVI** — `MviViewModel<S, I, E>` plus every ViewModel/UiState/Intent/Effect live in `:presentation` (`core/mvi/`); the Screens that consume them live in `:ui-android`.
- **Errors** — sealed `DomainException` (`:domain/shared/error/`) → `:data/shared/SafeCall.kt` translates
  SQLDelight exceptions into it → `:presentation/core/error/DomainExceptionExt.kt` turns it into a
  Spanish message via `toUserMessage()`. Add failure modes by extending `DomainException`, never by
  introducing a new exception type.

## Testing

JUnit4 + MockK + `kotlinx-coroutines-test` as JVM host tests (`androidHostTest`); `:domain` use cases are
the primary surface. Two suites are the ONLY net for their failure mode — a missing Koin binding
(`AppGraphKoinTest`, in `:presentation`) and a missing schema migration (the instrumented `:data` tests,
not on the default gate). Both are documented where they live.

## Delegation

| Tier | Role |
|---|---|
| Haiku | Runs and reports — gate/tests/builds/git, zero judgment |
| Sonnet | Writes from a decision already made — docs, mechanical refactors, tests from a spec |
| Opus | Decides — architecture, ADRs, plans, reviewers, judges, and code where nothing else catches the error |
| Main thread | Decides, delegates, verifies conclusions — never reads raw tool output |

Tiebreaker: Sonnet writes where the compiler/a test catches the error; Opus writes where nothing does
(that "nothing" list is `## Gotchas` below). The reviewer is always Opus, no exception — the Sonnet carve-out belongs only to Judgment Day's judges, for low blast radius. `model` passed explicitly every time, never relying on agent-file frontmatter. Loop + reasoning: `docs/WORKFLOW.md`.

## Gotchas

This is the Opus list (`docs/WORKFLOW.md` model-tier policy) — a Sonnet writer does not write here.

- **`./gradlew qualityGate` is the gate.** One definition, in `build-logic/.../QualityGateConventionPlugin.kt`;
  the pre-push hook and all three workflows invoke it. detekt over every source set holding code, the host
  test suites, dev lint, (macOS only) the iOS compile, and `:build-logic:test` — named explicitly on the
  root project, because an included build is unreachable by task-name matching. Change the plugin, not the callers. **Never
  gate on plain `./gradlew detekt`** — it is `NO-SOURCE` on all three KMP modules and only lints `:androidApp`.
- **Third-party actions in `.github/` are pinned to a commit SHA on purpose** — they hold the signing key,
  the Firebase credentials and the Play service account, and a floating `@v1` can be repointed by whoever
  owns the upstream repo. Do not "tidy" them into tags; dependabot proposes the bumps. GitHub's own
  `actions/*` stay on tags — they already own the runner and the secret store.
- **`versionName` is `git describe --match "v[0-9]*"`, and the filter is load-bearing.** The repo carries
  non-release tags (`pre-kmp`, `post-s5`, `pre-redesign`) and a bare `describe` returns the nearest one — that is how builds shipped `versionName = "pre-kmp"`. Same filter in `/release`.
- **A tag push does not ship.** `uploadRelease.yml` uploads the AAB to the alpha track as a **draft**; publishing it is manual in Play Console. A green workflow reached no one.
- **`run:` blocks take secrets through `env:`**, never `${{ }}` spliced into the script text. Validate workflow edits with `actionlint` — it catches errors a YAML parse cannot.
- **Every route the nav host can push MUST be `@Serializable`.** `AppNavHost` uses the reflective 1-arg
  `rememberNavBackStack`, which re-resolves each entry via `Class.forName(name).kotlin.serializer()` —
  miss the annotation and the app dies on process-death restore and nowhere else, invisible to the
  compiler. `RouteSerializationTest` round-trips every sealed `AppRoute` through that same serializer.

## Tooling Versions

Trust the source, not a version list here: `gradle/libs.versions.toml` (AGP under the `androidApplication`
key) and `gradle/wrapper/gradle-wrapper.properties`. Compose Multiplatform is **gone** — `:ui-android`
renders on Google's Compose under the BOM and the CMP Gradle plugin is applied nowhere; `:presentation`
still uses JetBrains' multiplatform `lifecycle-viewmodel`, which has to compile for iOS.

## Docs map (`docs/`)

- `PROGRESS.md` — "where are we now" plus the single open-work checklist (there is no tech-debt list here;
  sync debt lives in `sync/AUDIT.md`). **Read it first.**
- `sync/AUDIT.md` — the consolidated sync audit. **Read before touching sync**, and before assuming
  anything about the Supabase project. `sync/PLAN.md` — the original slice plan, **paused**.
- `adr/` — filenames state the decision. 004 amends 002; 005 supersedes 003's frozen-UI scope; 006
  supersedes 001's multi-device premise and leaves 004 dormant; 007 amends 003's point 5 (writer + reviewer, repo-wide); 008 moves the category/type invariant into the schema. **Read before changing anything an ADR decided** — ADRs are amended by a new ADR, never rewritten.
- `DATE_AUDIT.md` — the 13 date findings, all closed. **Read before touching dates.** Live rule #7:
  whatever asks "what day/month is it" takes an injected `Clock` **and** `TimeZone`, **neither carrying a
  default** (`hh/di/SharedModule.kt` is the only way in). Follow-up: #5 phase two.
- `PLAY_ADVERTISING_ID.md` — proof the app does not use the advertising ID. **Read before answering Play's declaration**; the console says "Yes", wrongly.
- `DESIGN_SYSTEM.md` — tokens and components. **Read before adding UI**; paths point at `ui-android/src/androidMain/`, the only source set `:ui-android` has.
- `CODE_QUALITY.md` — the two halves of the convention: detekt's real thresholds and its blind spots, and what only a reviewer can judge. **Read before adding a lint rule, a `@Suppress`, or a use case.**
- `WORKFLOW.md` — the writer/reviewer loop, the reinforced gate, the model-tier policy. **Required before
  any unit of work**, not just KMP. `archive/kmp/ORCHESTRATION.md` — the closed KMP slice ledger +
  landmines, historical. `swiftui/PLAN.md` — the 11 iOS slices and their status.
- `PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `POST_V1_PLAN.md` — the product definition ADR 001 amends by row id, plus unstarted growth work. **Read before scoping a feature.**
- `archive/` — closed tracks kept for the reasoning.

Latest tags: `v2.4.0`, `pre-kmp` (rollback point before the KMP migration). `v2.4.0` is tagged and built
but has NOT reached the alpha track — its Play upload was rejected, see `PLAY_ADVERTISING_ID.md`.
