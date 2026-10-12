-- Eve, the 3D home friend, is earned only from the weekly throne podium (never sold).
-- Each completed week from 2026-10-12 gives Eve shards by final rank: 1st = 3, 2nd = 2, 3rd = 1.
-- 12 shards make Eve (1st place 4 times, 2nd 6 times, 3rd 12 times, or any mix).
-- Shards are read from the verified weekly XP ledger, so nothing has to run at the week boundary.
-- Admins own Eve automatically. Existing balances, items and rewards are untouched.

create table if not exists public.eve_companion_owners (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  acquired_at timestamptz not null default now(),
  source text not null check (source in ('throne_shards', 'admin_grant'))
);
alter table public.eve_companion_owners enable row level security;
drop policy if exists eve_owner_read_own on public.eve_companion_owners;
create policy eve_owner_read_own on public.eve_companion_owners
  for select to authenticated using (user_id = (select auth.uid()));
revoke insert, update, delete, truncate on public.eve_companion_owners from anon, authenticated;
revoke all on public.eve_companion_owners from anon;
grant select on public.eve_companion_owners to authenticated;

create or replace function private.eve_shard_weeks(p_user uuid)
returns table(week_start date, rank integer, shards integer)
language sql stable security definer set search_path = '' as $$
  with ranked as (
    select e.week_start, e.user_id,
      row_number() over (partition by e.week_start
        order by sum(e.xp) desc, min(e.earned_at), e.user_id)::integer as rnk
    from public.throne_xp_events e
    join public.profiles p on p.id = e.user_id
    where e.week_start >= date '2026-10-12'
      and e.week_start < date_trunc('week', now() at time zone 'Europe/Istanbul')::date
    group by e.week_start, e.user_id
  )
  select r.week_start, r.rnk, case r.rnk when 1 then 3 when 2 then 2 when 3 then 1 else 0 end
  from ranked r
  where r.user_id = p_user and r.rnk <= 3
  order by r.week_start
$$;
revoke all on function private.eve_shard_weeks(uuid) from public, anon, authenticated;

create or replace function public.get_eve_status_v1()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  u uuid := auth.uid();
  w date := date_trunc('week', now() at time zone 'Europe/Istanbul')::date;
  total integer;
  weeks jsonb;
  is_admin boolean;
  current_rank integer;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select coalesce(sum(s.shards), 0)::integer,
         coalesce(jsonb_agg(jsonb_build_object('week_start', s.week_start, 'rank', s.rank, 'shards', s.shards)
           order by s.week_start), '[]'::jsonb)
    into total, weeks
  from private.eve_shard_weeks(u) s;
  is_admin := exists (select 1 from public.admin_users a where a.user_id = u and a.role = 'admin');
  if total >= 12 then
    insert into public.eve_companion_owners(user_id, source) values (u, 'throne_shards')
    on conflict (user_id) do nothing;
  end if;
  with t as (
    select e.user_id, row_number() over (order by sum(e.xp) desc, min(e.earned_at), e.user_id)::integer as rnk
    from public.throne_xp_events e join public.profiles p on p.id = e.user_id
    where e.week_start = w group by e.user_id
  )
  select t.rnk into current_rank from t where t.user_id = u;
  return jsonb_build_object(
    'shards', least(total, 12),
    'required', 12,
    'owned', is_admin or exists (select 1 from public.eve_companion_owners o where o.user_id = u),
    'admin', is_admin,
    'weeks', weeks,
    'per_rank', jsonb_build_array(3, 2, 1),
    'current_rank', coalesce(current_rank, 0),
    'week_resets_at', (w + 7)::timestamp at time zone 'Europe/Istanbul'
  );
end $$;
revoke all on function public.get_eve_status_v1() from public, anon;
grant execute on function public.get_eve_status_v1() to authenticated;
