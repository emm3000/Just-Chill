# ADR 009 implementation plan — snapshot backup

Replaces the row-replication sync engine with automatic snapshot backup: the complete versioned
JSON export, uploaded periodically to Supabase Storage. One blob per snapshot, staggered retention,
one restore path — the existing `importFromJson`.

**This is the only live sync document.** The decision of record is
[ADR 009](../adr/009-backup-is-a-snapshot-not-row-replication.md); where the two disagree, the ADR
wins. Everything actionable is here. The engine's forensic audit and the old slice plan are
historical and live in `docs/archive/sync/` — read them for *why* this exists, never for what to do
next. Phase status is tracked in `docs/PROGRESS.md`, not here.

**Citations are by symbol, not by line.** A line number in prose is a guarantee nothing enforces:
six of the eight `file:line` refs inherited from the audit had already rotted by 2026-08-13. Name
the symbol and let `rg` find it.

**Base**: a new branch off `origin/trunk`. NOT `worktree-sync-phase-1-identity` — that branch's
25 commits repair the engine this plan removes, and are abandoned (Phase 0 scanned them once for
salvage).

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

The engine's audit is archived, but five of its findings survive the engine and constrain the work
below. Everything else in it described machinery this plan deletes.

| Finding | Where it lands |
|---|---|
| **Account inheritance.** `signOut()` clears no local data and there is one SQLite file per device, no per-user DB, no wipe on user change. So the next account inherits the previous one's rows — and under snapshot backup that ledger gets uploaded to the new account's bucket | ADR 009 Decision 5; the disclosure in Phase 3, the sign-out fix in Phase 0 |
| **No read query filters by `userId`.** `all:`, `completeTransactions:`, `liveTotals:`, `getAccountBalance:` and `monthlyStats:` filter only `deletedAt IS NULL` | Does not retire. It is the same hazard as the row above, seen from the read side |
| **`updateFromRemote` stamps `userId` unconditionally**, and the 23 seeded default categories carry fixed identical UUIDs on every install — so the same PKs exist on every device that ever ran the app | Phase 5 cloud cleanup: this is why the two tenants' rows cannot simply be merged |
| **RLS is correct — preserve it.** `with check (user_id = (select auth.uid()))` on all four tables, policies `to authenticated` only; `delete_account()` is `security definer` with `set search_path = ''`, revoked from `public`/`anon`; `requireValidSession = true` in `SupabaseModule` | Phase 2b models the Storage bucket policy on it |
| **`syncMutex.withLock` has no timeout**, and the mutex is a shared Koin `single` | Phase 3 — `BackupOrchestrator` becomes a second holder |

**Production forensics (input to Phase 5, never executed).** Supabase project `pievwpleqmrjwszuuivr`,
two tenants: `edmashaki@gmail.com` with 50 rows frozen at 2026-06-11, and `edgardo.emm20@gmail.com`
with 14 rows from 2026-08-12. **Zero rows with a non-null `deleted_at` anywhere** — no soft delete
ever reached production. All 13 of the second tenant's transactions point at an account owned by
the first, as do 4 of their 5 category refs; the inserts succeeded because the public schema has no
foreign keys at all, deliberately, per ADR 001. That is the "proven garbage" Phase 5 truncates.

## Phases

| # | Delivers | Gate to next phase |
|---|---|---|
| 0 | ADR 009, doc hygiene, sign-out fix | ADR merged, sign-out change green |
| 1 | Export format v3: recurring_movements in, v2 frozen | Round-trip of v1/v2/v3 green |
| 2 | Snapshot pipeline (transactional export, upload, retention) | Hash-mismatch test green |
| 3 | Health visibility in Profile | — |
| 4 | Restore confidence (CI round-trip, in-app verify, drill doc) | **Flag flips here** |
| 5 | Engine decommission + cloud cleanup | — |

### Phase 0 — ADR 009, hygiene, sign-out

ADR 009 is written and carries the decision, the alternatives table, the trigger and restore-fork
rules, the encryption deferral and the forbidden list. Read it rather than this summary.

Doc hygiene (2026-08-13): `docs/sync/PLAN.md` and `docs/sync/AUDIT.md` moved to `docs/archive/sync/`,
their surviving findings absorbed above, and the line-number citations replaced with symbol names.
Seven documents described three different architectures at once with nothing marking which sentence
was live; that cost a wrong salvage verdict in this very file (see below).

