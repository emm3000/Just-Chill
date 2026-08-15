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

② pins both, one test each — they are opposite failures and no single test covers them. Both run the
exported bytes back through the real `pendingPeriods` rule instead of comparing a field to itself,
so they fail on a value that still deserializes but no longer produces the right pending list.
**They prove export, not restore** — the debt that leaves is ③'s, below.

`frequency` has a single enum member today, its column carries a DEFAULT, and `update:` never writes
it — it is in the list because the file is a format, not a snapshot of today's cardinality.

**Settled in ② — export reads through a new `allLive()`. REVERSED by Phase 2a; do not re-apply.**
`RecurringMovementRepository` gained `allLive()`, backed by a flat `selectAllLive:`
(`WHERE deletedAt IS NULL ORDER BY name`) — the same `all:` convention the other three tables
already follow.

The `selectAllLive:` statement **stays**; it is what the export still calls. What is gone is
`allLive()` — the domain-interface method, its `:data` implementation, its `LocalDataSource` method
and its test fake, all deleted in `fae2e147` once Phase 2a left it with zero callers.

What decided it in ②, and why that reasoning expired: `exportToJson` read the other three tables
through domain repository interfaces, and that symmetry was what let `DefaultBackupRepositoryTest`
mock every read — so a direct `db` query for this one table would have left the export test mockable
for three tables and not the fourth. **Phase 2a removed the premise.** The export cannot read through
repository interfaces at all any more: those return Flows, `transactionWithResult` is a synchronous
block, and a `Flow.first()` cannot run inside one. All four tables are now read the same way, by
direct `executeAsList()` on the frozen statements, and the test file mocks nothing — it runs against
a real in-memory `JdbcSqliteDriver`. The symmetry ② was protecting still holds; it just moved down a
layer.

