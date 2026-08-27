# :data — CLAUDE.md

Android library (`com.android.library`, ADR 011 — no iOS target). Implements `:domain` repository
interfaces. SQLDelight is the local source of truth; Supabase (`supabase-kt`) backs the optional auth
(`auth/`) and the backup pipeline (`backup/`). The row-replication sync engine that used to live in
`sync/` is gone (`docs/work/epics/E01-snapshot-backup.md`, ADR 009).

Root package: `com.emm.data.<entity>`. `minSdk = 26`. Depends on `:domain` only.

## Layer conventions

| Concept | Naming | Location |
|---|---|---|
| Repository impl | `Default{Entity}Repository` | `<entity>/` |
| Local model | `{Entity}Entity` | `<entity>/` |
| Local data source | `{Entity}LocalDataSource` | `<entity>/` |
| Mappers | `{entity}Mappers.kt` (extension fns) | `<entity>/` |

`Default{Entity}Repository` delegates to `LocalDataSource` (SQLDelight). Mappers convert between
`{Entity}Entity` and domain types.

`DefaultBackupRepository` is the one deliberate exception: a snapshot and a restore each have to be
a single transaction spanning every table, and per-entity `LocalDataSource`s cannot share one, so it
drives `EmmDatabaseData` and its `*Queries` directly.

## Persistence

SQLDelight 2.x. Schema, migrations and the generated `EmmDatabaseData` all live under
`data/src/main/sqldelight/`. The schema rules, the migration obligation and the migration-test
mechanics are [`docs/PERSISTENCE.md`](../docs/PERSISTENCE.md) — **read it before touching a `.sq`, a
`.sqm` or a migration test.**

What this module exports, and what it does not: `app.cash.sqldelight:coroutines-extensions` is an
`implementation` dependency (the `LocalDataSource`s use it for `asFlow()`), so it does NOT reach
consumers. The Supabase auth/postgrest/storage SDKs and the Ktor engines ARE `api`-exposed; the
consumer that actually uses them is `:presentation` (`hh/di/SupabaseModule.kt`).

## Error handling

`shared/SafeCall.kt` wraps local DB calls and translates SQLDelight exceptions into
`DomainException`. **Repositories must funnel I/O through `safeDbCall` / `catchAsDomainException`
instead of throwing raw SQLDelight errors.**

When adding a new failure mode, extend `DomainException` in `:domain` rather than introducing a new
exception type here.

## Testing

- Host tests (JUnit4 + MockK) in `data/src/test/kotlin/` — mappers, enum parsing, backup, plus the
  platform-neutral ones (`kotlin.test`, e.g. `Sha256HexTest`) that used to sit in `commonTest`. Run
  with `./gradlew :data:testDebugUnitTest`.
- Instrumented tests in `data/src/androidTest/` — the five `MigrationV*Test`s plus
  `DeleteUseCasesE2ETest` and `RecurringMovementFkTest`. Run them with
  `./gradlew :data:connectedDebugAndroidTest` (needs a device/emulator). They are the only thing
  that exercises migrations against the real `AndroidSqliteDriver`; what they prove and how to write
  one is [`docs/PERSISTENCE.md`](../docs/PERSISTENCE.md).

### Building a `SupabaseClient` in a test

**`awaitInitialization()` goes between `createSupabaseClient` and `importSession`, always** — every
call site wraps it as `settled()`. `Auth.init()` launches on the client's own dispatcher and ends at
`initDone()`, whose check-then-act on `sessionStatus` is not atomic (supabase-kt `Utils.kt`): an
`importSession()` landing between its read and its write is overwritten with `NotAuthenticated`, and
every test then runs on a session that is gone.

**`Dispatchers.setMain`/`resetMain` are inert here — do not add them.** `minimalConfig()` sets
`enableLifecycleCallbacks = false`, and `addLifecycleCallbacks` returns on that flag before the
single `scope.launch(Dispatchers.Main)` that is auth-kt's only Main dispatch on Android. A test that
builds the real graph instead of `minimalConfig()` does reach it — `AppGraphKoinTest` in
`:presentation` is the one that does.
