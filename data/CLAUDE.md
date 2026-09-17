# :data — CLAUDE.md

Android library (`com.android.library`, ADR 011) implementing the `:domain` repository interfaces. SQLDelight is the local source of truth; Supabase (`supabase-kt`) backs the optional auth (`auth/`) and the backup pipeline (`backup/`). The row-replication sync engine that used to live in `sync/` is gone (`docs/work/epics/E01-snapshot-backup.md`, ADR 009); read that epic before touching `backup/`.

Root package `com.emm.data.<entity>`, `minSdk = 26`. Depends on `:domain` only. Repositories funnel I/O through `shared/SafeCall.kt` (`safeDbCall`, `catchAsDomainException`), never throwing raw SQLDelight errors.

`DefaultBackupRepository` is the one deliberate exception to the `Default{Entity}Repository` → `{Entity}LocalDataSource` shape: a snapshot and a restore each have to be a single transaction spanning every table, and per-entity data sources cannot share one, so it drives `EmmDatabaseData` and its `*Queries` directly.

## Persistence

Schema, migrations, snapshots and the generated `EmmDatabaseData` live under `data/src/main/sqldelight/`. The rules a change must honour: `.claude/rules/sqldelight.md`; the reasoning, the destructive migrations and the restore drill: `docs/PERSISTENCE.md`.

What this module exports: `app.cash.sqldelight:coroutines-extensions` is `implementation` (the `LocalDataSource`s use it for `asFlow()`) and does NOT reach consumers. The Supabase auth/postgrest/storage SDKs and the Ktor engines ARE `api`-exposed; the consumer is `:presentation` (`hh/di/SupabaseModule.kt`).

## Testing

- Host tests (JUnit4 + MockK) in `data/src/test/kotlin/`: mappers, enum parsing, backup, plus plain `kotlin.test` suites. `./gradlew :data:testDebugUnitTest`.
- Instrumented tests in `data/src/androidTest/`: the `MigrationV*Test`s plus `DeleteUseCasesE2ETest` and `RecurringMovementFkTest`. `./gradlew :data:connectedDebugAndroidTest` on the `medium_phone` emulator; the only thing that exercises migrations against the real `AndroidSqliteDriver`.

### Building a `SupabaseClient` in a test

- **`awaitInitialization()` goes between `createSupabaseClient` and `importSession`, always**; every call site wraps it as `settled()`. `Auth.init()` launches on the client's own dispatcher and ends at `initDone()`, whose check-then-act on `sessionStatus` is not atomic: an `importSession()` landing between its read and its write is overwritten with `NotAuthenticated`, and every test then runs on a session that is gone.
- **`Dispatchers.setMain` / `resetMain` are inert here; do not add them.** `minimalConfig()` sets `enableLifecycleCallbacks = false`, and `addLifecycleCallbacks` returns on that flag before the single `scope.launch(Dispatchers.Main)` that is auth-kt's only Main dispatch on Android. `AppGraphKoinTest` in `:presentation` is the one test that builds the real graph and reaches it.
