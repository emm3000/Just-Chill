# ADR 009 implementation plan — snapshot backup

Replaces the row-replication sync engine with automatic snapshot backup: the complete versioned
JSON export, uploaded periodically to Supabase Storage. One blob per snapshot, staggered retention,
one restore path — the existing `importFromJson`.

**This is the only live sync document.** The decision of record is
[ADR 009](../adr/009-backup-is-a-snapshot-not-row-replication.md); where the two disagree, the ADR
wins. Phases 0–3 are closed and are summarised below one line per unit; their full unit-by-unit
chronicle is `docs/archive/sync/ADR009_PLAN_CHRONICLE.md`, read for the reasoning and never for what
to do next. The engine's forensic audit and the old slice plan are also in `docs/archive/sync/`.
Phase status is tracked in `docs/PROGRESS.md`, not here.

**Citations are by symbol, not by line.** A line number in prose is a guarantee nothing enforces.
Name the symbol and let `rg` find it.

## Hard constraints (violating any of these is failure)

1. **No DB schema migration anywhere in this plan.** Real accumulated data is on the device.
   The export FORMAT version (v2 → v3) is not the DB schema version; new `.sq` SELECTs are fine.
2. **Never drop** `userId` / `syncState` / `deletedAt`; never change UUID PKs; never revert to
   hard deletes. The sync SCHEMA stays so a future engine can be plugged in without re-migrating.
3. `SYNC_TEMPORARILY_DISABLED` stays `true` forever and is deleted only at the end of Phase 5.
   The new pipeline ships behind its own flag, flipped only after Phase 4 passes.
4. **No silent failure in the backup pipeline.** Every failure path logs with a distinct reason
   (the 2026-08-12 outage was invisible because the `KeepLocal` path logged nothing).
5. Phases land in order 0 → 1 → 2 → 3 → 4 → 5, each its own PR series with a green
   `qualityGate` before the next starts. Phase 5 (deletion) only after 2–4 are proven, so there
   is never a window with the old engine gone and the new backup unverified.

## Inherited from the audit — what still binds

| Finding | Where it lands |
|---|---|
| **Account inheritance.** `signOut()` clears no local data and there is one SQLite file per device, no per-user DB, no wipe on user change. So the next account inherits the previous one's rows — and under snapshot backup that ledger gets uploaded to the new account's bucket | ADR 009 Decision 5; disclosed in 3c, sign-out fixed in Phase 0 |
| **No read query filters by `userId`.** `all:`, `completeTransactions:`, `liveTotals:`, `getAccountBalance:` and `monthlyStats:` filter only `deletedAt IS NULL` | Does not retire. It is the same hazard as the row above, seen from the read side |
| **`updateFromRemote` stamps `userId` unconditionally**, and the 23 seeded default categories carry fixed identical UUIDs on every install — so the same PKs exist on every device that ever ran the app | Phase 5 cloud cleanup: this is why the two tenants' rows cannot simply be merged |
| **RLS is correct — preserve it.** `with check (user_id = (select auth.uid()))` on all four tables, policies `to authenticated` only; `delete_account()` is `security definer` with `set search_path = ''`, revoked from `public`/`anon`; `requireValidSession = true` in `SupabaseModule` | The `backups` bucket policy is modelled on it; Phase 4 item 3 is the probe |
| **`syncMutex.withLock` has no timeout**, and the mutex is a shared Koin `single` | Still open — `BackupOrchestrator` is now a second holder |

**Production forensics (input to Phase 5, never executed).** Supabase project `pievwpleqmrjwszuuivr`,
two tenants: `edmashaki@gmail.com` with 50 rows frozen at 2026-06-11, and `edgardo.emm20@gmail.com`
with 14 rows from 2026-08-12. **Zero rows with a non-null `deleted_at` anywhere** — no soft delete
ever reached production. All 13 of the second tenant's transactions point at an account owned by
the first, as do 4 of their 5 category refs; the inserts succeeded because the public schema has no
foreign keys at all, deliberately, per ADR 001. That is the "proven garbage" Phase 5 truncates.

## Phases

