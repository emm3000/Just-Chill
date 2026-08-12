# ADR 006 — Sync is backup, not replication: one device at a time

- **Status**: Accepted
- **Date**: 2026-08-12
- **Deciders**: Edgardo Muñoz
- **Supersedes**: [ADR 001](001-reverse-local-only-to-local-first-optional-sync.md), Context point 1
  (the single-user-multi-device need) and Decision point 4 (LWW conflict resolution).
- **Renders dormant**: [ADR 004](004-conflict-resolution-only-arbitrates-unpushed-edits.md) — its
  rule is correct and is kept in the tree, unused, restored verbatim if multi-device ever returns.

> Resumen (es): el sync deja de ser replicación multi-device y pasa a ser **backup**: un device a la
> vez, nunca dos escribiendo en paralelo. La data es del **device**; la cuenta es solo un destino de
> backup. Sin conflictos posibles, `ConflictResolver`, el upsert condicional del server, la carrera
> delete-vs-edit y el arbitraje de relojes dejan de hacer falta — y el loop de producción que forzó
> el kill switch resulta ser un bug adentro de maquinaria que el producto no necesita.

## Context

ADR 001 justified sync with "one person using the app across more than one device (phone + tablet)".
That need was never exercised: there is one owner and one phone. What the multi-device machinery did
produce is the 2026-08-12 outage — a self-feeding sync loop, an account deletion that never reached
the server, and two tenants in the cloud project with cross-referencing rows. Sync is switched off in
production (`SYNC_TEMPORARILY_DISABLED`); the full findings are in
[`docs/sync/AUDIT.md`](../sync/AUDIT.md).

The loop's proximate cause sits inside conflict resolution: `KeepLocal` → `markPendingForResync` is
the only thing in the system that re-dirties an already-synced row, and it exists **only** to resolve
conflicts. Convergent bidirectional replication was built for a scenario the product does not have.

## Decision

1. **Sync is backup, not replication.** One device at a time; two devices never write concurrently.
   This is a product contract, not a technical invariant — nothing enforces it.
2. **The data belongs to the DEVICE. The account is only a backup destination.** `userId` on a row
   means "which backup this row belongs to", not "who owns it".
3. **Signing in with a different email re-points the destination** and uploads this device's ledger
   there. That is acceptable behaviour, but **the app must say so on screen before doing it** — today
   it does not. This is the one new requirement the decision creates.
4. **Production is derived, never authoritative.** The server may be cleared and re-uploaded from the
   device. Restoring the device from the server requires first proving the local export works and the
   device's SQLite is intact.
5. **ADR 004's rule and `ConflictResolver` go dormant, not deleted**, together with the rest of the
   preserve-verbatim list in `AUDIT.md` §6.
6. **Server-side conditional upsert is deprioritized, not cancelled.** It remains the fix for the
   "no real LWW" finding, `AUDIT.md` §4, if multi-device ever returns.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Keep multi-device LWW (ADR 001 as written)** | Pays for convergence machinery — the conflict resolver, server-side conditional upsert, clock-skew arbitration, multi-device QA — to serve zero concurrent writers, and that machinery is what broke production. |
| **Drop sync entirely, keep the manual JSON export** | Local-first already makes the app fully usable with no account, but the export is a manual step the owner has to remember; a device loss between exports is real data loss on a phone with accumulated real data. |
| **CRDTs / version vectors** | Correct under concurrent writers. There are none, so it buys nothing and costs a schema. |
| **Enforce one device technically (server-side device lock)** | A distributed lock plus a recovery story for a constraint one human already satisfies by owning one phone. |

## Consequences

### Positive
- **No conflicts are possible**, so `ConflictResolver`, server-side LWW, the delete-vs-edit race and
  clock-skew arbitration all stop being problems worth solving.
- **The production loop is a bug in machinery the product does not need** — under backup-only,
  `markPendingForResync` should never run at all.
- Roughly half of `AUDIT.md`'s findings retire; the problem shrinks from "convergent bidirectional
  replication" to "upload my ledger, download my ledger".
- Multi-device QA is moot, and ADR 001's knowingly-accepted clock-skew risk stops applying.

### Negative / costs
- The constraint is a promise, not an invariant. A second install that writes will overwrite
  wholesale, with no resolver left to soften it.
- **Identity and data scoping get worse, not better** (`AUDIT.md` §5 stays fully live): re-pointing
  the backup destination becomes a normal operation, and today no read query filters by `userId`,
  there is one SQLite file per device, and sign-out only signs out.
- ADR 001 and 002 stay on the books describing a system that no longer exists. A reader who stops at
  001 gets the wrong model; this ADR has to be reached first.

## Notes

- ADR 002's `server_updated_at` cursor is **unaffected** — it orders pulls and never resolved
  conflicts. ADR 003/005 (iOS) are unrelated.
- Findings, production forensics, target shape and the phased plan:
  [`docs/sync/AUDIT.md`](../sync/AUDIT.md). Status: [`docs/PROGRESS.md`](../PROGRESS.md).
