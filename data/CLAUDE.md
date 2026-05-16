# :data — CLAUDE.md

Android library. Implements `:domain` repository interfaces using SQLDelight (local-only).

Root package: `com.emm.data.<entity>`. `minSdk = 26`.

Depends on: `:domain`. **Does not depend on `:app`.**

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Repository impl | `Default{Entity}Repository` | `:data/<entity>/` |
| Local model | `{Entity}Entity` | `:data/<entity>/` |
| Local data source | `{Entity}LocalDataSource` | `:data/<entity>/` |
| Mappers | `{entity}Mappers.kt` (extension fns) | `:data/<entity>/` |

`Default{Entity}Repository` delegates to `LocalDataSource` (SQLDelight). Mappers convert between `{Entity}Entity` and domain types.

## Persistence (SQLDelight 2.x)

- Schema files in `data/src/main/sqldelight/com/emm/data/`: `accounts.sq`, `categories.sq`, `transactions.sq`.
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in `data/build.gradle.kts`.
- Hard-delete only (no `isDeleted` column). Foreign keys: `transactions.accountId → accounts(accountId) ON DELETE RESTRICT`, `transactions.categoryId → categories(categoryId) ON DELETE SET NULL`.
- `app.cash.sqldelight:coroutines-extensions` is exported (`api`) from this module for `asFlow()` / suspend query support.

## Error handling

`shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into `DomainException`. **Repositories must funnel I/O through `safeDbCall` / `catchAsDomainException` instead of throwing raw SQLDelight errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new exception type here.

## Testing

Instrumented tests live in `data/src/androidTest/`. Reserve them for behaviour that genuinely depends on the Android runtime (e.g. real SQLDelight driver). For pure mapping/logic, prefer unit tests.