**The sign-out fix.** `SignOutUseCase` → `AuthRepository.signOut()` → `DefaultAuthRepository.signOut()`
→ `client.auth.signOut()`, with no `SignOutScope` argument. supabase-kt 3.7.0's `AuthImpl.signOut`
posts `logout` whenever a session exists — for **every** scope, `LOCAL` included — and wraps that
post in `catch (e: RestException)`. A network failure is `HttpRequestException`, an `IOException`,
so it is not a `RestException`: it escapes before `clearSession()` runs and **the device stays
signed in**. `SignOutScope.LOCAL` narrows which sessions the server revokes; it does not remove the
round-trip.

Under ADR 009 that is no longer cosmetic. The pipeline uploads only while a session exists, so
**signing out is the mechanism for ceasing to upload to an account.** A sign-out that fails
silently means the next background snapshot still goes to the account the user believes they left.
The disclosure of Decision 5 rests on it too — it fires on the first upload to a *new* destination,
which never happens if the old session never ended.

**Shipped** in `336de6a3` + `41940308`. `signOut()` runs the local clear unconditionally — outside
the swallow, so a broken session store propagates instead of being reported as success — and
attempts the server-side revoke as best effort, returning `SignOutResult.Revoked` / `LocalOnly`
when only the revoke failed. The accepted
cost is a refresh token that stays valid remotely until it expires when the sign-out happened
offline; the alternative leaves the user signed in to an account they are actively trying to
abandon, which is the worse failure under this plan.

**One method, not two.** The abandoned `9b86dfdb` added `signOutLocally` *alongside* `signOut`,
arguing the Perfil button wanted the strictest sign-out and could surface a failure to a user free
to ignore it. That argument held while its consumer was a non-dismissible dialog and inverts here —
and after Phase 5 removes `SyncOrchestrator`, the Perfil button is the only sign-out caller left, so
the split would leave one method dead. A failed remote revoke is therefore a qualified success, and
the UI says so out loud rather than reporting an error while leaving the user signed in.

**One property the tests pin**, because prose asserting it would not: `CancellationException` is
rethrown before the generic catch. The test that pins it asserts the session survives cancellation,
because asserting that `await()` throws cannot discriminate the mutation — cancelling a coroutine's
own job makes `await()` report cancellation whatever the body did.

**One property they do not.** `clearSession()` runs **outside** the swallow, so a broken session
store still propagates — the user is still signed in and has to hear about it. That is the intended
design and the code does it, but nothing enforces it: all three client factories in
`DefaultAuthRepositorySignOutTest` build auth with `minimalConfig()`, whose in-memory
`MemorySessionManager` cannot fail, so wrapping `clearSession()` in its own swallow leaves all four
tests green. Recorded as a follow-up below.

`Revoked` means the server did not refuse, not that a live token was destroyed: supabase-kt's
`AuthImpl` carries `SIGN_OUT_IGNORE_CODES` for 401/403/404, so an already-invalid JWT returns
normally.

**Open follow-up — pin the propagating local clear.** The one Phase 0 property that is prose only.
A test needs a session store that can fail, which `minimalConfig()` cannot supply; `AuthConfig`
exposes `sessionManager` as a plain `var`, so the fix is to set a `SessionManager` whose
`deleteSession()` throws *after* `minimalConfig()` runs, then assert `signOut()` propagates instead
of returning `LocalOnly`. Until that test exists, wrapping `clearSession()` in the swallow is an
undetectable regression. Not written in Phase 0 — logged here so Phase 5, which rewrites
`DeleteUserAccountUseCase` around the same method, does not inherit the same blind spot.

Salvage of the 25 abandoned commits, decided per commit:

- `9b86dfdb` (`SignOutLocallyUseCase`) — **re-apply, do not cherry-pick.** Its earlier verdict in
  this file ("no consumer exists on trunk, park it") was correct when the consumer was the
  branch-only re-point dialog, and is wrong now: the consumer is the Perfil button. Its KDoc
  references `RepointBackupDestinationUseCase`, which does not exist and never will — rewrite the
  prose against the snapshot pipeline before it lands.
- `4b0b6402` (`OP_TIMEOUT` hung-logout test) — the test is a real candidate and pins a real
  mechanism. It also edits `BackupDestinationViewModel`, which does not exist on trunk; only the
  `DefaultAuthRepositorySignOutTest` half is portable. Its timeout finding is recorded in Phase 2b.
- The other 22 repair the engine this plan deletes. They die with it.

### Phase 1 — recurring_movements into the export format (v3)

