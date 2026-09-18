# :core:backup — CLAUDE.md

Android library (`com.android.library`, ADR 011) holding the opt-in snapshot backup (ADR 009) and the Supabase account it rides on: the export DTOs and their frozen older generations, the payload decoder, the object store over Supabase Storage, the uploader, verifier, pruner and eraser, and `auth/`. It depends on `:core:domain` and nothing else — never on `:core:database`, which `checkModuleBoundaries` enforces. Rows reach it through `SnapshotStore`, the port `:core:domain` declares, `:core:database` implements and `hh/di/DataModule.kt` binds.

Root package `com.emm.justchill.core.backup`, `minSdk = 28`. The Supabase auth/postgrest/storage SDKs and the Ktor engine are `api`-exposed; the consumer is `:presentation` (`hh/di/SupabaseModule.kt`), which builds the `SupabaseClient` this module takes by constructor.

`shared/` holds this module's own copies of the date-text, enum-parsing and dispatcher helpers `:core:database` also carries. The duplication is the point: the file format is frozen for every snapshot already on a disk, and the database's storage format has to stay free to move.

## Backup

- The export format version is not the DB schema version. Version gates are frozen literals (`BACKUP_RECURRING_SINCE_VERSION`, `BACKUP_SCHEMA_VERSION_V2`, `BACKUP_LOANS_SINCE_VERSION`), never the live `BACKUP_SCHEMA_VERSION`, which moves in the commit that changes the shape. Each format version gets a frozen type, a hand-written fixture and a `BackupV*CompatibilityTest` first; gate on `declaredVersion`, never on an empty array. `BackupPayloadDecoder` refuses an unknown version, so a new one is unreadable on older builds.
- A `LocalSnapshot` carries a null list for a table the file's version predates, and `SqlDelightSnapshotStore` leaves that table untouched. An empty list is the opposite instruction and does empty the table; the two must never be confused.
- A table reaches a snapshot only when wired end to end: an `ExportPayloadDto` field, a `*_SINCE_VERSION` gate, a DTO mapper both ways, the `LocalSnapshot` field, and the read and replace in `:core:database`'s `SqlDelightSnapshotStore`. An unwired table dies with the device and goes stale on restore.
- Upload order is the meaning: payload, read-back, then `<name>.json.manifest.json`, whose existence says the payload verified. It holds only because the bucket forbids update and uploads use `upsert = false`. Send bare `application/json`: the bucket's mime check is a verbatim match.
- The `backup-v<n>-` generation in a snapshot name is decorative; a payload is what its own `schemaVersion` says. Never share one constant between the name writer and the matcher, or every uploaded snapshot vanishes from prune and verify. `BackupSnapshotNameTest` pins the names, so `SupabaseBackupObjectStore.upload`'s `.json` guard stays `DomainException.Unknown`.
- Every failure path in `DefaultBackup{Uploader,Pruner,Verifier}` logs its own named reason; a new path adds a reason, never reuses one.
- Prune: slots (`SnapshotRetention`, 7 + 8 + 12) fill from snapshots that exist, never today's calendar. Any read failure aborts before the first delete; a failed delete is recorded and the run continues. An orphan payload is deleted; a pair with a bad manifest is kept.
- Verify is read-only and not a cycle: it deletes nothing, orphans included, touches no watermark, streak or health, and emits no `BackupEvent`. `isNewestPair` is measured against the newest parseable name, orphan or not. A list or download failure is an error, never `NothingVerified`.
- `storage.protect_delete()` refuses direct deletes; delete through the Storage API. Storage calls are bounded by the 120s `transferTimeout`, not Postgrest's 10s `requestTimeout`; `SupabaseModule` configures neither.
- RLS stays as is: `with check (user_id = (select auth.uid()))`, `to authenticated`; `delete_account()` is `security definer` with `set search_path = ''`. `supabase/tests/backups-rls-probe.sh` is its only proof, run by `probeStorageRls` and kept off `qualityGate` (no Docker, no network).

## Testing

`./gradlew :core:backup:testDebugUnitTest` — JUnit4 + MockK over the DTO mappers, the decoder, the object store and the four cycle classes, with `ktor-client-mock` standing in for the network. The tests that carry a snapshot all the way into real SQLite live in `:androidApp` (`androidApp/src/test/.../core/backup/`), the only module that sees this one and `:core:database` at once.

### Building a `SupabaseClient` in a test

- **`awaitInitialization()` goes between `createSupabaseClient` and `importSession`, always**; every call site wraps it as `settled()`. `Auth.init()` launches on the client's own dispatcher and ends at `initDone()`, whose check-then-act on `sessionStatus` is not atomic: an `importSession()` landing between its read and its write is overwritten with `NotAuthenticated`, and every test then runs on a session that is gone.
- **`Dispatchers.setMain` / `resetMain` are inert here; do not add them.** `minimalConfig()` sets `enableLifecycleCallbacks = false`, and `addLifecycleCallbacks` returns on that flag before the single `scope.launch(Dispatchers.Main)` that is auth-kt's only Main dispatch on Android. `AppGraphKoinTest` in `:androidApp` is the one test that builds the real graph and reaches it.
