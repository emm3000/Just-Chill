# Sync — consolidated audit

Sync is **off in production** since 2026-08-12 (`253e170`). This doc is the reference for the
redesign: root cause, what the backup-only decision retires, what still has to be fixed, and what
must survive the rewrite. Status lives in `docs/PROGRESS.md`; `docs/sync/PLAN.md` is the old slice
plan, paused.

All of it is static reading of code, SQL and server logs. **Nothing was executed against a device**
except the one observation in §2. `file:line` refs were re-checked against trunk `253e170`.

## 1. The decision

Recorded as [ADR 006](../adr/006-sync-is-backup-only-one-device-at-a-time.md), which supersedes ADR
001's multi-device premise and leaves ADR 004 dormant.

**Sync is backup, not replication.** One device at a time; two devices never write concurrently. The
data belongs to the **device**, the account is only a backup destination — `userId` means "which
backup this row belongs to", not "who owns it".

Consequences: no two replicas can diverge, so **conflict resolution is dead weight** (§4) — and the
production loop is therefore a bug in machinery the product does not need, since `KeepLocal` →
`markPendingForResync` is the only thing that re-dirties a synced row and exists solely to resolve
conflicts. Two obligations follow: the app must **say on screen** that signing in with another email
re-points the backup and uploads this device's ledger there, and production must be **derived**,
never authoritative.

## 2. State now

| | |
|---|---|
| Kill switch | `SYNC_TEMPORARILY_DISABLED` — `presentation/.../core/sync/SyncKillSwitch.kt:16` |
| Gate 1 | `core/AppGraph.kt:67` — `SyncOrchestrator.start()` never called, so no trigger and no request consumer; that consumer is the only caller of the private `runSync()` |
| Gate 2 | `hh/profile/ProfileViewModel.kt:144` — manual path returns at its origin |
| Not gated | `ClaimLocalDataOnAuthenticationUseCase` (`AppGraph.kt:59-63`) — stamps ownership, no network, so flipping back needs no catch-up |
| Review | Judgment Day round 1: 0 SEVERE, 0 corrections, 1 SUGGESTION (`ProfileScreen.kt:226` suppression too wide). `qualityGate --rerun-tasks` green |

Nothing was removed; every binding, test and engine class is still wired. Profile reads
"Sincronización en pausa". **The only runtime observation of the bug:** the owner signed out and the
loop stopped — proving an authenticated-session trigger drives it, not which one. All else is static.

## 3. Root cause of the loop

Two predicates that must agree, and do not.

- **Counted** — `WHERE syncState = 'Pending' AND userId IS NOT NULL`. `transactions.sq:264-265`,
  `accounts.sq:90-91`, `categories.sq:90-91`, `recurring_movements.sq:98-99`.
- **Pushed** — that SQL **plus** `.filter { it.userId == userId }` in Kotlin.
  `TransactionTableSync.kt:44`, `AccountTableSync.kt:36`, `CategoryTableSync.kt:36`,
  `RecurringMovementTableSync.kt:38`.

A row whose `userId` is non-null but not the current user is counted forever and pushed never;
`markSynced` is reachable only from the push path (`BaseTableSync.kt:200-204`), so nothing can clear
it. The cycle closes: `localRevision` matches by PK with no userId filter
(`AccountTableSync.kt:82`) → resolver sees Pending + newer local `updatedAt`
(`ConflictResolver.kt:36-39`) → `KeepLocal` (`BaseTableSync.kt:314-315`) → `markPendingForResync`
(`accounts.sq:72-73`) → unconditional `notifyQueries` → `countPending` re-emits the same value, with
no `distinctUntilChanged` anywhere in the chain (`DefaultSyncRepository.kt:54-59` is a bare
`combine`) → `filter { > 0 }` → `debounce(3.seconds)` (`SyncOrchestrator.kt:198-199`) → `runSync()`
→ push skips those rows → repeat.

Every cycle **succeeds** and the `KeepLocal` path logs nothing — which is why it never reached
Crashlytics. **Second, independent path to the same loop:** `TransactionTableSync.kt:45-52`
`mapNotNull`-drops any pending transaction whose `occurredAt` does not parse. Needs no account
switch.

