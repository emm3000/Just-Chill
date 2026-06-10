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

- Schema files in `data/src/main/sqldelight/com/emm/data/`: `accounts.sq`, `categories.sq`, `transactions.sq`, `recurring_movements.sq`. Migrations in `*.sqm` (current schema v3 via `2.sqm`).
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in `data/build.gradle.kts`.
- **Soft-delete (tombstones)** since schema v3: deletes are `UPDATE ... SET deletedAt, syncState='Pending'`; every read query filters `deletedAt IS NULL`. Sync metadata columns on all 4 tables: `userId` (nullable), `deletedAt` (nullable epoch ms), `syncState` (default `'Pending'`). See `docs/sync/DESIGN.md`.
- FK clauses still exist (`transactions.accountId → accounts ON DELETE RESTRICT`, `transactions.categoryId → categories ON DELETE SET NULL`) but **only fire on physical DELETE — never on soft-delete**. Referential integrity is enforced in domain use cases (`DeleteAccountUseCase`, `DeleteCategoryUseCase`), not by these clauses.
- `app.cash.sqldelight:coroutines-extensions` is exported (`api`) from this module for `asFlow()` / suspend query support.

## Error handling

`shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into `DomainException`. **Repositories must funnel I/O through `safeDbCall` / `catchAsDomainException` instead of throwing raw SQLDelight errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new exception type here.

## Testing

Instrumented tests live in `data/src/androidTest/`. Reserve them for behaviour that genuinely depends on the Android runtime (e.g. real SQLDelight driver). For pure mapping/logic, prefer unit tests.

### Migration tests: use raw SQL against historical schemas

SQLDelight generates query classes from the CURRENT schema. A migration test that builds a historical schema (e.g. v2 via a custom `SqlSchema`) and then calls generated queries (`accountsQueries.insert(...)`) FAILS with `table accounts has no column named syncState` — the generated INSERT references columns the historical schema doesn't have yet.

Pattern (see `MigrationV1ToV2Test`, `MigrationV2ToV3Test`):
- Set-up inserts against the historical schema: `driver.execute(null, "INSERT INTO ... raw SQL ...", 0)`.
- Reads within the historical window: `driver.executeQuery(null, sql, { cursor -> QueryResult.Value(...) }, 0).value` — the mapper must return `QueryResult<T>`, not `T`.
- After `Schema.migrate(...)` completes, the schema IS current — generated queries work fine for assertions.
