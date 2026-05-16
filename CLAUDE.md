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
  - `dev` — `applicationIdSuffix = ".dev"`, ships JavaFaker (`devDebugImplementation` only) and Chucker (debug only; release uses `library-no-op`).
  - `prod` — release signing via `keystore.properties`, Firebase Analytics + Crashlytics.
- Supabase URL/key are injected per-flavor via `resValue` from `keystore.properties` (not committed).

## Architecture

Clean Architecture, three modules:

```
:domain  →  pure Kotlin JVM lib, no Android deps        (java-library + kotlin.jvm)
:data    →  Android lib, implements domain interfaces   (SQLDelight + Supabase + Ktor)
:app     →  Compose UI, ViewModels, Koin DI wiring      (android-application)
```

**Dependency direction**: `:app` → `:domain`, `:data`; `:data` → `:domain`; `:domain` has no module deps.

### Package roots

| Module | Root package |
|---|---|
| `:domain` | `com.emm.domain.<entity>` |
| `:data` | `com.emm.data.<entity>` |
| `:app` | `com.emm.justchill.{hh.<feature>, core, components, sync}` |

### Data flow

`Screen` collects `StateFlow<UiState>` from `ViewModel` → `ViewModel` calls a domain use case → use case calls a `Repository` interface → `Default{Entity}Repository` coordinates `LocalDataSource` (SQLDelight) and `RemoteDataSource` (Supabase).

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
- `:data/shared/SafeCall.kt` wraps remote/local calls and translates third-party exceptions into `DomainException`. Repositories should funnel I/O through it instead of throwing raw Supabase/SQLDelight errors.
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
- Koin `4.2.x` (via BOM), SQLDelight `2.3.2`, Ktor `3.5.0`, Retrofit `3.0.0`, Supabase `3.6.0` BOM, Compose BOM `2026.05.x`.

## Ongoing Refactor

`docs/PLAN_DE_ACCION.md` tracks a planned migration. Status:

- **Fases 4 + 5 (MVI) — DONE**: All ViewModels use `MviViewModel`. Use `XxxIntent` (not `XxxAction`). See `app/core/mvi/` for the base classes.
- **Fase 1 (use-case rename) — DONE**: All use cases now follow `[Verb][Noun]UseCase` naming. See `domain/CLAUDE.md`.
- **Fases 0, 2–3, 6–7 — pending**: Domain schema redesign, model/mapper cleanup, Compose performance, SOLID audit.

Use case naming convention: **`[Verb][Noun]UseCase`** (e.g. `CreateTransactionUseCase`, `DeleteCategoryUseCase`).