## 4. Retired by backup-only

Real findings against a replication protocol. Backup-only removes the protocol, so all three die at
once — no two replicas, no conflict, nothing to arbitrate. Kept, not deleted: if multi-device
returns, they return exactly as written.

| Finding | Evidence |
|---|---|
| **No real LWW.** `upsert` defaults to `ignoreDuplicates = false` → `Prefer: resolution=merge-duplicates` → `ON CONFLICT DO UPDATE SET <every column> = excluded.<column>`, no timestamp guard. Push runs before pull, so the resolver never protects the newer replica. The false premise is ADR 001 line 57, "push/pull ordering is irrelevant". | `DefaultSyncRepository.kt:69-70`, pinned intentional by `DefaultSyncRepositoryTest.kt:143-148` |
| **Delete loses to an older concurrent edit**; the tombstone is resurrected on the deleting device. | — |
| **LWW tie-break is the raw device clock.** | — |

Also retired: multi-device pull convergence was never tested — now moot.

## 5. Live findings

### Protocol

| Where | Defect | Consequence |
|---|---|---|
| `DefaultSyncRepository.kt:88-111` (order `:90-93`, hold `:99`), `SyncCursorUtils.kt:23-25` | One cursor for four sequentially-fetched tables, advanced to the global max with a flat 10s overlap | A row written mid-cycle can fall below the next window. If it is a parent, the child's FK miss → `Deferred` (`TransactionTableSync.kt:164`) → `skipped` (`BaseTableSync.kt:298-302`) → cursor held for **all four** → permanent silent total freeze. Only recovery is `SyncCursorStore.clear`, called only from account deletion. **Fix: four per-table cursors.** |
| `BaseTableSync.kt:194-205` | One `upsert(dtos)` with the whole pending set. No chunking, no backoff | On timeout the rows stay Pending even if the server committed, and the identical payload retries forever. **Unverified:** whether `HttpTimeout` is installed at all, or installed internally by supabase-kt 3.7.0 at 10s. `SupabaseModule.kt:25-44` overrides nothing either way. Resolve before sizing the fix. |
| `BaseTableSync.kt:221`, `:239` | `MAX_PULL_PAGES` sets `skipped`, and `skipped` holds the cursor | Livelock, not a guard: past 500×200 rows it re-applies the same first 100k forever |
| `TransactionTableSync.kt:129-133` | Out-of-range date returns `Dropped`, cursor advances past it | Silent per-row data loss, logged and nothing else |
| `FixedPeruOffset.kt:35`,`:37`,`:65-74`; `OccurredAtText.kt:18-28` | Round trip is **lossless** — same fixed UTC-5 both directions, second precision — confirming DATE_AUDIT #5. But the stored server value misrepresents the instant | Phase two's conversion must use **UTC-5, not `AT TIME ZONE 'UTC'`**; `3.sqm` already applied that assumption to historical local data and the two must agree. Any server-side read of the column is wrong today. |

### Identity and data scoping

The hole that generated most of the damage. Backup-only makes it **worse**, because re-pointing the
backup destination becomes routine.

| Where | Defect | Consequence |
|---|---|---|
| all four `.sq` files | **No read query filters by `userId`** — `all:`, `completeTransactions:`, `liveTotals:`, `getAccountBalance:`, `monthlyStats:` filter only `deletedAt IS NULL` | One account's money appears in another account's Home balance and Reporte |
| `DefaultAuthRepository.kt:88-89`, `AndroidPlatformModule.kt:34` | `signOut()` calls `client.auth.signOut()` and nothing else. One SQLite file per device, no per-user DB, no wipe on user change | The next account inherits the previous one's rows |
| `transactions.sq:261`, `categories.sq:87`, `accounts.sq:87` | `updateFromRemote` sets `userId` unconditionally; default categories carry fixed identical UUIDs on every install | The same 23 PKs exist on every device that ever ran the app |

### Architecture