Commit order: ① freeze v2 → ② v3 with recurring → ③ versioned sweep. Nothing else ships until this
phase is merged and green — without it, a snapshot restore loses recurring movements on device loss.

**① Freeze v2 first.** `BACKUP_SCHEMA_VERSION` is currently 2 and v2 files are read by the *current*
DTO (`ExportPayloadDto`). `decodePayload` dispatches with a `when` over the **declared** version Int
— no sealed type, no migration chain, and an absent `schemaVersion` key means version 1 — and its
branches are const-driven. So the moment that constant reads 3, a file declaring 2 matches no branch
at all: it falls to `else` and is refused as `ValidationCode.BackupVersionUnsupported`. Not
misread — **refused**. Every v2 file already sitting on a user's disk stops loading, and the app
tells them an intact backup cannot be restored. That is precisely what `BackupV1` exists to prevent
and its KDoc argues at length: a backup lives "in storage the user chose, outside the app, beyond
the reach of any schema migration", and the ugliest failure mode is "telling the user their
perfectly good file is corrupt". Create `BackupV2` — own frozen type, hand-written fixture,
`BackupV2CompatibilityTest` — so that branch exists before anything can need it.

**The version number moves with the shape, never before it.** Step ① lands the frozen v2 type, its
fixture and its test, and nothing else; step ② adds the field, moves `BACKUP_SCHEMA_VERSION` to 3
and wires the v2 branch, in one commit. Bump inside the freeze commit instead and the window between
that commit and the shape landing writes **v2-shaped files stamped 3** — and the sweep in ③ keys off
the declared version, so on restore those files take the v3 path, their absent recurring array reads
as "this device had none", and the wipe runs with nothing to put back. No frozen reader rescues a
file that self-declares 3.

**The freeze is deliberately partial, and v3 inherits that.** `BackupV1` froze only what changed:
`ExportPayloadV1Dto` holds `List<AccountDto>` and `List<CategoryDto>` — the live DTOs, not copies —
and only the transaction shape was frozen. Its KDoc says so: *"Only the transaction changed between
the two versions; accounts and categories are shared."* Decision taken: `BackupV2` follows the same
pattern and shares the three item DTOs with v3. The coupling that buys is real — editing a shared
item DTO edits what every version's reader parses — and it is safe only because the guard already
exists and works: hand-written fixtures plus per-version compatibility tests turn red on both
readers at once. Written down so nobody discovers it by breaking it.

Two consequences of that bump — both land in ②, not ①, and no compiler flags either:

- `DefaultBackupRepositoryTest` asserts the literal `2` against `payload.schemaVersion` in two
  places, not the constant. The bump commit expects exactly those two reds — a suite that stays
  green means the bump did not land.
- `BACKUP_SCHEMA_VERSION` is public and `:presentation` does `export(project(":data"))`, so the
  constant sits on `JustChillKit`'s ABI. Phase 5 revisits that export; here it only means the bump
  is visible to iOS.

**② v3 — `RecurringMovementDto`.** Fields: id, name, type, amount (nullable), description,
categoryId, accountId, frequency, dayOfMonth, isActive, **lastConfirmedPeriod**, **createdAt**.

The last two are behavior, not storage stamps, and dropping them loses data in **opposite**
directions:

- **`lastConfirmedPeriod`** — nullable TEXT holding a zero-padded `"YYYY-MM"` period key; string
  ordering is chronological ordering and `ensureNotSettled` depends on that. Without it a restore
  re-mints the already-confirmed month.
- **`createdAt` — the one that deletes work in silence.** `RecurringDueRules.pendingPeriods` floors
  the catch-up window at `max(createdMonth, oldestAllowed, afterMark)`, and `createdMonth` derives
  from `rm.createdAt`. Every existing restore path stamps `createdAt = now`; applied to a recurring
  movement that lifts the floor to the restore month and **every owed past period vanishes**. No
  error, no row, nothing to notice.

Both need a test, and one test cannot cover both: they are opposite failures. *A restore does not
re-mint a confirmed period; a restore does not swallow the owed ones.*

`frequency` has a single enum member today, its column carries a DEFAULT, and `update:` never writes
it — it is in the list because the file is a format, not a snapshot of today's cardinality.

**Open — the export path has no all-live read for recurring.** Export reads through the three domain
repository interfaces (`all()`) while import writes through `db` directly, and that asymmetry is
what lets the repository test mock every read. `RecurringMovementRepository`'s only **all-row**
reads are `allActive()` — filters `isActive = 1`, so exporting through it drops every paused
template — and `allWithDetails()`, a joined details model; everything else on the interface is
per-id or per-account work. So either the domain repository gains a method, or export reads `db`
directly for this one table and the export test loses its mockability. Decide it in the commit
that writes the export, not here.

