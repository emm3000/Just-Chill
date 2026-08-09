# ADR 002 — Pull cursor uses a server-set timestamp, never client clocks

- **Status**: Accepted, Decision point 4 amended by [ADR 004](004-conflict-resolution-only-arbitrates-unpushed-edits.md)
- **Date**: 2026-06-09
- **Deciders**: Edgardo Muñoz
- **Amends**: [ADR 001](001-reverse-local-only-to-local-first-optional-sync.md), Decision point 5 (change detection / pull cursor).

> Resumen (es): el cursor de pull (`lastPulledAt`) deja de comparar contra el `updatedAt` escrito por el cliente y pasa a comparar contra una columna `server_updated_at` mantenida por un trigger en Supabase. La resolución de conflictos LWW NO cambia: sigue comparando `updatedAt` de cliente. El cambio elimina dos modos de pérdida silenciosa de filas causados por relojes de cliente.

## Context

ADR 001 (Decision point 5) defined pull change-detection as a per-user `lastPulledAt` cursor. The design initially compared that cursor against `updated_at` — a client-written, client-clock epoch timestamp, the same value LWW uses for conflict resolution.

An architecture review of the ADR found that a cursor over client-written timestamps has two failure modes, and both are **silent whole-row data loss**, not lost conflict races:

1. **Clock skew between the user's own devices.** A device with a skewed-behind clock pushes rows whose `updated_at` is already behind every other device's cursor. Those rows are never pulled anywhere — permanent, invisible divergence.
2. **Push/pull commit race, even with perfect clocks.** A push committing concurrently with another device's pull lands behind that device's just-advanced cursor and is skipped forever.

Realtime only mitigates this in the foreground; the on-resume reconcile relies on the same cursor, so it cannot recover the skipped rows.

ADR 001 accepted client-clock skew as a risk — but that acceptance is only sound for LWW conflict ordering, where losing a race costs one overwritten edit. A cursor exposed to client clocks loses entire rows.

## Decision

1. Every Supabase table gains a **`server_updated_at timestamptz not null default now()`** column, maintained by a shared `BEFORE INSERT OR UPDATE` trigger. This is the only server-side logic in the otherwise "dumb" LWW store — it orders pulls, it never resolves conflicts.
2. **Pulls fetch by `server_updated_at`**, with a small overlap window (`>= lastPulledAt - 10s`) to cover the commit-ordering race inherent to `now()` being transaction-start time. Re-pulled rows are no-ops because the LWW upsert is idempotent.
3. **`lastPulledAt` stores the max `server_updated_at` seen** as an ISO-8601 string (was a client epoch `Long`). An empty/lost cursor triggers a full re-pull, which is safe and idempotent.
4. **LWW conflict resolution is unchanged**: `ConflictResolver` still compares client-written `updatedAt`. The server column never decides a winner.
   > **Amended by [ADR 004](004-conflict-resolution-only-arbitrates-unpushed-edits.md) (2026-08-09).** The server column still never decides a winner, but the resolver no longer compares clocks at all on rows with no unpushed local edit — running LWW there let a fast-clocked device re-push its own synced copy and destroy the other device's newer edit on every cycle. Client `updatedAt` now decides only genuine two-sided conflicts.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Cursor on client `updated_at`** (the amended design) | The two silent data-loss modes described in Context. |
| **Hybrid logical clocks / per-row version vectors** | Correct and robust, but overkill for single-user-multi-device LWW; the trigger column is ~15 lines of SQL with the same practical guarantee for pulls. |
| **Full-table re-pull on every sync (no cursor)** | Immune to clocks but O(dataset) per sync; wasteful on mobile data and grows with history. The cursor + overlap window gets incremental cost with the same safety. |

## Consequences

### Positive
- The pull path is immune to client clock skew and push/pull races; cursor-based sync can no longer silently skip rows.
- Clock-skew exposure is now confined to LWW conflict ordering, where the worst case is one overwritten edit — the risk ADR 001 already accepted knowingly.
- The server stays brainless: one trigger, no FKs, no conflict logic.

### Negative / costs
- One more column + trigger per table in the Supabase schema (setup SQL grows slightly).
- The overlap window re-pulls a few rows per sync (negligible; idempotent upsert).
- `lastPulledAt` changes type (`Long` → ISO-8601 `String`), adjusting the planned `AppPreferences` field and `fetchSince` signature before slice 3 implements them.

## Notes

- Full technical detail (trigger SQL, `fetchSince` contract, watermark advancement) lives in the SDD design artifact (`sdd/local-first-sync/design`, DECISION 5).
- Nothing implemented yet depends on the amended cursor: slice 1 (already on trunk) is schema + soft-delete only; the sync engine arrives in slice 3.
