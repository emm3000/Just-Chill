-- Sync schema for local-first sync (ADR 001 + ADR 002).
-- Mirrors the local SQLDelight v3 schema. Client-generated TEXT PKs,
-- no server-side FKs, RLS as the tenant guard. The only server-side
-- logic is the server_updated_at trigger that orders pulls (ADR 002);
-- it never resolves conflicts (LWW compares client updated_at).
--
-- Mapping rules (see docs/sync/PLAN.md):
--   - syncState is local-only and never pushed.
--   - deleted_at IS pushed (tombstones must propagate).
--   - Client timestamps stay bigint epoch millis.
--   - Amounts stay bigint (cents).

create or replace function public.set_server_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.server_updated_at := now();
  return new;
end;
$$;

-- Primary keys are COMPOSITE (user_id, <client_id>). The client-generated id alone is NOT
-- unique across tenants: default categories are seeded with FIXED, byte-identical UUIDs on
-- every install (see DatabaseDriver.android.kt / DefaultCategorySeed.ios.kt). A single-column
-- PK on the client id makes account B's push upsert collide with account A's row and fail the
-- RLS USING check (42501). Scoping uniqueness by (user_id, id) lets the SAME user's multiple
-- devices still dedupe a shared id while two DIFFERENT users may hold the same id independently.
-- Safe to change: this schema has no server-side FKs, so the composite PK cascades nothing.
create table public.accounts (
  account_id        text not null,
  name              text not null,
  type              text not null,
  currency          text not null,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now(),
  primary key (user_id, account_id)
);

create table public.categories (
  category_id       text not null,
  name              text not null,
  icon              text not null,
  color             text not null,
  category_type     text not null,
  is_default        boolean not null default false,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now(),
  primary key (user_id, category_id)
);

create table public.transactions (
  transaction_id    text not null,
  type              text not null,
  amount            bigint not null,
  description       text not null default '',
  date              bigint not null,
  category_id       text,
  account_id        text not null,
  updated_at        bigint not null,
  created_at        bigint not null,
  deleted_at        bigint,
  user_id           uuid not null,
  server_updated_at timestamptz not null default now(),
  primary key (user_id, transaction_id)
);

create table public.recurring_movements (
  id                     text not null,
  name                   text not null,
  type                   text not null,
  amount                 bigint,
  description            text not null default '',
  category_id            text,
  account_id             text not null,
  frequency              text not null,
  day_of_month           integer not null,
  is_active              boolean not null default true,
  last_confirmed_period  text,
  updated_at             bigint not null,
  created_at             bigint not null,
  deleted_at             bigint,
  user_id                uuid not null,
  server_updated_at      timestamptz not null default now(),
  primary key (user_id, id)
);

-- Trigger + pull index + RLS, per table.
do $$
declare t text;
begin
  foreach t in array array['accounts','categories','transactions','recurring_movements']
  loop
    execute format(
      'create trigger %I before insert or update on public.%I
       for each row execute function public.set_server_updated_at()',
      t || '_set_server_updated_at', t);
    execute format(
      'create index %I on public.%I (user_id, server_updated_at)',
      t || '_pull_idx', t);
    execute format('alter table public.%I enable row level security', t);
    execute format(
      'create policy %I on public.%I for all to authenticated
       using (user_id = (select auth.uid()))
       with check (user_id = (select auth.uid()))',
      t || '_owner_policy', t);
  end loop;
end;
$$;