Correcting this plan's own earlier claim: `selectAllWithDetails:` **does** return every live row,
paused templates included. What does not exist is a **flat** all-live select — that one is joined
and carries three extra columns, and every flat select (`selectActive:`, `find:`, `selectPending:`)
filters something out.

**Extend the dangling-category scrub to recurring.** `exportToJson` nulls the categoryId of
transactions whose category is tombstoned (`DefaultBackupRepository`); `recurring_movements` carries
the same composite FK `(categoryId, type) → categories(categoryId, categoryType)`, identical to the
one on `transactions`, so without the same scrub the exported file fails its own import.

**③ The SQL is four statements, not one SELECT.** `recurring_movements.sq` declares
`clearCategoryOnTypeChange:` and full soft-delete like the other three tables, but **none** of the
three write statements the sweep and restore paths consume on those tables: `softDeleteAllLive:`,
`insertOrIgnoreFromBackup:`, `restoreFromBackup:`. The insert-then-update restore idiom — chosen
over `INSERT OR REPLACE`, which physically deletes the old row and is therefore refused by
`ON DELETE RESTRICT` while anything still references it, with the reasoning in a comment block above
those statements in `accounts.sq` — has no counterpart on this table at all. Those three plus the
flat all-live select ② needs are the phase's SQL. New `.sq` statements are not a migration
(constraint 1).

**Version the import sweep — the data-loss trap.** With v3 the sweep includes recurring **only for
v3+ payloads**; importing a v1/v2 file must keep leaving local recurring intact, since those formats
carry none and wiping them would destroy data no backup can restore. Pin both sides with tests:
*importing v1/v2 leaves recurring untouched; importing v3 replaces it.*

**"The import leaves recurring untouched" is true of the sweep and false of the import.**
`restore(dto: CategoryDto, …)` already calls `clearCategoryOnTypeChange` on
`recurring_movementsQueries`, nulling `categoryId` on every recurring row whose `type` disagrees
with the restored category's `categoryType`. Its KDoc explains why: SQLite otherwise refuses the
parent-key change, and inside the one transaction wrapping the restore that is not one failed row —
it is the whole import rolled back after the tombstone sweep. Only the narrow claim holds: the sweep
neither wipes nor restores that table. Today.

**Open — statement order inside the transaction.** Under a v3 sweep that also wipes and restores
recurring rows, that detach and the recurring restore land in the **same transaction**, and their
relative order becomes a correctness question: run the detach first and it scrubs rows about to be
replaced anyway; run it after the restore and it may scrub the pairs the file just wrote. Name the
invariant before writing the statements. This plan does not choose it.

**Two stale KDocs, in scope because v3 makes them wrong on the destructive path.** In
`DefaultBackupRepositoryImportTest`, the KDoc above the test for a backup that redefines a
category's type asserts *"Import never touches `recurring_movements`"* — while the test directly
beneath it asserts that exact mutation. `restore(dto: CategoryDto, …)`'s own KDoc says the same
("the import deliberately never touches that table"). Both are defects now and worse after v3;
correct them in the commit that changes the behavior.

**`ImportBackupDialog` (`:ui-android`) is user-facing and goes stale.** Both its `isSignedIn`
body-copy branches enumerate the tables in Spanish — *"Tus movimientos, categorías y cuentas quedan
tal cual el archivo"* — and its KDoc makes the same list in English ("replaces every movement,
category and account"). Both become incomplete the moment v3 sweeps recurring, on the one screen
where the user consents to a destructive operation. Correctness, not copy polish, and in this
phase's scope. The body copy stays Spanish; it is UI.

**`ImportStats`** gains a `recurring` count (today `accounts` / `categories` / `transactions` only),
plus the UI message that reports it.

**Known downgrade behavior, recorded not fixed.** `importJson` is configured with
`ignoreUnknownKeys = true`, so an older app build reading a v3 file drops `recurringMovements`
silently instead of failing. Nothing here changes that — it is written down so a restore that comes
back short on an old build is diagnosable rather than mysterious.

### Phase 2 — snapshot pipeline

Three sub-phases, three PR series: the transactional refactor is its own risk and must be
reviewable alone.

