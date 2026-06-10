# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Per-module guidance lives in `domain/CLAUDE.md`, `data/CLAUDE.md`, and `app/CLAUDE.md`; Claude loads each one automatically when working in that module.

## Build & Development Commands

```bash
# Build
./gradlew assembleDevDebug              # Dev debug build (most common during dev)
./gradlew assembleProdRelease           # Production signed release

# Unit tests
./gradlew test                          # All unit tests across all modules
./gradlew :domain:test                  # Domain-only (fastest, no Android)
./gradlew testDevDebugUnitTest          # Tests for the dev+debug variant
./gradlew :domain:test --tests "com.emm.domain.transaction.TransactionCreatorTest"   # Single test

# Instrumented tests (requires device/emulator)
./gradlew connectedDevDebugAndroidTest

# Clean
./gradlew clean
```

## Project Layout

- Gradle modules included in `settings.gradle.kts`: `:app`, `:domain`, `:data`.
- Java toolchain 17 across all modules. `compileSdk = 36`. `minSdk = 28` (`:app`) / `26` (`:data`).
- Two product flavors on dimension `tier`:
  - `dev` — `applicationIdSuffix = ".dev"`.
  - `prod` — release signing via `keystore.properties`, Firebase Analytics + Crashlytics.

## Architecture

Clean Architecture, three modules:

```
:domain  →  pure Kotlin JVM lib, no Android deps        (java-library + kotlin.jvm)
:data    →  Android lib, implements domain interfaces   (SQLDelight + Supabase sync/auth)
:app     →  Compose UI, ViewModels, Koin DI wiring      (android-application)
```

The app is **local-first**: SQLDelight on-device is the single source of truth and the app is fully usable with no account and no network. Optional multi-device sync via Supabase (opt-in email/password sign-in, LWW) is **in progress** — slices 1-3 are on trunk: soft-delete + sync metadata (slice 1), auth + claim-on-sign-in (slice 2), and the manual-trigger sync engine (slice 3, push/pull + cursor — see `data/CLAUDE.md`). Remaining: automatic sync lifecycle (slice 4) and compliance/release gate (slice 5), per `docs/sync/PLAN.md`. Decisions in `docs/adr/001` and `docs/adr/002`.

**Dependency direction**: `:app` → `:domain`, `:data`; `:data` → `:domain`; `:domain` has no module deps.

### Package roots

| Module | Root package |
|---|---|
| `:domain` | `com.emm.domain.<entity>` |
| `:data` | `com.emm.data.<entity>` |
| `:app` | `com.emm.justchill.{hh.<feature>, core, components}` |

### Data flow

`Screen` collects `StateFlow<UiState>` from `ViewModel` → `ViewModel` calls a domain use case → use case calls a `Repository` interface → `Default{Entity}Repository` delegates to a `LocalDataSource` (SQLDelight).

### MVI pattern (`app/core/mvi/`)

All ViewModels extend `MviViewModel<S : UiState, I : UiIntent, E : UiEffect>` from `app/core/mvi/`. The base class provides:

- `state: StateFlow<S>` — collected in the Screen with `collectAsStateWithLifecycle()`
- `effect: Flow<E>` — one-shot side-effects (navigation, snackbars) collected in `LaunchedEffect(vm) { vm.effect.collect { } }`
- `updateState(reducer: S.() -> S)` — atomic state update
- `sendEffect(effect: E)` — fires a one-shot effect
- `abstract fun onIntent(intent: I)` — single entry point for user actions

Per feature, create three files alongside the ViewModel:
- `XxxUiState.kt` — `data class` implementing `UiState`, no navigation flags or message strings
- `XxxIntent.kt` — `sealed interface` implementing `UiIntent`
- `XxxEffect.kt` — `sealed interface` implementing `UiEffect` (navigation targets, `ShowError`)

`SnackbarHostState` lives in the root `Scaffold` in `Hh.kt` and is passed down to each Screen that needs it.

### Error model (cross-module)

