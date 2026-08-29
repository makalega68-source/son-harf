-- Weekly Tournament Fair-Play + History v2
-- Zero-match entrants do not rank and cannot claim rewards.
-- Claiming skips already-claimed tournaments and finds the latest eligible unclaimed reward.

create or replace function public.get_weekly_tournament_v1()
returns table(
  tournament_id uuid,
  name text,
  week_start date,
  starts_at timestamptz,
  ends_at timestamptz,
  joined boolean,
  my_points bigint,
  my_wins bigint,
  my_losses bigint,
  my_rank bigint,
  player_count bigint
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_tid uuid;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  v_tid:=public.ensure_weekly_tournament_v1();

  return query
  with score as (
    select
      e.user_id,
      coalesce(sum(m.points),0)::bigint pts,
      count(*) filter(where m.won)::bigint wins,
      count(*) filter(where not m.won)::bigint losses,
      count(m.id)::bigint matches
    from public.weekly_tournament_entries e
    left join public.weekly_tournament_match_events m
      on m.tournament_id=e.tournament_id
     and m.user_id=e.user_id
    where e.tournament_id=v_tid
    group by e.user_id
  ),
  ranked as (
    select
      s.*,
      row_number() over(
        order by s.pts desc,s.wins desc,s.matches desc,s.user_id
      )::bigint rnk
    from score s
    where s.matches>0
  )
  select
    t.id,
    t.name,
    t.week_start,
    t.starts_at,
    t.ends_at,
    exists(
      select 1
      from public.weekly_tournament_entries e
      where e.tournament_id=t.id and e.user_id=v_uid
    ),
    coalesce((select s.pts from score s where s.user_id=v_uid),0)::bigint,
    coalesce((select s.wins from score s where s.user_id=v_uid),0)::bigint,
    coalesce((select s.losses from score s where s.user_id=v_uid),0)::bigint,
    coalesce((select r.rnk from ranked r where r.user_id=v_uid),0)::bigint,
    (
      select count(*)::bigint
      from public.weekly_tournament_entries e
      where e.tournament_id=t.id
    )
  from public.weekly_tournaments t
  where t.id=v_tid;
end
$$;

create or replace function public.get_weekly_tournament_leaderboard_v1(
  p_limit integer default 50
)
returns table(
  user_id uuid,
  display_name text,
  rating integer,
  league_name text,
  points bigint,
  wins bigint,
  losses bigint,
  rank bigint
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_tid uuid;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  v_tid:=public.ensure_weekly_tournament_v1();

  return query
  with score as (
    select
      e.user_id,
      coalesce(sum(m.points),0)::bigint pts,
      count(*) filter(where m.won)::bigint wins,
      count(*) filter(where not m.won)::bigint losses,
      count(m.id)::bigint matches
    from public.weekly_tournament_entries e
    left join public.weekly_tournament_match_events m
      on m.tournament_id=e.tournament_id
     and m.user_id=e.user_id
    where e.tournament_id=v_tid
    group by e.user_id
  ),
  ranked as (
    select
      s.*,
      row_number() over(
        order by s.pts desc,s.wins desc,s.matches desc,s.user_id
      )::bigint rnk
    from score s
    where s.matches>0
  )
  select
    p.id,
    p.display_name,
    p.rating,
    public.league_for_rating_v1(p.rating),
    r.pts,
    r.wins,
    r.losses,
    r.rnk
  from ranked r
  join public.profiles p on p.id=r.user_id
  order by r.rnk
  limit greatest(1,least(coalesce(p_limit,50),100));
end
$$;

create or replace function public.claim_previous_weekly_tournament_reward_v1()
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_tid uuid;
  v_rank int;
  v_reward int;
  v_balance int;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select t.id into v_tid
  from public.weekly_tournaments t
  where t.ends_at<=now()
    and exists(
      select 1
      from public.weekly_tournament_entries e
      where e.tournament_id=t.id
        and e.user_id=v_uid
    )
    and exists(
      select 1
      from public.weekly_tournament_match_events m
      where m.tournament_id=t.id
        and m.user_id=v_uid
    )
    and not exists(
      select 1
      from public.weekly_tournament_reward_claims c
      where c.tournament_id=t.id
        and c.user_id=v_uid
    )
  order by t.ends_at desc
  limit 1;

  if v_tid is null then
    raise exception 'no_eligible_tournament_reward';
  end if;

  with score as (
    select
      e.user_id,
      coalesce(sum(m.points),0)::bigint pts,
      count(*) filter(where m.won)::bigint wins,
      count(m.id)::bigint matches
    from public.weekly_tournament_entries e
    join public.weekly_tournament_match_events m
      on m.tournament_id=e.tournament_id
     and m.user_id=e.user_id
    where e.tournament_id=v_tid
    group by e.user_id
  ),
  ranked as (
    select
      s.user_id,
      row_number() over(
        order by s.pts desc,s.wins desc,s.matches desc,s.user_id
      )::int rnk
    from score s
    where s.matches>0
  )
  select r.rnk into v_rank
  from ranked r
  where r.user_id=v_uid;

  if v_rank is null then
    raise exception 'no_eligible_tournament_reward';
  end if;

  v_reward:=case
    when v_rank=1 then 1000
    when v_rank=2 then 600
    when v_rank=3 then 400
    when v_rank between 4 and 10 then 150
    else 50
  end;

  insert into public.weekly_tournament_reward_claims(
    tournament_id,user_id,final_rank,reward_coins
  )
  values(v_tid,v_uid,v_rank,v_reward)
  on conflict(tournament_id,user_id) do nothing;

  if not found then
    raise exception 'reward_already_claimed';
  end if;

  update public.profiles
  set diamonds=coalesce(diamonds,0)+v_reward,
      updated_at=now()
  where id=v_uid
  returning diamonds into v_balance;

  insert into public.diamond_ledger(user_id,delta,reason)
  values(v_uid,v_reward,'weekly_tournament_reward:'||v_tid::text);

  return jsonb_build_object(
    'success',true,
    'rank',v_rank,
    'reward_coins',v_reward,
    'balance',v_balance
  );
end
$$;

create or replace function public.get_weekly_tournament_history_v1(
  p_limit integer default 12
)
returns table(
  tournament_id uuid,
  name text,
  week_start date,
  starts_at timestamptz,
  ends_at timestamptz,
  points bigint,
  wins bigint,
  losses bigint,
  matches bigint,
  final_rank bigint,
  participant_count bigint,
  reward_coins integer,
  reward_claimed boolean,
  reward_eligible boolean
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
  with my_tournaments as (
    select t.*
    from public.weekly_tournaments t
    join public.weekly_tournament_entries e
      on e.tournament_id=t.id
     and e.user_id=v_uid
    where t.ends_at<=now()
    order by t.ends_at desc
    limit greatest(1,least(coalesce(p_limit,12),52))
  ),
  all_scores as (
    select
      e.tournament_id,
      e.user_id,
      coalesce(sum(m.points),0)::bigint pts,
      count(*) filter(where m.won)::bigint wins,
      count(*) filter(where not m.won)::bigint losses,
      count(m.id)::bigint matches
    from public.weekly_tournament_entries e
    left join public.weekly_tournament_match_events m
      on m.tournament_id=e.tournament_id
     and m.user_id=e.user_id
    where e.tournament_id in (select mt.id from my_tournaments mt)
    group by e.tournament_id,e.user_id
  ),
  ranked as (
    select
      s.*,
      row_number() over(
        partition by s.tournament_id
        order by s.pts desc,s.wins desc,s.matches desc,s.user_id
      )::bigint rnk
    from all_scores s
    where s.matches>0
  )
  select
    mt.id,
    mt.name,
    mt.week_start,
    mt.starts_at,
    mt.ends_at,
    coalesce(ms.pts,0)::bigint,
    coalesce(ms.wins,0)::bigint,
    coalesce(ms.losses,0)::bigint,
    coalesce(ms.matches,0)::bigint,
    coalesce(r.rnk,0)::bigint,
    (
      select count(*)::bigint
      from all_scores x
      where x.tournament_id=mt.id
        and x.matches>0
    ),
    coalesce(
      c.reward_coins,
      case
        when r.rnk=1 then 1000
        when r.rnk=2 then 600
        when r.rnk=3 then 400
        when r.rnk between 4 and 10 then 150
        when r.rnk>10 then 50
        else 0
      end
    )::int,
    c.user_id is not null,
    (
      coalesce(ms.matches,0)>0
      and c.user_id is null
    )
  from my_tournaments mt
  left join all_scores ms
    on ms.tournament_id=mt.id
   and ms.user_id=v_uid
  left join ranked r
    on r.tournament_id=mt.id
   and r.user_id=v_uid
  left join public.weekly_tournament_reward_claims c
    on c.tournament_id=mt.id
   and c.user_id=v_uid
  order by mt.ends_at desc;
end
$$;

revoke all on function public.get_weekly_tournament_v1() from public,anon;
revoke all on function public.get_weekly_tournament_leaderboard_v1(integer) from public,anon;
revoke all on function public.claim_previous_weekly_tournament_reward_v1() from public,anon;
revoke all on function public.get_weekly_tournament_history_v1(integer) from public,anon;

grant execute on function public.get_weekly_tournament_v1() to authenticated;
grant execute on function public.get_weekly_tournament_leaderboard_v1(integer) to authenticated;
grant execute on function public.claim_previous_weekly_tournament_reward_v1() to authenticated;
grant execute on function public.get_weekly_tournament_history_v1(integer) to authenticated;


-- Cover the history/reward access paths introduced by this migration.
create index if not exists weekly_tournament_entries_user_idx
  on public.weekly_tournament_entries(user_id,tournament_id);

create index if not exists weekly_tournament_match_events_room_idx
  on public.weekly_tournament_match_events(room_id)
  where room_id is not null;

create index if not exists weekly_tournament_match_events_user_idx
  on public.weekly_tournament_match_events(user_id,tournament_id,created_at desc);

create index if not exists weekly_tournament_reward_claims_user_idx
  on public.weekly_tournament_reward_claims(user_id,tournament_id,claimed_at desc);