| # | Delivers | Status |
|---|---|---|
| 0 | ADR 009, doc hygiene, sign-out fix | closed |
| 1 | Export format v3: recurring_movements in, v2 frozen | closed |
| 2 | Snapshot pipeline (transactional export, upload, retention, orchestration) | closed |
| 3 | Health visibility in Profile | closed but for the items below |
| 4 | Restore confidence (in-app verify, drill doc, RLS probe) | **open — the flag flips here** |
| 5 | Engine decommission + cloud cleanup | open |

## Phases 0–3 — closed

One line per unit. Full chronicle: `docs/archive/sync/ADR009_PLAN_CHRONICLE.md`.

**Phase 0 — ADR 009, hygiene, sign-out.**

- ADR 009 written; it carries the decision, the alternatives table, the trigger and restore-fork
  rules, the encryption deferral and the forbidden list. LANDED — read the ADR, not a summary.
- Doc hygiene: `docs/sync/PLAN.md` and `AUDIT.md` moved to `docs/archive/sync/`. LANDED.
- Sign-out fix — the local clear runs unconditionally, the server revoke is best effort,
  `SignOutResult.Revoked` / `LocalOnly`. LANDED `336de6a3`, `41940308`.
- Salvage of the 25 abandoned engine-repair commits, decided per commit; 22 died with the engine.

**Phase 1 — export format v3.**

- ① `BackupV2` frozen — own type, hand-written fixture, compatibility test. LANDED `942dadf1`.
- ② `RecurringMovementDto` in the export, `BACKUP_SCHEMA_VERSION` → 3, dangling-category scrub
  extended to recurring. LANDED `90417807`, `55b7b8d1`, `97e92856`.
- ③ Versioned import sweep and restore mapper gated on `declaredVersion`, `ImportStats.recurring`,
  `ImportBackupDialog` copy. LANDED `69f28de5`, `dd834ab4`, `f2e1fa37`, `2bca4013`, `3cfffebe`,
  `77b2c8bd`.

**Phase 2 — snapshot pipeline.**

- 2a — export moved into one `transactionWithResult` over direct `executeAsList()`; `allLive()`
  deleted once its only caller was gone. LANDED `7a16ce19`, `fae2e147`.
- 2b-i — SHA-256 integrity primitive and the sidecar manifest. LANDED `5d43cc90`, `7dc783f0`,
  `d0b9a871`, `8e343996`.
- 2b-ii A — private `backups` bucket, owner-scoped RLS, Storage plugin. LANDED `18ac5838`,
  `30098d14`, `04cae52c`, `74a0bdea`.
- 2b-ii B — upload, read-back, verify before the manifest. LANDED `e0aef49a`, `192e6032`,
  `8335cf29`, `08267bdd`, `56be26e0`, `4b56c131`, `66f499bf`, `4359e11c`.
- 2c-i — local-change watermark and the persisted `lastSuccessfulBackupAt`. LANDED `646e2666`,
  `1da00cfa`.
- 2c-ii — snapshot naming and the retention prune. LANDED `3e3c5dd9`, `b1c2e3ce`, `2a186ebe`,
  `217d4081`, `0c510b2a`, `05296659`, `95b7bc90`, `559cf2e7`, `955b8797`.
- 2c-iii-a — `backgroundEvents()` / `resumeEvents()` in `core/lifecycle`, and the
  `BackupMetadataStore` seam. LANDED `ab7d90e7`, `ac2bed36`, `0acd47ec`, `3409560f`.
- 2c-iii-b — `BackupOrchestrator`, started by `bootstrapAppGraph` behind the flag. LANDED
  `e5989df4`, `8ab93972`, `d7fa1cff`, `e7fc821d`, `1a8df300`.
- 2c-iv — `BackupController` port and "Respaldar ahora" behind the flag. LANDED `c8a0540f`,
  `44461895`, `e1b90622`.

**Phase 3 — health visibility.**

- 3a-i — `DomainException.SerializationError` wired through every bytes/wire-format leg. LANDED
  `16943dc9`, `bc74aad1`, `f95720ad`.
