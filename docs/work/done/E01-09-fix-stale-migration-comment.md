# E01-09 — Fix the stale doc pointer in the sync migration

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] `supabase/migrations/20260610043814_sync_schema.sql`'s comment no longer points at the archived `docs/sync/PLAN.md`
- [x] `supabase/migrations/20260814200043_backup_storage_bucket.sql`'s comment no longer points at the archived `docs/sync/ADR009_PLAN.md`
- [x] every `docs/...` path referenced anywhere under `supabase/migrations/*.sql` resolves to a file
      that exists today: `rg -o --no-filename 'docs/[^ )]+' supabase/migrations/*.sql | sort -u |
      while read -r p; do test -f "$p" && echo "OK $p" || echo "MISSING $p"; done` prints only `OK`