**Verdict: redesign the boundaries, do not rewrite the algorithm.** The algorithm is the part that
was hardened. The **Gradle/import dependency rule is clean** — every violation below is placement
and vocabulary, not imports.

| Where | Defect |
|---|---|
| `SyncRepository.sync()` | Returns `Unit`. `RemoteRowOutcome`, `PullResult`, per-row `Resolution` all discarded at the boundary — nothing above `:data` can know what happened |
| `SyncDataUseCase.kt:11-15` | The application layer is three lines. Policy lives in `:presentation` (`SyncOrchestrator`: triggers, debounce, backoff, unilateral sign-out at `:273-282`) and `:data` (`DefaultSyncRepository`: FK order, cursor hold, global max, silent no-op at `:66`) |
| `data/.../sync/` (9 files) | Never touches a domain entity — only `com.emm.domain.*` imports are ports plus `DomainException`. Row → `*RowDto` → PostgREST, so no domain invariant applies to inbound rows |
| `DefaultSyncRepository.kt:45`, `:66` | Takes a use case as a constructor dependency, and silently no-ops when unauthenticated — so `runSync()` stamps `lastSyncedAt` for a zero-byte cycle |
| `SyncLogger` | A second, untyped error channel parallel to `DomainException`, against the stated contract; eleven failure modes exist only as formatted strings |
| `BaseTableSync.kt` | 428 lines, 214 of them comment, 10 abstract members, `transact: (() -> Unit) -> Unit` passed as a parameter because inheritance could not express the collaboration |
| the four `*TableSync` | 72–82% identical (Account↔Category: 107 of 131/137), ~260 duplicated lines. Wrong axis: what varies is (DTO, query object, field mapping) — data, not behaviour. Wants a descriptor, not a subclass |
| `SyncOrchestrator` | Eleven reasons to change, five pieces of mutable state |
| cursor store | Persisted by `:presentation` into prefs, outside the DB transaction that writes the rows it orders. `AndroidPlatformModule.kt:25-27` records it already lost data once via a `Build.ID`-keyed prefs file |
| `presentation/build.gradle.kts:39-40` | `export(project(":data"))` makes the whole engine public ABI of `JustChillKit`, for zero consumers |

### Concurrency

| Where | Defect | Consequence |
|---|---|---|
| `SyncOrchestrator.kt:113-114`, read/cleared `:258-259` | `manualRequestPending` is check-then-act across threads; `@Volatile` gives visibility, not atomicity | A manual tap in the window is reclassified as automatic and its failure snackbar suppressed. Needs `getAndSet(false)` |
| `:171` vs `:261`,`:269`,`:274`,`:288`,`:294` | `_status` mutated read-modify-write from two concurrent coroutines | Lost updates. Needs `update {}` |
| `:265` | `currentUserId` read **after** `syncData()` returns | A cycle spanning an account switch stamps the wrong user's `lastSyncedAt` |
| `_events` = `Channel(BUFFERED)` (64); `iosApp/iosApp/ContentView.swift` | `send` suspends inside the single consumer loop, and iOS has no collector yet still calls `bootstrapAppGraph` → `start()` | After 64 events sync wedges permanently on iOS |
| `:198-199` | `filter` sits before `debounce` | A transition to 0 cannot cancel an in-flight window |
| triggers (a) and (c); `DefaultAuthRepository.kt:32-34` | `flatMapLatest` over `observeSession()` with no `distinctUntilChanged`; mapping a StateFlow drops de-duplication, and re-subscribing `resumeEvents` replays `onResume` because `LifecycleRegistry` replays to current state | Spurious cycles. Trigger (b) at `:159-164` is correctly guarded — copy it |
| `SyncModule.kt:82-88` | No cycle-level retry or backoff; the app scope is never cancelled and there is no `stop()` | — |
| `SyncOrchestrator` triggers | **No connectivity-regained trigger** — only on-resume, sign-in and debounced writes | A sync that fails offline waits for the next `ON_RESUME`. No data loss, just latency. Fix: `callbackFlow` over `ConnectivityManager.NetworkCallback.onAvailable`, filtered by authenticated + (`pendingCount > 0` or `lastSyncFailed`), injected like `resumeEvents`. Do **not** fix it in isolation: the debounced-writes trigger is half the loop in §3, and these triggers are being replaced by the pure `SyncSchedule` of §9 |
| `data/.../shared/Dispatchers.kt:5` | `ioDispatcher` is a global `expect val` that cannot be redirected in a test, while a real `DispatchersProvider` exists (`presentation/.../core/DispatchersProvider.kt:5`) used only by the dev `experiences/` playground | Engine code is untestable off the IO dispatcher |

