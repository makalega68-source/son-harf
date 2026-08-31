-- Training bot competitive guard v1
-- Bot rooms remain server-authoritative training matches, never competitive PvP.

create or replace function public.sonharf_apply_finish(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  r public.game_rooms;
  v_meaningful boolean;
begin
  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status<>'finished' or r.stats_applied then return r; end if;

  v_meaningful := (r.winner_id is not null or coalesce(r.valid_word_count,0)>0);

  if coalesce(r.is_bot,false) then
    -- Training-only: deliberately do not touch rating, league inputs, PvP W/L,
    -- total competitive matches or competitive streak state.
    update public.profiles
    set presence_status='online',last_seen_at=now()
    where id=r.host_id;
  else
    if r.guest_id is null then raise exception 'competitive_match_requires_two_humans'; end if;
    update public.profiles
    set total_matches=total_matches+case when v_meaningful then 1 else 0 end,
        wins=wins+case when r.winner_id is not null and id=r.winner_id then 1 else 0 end,
        losses=losses+case when r.winner_id is not null and id<>r.winner_id then 1 else 0 end,
        rating=greatest(
          100,
          rating+case
            when r.winner_id is null then 0
            when id=r.winner_id then 20
            else -15
          end
        ),
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
    and coalesce(g.is_bot,false)=false
    and g.host_id is not null
    and g.guest_id is not null
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
      and coalesce(g.is_bot,false)=false
      and g.host_id is not null
      and g.guest_id is not null
      and coalesce(g.finished_at,g.created_at)::date=current_date
      and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
  ),
  40::int as daily_reward,
  exists(select 1 from public.daily_checkins d where d.user_id=(select auth.uid()) and d.checkin_date=current_date),
  exists(select 1 from public.daily_challenge_claims d where d.user_id=(select auth.uid()) and d.challenge_date=current_date),
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

  select * into r from public.game_rooms
  where id=p_room_id and status='finished' and (host_id=uid or guest_id=uid);
  if r.id is null then raise exception 'finished_room_not_found'; end if;

  v_won:=r.winner_id=uid;
  select count(*)::int into v_words from public.game_words where room_id=r.id and player_id=uid;
  v_rounds:=case when uid=r.host_id then r.host_rounds else r.guest_rounds end;
  select rating into v_rating from public.profiles where id=uid;
  v_diamonds:=case when v_won then 8 else 3 end;

  insert into public.match_reward_claims(room_id,user_id,diamonds)
  values(r.id,uid,v_diamonds)
  on conflict do nothing;
  if found then
    v_inserted:=true;
    update public.profiles set diamonds=diamonds+v_diamonds,updated_at=now() where id=uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(uid,v_diamonds,'match_result:'||r.id::text,null);
  end if;

  select coalesce(g.current_win_streak,0) into v_streak
  from public.get_growth_dashboard_v1() g limit 1;

  return query select
    v_won,
    ((case when v_won then 120 else 35 end)+v_words*3+v_rounds*5)::int,
    (case when v_inserted then v_diamonds else 0 end)::int,
    (case
      when coalesce(r.is_bot,false) then 0
      when v_won then 20
      else -15
    end)::int,
    v_rating::int,
    coalesce(v_streak,0)::int;
end
$$;

-- Server assigns a natural display name to bot rooms. Bot identity is still the
-- immutable server-side is_bot flag; no profile/account is created for it.
create or replace function private.assign_training_bot_name_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
declare
  v_names text[]:=array[
    'Elif','Zeynep','Ece','Defne','Duru','İrem','Selin','Ceren','Melis','Yağmur',
    'Ada','İlayda','Buse','Nehir','Aslı','Sude','Derin','Naz','Gökçe','Merve',
    'Emir','Mert','Kerem','Arda','Eren','Can','Berk','Kaan','Onur','Barış',
    'Deniz','Atlas','Yiğit','Ozan','Umut','Tolga','Burak','Alp','Doruk','Cem'
  ];
  v_previous text;
  v_candidate text;
begin
  if not coalesce(new.is_bot,false) then return new; end if;
  select g.bot_name into v_previous
  from public.game_rooms g
  where g.host_id=new.host_id and coalesce(g.is_bot,false)
  order by g.created_at desc
  limit 1;

  loop
    v_candidate:=v_names[1+floor(random()*array_length(v_names,1))::int];
    exit when v_previous is null or v_candidate<>v_previous;
  end loop;
  new.bot_name:=v_candidate;
  return new;
end
$$;

drop trigger if exists game_rooms_training_bot_name_v1 on public.game_rooms;
create trigger game_rooms_training_bot_name_v1
before insert on public.game_rooms
for each row execute function private.assign_training_bot_name_v1();

-- Defense-in-depth: no tournament scorer may turn a server-known bot room into
-- a competitive event by changing client payloads.
create or replace function private.reject_bot_tournament_event_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if new.room_id is not null and exists(
    select 1 from public.game_rooms g
    where g.id=new.room_id and coalesce(g.is_bot,false)
  ) then
    raise exception 'bot_match_not_competitive';
  end if;
  return new;
end
$$;

drop trigger if exists weekly_tournament_no_bot_room_v1 on public.weekly_tournament_match_events;
create trigger weekly_tournament_no_bot_room_v1
before insert or update of room_id on public.weekly_tournament_match_events
for each row execute function private.reject_bot_tournament_event_v1();

revoke all on function private.assign_training_bot_name_v1() from public,anon,authenticated;
revoke all on function private.reject_bot_tournament_event_v1() from public,anon,authenticated;
revoke all on function public.sonharf_apply_finish(uuid) from public,anon;
revoke all on function public.get_growth_dashboard_v1() from public,anon;
revoke all on function public.claim_match_result_v10(uuid) from public,anon;
grant execute on function public.sonharf_apply_finish(uuid) to authenticated;
grant execute on function public.get_growth_dashboard_v1() to authenticated;
grant execute on function public.claim_match_result_v10(uuid) to authenticated;