Still true and still load-bearing: neither pre-existing read fit. `allActive()` filters `isActive = 1`
and would drop every paused template — data the user still owns. `allWithDetails()` **does** return
every live row, paused ones included (correcting this plan's earlier "none returns every live row"),
but it is a joined model carrying three extra columns; what was actually missing was a **flat**
all-live select, since every flat select that existed (`selectActive:`, `find:`, `selectPending:`)
filters something out. Adding a SELECT is not a schema change (constraint 1), and
`verifySqlDelightMigration` — on the gate — is the check that would say otherwise if it were.

**Extend the dangling-category scrub to recurring.** `exportToJson` nulls the categoryId of
transactions whose category is tombstoned (`DefaultBackupRepository`); `recurring_movements` carries
the same composite FK `(categoryId, type) → categories(categoryId, categoryType)`, identical to the
one on `transactions`, so without the same scrub the exported file fails its own import.

**③ The SQL is four statements, not one SELECT — LANDED.** ② landed the first, `selectAllLive:`. The
other three are the ones the sweep and restore paths consume on the other tables and that
`recurring_movements.sq` declared **none** of: `softDeleteAllLive:`, `insertOrIgnoreFromBackup:`,
`restoreFromBackup:`. That table already carries `clearCategoryOnTypeChange:` and full soft-delete
like the other three, but the insert-then-update restore idiom — chosen over `INSERT OR REPLACE`,
which physically deletes the old row and is therefore refused by `ON DELETE RESTRICT` while anything
still references it, with the reasoning in a comment block above those statements in `accounts.sq` —
has no counterpart on it at all. New `.sq` statements are not a migration (constraint 1).

**Version the import sweep — the data-loss trap. LANDED, including the two user-facing pieces below
(`ImportStats.recurring` and `ImportBackupDialog`), which landed as their own commit.** With
v3 the sweep includes recurring **only for v3+ payloads**; importing a v1/v2 file neither tombstones
nor restores local recurring rows, since those formats carry none and wiping them would destroy data
no backup can restore. Both sides are pinned: *importing v1/v2 neither tombstones nor restores
recurring* (`BackupV1CompatibilityTest`, `BackupV2CompatibilityTest`); *importing v3 replaces it,
including a v3 file whose array is empty* (`BackupV3CompatibilityTest` — that second test is the one
an emptiness gate cannot pass, and it is what makes the other two mean anything). Not "leaves it
untouched" — the category detach below runs on every version.

**The gate value exists.** `decodePayload` returns `DecodedBackup(declaredVersion, payload)`, so the
version the FILE declared reaches `importFromJson` unchanged. Gate on `declaredVersion`, never on
`payload.recurringMovements` being empty: every `toCurrent()` restamps `payload.schemaVersion` to the
current version, so a v1 file, a v2 file and a v3 file from a device with no templates are otherwise
indistinguishable. The three compatibility suites each pin their own file's `declaredVersion`.

**③ re-pointed ②'s two data-loss guards at the real restore — DONE.** ②'s `lastConfirmedPeriod` and
`createdAt` tests rebuilt the template from the exported DTO with a **test-local** reconstruction,
because the production restore mapper did not exist until ③. They proved those two fields survive
the *export* and nothing about a restore — the exact failure this phase exists to prevent, a restore
stamping `createdAt = now` and swallowing every owed past period, left both of them green. They now
live in `DefaultBackupRepositoryImportTest` as full round trips against a real database: export,
import, read the row back through the production mappers, hand the result to `pendingPeriods`. Both
were verified red against a restore mutated to stamp `now`. `restoreFromBackup` writes `createdAt`
for the same reason, unlike its three siblings, where `createdAt` is a storage stamp: on a re-import
the file's value has to win over the one already in the column.

**"The import leaves recurring untouched" is true of the sweep and false of the import.**
`restore(dto: CategoryDto, …)` already calls `clearCategoryOnTypeChange` on
`recurring_movementsQueries`, nulling `categoryId` on every recurring row whose `type` disagrees
with the restored category's `categoryType`. Its KDoc explains why: SQLite otherwise refuses the
parent-key change, and inside the one transaction wrapping the restore that is not one failed row —
it is the whole import rolled back after the tombstone sweep. Only the narrow claim holds: the sweep
neither wipes nor restores that table. Today.

**Settled — statement order inside the transaction.** Under a v3 sweep the four tombstones move
first (the recurring one gated on `declaredVersion >= 3`, the other three as today), then accounts,
then categories — the detach still runs there, on every version, unchanged — then transactions, and
the recurring restore lands **last**, gated on `declaredVersion >= 3` the same as its tombstone:

```
softDeleteAllLive x4        (recurring only when declaredVersion >= 3)
payload.accounts            restore
payload.categories          restore   <- the detach runs here, on EVERY version, before the category is written
payload.transactions        restore
payload.recurringMovements  restore   <- LAST, v3+ only
```

Both alternatives lose data, and the same way: a recurring row loses its `categoryId`, silently — it
is only the WHICH rows that differs. Detaching after the recurring restore runs
`clearCategoryOnTypeChange` over rows the file just wrote in this same transaction, stripping every
row whose restored category disagrees with it. Restoring recurring before the categories loop looks
different but lands in the same place, once `usableCategoryId` — settled below as the shared guard —
sits in the call: it reads `typeOf(categoryId)` off whatever the categories table holds **at the
point it runs**, and under this ordering that is still the pre-restore world, since
`softDeleteAllLive` tombstones a row without rewriting its `categoryType`. A recurring row whose
file-declared type matches the category the file is about to write, but not the one still sitting in
the table, reads a mismatch and comes back null — not a thrown constraint, `usableCategoryId` never
throws, it resolves. So the row still lands, still gets counted, just uncategorized: every template
whose category changed type in this file loses that category with nothing on screen to say so.
`usableCategoryId`'s own KDoc names the throw-and-roll-back failure this function exists to prevent
(`DefaultBackupRepository.kt:340-349`) — that risk is real in general, it is simply not what THIS
ordering produces, because the guard the plan already commits to catches it first and turns it into
a quieter loss instead. Last position is the one place `usableCategoryId` sees the file's own
categories rather than a world the import is mid-replacing, which is the actual reason it goes last:
not that the earlier positions crash, but that they are the only positions where the pairs the file
carries are NOT the pairs that survive.

The detach does not become dead weight once the sweep tombstones recurring rows first — this is the
non-obvious part. A tombstone is not a delete: `clearCategoryOnTypeChange` carries no
`deletedAt IS NULL` filter, so a recurring row the file does not mention, freshly tombstoned by the
sweep, still holds its old `(categoryId, type)` pair and still makes SQLite refuse the category's
type change. The detach stays, unversioned, on every import, sweep or no sweep.

**`usableCategoryId` is reused, not duplicated.** It already takes `(categoryId, type)` and reads
`db.categoriesQueries.typeOf(categoryId)` — nothing about it is transaction-specific, and
`recurring_movements` carries the identical composite FK. Its name is already table-neutral, so the
recurring restore calls it as-is. The order above is what makes that reuse correct: by the time it
runs over a recurring row, every category in the file has already been written, so `typeOf` reads
the file's world, not the categories table's old one — the same argument the function's own KDoc
already makes for transactions ("the file is the whole world … there is no later arrival to wait
for").

That KDoc needs widening when the reuse lands, though, and not just moved: it justifies the fallback
by LEGACY provenance today — *"Every backup file that exists today predates the composite key"* —
and that does not hold for recurring. v3 was born after the composite key existed, and Phase 1's
dangling-category scrub already keeps a tombstoned category's id out of the export, so a file this
app writes can never carry a broken recurring pair. The only remaining provenance is a hand-edited
file. The fallback still stands — the file is untrusted, and a rollback after the sweep already ran
is the catastrophic case either way — but the KDoc has to name both provenances once it serves both
callers, or it argues something false for half of them.

**Settled by the commit that wrote the mapper — the validating drop, and it splits in two.** The
question was whether the recurring restore needs the equivalent of `restore(dto: TransactionDto)`'s
`toEntityOrNull()`. The answer is yes for the enums and no for the period key, and the asymmetry is
the whole of it:

- **Unknown `type` or `frequency` → drop the row.** Not for the transaction's reason: a template
  cannot skew a total, since the recurring screen and `GetRecurringMonthlyTotals` fold the same list
  and `asExternalModelOrNull` drops it from both. It is dropped because invisible is not inert — the
  row still counts in `countLiveByAccount`, which is what tells the owner an account cannot be
  deleted because it still has recurring movements, on a screen showing none. And there is no safe
  default: coercing Income to Spend corrupts the ledger the template mints into.
- **Unparseable `lastConfirmedPeriod` → null, keep the row.** The hazard is real —
  `ensureNotSettled` orders that column as a plain string, so `"julio"` sorts above every real key
  and refuses every confirmation while `pendingPeriods` parses it, gets null, and keeps listing
  those months as owed: months the user is shown and cannot confirm. But null is a state the column
  already means ("never settled"), `SkipRecurringMovementUseCase` puts the mark back in one tap per
  month without writing money the user did not enter, and dropping instead would throw away the
  name, the amount, the account and the day the file carried correctly. The value is also
  **re-encoded** through `parsePeriodKey` + `periodKey` rather than copied through — the same policy
  `occurredAt` follows — which repairs the near-miss the parser accepts and the comparison does not:
  `"2026-7"` reads as July and sorts above `"2026-08"`.

**The gate is `>= BACKUP_RECURRING_SINCE_VERSION`, a frozen literal, not `>= BACKUP_SCHEMA_VERSION`.**
The two are equal today and the equality is a coincidence of timing: the moment a version 4 exists, a
gate written against the current version stops restoring the templates in every v3 file ever written,
silently, since the import would read the file, skip the table and report success. Same reasoning as
`BACKUP_SCHEMA_VERSION_V2`.

**Two stale KDocs — CORRECTED, ahead of the behavior change.** In
`DefaultBackupRepositoryImportTest`, the KDoc above the test for a backup that redefines a
category's type asserted *"Import never touches `recurring_movements`"* — while the test directly
beneath it asserts that exact mutation. `restore(dto: CategoryDto, …)`'s own KDoc said the same
("the import deliberately never touches that table"). Both now state the narrow claim instead — the
import neither tombstones nor restores that table — and both name the detach as the one thing it
does do. `DefaultBackupRepository`'s in-transaction comment and `docs/PROGRESS.md`'s 2.4.0
measurement carried the same wording and were corrected with them. ③ then inverted the narrow claim
for v3 and kept it for v1 and v2, in every one of those places. What survived the inversion unchanged
is the detach: a tombstone is not a delete, so a swept row still holds its old `(categoryId, type)`
pair and still makes SQLite refuse the category's type change.

**`ImportBackupDialog` (`:ui-android`) was user-facing and stale — FIXED.** Both its `isSignedIn`
body-copy branches enumerated the tables in Spanish — *"Tus movimientos, categorías y cuentas quedan
tal cual el archivo"* — and its KDoc made the same list in English ("replaces every movement,
category and account"). Both went incomplete the moment v3 started sweeping recurring, on the one
screen where the user consents to a destructive operation. Both now name the table: the two Spanish
body-copy branches say "recurrentes", the KDoc says "recurring movement" in English, as the
repo-wide convention requires. They name it for every file — including a v1/v2 one, since the
dialog runs before the file is decoded and cannot tell it apart from a v3 one; that deliberate
over-warning is argued in the dialog's own KDoc.
Correctness, not copy polish, and in this phase's scope. The body copy stays Spanish; it is UI.

**`ImportStats` — LANDED.** Gained a `recurring` count (`accounts` / `categories` / `transactions`
before it), counted the same way `transactions` already was — what LANDED, not what the file held,
so a template dropped for an unknown `type`/`frequency` is not counted either — plus the UI message
that reports it (`ProfileMessage.ImportDone`, `buildImportDoneMessage`).

**Downgrade behavior, and it is operational rather than cosmetic.** `decodePayload` reads the
declared version **before** it deserializes anything — its KDoc: the version is read first, "and
only then is the payload decoded as the shape that version actually had" — so `ignoreUnknownKeys`
never gets the chance to drop an unknown array. A build still shipping `BACKUP_SCHEMA_VERSION = 2`
has branches for 2 and 1 only, so a v3 file falls to `else` and is refused as
`ValidationCode.BackupVersionUnsupported`. That refusal is loud and correct, and far better than a
silent partial read. The consequence to plan around: **from the first build that ships ②, every file
this app exports is unreadable on any older build still installed.** On a device that runs the
release build daily off App Distribution, a rollback or a sideloaded older APK cannot read the
backups the current build is writing.

### Phase 2 — snapshot pipeline

Three sub-phases, three PR series: the transactional refactor is its own risk and must be
reviewable alone.

**2a — transactional export.** After Phase 1, `exportToJson` does four independent `Flow.first()`
reads — accounts, categories, transactions, and the `allLive()` recurring read ② added. A write
landing between any two of them corrupts the snapshot. SQLDelight transactions are synchronous
blocks, so `.first()` cannot run inside one: switch the export to direct queries
(`executeAsList()`) inside a single `transactionWithResult`. `DefaultBackupRepository` already
holds the `db`; the refactor is contained to that class.

**2b — storage. SPLIT IN TWO when it was built**, for the same reason Phase 2 itself is three PR
series: as written below it bundles a dependency, a server-side migration and the integrity
primitive, and those three fail in different ways. Landing the primitive alone and first means a
hash mismatch and a network failure are never being debugged at the same time.

**2b-i — the integrity primitive. LANDED** (`5d43cc90`, `7dc783f0`, `d0b9a871`, `8e343996`). No
network, no server. SHA-256 of the payload + the sidecar manifest (filename, hash, format version,
per-table row counts).

Three things it settled that this plan had left open or got wrong:

- **okio is declared, not inherited.** The plan said "use okio's `ByteString.sha256()` if okio is
  already transitive". It is — 3.17.0, via `auth-kt` → `supabase-kt` — but arriving only as a side
  effect of someone else's dependency graph is exactly the hazard this plan flags one paragraph
  down for supabase-kt's 10-second `requestTimeout`. It now has its own catalog entry. Verified in a
  scratch worktree: the pin does not move resolution, because Gradle's highest-wins had already
  selected 3.17.0.
- **No `expect/actual` was needed.** The plan's fallback (`java.security.MessageDigest` /
  CommonCrypto) is moot — okio's `sha256()` compiles from commonMain for both targets.
- **`sha256Hex` takes bytes, never a String.** The upload sends UTF-8 bytes and the read-back must
  hash the identical sequence; an API taking a String and encoding internally invites a later caller
  to hash something one encoding step away from what actually travelled.

The manifest carries **two** version fields that must never be conflated: `manifestVersion` (its own
format, born at 1) and `payloadSchemaVersion` (the snapshot's `schemaVersion`, 3 today). Neither
carries a default — a missing key must not be indistinguishable from a value, which is the lesson
Phase 1 paid for. Every unreadable-payload path names a distinct reason (hard constraint 4).

**2b-ii — storage. SPLIT IN TWO as well**, along the line the client/server boundary already draws:
the infrastructure can land and be probed with no pipeline code, and a bucket that refuses the wrong
thing is worth having before anything writes to it.

**2b-ii part A — the bucket and the client plugin. LANDED** (`18ac5838`, `30098d14`, `04cae52c`,
`74a0bdea`).
`storage-kt` in the catalog (the BOM shipped only auth-kt + postgrest-kt; the repo had zero Storage
usage), `install(Storage)` in `SupabaseModule` with `requireValidSession = true` beside Postgrest's,
and `supabase/migrations/20260814200043_backup_storage_bucket.sql`: a private `backups` bucket capped
at 10 MiB and constrained to `application/json`, plus select/insert/delete policies `to authenticated`
keyed on `(storage.foldername(name))[1] = (select auth.uid())::text`, modelled on the row-table
policies named above. No update grant — every snapshot name carries its own timestamp, so no key is
ever rewritten. The predicate was probed against a live local stack with two real authenticated
users and could not be crossed.

Five things it settled that this plan had left open or got wrong:

- **`storage.protect_delete()` refuses `delete from storage.objects` outright**, whatever the policy
  says. So Phase 2c's retention prune MUST go through the Storage API; the delete policy is what
  authorises the prune there, not what performs it. A policy-only reading of "delete works" is wrong.
- **The mime constraint is an exact string match, parameters included.** storage-api compares the
  Content-Type header verbatim against `allowed_mime_types`, so `application/json; charset=utf-8` is
  refused with HTTP 415 `invalid_mime_type` — a hard failure that looks like a server fault. Part B
  sends a **bare** `application/json`, which is also storage-kt's own default for a `.json` key
  (`ContentType.defaultForFilePath`), so the trap springs only on an explicit `withCharset`.
- **Resumable upload is unavailable, not merely unused.** `storage.s3_multipart_uploads` and
  `storage.s3_multipart_uploads_parts` have RLS enabled with zero policies, so `uploadAsFlow` fails
  for `authenticated` with an error naming none of that. Harmless under a 10 MiB ceiling — one-shot
  upload is the right call anyway — but it is not a choice part B gets to make.
- **`on conflict do nothing` is not a migration.** The bucket insert shipped with it, and it was
  proven non-convergent: with the local bucket set to `public = true` and both limits null, re-running
  the migration reported success and corrected nothing. On a project where `backups` already exists —
  a hand-made Dashboard bucket, which sets neither a size nor a mime limit — production would have got
  an unbounded bucket accepting any content type, and a later migration raising the limit would have
  been a no-op forever. It is `do update` on the three settings columns now, so this file is the single
  declaration of them; proven convergent by breaking all three by hand and re-running.
- **Nothing in CI protects the RLS predicate.** `qualityGate` cannot see SQL policies and the repo has
  no Supabase test infrastructure; the only proof this bucket is owner-scoped is a manual probe against
  a local stack, which no green build repeats. Deferred to Phase 4, where the CI round-trip work
  already lives — not given a home of its own here.

**2b-ii part B — upload and read-back. LANDED** (`e0aef49a`, `192e6032`, `8335cf29`, `08267bdd`,
`56be26e0`, `4b56c131`, `66f499bf`, `4359e11c`). The payload and its sidecar manifest go under the `<uid>/` prefix the
policies expect, the payload is read back and verified before the manifest is written, and the
manifest is read back too. `BackupUploader` is the port in `:domain`; `DefaultBackupUploader` holds
the whole decision; `BackupObjectStore` is the four-operation seam that makes a mismatch provable on
a host suite with no network, implemented by `SupabaseBackupObjectStore`.

Seven things it settled that this plan had left open or got wrong:

- **The order carries the meaning, not just the bytes.** Payload → read-back → manifest, so *a
  manifest present means the payload beside it was verified*. Uploading both together leaves the
  same objects on the server and destroys that claim. The invariant holds because the bucket grants
  no `update` and uploads use `upsert = false`, so no key is ever rewritten — it is a property of the
  migration as much as of this class.
- **The manifest is verified too**, by plain byte equality against the array that was uploaded. A
  200 is exactly the claim this unit exists to refuse, and the failure it prevents is a false alarm
  on a GOOD snapshot: a manifest corrupted in transit states a digest the intact payload will never
  match, so a later pre-restore check condemns bytes that would have restored perfectly. On a
  manifest mismatch both objects are deleted — no valid receipt means nothing worth keeping.
- **A read-back that FAILS does not delete.** Whatever stopped the read would very likely stop the
  delete, and a vague "could not clean up" would replace a precise "could not read it back". The
  orphan that leaves is what the Retention row above is now written around.
- **The sidecar's name APPENDS the extension** — `<name>.json.manifest.json`, which looks clumsy and
  is the point: the bucket's mime check is a verbatim string match and storage-kt derives the header
  from the key, so a name that stops ending in `.json` is an HTTP 415 that reads like a server fault.
  Appending cannot produce one whatever the caller passed; swapping an extension can. It also gives
  a listing a shape it can read without downloading anything, which is what the prune runs on.
- **Session resolution needed its own bound, and the deferral above did not cover it.** The
  `requestTimeout` / `transferTimeout` paragraphs are about HTTP knobs; waiting for a persisted
  session to load is not an HTTP call. Unbounded, it would hold 2c's `launchOp` concurrent-op guard
  with no exception and no message — hard constraint 4's exact shape. It is 10s, copied from
  `DefaultSyncRepository`'s identical constant rather than imported from it, since that file is
  deleted in Phase 5.
- **A hash mismatch is NOT `BackupFileInvalid`.** That code renders "El archivo está dañado o no es
  un respaldo de JustChill", written for a user who picked a bad file to *restore*. A failed upload
  is the opposite event with the opposite actor, and it gets `BackupUploadUnverified`.
- **A test that pins a real `SupabaseClient` has to settle the Auth plugin before importing a
  session**, and a single green gate run does not prove it does. `SupabaseBackupObjectStoreTest`
  imported into a plugin that had not finished initializing, and once, on a cold `--rerun-tasks`
  gate, that produced `Unauthorized` from the prefix test. The observed red is an **observation, not
  a confirmed diagnosis**: it never reproduced since, across 15 more cold gate runs, 15 runs of the
  class under 9 busy-loop CPU hogs, and a scheduler probe — all green. The machine was under heavy
  load when it fired (Android Studio at ~67% CPU, load average ~49), which is the load source that
  matters: the gate itself is sequential (`org.gradle.parallel` is commented out in
  `gradle.properties`, no `--parallel` / `maxParallelForks` / `forkEvery` anywhere in the build), so a
  real race here would be settled by JVM thread scheduling, not by Gradle task concurrency. What the
  settle demonstrably does is remove an ordering ambiguity between `Auth.init` and `importSession`;
  calling that "removes the race" claims a diagnosis nobody has reproduced.

  Two sub-mechanisms both fit the one `Unauthorized` observed — which can only mean
  `awaitInitialization()` returned *and* `currentUserOrNull()` was null — and the settle closes both:
  **(a) clobber**, `importSession` writes `Authenticated` and then a queued `Auth.init` continuation
  writes `NotAuthenticated` over it; **(b) pending write**, `importSession` is `suspend`, and if its
  status write is still queued when the helper returns, `ownedPrefix()` reads `NotAuthenticated` and
  throws. The existing scheduler probe cannot distinguish them: `advanceUntilIdle()` drains exactly
  the queue (b) depends on, so a probe that drains before observing will see the imported user
  survive whether or not (b) is real — its result is what (b) predicts, so it is not evidence against
  the diagnosis.

  The observable that would distinguish them: read `client.auth.sessionStatus.value` **immediately
  after `importSession` returns, with nothing drained in between**, then again after a
  real-dispatcher settle. `Authenticated` first, later flipping to `NotAuthenticated` ⇒ (a).
  `NotAuthenticated` / `Initializing` first, becoming `Authenticated` only after draining ⇒ (b).
  `Authenticated` first and stable under load ⇒ neither, and the real cause is still unknown.
  Sharper: loop the *unsettled* helper 500+ times under load asserting `currentUserOrNull() != null`
  immediately after it returns — if that never reds, the helper is not the cause. `runTest` never
  drives the standalone `UnconfinedTestDispatcher` these tests pin to `Dispatchers.Main`, so any
  settle has to happen on a real dispatcher. Twelve, not eleven, is also the pipeline's failure
  count: the `.json` refusal was never added to the total the KDocs quote.

**What "verify" means, precisely, because the obvious reading is wrong.** It is
`sha256Hex(readBackBytes) == manifest.payloadSha256`, raw bytes on both sides. It is **not** a
comparison of the manifest's `rowCounts`: those are derived from the same bytes as the digest, so
if the hash matches the counts match by construction, and if it does not they are meaningless. The
counts earn their place at listing time and in a pre-restore "this file holds N movements" display —
not in the integrity check.

**The request timeout is 10s**, and it is not configured here. supabase-kt 3.7.0's
`KtorSupabaseHttpClient.applyDefaultConfiguration` installs `HttpTimeout` on every client it
builds, custom engine included, reading `SupabaseClientBuilder.requestTimeout`, whose default is
`10.seconds`. `SupabaseModule` overrides nothing, so that default is what runs. Size upload
retries and any outer timeout above it. **Caveat**: read once out of the Gradle cache on
2026-08-13 and nothing enforces it — a BOM bump can change it silently. Installing `HttpTimeout`
explicitly in `SupabaseModule` would make it visible, but it also moves the sync push path, so it
belongs to its own unit.

**…and it does not govern the upload.** `Storage.Config.transferTimeout` is a separate knob with its
own default of **120 seconds**, and it — not `requestTimeout` — is what bounds every call `StorageImpl`
makes, not only an upload or a download: it sets `HttpTimeoutConfig.requestTimeoutMillis` on the
Storage API client wholesale, so list and delete inherit the same 120s ceiling. `SupabaseModule`
configures neither. So the paragraph above sizes retries for ordinary Postgrest calls only, and part B
must not build chunking or progress reporting against a 10-second limit that is not there — and Phase
2c's retention prune, a list-then-delete loop, runs under the 120s bound rather than the 10s
`requestTimeout`.

**2c — orchestration.**

| Concern | Design |
|---|---|
| Dirty flag | `max(updatedAt)` across the 4 tables (new SELECTs; `softDelete` already bumps `updatedAt`, so deletes mark dirty) vs `lastSuccessfulBackupAt` in `AppPreferences` — the facade already holds cursor/lastSyncedAt keys |
| Trigger | new `backgroundEvents()` expect/actual, sibling of `resumeEvents()` (ProcessLifecycleOwner ON_STOP / UIApplicationDidEnterBackground), capped at 1 automatic snapshot/day; **plus a staleness check on resume** — if the process died mid-upload, the next foreground retries (dirty flag is still set) |
| Manual action | "Back up now" in Profile, through the existing `launchOp` concurrent-op guard |
| Naming | `backup-v3-<ISO8601-UTC, colons replaced>.json` |
| Retention | client-side prune (no server code exists): list the bucket and **key on the presence of `<name>.manifest.json`, never on `<name>` alone**. A payload without its sidecar is not a snapshot: it counts toward no retention bucket and is **deleted on sight** — see the paragraph below, which is the rule, not a note about it. Parse timestamps from the names that DO carry a manifest, keep 7 daily + 8 weekly + 12 monthly, delete the rest. Pinned snapshots live under a `pinned/` prefix the prune never scans. "Pin before risky operation" action; every future DB schema migration must be preceded by a pinned snapshot |
| Flag | `SNAPSHOT_BACKUP_ENABLED = false` in `core/backup/`, mirroring the `SyncKillSwitch` pattern (const + KDoc + grep-able). `bootstrapAppGraph` starts `BackupOrchestrator` behind it, the same pattern that gates `SyncOrchestrator.start()` in `AppGraph` today |

**Why the prune keys on the manifest — a name-only prune loses verified data.** 2b-ii part B
uploads the payload, reads it back, and writes the sidecar **only** after the bytes matched, so
*the presence of `<name>.manifest.json` is the statement that the payload beside it was verified*.
A prune that lists names and keeps "7 daily + 8 weekly + 12 monthly" without looking for the sidecar
gives an orphan a retention slot, and the snapshot it evicts to make room is one that was verified.
That is not untidiness, it is data loss, and it is invisible: both objects are just names in a
listing. The rule is stated here rather than in `DefaultBackupUploader`'s KDoc because this file is
what 2c is built from, and nobody implementing a prune has a reason to open a `:data` class.

**Deleting an orphan on sight does throw away good bytes sometimes, and that is still the right
call.** There are **five** ways a payload ends up without a sidecar — two of them are handled
identically and share one entry below — and nothing on the wire tells them apart:

1. **the read-back failed** — the payload is *unverified*, and deliberately not deleted at the time
   (a delete over the same dead transport would replace a precise reason with a vague one);
2. **the manifest upload failed, or a manifest mismatch's cleanup deleted the manifest but not the
   payload** — the payload is *verified*; this is the shape where deletion-on-sight discards an
   intact snapshot. Two different causes land here: either the manifest never made it up at all, or
   `delete(manifestKey)` succeeded while `delete(payloadKey)` failed in the manifest-mismatch cleanup
   (`DefaultBackupUploader.kt:136-141`) — the manifest is gone either way, so the end state and the
   handling are the same;
3. **a payload mismatch whose cleanup delete failed** — the payload is *known bad*, and this is the
   orphan the rule is unambiguously right about: the bytes are proven not to match their own digest
   and the uploader already tried to remove them;
4. **the process died mid-upload** — the case the Trigger row above already plans for, where the
   dirty flag is still set and the next foreground retries.

Because 2 cannot be distinguished from 1, the choice is between *occasionally discarding a good
snapshot* and *keeping a possibly-unverified one indefinitely* — and the second is the failure this
whole ADR exists to end: a restore from bytes nothing ever vouched for. The cost of the first is one
skipped backup cycle, on a device that backs up daily and whose dirty flag is still set. The cost of
the second is discovering, at restore time, that the only copy is corrupt. Delete on sight.

**Two rarer leftovers are not orphans at all, and the prune must not be changed to chase them.** They
are a *complete-looking pair* — payload plus sidecar — that will fail a pre-restore check:

- **the manifest read-back failed**: the payload is verified, the sidecar is of unknown state, and
  the uploader deliberately deletes neither, for the same dead-transport reason as orphan 1.
  `a manifest that cannot be read back leaves both objects alone` pins exactly that.
- **a manifest mismatch whose manifest-delete failed**: the payload is verified and the sidecar is
  known to state the wrong digest, so the pair looks complete and can never verify.

The prune keeps both, correctly — it keys on the sidecar and both have one. This is the residue the
read-back verification cannot eliminate, and it lands on the reader instead: **a pre-restore check
must fall back to the newest snapshot that VERIFIES, rather than declaring the newest one broken.**
A check that only ever looks at the latest pair turns one bad receipt into "you have no backup" on a
device holding a shelf of good snapshots. That requirement is repeated in Phase 4, which is where it
gets built.

**2c-i — the dirty-flag watermark and the persisted timestamp. LANDED** (`646e2666`, `1da00cfa`).
Both halves of the Dirty flag row above, with nothing consuming either yet — 2c-iii is what
compares them and decides whether to trigger a backup.

- `BackupRepository.latestLocalChangeAt(): Long?`, backed by `backup.sq`'s `latestLocalChange`:
  one `UNION ALL` of `max(updatedAt)` across the four backed-up tables, reduced by one outer `max`.
  **One statement, not four reads**, for the same reason Phase 2a moved the export into
  `transactionWithResult`: four independent reads could interleave with a write landing between
  any two of them and produce a watermark that is not any state the database was ever actually in.
- **It counts soft-deleted rows on purpose** — the opposite of the export, which filters
  `deletedAt IS NULL`. `softDelete` bumps `updatedAt` the same as any other write (verified: all
  four `.sq` files), so a deletion is a local change worth backing up; excluding tombstones would
  let a device whose only change since the last backup was a deletion stay marked clean forever.
- **`null` (all four tables empty) is distinguishable from `0`, and that is structural, not
  conventional**: SQLDelight generates `Long?` for the column, so an empty database and a database
  whose oldest row has `updatedAt = 0` cannot be confused by construction.
- `AppPreferences.lastSuccessfulBackupAt(userId)` / `setLastSuccessfulBackupAt` /
  `clearBackupMetadata`, per-user with the same `-1L` "never" sentinel as `lastSyncedAt`.
  `clearBackupMetadata` is wired into `DefaultSyncCursorStore.clear` alongside
  `clearSyncMetadata`, riding `DeleteUserAccountUseCase`'s account-deletion call — **not** because
  the watermark is sync metadata (it explicitly is not), but because that `clear` is the only
  account-deletion seam that exists today, under `NonCancellable`, and inventing a dedicated port
  for a key nothing reads yet would be YAGNI. **2c-iii is the unit that must introduce the proper
  backup-metadata seam and move both the write and the clear onto it** — it is where the writer of
  this timestamp arrives, so it is the natural owner of the seam too.
- `DefaultBackupRepository` was already at detekt's `TooManyFunctions` ceiling of 11, so adding the
  override forced extracting the existing private `usableCategoryId` to a file-private top-level
  function, matching `snapshot`'s existing extension-on-`EmmDatabaseData` shape. Future work on that
  class pays this tax again — worth knowing before a new method trips a lint failure that looks
  unrelated to the change that caused it.

**Verification**: gate green + a test proving an upload whose hash mismatches is NOT marked
successful.

### Phase 3 — health visibility

- Show "Last backup: X ago" in Profile, reading `AppPreferences.lastSuccessfulBackupAt`. The storage
  itself — `lastSuccessfulBackupAt` / `setLastSuccessfulBackupAt` / `clearBackupMetadata` — already
  landed in 2c-i (see that section); nothing calls `setLastSuccessfulBackupAt` yet, since there is no
  backup pipeline to call it after. Still open here: wiring the write after a verified upload, the
  Profile read, and — per 2c-i's note — routing both the write and the account-deletion clear
  through the proper backup-metadata seam 2c-iii introduces, instead of a naked `AppPreferences` call.
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
   counts against local — **without applying it**. Result in UI. **It must walk back to the newest
   snapshot that verifies rather than reporting the newest one broken**: 2b-ii part B can leave a
   complete-looking pair whose manifest is wrong or of unknown state — the note under Retention
   enumerates the two ways — and a check that only looks at the latest pair turns one bad receipt
   into "you have no backup" on a device holding a shelf of good snapshots. Which one was actually
   verified is what the UI must name, not just "OK".
3. **Documented drill** added to the release checklist: after every DB schema bump, restore the
   latest production snapshot on a clean emulator and compare.
4. **An automated RLS probe for the `backups` bucket**: two authenticated users, each proving it
   reaches its own prefix and cannot list, read or delete the other's. Today that proof exists only
   as a manual probe run once by hand against a local stack (2b-ii part A) — `qualityGate` cannot
   see a SQL policy and the repo has no Supabase test infrastructure. This is where it stops being
   a thing someone remembered to do.

Only when all four pass does `SNAPSHOT_BACKUP_ENABLED` flip to `true`.

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
