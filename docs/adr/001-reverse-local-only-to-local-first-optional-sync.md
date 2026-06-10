# ADR 001 — Reverse local-only to local-first with optional sync

- **Status**: Accepted
- **Date**: 2026-06-04
- **Deciders**: Edgardo Muñoz
- **Supersedes**: the "100% local-only, no backend, no auth, no sync" positioning baked into `CLAUDE.md`, `domain/CLAUDE.md`, and the `Won't Have` rows W-02 / W-03 / W-11 of `docs/PRODUCT_REQUIREMENTS.md`.
- **Amended by**: [ADR 002](002-pull-cursor-uses-server-set-timestamp.md) (Decision point 5 — the pull cursor no longer compares client-written `updatedAt`).

> Resumen (es): JustChill deja de ser estrictamente local-only y pasa a ser local-first con sync OPCIONAL. La app sigue siendo 100% usable sin cuenta (estado anónimo-local, sin gate de login). Iniciar sesión es opt-in desde Perfil y habilita sync multi-dispositivo de un mismo usuario. La resolución de conflictos es last-write-wins propia (sin PowerSync/ElectricSQL), auth solo email/contraseña, y los borrados pasan a ser soft-delete con tombstones.

## Context

JustChill shipped as a deliberately local-only personal finance app: SQLDelight on-device, no auth, no network, no sync. The product requirements explicitly listed three reinforcing "Won't Have" items:

- **W-02** — no cloud backend
- **W-03** — no authentication / login
- **W-11** — no device-to-device sync

That positioning was a signed product decision, not an accident. Reversing it therefore requires a documented decision, which is this ADR.

Two facts changed the calculus:

1. **A real user need emerged**: one person using the app across more than one device (phone + tablet) currently has no way to keep both in sync, and a device loss means data loss beyond the manual JSON backup. This is single-user-multi-device, NOT multi-user collaboration.
2. **Effectively zero deployed users**: version `2.0.0-alpha` was never uploaded to the Play Console, so reversing the privacy posture now carries no migration burden on a live user base. There is also a full prior Supabase + auth + sync implementation recoverable from git history (commits `ff7afc8`, `906d55c`, `d322a2d`, `5c0e471`), so the change is largely an adaptation rather than a from-scratch build.

A hard constraint shaped the final form of the decision: the original local-first value proposition (US-01: "the app works immediately, no sign-up wall") MUST be preserved. An earlier draft over-corrected by making login mandatory; that was explicitly walked back (see Engram decision `decision/local-first-sync-auth-model`).

## Decision

Adopt **local-first architecture with OPTIONAL sync**:

1. **Anonymous-local is the default, first-class running state.** First launch shows the main UI directly. No login screen, no prompt, no gate. The app is fully functional with no account and no network. "No session" is a valid state where sync is simply paused.
2. **Sign-in is opt-in from the Profile screen** and is what enables sync. Auth is **email/password only** (Google Sign-In explicitly excluded from v1).
3. **The local SQLDelight database remains the single source of truth.** Supabase is a sync backend, never the primary store.
4. **Conflict resolution is roll-your-own last-write-wins (LWW)** by `updatedAt`, implemented as a **pure domain policy** (`ConflictResolver` in `:domain`, JVM-unit-testable, no Android/Supabase dependencies). We do NOT adopt PowerSync or ElectricSQL.
5. **Change detection uses a dirty flag** (`syncState = 'Pending'`, clock-skew safe) plus a per-user `lastPulledAt` cursor for pulls.
6. **Hard-delete is replaced by soft-delete tombstones** (`deletedAt`). Referential integrity moves out of SQLDelight foreign-key actions and into domain use cases.
7. **`userId` is nullable LOCALLY** (anonymous rows have none until claimed on first sign-in); on the Supabase server `user_id` is `NOT NULL` because only signed-in/claimed rows are ever pushed.
8. **Supabase is a "dumb" LWW store**: mirrored tables with client-generated TEXT primary keys, no server-side foreign-key constraints, and Row Level Security (`auth.uid() = user_id`) as the real tenant guard.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **PowerSync** | Turn-key Postgres sync with conflict handling, but: adds a hosted dependency + its own pricing, imposes its sync protocol and client SDK on a Clean-Architecture codebase, and is overkill for single-user-multi-device LWW. It optimizes for the multi-user/CRDT case we explicitly do NOT have. |
| **ElectricSQL** | Strong local-first/Postgres story, but same objections: heavier runtime, an opinionated sync engine that would bury our conflict rule inside a third-party layer instead of a testable domain policy, and immaturity risk for a solo-maintained app. |
| **Keep local-only + rely on manual JSON backup/restore for "sync"** | Zero infra, but it does not solve the actual need (two devices converging automatically) and is error-prone (manual export/import, easy to clobber newer data). |
| **Mandatory login gate (the earlier draft)** | Would have satisfied sync cleanly, but destroys US-01 (instant, no-account usability) — the core local-first promise. Explicitly reversed. |
| **CRDTs / operation-based merge** | Correct for concurrent multi-user editing, but unnecessary complexity for a single user on their own devices; LWW by `updatedAt` is sufficient and far cheaper to reason about and test. |

