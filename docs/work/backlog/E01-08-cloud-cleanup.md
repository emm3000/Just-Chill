# E01-08 — Cloud cleanup

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Runs after:** the flag flips and one real snapshot exists — see Context.

## Done when

- [ ] the two tenants' row tables in Supabase are truncated only after explicit owner confirmation at the moment, and only once a pinned, verified snapshot exists
- [ ] Auth stays intact — Storage RLS still needs it

## Context

Destructive and outward-facing: this truncates the proven-garbage rows the production forensics
in the epic describe, not a reversible local change.

The precondition is an ordering fact, not a missing decision: `SNAPSHOT_BACKUP_ENABLED` has never
been `true`, so nothing has ever been uploaded and there is no snapshot to pin. This cannot run
before the flag flips and one cycle completes. Everything up to the confirmation — the exact SQL,
the row counts, the pinned snapshot's key — can be prepared ahead of that.
