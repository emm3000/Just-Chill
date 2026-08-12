-- Repair drifted primary keys on already-provisioned servers.
--
-- WHY THIS FILE EXISTS
-- --------------------
-- Commit e883ef3 (2026-06-14) edited 20260610043814_sync_schema.sql *in place*, after that
-- migration had already been applied to the production project. It changed four primary keys
-- from single-column `(<client_id>)` to composite `(user_id, <client_id>)`, and in the same
-- commit gave the Kotlin client four matching two-column `onConflict` strings.
--
-- Production was never re-migrated, so its tables kept the single-column primary keys. Postgres
-- requires a unique index whose columns match the ON CONFLICT list exactly, so from that commit
-- onward every push failed with SQLSTATE 42P10 -- "there is no unique or exclusion constraint
-- matching the ON CONFLICT specification".
--
-- TIMELINE -- be careful with these two dates, they are not the same event:
--   2026-06-11  last observed write on the production tables.
--   2026-06-14  e883ef3 ships the two-column onConflict; pushes break here.
-- The breakage starts on the 14th, three days AFTER the last remote write. Before e883ef3
-- `upsertDtos` passed no onConflict at all, so PostgREST inferred the table's own primary key
-- and it matched. The remote silence therefore PREDATES the bug and is not by itself evidence
-- of it -- the 42P10 was confirmed by reproducing it, not inferred from the quiet table.
--
-- WHY THE TOOLING NEVER NOTICED
-- -----------------------------
-- Not because the evidence was missing. `supabase_migrations.schema_migrations` is
-- (version text, statements text[], name text) and `statements` stores the applied SQL
-- verbatim, comments included -- the production ledger still literally contains
-- `account_id        text primary key`, the OLD single-column text. The drift was recorded
-- server-side the whole time.
--
-- What failed is the comparison: `supabase migration list` and `db push --dry-run` match on the
-- version string only, never on content, so both keep reporting "up to date" after an in-place
-- edit of an applied file. To actually detect this class of drift, compare the stored SQL to the
-- file on disk:
--
--   select md5(regexp_replace(array_to_string(statements, ';') || ';', '\s', '', 'g'))
--   from supabase_migrations.schema_migrations
--   where version = '20260610043814';
--
-- against  `tr -d '[:space:]' < <migration-file> | md5`.  The whitespace stripping and the
-- appended ';' are load-bearing, not cosmetic: the CLI splits the file on ';' and stores the
-- pieces, which drops the blank lines between statements and the final terminator. Hashing
-- `array_to_string(statements, ';')` raw against the file does NOT match (verified: 4316 vs
-- 4328 bytes for 20260610043814). Normalized, it matches exactly.
--
-- 20260610043814_sync_schema.sql is deliberately NOT edited again: it is already correct for
-- provisioning from zero, and re-editing an applied migration is the exact mistake being
-- repaired here. This file is the forward fix instead, and it is idempotent:
--   - fresh install / local dev  -> the PKs are already composite, this is a no-op
--   - production (drifted)       -> the PKs are rebuilt as composite
--
-- WHY THE COMPOSITE KEY IS THE CORRECT SHAPE (long form in 20260610043814_sync_schema.sql):
-- default categories are seeded with fixed, byte-identical UUIDs on every install, so a
-- client-generated id is not unique across tenants. Uniqueness must be scoped per user.
--
-- WHY THIS IS SAFE TO RUN ON A SERVER HOLDING DATA
--   - The public schema has no foreign keys on either side, so dropping a primary key cascades
--     nothing. The drop is deliberately not CASCADE: if a future dependency does exist, this
--     must fail loudly rather than silently discard it.
--   - Where an old single-column PK exists, `(id)` -> `(user_id, id)` RELAXES uniqueness. Every
--     row that satisfied the old constraint satisfies the new one, so no pre-existing row can
--     collide and no dedupe step is needed.
--   - That relaxation argument does NOT cover the no-PK-at-all branch below. If a table somehow
--     has no primary key, nothing ever enforced uniqueness and ADD PRIMARY KEY can legitimately
--     abort with a duplicate-key error. That is a safe failure, not a partial one: the whole DO
--     block is atomic, so an abort rolls back every table it already rebuilt. It is a stop-and-
--     look-at-it signal, not something this migration should paper over.
--   - user_id is `uuid not null` on all four tables, which a primary key requires.
--
-- IMPLEMENTATION NOTES
--   - The new constraint is named explicitly. `ADD PRIMARY KEY` without a name cannot fail on a
--     collision -- Postgres just picks the next free `<table>_pkey1`, which would leave the
--     server permanently disagreeing with what a fresh provision produces, reintroducing this
--     very drift class, and the column-based idempotency check below would never see it.
--   - The existing constraint name is read from the catalog rather than assumed to be
--     `<table>_pkey`, and the idempotency test compares the actual column array (conkey) rather
--     than pg_get_constraintdef text, which would be at the mercy of identifier quoting.
--   - ALTER TABLE takes ACCESS EXCLUSIVE and the block holds it on the first table until commit.
--     The work is milliseconds at these row counts, so lock contention is the only slow path --
--     hence lock_timeout, since the postgres role runs with lock_timeout = 0 and would otherwise
--     queue every query on these tables behind an indefinite wait.