**2a — transactional export.** `exportToJson` does three independent `Flow.first()` reads on
trunk, and one more once Phase 1 adds recurring — whether that fourth read is a `Flow` or a
direct `db` query depends on how Phase 1 settles its export-read question. A write landing
between any two of them corrupts the snapshot. SQLDelight transactions are synchronous
blocks, so `.first()` cannot run inside one: switch the export to direct queries
(`executeAsList()`) inside a single `transactionWithResult`. `DefaultBackupRepository` already
holds the `db`; the refactor is contained to that class.

**2b — storage.** Add `storage-kt` to the catalog (BOM 3.7.0 currently ships only auth-kt +
postgrest-kt; zero Storage usage exists in the repo). Bucket + RLS restricted to the owning
user — model it on the row-table policies named above — created as a SQL migration under
`supabase/`, where that CLI infra already exists. Integrity: SHA-256 of the payload + sidecar
manifest (filename, hash, format version, per-table row counts); after upload, read back and
verify the hash before marking success. KMP has no SHA-256 in the common stdlib: use okio's
`ByteString.sha256()` if okio is already transitive, else an `expect/actual` over
`java.security.MessageDigest` / CommonCrypto.

**The request timeout is 10s**, and it is not configured here. supabase-kt 3.7.0's
`KtorSupabaseHttpClient.applyDefaultConfiguration` installs `HttpTimeout` on every client it
builds, custom engine included, reading `SupabaseClientBuilder.requestTimeout`, whose default is
`10.seconds`. `SupabaseModule` overrides nothing, so that default is what runs. Size upload
retries and any outer timeout above it. **Caveat**: read once out of the Gradle cache on
2026-08-13 and nothing enforces it — a BOM bump can change it silently. Installing `HttpTimeout`
explicitly in `SupabaseModule` would make it visible, but it also moves the sync push path, so it
belongs to its own unit.

**2c — orchestration.**

| Concern | Design |
|---|---|
| Dirty flag | `max(updatedAt)` across the 4 tables (new SELECTs; `softDelete` already bumps `updatedAt`, so deletes mark dirty) vs `lastSuccessfulBackupAt` in `AppPreferences` — the facade already holds cursor/lastSyncedAt keys |
| Trigger | new `backgroundEvents()` expect/actual, sibling of `resumeEvents()` (ProcessLifecycleOwner ON_STOP / UIApplicationDidEnterBackground), capped at 1 automatic snapshot/day; **plus a staleness check on resume** — if the process died mid-upload, the next foreground retries (dirty flag is still set) |
| Manual action | "Back up now" in Profile, through the existing `launchOp` concurrent-op guard |
| Naming | `backup-v3-<ISO8601-UTC, colons replaced>.json` |
| Retention | client-side prune (no server code exists): list bucket, parse timestamps from names, keep 7 daily + 8 weekly + 12 monthly, delete the rest. Pinned snapshots live under a `pinned/` prefix the prune never scans. "Pin before risky operation" action; every future DB schema migration must be preceded by a pinned snapshot |
| Flag | `SNAPSHOT_BACKUP_ENABLED = false` in `core/backup/`, mirroring the `SyncKillSwitch` pattern (const + KDoc + grep-able). `bootstrapAppGraph` starts `BackupOrchestrator` behind it, the same pattern that gates `SyncOrchestrator.start()` in `AppGraph` today |

**Verification**: gate green + a test proving an upload whose hash mismatches is NOT marked
successful.

### Phase 3 — health visibility

- Persist `lastSuccessfulBackupAt`; show "Last backup: X ago" in Profile.
- Warning state when staleness exceeds 3 days with pending mutations.
- Every failure (serialization, network, hash mismatch, storage error) logs a distinct reason
  and increments a visible failure indicator.
- `ProfileOp` gains `BackingUp` / `VerifyingBackup`; the concurrent-op guard already emits
  `OperationInProgress`. Automatic backups do NOT go through `ProfileViewModel` —
  `BackupOrchestrator` is a singleton in the style of `SyncOrchestrator`. Register every new
  binding in `AppGraphKoinTest`.
- **Destination-change disclosure (ADR 009 Decision 5).** Before the first upload to an account
  this device has not backed up to before, say on screen that this device's whole ledger — including
  rows written under a previous account, since sign-out wipes nothing — goes into that account's
  backup. A one-time disclosure, not a confirm-or-cancel dialog: nothing is downloaded, merged or
  destroyed locally. Persist "already disclosed for this destination" so it fires once per account,
  and pin it with a test — an undisclosed first upload is the failure this exists to prevent.
