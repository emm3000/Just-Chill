# :data — CLAUDE.md

Android library (`com.android.library`, ADR 011) implementing the `:domain` repository interfaces. SQLDelight is the local source of truth; Supabase (`supabase-kt`) backs the optional auth (`auth/`) and the backup pipeline (`backup/`). The row-replication sync engine is gone (ADR 009); read `## Backup` before touching `backup/`.

Root package `com.emm.data.<entity>`, `minSdk = 28`. Depends on `:domain` only. Repositories funnel I/O through `shared/SafeCall.kt` (`safeDbCall`, `catchAsDomainException`), never throwing raw SQLDelight errors.

`DefaultBackupRepository` is the one deliberate exception to the `Default{Entity}Repository` → `{Entity}LocalDataSource` shape: a snapshot and a restore each have to be a single transaction spanning every table, and per-entity data sources cannot share one, so it drives `EmmDatabaseData` and its `*Queries` directly.

## Persistence

Schema, migrations, snapshots and the generated `EmmDatabaseData` live under `data/src/main/sqldelight/`. The rules a change must honour, the FK-on test and the restore drill: `.claude/rules/sqldelight.md`.

Two migrations are destructive, and they are why the instrumented suite exists. `3.sqm` rebuilds `transactions` because SQLite cannot change a column's type. `4.sqm` rebuilds `transactions` and `recurring_movements` because SQLite cannot add a table constraint, and it repairs the data first: with foreign keys on, `INSERT INTO transactions_new SELECT` is checked against the new key as it copies, so a repair afterwards would fix rows that never crossed. `MigrationV3ToV4Test` and `MigrationV4ToV5Test` guard them.

What this module exports: `app.cash.sqldelight:coroutines-extensions` is `implementation` (the `LocalDataSource`s use it for `asFlow()`) and does NOT reach consumers. The Supabase auth/postgrest/storage SDKs and the Ktor engines ARE `api`-exposed; the consumer is `:presentation` (`hh/di/SupabaseModule.kt`).

## Backup

- The sync schema stays so a future engine plugs in without re-migrating: never drop `userId` / `syncState` / `deletedAt`, never change the UUID keys, never return to hard deletes. `claimAll:` / `unclaimAll:` / `countUnclaimed:` and `updateFromRemote:` in the `.sq` files have no production caller and are that engine's re-entry point: a dead-code sweep leaves them.
- Rows are not stamped with a `userId` and no read query filters by one. One SQLite file per device, no wipe on sign-out: the next account inherits the ledger and backs it up. The 23 seeded categories carry the same UUIDs on every install.
- The export format version is not the DB schema version. Version gates are frozen literals (`BACKUP_RECURRING_SINCE_VERSION`, `BACKUP_SCHEMA_VERSION_V2`, `BACKUP_LOANS_SINCE_VERSION`), never the live `BACKUP_SCHEMA_VERSION`, which moves in the commit that changes the shape. Each format version gets a frozen type, a hand-written fixture and a `BackupV*CompatibilityTest` first; gate on `declaredVersion`, never on an empty array. `BackupPayloadDecoder` refuses an unknown version, so a new one is unreadable on older builds.
- A table reaches a snapshot only when wired end to end: an `ExportPayloadDto` field, an export read, a `*_SINCE_VERSION` gate, `softDeleteAllLive` plus `insertOrIgnoreFromBackup` / `restoreFromBackup` inside `importFromJson`'s transaction, and an `ImportStats` count. An unwired table dies with the device and goes stale on restore.
- `clearCategoryOnTypeChange` runs on every version. The recurring restore runs last so it reads the file's own categories. An unparseable `lastConfirmedPeriod` degrades to null, never drops the row.
- Upload order is the meaning: payload, read-back, then `<name>.json.manifest.json`, whose existence says the payload verified. It holds only because the bucket forbids update and uploads use `upsert = false`. Send bare `application/json`: the bucket's mime check is a verbatim match.
- The `backup-v<n>-` generation in a snapshot name is decorative; a payload is what its own `schemaVersion` says. Never share one constant between the name writer and the matcher, or every uploaded snapshot vanishes from prune and verify. `BackupSnapshotNameTest` pins the names, so `SupabaseBackupObjectStore.upload`'s `.json` guard stays `DomainException.Unknown`.
- Every failure path in `DefaultBackup{Uploader,Pruner,Verifier}` logs its own named reason; a new path adds a reason, never reuses one.
- Prune: slots (`SnapshotRetention`, 7 + 8 + 12) fill from snapshots that exist, never today's calendar. Any read failure aborts before the first delete; a failed delete is recorded and the run continues. An orphan payload is deleted; a pair with a bad manifest is kept.
- Verify is read-only and not a cycle: it deletes nothing, orphans included, touches no watermark, streak or health, and emits no `BackupEvent`. `isNewestPair` is measured against the newest parseable name, orphan or not. A list or download failure is an error, never `NothingVerified`.
- `storage.protect_delete()` refuses direct deletes; delete through the Storage API. Storage calls are bounded by the 120s `transferTimeout`, not Postgrest's 10s `requestTimeout`; `SupabaseModule` configures neither.
- RLS stays as is: `with check (user_id = (select auth.uid()))`, `to authenticated`; `delete_account()` is `security definer` with `set search_path = ''`. `supabase/tests/backups-rls-probe.sh` is its only proof, run by `probeStorageRls` and kept off `qualityGate` (no Docker, no network). Its owner-side assertions are what make each refusal mean something.

## Testing

- Host tests (JUnit4 + MockK) in `data/src/test/kotlin/`: mappers, enum parsing, backup, plus plain `kotlin.test` suites. `./gradlew :data:testDebugUnitTest`.
- Instrumented tests in `data/src/androidTest/`: the `MigrationV*Test`s plus `DeleteUseCasesE2ETest` and `RecurringMovementFkTest`. `./gradlew :data:connectedDebugAndroidTest` on the `medium_phone` emulator; the only thing that exercises migrations against the real `AndroidSqliteDriver`.

### Building a `SupabaseClient` in a test

- **`awaitInitialization()` goes between `createSupabaseClient` and `importSession`, always**; every call site wraps it as `settled()`. `Auth.init()` launches on the client's own dispatcher and ends at `initDone()`, whose check-then-act on `sessionStatus` is not atomic: an `importSession()` landing between its read and its write is overwritten with `NotAuthenticated`, and every test then runs on a session that is gone.
- **`Dispatchers.setMain` / `resetMain` are inert here; do not add them.** `minimalConfig()` sets `enableLifecycleCallbacks = false`, and `addLifecycleCallbacks` returns on that flag before the single `scope.launch(Dispatchers.Main)` that is auth-kt's only Main dispatch on Android. `AppGraphKoinTest` in `:presentation` is the one test that builds the real graph and reaches it.
