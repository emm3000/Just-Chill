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
:data    →  Android lib, implements domain interfaces   (SQLDelight, local-only)
:app     →  Compose UI, ViewModels, Koin DI wiring      (android-application)
```

The app is **100% local**: no auth, no remote backend, no sync. All persistence is SQLDelight on-device.

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
- Koin `4.2.x` (via BOM), SQLDelight `2.3.2`, Retrofit `3.0.0`, Compose BOM `2026.05.x`.

## Ongoing Refactor

The app was migrated to **local-only** via `docs/PLAN_LOCAL.md` (auth, Supabase, Ktor, sync, WorkManager all removed). `docs/PLAN_DE_ACCION.md` and `docs/PLAN_SONNET.md` are retained for historical context; their sync-related phases are N/A.

**Done:**
- **Fase 1 (use-case rename)**: All use cases follow `[Verb][Noun]UseCase`. See `domain/CLAUDE.md`.
- **Fase 3 (typed errors)**: `DomainException` + `SafeCall` + `toUserMessage()`.
- **Fases 4 + 5 (MVI)**: All ViewModels extend `MviViewModel<S, I, E>`.
- **Local-only migration (PLAN_LOCAL)**: removed auth/Supabase/Ktor/WorkManager; SQLDelight schema reset (no `syncState`/`isDeleted`/`userId`); hard-delete with `ON DELETE` foreign keys.

**Partial / Pending:**
- **Fase 2 (data models)**: mappers are clean, `*Entity` data classes exist.
- **Fase 6 (Compose perf)**: pending `derivedStateOf`, `remember`-ed lambdas, `contentType`, Layout Inspector audit.
- **Fase 7 (SOLID/cleanup)**: DI per feature in place; pending OCP audit and final naming sweep.
- **Legacy tables**: `drivers`, `dailies`, `loans`, `payments` still need to be resolved.

Use case naming convention: **`[Verb][Noun]UseCase`** (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`).
