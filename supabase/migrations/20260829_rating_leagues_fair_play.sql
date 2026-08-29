-- Rating leagues + fair-play cleanup.
-- Rating is the single source for competitive league identity.

update public.profiles
set vip_xp_bonus=0
where coalesce(vip_xp_bonus,0)<>0;

create or replace function public.get_rating_leaderboard_v1(
  p_language text,
  p_period text default 'total',
  p_limit integer default 50
)
returns table(
  user_id uuid,
  display_name text,
  wins integer,
  losses integer,
  matches integer,
  win_rate numeric,
  rating integer,
  league_name text
)
language sql
stable
security definer
set search_path=public,pg_temp
as $$
with params as (
  select
    case when lower(trim(coalesce(p_language,'tr')))='en' then 'en' else 'tr' end as lang,
    lower(trim(coalesce(p_period,'total'))) as period,
    greatest(1,least(coalesce(p_limit,50),100)) as lim
),
eligible as (
  select r.*
  from public.game_rooms r, params p
  where r.status='finished'
    and lower(coalesce(r.language,'tr'))=p.lang
    and r.finished_at is not null
    and not coalesce(r.is_bot,false)
    and (
      p.period='total'
      or (p.period='week' and r.finished_at>=date_trunc('week',now()))
      or (p.period='month' and r.finished_at>=date_trunc('month',now()))
    )
),
appearances as (
  select host_id as user_id,
         case when winner_id=host_id then 1 else 0 end as win,
         case when winner_id=guest_id then 1 else 0 end as loss
  from eligible
  where host_id is not null
  union all
  select guest_id as user_id,
         case when winner_id=guest_id then 1 else 0 end as win,
         case when winner_id=host_id then 1 else 0 end as loss
  from eligible
  where guest_id is not null
),
agg as (
  select user_id,
         sum(win)::int wins,
         sum(loss)::int losses,
         count(*)::int matches
  from appearances
  group by user_id
)
select
  a.user_id,
  p.display_name,
  a.wins,
  a.losses,
  a.matches,
  round((a.wins::numeric/greatest(a.matches,1)::numeric)*100,1) as win_rate,
  coalesce(p.rating,1000)::int as rating,
  public.league_for_rating_v1(p.rating) as league_name
from agg a
join public.profiles p on p.id=a.user_id
order by coalesce(p.rating,1000) desc,
         a.wins desc,
         win_rate desc,
         a.matches desc,
         lower(p.display_name),
         a.user_id
limit (select lim from params)
$$;

revoke all on function public.get_rating_leaderboard_v1(text,text,integer) from public,anon;
grant execute on function public.get_rating_leaderboard_v1(text,text,integer) to authenticated;

