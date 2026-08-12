# Local-first sync — execution plan (slices 1-5)

> Operational plan for the local-first-sync track. Decisions live in
> [ADR 001](../adr/001-reverse-local-only-to-local-first-optional-sync.md)
> and [ADR 002](../adr/002-pull-cursor-uses-server-set-timestamp.md) —
> this doc only sequences the work and carries the re-derived Supabase
> SQL. If this doc and an ADR disagree, the ADR wins.
>
> Re-derived 2026-06-09 from ADRs + trunk code + git history (the
> original SDD design artifact lived in engram, now disabled for this
> repo). Prior auth/sync implementation recoverable from commits
> `5c0e471` (Supabase client + auth), `906d55c`⁻¹ (auth layers),
> `d322a2d`⁻¹ (auth screens), `ff7afc8`⁻¹ (remote/sync layer).
>
> **Module names below predate the KMP migration.** `:app` is now `:androidApp`.
> Corrected 2026-08-12: the Koin wiring, `SyncOrchestrator` and the `SyncCursorStore`
> implementation live in **`:presentation`** commonMain
> (`presentation/src/commonMain/kotlin/com/emm/justchill/core/sync/`), not in
> `:ui-android` commonMain as this paragraph used to say — `:ui-android` has only
> `androidMain` and `androidHostTest`, no commonMain at all, and holds just the
> Compose UI. The `SyncCursorStore` port is in `:domain`; the engine is still in `:data`.
>
> **⏸ PAUSED 2026-08-12 — sync is switched off in production** (`253e170`,
> `SYNC_TEMPORARILY_DISABLED`). Do not resume the slices below as written: the
> decision of record is now **backup only, one device at a time**, which retires
> the conflict-convergence design this plan assumes. Read
> [`docs/sync/AUDIT.md`](AUDIT.md) first — it carries the root cause, the
> production forensics, and the phased plan that replaces slice 5. Slices 1-4 stay
> as the record of what shipped.

## Status

| Slice | Scope | Status |
|---|---|---|
| 1 | Schema v3: soft-delete + sync metadata | ✅ trunk `59b8adf` |
| 2 | Auth opt-in (email/password) + claim local data | ✅ trunk `700d28b` |
| 3 | Sync engine: push/pull + LWW + cursor | ✅ trunk (hardened, device-verified 2026-06-10) |
| 4 | Sync lifecycle: triggers + status UI | ✅ trunk (device-verified on emulator 2026-06-10) — realtime was never built, see slice 4 item 2 below |
| 5 | Compliance + multi-device QA + release gate | ⏸ paused 2026-08-12 — superseded by `AUDIT.md` |

## Environments

**Dev = local Supabase stack** (since 2026-06-10): `supabase start`
(Docker) at the repo root. The schema lives as a CLI migration in
`supabase/migrations/` — the single source of truth for the server
schema (the SQL in this doc is reference only; if they diverge, the
migration wins). `supabase db reset` re-applies from scratch.

- Emulator reaches the host stack via `http://10.0.2.2:54321`;
  physical device uses the machine's LAN IP.
- `supabase.properties` (gitignored, repo root) already carries the
  dev values, consumed as `BuildConfig` fields per flavor:

  ```properties
  dev.supabase.url=http://10.0.2.2:54321
  dev.supabase.anonKey=<local demo anon JWT>
  prod.supabase.url=
  prod.supabase.anonKey=
  ```

**Prod = cloud project** — still pending (human task), but it no
longer blocks slices 2-4; it blocks slice 5 (release). When created:
`supabase link --project-ref <ref>` + `supabase db push` applies the
same migrations, then fill the `prod.*` properties.

The anon key is public by design (RLS is the guard). The
`service_role` key must NEVER appear in the repo or the app.
Note: the old implementation (`5c0e471`) read url/key from string
resources — do not repeat that; BuildConfig from a gitignored
properties file keeps keys out of git.

## Supabase schema (re-derived)