- Sealed `DomainException` in `:domain/shared/error/` with subtypes: `NotFound`, `ValidationError`, `NetworkUnavailable`, `DatabaseError`, `Unauthorized`, `Unknown`.
- `:data/shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into `DomainException`. Repositories should funnel I/O through it instead of throwing raw SQLDelight errors.
- `:app/core/error/DomainExceptionExt.kt` maps each subtype to a user-facing Spanish string via `DomainException.toUserMessage()`.

When adding a new failure mode, prefer extending `DomainException` (and `toUserMessage`) over introducing a new exception type.

## Testing (cross-module)

- JUnit4 + MockK + `kotlinx-coroutines-test` across all modules.
- Domain use-case tests under `domain/src/test/` are the primary unit-test surface (no Android = fast). They use `runTest`, `mockk()`, `coEvery`, `coVerify`.
- Instrumented tests live in `data/src/androidTest/` and `app/src/androidTest/`; reserve them for behaviour that genuinely depends on the Android runtime.

## Tooling Versions

- Kotlin `2.3.21` (Compose plugin matches).
- AGP `9.2.1` (built-in Kotlin).
- Gradle wrapper `9.5.1`.
- Koin `4.2.x` (via BOM), SQLDelight `2.3.2`, Compose BOM `2026.05.x`.

## Refactor history

> Refactoring work prior to the product-definition Fases 1-5. Listed
> here as background for code archaeology — for current execution
> state read `docs/PROGRESS.md`.

**Done:**
- **Use-case rename**: All use cases follow `[Verb][Noun]UseCase`. See `domain/CLAUDE.md`.
- **Typed errors**: `DomainException` + `SafeCall` + `toUserMessage()`.
- **MVI**: All ViewModels extend `MviViewModel<S, I, E>`. `launchSafe { }` covers try/catch in the base class.
- **Local-only migration**: removed auth/Supabase/Ktor/WorkManager; SQLDelight schema reset (no `syncState`/`isDeleted`/`userId`); hard-delete with `ON DELETE` foreign keys.
- **Cleanup pass**: dropped dead Retrofit/parcelize/viewBinding; `@Immutable` on `TransactionUi`/`CategoryUi`; current-month filtering pushed to SQL (`completeTransactionsByDateRange`); `AddTransactionScreen`/`EditTransaction` decomposed into shared `TransactionFormSections.kt`; unit tests for all transaction, category and home use cases.

**Ad-hoc tech debt still open** (not blocking v1, picked up opportunistically):
- Compose perf pass: audit `derivedStateOf`, `remember`-ed lambdas, `contentType` in `LazyColumn`, Layout Inspector for overdraw.
- Orphan deps in `libs.versions.toml` (Ktor/Retrofit/Supabase/WorkManager declarations without uses).

## Product definition (Fases 1-5) + execution status

> **Read `docs/PROGRESS.md` first** if you're resuming this project
> after a context reset. It's the canonical "where are we now" doc.

The product definition (Fases 1-5) lives in `docs/`:

- `PRODUCT_DISCOVERY.md` — persona, manifesto, positioning, competence map.
- `PRODUCT_REQUIREMENTS.md` — 15 Must / 12 Won't user stories with acceptance criteria.
- `ROADMAP_V1.md` — 8 sprints to Play Store alpha.
- `POST_V1_PLAN.md` — 12-month funnel, monetization paths, pivot triggers.
- `DESIGN_SYSTEM.md` — tokens, components, screen specs.
- `PROGRESS.md` — current execution state, rollback points, next concrete step.
- `adr/` — architecture decision records (001 local-first reversal, 002 pull cursor).
- `archive/` — closed-track docs kept for history (ARCHITECTURE_REVIEW, DESIGN_BRIEF, PLAN_REDESIGN).

Rollback tags: `pre-s0` (before execution started), `post-s0` (after
9 quick wins). Next expected: `post-s1` after manual device verification.

Use case naming convention: **`[Verb][Noun]UseCase`** (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`).