create or replace function public.get_growth_dashboard_v1()
returns table(
  display_name text,
  xp integer,
  level integer,
  level_progress integer,
  level_target integer,
  current_win_streak integer,
  best_streak integer,
  total_matches integer,
  wins integer,
  losses integer,
  valid_words integer,
  matches_today integer,
  daily_reward integer,
  daily_claimed boolean,
  daily_challenge_claimed boolean,
  league_name text,
  next_title text,
  achievements_unlocked integer,
  achievement_total integer
)
language sql
security definer
set search_path=public,pg_temp
as $$
with me as (
  select p.* from public.profiles p where p.id=(select auth.uid())
),
ordered as (
  select
    row_number() over(order by coalesce(g.finished_at,g.created_at) desc) rn,
    (g.winner_id=(select auth.uid())) won
  from public.game_rooms g
  where g.status='finished'
    and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
),
first_break as (
  select min(rn) rn from ordered where not won
),
streak as (
  select count(*)::int n
  from ordered
  where won and rn<coalesce((select rn from first_break),2147483647)
),
calc as (
  select m.*,
         (
           (coalesce(m.wins,0)*120)
           +(coalesce(m.losses,0)*35)
           +(coalesce(m.valid_words,0)*3)
           +(coalesce(m.total_rounds,0)*5)
         )::int x
  from me m
)
select
  c.display_name,
  c.x as xp,
  greatest(1,(c.x/500)+1)::int as level,
  (c.x%500)::int as level_progress,
  500::int as level_target,
  coalesce((select n from streak),0)::int as current_win_streak,
  coalesce(c.best_streak,0)::int,
  coalesce(c.total_matches,c.wins+c.losses,0)::int,
  coalesce(c.wins,0)::int,
  coalesce(c.losses,0)::int,
  coalesce(c.valid_words,0)::int,
  (
    select count(*)::int
    from public.game_rooms g
    where g.status='finished'
      and coalesce(g.finished_at,g.created_at)::date=current_date
      and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
  ),
  40::int as daily_reward,
  exists(
    select 1 from public.daily_checkins d
    where d.user_id=(select auth.uid()) and d.checkin_date=current_date
  ),
  exists(
    select 1 from public.daily_challenge_claims d
    where d.user_id=(select auth.uid()) and d.challenge_date=current_date
  ),
  public.league_for_rating_v1(c.rating),
  case
    when coalesce(c.wins,0)>=100 then 'EFSANE'
    when coalesce(c.wins,0)>=50 then 'USTA'
    when coalesce(c.wins,0)>=20 then 'DÜELLOCU'
    when coalesce(c.wins,0)>=5 then 'YÜKSELEN'
    else 'ÇAYLAK'
  end,
  (
    (coalesce(c.total_matches,0)>=1)::int
    +(coalesce(c.wins,0)>=1)::int
    +(coalesce(c.wins,0)>=10)::int
    +(coalesce(c.wins,0)>=50)::int
    +(coalesce(c.valid_words,0)>=50)::int
    +(coalesce(c.valid_words,0)>=250)::int
    +(coalesce(c.best_streak,0)>=5)::int
    +(coalesce(c.word_storms,0)>=1)::int
    +(coalesce(c.rating,1000)>=1200)::int
    +(coalesce(c.total_matches,0)>=100)::int
  )::int,
  10::int
from calc c
$$;

revoke all on function public.get_growth_dashboard_v1() from public,anon;
grant execute on function public.get_growth_dashboard_v1() to authenticated;

