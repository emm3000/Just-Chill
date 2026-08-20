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

## Persistence

SQLDelight 2.x. Schema, migrations and the generated `EmmDatabaseData` all live under
`data/src/commonMain/sqldelight/`. The schema rules, the migration obligation and the migration-test
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

- Host tests (JUnit4 + MockK) in `data/src/androidHostTest/kotlin/` — mappers, enum parsing, backup.
  Run with `./gradlew :data:testAndroidHostTest`.
- Platform-neutral tests in `data/src/commonTest/kotlin/` (`kotlin.test`), e.g. `Sha256HexTest`.
- Instrumented tests in `data/src/androidDeviceTest/` — the four `MigrationV*Test`s plus
  `DeleteUseCasesE2ETest` and `RecurringMovementFkTest`. Run them with
  `./gradlew :data:connectedAndroidDeviceTest` (needs a device/emulator). They are the only thing
  that exercises migrations against the real `AndroidSqliteDriver`; what they prove and how to write
  one is [`docs/PERSISTENCE.md`](../docs/PERSISTENCE.md).
