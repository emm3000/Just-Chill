-- Snapshot backup bucket (docs/adr/009-backup-is-a-snapshot-not-row-replication.md).
--
-- The app uploads ONE private blob per snapshot: the complete versioned JSON export, plus its
-- sidecar manifest. There is no row replication here and nothing server-side ever reads inside
-- these objects — they are opaque to the server by design (ADR 009 Decision 1).
--
-- Object key layout, and the reason the policies below look at the FIRST path segment only:
--   <user-uid>/backup-v3-<timestamp>.json          automatic snapshots
--   <user-uid>/pinned/backup-v3-<timestamp>.json   pinned snapshots, which retention never prunes
-- storage.foldername(name) returns every path segment EXCEPT the filename, so element 1 is the
-- owning uid under both shapes and one predicate covers them.
--
-- RLS on storage.objects is already enabled by Supabase and this migration must NOT try to enable
-- it: storage.objects is owned by supabase_storage_admin, so `alter table ... enable row level
-- security` would fail for the migration role. Creating policies on it is the supported path.

-- Private bucket. `public = false` is the whole point: a snapshot is the user's entire ledger, and
-- a public bucket would serve it to anyone holding the object name.
--
-- allowed_mime_types is constrained to JSON. Both things that land here are JSON (the export and
-- the manifest), so anything else arriving is a client bug, and this turns that bug into a loud
-- refusal at the edge instead of a mystery blob in the user's backup history. The uploader must
-- therefore send a BARE `application/json`, with no `charset` parameter: storage-api matches the
-- Content-Type header verbatim against this array rather than stripping parameters, so
-- `application/json; charset=utf-8` is refused with HTTP 415 `invalid_mime_type` — a hard failure
-- that reads like a server fault and is not one. The default path is safe: storage-kt falls back to
-- `ContentType.defaultForFilePath(path)`, which yields bare `application/json` for a `.json` key,
-- so the trap is only sprung by passing a charset explicitly.
--
-- file_size_limit is 10 MiB. Basis, so a future reader can move it on evidence rather than on
-- feeling: one human's personal finances, where an exported transaction costs roughly 200 bytes of
-- JSON, so 10 MiB is on the order of 50k transactions — about a hundred times the largest real
-- dataset this app has ever held, and still bounded, so a corrupt or runaway payload cannot quietly
-- consume the project's storage quota. Raise it here if a real export ever approaches it; a
-- rejected upload is a reported failure, not a silent one (hard constraint 4).
--
-- `do update`, NOT `do nothing`, and the difference is the whole value of the two paragraphs above.
-- With `do nothing`, a bucket that already exists keeps whatever settings it was born with and this
-- migration reports success while enforcing none of them — the Dashboard's "New bucket" form is
-- believed to leave both file_size_limit and allowed_mime_types null (unverified against the hosted
-- Dashboard), so a `backups` bucket created by hand and then migrated over would silently accept
-- unbounded payloads of any content type. It also breaks the "raise it here" workflow: a later
-- migration editing these values would be a no-op forever. `do update` makes this file the single
-- declaration of the bucket's settings and makes re-running it converge. It touches bucket metadata
-- only — never storage.objects, so no stored snapshot moves.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('backups', 'backups', false, 10485760, array['application/json'])
on conflict (id) do update set
  public             = excluded.public,
  file_size_limit    = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- Owner policies, modelled on the row-table policies in 20260610043814_sync_schema.sql:
--   - `to authenticated` only. anon never touches this bucket; ADR 009 Decision 1 says no session
--     means no pipeline, never a queued upload.
--   - `(select auth.uid())` rather than a bare `auth.uid()`. That form is deliberate and is the
--     same one the sync schema uses: wrapped in a scalar subquery, Postgres evaluates it once per
--     statement instead of once per row.
--
-- Least privilege — the grants are select, insert and delete, and NOT update. The pipeline needs
-- select to list snapshots and to read a snapshot back for the hash check, insert to upload, and
-- delete for the retention prune (Phase 2c). It never needs update: a snapshot name carries its own
-- timestamp, so every blob is write-once and no key is ever rewritten. Granting update would only
-- widen what a stolen JWT can do to an existing backup.
--
-- The delete grant is necessary but not sufficient, and the difference is easy to trip over:
-- storage.objects carries a storage.protect_delete() trigger that refuses `delete from
-- storage.objects` outright ("Direct deletion from storage tables is not allowed. Use the Storage
-- API instead."), whatever the policy says. The prune therefore has to go through the Storage API —
-- which is what the client does anyway — and this policy is what authorises it there.
--
-- Same class of trap, on the upload side: `storage.s3_multipart_uploads` and
-- `storage.s3_multipart_uploads_parts` have RLS enabled with zero policies, so the resumable/TUS
-- path (storage-kt's `uploadAsFlow`) is closed to `authenticated` and fails with an RLS error that
-- names none of this. Irrelevant under a 10 MiB ceiling — a one-shot upload is the right call
-- anyway — but reach for resumable and this is why it refuses.
--
-- Idempotent: drop-then-create, so re-running this migration is safe.

drop policy if exists "backups_owner_select" on storage.objects;
create policy "backups_owner_select"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'backups'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists "backups_owner_insert" on storage.objects;
create policy "backups_owner_insert"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'backups'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

drop policy if exists "backups_owner_delete" on storage.objects;
create policy "backups_owner_delete"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'backups'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
