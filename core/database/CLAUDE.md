# :core:database — CLAUDE.md

KMP library (`justchill.kmp.library`, ADR 024 Decision 4) implementing the `:core:domain` repository interfaces. SQLDelight is the local source of truth and the only framework here: Supabase, Ktor and the snapshot file live in `:core:backup`, which reaches these tables through the `SnapshotStore` port. The row-replication sync engine is gone (ADR 009); read `## Snapshots` before touching `backup/`.

Root package `com.emm.justchill.core.database.<entity>`, `minSdk = 28`. Depends on `:core:domain` only. Repositories funnel I/O through `shared/SafeCall.kt` (`safeDbCall`, `catchAsDomainException`), never throwing raw SQLDelight errors.

`SqlDelightSnapshotStore` is the one deliberate exception to the `Default{Entity}Repository` → `{Entity}LocalDataSource` shape: an export and a restore are each a single transaction spanning every table, and per-entity data sources cannot share one, so it drives `JustChillDatabase` and its `*Queries` directly.

## Persistence

Schema, migrations, snapshots and the generated `JustChillDatabase` live under `core/database/src/commonMain/sqldelight/`; `justchill.sqldelight` configures the database with `verifyMigrations` on. The rules a change must honour, the FK-on test and the restore drill: `.claude/rules/sqldelight.md`.

Two migrations are destructive, and they are why the instrumented suite exists. `3.sqm` rebuilds `transactions` because SQLite cannot change a column's type. `4.sqm` rebuilds `transactions` and `recurring_movements` because SQLite cannot add a table constraint, and it repairs the data first: with foreign keys on, `INSERT INTO transactions_new SELECT` is checked against the new key as it copies, so a repair afterwards would fix rows that never crossed. `MigrationV3ToV4Test` and `MigrationV4ToV5Test` guard them.

What this module exports: `app.cash.sqldelight:coroutines-extensions` is `implementation` (the `LocalDataSource`s use it for `asFlow()`) and does NOT reach consumers. Only the Android driver is `api`-exposed, from `androidMain`, for the `SqlDriver` `:androidApp` builds.

## Source sets

- `commonMain`: every repository, mapper and data source, the `.sq` / `.sqm` / `databases/N.db` under `sqldelight/`, `DATABASE_NAME` and the category seed, and the `expect` half of `shared/SqliteExceptions.kt`.
- `androidMain`: `provideSqlDriver(context)` and `csm()` on `AndroidSqliteDriver`, the Android exception actuals.
- `iosMain`: `provideSqlDriver()` on `NativeSqliteDriver` with the same seed, the SQLiter exception actuals. SQLiter turns foreign keys on before create and upgrade, so `openSqlDriver` runs them first on a connection with foreign keys off, then opens the driver with them on: the order `csm()` gives Android.

## Snapshots

`backup/` holds this module's half of ADR 009: `SqlDelightSnapshotStore` reads the six tables into a `LocalSnapshot` and replaces them from one, and `SnapshotRestore.kt` writes a row at a time. The file, its versions and Supabase are `:core:backup`'s; read `core/backup/CLAUDE.md` before either side.

- The sync schema stays so a future engine plugs in without re-migrating: never drop `userId` / `syncState` / `deletedAt`, never change the UUID keys, never return to hard deletes. `claimAll:` / `unclaimAll:` / `countUnclaimed:` and `updateFromRemote:` in the `.sq` files, except `recurring_movements.sq`, frozen by ADR 023, have no production caller and are that engine's re-entry point: a dead-code sweep leaves them.
- Rows are not stamped with a `userId` and no read query filters by one. One SQLite file per device, no wipe on sign-out: the next account inherits the ledger and backs it up. The 23 seeded categories carry the same UUIDs on every install.
- A `LocalSnapshot`'s null list means the file predates that table, and the restore leaves it alone; an empty list empties it. A table joins a snapshot only when both halves are wired: the `:core:backup` field and gate, and here `softDeleteAllLive` plus `insertOrIgnoreFromBackup` / `restoreFromBackup` inside `replaceWith`'s transaction, with its count in `ImportStats`.
- `clearCategoryOnTypeChange` runs on every restore, and the recurring templates go last so they read the categories the same restore just wrote. A payment whose loan the snapshot dropped is skipped, not detached: `loan_payments.loanId` is NOT NULL and the insert would abort the whole transaction. A template whose account the restore does not hold live is skipped the same way (`recurring_movements.accountId` is NOT NULL), and so stays tombstoned if the device already had it. `ImportStats.loanPayments` and `ImportStats.recurring` count the rows written, not the rows the file carried.
- Accounts restore with the one currency the ledger has, the same literal every other insert writes. The column is older than the product decision and nothing reads it.

## Testing

- Host tests (JUnit4 + MockK) in `core/database/src/androidHostTest/kotlin/`: mappers, enum parsing, the snapshot store, plus plain `kotlin.test` suites. `./gradlew :core:database:testAndroidHostTest`, which `./gradlew test` never reaches. A snapshot test that starts from a JSON file lives in `:androidApp`, the only module that sees `:core:backup` too.
- Native tests (`kotlin.test`, no MockK) in `core/database/src/iosTest/kotlin/`: the constraint mapping and the foreign-key order on `provideSqlDriver`'s configuration. `./gradlew :core:database:iosSimulatorArm64Test` on the booted `justchill-ios27`, never another device; not on the gate.
- Instrumented tests in `core/database/src/androidDeviceTest/`: the `MigrationV*Test`s plus `DeleteUseCasesE2ETest` and `RecurringMovementFkTest`. `./gradlew :core:database:connectedAndroidDeviceTest` on `justchill-api36`; the only thing that exercises migrations against the real `AndroidSqliteDriver`.