do $$
declare
  target        record;
  existing_name text;
  existing_cols text[];
  wanted_cols   text[];
  replident     "char";
begin
  -- Valid because the CLI runs each migration file inside a transaction.
  set local lock_timeout = '3s';

  for target in
    select *
    from (values
      ('accounts',            'account_id'),
      ('categories',          'category_id'),
      ('recurring_movements', 'id'),
      ('transactions',        'transaction_id')
    ) as t(table_name, client_id_column)
  loop
    wanted_cols := array['user_id', target.client_id_column];

    -- The catalog query below returns NULL both for "table absent" and for "table present but
    -- has no PK". Separate them here, or a missing table would be misreported as a PK-less one
    -- and then die on ALTER TABLE with a bare "relation does not exist".
    if to_regclass('public.' || quote_ident(target.table_name)) is null then
      raise exception
        'public.% does not exist, but 20260610043814_sync_schema.sql should have created it. '
        'Refusing to continue: this database is not the schema this migration repairs.',
        target.table_name;
    end if;

    -- REPLICA IDENTITY USING INDEX pins a specific index; dropping the PK it points at leaves it
    -- dangling and the next UPDATE on a published table fails with 55000. Production is on the
    -- default 'd' (its public-schema dump declares no REPLICA IDENTITY and no publication
    -- membership), which self-heals, so this is insurance rather than a live concern. It stops
    -- on any 'i' without checking whether the pinned index is actually the PK -- deliberately
    -- conservative, because a false stop is recoverable and a false proceed is not.
    select rel.relreplident into replident
      from pg_class rel
      join pg_namespace n on n.oid = rel.relnamespace
     where n.nspname = 'public'
       and rel.relname = target.table_name;

    if replident = 'i' then
      raise exception
        'public.% uses REPLICA IDENTITY USING INDEX. Dropping its primary key would leave that '
        'index dangling and break replicated UPDATEs (55000). Resolve this table by hand.',
        target.table_name;
    end if;

    select c.conname,
           (
             select array_agg(a.attname order by k.ord)
             from unnest(c.conkey) with ordinality as k(attnum, ord)
             join pg_attribute a
               on a.attrelid = c.conrelid
              and a.attnum = k.attnum
           )
      into existing_name, existing_cols
      from pg_constraint c
      join pg_class rel on rel.oid = c.conrelid
      join pg_namespace n on n.oid = rel.relnamespace
     where n.nspname = 'public'
       and rel.relname = target.table_name
       and c.contype = 'p';

    if existing_cols is not distinct from wanted_cols then
      raise notice 'public.% already has primary key (%), leaving it alone',
        target.table_name, array_to_string(wanted_cols, ', ');
      continue;
    end if;

    if existing_name is not null then
      raise notice 'public.%: dropping primary key % (%)',
        target.table_name, existing_name, array_to_string(existing_cols, ', ');
      execute format('alter table public.%I drop constraint %I',
        target.table_name, existing_name);
    else
      raise notice 'public.% has no primary key, adding one', target.table_name;
    end if;

    execute format('alter table public.%I add constraint %I primary key (user_id, %I)',
      target.table_name, target.table_name || '_pkey', target.client_id_column);

    raise notice 'public.%: primary key is now % (%)',
      target.table_name, target.table_name || '_pkey', array_to_string(wanted_cols, ', ');
  end loop;
end;
$$;
