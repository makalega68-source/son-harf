-- Rival History v1
-- Aggregates Classic Son Harf + Word Arena opponent history for retention/social rematches.

create or replace function public.get_rival_history_v1(
  p_limit integer default 20
)
returns table(
  opponent_id uuid,
  display_name text,
  matches integer,
  wins integer,
  losses integer,
  draws integer,
  my_points integer,
  their_points integer,
  classic_matches integer,
  arena_matches integer,
  last_mode text,
  last_played_at timestamptz,
  is_friend boolean,
  presence_status text,
  can_challenge boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  return query
  with duels as (
    select
      case when g.host_id=v_uid then g.guest_id else g.host_id end rival_id,
      'classic'::text mode,
      (g.winner_id=v_uid) won,
      (g.winner_id is not null and g.winner_id<>v_uid) lost,
      (g.winner_id is null) drawn,
      case when g.host_id=v_uid then g.host_score else g.guest_score end mine,
      case when g.host_id=v_uid then g.guest_score else g.host_score end theirs,
      coalesce(g.finished_at,g.created_at) played_at
    from public.game_rooms g
    where g.status='finished'
      and coalesce(g.is_bot,false)=false
      and g.host_id is not null
      and g.guest_id is not null
      and (g.host_id=v_uid or g.guest_id=v_uid)

    union all

    select
      case when a.host_id=v_uid then a.guest_id else a.host_id end rival_id,
      'arena'::text mode,
      (a.winner_id=v_uid) won,
      (a.winner_id is not null and a.winner_id<>v_uid) lost,
      (a.winner_id is null) drawn,
      case when a.host_id=v_uid then a.host_score else a.guest_score end mine,
      case when a.host_id=v_uid then a.guest_score else a.host_score end theirs,
      coalesce(a.finished_at,a.ends_at,a.created_at) played_at
    from public.word_arena_rooms a
    where a.status='finished'
      and a.result_applied
      and (a.host_id=v_uid or a.guest_id=v_uid)
  ),
  agg as (
    select
      d.rival_id,
      count(*)::int total_matches,
      count(*) filter(where d.won)::int total_wins,
      count(*) filter(where d.lost)::int total_losses,
      count(*) filter(where d.drawn)::int total_draws,
      coalesce(sum(d.mine),0)::int total_my_points,
      coalesce(sum(d.theirs),0)::int total_their_points,
      count(*) filter(where d.mode='classic')::int classic_count,
      count(*) filter(where d.mode='arena')::int arena_count,
      (array_agg(d.mode order by d.played_at desc))[1]::text latest_mode,
      max(d.played_at) latest_played
    from duels d
    where d.rival_id is not null
    group by d.rival_id
  )
  select
    a.rival_id,
    p.display_name,
    a.total_matches,
    a.total_wins,
    a.total_losses,
    a.total_draws,
    a.total_my_points,
    a.total_their_points,
    a.classic_count,
    a.arena_count,
    a.latest_mode,
    a.latest_played,
    public.are_friends(v_uid,a.rival_id),
    coalesce(p.presence_status,'offline'),
    (
      public.are_friends(v_uid,a.rival_id)
      and coalesce(p.presence_status,'offline')='online'
      and not exists(
        select 1 from public.user_blocks b
        where (b.blocker_id=v_uid and b.blocked_id=a.rival_id)
           or (b.blocker_id=a.rival_id and b.blocked_id=v_uid)
      )
    )
  from agg a
  join public.profiles p on p.id=a.rival_id
  order by a.latest_played desc,a.total_matches desc
  limit greatest(1,least(coalesce(p_limit,20),50));
end
$$;

revoke all on function public.get_rival_history_v1(integer) from public,anon;
grant execute on function public.get_rival_history_v1(integer) to authenticated;