Mirrors the local SQLDelight v3 schema (`data/src/commonMain/sqldelight/com/emm/data/*.sq`).
Per ADR 001: client-generated TEXT PKs, `user_id NOT NULL`, no server
FKs, RLS as tenant guard. Per ADR 002: `server_updated_at` + shared
trigger, only server-side logic allowed.

Mapping rules:
- `syncState` is local-only — never pushed.
- `deletedAt` IS pushed (tombstones must propagate).
- Client timestamps (`updated_at`, `created_at`, `deleted_at`, `date`)
  stay `bigint` epoch millis — LWW compares client values; converting
  to `timestamptz` would invite tz/precision bugs.
- Amounts stay `bigint` (cents).

```sql
-- Shared trigger (ADR 002): orders pulls, never resolves conflicts.
create or replace function public.set_server_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.server_updated_at := now();
  return new;
end;
$$;

create table public.accounts (
  account_id        text primary key,
  name              text not null,
  type              text not null,
  currency          text not null,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now()
);

create table public.categories (
  category_id       text primary key,
  name              text not null,
  icon              text not null,
  color             text not null,
  category_type     text not null,
  is_default        boolean not null default false,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now()
);

create table public.transactions (
  transaction_id    text primary key,
  type              text not null,
  amount            bigint not null,
  description       text not null default '',
  date              bigint not null,
  category_id       text,
  account_id        text not null,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now()
);

create table public.recurring_movements (
  id                     text primary key,
  name                   text not null,
  type                   text not null,
  amount                 bigint,
  description            text not null default '',
  category_id            text,
  account_id             text not null,
  frequency              text not null,
  day_of_month           integer not null,
  is_active              boolean not null default true,
  last_confirmed_period  text,
  updated_at             bigint not null,
  created_at             bigint not null,
  deleted_at             bigint,
  user_id                uuid not null,
  server_updated_at      timestamptz not null default now()
);

-- Trigger + pull index + RLS, per table.
do $$
declare t text;
begin
  foreach t in array array['accounts','categories','transactions','recurring_movements']
  loop
    execute format(
      'create trigger %I before insert or update on public.%I
       for each row execute function public.set_server_updated_at()',
      t || '_set_server_updated_at', t);
    execute format(
      'create index %I on public.%I (user_id, server_updated_at)',
      t || '_pull_idx', t);
    execute format('alter table public.%I enable row level security', t);
    execute format(
      'create policy %I on public.%I for all to authenticated
       using (user_id = (select auth.uid()))
       with check (user_id = (select auth.uid()))',
      t || '_owner_policy', t);
  end loop;
end;
$$;
```

Deferred (documented in ADR 001, NOT in v1): tombstone purge via
`pg_cron` (~90d window) + local purge of already-synced tombstones.

## Slice 2 — Auth opt-in (~550 lines)

Goal: a user can sign in / sign up / sign out from Profile. No sync
yet. Anonymous-local stays the default; no gate anywhere.

Tasks:

1. **Gradle**: restore `supabase-bom`, `auth-kt`, `postgrest-kt`,
   `ktor-client-okhttp` to `libs.versions.toml` + `:data` deps
   (reference versions at `5c0e471`: BOM 3.2.x, ktor 3.3.x — bump to
   current stable).
2. **Build wiring**: read `supabase.properties` in `app/build.gradle.kts`
   per flavor → `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY`.
3. **`:domain/auth`**: `AuthRepository` interface (`sessionStatus: Flow<SessionStatus>`,
   `signIn`, `signUp`, `signOut`), `SessionStatus` sealed type, use cases
   `SignInUseCase`, `SignUpUseCase`, `SignOutUseCase`,
   `ObserveSessionUseCase` (naming: `[Verb][Noun]UseCase`).
4. **`:domain/auth`**: `ClaimLocalDataUseCase` — on FIRST sign-in,
   `UPDATE <table> SET userId = :uid, syncState = 'Pending' WHERE userId IS NULL`
   across the four tables (ADR 001, point 7). Idempotent. Runs before
   any push ever happens (sync arrives in slice 3, so ordering is safe).
