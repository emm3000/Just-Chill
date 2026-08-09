# ADR 004 — Conflict resolution only arbitrates unpushed edits

- **Status**: Accepted
- **Date**: 2026-08-09
- **Deciders**: Edgardo Muñoz
- **Amends**: [ADR 002](002-pull-cursor-uses-server-set-timestamp.md), Decision point 4 ("LWW conflict resolution is unchanged").

> Resumen (es): `ConflictResolver` deja de comparar relojes en filas que ya están sincronizadas. Si la copia local no tiene ediciones sin pushear, gana el servidor y no se mira ningún timestamp. El `updatedAt` de cliente sigue decidiendo únicamente cuando ambas réplicas tienen una edición, que es el único caso sin respuesta libre de reloj. No cambia el schema ni el servidor.

## Context

ADR 001 accepted client-clock skew as a risk for LWW ordering, reasoning that losing a race costs
one overwritten edit. ADR 002 narrowed the blast radius further by moving the *pull cursor* to a
server-set column, and explicitly left conflict resolution alone.

That reasoning had a hole. `ConflictResolver.resolve` ran for **every** row a pull delivered, not
only for rows in genuine conflict, and it saw nothing but two timestamps. `findForSync` returned
`updatedAt` and nothing else, so a row the device had merely *received* from the server was
arbitrated exactly like a row the user had just edited.

The failure that produces, with device A running two hours fast:

1. A touches row X at some point. Its `updatedAt` is stamped two hours into the future.
2. B edits X later, in real time, and pushes. `updatedAt` is honest.
3. A pulls. `local (inflated) > remote (real)` → `KeepLocal` → `markPendingForResync(X)`.
4. A re-pushes its **older** content. B pulls it and loses its own newer edit.
5. Every subsequent cycle repeats step 3.

The cost is not "one overwritten edit". It is B's edit destroyed, silently, on repeat, with no
convergence — and step 3 fires on a row where A had **nothing unpushed to protect**.

## Decision

1. `ConflictResolver.resolve` takes a `LocalRevision(updatedAt, hasUnpushedEdit)` instead of a bare
   `Long?`. `hasUnpushedEdit` is the local `syncState`.
2. **A local row with no unpushed edit always yields `ApplyRemote`.** No timestamp is read. There is
   no local change to protect, so the server copy is authoritative by definition.
3. **Client `updatedAt` still decides when the local row has an unpushed edit**, with the existing
   equal-timestamps → `ApplyRemote` tie-break.
4. An unrecognised `syncState` counts as an unpushed edit — the direction that preserves what the
   user typed.
5. The four `findForSync` queries become `{account,category,transaction,recurringMovement}SyncRevision`
   and select `syncState` alongside `updatedAt`. Per-table names because four queries called
   `findForSync` would generate four colliding result classes.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Compare `server_updated_at` instead of `updatedAt`** (the obvious reading of the finding) | Does not work as stated. The local schema has no such column, and adding one answers the wrong question: a locally-edited, unpushed row has **no** server timestamp, so server time cannot order it against the remote row. It would still need this ADR's rule for the pending case, and for synced rows it yields the identical outcome — remote wins — because a synced row's server timestamp is by construction never ahead of the remote one. A schema v4 migration to arrive at the same behaviour. |
| **Hybrid logical clocks / version vectors** | Still overkill for single-user-multi-device, same as ADR 002 concluded. This change removes the data-loss mode without them. |
| **Do nothing; document it** | Defensible while there are no users, but slice 5 is the release gate and the change is small enough to land before it. |

## Consequences

### Positive
- The resurrection loop is gone. A skewed device can no longer win with, and re-push, a row it
  never edited.
- Clock exposure is confined to the one case that has no clock-free answer: two devices that both
  edited the same row before either synced.
- Deleting on one device now propagates to an untouched second device regardless of clocks — the
  old code let a fast-stamped synced row resist a remote tombstone.
- No schema change, no `.sqm`, no Supabase change, no cursor change.

### Negative / costs
- A genuine concurrent edit on two devices is still ordered by client clocks, and a skewed device
  still wins that race. Accepted: the cost there really is one overwritten edit, which is the risk
  ADR 001 knowingly took.
- `syncState`, previously local-only bookkeeping, is now an input to conflict semantics. Anything
  that sets it carelessly changes resolution behaviour.

## Notes

- The audit finding that prompted this (M11) framed it as "LWW compares client clocks", implying a
  swap to `server_updated_at`. Reading the code showed it is a bug in *when* arbitration runs, plus
  a much smaller residual decision about *how* to break a true tie. Only the second is a matter of
  taste.
- `BaseTableSyncPaginationTest` pins the payoff end-to-end: a synced local row stamped two hours
  ahead is applied, not re-pushed.
