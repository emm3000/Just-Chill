# :data — CLAUDE.md

Kotlin Multiplatform library (`android` + `iosArm64` + `iosSimulatorArm64`). Implements `:domain`
repository interfaces. SQLDelight is the local source of truth; Supabase (`supabase-kt`) backs the
optional auth (`auth/`) and the backup pipeline (`backup/`). The row-replication sync engine that
used to live in `sync/` is gone (`docs/work/epics/E01-snapshot-backup.md`, ADR 009).

Root package: `com.emm.data.<entity>`. `minSdk = 26`. Depends on `:domain` only.

Almost everything lives in `commonMain`. Only three concerns are platform-split, and only **two** of
them are genuine `expect/actual` pairs — do not add a third without a real platform reason:

| File | Why it is platform-split |
|---|---|
| `DatabaseDriver` | `AndroidSqliteDriver` vs `NativeSqliteDriver`. **Not** an `expect/actual` pair, and structurally cannot be: Android's `provideSqlDriver` takes a `Context` and iOS's takes nothing, so the signatures cannot match. They are two independent platform-only files, each picked by its own Koin module (+ `DefaultCategorySeed.ios.kt`, iOS-only, because there is no `onCreate` hook to seed from). |
| `shared/Dispatchers.kt` | `expect val ioDispatcher` — `Dispatchers.IO` is JVM-only, absent in commonMain. |
| `shared/SqliteExceptions.kt` | Two `expect fun`s — `SQLiteException` / `SQLiteConstraintException` are Android types. |

Those three `expect` declarations are also why `:data:detektMainAndroid` reports nine compiler
errors: detekt analyses commonMain and androidMain as one unit, so it sees each `expect` and its
`actual` together. Three errors per pair, and the same effect gives `:presentation` three. Tracked
in `docs/PROGRESS.md`; it does not fail the gate.

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
  `transactions.sq`, `recurring_movements.sq`. Migrations `0.sqm`…`4.sqm` (current schema v5).
- **`transactions.occurredAt` is ISO local text, not an instant** — `'2026-08-10T21:47:33'`, no
  timezone. Ordering, month windows and day grouping are all plain string operations on it; see the
  header comment in `transactions.sq` for why that works and `docs/DATE_AUDIT.md` #5 for why it had
  to. `createdAt` / `updatedAt` / `deletedAt` stay epoch millis: those are genuine instants.
- **Migrations are MANDATORY for every schema change.** Never edit a `.sq` CREATE TABLE without a
  matching `.sqm`, and never reset the schema. There IS real data to lose: no third-party users, but
  the author runs the release build daily off Firebase App Distribution, and every push to trunk
  distributes to that device. The rule also stands on its own — a missing migration is the one
  defect this repo cannot test its way out of, because it only fires when an already-installed app
  opens a newer schema.
  The snapshot lives in `src/commonMain/sqldelight/databases/`; regenerate with
  `./gradlew :data:generateCommonMainEmmDatabaseDataSchema` when bumping the version.
  `./gradlew :data:verifySqlDelightMigration` replays the `.sqm` files over that snapshot and
  fails if the result differs from the `.sq` CREATE statements. It is on `check` (SQLDelight wires
  it there) **and on `qualityGate`** — the second one is what matters, since nothing here runs
  `check`.
- Generated database class: `EmmDatabaseData` (package `com.emm.data`), configured in `data/build.gradle.kts`.
- **Soft-delete (tombstones)** since schema v3: deletes are `UPDATE ... SET deletedAt,
  syncState='Pending'`; every read query filters `deletedAt IS NULL`. Sync metadata columns on all
  4 tables: `userId` (nullable), `deletedAt` (nullable epoch ms), `syncState` (default `'Pending'`).
- **The category/type relation IS enforced by the schema**, and is the one exception to the line
  below. Since v5 `transactions` and `recurring_movements` declare a COMPOSITE foreign key,
  `(categoryId, type) → categories(categoryId, categoryType)`, so a movement can never carry a
  category of the other type. It lives here and not in a use case because `DefaultBackupRepository`
  (import) reaches the database without passing through `:domain`. Full reasoning in the header of
  `transactions.sq`; the decision and the rejected alternatives in
  [ADR 008](../docs/adr/008-the-schema-owns-the-category-type-invariant.md).
  Two properties of it that surprise people: a composite FK with any NULL column is SATISFIED, so an
  uncategorized movement needs no special case; and it carries **no `ON DELETE` clause**, because
  `SET NULL` on a composite key would try to null `type`, which is `NOT NULL`.
