# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Per-module guidance lives in `domain/CLAUDE.md`, `data/CLAUDE.md`, `presentation/CLAUDE.md`,
`ui-android/CLAUDE.md`, and `androidApp/CLAUDE.md`; Claude loads each one automatically when working
in that module.

> **KMP everywhere; each platform owns its UI.** Android renders Compose (`:ui-android`); iOS is a
> native SwiftUI app over the `JustChillKit` framework that `:presentation` exports with SKIE —
> [ADR 005](docs/adr/005-native-swiftui-ios-over-the-kmp-core.md) unfroze iOS as a *learning
> track* (supersedes ADR 003's frozen-UI scope; the compile-gate invariant survives, relocated).
> The slice plan and ledger live in `docs/swiftui/PLAN.md`. `docs/kmp/ORCHESTRATION.md` keeps the
> KMP landmines and gate rationale; `PHASE_3_SPEC.MD` / `MIGRATION_PLAN.md` are historical.
>
> Keep the iOS compile (`:presentation:compileKotlinIosSimulatorArm64`, plus `:domain`/`:data`) in
> every gate run — it is the only thing stopping the exported core from silently filling with
> `java.*`. One writer per slice, review inline. Android-only *capabilities* may live in
> `:androidApp`, but their platform-neutral *logic* stays in the KMP core (ADR 003 constraint 8,
> alive in ADR 005). **There are no users on either platform** — see `docs/PROGRESS.md`.

## Build & Development Commands

```bash
# Build
./gradlew assembleDevDebug              # Dev debug build (most common during dev)
./gradlew assembleProdRelease           # Production signed release

# Unit tests (JVM host tests — no device)
./gradlew test                             # Everything
./gradlew :domain:testAndroidHostTest      # also :data:, :presentation:, :ui-android:
./gradlew :androidApp:testDevDebugUnitTest # the MockK ViewModel suite lives here
./gradlew :domain:testAndroidHostTest --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"

# iOS
./gradlew :presentation:compileKotlinIosSimulatorArm64      # proves zero java.*/android.* leak
./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64 # links JustChillKit + runs SKIE
open iosApp/iosApp.xcodeproj                                # run the SwiftUI app from Xcode
```

KMP host-test tasks go `UP-TO-DATE` across sessions — add `--rerun` to force a real run, and
`--rerun` is **per-task**: with several tasks in one invocation it forces only the task it follows.
There is no `:domain:test` and no `connectedDevDebugAndroidTest`; both died with the KMP migration.

## Project Layout

- Modules in `settings.gradle.kts`: `:androidApp`, `:ui-android`, `:presentation`, `:domain`, `:data`.
- Java toolchain 17 everywhere. `compileSdk = 37`. `minSdk = 28` (`:androidApp`, `:ui-android`,
  `:presentation`) / `26` (`:domain`, `:data`).
- `iosApp/` — Xcode project: SwiftUI app consuming `:presentation`'s `JustChillKit` framework.
  `supabase/` — CLI migrations for the server schema.
- Two product flavors on dimension `tier` (`:androidApp` only):
  - `dev` — `applicationIdSuffix = ".dev"`.
  - `prod` — release signing via `keystore.properties`, Firebase Crashlytics (dev disables
    collection via manifest meta-data; Firebase Analytics is NOT used, only declared in the catalog).

## Architecture

Clean Architecture, five KMP modules. Dependency direction is top to bottom:

| Module | Role | Root package |
|---|---|---|
| `:androidApp` | thin Android entry point (Activity, platform Koin module) | `com.emm.justchill.*` |
| `:ui-android` | Android-only Compose UI (screens, nav, theme) | `com.emm.justchill.{hh.<feature>, core, components}` |
| `:presentation` | compose-free MVI core, ViewModels, Koin DI, formatters; exports `JustChillKit` (SKIE) to iOS | same packages as `:ui-android` on purpose |
| `:data` | implements domain interfaces (commonMain/androidMain/iosMain) | `com.emm.data.<entity>` |
| `:domain` | pure Kotlin, no framework deps | `com.emm.domain.<entity>` |

The iOS SwiftUI app (`iosApp/`) sits on `:presentation` directly; `:ui-android` sits on it as a
Gradle dependency. Same Kotlin packages across the `:ui-android`/`:presentation` boundary — explicit
imports are required where same-package symbols crossed modules.

The app is **local-first**: SQLDelight on-device is the single source of truth and the app is fully
usable with no account and no network. Optional multi-device sync via Supabase (opt-in
email/password or Google sign-in, LWW) — slices 1-4 are done and device-verified; slice 5
(compliance + release gate) is in progress. See `docs/sync/PLAN.md`, `docs/adr/001`, `docs/adr/002`.

`:presentation` depending on `:data` is deliberate (slice H's layering, inherited by the S1
extraction) — it lets the Koin wiring exist once instead of per platform. ViewModel purity (VMs
take `:domain` interfaces, never SQLDelight or `Default*` types) stays a reviewed convention
inside `:presentation`; what IS structural again since S1 is that ViewModels cannot touch Compose
(`:presentation` has no compose dependency, and the Swift framework would expose the leak).

### Data flow

`Screen` collects `StateFlow<UiState>` from `ViewModel` → `ViewModel` calls a domain use case →
use case calls a `Repository` interface → `Default{Entity}Repository` delegates to a
`LocalDataSource` (SQLDelight).

### MVI pattern (`presentation/commonMain/core/mvi/`)

All ViewModels extend `MviViewModel<S : UiState, I : UiIntent, E : UiEffect>`. The base class provides:

- `state: StateFlow<S>` — collected in the Screen with `collectAsStateWithLifecycle()`
- `effect: Flow<E>` — one-shot side-effects (navigation, snackbars) collected in
  `LaunchedEffect(vm) { vm.effect.collect { } }`
- `updateState(reducer: S.() -> S)` — atomic state update
- `sendEffect(effect: E)` — fires a one-shot effect
- `abstract fun onIntent(intent: I)` — single entry point for user actions

Per feature, alongside the ViewModel: `XxxUiState.kt` (`data class`, no navigation flags or message
strings), `XxxIntent.kt` and `XxxEffect.kt` (`sealed interface`s — navigation targets, `ShowError`).

`SnackbarHostState` lives in the root `Scaffold` of `hh/shared/AppNavHost.kt` (commonMain) and is
passed down to each Screen that needs it.

### Error model (cross-module)

- Sealed `DomainException` in `:domain/shared/error/` with subtypes: `NotFound`, `ValidationError`,
  `NetworkUnavailable`, `DatabaseError`, `Unauthorized`, `Unknown`.
- `:data/shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into
  `DomainException`. Repositories funnel I/O through it instead of throwing raw SQLDelight errors.
- `:presentation/core/error/DomainExceptionExt.kt` maps each subtype to a user-facing Spanish string
  via `DomainException.toUserMessage()`.

When adding a new failure mode, prefer extending `DomainException` (and `toUserMessage`) over
introducing a new exception type.

## Testing (cross-module)

- JUnit4 + MockK + `kotlinx-coroutines-test`, running as JVM host tests (`androidHostTest`).
- Domain use-case tests are the primary unit-test surface. They use `runTest`, `mockk()`,
  `coEvery`, `coVerify`.
- `presentation/androidHostTest/core/AppGraphKoinTest.kt` resolves the whole Koin graph off-device.
  A missing binding compiles clean and passes `assembleDevDebug` — this test is the only thing
  that catches it before a user does. Keep it green.
- Instrumented tests live in `data/src/androidDeviceTest/` (15 tests, schema migrations + FK
  behaviour). Run with `./gradlew :data:connectedAndroidDeviceTest` on a device or emulator. They
  are not part of the default gate — run them before shipping a schema change.

## Gotchas

- **`./gradlew qualityGate` is the gate.** One definition, in
  `build-logic/.../QualityGateConventionPlugin.kt`; the pre-push hook and all three workflows
  invoke it. detekt over every source set that holds code, the host test suites, dev lint, and
  (on macOS only) the iOS compile. Change the plugin, not the callers.
- **Never gate on plain `./gradlew detekt`** — it is `NO-SOURCE` on all three KMP modules and only
  lints `:androidApp`.
- **Third-party actions in `.github/` are pinned to a commit SHA on purpose** — they hold the
  signing key, the Firebase credentials and the Play service account, and a floating `@v1` can be
  repointed at new code by whoever owns the upstream repo. Do not "tidy" them back into tags;
  dependabot proposes the bumps. GitHub's own `actions/*` stay on tags: trusting them is not an
  extra trust decision, they already own the runner and the secret store.
- **`versionName` is `git describe --match "v[0-9]*"`, and the filter is load-bearing.** The repo
  carries non-release tags (`pre-kmp`, `post-s5`, `pre-redesign`) and a bare `describe` returns the
  nearest one — that is how builds shipped `versionName = "pre-kmp"`. Same filter in `/release`.
- **A tag push does not ship.** `uploadRelease.yml` uploads the AAB to the alpha track as a
  **draft**; publishing it is a manual step in Play Console. A green workflow reached no one.
- `run:` blocks take secrets through `env:`, never `${{ }}` spliced into the script text. Validate
  workflow edits with `actionlint` before pushing — it catches expression and input errors that a
  YAML parse cannot.
- Every route the nav host can push MUST be registered in `NavSavedStateConfiguration.kt`
  (commonMain), else `rememberNavBackStack` crashes on process-death restore. Invisible to the compiler.

## Tooling Versions

Kotlin `2.4.0` · AGP `9.2.1` · Gradle wrapper `9.5.1` · Koin BOM `4.2.1` · SQLDelight `2.3.2` ·
Compose BOM `2026.05.01` · Compose Multiplatform `1.11.1` · detekt `2.0.0-alpha.3` · Supabase BOM `3.6.0`.

## Open tech debt

- Compose perf pass: audit `derivedStateOf`, `remember`-ed lambdas, `contentType` in `LazyColumn`.
- `SyncOrchestrator` has no connectivity-regained trigger (only on-resume, sign-in, debounced
  writes), so a sync that fails offline waits for the next `ON_RESUME`. No data loss — just latency.
  Fix: `callbackFlow` over `ConnectivityManager.NetworkCallback.onAvailable`, filtered by
  authenticated + (`pendingCount > 0` or `lastSyncFailed`), injected like `resumeEvents`.

## Docs map (`docs/`)

- `kmp/ORCHESTRATION.md` — shared-UI slice workflow + ledger. Current and trustworthy.
- `adr/` — 001 local-first reversal, 002 pull cursor, 003 iOS frozen (compile gate only),
  004 conflicts are arbitrated only on unpushed edits (amends 002),
  005 native SwiftUI iOS over the KMP core (supersedes 003's frozen-UI scope).
  `sync/PLAN.md` — sync slices. `swiftui/PLAN.md` — iOS SwiftUI slices + ledger.
- `PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `ROADMAP_V1.md`, `POST_V1_PLAN.md` — Fases 1-5.
- `DESIGN_SYSTEM.md` — tokens and components (its paths still point at the pre-KMP `app/` module).
- `archive/` — closed tracks kept for history.
- `PROGRESS.md` — canonical "where are we now". Rewritten 2026-08-08; pre-KMP sprint detail is in
  git history, not in the file.

Latest tags: `v2.2.0`, `pre-kmp` (rollback point before the KMP migration).

Use case naming convention: **`[Verb][Noun]UseCase`** (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`).
