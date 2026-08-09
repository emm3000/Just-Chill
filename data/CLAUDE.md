# :data — CLAUDE.md

Kotlin Multiplatform library (`android` + `iosArm64` + `iosSimulatorArm64`). Implements `:domain`
repository interfaces. SQLDelight is the local source of truth; Supabase (`supabase-kt`) backs the
optional auth (`auth/`) and multi-device sync (`sync/`) implementations.

Root package: `com.emm.data.<entity>`. `minSdk = 26`. Depends on `:domain` only.

Almost everything lives in `commonMain`. `androidMain` / `iosMain` hold exactly three
`expect/actual` pairs — do not add a fourth without a real platform reason:

| File | Why it needs a platform actual |
|---|---|
| `DatabaseDriver` | `AndroidSqliteDriver` vs `NativeSqliteDriver` (+ `DefaultCategorySeed` on iOS) |
| `shared/Dispatchers.kt` | `Dispatchers.IO` is JVM-only; absent in commonMain |
| `shared/SqliteExceptions.kt` | `SQLiteException` / `SQLiteConstraintException` are Android types |

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Repository impl | `Default{Entity}Repository` | `<entity>/` |
| Local model | `{Entity}Entity` | `<entity>/` |
| Local data source | `{Entity}LocalDataSource` | `<entity>/` |
| Mappers | `{entity}Mappers.kt` (extension fns) | `<entity>/` |

`Default{Entity}Repository` delegates to `LocalDataSource` (SQLDelight). Mappers convert between
`{Entity}Entity` and domain types.

## Persistence (SQLDelight 2.x)

- Schema in `data/src/commonMain/sqldelight/com/emm/data/`: `accounts.sq`, `categories.sq`,
  `transactions.sq`, `recurring_movements.sq`. Migrations `0.sqm`, `1.sqm`, `2.sqm` (current schema v3).
- **Migrations are MANDATORY for every schema change.** Real user data exists on devices since
  commit `4e6de6c` (2026-06-04) — never edit a `.sq` CREATE TABLE without a matching `.sqm`,
  never reset the schema. The snapshot lives in `src/commonMain/sqldelight/databases/`; regenerate
  with `./gradlew :data:generateCommonMainEmmDatabaseDataSchema` when bumping the version, and
  verify with `./gradlew :data:verifySqlDelightMigration` (also wired into `check`/`build`).
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in `data/build.gradle.kts`.
- **Soft-delete (tombstones)** since schema v3: deletes are `UPDATE ... SET deletedAt,
  syncState='Pending'`; every read query filters `deletedAt IS NULL`. Sync metadata columns on all
  4 tables: `userId` (nullable), `deletedAt` (nullable epoch ms), `syncState` (default `'Pending'`).
- FK clauses still exist (`transactions.accountId → accounts ON DELETE RESTRICT`,
  `transactions.categoryId → categories ON DELETE SET NULL`) but **only fire on physical DELETE —
  never on soft-delete**. Referential integrity is enforced in domain use cases
  (`DeleteAccountUseCase`, `DeleteCategoryUseCase`), not by these clauses.
- `app.cash.sqldelight:coroutines-extensions` is exported (`api`) from this module for `asFlow()`.
- This module also `api`-exposes the Supabase auth/postgrest SDK and the Ktor engines, so
  `shared-ui` inherits them transitively.

## Sync engine (`sync/`)

- The per-table push/pull algorithm lives once in `BaseTableSync<DTO : SyncRowDto>` (template
  method); each table class (`AccountTableSync`, etc.) supplies only generated-query adapters and
  the reified Postgrest calls.
- `DefaultSyncRepository` orchestrates push then pull in FK-safe order (accounts → categories →
  transactions → recurring_movements), holds the pull cursor when any table skips rows, and funnels
  remote/ktor errors through `toSyncDomainException()`.
- Cursor semantics (server-set `server_updated_at`, 10s overlap window) are defined in
  `docs/adr/002`; the `SyncCursorStore` port is implemented in `shared-ui`.

## Error handling

`shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into
`DomainException`. **Repositories must funnel I/O through `safeDbCall` / `catchAsDomainException`
instead of throwing raw SQLDelight errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new
exception type here.

## Testing

- Host tests (JUnit4 + MockK) in `data/src/androidHostTest/kotlin/` — mappers, sync repository,
  pagination, enum parsing, backup. Run with `./gradlew :data:testAndroidHostTest`.
- Platform-neutral tests in `data/src/commonTest/kotlin/` (`kotlin.test`), e.g. `SyncCursorUtilsTest`.
- Instrumented tests in `data/src/androidDeviceTest/`: `MigrationV1ToV2Test`, `MigrationV2ToV3Test`,
  `DeleteUseCasesE2ETest`, `RecurringMovementFkTest`, `SyncFkExceptionTest`. Run them with
  `./gradlew :data:connectedAndroidDeviceTest` (needs a device/emulator; 15 tests). They are the
  only thing that exercises migrations against the real `AndroidSqliteDriver` — **run them before
  shipping any schema change.** Gotcha: `kotlin.assert()` is a no-op on ART; always use
  `kotlin.test.assertTrue`.

### Migration tests: use raw SQL against historical schemas

SQLDelight generates query classes from the CURRENT schema. A migration test that builds a
historical schema (e.g. v2 via a custom `SqlSchema`) and then calls generated queries
(`accountsQueries.insert(...)`) FAILS with `table accounts has no column named syncState` — the
generated INSERT references columns the historical schema doesn't have yet.

Pattern (see `MigrationV1ToV2Test`, `MigrationV2ToV3Test`):
- Set-up inserts against the historical schema: `driver.execute(null, "INSERT INTO ... raw SQL ...", 0)`.
- Reads within the historical window: `driver.executeQuery(null, sql, { cursor -> QueryResult.Value(...) }, 0).value`
  — the mapper must return `QueryResult<T>`, not `T`.
- After `Schema.migrate(...)` completes, the schema IS current — generated queries work for assertions.
