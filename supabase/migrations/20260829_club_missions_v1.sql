-- Club Missions + Team Chest v1
-- Weekly cooperative club goals with personal contribution gates.
-- Rewards are Son Coin only and never grant match power.

create table if not exists public.club_weekly_reward_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  club_id uuid not null references public.clubs(id) on delete cascade,
  week_start date not null,
  tier integer not null check(tier between 1 and 3),
  reward_coin integer not null check(reward_coin>=0),
  claimed_at timestamptz not null default now(),
  primary key(user_id,week_start,tier)
);

create index if not exists club_weekly_reward_claims_club_idx
  on public.club_weekly_reward_claims(club_id,week_start,claimed_at desc);

alter table public.club_weekly_reward_claims enable row level security;
revoke all on public.club_weekly_reward_claims from anon,authenticated;

create or replace function public.get_club_weekly_missions_v1()
returns table(
  tier integer,
  target_points integer,
  reward_coin integer,
  min_contribution integer,
  club_points bigint,
  my_points bigint,
  claimed boolean,
  eligible boolean,
  week_start date,
  week_end date
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_club uuid;
  v_week_start_ts timestamptz;
  v_week_start date;
  v_club_points bigint:=0;
  v_my_points bigint:=0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select cm.club_id into v_club
  from public.club_members cm
  where cm.user_id=v_uid;

  if v_club is null then return; end if;

  v_week_start_ts:=(date_trunc('week',timezone('Europe/Istanbul',now())) at time zone 'Europe/Istanbul');
  v_week_start:=(timezone('Europe/Istanbul',v_week_start_ts))::date;

  select coalesce(sum(e.points),0)::bigint into v_club_points
  from public.club_point_events e
  where e.club_id=v_club and e.created_at>=v_week_start_ts;

  select coalesce(sum(e.points),0)::bigint into v_my_points
  from public.club_point_events e
  where e.club_id=v_club and e.user_id=v_uid and e.created_at>=v_week_start_ts;

  return query
  with defs(t,target,reward,minc) as (
    values
      (1,100,20,10),
      (2,300,35,25),
      (3,600,60,50)
  )
  select
    d.t,
    d.target,
    d.reward,
    d.minc,
    v_club_points,
    v_my_points,
    exists(
      select 1 from public.club_weekly_reward_claims c
      where c.user_id=v_uid and c.week_start=v_week_start and c.tier=d.t
    ),
    (
      v_club_points>=d.target
      and v_my_points>=d.minc
      and not exists(
        select 1 from public.club_weekly_reward_claims c
        where c.user_id=v_uid and c.week_start=v_week_start and c.tier=d.t
      )
    ),
    v_week_start,
    v_week_start+7
  from defs d
  order by d.t;
end
$$;

create or replace function public.claim_club_weekly_mission_v1(p_tier integer)
returns table(
  success boolean,
  tier integer,
  reward_coin integer,
  balance integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_club uuid;
  v_week_start_ts timestamptz;
  v_week_start date;
  v_target int;
  v_reward int;
  v_minc int;
  v_club_points bigint:=0;
  v_my_points bigint:=0;
  v_balance int:=0;
  v_inserted boolean:=false;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select cm.club_id into v_club
  from public.club_members cm
  where cm.user_id=v_uid
  for update;

  if v_club is null then raise exception 'club_required'; end if;

  select x.target,x.reward,x.minc into v_target,v_reward,v_minc
  from (values
    (1,100,20,10),
    (2,300,35,25),
    (3,600,60,50)
  ) x(t,target,reward,minc)
  where x.t=p_tier;

  if v_target is null then raise exception 'invalid_club_mission_tier'; end if;

  v_week_start_ts:=(date_trunc('week',timezone('Europe/Istanbul',now())) at time zone 'Europe/Istanbul');
  v_week_start:=(timezone('Europe/Istanbul',v_week_start_ts))::date;

  select coalesce(sum(e.points),0)::bigint into v_club_points
  from public.club_point_events e
  where e.club_id=v_club and e.created_at>=v_week_start_ts;

  select coalesce(sum(e.points),0)::bigint into v_my_points
  from public.club_point_events e
  where e.club_id=v_club and e.user_id=v_uid and e.created_at>=v_week_start_ts;

  if v_club_points<v_target then raise exception 'club_mission_locked'; end if;
  if v_my_points<v_minc then raise exception 'club_contribution_required'; end if;

  insert into public.club_weekly_reward_claims(user_id,club_id,week_start,tier,reward_coin)
  values(v_uid,v_club,v_week_start,p_tier,v_reward)
  on conflict on constraint club_weekly_reward_claims_pkey do nothing;

  if found then
    v_inserted:=true;

    update public.profiles
    set diamonds=coalesce(diamonds,0)+v_reward,updated_at=now()
    where id=v_uid
    returning diamonds into v_balance;

    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(
      v_uid,
      v_reward,
      'club_weekly_chest:tier'||p_tier::text||':'||v_week_start::text,
      null
    );
  else
    select p.diamonds into v_balance
    from public.profiles p
    where p.id=v_uid;
  end if;

  return query
  select
    v_inserted,
    p_tier,
    (case when v_inserted then v_reward else 0 end),
    coalesce(v_balance,0);
end
$$;

revoke all on function public.get_club_weekly_missions_v1() from public,anon;
revoke all on function public.claim_club_weekly_mission_v1(integer) from public,anon;
grant execute on function public.get_club_weekly_missions_v1() to authenticated;
grant execute on function public.claim_club_weekly_mission_v1(integer) to authenticated;
