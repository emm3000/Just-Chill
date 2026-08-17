# E01-08 — Cloud cleanup

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocked by:** E01-02

## Done when

- [ ] the two tenants' row tables in Supabase are truncated only after explicit owner confirmation at the moment, and only once a pinned, verified snapshot exists
- [ ] Auth stays intact — Storage RLS still needs it

## Context

Destructive and outward-facing: this truncates the proven-garbage rows the production forensics
in the epic describe, not a reversible local change.