- **Close the `SyncMutex` timeout defect** recorded in `docs/PROGRESS.md` before the pipeline
  ships. `BackupOrchestrator` becomes a second holder alongside `DeleteUserAccountUseCase`, so a
  stuck upload would block account deletion indefinitely — the same shape as the defect the engine
  had, under a new name.

### Phase 4 — restore confidence (the flag's gate)

1. **CI round-trip, no device needed**: host test with in-memory JDBC SQLite (the pattern
   `AppGraphKoinTest` already uses) — fixture across all 4 tables including tombstoned rows →
   export → wipe → import → row-level equality.
2. **In-app "Verify backup"**: download latest snapshot, check hash, parse, compare per-table
   counts against local — **without applying it**. Result in UI.
3. **Documented drill** added to the release checklist: after every DB schema bump, restore the
   latest production snapshot on a clean emulator and compare.

Only when all three pass does `SNAPSHOT_BACKUP_ENABLED` flip to `true`.

### Phase 5 — decommission the engine

Verified inventory (against trunk, 2026-08-13). Four buckets — delete, move, rewrite, keep:

| Action | What |
|---|---|
| Delete, `:data` / `:domain` | the 9 files in `data/sync/`; `ConflictResolver`, `SyncCursorStore`, `SyncDataUseCase`, `SyncRepository`; the claim machinery (`ClaimLocalDataOnAuthenticationUseCase` + its un-gated observer in `bootstrapAppGraph` — local `userId` is dead metadata without a push) |
| Delete, `:presentation` | `SyncOrchestrator`, `SyncController`, `SyncStatus`, `SyncEvent`, `DefaultSyncCursorStore`; the cursor keys in `AppPreferences`; the sync surface of `ProfileViewModel` (it consumes `SyncStatus`/`SyncController` for the Perfil badge) |
| Delete, `:ui-android` | `hh/shared/SyncEventsHandler.kt` — whose own KDoc calls itself *"the ONLY collector of `SyncController.events`, and that is load-bearing"* — plus the sync references in `AppNavHost` and `AppNavigator`. **Miss these and the Android build breaks**, since the `:presentation` deletions above remove what they collect |
| Move | the `appScope` single (`named("appScope")`) out of `syncModule` into CoreModule — it drives the claim observer today and `BackupOrchestrator` tomorrow; it is not engine property |
| Rewrite | `DeleteUserAccountUseCase` — its "unclaim" and "cursor clear" steps are engine parts; remote delete becomes: delete the user's Storage objects + the auth user. Also carries the sign-out fix Phase 0 gave `DefaultAuthRepository.signOut()`: `DefaultAuthRepository.deleteAccount()` still calls bare `client.auth.signOut(SignOutScope.LOCAL)` after the `delete_account` RPC, with the identical defect — a network failure mid-request leaves the device holding a session for a user that no longer exists server-side. Its KDoc used to claim the call "clears the on-device session"; Phase 0 corrected that, **by inspection of `AuthImpl.signOut` — not by test.** `DefaultAuthRepositorySignOutTest` exercises `signOut()` only; nothing runs `deleteAccount()`'s call shape, so this defect is unpinned and a fix here has no red test to turn green. Not fixed in Phase 0: `deleteAccount()` is its own contract question (the account is gone either way, so "qualified success" may not even be the right shape there) |
| Keep | `SyncMutex` and `SyncLogger`, renamed to drop the "Sync" prefix. After this phase the mutex's only holders are the rewritten `DeleteUserAccountUseCase` and `BackupOrchestrator`; the logger is what constraint 4 rests on, and its platform impls (`CrashReportingSyncLogger`, `PrintlnSyncLogger`) stay with it. Also `resumeEvents()`, and `importFromJson` + the export code — they are the product now |

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
  against the server. It is the **only** surviving pointer to either archived path, and that is
  checkable rather than asserted: `rg 'docs/sync/(AUDIT|PLAN)\.md'` returns exactly three lines —
  that SQL comment, the doc-hygiene note above recording the move, and this bullet quoting the
  comment. The other two describe the old paths; only the SQL one still asks a reader to follow them.

## Settled — do not reopen

Shared accounts (rejected by the product definition, `PRODUCT_REQUIREMENTS.md` W-04, "Multi-cuenta
compartida (parejas, equipos)"). Multi-device, which is a different rejection — ADR 006, one device
at a time, and W-04 does not cover it. CRDTs,
server-side LWW/conditional upsert, push-only row sync. Fixing the loop and keeping the engine.
E2E encryption now (deferred by ADR — a Keystore-bound key dies with the phone). Any DB schema
migration.
