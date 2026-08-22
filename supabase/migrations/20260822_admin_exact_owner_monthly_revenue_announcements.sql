create table if not exists public.admin_announcement (
  singleton boolean primary key default true check (singleton),
  message text not null default '',
  enabled boolean not null default false,
  updated_at timestamptz not null default now(),
  updated_by uuid null references auth.users(id) on delete set null
);

insert into public.admin_announcement(singleton, message, enabled)
values (true, '', false)
on conflict (singleton) do nothing;

alter table public.admin_announcement enable row level security;
revoke all on public.admin_announcement from anon, authenticated;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select lower(coalesce(auth.jwt()->>'email','')) = 'makalega68@gmail.com'
     and exists(select 1 from public.admin_users where user_id = auth.uid());
$$;

create or replace function public.admin_monthly_revenue_v1()
returns table(month text, revenue_minor bigint, currency text)
language sql
security definer
set search_path = public, pg_temp
as $$
  with guard as (
    select case when public.is_admin() then 1 else (select 1/0) end ok
  ), months as (
    select generate_series(date_trunc('month', now()) - interval '11 months', date_trunc('month', now()), interval '1 month') as month_start
  )
  select to_char(m.month_start, 'YYYY-MM')::text,
         coalesce(sum(c.gross_price_minor) filter (
           where p.status='verified'
             and p.created_at >= m.month_start
             and p.created_at < m.month_start + interval '1 month'
             and not exists(select 1 from public.admin_users a where a.user_id=p.user_id)
         ),0)::bigint,
         coalesce(min(c.currency) filter (where c.gross_price_minor is not null),'TRY')::text
  from guard g cross join months m
  left join public.purchases p on p.created_at >= m.month_start and p.created_at < m.month_start + interval '1 month'
  left join public.admin_product_catalog c on c.product_id=p.product_id
  group by m.month_start
  order by m.month_start desc;
$$;

create or replace function public.admin_get_announcement_v1()
returns table(message text, enabled boolean)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query select a.message, a.enabled from public.admin_announcement a where a.singleton=true;
end $$;

create or replace function public.admin_set_announcement_v1(p_message text, p_enabled boolean)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  insert into public.admin_announcement(singleton,message,enabled,updated_at,updated_by)
  values(true,left(coalesce(p_message,''),500),coalesce(p_enabled,false),now(),auth.uid())
  on conflict(singleton) do update set message=excluded.message, enabled=excluded.enabled, updated_at=excluded.updated_at, updated_by=excluded.updated_by;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'announcement_update','system','announcement',jsonb_build_object('enabled',coalesce(p_enabled,false),'message_length',length(coalesce(p_message,''))));
end $$;

grant execute on function public.admin_monthly_revenue_v1() to authenticated;
grant execute on function public.admin_get_announcement_v1() to authenticated;
grant execute on function public.admin_set_announcement_v1(text,boolean) to authenticated;

select pg_notify('pgrst','reload schema');