create or replace function public.claim_match_result_v10(p_room_id uuid)
returns table(
  won boolean,
  xp_gain integer,
  diamonds_awarded integer,
  league_points integer,
  current_rating integer,
  current_streak integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  r public.game_rooms;
  uid uuid:=auth.uid();
  v_won boolean;
  v_words int;
  v_rounds int;
  v_diamonds int;
  v_rating int;
  v_streak int:=0;
  v_inserted boolean:=false;
begin
  if uid is null then raise exception 'not_authenticated'; end if;

  select * into r
  from public.game_rooms
  where id=p_room_id
    and status='finished'
    and (host_id=uid or guest_id=uid);

  if r.id is null then raise exception 'finished_room_not_found'; end if;

  v_won:=r.winner_id=uid;
  select count(*)::int into v_words
  from public.game_words
  where room_id=r.id and player_id=uid;

  v_rounds:=case when uid=r.host_id then r.host_rounds else r.guest_rounds end;
  select rating into v_rating from public.profiles where id=uid;

  v_diamonds:=case when v_won then 8 else 3 end;

  insert into public.match_reward_claims(room_id,user_id,diamonds)
  values(r.id,uid,v_diamonds)
  on conflict do nothing;

  if found then
    v_inserted:=true;
    update public.profiles
    set diamonds=diamonds+v_diamonds,updated_at=now()
    where id=uid;

    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(uid,v_diamonds,'match_result:'||r.id::text,null);
  end if;

  select coalesce(g.current_win_streak,0)
  into v_streak
  from public.get_growth_dashboard_v1() g
  limit 1;

  return query
  select
    v_won,
    ((case when v_won then 120 else 35 end)+v_words*3+v_rounds*5)::int,
    (case when v_inserted then v_diamonds else 0 end)::int,
    (case
      when r.is_bot then case when v_won then 18 else -14 end
      else case when v_won then 20 else -15 end
    end)::int,
    v_rating::int,
    coalesce(v_streak,0)::int;
end
$$;

revoke all on function public.claim_match_result_v10(uuid) from public,anon;
grant execute on function public.claim_match_result_v10(uuid) to authenticated;

create or replace function public.sonharf_apply_finish(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  r public.game_rooms;
begin
  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status<>'finished' or r.stats_applied then return r; end if;

  if r.is_bot then
    update public.profiles
    set total_matches=total_matches+1,
        wins=wins+case when r.winner_id=r.host_id and not r.winner_is_bot then 1 else 0 end,
        losses=losses+case when r.winner_is_bot or r.winner_id is null then 1 else 0 end,
        rating=greatest(100,rating+case when r.winner_id=r.host_id and not r.winner_is_bot then 18 else -14 end),
        presence_status='online',
        last_seen_at=now()
    where id=r.host_id;
  else
    update public.profiles
    set total_matches=total_matches+1,
        wins=wins+case when id=r.winner_id then 1 else 0 end,
        losses=losses+case when id<>r.winner_id then 1 else 0 end,
        rating=greatest(100,rating+case when id=r.winner_id then 20 else -15 end),
        presence_status='online',
        last_seen_at=now()
    where id in (r.host_id,r.guest_id);
  end if;

  update public.game_rooms
  set stats_applied=true,
      streak_shielded_user_id=null
  where id=r.id
  returning * into r;

  delete from public.profile_photo_access
  where owner_id in (r.host_id,r.guest_id)
     or viewer_id in (r.host_id,r.guest_id);

  return r;
end
$$;

revoke all on function public.sonharf_apply_finish(uuid) from public,anon;
grant execute on function public.sonharf_apply_finish(uuid) to authenticated;

create or replace function public.get_meta_dashboard_v10()
returns table(
  total_matches integer,
  wins integer,
  losses integer,
  win_rate integer,
  valid_words integer,
  longest_word text,
  favorite_start_letter text,
  best_streak integer,
  rating integer,
  season_league text,
  achievements_unlocked integer,
  achievement_total integer,
  checkin_streak integer
)
language sql
stable
security definer
set search_path=public,pg_temp
as $$
with p as (
  select * from public.profiles where id=(select auth.uid())
),
longest as (
  select word
  from public.game_words
  where player_id=(select auth.uid())
  order by length(word) desc,id desc
  limit 1
),
fav as (
  select upper(left(normalized_word,1)) l,count(*) c
  from public.game_words
  where player_id=(select auth.uid())
  group by 1
  order by c desc,l
  limit 1
),
dates as (
  select checkin_date,row_number() over(order by checkin_date desc) rn
  from public.daily_checkins
  where user_id=(select auth.uid())
),
streak as (
  select count(*)::int n
  from dates
  where checkin_date=current_date-(rn-1)::int
)
select
  coalesce(p.total_matches,p.wins+p.losses,0)::int,
  p.wins::int,
  p.losses::int,
  case
    when coalesce(p.total_matches,p.wins+p.losses,0)=0 then 0
    else round(p.wins*100.0/coalesce(nullif(p.total_matches,0),p.wins+p.losses))::int
  end,
  p.valid_words::int,
  coalesce((select word from longest),'-'),
  coalesce((select l from fav),'-'),
  p.best_streak::int,
  p.rating::int,
  public.league_for_rating_v1(p.rating),
  (
    (p.total_matches>=1)::int
    +(p.wins>=1)::int
    +(p.wins>=10)::int
    +(p.wins>=50)::int
    +(p.valid_words>=50)::int
    +(p.valid_words>=250)::int
    +(p.best_streak>=5)::int
    +(p.word_storms>=1)::int
    +(p.rating>=1200)::int
    +(p.total_matches>=100)::int
  )::int,
  10::int,
  coalesce((select n from streak),0)::int
from p
$$;

revoke all on function public.get_meta_dashboard_v10() from public,anon;
grant execute on function public.get_meta_dashboard_v10() to authenticated;
