# :data — CLAUDE.md

Android library. Implements `:domain` repository interfaces using SQLDelight (local) + Supabase/Ktor (remote).

Root package: `com.emm.data.<entity>`. `minSdk = 26`.

Depends on: `:domain`. **Does not depend on `:app`.**

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Repository impl | `Default{Entity}Repository` | `:data/<entity>/` |
| Local model | `{Entity}Entity` | `:data/<entity>/` |
| Remote model | `Network{Entity}` | `:data/<entity>/` |
| Local data source | `{Entity}LocalDataSource` | `:data/<entity>/` |
| Remote data source | `{Entity}RemoteDataSource` | `:data/<entity>/` |
| Mappers | `{entity}Mappers.kt` (extension fns) | `:data/<entity>/` |

`Default{Entity}Repository` coordinates `LocalDataSource` (SQLDelight) and `RemoteDataSource` (Supabase). Mappers convert between `{Entity}Entity` / `Network{Entity}` and domain types.

## Persistence (SQLDelight 2.x)

- Schema files in `data/src/main/sqldelight/com/emm/data/`: `accounts.sq`, `categories.sq`, `transactions.sq`.
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in `data/build.gradle.kts`.
- Migrations live in `data/src/main/sqldelight/migrations/` (current: `7.sqm`). **Always add a new `.sqm` when changing a `.sq` schema — do not edit existing migrations.**
- `app.cash.sqldelight:coroutines-extensions` is exported (`api`) from this module for `asFlow()` / suspend query support.

## Backend & Sync

- Supabase via `io.github.jan-tennert.supabase` BOM (`postgrest-kt` + `auth-kt`); Ktor OkHttp engine. All three are exported `api` so `:app` can configure the client.
- `sync/Synchronizer.kt` — single-method interface (`suspend fun sync()`) that orchestrates local↔remote sync. Implementations typically touch both `LocalDataSource` and `RemoteDataSource`; changes here are bidirectional by design.
- `DefaultAuthRepository` (in `auth/`) wraps Supabase `auth-kt` and implements the domain `AuthRepository` interface.
- `UserIdProvider` (in `auth/`) exposes the current Supabase user id to repositories that need to scope queries per-user.

## Error handling

`shared/SafeCall.kt` wraps remote/local calls and translates third-party exceptions (Supabase, Ktor, SQLDelight) into `DomainException`. **Repositories must funnel I/O through `SafeCall` instead of throwing raw third-party errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new exception type here.

## Testing

Instrumented tests live in `data/src/androidTest/`. Reserve them for behaviour that genuinely depends on the Android runtime (e.g. real SQLDelight driver, real Ktor client). For pure mapping/logic, prefer unit tests.
