-- Match History + Personal Records v1
-- Read-only retention surfaces built from real finalized PvP data.

create or replace function public.get_match_history_v1(
  p_limit integer default 30
)
returns table(
  match_id uuid,
  mode text,
  opponent_id uuid,
  display_name text,
  result text,
  my_score integer,
  their_score integer,
  rating_delta integer,
  language text,
  played_at timestamptz,
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
  with matches as (
    select
      g.id match_id,
      'classic'::text mode,
      case when g.host_id=v_uid then g.guest_id else g.host_id end opponent_id,
      case
        when g.winner_id is null then 'draw'
        when g.winner_id=v_uid then 'win'
        else 'loss'
      end result,
      case when g.host_id=v_uid then g.host_score else g.guest_score end my_score,
      case when g.host_id=v_uid then g.guest_score else g.host_score end their_score,
      case
        when g.winner_id is null then 0
        when g.winner_id=v_uid then 20
        else -15
      end rating_delta,
      g.language,
      coalesce(g.finished_at,g.created_at) played_at
    from public.game_rooms g
    where g.status='finished'
      and g.stats_applied
      and coalesce(g.is_bot,false)=false
      and g.host_id is not null
      and g.guest_id is not null
      and (g.host_id=v_uid or g.guest_id=v_uid)

    union all

    select
      a.id,
      'arena'::text,
      case when a.host_id=v_uid then a.guest_id else a.host_id end,
      case
        when a.winner_id is null then 'draw'
        when a.winner_id=v_uid then 'win'
        else 'loss'
      end,
      case when a.host_id=v_uid then a.host_score else a.guest_score end,
      case when a.host_id=v_uid then a.guest_score else a.host_score end,
      case
        when a.winner_id is null then 0
        when a.winner_id=v_uid then 18
        else -12
      end,
      a.language,
      coalesce(a.finished_at,a.ends_at,a.created_at)
    from public.word_arena_rooms a
    where a.status='finished'
      and a.result_applied
      and (a.host_id=v_uid or a.guest_id=v_uid)
  )
  select
    m.match_id,
    m.mode,
    m.opponent_id,
    p.display_name,
    m.result,
    m.my_score,
    m.their_score,
    m.rating_delta,
    m.language,
    m.played_at,
    public.are_friends(v_uid,m.opponent_id),
    coalesce(p.presence_status,'offline'),
    (
      public.are_friends(v_uid,m.opponent_id)
      and coalesce(p.presence_status,'offline')='online'
      and not exists(
        select 1 from public.user_blocks b
        where (b.blocker_id=v_uid and b.blocked_id=m.opponent_id)
           or (b.blocker_id=m.opponent_id and b.blocked_id=v_uid)
      )
    )
  from matches m
  join public.profiles p on p.id=m.opponent_id
  order by m.played_at desc,m.match_id
  limit greatest(1,least(coalesce(p_limit,30),100));
end
$$;

create or replace function public.get_personal_records_v1()
returns table(
  real_pvp_matches integer,
  real_pvp_wins integer,
  real_pvp_losses integer,
  real_pvp_draws integer,
  classic_matches integer,
  arena_matches integer,
  current_rating integer,
  best_streak integer,
  valid_words integer,
  longest_word text,
  longest_word_length integer,
  best_classic_score integer,
  best_arena_score integer,
  biggest_win_margin integer,
  favorite_rival_name text,
  favorite_rival_matches integer
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
      case when g.host_id=v_uid then g.guest_id else g.host_id end opponent_id,
      'classic'::text mode,
      case
        when g.winner_id is null then 'draw'
        when g.winner_id=v_uid then 'win'
        else 'loss'
      end result,
      case when g.host_id=v_uid then g.host_score else g.guest_score end my_score,
      case when g.host_id=v_uid then g.guest_score else g.host_score end their_score
    from public.game_rooms g
    where g.status='finished'
      and g.stats_applied
      and coalesce(g.is_bot,false)=false
      and g.host_id is not null
      and g.guest_id is not null
      and (g.host_id=v_uid or g.guest_id=v_uid)

    union all

    select
      case when a.host_id=v_uid then a.guest_id else a.host_id end,
      'arena'::text,
      case
        when a.winner_id is null then 'draw'
        when a.winner_id=v_uid then 'win'
        else 'loss'
      end,
      case when a.host_id=v_uid then a.host_score else a.guest_score end,
      case when a.host_id=v_uid then a.guest_score else a.host_score end
    from public.word_arena_rooms a
    where a.status='finished'
      and a.result_applied
      and (a.host_id=v_uid or a.guest_id=v_uid)
  ),
  word_candidates as (
    select gw.word, char_length(gw.word)::int len, gw.created_at
    from public.game_words gw
    where gw.player_id=v_uid

    union all

    select aw.word, char_length(aw.word)::int len, aw.created_at
    from public.word_arena_words aw
    where aw.user_id=v_uid
  ),
  longest as (
    select wc.word,wc.len
    from word_candidates wc
    order by wc.len desc,wc.created_at desc,wc.word
    limit 1
  ),
  rival_counts as (
    select d.opponent_id,count(*)::int matches
    from duels d
    where d.opponent_id is not null
    group by d.opponent_id
    order by count(*) desc,d.opponent_id
    limit 1
  )
  select
    count(*)::int,
    count(*) filter(where d.result='win')::int,
    count(*) filter(where d.result='loss')::int,
    count(*) filter(where d.result='draw')::int,
    count(*) filter(where d.mode='classic')::int,
    count(*) filter(where d.mode='arena')::int,
    p.rating,
    p.best_streak,
    p.valid_words,
    coalesce((select l.word from longest l),''),
    coalesce((select l.len from longest l),0)::int,
    coalesce(max(d.my_score) filter(where d.mode='classic'),0)::int,
    coalesce(max(d.my_score) filter(where d.mode='arena'),0)::int,
    coalesce(max(d.my_score-d.their_score) filter(where d.result='win'),0)::int,
    coalesce((
      select rp.display_name
      from rival_counts rc
      join public.profiles rp on rp.id=rc.opponent_id
      limit 1
    ),''),
    coalesce((select rc.matches from rival_counts rc),0)::int
  from public.profiles p
  left join duels d on true
  where p.id=v_uid
  group by p.rating,p.best_streak,p.valid_words;
end
$$;

revoke all on function public.get_match_history_v1(integer) from public,anon;
revoke all on function public.get_personal_records_v1() from public,anon;
grant execute on function public.get_match_history_v1(integer) to authenticated;
grant execute on function public.get_personal_records_v1() to authenticated;