**Time:** the only direct clock read on the sync path is `SyncOrchestrator.kt:264`, already recorded
as injection debt in DATE_AUDIT #7. A full sweep found no undocumented ones — which independently
verifies #7's claim to name every surviving production read.

### Tests

~70% of the code is exercised and ~0% of the behaviour that produced the bug: every suite stops at a
module boundary and sync is *entirely* an inter-module protocol. **Zero push tests exist** — only
`TransactionTableSync` is `open`, the other three are final. Tautologies to fix:
`SyncOrchestratorTest.kt:112` (`coVerify(atLeast = 1)` already satisfied by the sign-in sync),
`:140` (`MutableStateFlow(0L).value = 0L` never emits), and both tests in
`ObservePendingSyncCountUseCaseTest`.

**The test that would have caught it:** *given a pending count that never reaches 0 and a cycle that
always writes, when the orchestrator runs 60 virtual seconds with no input, then `syncData` is
invoked at most once.* It cannot be written today — the feedback edge has no representation, the
`resumeEvents` fake does not replay, and the `sessionFlow` fake de-duplicates where production does
not. **Making it writable is the deliverable of the redesign.**

## 6. Correct — preserve verbatim

Do not "clean up" any of these during the rewrite.

- **`markSynced ... AND updatedAt = ?`** on all four tables — closes the read-then-mark write-loss
  window. Two known narrow residuals: a same-millisecond collision via `softDeleteAllLive`
  (`transactions.sq:145-148`), and `claimAll`/`unclaimAll`/`markPendingForResync` setting
  `syncState` without touching `updatedAt`.
- **RLS.** `with check (user_id = (select auth.uid()))` on all four tables, policies `to
  authenticated` only; `delete_account()` is `security definer` with `set search_path = ''`, revoked
  from `public`/`anon`; `requireValidSession = true` (`SupabaseModule.kt:37`). The cross-tenant
  upload in §7 is **not** an RLS failure — by push time those rows genuinely carried the new user's
  id.
- Composite-keyset pagination · hold-cursor-on-skip · the `Deferred`/`Dropped` taxonomy ·
  `toSyncDomainException` · the `SyncMutex` rule · the entire local schema.
- `ConflictResolver` and ADR 004's rule — dormant under backup-only, restored intact if multi-device
  returns.

## 7. Production forensics

Project `pievwpleqmrjwszuuivr` ("Justtt"). Two tenants. **No data merged, no PK collisions**, and no
`<entity>_id` under two user_ids.

| user_id | email | accounts | categories | transactions | recurring |
|---|---|---|---|---|---|
| `5d008477-…-59bdfc710be5` | edmashaki@gmail.com | 2 | 28 | 19 | 1 |
| `0b8482e5-…-c06b4a6bd38d` | edgardo.emm20@gmail.com | 0 | 1 | 13 | 0 |

- **The account deletion never happened.** edmashaki is still in `auth.users`, `deleted_at` NULL,
  not banned; its 50 rows frozen at `server_updated_at` 2026-06-11 10:25:27–10:35:24. edgardo.emm20
  was created 2026-08-12 06:27:25Z, first sign-in ever; all 14 rows from that day. The 23 default
  categories match the seed at `DatabaseDriver.android.kt:40-62` exactly, 23/23, all edmashaki's.