- 3a-ii — persisted `BackupHealth`: watermark, failure streak, named reason. LANDED `a82a77c8`,
  `5def67d9`, `5a4c965b`, `04c248fe`.
- 3b — the "Último respaldo" row and `GetBackupStalenessUseCase`. LANDED `73843f42`, `aa455726`,
  `d6c128ed`, `9db76f3b`.
- 3c — the destination disclosure (ADR 009 Decision 5). LANDED `ec3e4fcb`, `3ce1dfb0`, `df452737`,
  `47cd0159`, `4dbe5bd5`, `8b12fc45`.

**Still open from Phases 0–3.**

- **Pin the propagating local clear.** `clearSession()` runs outside `signOut()`'s swallow on
  purpose, and nothing enforces it: `minimalConfig()`'s `MemorySessionManager` cannot fail, so
  wrapping it leaves `DefaultAuthRepositorySignOutTest` green. `AuthConfig.sessionManager` is a
  plain `var`, so the fix is a `SessionManager` whose `deleteSession()` throws, set after
  `minimalConfig()` runs, asserting `signOut()` propagates instead of returning `LocalOnly`.
  Phase 5 rewrites `DeleteUserAccountUseCase` around the same method and must not inherit the gap.
- **"Storage error" is the one failure reason still indistinguishable.** A Supabase `RestException`
  — the bucket's 413 over the 10 MiB ceiling, its 415 for a refused content type, any 5xx — maps to
  `DomainException.Unknown` with the status in the message string, as does the `.json` naming defect
  and any unanticipated throwable. Closing it needs a `DomainException` member carrying the status
  as data plus the `:data` mapping to raise it: a `:domain` + `:data` change, not a presentation one.
- **No per-reason UI route.** `ProfileViewModel.onBackupEvent` collapses every `BackupEvent.Failed`
  to one `ProfileMessage.BackupFailed`, so `SerializationError`'s Spanish string is correct and
  unreachable. Finer copy, or routing the failure through `toUserMessage()`, is unowned.
- **`ProfileOp.VerifyingBackup`** does not exist yet; Phase 4's verify action needs it, and the
  concurrent-op guard already emits `OperationInProgress`.
- **Close the `SyncMutex` timeout defect** (recorded in `docs/PROGRESS.md`) before the pipeline
  ships. `BackupOrchestrator` is now a second holder alongside `DeleteUserAccountUseCase`, so a
  stuck upload blocks account deletion indefinitely — the engine's defect under a new name.
- **The hash-mismatch watermark case is unpinned.** `DefaultBackupUploaderTest` pins that a
  mismatched upload throws, and `a failed upload records no watermark and the next trigger retries`
  covers the shape, but nothing asserts the orchestrator leaves the watermark unwritten *for that
  specific failure*.

## Constraints inherited from closed phases

Everything a future unit still has to respect. Each item is a property something already relies on,
not a description of what shipped.

**Preferences and persisted keys**

- Backup preference keys live in `DefaultBackupMetadataStore` (`:presentation/core/backup/`), which
  takes `Settings` directly — **not** in `AppPreferences`. The prefixes
  (`last_successful_backup_at_`, `backup_failure_`, `backup_destination_disclosed_at_`), the `-1L`
  "never" sentinel and the `'|'` failure separator are byte-identical to what they were before the
  move and are load-bearing: renaming one reads as "this device never backed up".
- Both halves of the failure streak stay under **one** key (`count|REASON`). `Settings` has no
  transaction, so two keys are two commits, and a process killed between them leaves a count
  carrying the previous outage's reason. `clear(userId)` removes all three keys.
- `SNAPSHOT_BACKUP_ENABLED` has never been `true`, so no install carries any of these keys yet — a
  rename is free today and stops being free the moment the flag flips.

**Refusals are not failures**

- An undisclosed destination and `SnapshotOutcome.OwnerChanged` are **refusals**: no streak
  increment, no `BackupFailureReason`, no watermark touched, no `BackupEvent` emitted. Any new
  refusal follows that shape. The uploader's *pre-upload* account assertion is the exception — it
  throws and does count.
