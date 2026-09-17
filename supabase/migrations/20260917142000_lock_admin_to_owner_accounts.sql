begin;

-- Admin panel access is intentionally restricted to the two owner accounts below.
-- Fail closed: any historical/accidental admin_users row outside this allow-list is removed.
delete from public.admin_users a
where a.role = 'admin'
  and not exists (
    select 1
    from auth.users u
    where u.id = a.user_id
      and lower(trim(u.email)) in ('makalega58@gmail.com', 'makalega68@gmail.com')
  );

insert into public.admin_users(user_id, role, free_test_purchases)
select u.id, 'admin', true
from auth.users u
where lower(trim(u.email)) in ('makalega58@gmail.com', 'makalega68@gmail.com')
on conflict (user_id) do update
set role = 'admin',
    free_test_purchases = true;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
  select exists (
    select 1
    from public.admin_users a
    join auth.users u on u.id = a.user_id
    where a.user_id = auth.uid()
      and a.role = 'admin'
      and lower(trim(u.email)) in ('makalega58@gmail.com', 'makalega68@gmail.com')
  );
$$;

revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

commit;
