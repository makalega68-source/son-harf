-- Competitive Season History + Fair-Play Rewards v1
-- Separate from paid Season Pass rewards.
-- Requires at least 5 real competitive PvP matches for end-of-season rewards.
-- Rank honors/rewards require at least 5 eligible players.

create table if not exists public.competitive_season_reward_claims (
  season_id text not null references public.competitive_seasons(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  final_rank integer not null default 0 check(final_rank>=0),
  final_rating integer not null check(final_rating>=100),
  reward_coins integer not null check(reward_coins>=0),
  claimed_at timestamptz not null default now(),
  primary key(season_id,user_id)
);

alter table public.competitive_season_reward_claims enable row level security;
revoke all on public.competitive_season_reward_claims from anon,authenticated;

create index if not exists competitive_season_reward_claims_user_idx
  on public.competitive_season_reward_claims(user_id,claimed_at desc);

create index if not exists season_player_stats_user_history_idx
  on public.season_player_stats(user_id,season_id);

create schema if not exists private;
revoke all on schema private from public,anon,authenticated;
grant usage on schema private to authenticated;

create or replace function private.competitive_season_reward_v1(
  p_rank integer,
  p_final_rating integer,
  p_matches integer,
  p_eligible_players integer
)
returns integer
language sql
immutable
set search_path=''
as $$
  select case
    when coalesce(p_matches,0)<5 then 0
    when coalesce(p_eligible_players,0)>=5 and p_rank=1 then 500
    when coalesce(p_eligible_players,0)>=5 and p_rank between 2 and 3 then 300
    when coalesce(p_eligible_players,0)>=5 and p_rank between 4 and 10 then 150
    when coalesce(p_final_rating,1000)>=1800 then 125
    when coalesce(p_final_rating,1000)>=1600 then 100
    when coalesce(p_final_rating,1000)>=1400 then 80
    when coalesce(p_final_rating,1000)>=1250 then 60
    when coalesce(p_final_rating,1000)>=1100 then 50
    else 40
  end
$$;

create or replace function private.settle_competitive_seasons_v2(
  p_reference timestamptz default now()
)
returns integer
language plpgsql
security definer
set search_path=''
as $$
declare
  v_count int:=0;
begin
  perform pg_advisory_xact_lock(88112602);

  with ended as (
    select s.id,s.name_tr,s.name_en
    from public.competitive_seasons s
    where s.ends_at<=p_reference
      and s.settled_at is null
  ),
  ranked as (
    select
      ps.season_id,
      ps.user_id,
      ps.rating,
      row_number() over(
        partition by ps.season_id
        order by ps.rating desc,ps.wins desc,ps.matches desc,ps.user_id
      )::int rnk,
      count(*) over(partition by ps.season_id)::int eligible_count
    from public.season_player_stats ps
    join ended e on e.id=ps.season_id
    where ps.matches>=5
  ),
  honors as (
    select
      r.season_id,
      r.user_id,
      case
        when r.rnk=1 then 'CHAMPION'
        when r.rnk between 2 and 3 then 'PODIUM'
        when r.rnk between 4 and 10 then 'TOP10'
        else null
      end honor_code,
      case
        when r.rnk=1 then e.name_tr||' Şampiyonu'
        when r.rnk between 2 and 3 then e.name_tr||' Podyumu'
        when r.rnk between 4 and 10 then e.name_tr||' İlk 10'
        else null
      end label_tr,
      case
        when r.rnk=1 then e.name_en||' Champion'
        when r.rnk between 2 and 3 then e.name_en||' Podium'
        when r.rnk between 4 and 10 then e.name_en||' Top 10'
        else null
      end label_en,
      r.rating,
      r.rnk
    from ranked r
    join ended e on e.id=r.season_id
    where r.eligible_count>=5
      and r.rnk<=10
  )
  insert into public.season_honors(
    season_id,user_id,honor_code,label_tr,label_en,final_rating,rank
  )
  select
    h.season_id,h.user_id,h.honor_code,h.label_tr,h.label_en,h.rating,h.rnk
  from honors h
  where h.honor_code is not null
  on conflict do nothing;

  update public.competitive_seasons s
  set status='ended',
      settled_at=coalesce(s.settled_at,p_reference)
  where s.ends_at<=p_reference
    and s.settled_at is null;

  get diagnostics v_count=row_count;
  return v_count;
end
$$;

create or replace function public.ensure_competitive_season_v1(
  p_reference timestamptz default now()
)
returns text
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_id text;
  v_local timestamp;
  v_start timestamptz;
  v_end timestamptz;
begin
  perform private.settle_competitive_seasons_v2(p_reference);

  select id into v_id
  from public.competitive_seasons
  where p_reference>=starts_at and p_reference<ends_at
  order by starts_at desc
  limit 1;

  if v_id is not null then return v_id; end if;

  v_local:=p_reference at time zone 'Europe/Istanbul';
  v_start:=make_timestamptz(
    extract(year from v_local)::int,
    extract(month from v_local)::int,
    1,0,0,0,'Europe/Istanbul'
  );
  v_end:=v_start+interval '1 month';
  v_id:=to_char(v_local,'YYYY-MM');

  insert into public.competitive_seasons(
    id,name_tr,name_en,starts_at,ends_at,status
  )
  values(
    v_id,
    public.competitive_season_name_tr_v1(v_start),
    to_char(v_start at time zone 'Europe/Istanbul','FMMonth YYYY') || ' Season',
    v_start,
    v_end,
    'active'
  )
  on conflict(id) do nothing;

  return v_id;
end
$$;

create or replace function private.get_competitive_season_history_v1(
  p_limit integer default 12
)
returns table(
  season_id text,
  name_tr text,
  name_en text,
  starts_at timestamptz,
  ends_at timestamptz,
  final_rating integer,
  peak_rating integer,
  league_name text,
  final_rank bigint,
  eligible_player_count bigint,
  matches integer,
  wins integer,
  losses integer,
  draws integer,
  valid_words integer,
  honor_code text,
  honor_label_tr text,
  honor_label_en text,
  reward_coins integer,
  reward_claimed boolean,
  reward_eligible boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.settle_competitive_seasons_v2(clock_timestamp());

  return query
  with my_seasons as (
    select
      s.*,
      ps.rating,ps.peak_rating,ps.matches,ps.wins,ps.losses,ps.draws,ps.valid_words
    from public.competitive_seasons s
    join public.season_player_stats ps
      on ps.season_id=s.id
     and ps.user_id=v_uid
    where s.status='ended'
      and s.ends_at<=clock_timestamp()
    order by s.ends_at desc
    limit greatest(1,least(coalesce(p_limit,12),36))
  ),
  ranked as (
    select
      ps.season_id,
      ps.user_id,
      row_number() over(
        partition by ps.season_id
        order by ps.rating desc,ps.wins desc,ps.matches desc,ps.user_id
      )::bigint rnk,
      count(*) over(partition by ps.season_id)::bigint eligible_count
    from public.season_player_stats ps
    where ps.matches>=5
      and ps.season_id in (select ms.id from my_seasons ms)
  )
  select
    ms.id,
    ms.name_tr,
    ms.name_en,
    ms.starts_at,
    ms.ends_at,
    ms.rating,
    ms.peak_rating,
    public.league_for_rating_v1(ms.rating),
    coalesce(r.rnk,0)::bigint,
    coalesce(
      r.eligible_count,
      (
        select count(*)::bigint
        from public.season_player_stats ps2
        where ps2.season_id=ms.id and ps2.matches>=5
      ),
      0
    )::bigint,
    ms.matches,
    ms.wins,
    ms.losses,
    ms.draws,
    ms.valid_words,
    h.honor_code,
    h.label_tr,
    h.label_en,
    coalesce(
      c.reward_coins,
      private.competitive_season_reward_v1(
        coalesce(r.rnk,0)::int,
        ms.rating,
        ms.matches,
        coalesce(r.eligible_count,0)::int
      )
    )::int,
    c.user_id is not null,
    (ms.matches>=5 and c.user_id is null)
  from my_seasons ms
  left join ranked r
    on r.season_id=ms.id
   and r.user_id=v_uid
  left join lateral (
    select sh.honor_code,sh.label_tr,sh.label_en
    from public.season_honors sh
    where sh.season_id=ms.id
      and sh.user_id=v_uid
    order by
      case sh.honor_code when 'CHAMPION' then 1 when 'PODIUM' then 2 else 3 end,
      sh.created_at
    limit 1
  ) h on true
  left join public.competitive_season_reward_claims c
    on c.season_id=ms.id
   and c.user_id=v_uid
  order by ms.ends_at desc;
end
$$;

create or replace function private.claim_competitive_season_reward_v1(
  p_season_id text
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  s public.competitive_seasons;
  ps public.season_player_stats;
  v_rank int:=0;
  v_count int:=0;
  v_reward int:=0;
  v_balance int:=0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if nullif(trim(coalesce(p_season_id,'')),'') is null then
    raise exception 'invalid_season';
  end if;

  perform private.settle_competitive_seasons_v2(clock_timestamp());

  select cs.* into s
  from public.competitive_seasons cs
  where cs.id=p_season_id;

  if s.id is null then raise exception 'season_not_found'; end if;
  if s.status<>'ended' or s.ends_at>clock_timestamp() then
    raise exception 'season_not_ended';
  end if;

  select sp.* into ps
  from public.season_player_stats sp
  where sp.season_id=s.id and sp.user_id=v_uid;

  if ps.user_id is null or ps.matches<5 then
    raise exception 'season_reward_not_eligible';
  end if;

  with ranked as (
    select
      x.user_id,
      row_number() over(
        order by x.rating desc,x.wins desc,x.matches desc,x.user_id
      )::int rnk,
      count(*) over()::int cnt
    from public.season_player_stats x
    where x.season_id=s.id
      and x.matches>=5
  )
  select r.rnk,r.cnt into v_rank,v_count
  from ranked r
  where r.user_id=v_uid;

  v_reward:=private.competitive_season_reward_v1(
    v_rank,ps.rating,ps.matches,v_count
  );

  insert into public.competitive_season_reward_claims(
    season_id,user_id,final_rank,final_rating,reward_coins
  )
  values(s.id,v_uid,v_rank,ps.rating,v_reward)
  on conflict(season_id,user_id) do nothing;

  if not found then
    raise exception 'season_reward_already_claimed';
  end if;

  update public.profiles p
  set diamonds=coalesce(p.diamonds,0)+v_reward,
      updated_at=clock_timestamp()
  where p.id=v_uid
  returning p.diamonds into v_balance;

  insert into public.diamond_ledger(user_id,delta,reason)
  values(v_uid,v_reward,'competitive_season_reward:'||s.id);

  perform public.sync_achievement_unlocks_v1(v_uid);

  return jsonb_build_object(
    'success',true,
    'season_id',s.id,
    'rank',v_rank,
    'eligible_players',v_count,
    'reward_coins',v_reward,
    'balance',v_balance
  );
end
$$;

create or replace function public.get_competitive_season_history_v1(
  p_limit integer default 12
)
returns table(
  season_id text,
  name_tr text,
  name_en text,
  starts_at timestamptz,
  ends_at timestamptz,
  final_rating integer,
  peak_rating integer,
  league_name text,
  final_rank bigint,
  eligible_player_count bigint,
  matches integer,
  wins integer,
  losses integer,
  draws integer,
  valid_words integer,
  honor_code text,
  honor_label_tr text,
  honor_label_en text,
  reward_coins integer,
  reward_claimed boolean,
  reward_eligible boolean
)
language sql
security invoker
set search_path=''
as $$
  select * from private.get_competitive_season_history_v1(p_limit);
$$;

create or replace function public.claim_competitive_season_reward_v1(
  p_season_id text
)
returns jsonb
language sql
security invoker
set search_path=''
as $$
  select private.claim_competitive_season_reward_v1(p_season_id);
$$;

revoke all on function public.get_competitive_season_history_v1(integer)
  from public,anon;
revoke all on function public.claim_competitive_season_reward_v1(text)
  from public,anon;

grant execute on function public.get_competitive_season_history_v1(integer)
  to authenticated;
grant execute on function public.claim_competitive_season_reward_v1(text)
  to authenticated;

revoke all on function private.competitive_season_reward_v1(integer,integer,integer,integer)
  from public,anon,authenticated;
revoke all on function private.settle_competitive_seasons_v2(timestamptz)
  from public,anon,authenticated;
revoke all on function private.get_competitive_season_history_v1(integer)
  from public,anon,authenticated;
revoke all on function private.claim_competitive_season_reward_v1(text)
  from public,anon,authenticated;

grant execute on function private.get_competitive_season_history_v1(integer)
  to authenticated;
grant execute on function private.claim_competitive_season_reward_v1(text)
  to authenticated;