5. **`:data/auth`**: `DefaultAuthRepository` over supabase `auth-kt`
   (pattern at `git show 5c0e471` — `signInWith(Email)`, `signUpWith(Email)`,
   `signOut()`, `sessionStatus` Flow). Errors funnel through `SafeCall`
   → `DomainException.Unauthorized` / `NetworkUnavailable` (the
   `Unauthorized` copy from S5 finally becomes reachable).
6. **`:data`**: `claimAll` queries in the four `.sq` files.
7. **`:app`**: `SupabaseModule` (client with `Auth` + `Postgrest`
   plugins) + `AuthModule` Koin, wired in the existing module list.
8. **`:app` UI**: Profile gains a "Cuenta" section — signed-out: row
   "Iniciar sesión"; signed-in: email + "Cerrar sesión". New
   `AuthScreen` (email/password, sign-in/sign-up toggle) as standard
   MVI trio (`AuthUiState` / `AuthIntent` / `AuthEffect`), pushed route
   (NOT a start-destination gate). UI copy in Peruvian Spanish, "tú".
9. **Tests**: domain use-case tests (MockK), `ClaimLocalDataUseCase`
   ordering/idempotency, error-mapping tests for the repo.

Sign-out behavior: keep local data, keep session-less state — sync
simply pauses (ADR 001: "no session" is a valid state). No wipe.

Verification (manual, dev build): sign-up new user → session survives
process death → sign-out → app fully usable anonymous → sign-in again
→ no duplicate claim.

## Slice 3 — Sync engine core (~500 lines) ✅ DONE 2026-06-10

> Shipped + hardened (2 judgment-day rounds) + verified E2E on two
> emulators against the local Supabase stack: push/pull convergence,
> second-sync cursor path, tombstone propagation, real LWW conflict
> (same row edited on both devices — newest write wins everywhere).
> Implementation note: the per-table push/pull algorithm lives once in
> `data/sync/BaseTableSync.kt`; tables provide only generated-query
> adapters and reified postgrest calls.

Goal: push + pull converge two devices. Manual trigger only (a debug
"Sync now" row); automatic lifecycle is slice 4.

Tasks:

1. **`:domain/sync`**: `ConflictResolver` — pure LWW policy comparing
   client `updatedAt`, tombstone-aware (a tombstone wins/loses by the
   same rule; no special casing). JVM unit tests are the contract:
   newer-local, newer-remote, equal-timestamps (deterministic
   tie-break: remote wins), tombstone-vs-edit both directions.
2. **`:domain/sync`**: `SyncRepository` interface + `SyncDataUseCase`
   (push then pull, under a mutex).
3. **`:data/sync`**: per-table remote data sources over postgrest
   (`from(table).upsert(...)`, `select { gte("server_updated_at", cursor - 10s) }` —
   pattern at `ff7afc8`⁻¹, adjusted to ADR 002).
4. **Push**: rows with `syncState = 'Pending'` AND `userId` set →
   upsert to Supabase → mark `Synced`. Tombstones push like any row.
5. **Pull** (ADR 002): fetch by `server_updated_at >= lastPulledAt - 10s`,
   LWW-merge each row via `ConflictResolver`, idempotent local upsert,
   advance `lastPulledAt` to max `server_updated_at` seen (ISO-8601
   String). Empty/lost cursor → full re-pull (safe).
6. **`AppPreferences`**: `lastPulledAt: String?` key (per-user — clear
   on sign-out? No: scope the key by userId to survive re-sign-in).
7. **Tests**: `ConflictResolver` matrix (the core deliverable of the
   slice), merge application tests with fake local source.

Verification: two emulators, same account — create/edit/delete on A,
"Sync now" on both → B converges, tombstones propagate, no ghost rows.

## Slice 4 — Sync lifecycle (~350 lines) ✅ DONE 2026-06-10

> Implemented + verified E2E on emulator against the local Supabase
> stack. Critical pre-existing bug found and fixed during verification:
> rows created while already signed-in had userId=NULL and were invisible
> to push (distinctUntilChanged on session flow suppressed claim). Fix:
> reactive claim via observeUnclaimedCount() + flatMapLatest — claimAll
> fires whenever session is Authenticated AND unclaimed rows exist.

