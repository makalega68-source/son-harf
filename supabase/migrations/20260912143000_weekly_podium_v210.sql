-- SON HARF — Haftanın Zirvesi Podyum Modülü v2.1.0
-- Current-schema compatibility: the legacy package expected match_history.xp_gained.
-- Production stores finished classic matches in game_rooms/game_words instead, so weekly RP
-- reconstructs the exact XP formula used by claim_match_result_v10:
--   win 120 / loss 35 + own words * 3 + own rounds * 5.

create or replace view public.weekly_rp as
with participants as (
  select
    r.id as room_id,
    r.host_id as user_id,
    r.winner_id,
    coalesce(r.host_rounds, 0) as rounds
  from public.game_rooms r
  where r.status = 'finished'
    and r.host_id is not null
    and coalesce(r.finished_at, r.created_at) >= date_trunc('week', now())

  union all

  select
    r.id as room_id,
    r.guest_id as user_id,
    r.winner_id,
    coalesce(r.guest_rounds, 0) as rounds
  from public.game_rooms r
  where r.status = 'finished'
    and r.guest_id is not null
    and coalesce(r.finished_at, r.created_at) >= date_trunc('week', now())
),
word_counts as (
  select
    gw.room_id,
    gw.player_id as user_id,
    count(*)::integer as words
  from public.game_words gw
  join participants p
    on p.room_id = gw.room_id
   and p.user_id = gw.player_id
  group by gw.room_id, gw.player_id
)
select
  p.user_id,
  sum(
    (case when p.winner_id = p.user_id then 120 else 35 end)
    + coalesce(w.words, 0) * 3
    + p.rounds * 5
  )::bigint as rp,
  count(*)::bigint as matches
from participants p
left join word_counts w
  on w.room_id = p.room_id
 and w.user_id = p.user_id
group by p.user_id;

revoke all on public.weekly_rp from public, anon, authenticated;
grant select on public.weekly_rp to service_role;

create or replace function public.weekly_top(p_limit integer default 20)
returns table(
  user_id text,
  username text,
  rp integer,
  avatar_url text
)
language plpgsql
stable
security definer
set search_path to 'pg_catalog', 'public', 'pg_temp'
as $$
declare
  v_limit integer := least(greatest(coalesce(p_limit, 20), 1), 100);
begin
  if auth.uid() is null then
    raise exception 'not_authenticated';
  end if;

  return query
  select
    wr.user_id::text,
    coalesce(nullif(trim(p.display_name), ''), 'Oyuncu')::text as username,
    least(wr.rp, 2147483647)::integer as rp,
    nullif(trim(p.avatar_url), '')::text as avatar_url
  from public.weekly_rp wr
  join public.profiles p on p.id = wr.user_id
  order by wr.rp desc, wr.user_id
  limit v_limit;
end
$$;

revoke all on function public.weekly_top(integer) from public, anon;
grant execute on function public.weekly_top(integer) to authenticated, service_role;

create or replace function public.my_weekly_rp()
returns integer
language plpgsql
stable
security definer
set search_path to 'pg_catalog', 'public', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_rp bigint;
begin
  if v_uid is null then
    raise exception 'not_authenticated';
  end if;

  select wr.rp into v_rp
  from public.weekly_rp wr
  where wr.user_id = v_uid;

  return least(coalesce(v_rp, 0), 2147483647)::integer;
end
$$;

revoke all on function public.my_weekly_rp() from public, anon;
grant execute on function public.my_weekly_rp() to authenticated, service_role;