- **Zero rows with non-null `deleted_at` anywhere** — no soft delete ever reached production.
- **Cross-tenant dangling references.** All 13 of the new tenant's transactions point at account
  `e7139907-…-5368778169b2`, owned by edmashaki, as do 4 of their 5 distinct category refs. RLS
  hides those parents from the new tenant. The inserts succeeded because the public schema has no
  foreign keys at all — deliberate, per ADR 001.

## 8. Why the account deletion failed

**The RPC was never sent.** Zero `/rest/v1/rpc/*` requests in the full 24h log window, zero
executions in `postgres_logs` — a client-side failure before the HTTP call. Sign-out is not the
cause: `DefaultAuthRepository.kt:103-106` runs the RPC at `:104` and `signOut(SignOutScope.LOCAL)`
at `:105`, in that order.

| Candidate | Evidence | Predicts on screen |
|---|---|---|
| **Leading** — `ProfileViewModel.kt:99` `if (currentState.op != ProfileOp.None) return` silently discards a confirmed intent: no effect, no snackbar, no state change, and the delete path has no logging of its own (`CrashReportingSyncLogger` covers only sync) | the only candidate producing **both** zero traffic and zero feedback | nothing |
| Runner-up — `DeleteUserAccountUseCase.kt:77` throws `Unauthorized("No authenticated session")` | zero traffic | "Credenciales incorrectas o sesión expirada" |

> **Open question, only the owner can settle it: what appeared on screen when he pressed delete —
> nothing, or an error?** That answer picks between the two rows.

The success message (`hh/shared/ProfileMessageText.kt:9`) is emitted at `ProfileViewModel.kt:124`
only after all three steps succeed. **The server is healthy** — function exists, `security
definer`, `authenticated` has EXECUTE, `postgres` has DELETE on `auth.users`, all three migrations
applied. **Would recur today:** server no, client yes, the guard is unchanged.

**Fix regardless:** `toAuthDomainException` (`DefaultAuthRepository.kt:205-227`) has no
`RestException` or `SessionRequiredException` branch, so both fall to `DomainException.Unknown`.
`toSyncDomainException` (`DefaultSyncRepository.kt:145-166`) has both.

## 9. Target shape

Introduce in `:domain`: **`SyncOutcome`** (replaces `Unit`, with a `NotAuthenticated` case),
**`SyncTrigger`** sealed, **`Watermark`**, **`SyncState`** as a type instead of string literals,
**`SyncableRecord`**, and **`SyncSchedule`** as a pure
`(session, pendingCount, resume) -> Flow<SyncTrigger>` — that last one is what makes the §5 test
writable. Collapse the four `*TableSync` into one engine plus four spec value objects. Move cursor
persistence into SQLDelight, inside the transaction that writes the rows it orders.

### Data safety — the binding constraint

**No `CREATE TABLE` change to the four data tables**: `syncState`, `deletedAt`, `userId` stay
byte-identical. **Exactly one additive migration** — `4.sqm`, `CREATE TABLE sync_state(...)` — at
the very end, alone, with its own migration test; every step before it touches zero persisted
bytes. Skipping the cursor copy is safe: a null cursor triggers a full re-pull, certified idempotent
by `BaseTableSync.kt:81-85` and ADR 002 Decision 3 — one slow sync, not data loss.

## 10. Phased plan

| Phase | Scope | State |
|---|---|---|
| 0 | Why the delete failed. Root cause found (§8); make delete report its failure; decide the tenants | in progress — blocked on the open question |
| 1 | Per-user data scoping (§5) | **design fork undecided**: per-user DB file vs `userId` filter on every read vs wipe-on-switch |
| 2 | Server-side conditional upsert | **deprioritized** by backup-only, not cancelled — it is the fix for §4 row 1 if multi-device returns |
| 3 | Boundary redesign (§9) | queued |

**Tenant cleanup — not executed.** Recommendation: clear both tenants and re-upload from the device.
**Hard prerequisite:** verify a local export works and the device's SQLite is intact *before*
touching the server.

For scale on how much runtime evidence exists at all: `docs/PROGRESS.md:237` records the sync paths
were validated against a local Supabase stack exactly **once**, on 2026-06-10.