- The remaining FK clauses (`transactions.accountId → accounts ON DELETE RESTRICT`, same on
  `recurring_movements`) **only fire on physical DELETE — never on soft-delete**. Deletion integrity
  is enforced in domain use cases (`DeleteAccountUseCase`, `DeleteCategoryUseCase`), not by them.
- `app.cash.sqldelight:coroutines-extensions` is exported (`api`) from this module for `asFlow()`.
- This module also `api`-exposes the Supabase auth/postgrest SDK and the Ktor engines. The consumer
  that actually uses them is `:presentation` (`hh/di/SupabaseModule.kt`).

## Error handling

`shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into
`DomainException`. **Repositories must funnel I/O through `safeDbCall` / `catchAsDomainException`
instead of throwing raw SQLDelight errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new
exception type here.

## Testing

- Host tests (JUnit4 + MockK) in `data/src/androidHostTest/kotlin/` — mappers, enum parsing, backup.
  Run with `./gradlew :data:testAndroidHostTest`.
- Platform-neutral tests in `data/src/commonTest/kotlin/` (`kotlin.test`), e.g. `Sha256HexTest`.
- Instrumented tests in `data/src/androidDeviceTest/`: `MigrationV1ToV2Test`, `MigrationV2ToV3Test`,
  `MigrationV3ToV4Test`, `MigrationV4ToV5Test`, `DeleteUseCasesE2ETest`, `RecurringMovementFkTest`.
  Run them with `./gradlew :data:connectedAndroidDeviceTest` (needs a device/emulator; 31 tests).
  They are the only thing that exercises migrations against the real `AndroidSqliteDriver` —
  **run them before shipping any schema change.** Gotcha: `kotlin.assert()` is a no-op on ART;
  always use `kotlin.test.assertTrue`.
- Two **destructive** migrations, and they are the reason that suite exists. `3.sqm` rebuilds
  `transactions` because SQLite cannot change a column's type; `4.sqm` rebuilds `transactions` AND
  `recurring_movements` because it cannot add a table constraint either, and repairs the data first
  — on iOS the copy runs with foreign keys ON, so repairing afterwards would repair rows that never
  crossed. `MigrationV3ToV4Test` and `MigrationV4ToV5Test` are what say the rows, the indexes, the
  types and the ability to open the app at all survive them. Two of `MigrationV4ToV5Test`'s cases
  migrate with **foreign keys ON** — the iOS configuration, and the only one under which statement
  order in `4.sqm` matters at all. Android runs them off here (`onOpen` after `onUpgrade`), so a
  suite that only tested Android would pass whatever order the migration were written in.
- **A schema bump also rehearses the restore**, and it is a different question: the suite proves a
  migration preserves rows already on the device, never that a snapshot written *before* the bump
  can still be read *after* it. Before shipping one, import the latest production snapshot onto a
  clean emulator and compare `ImportStats` — `accounts`, `categories`, `transactions`, `recurring` —
  against the pre-bump counts. Any of the four that moved is a failure. Restoring is the only path
  back from a migration that loses data, so a bump that has not been restored from is untested.

### Migration tests: use raw SQL against historical schemas

SQLDelight generates query classes from the CURRENT schema. A migration test that builds a
historical schema (e.g. v2 via a custom `SqlSchema`) and then calls generated queries
(`accountsQueries.insert(...)`) FAILS with `table accounts has no column named syncState` — the
generated INSERT references columns the historical schema doesn't have yet.

Pattern (see `MigrationV1ToV2Test`, `MigrationV2ToV3Test`):
- Set-up inserts against the historical schema: `driver.execute(null, "INSERT INTO ... raw SQL ...", 0)`.
- Reads within the historical window: `driver.executeQuery(null, sql, { cursor -> QueryResult.Value(...) }, 0).value`
  — the mapper must return `QueryResult<T>`, not `T`.
- **Migrate to `EmmDatabaseData.Schema.version`, not to the next one.** Generated queries only match
  the CURRENT schema, so a test that stops mid-chain cannot use them for its assertions — and that
  is also what a real device does, since it runs the whole chain in one open. `MigrationV2ToV3Test`
  stopped at 3 and started failing the moment a 4 existed.
- After `Schema.migrate(...)` reaches the current version, generated queries work for assertions.
