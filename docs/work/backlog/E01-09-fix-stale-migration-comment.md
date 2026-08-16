# E01-09 — Fix the stale doc pointer in the sync migration

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `supabase/migrations/20260610043814_sync_schema.sql`'s comment no longer points at the archived `docs/sync/PLAN.md`
- [ ] `supabase/migrations/20260814200043_backup_storage_bucket.sql`'s comment no longer points at the archived `docs/sync/ADR009_PLAN.md`
- [ ] the fix is verified against the server, since the Supabase CLI can detect drift on an already-applied migration by content