Goal: sync happens without the user thinking about it (foreground-only,
per ADR 001 — WorkManager background sync explicitly deferred).

Tasks:

1. Triggers: on-resume reconcile (app start / `ON_RESUME`), debounced
   push after local writes, sync-on-sign-in (right after claim).
2. ~~Realtime (`realtime-kt`) for foreground remote changes — optional
   within the slice.~~ **Never taken; out of scope permanently**
   (corrected 2026-08-12). No `realtime-kt` dependency exists anywhere in
   the repo; the only `realtime` string in Kotlin is
   `SyncOrchestrator.kt:33` saying there is none. Backup-only sync has no
   use for it.
3. Profile "Cuenta" section: last-synced timestamp + lightweight
   syncing indicator. No sync settings screen — it just works.
4. Failure posture: sync errors are silent-retry-later, NEVER block UI;
   surface only persistent auth failure (session revoked → signed-out
   state + snackbar).
5. Edge case to test: device B signs in with pre-existing anonymous
   local data → claim + LWW merge against server data converges.

## Slice 5 — Compliance + release gate (~doc-heavy) ⏸ paused 2026-08-12

> The DONE list still holds; the REMAINING list is replaced by
> [`AUDIT.md` §10](AUDIT.md#10-phased-plan). "Multi-device QA" is **moot** under
> backup-only, and the account deletion credited to `359b9ce` was verified against
> a *local* stack, then failed in production without ever sending its RPC
> (`AUDIT.md` §8).

Items DONE (2026-06-10):
- `34cca18` — defensive enum parsing: unknown remote enum values skip
  row instead of crashing. Covers `TransactionType`, `CategoryType`,
  and all `valueOf`-over-remote paths in mappers.
- `297fad3` — pull pagination with composite keyset
  `(server_updated_at, pk)` — safe for large remote datasets.
- `50ff8d4` — privacy policy rewrite (`docs/PRIVACY_POLICY.md` +
  `PrivacyPolicyScreen`) for optional sync; firebase-analytics
  dependency dropped (Crashlytics stays in prod).
- `65da609` — audit fixes: dev Crashlytics disabled; clean stop-guard
  on pull pagination.
- `359b9ce` — in-app account deletion: RPC `delete_account` (security
  definer on server); local data preserved via `unclaimAll`
  (userId→NULL, syncState→Pending, tombstones included); per-user cursor
  prefs cleared; E2E verified on emulator against local Supabase
  2026-06-10 (see verification record in `docs/PROGRESS.md`).

Items REMAINING (code):
- Instrumented E2E tests of delete use cases against real SQLite
  (slice-1 follow-up, tracked since PROGRESS).
- Multi-device QA checklist pass (clean install, offline-first week,
  sign-in late, two devices, sign-out).
- Tag + AAB.

Human tasks (unblockable by code):
- Create prod Supabase cloud project: `supabase link --project-ref <ref>`
  + `supabase db push`, then fill `prod.*` entries in `supabase.properties`.
- Host `docs/PRIVACY_POLICY.md` as a public Gist (deletion URL required
  by Play for apps with account deletion).
- Google Play Data Safety form declaration (ADR 001 marks this a legal
  obligation, gated here).

## Working agreement

- One slice ≈ one reviewable unit (~400-550 lines). Work-unit commits
  inside the slice.
- Every slice lands green: `./gradlew test` + detekt + manual
  verification listed in the slice.
- Schema changes on either side get their own commit and a note here.
- **Local schema changes always ship an `.sqm` migration** — real user
  data exists on devices since `4e6de6c` (2026-06-04). Destructive
  resets are off the table. Verified by `./gradlew :data:verifySqlDelightMigration`
  (snapshot in `data/src/commonMain/sqldelight/databases/`) and by the
  instrumented tests: `./gradlew :data:connectedAndroidDeviceTest`.