- `DomainException.Unauthorized` cannot separate an account switch from an expired session, and both
  existing readings of that type (sign out; "sesión expirada") are wrong for a user who is signed in
  just as somebody else. **No backup failure signs the user out**: a failure reaches the UI as
  `ProfileEffect.Notify`, never `ShowError`, the only effect that reaches `toUserMessage()`.
- Failure state is booked against the account the **cycle captured**, and published only while that
  account is still signed in — `publishHealth` re-checks after the write and reverts with
  `compareAndSet`. The watermark is re-checked after the upload for the same reason: a false
  watermark suppresses that account's next backup for a whole day.
- The disclosure gate is the **first** thing `takeSnapshot` checks, ahead of the `isBackupDue` check
  a manual request skips. `uploader.upload` has exactly one call site, reachable only through
  `takeSnapshot` ← `runBackup` ← the single `requestChannel` consumer; any new gate belongs at that
  choke point or a tap walks around it.

**The Perfil row (`BackupRowUi`)**

- Rank order is load-bearing, not cosmetic: `NeedsAccount` > `DisclosurePending` > `BackingUp`.
  `NeedsAccount` first because a cycle still in flight when the session ends would render
  "Respaldando…" to a signed-out user; `DisclosurePending` above `BackingUp` because the
  orchestrator raises `isBackingUp` for the whole cycle including the one it is about to refuse;
  below `NeedsAccount` because the disclosure is per-account and there is nobody to disclose to.
- `DisclosurePending` severity is `Warning`, never `Danger`. `Danger` means the ledger is
  unprotected **and will not become protected on its own**; the disclosure's remedy is the button in
  the same row and it fires on every first sign-in, so colouring routine onboarding red destroys red.
- A failure **annotates** the snapshot, never replaces it: `Failed` carries a `LastSnapshot`
  (`None` / `AgeUnknown` / `DaysAgo(days, isStale)`), never a nullable `Int`.
- **Warn on the count, never on the reason.** `BackupFailureReason.fromNameOrNull` answers null for a
  member this build no longer names, so `(5, null)` is reachable and a null-reason branch is required.
- State resolution and copy live in `:presentation` (`BackupRowUi.severity()`,
  `BackupRowUi.toMetaText()`), never in a composable — SwiftUI must reach the same answer.

**detekt ceilings**

- `TooManyFunctions` allows 11 functions per class. `DefaultBackupRepository`, `BackupOrchestrator`
  and `ProfileViewModel` each sit at **exactly 11** today: the next member on any of them needs a
  top-level hoist first, the shape `usableCategoryId`, `resolveBackupRow` and `snapshotRow` already
  took. The failure looks unrelated to the change that caused it. (3c's key move is what dropped
  `AppPreferences` off that ceiling.)

**Untested surfaces**

- **`:ui-android` has no Compose UI test harness** — its `androidHostTest` is plain JVM, no
  `ui-test-junit4` and no Robolectric in the catalog. So no flag gate is asserted anywhere: nothing
  proves "Respaldar ahora", "Último respaldo" and the disclosure row are absent while
  `SNAPSHOT_BACKUP_ENABLED` is false, nor that they and `bootstrapAppGraph`'s `start()` read the same
  constant. Paired comments at each site are the whole enforcement; Phase 4 exercises the pair.
- **Nothing in CI protects the bucket's RLS predicate.** `qualityGate` cannot see a SQL policy and
  the repo has no Supabase test infrastructure; the only proof the bucket is owner-scoped is a manual
  probe run once against a local stack. Phase 4 item 3.
- **`AppGraphKoinTest` cannot see a definition that was never registered**, because it iterates the
  registry. Anything whose only consumer is a direct `koinInject` / `koin.get` outside the graph
  stays invisible (the `CommitHash` shape, resolved by `AndroidPlatformModuleTest` instead). Register
  every new binding and resolve it from the graph or from its own module test. Rule recorded in
  `presentation/CLAUDE.md`.

**Export format and version gates**

- **Version gates are frozen literals** — `BACKUP_RECURRING_SINCE_VERSION`,
  `BACKUP_SCHEMA_VERSION_V2` — never `BACKUP_SCHEMA_VERSION`. A gate written against the current
  version silently stops restoring every older file the moment a v4 exists.
