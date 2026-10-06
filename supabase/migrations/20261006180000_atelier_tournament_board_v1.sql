-- Workshop lobby leaderboard: the top 100 of the live tournament, or of the most recent one while
-- the next has not started. Tournaments run every 2 hours, so the board changes every 2 hours.
create or replace function public.get_atelier_tournament_board_v1()
returns jsonb
language plpgsql
stable
security definer
set search_path to ''
as $$
declare
  u uuid := auth.uid();
  e timestamptz;
  rows jsonb;
  me jsonb;
  total integer;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select max(event_start) into e from public.atelier_tournament_runs where finished_at is not null;
  if e is null then
    return jsonb_build_object('server_time', now(), 'event_start', null, 'live', false,
      'next_start', private.atelier_event_next(now()), 'rows', '[]'::jsonb, 'me', null, 'total', 0);
  end if;
  with ranked as (
    select row_number() over (order by count(*) desc, sum(r.score) desc, max(r.finished_at), r.user_id)::int as rank,
           r.user_id, coalesce(max(p.display_name), 'Oyuncu') as name,
           sum(r.score)::int as score, count(*)::int as stages
    from public.atelier_tournament_runs r
    join public.profiles p on p.id = r.user_id
    where r.event_start = e and r.finished_at is not null
    group by r.user_id
  )
  select
    coalesce(jsonb_agg(jsonb_build_object('rank', rank, 'user_id', user_id, 'name', name, 'score', score,
      'stages', stages, 'me', user_id = u) order by rank) filter (where rank <= 100), '[]'::jsonb),
    (select jsonb_build_object('rank', x.rank, 'score', x.score, 'stages', x.stages) from ranked x where x.user_id = u),
    count(*)::int
  into rows, me, total
  from ranked;
  return jsonb_build_object('server_time', now(), 'event_start', e,
    'live', e = private.atelier_event_start(now()) and now() < e + interval '30 minutes',
    'next_start', private.atelier_event_next(now()), 'rows', rows, 'me', me, 'total', total);
end $$;

revoke all on function public.get_atelier_tournament_board_v1() from public;
grant execute on function public.get_atelier_tournament_board_v1() to authenticated;
