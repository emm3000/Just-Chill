create or replace function public.delete_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'delete_account requires an authenticated user';
  end if;

  -- FK-safe order: children before parents.
  delete from public.transactions where user_id = uid;
  delete from public.recurring_movements where user_id = uid;
  delete from public.categories where user_id = uid;
  delete from public.accounts where user_id = uid;

  delete from auth.users where id = uid;
end;
$$;

revoke execute on function public.delete_account() from public, anon;
grant execute on function public.delete_account() to authenticated;