- **The version number moves with the shape, in the same commit that changes the shape.** Bumping
  first writes old-shaped files stamped with the new version, which no frozen reader rescues.
- Each format version needs its own frozen type, a **hand-written** fixture and a compatibility test
  before the constant moves. Item DTOs are deliberately **shared** across versions, and those
  per-version suites are the only guard on that sharing.
- Gate on the file's `declaredVersion` from `DecodedBackup`, never on an array being empty: every
  `toCurrent()` restamps `payload.schemaVersion`, so files of different versions are otherwise
  indistinguishable.
- The category detach (`clearCategoryOnTypeChange`) runs on **every** version, unversioned. A
  tombstone is not a delete: a swept row still holds its old `(categoryId, type)` pair and still
  makes SQLite refuse the category's type change.
- The recurring restore runs **last** inside the import transaction — that is the only position where
  `usableCategoryId` reads the file's own categories rather than a world the import is mid-replacing.
- **From v3 on, every file this app exports is unreadable on any older build still installed.**
  `decodePayload` reads the declared version before deserializing and refuses an unknown one, so a
  rollback or a sideloaded older APK cannot read current backups. Loud and correct; plan around it.
- `BACKUP_SCHEMA_VERSION` sits on `JustChillKit`'s ABI via `export(project(":data"))`.

**Pipeline invariants**

- **Upload order carries the meaning**: payload → read-back → manifest. The presence of
  `<name>.json.manifest.json` is the statement that the payload beside it was verified. It holds only
  because the bucket grants no `update` and uploads use `upsert = false`.
- **The sidecar name APPENDS the extension.** The bucket's mime check is a verbatim string match and
  storage-kt derives the header from the key, so a name that stops ending in `.json` is an HTTP 415
  that reads like a server fault. Send a bare `application/json` — never with a charset parameter.
- **`storage.protect_delete()` refuses `delete from storage.objects` outright**, whatever the policy
  says. Every delete goes through the Storage API; the delete policy authorises it, it does not
  perform it. Resumable upload is unavailable (`storage.s3_multipart_uploads` has RLS with no
  policies), not merely unused.
- **Retention slots fill from the data, never from today's calendar** — a slot is one distinct day,
  ISO week or month that actually holds a snapshot. Future-stamped snapshots run the same three
  buckets on their own shelf, so the storage bound is `7 + 8 + 12` per shelf, 54 in total. `pinned/`
  is never scanned.
- **A prune that deletes on a failure is worse than one that skips**: any read failure — prefix, page,
  page cap — aborts before the first delete; an individual failed delete is recorded in
  `BackupPruneReport.failedDeletes` and never stops the run. The pager terminates only on a
  server-returned count of **zero** and advances by that count, never by the requested limit.
- **An orphan payload is deleted on sight**, because a verified-but-orphaned payload cannot be told
  from an unverified one and the second failure is the one this ADR exists to end. A
  *complete-looking pair* whose manifest is bad is kept — which is why any pre-restore check **must
  walk back to the newest snapshot that verifies** rather than declaring the newest one broken.
- **`Storage.Config.transferTimeout` is 120s and bounds every Storage call** (list and delete
  included), not the 10s `requestTimeout` that governs Postgrest. `SupabaseModule` configures
  neither, and a BOM bump can move either silently.
- Hard constraint 4 is cashed out as **thirteen named failures**; a new failure path adds a
  fourteenth rather than reusing one.
- Whatever asks "what day is it" takes an injected `Clock` **and** `TimeZone`, neither carrying a
  default (`docs/DATE_AUDIT.md` rule 7). Staleness is `BACKUP_STALE_AFTER_DAYS` (3) **calendar** days
  AND a ledger that moved since the last verified snapshot — both halves required.
- `hasLocalChangesSince` in `:domain` is the single predicate for "is there anything to back up": the
  gate that runs a cycle and the row that warns the user must not be able to disagree.

**Reaches beyond Android**

