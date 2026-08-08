# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Per-module guidance lives in `domain/CLAUDE.md`, `data/CLAUDE.md`, `shared-ui/CLAUDE.md`, and
`androidApp/CLAUDE.md`; Claude loads each one automatically when working in that module.

> **KMP / Compose Multiplatform: migrated and merged to trunk.** All four modules are KMP;
> `shared-ui` holds the shared Compose UI for Android and iOS. `docs/kmp/ORCHESTRATION.md` is the
> canonical workflow for any further shared-UI slice — read it first; its ledger, landmines and
> reinforced gate are current. `PHASE_3_SPEC.md` / `MIGRATION_PLAN.md` are historical and contain
> superseded decisions (the "Option A" nav split was reversed).
>
> **iOS is frozen, not closed** — [ADR 003](docs/adr/003-freeze-ios-keep-the-compile-gate.md). It
> compiles; nothing beyond that is claimed. Keep `:shared-ui:compileKotlinIosSimulatorArm64` in
> every gate run — 12.9s, and the only thing stopping `commonMain` from silently filling with
> `java.*`. The per-slice writer + reviewer (both Opus) ritual is retired: one writer, review
> inline. Android-only *capabilities* may live in `:androidApp`, but their platform-neutral *logic*
> stays in `commonMain`. **There are no users on either platform** — see `docs/PROGRESS.md`.

## Build & Development Commands

```bash
# Build
./gradlew assembleDevDebug              # Dev debug build (most common during dev)
./gradlew assembleProdRelease           # Production signed release

# Unit tests (JVM host tests — no device)
./gradlew test                             # Everything
./gradlew :domain:testAndroidHostTest      # also :data: and :shared-ui: — fastest feedback
./gradlew :androidApp:testDevDebugUnitTest # the MockK ViewModel suite lives here
./gradlew :domain:testAndroidHostTest --tests "com.emm.domain.transaction.CreateTransactionUseCaseTest"

# iOS
./gradlew :shared-ui:compileKotlinIosSimulatorArm64   # proves zero java.*/android.* leak
open iosApp/iosApp.xcodeproj                          # run the iOS app from Xcode
```

KMP host-test tasks go `UP-TO-DATE` across sessions — add `--rerun` to force a real run.
There is no `:domain:test` and no `connectedDevDebugAndroidTest`; both died with the KMP migration.

## Project Layout

- Modules in `settings.gradle.kts`: `:androidApp`, `:shared-ui`, `:domain`, `:data`.
- Java toolchain 17 everywhere. `compileSdk = 37`. `minSdk = 28` (`:androidApp`, `:shared-ui`) /
  `26` (`:domain`, `:data`).
- `iosApp/` — Xcode project consuming `shared-ui`. `supabase/` — CLI migrations for the server schema.
- Two product flavors on dimension `tier` (`:androidApp` only):
  - `dev` — `applicationIdSuffix = ".dev"`.
  - `prod` — release signing via `keystore.properties`, Firebase Crashlytics (dev disables
    collection via manifest meta-data; Firebase Analytics is NOT used, only declared in the catalog).

## Architecture

Clean Architecture, four KMP modules. Dependency direction is top to bottom:

| Module | Role | Root package |
|---|---|---|
| `:androidApp` | thin Android entry point (Activity, platform Koin module) | `com.emm.justchill.*` |
| `:shared-ui` | Compose Multiplatform UI + ViewModels + Koin wiring | `com.emm.justchill.{hh.<feature>, core, components}` |
| `:data` | implements domain interfaces (commonMain/androidMain/iosMain) | `com.emm.data.<entity>` |
| `:domain` | pure Kotlin, no framework deps | `com.emm.domain.<entity>` |

The app is **local-first**: SQLDelight on-device is the single source of truth and the app is fully
usable with no account and no network. Optional multi-device sync via Supabase (opt-in
email/password or Google sign-in, LWW) — slices 1-4 are done and device-verified; slice 5
(compliance + release gate) is in progress. See `docs/sync/PLAN.md`, `docs/adr/001`, `docs/adr/002`.

`shared-ui/commonMain` depending on `:data` is deliberate (slice H) — it lets the Koin wiring exist
once instead of per platform. The cost: ViewModel purity (VMs take `:domain` interfaces, never
SQLDelight or `Default*` types) is now **convention only**, no longer enforced by the module graph.

### Data flow

`Screen` collects `StateFlow<UiState>` from `ViewModel` → `ViewModel` calls a domain use case →
use case calls a `Repository` interface → `Default{Entity}Repository` delegates to a
`LocalDataSource` (SQLDelight).

### MVI pattern (`shared-ui/commonMain/core/mvi/`)

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
- `:shared-ui/core/error/DomainExceptionExt.kt` maps each subtype to a user-facing Spanish string
  via `DomainException.toUserMessage()`.

When adding a new failure mode, prefer extending `DomainException` (and `toUserMessage`) over
introducing a new exception type.

## Testing (cross-module)

- JUnit4 + MockK + `kotlinx-coroutines-test`, running as JVM host tests (`androidHostTest`).
- Domain use-case tests are the primary unit-test surface. They use `runTest`, `mockk()`,
  `coEvery`, `coVerify`.
- `shared-ui/androidHostTest/core/AppGraphKoinTest.kt` resolves the whole Koin graph off-device.
  A missing binding compiles clean and passes `assembleDevDebug` — this test is the only thing
  that catches it before a user does. Keep it green.
- Instrumented tests live in `data/src/androidDeviceTest/` (16 tests, schema migrations + FK
  behaviour). Run with `./gradlew :data:connectedAndroidDeviceTest` on a device or emulator. They
  are not part of the default gate — run them before shipping a schema change.

## Gotchas

- **`./gradlew qualityGate` is the gate.** One definition, in
  `build-logic/.../QualityGateConventionPlugin.kt`; the pre-push hook and all three workflows
  invoke it. detekt over every source set that holds code, the host test suites, dev lint, and
  (on macOS only) the iOS compile. Change the plugin, not the callers.
- **Never gate on plain `./gradlew detekt`** — it is `NO-SOURCE` on all three KMP modules and only
  lints `:androidApp`.
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
- `adr/` — 001 local-first reversal, 002 pull cursor, 003 iOS frozen (compile gate only).
  `sync/PLAN.md` — sync slices.
- `PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `ROADMAP_V1.md`, `POST_V1_PLAN.md` — Fases 1-5.
- `DESIGN_SYSTEM.md` — tokens and components (its paths still point at the pre-KMP `app/` module).
- `archive/` — closed tracks kept for history.
- `PROGRESS.md` — canonical "where are we now". Rewritten 2026-08-08; pre-KMP sprint detail is in
  git history, not in the file.

Latest tags: `v2.2.0`, `pre-kmp` (rollback point before the KMP migration).

Use case naming convention: **`[Verb][Noun]UseCase`** (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`).