## Consequences

### Positive
- The local-first promise (US-01) is preserved: no sign-up wall, full offline use, instant first launch.
- Multi-device convergence and cloud-backed durability become available to users who opt in.
- The conflict rule lives in `:domain` as a pure, fast unit-testable policy — it cannot be silently broken by a data-layer change.
- Supabase stays brainless (no server FKs, RLS as the guard), so push/pull ordering is irrelevant and all integrity logic is client-side and testable.
- Reuses substantial git-recoverable prior work (auth, Supabase client, sync layer), minus the deferred WorkManager piece.

### Negative / costs
- **Integrity shift is a real footgun.** SQLDelight FK actions (`ON DELETE SET NULL` for `categoryId`, `ON DELETE RESTRICT` for `accountId`) fire only on a physical row DELETE, NOT on `UPDATE ... SET deletedAt`. Once delete is soft, those guarantees MUST be reimplemented in use cases: `DeleteCategoryUseCase` must explicitly null `categoryId` on live transactions and recurring movements (and mark them `Pending` so the de-link propagates), and `DeleteAccountUseCase`'s `countByAccount` RESTRICT check must filter `deletedAt IS NULL`. Every read path across the four tables must add `deletedAt IS NULL` or risk surfacing ghost rows.
- **Client-clock `updatedAt`** means device clock skew can mis-order LWW. Accepted for the single-user case.
- **Privacy posture reversal.** The app now (optionally) transmits and stores personal financial data off-device. This forces a rewrite of `docs/PRIVACY_POLICY.md` and a corresponding Google Play Data Safety form declaration. This is a legal/compliance obligation, not just code, and is gated as the final delivery slice.
- The anon key shipped in `BuildConfig` is public by design; security rests entirely on RLS. The `service_role` key must never be embedded.
- Three previously-signed "Won't Have" requirements (W-02/W-03/W-11) are reclassified as "optional (opt-in)" in the PRD, with a dated note.

### Neutral / follow-ups (deferred, documented)
- Tombstone purge window (server `pg_cron` ~90d; local purge of already-synced tombstones) — stub documented, not implemented in v1.
- WorkManager periodic background sync — deferred; Realtime + on-resume reconcile covers v1.

## Notes

- Full technical design (schema migration `2.sqm`, the complete Supabase SQL, the `ConflictResolver` contract, layer-by-layer file changes, and the resolved deferred decisions) lives in the SDD design artifact (`sdd/local-first-sync/design`).
- This is the first ADR in the repository; the directory `docs/adr/` is created with this file. (The spec provisionally referenced `docs/adr/004`; corrected to `001` because no prior ADRs exist.)
