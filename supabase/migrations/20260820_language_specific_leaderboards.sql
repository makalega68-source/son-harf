create or replace function public.get_language_leaderboard(
  p_language text,
  p_period text default 'total',
  p_limit integer default 50
)
returns table (
  user_id uuid,
  display_name text,
  avatar_url text,
  wins integer,
  losses integer,
  total_matches integer,
  win_rate integer,
  ranking_score integer
)
language sql
stable
security invoker
set search_path = public
as $$
with params as (
  select
    case when lower(coalesce(p_language,'tr')) = 'en' then 'en' else 'tr' end as lang,
    case lower(coalesce(p_period,'total'))
      when 'week' then date_trunc('week', now())
      when 'month' then date_trunc('month', now())
      else null::timestamptz
    end as since_at
), player_games as (
  select r.host_id as user_id,
         (r.winner_id = r.host_id and coalesce(r.winner_is_bot,false) = false) as won
  from public.game_rooms r, params p
  where r.status = 'finished'
    and r.language = p.lang
    and r.host_id is not null
    and (p.since_at is null or r.finished_at >= p.since_at)
  union all
  select r.guest_id as user_id,
         (r.winner_id = r.guest_id) as won
  from public.game_rooms r, params p
  where r.status = 'finished'
    and r.language = p.lang
    and r.guest_id is not null
    and (p.since_at is null or r.finished_at >= p.since_at)
), agg as (
  select pg.user_id,
         count(*) filter (where pg.won)::int as wins,
         count(*) filter (where not pg.won)::int as losses,
         count(*)::int as total_matches
  from player_games pg
  group by pg.user_id
), scored as (
  select a.user_id,
         a.wins,
         a.losses,
         a.total_matches,
         case when a.total_matches = 0 then 0 else round(a.wins * 100.0 / a.total_matches)::int end as win_rate,
         (a.wins * 100 + case when a.total_matches = 0 then 0 else round(a.wins * 100.0 / a.total_matches)::int end * 10 + least(a.total_matches,99))::int as ranking_score
  from agg a
)
select s.user_id, p.display_name, p.avatar_url, s.wins, s.losses, s.total_matches, s.win_rate, s.ranking_score
from scored s
join public.profiles p on p.id = s.user_id
order by s.wins desc, s.win_rate desc, s.total_matches desc, s.ranking_score desc, lower(p.display_name), s.user_id
limit greatest(1, least(coalesce(p_limit,50), 100));
$$;

grant execute on function public.get_language_leaderboard(text,text,integer) to authenticated;
grant execute on function public.get_language_leaderboard(text,text,integer) to anon;