- **`appVersion` is hardcoded `"1.0.0"` on iOS** (`presentation/src/iosMain/.../KoinIos.kt`), where
  Android reads `BuildConfig.VERSION_NAME` — so every snapshot an iOS device uploads stamps a false
  version into its payload and its manifest. Its own future unit.
- **iOS exports `BackupRowUi.DisclosurePending` but renders nothing for it** — an iOS-slice item.

**Known product-visible gaps once the flag flips**

- A user who signs into a new account and **never opens Perfil** gets no backups and no signal
  outside that screen: the disclosure gate refuses every cycle and the only place that says so is the
  row they are not looking at.
- The Perfil day count does not refresh across midnight — computed per emission, deliberately without
  a ticker, the same disclosure `ReportUiState.isCurrentMonth` carries (`docs/DATE_AUDIT.md` #7).
- Session and health are two flows, so across an account switch there is one emission where B's
  session pairs with A's watermark. A display seam, not a recording one.

### Phase 4 — restore confidence (the flag's gate)

1. **In-app "Verify backup"**: download latest snapshot, check hash, parse, compare per-table
   counts against local — **without applying it**. Result in UI. **It must walk back to the newest
   snapshot that verifies rather than reporting the newest one broken**: 2b-ii part B can leave a
   complete-looking pair whose manifest is wrong or of unknown state — the note under Retention
   enumerates the two ways — and a check that only looks at the latest pair turns one bad receipt
   into "you have no backup" on a device holding a shelf of good snapshots. Which one was actually
   verified is what the UI must name, not just "OK".
2. **Documented drill** added to the release checklist: after every DB schema bump, restore the
   latest production snapshot on a clean emulator and compare.
3. **An automated RLS probe for the `backups` bucket**: two authenticated users, each proving it
   reaches its own prefix and cannot list, read or delete the other's. Today that proof exists only
   as a manual probe run once by hand against a local stack (2b-ii part A) — `qualityGate` cannot
   see a SQL policy and the repo has no Supabase test infrastructure. This is where it stops being
   a thing someone remembered to do.

Only when all three pass does `SNAPSHOT_BACKUP_ENABLED` flip to `true`.

- The round-trip test is done (`BackupRoundTripTest`, `:data` host tests). It runs the fixture through
  two compositions: `export → physical wipe → import`, and `export → stale the live rows → import`
  **without wiping**. The second one is the production path — `importFromJson` never deletes
  physically, so on a device that already holds the row the INSERT is ignored on PK conflict and
  `restoreFromBackup` is the only statement that acts. Dropping a bound `SET` clause from it is a
  compile error, but dropping the literal `deletedAt = NULL` is not: without the no-wipe composition
  that regression stays green while a real restore leaves every row tombstoned and the app empty.
- **Open, found while writing that test**: `RecurringMovementDto.toEntityOrNull` runs
  `lastConfirmedPeriod` through `parsePeriodKey` and keeps the value only if it parses. A key that is
  malformed or whose year falls outside `MIN_PERIOD_KEY_YEAR..MAX_PERIOD_KEY_YEAR` is **silently
  nulled on import**, so a settled template comes back unsettled and `RecurringDueRules.pendingPeriods`
  re-mints up to `MAX_CATCH_UP_MONTHS` the user already closed. A value this app wrote round-trips
  fine; the exposure is a key an older build left in the DB. No coverage.

### Phase 5 — decommission the engine

Verified inventory (against trunk, 2026-08-13). Four buckets — delete, move, rewrite, keep:

| Action | What |
|---|---|
| Delete, `:data` / `:domain` | the 9 files in `data/sync/`; `ConflictResolver`, `SyncCursorStore`, `SyncDataUseCase`, `SyncRepository`; the claim machinery (`ClaimLocalDataOnAuthenticationUseCase` + its un-gated observer in `bootstrapAppGraph` — local `userId` is dead metadata without a push) |
| Delete, `:presentation` | `SyncOrchestrator`, `SyncController`, `SyncStatus`, `SyncEvent`, `DefaultSyncCursorStore`; the cursor keys in `AppPreferences`; the sync surface of `ProfileViewModel` (it consumes `SyncStatus`/`SyncController` for the Perfil badge) |
| Delete, `:ui-android` | `hh/shared/SyncEventsHandler.kt` — whose own KDoc calls itself *"the ONLY collector of `SyncController.events`, and that is load-bearing"* — plus the sync references in `AppNavHost` and `AppNavigator`. **Miss these and the Android build breaks**, since the `:presentation` deletions above remove what they collect |
| Move | **Nothing left.** The one entry this row carried — the `appScope` single (`named("appScope")`) out of `syncModule` into CoreModule — is **already done**: 2c-iii-b pulled it forward (`8ab93972`), because the orchestrator needs an application-lifetime scope and could not take one from a module Phase 5 deletes. It was never engine property; that is why it survived rather than moving. |
| Rewrite | `DeleteUserAccountUseCase` — its "unclaim" and "cursor clear" steps are engine parts; remote delete becomes: delete the user's Storage objects + the auth user. Also carries the sign-out fix Phase 0 gave `DefaultAuthRepository.signOut()`: `DefaultAuthRepository.deleteAccount()` still calls bare `client.auth.signOut(SignOutScope.LOCAL)` after the `delete_account` RPC, with the identical defect — a network failure mid-request leaves the device holding a session for a user that no longer exists server-side. Its KDoc used to claim the call "clears the on-device session"; Phase 0 corrected that, **by inspection of `AuthImpl.signOut` — not by test.** `DefaultAuthRepositorySignOutTest` exercises `signOut()` only; nothing runs `deleteAccount()`'s call shape, so this defect is unpinned and a fix here has no red test to turn green. Not fixed in Phase 0: `deleteAccount()` is its own contract question (the account is gone either way, so "qualified success" may not even be the right shape there) |
| Keep | `SyncMutex`, renamed to drop the "Sync" prefix. After this phase its only holders are the rewritten `DeleteUserAccountUseCase` and `BackupOrchestrator`. The logger this row used to name alongside it is **already done**: 2c-iii-b moved it to `domain/.../shared/logging/DiagnosticsLogger.kt` with its two platform impls (`CrashReportingDiagnosticsLogger`, `PrintlnDiagnosticsLogger`), because the orchestrator needs it for constraint 4 and could not depend on a sync-named symbol. Also `resumeEvents()`, and `importFromJson` + the export code — they are the product now |

Also in scope, and easy to miss: `export(project(":data"))` in `presentation/build.gradle.kts` puts
the whole engine in `JustChillKit`'s public ABI, for zero consumers. Re-check what that export
still needs to carry once the deletions land.

- The local DB schema is untouched (constraint 2). Dead columns stay dead in place.
- **Cloud cleanup is destructive and outward-facing**: truncating the two tenants' row tables
  (proven garbage — see the forensics above) happens only with explicit owner confirmation at the
  moment, after a pinned and verified snapshot exists. Auth stays — Storage RLS needs it.
- Remove `SYNC_TEMPORARILY_DISABLED` and its gates last, once nothing references the engine.
- `supabase/migrations/20260610043814_sync_schema.sql` opens with "Mapping rules (see
  docs/sync/PLAN.md)", a path archived on 2026-08-13. Deliberately NOT corrected in place: the
  Supabase CLI can detect drift on an already-applied migration by content, and risking a migration
  history mismatch on a project awaiting tenant cleanup costs more than a stale pointer in a comment.
  Fix it in this phase, where `supabase/` is being touched anyway and the outcome can be verified
  against the server. Outside `docs/archive/`, it is the **only** pointer that still asks a reader
  to follow either archived path — every other `rg 'docs/sync/(AUDIT|PLAN)\.md'` hit merely
  describes the old paths or records the move.

## Settled — do not reopen

Shared accounts (rejected by the product definition, `PRODUCT_REQUIREMENTS.md` W-04, "Multi-cuenta
compartida (parejas, equipos)"). Multi-device, which is a different rejection — ADR 006, one device
at a time, and W-04 does not cover it. CRDTs,
server-side LWW/conditional upsert, push-only row sync. Fixing the loop and keeping the engine.
E2E encryption now (deferred by ADR — a Keystore-bound key dies with the phone). Any DB schema
migration.
