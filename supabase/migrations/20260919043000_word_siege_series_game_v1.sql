-- Kelime Kuşatması premium Seri Oyun.
-- Server-authoritative rules:
-- * separate matchmaking pool
-- * 5-minute turns
-- * expired turns are automatic passes
-- * a player's third consecutive missed turn ends the game as a loss
-- * a successful/manual action resets that player's missed-turn streak

alter table public.word_siege_games
  add column if not exists game_mode text not null default 'classic',
  add column if not exists turn_duration_minutes smallint,
  add column if not exists player_one_missed_turns smallint not null default 0,
  add column if not exists player_two_missed_turns smallint not null default 0;

update public.word_siege_games
set game_mode='classic'
where game_mode is null or game_mode='';

alter table public.word_siege_games drop constraint if exists word_siege_games_game_mode_v1_check;
alter table public.word_siege_games
  add constraint word_siege_games_game_mode_v1_check check (game_mode in ('classic','series')),
  add constraint word_siege_games_series_minutes_v1_check check (turn_duration_minutes is null or turn_duration_minutes between 1 and 30),
  add constraint word_siege_games_p1_missed_v1_check check (player_one_missed_turns between 0 and 3),
  add constraint word_siege_games_p2_missed_v1_check check (player_two_missed_turns between 0 and 3);

create index if not exists word_siege_series_waiting_v1_idx
  on public.word_siege_games(language,turn_duration_minutes,created_at)
  where status='waiting' and game_mode='series';
create index if not exists word_siege_series_deadline_v1_idx
  on public.word_siege_games(turn_deadline)
  where status='playing' and game_mode='series' and turn_deadline is not null;

create or replace function public.has_series_game_access_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path=''
as $$
  select exists(select 1 from public.profiles p where p.id=p_user_id and coalesce(p.is_vip,false))
      or public.has_permanent_entitlement_v1(p_user_id,'series_game')
      or public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$$;
revoke all on function public.has_series_game_access_v1(uuid) from public,anon,authenticated;
grant execute on function public.has_series_game_access_v1(uuid) to service_role;

-- Keep the normal Kuşatma pool isolated from premium Series Game rows.
create or replace function private.find_or_create_word_siege_game_v2(
  p_language text default 'tr'::text,
  p_turn_duration_hours integer default 12
)
returns public.word_siege_games
language plpgsql
security definer
set search_path to 'pg_catalog','public','private','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_language text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_duration smallint;
  v_bag text;
  v_rack text;
  v_active_count integer;
  v_now timestamptz := clock_timestamp();
  r public.word_siege_games;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not exists(select 1 from public.profiles p where p.id=v_uid) then raise exception 'word_siege_profile_required'; end if;
  if p_turn_duration_hours not in (12,72) then raise exception 'word_siege_invalid_turn_duration'; end if;
  v_duration := p_turn_duration_hours::smallint;

  perform pg_advisory_xact_lock(hashtextextended('word_siege:classic:'||v_language||':'||v_duration::text,0));

  update public.word_siege_games
     set status='cancelled',last_action='matchmaking_expired',updated_at=v_now
   where status='waiting' and game_mode='classic'
     and greatest(coalesce(updated_at,created_at),created_at) < v_now-interval '30 minutes';

  select count(*)::integer into v_active_count
    from public.word_siege_games g
   where g.status in ('waiting','playing') and v_uid in (g.player_one_id,g.player_two_id);
  if v_active_count>=public.word_siege_active_limit_v1(v_uid) then raise exception 'word_siege_active_limit'; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting' and g.game_mode='classic' and g.player_one_id=v_uid
     and g.language=v_language and g.turn_duration_hours=v_duration
   order by g.created_at limit 1;
  if r.id is not null then return r; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting' and g.game_mode='classic' and g.language=v_language
     and g.turn_duration_hours=v_duration and g.player_one_id<>v_uid
     and not exists(
       select 1 from public.user_blocks b
       where (b.blocker_id=v_uid and b.blocked_id=g.player_one_id)
          or (b.blocker_id=g.player_one_id and b.blocked_id=v_uid)
     )
   order by g.created_at
   for update skip locked
   limit 1;

  if r.id is not null then
    update public.word_siege_games
       set player_two_id=v_uid,
           player_two_rack=substring(r.bag from 1 for 7),
           bag=substring(r.bag from 8),
           status='playing',current_player_id=r.player_one_id,
           turn_started_at=v_now,turn_deadline=v_now+make_interval(hours=>v_duration),
           last_action='game_started',last_action_player_id=null,updated_at=v_now
     where id=r.id returning * into r;
    return r;
  end if;

  v_bag := private.word_siege_new_bag_v1(v_language);
  v_rack := substring(v_bag from 1 for 7);
  v_bag := substring(v_bag from 8);
  insert into public.word_siege_games(
    player_one_id,language,turn_duration_hours,game_mode,board,bag,player_one_rack,last_action
  ) values(
    v_uid,v_language,v_duration,'classic',private.word_siege_new_board_v1(),v_bag,v_rack,'waiting_for_opponent'
  ) returning * into r;
  return r;
end
$$;
revoke all on function private.find_or_create_word_siege_game_v2(text,integer) from public,anon,authenticated;

create or replace function public.find_or_create_word_siege_series_game_v1(
  p_language text default 'tr',
  p_turn_duration_minutes integer default 5
)
returns public.word_siege_games
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid();
  v_language text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_minutes smallint:=coalesce(p_turn_duration_minutes,5)::smallint;
  v_bag text;
  v_rack text;
  v_active_count integer;
  v_now timestamptz:=clock_timestamp();
  r public.word_siege_games;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not public.has_series_game_access_v1(v_uid) then raise exception 'series_game_required'; end if;
  if v_minutes not in (3,5,10) then raise exception 'series_game_invalid_turn_duration'; end if;
  if not exists(select 1 from public.profiles p where p.id=v_uid) then raise exception 'word_siege_profile_required'; end if;
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then raise exception 'maintenance_mode'; end if;
  if not public.sonharf_config_enabled('word_siege_enabled',true) and not public.is_admin() then raise exception 'word_siege_disabled'; end if;
  if (not public.sonharf_config_enabled('word_siege_matchmaking_enabled',true) or not public.sonharf_config_enabled('matchmaking_enabled',true)) and not public.is_admin() then raise exception 'matchmaking_disabled'; end if;

  perform pg_advisory_xact_lock(hashtextextended('word_siege:series:'||v_language||':'||v_minutes::text,0));

  update public.word_siege_games
     set status='cancelled',last_action='series_matchmaking_expired',updated_at=v_now
   where status='waiting' and game_mode='series'
     and greatest(coalesce(updated_at,created_at),created_at) < v_now-interval '30 minutes';

  select count(*)::integer into v_active_count
    from public.word_siege_games g
   where g.status in ('waiting','playing') and v_uid in (g.player_one_id,g.player_two_id);
  if v_active_count>=public.word_siege_active_limit_v1(v_uid) then raise exception 'word_siege_active_limit'; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting' and g.game_mode='series' and g.player_one_id=v_uid
     and g.language=v_language and g.turn_duration_minutes=v_minutes
   order by g.created_at limit 1;
  if r.id is not null then return r; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting' and g.game_mode='series' and g.language=v_language
     and g.turn_duration_minutes=v_minutes and g.player_one_id<>v_uid
     and public.has_series_game_access_v1(g.player_one_id)
     and not exists(
       select 1 from public.user_blocks b
       where (b.blocker_id=v_uid and b.blocked_id=g.player_one_id)
          or (b.blocker_id=g.player_one_id and b.blocked_id=v_uid)
     )
   order by g.created_at
   for update skip locked limit 1;

  if r.id is not null then
    update public.word_siege_games
       set player_two_id=v_uid,
           player_two_rack=substring(r.bag from 1 for 7),
           bag=substring(r.bag from 8),status='playing',current_player_id=r.player_one_id,
           turn_started_at=v_now,turn_deadline=v_now+make_interval(mins=>v_minutes),
           player_one_missed_turns=0,player_two_missed_turns=0,
           last_action='series_game_started',last_action_player_id=null,updated_at=v_now
     where id=r.id returning * into r;
    return r;
  end if;

  v_bag:=private.word_siege_new_bag_v1(v_language);
  v_rack:=substring(v_bag from 1 for 7);
  v_bag:=substring(v_bag from 8);
  insert into public.word_siege_games(
    player_one_id,language,game_mode,turn_duration_minutes,turn_duration_hours,
    board,bag,player_one_rack,last_action
  ) values(
    v_uid,v_language,'series',v_minutes,12,
    private.word_siege_new_board_v1(),v_bag,v_rack,'series_waiting_for_opponent'
  ) returning * into r;
  return r;
end
$$;
revoke all on function public.find_or_create_word_siege_series_game_v1(text,integer) from public,anon;
grant execute on function public.find_or_create_word_siege_series_game_v1(text,integer) to authenticated,service_role;

-- Existing public move RPCs already call this helper after every successful action.
-- Series Game therefore gets minute deadlines without duplicating the scoring engine.
create or replace function private.word_siege_arm_next_turn_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare r public.word_siege_games; v_now timestamptz:=clock_timestamp();
begin
  select * into r from public.word_siege_games where id=p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if r.status='playing' and r.current_player_id is not null then
    update public.word_siege_games
       set turn_started_at=v_now,
           turn_deadline=case when game_mode='series'
             then v_now+make_interval(mins=>coalesce(turn_duration_minutes,5))
             else v_now+make_interval(hours=>turn_duration_hours) end,
           updated_at=v_now
     where id=r.id returning * into r;
  else
    update public.word_siege_games set turn_started_at=null,turn_deadline=null where id=r.id returning * into r;
  end if;
  return r;
end
$$;
revoke all on function private.word_siege_arm_next_turn_v2(uuid) from public,anon,authenticated;

-- Reset only the actor's missed-turn streak after any successful manual action.
create or replace function private.word_siege_series_reset_actor_v1(p_game_id uuid,p_actor uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare r public.word_siege_games;
begin
  update public.word_siege_games
     set player_one_missed_turns=case when p_actor=player_one_id then 0 else player_one_missed_turns end,
         player_two_missed_turns=case when p_actor=player_two_id then 0 else player_two_missed_turns end
   where id=p_game_id and game_mode='series'
   returning * into r;
  if r.id is null then select * into r from public.word_siege_games where id=p_game_id; end if;
  return r;
end
$$;
revoke all on function private.word_siege_series_reset_actor_v1(uuid,uuid) from public,anon,authenticated;

-- Branch the already-scheduled timeout engine: classic games keep their current timeout loss;
-- Series Game auto-passes and only loses on the third consecutive miss by that player.
create or replace function private.word_siege_finalize_timeout_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare
  r public.word_siege_games;
  v_loser uuid;
  v_winner uuid;
  v_other uuid;
  v_missed smallint;
  v_now timestamptz:=clock_timestamp();
begin
  select * into r from public.word_siege_games where id=p_game_id for update;
  if r.id is null then return null; end if;
  if r.status<>'playing' or r.current_player_id is null or r.turn_deadline is null then return r; end if;
  if v_now<r.turn_deadline then return r; end if;

  v_loser:=r.current_player_id;
  v_winner:=case when v_loser=r.player_one_id then r.player_two_id else r.player_one_id end;

  if r.game_mode<>'series' then
    update public.word_siege_games
       set status='finished',current_player_id=null,winner_id=v_winner,loser_id=v_loser,
           finish_reason='timeout',result_applied=true,last_action='timeout',last_action_player_id=v_loser,
           last_move_at=coalesce(last_move_at,turn_started_at),turn_started_at=null,turn_deadline=null,
           finished_at=v_now,updated_at=v_now
     where id=r.id and status='playing' and current_player_id=v_loser and turn_deadline is not null and v_now>=turn_deadline
     returning * into r;
    if r.id is null then select * into r from public.word_siege_games where id=p_game_id; end if;
    return r;
  end if;

  v_other:=v_winner;
  v_missed:=case when v_loser=r.player_one_id then r.player_one_missed_turns+1 else r.player_two_missed_turns+1 end;

  if v_missed>=3 then
    update public.word_siege_games
       set player_one_missed_turns=case when v_loser=player_one_id then 3 else player_one_missed_turns end,
           player_two_missed_turns=case when v_loser=player_two_id then 3 else player_two_missed_turns end,
           status='finished',current_player_id=null,winner_id=v_winner,loser_id=v_loser,
           finish_reason='series_three_missed_turns',result_applied=true,
           last_action='series_timeout_loss',last_action_player_id=v_loser,
           last_move_at=v_now,turn_started_at=null,turn_deadline=null,finished_at=v_now,updated_at=v_now
     where id=r.id returning * into r;
    return r;
  end if;

  update public.word_siege_games
     set player_one_missed_turns=case when v_loser=player_one_id then v_missed else player_one_missed_turns end,
         player_two_missed_turns=case when v_loser=player_two_id then v_missed else player_two_missed_turns end,
         current_player_id=v_other,
         consecutive_passes=0,
         move_count=move_count+1,
         last_action='series_auto_pass',last_action_player_id=v_loser,last_move_at=v_now,
         turn_started_at=v_now,
         turn_deadline=v_now+make_interval(mins=>coalesce(turn_duration_minutes,5)),
         updated_at=v_now
   where id=r.id returning * into r;
  return r;
end
$$;
revoke all on function private.word_siege_finalize_timeout_v2(uuid) from public,anon,authenticated;

-- Existing move RPCs are wrapped to reset a Series player's missed streak after a real action.
do $series_wrap_moves$
declare f text;
begin
  f:=pg_get_functiondef('public.submit_word_siege_move_v1(uuid,jsonb,boolean)'::regprocedure);
  if position('word_siege_series_reset_actor_v1' in f)=0 then
    f:=replace(f,
      'if r.status=''playing'' then r:=private.word_siege_arm_next_turn_v2(r.id); end if; return r;',
      'if r.game_mode=''series'' then r:=private.word_siege_series_reset_actor_v1(r.id,v_uid); end if; if r.status=''playing'' then r:=private.word_siege_arm_next_turn_v2(r.id); end if; return r;');
    if position('word_siege_series_reset_actor_v1' in f)=0 then raise exception 'series_submit_wrap_hook_missing'; end if;
    execute f;
  end if;

  f:=pg_get_functiondef('public.pass_word_siege_turn_v1(uuid)'::regprocedure);
  if position('word_siege_series_reset_actor_v1' in f)=0 then
    if position('return r;' in f)=0 then raise exception 'series_pass_wrap_hook_missing'; end if;
    f:=replace(f,'return r;','if r.game_mode=''series'' then r:=private.word_siege_series_reset_actor_v1(r.id,v_uid); end if; return r;');
    execute f;
  end if;

  f:=pg_get_functiondef('public.exchange_word_siege_tiles_v1(uuid,jsonb)'::regprocedure);
  if position('word_siege_series_reset_actor_v1' in f)=0 then
    if position('return r;' in f)=0 then raise exception 'series_exchange_wrap_hook_missing'; end if;
    f:=replace(f,'return r;','if r.game_mode=''series'' then r:=private.word_siege_series_reset_actor_v1(r.id,v_uid); end if; return r;');
    execute f;
  end if;
end
$series_wrap_moves$;

create table if not exists public.word_siege_series_invites(
  id uuid primary key default gen_random_uuid(),
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check(language in ('tr','en')),
  turn_duration_minutes smallint not null default 5 check(turn_duration_minutes in (3,5,10)),
  status text not null default 'pending' check(status in ('pending','accepted','declined','expired','cancelled')),
  game_id uuid references public.word_siege_games(id) on delete set null,
  expires_at timestamptz not null default (now()+interval '10 minutes'),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check(sender_id<>receiver_id)
);
alter table public.word_siege_series_invites enable row level security;
revoke all on public.word_siege_series_invites from anon,authenticated;
drop policy if exists word_siege_series_invites_read_v1 on public.word_siege_series_invites;
create policy word_siege_series_invites_read_v1 on public.word_siege_series_invites
for select to authenticated using((select auth.uid()) in (sender_id,receiver_id));
grant select on public.word_siege_series_invites to authenticated;
create index if not exists word_siege_series_invites_receiver_v1_idx on public.word_siege_series_invites(receiver_id,status,expires_at);

create or replace function public.invite_friend_to_word_siege_series_v1(
  p_friend_id uuid,p_language text default 'tr',p_turn_duration_minutes integer default 5
)
returns public.word_siege_series_invites
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare v_uid uuid:=auth.uid(); v_language text; v_minutes smallint; inv public.word_siege_series_invites;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not public.has_series_game_access_v1(v_uid) then raise exception 'series_game_required'; end if;
  if p_friend_id is null or p_friend_id=v_uid then raise exception 'word_siege_invalid_friend'; end if;
  if not public.has_series_game_access_v1(p_friend_id) then raise exception 'series_game_friend_required'; end if;
  v_language:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_minutes:=coalesce(p_turn_duration_minutes,5)::smallint;
  if v_minutes not in (3,5,10) then raise exception 'series_game_invalid_turn_duration'; end if;
  if not exists(select 1 from public.friendships f where f.user_id=least(v_uid,p_friend_id) and f.friend_id=greatest(v_uid,p_friend_id) and f.status='accepted') then raise exception 'word_siege_not_friends'; end if;
  if exists(select 1 from public.user_blocks b where (b.blocker_id=v_uid and b.blocked_id=p_friend_id) or (b.blocker_id=p_friend_id and b.blocked_id=v_uid)) then raise exception 'word_siege_blocked_relationship'; end if;
  update public.word_siege_series_invites set status='expired',responded_at=now() where status='pending' and expires_at<now();
  if exists(select 1 from public.word_siege_series_invites i where i.status='pending' and i.expires_at>=now() and ((i.sender_id=v_uid and i.receiver_id=p_friend_id) or (i.sender_id=p_friend_id and i.receiver_id=v_uid))) then raise exception 'word_siege_invite_pending'; end if;
  insert into public.word_siege_series_invites(sender_id,receiver_id,language,turn_duration_minutes)
  values(v_uid,p_friend_id,v_language,v_minutes) returning * into inv;
  return inv;
end
$$;
revoke all on function public.invite_friend_to_word_siege_series_v1(uuid,text,integer) from public,anon;
grant execute on function public.invite_friend_to_word_siege_series_v1(uuid,text,integer) to authenticated,service_role;

create or replace function public.respond_word_siege_series_invite_v1(p_invite_id uuid,p_accept boolean)
returns public.word_siege_games
language plpgsql
security definer
set search_path='pg_catalog','public','private','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid(); inv public.word_siege_series_invites; r public.word_siege_games;
  v_bag text; v_one_rack text; v_two_rack text; v_sender_active integer; v_receiver_active integer; v_now timestamptz:=clock_timestamp();
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into inv from public.word_siege_series_invites where id=p_invite_id for update;
  if inv.id is null then raise exception 'word_siege_invite_not_found'; end if;
  if inv.receiver_id<>v_uid then raise exception 'word_siege_not_invite_receiver'; end if;
  if inv.status<>'pending' then raise exception 'word_siege_invite_not_pending'; end if;
  if inv.expires_at<now() then update public.word_siege_series_invites set status='expired',responded_at=now() where id=inv.id; return null; end if;
  if not coalesce(p_accept,false) then update public.word_siege_series_invites set status='declined',responded_at=now() where id=inv.id; return null; end if;
  if not public.has_series_game_access_v1(inv.sender_id) or not public.has_series_game_access_v1(inv.receiver_id) then raise exception 'series_game_required'; end if;
  if not exists(select 1 from public.friendships f where f.user_id=least(inv.sender_id,inv.receiver_id) and f.friend_id=greatest(inv.sender_id,inv.receiver_id) and f.status='accepted') then raise exception 'word_siege_not_friends'; end if;
  if exists(select 1 from public.user_blocks b where (b.blocker_id=inv.sender_id and b.blocked_id=inv.receiver_id) or (b.blocker_id=inv.receiver_id and b.blocked_id=inv.sender_id)) then raise exception 'word_siege_blocked_relationship'; end if;

  perform pg_advisory_xact_lock(hashtextextended('word_siege_series_friend:'||least(inv.sender_id,inv.receiver_id)::text||':'||greatest(inv.sender_id,inv.receiver_id)::text,0));
  select count(*)::integer into v_sender_active from public.word_siege_games g where g.status in ('waiting','playing') and inv.sender_id in(g.player_one_id,g.player_two_id);
  select count(*)::integer into v_receiver_active from public.word_siege_games g where g.status in ('waiting','playing') and inv.receiver_id in(g.player_one_id,g.player_two_id);
  if v_sender_active>=public.word_siege_active_limit_v1(inv.sender_id) or v_receiver_active>=public.word_siege_active_limit_v1(inv.receiver_id) then raise exception 'word_siege_active_limit'; end if;

  v_bag:=private.word_siege_new_bag_v1(inv.language);
  v_one_rack:=substring(v_bag from 1 for 7); v_two_rack:=substring(v_bag from 8 for 7); v_bag:=substring(v_bag from 15);
  insert into public.word_siege_games(
    player_one_id,player_two_id,status,language,current_player_id,game_mode,turn_duration_minutes,turn_duration_hours,
    board,bag,player_one_rack,player_two_rack,last_action,last_action_player_id,turn_started_at,turn_deadline
  ) values(
    inv.sender_id,inv.receiver_id,'playing',inv.language,inv.sender_id,'series',inv.turn_duration_minutes,12,
    private.word_siege_new_board_v1(),v_bag,v_one_rack,v_two_rack,'series_friend_game_started',inv.sender_id,
    v_now,v_now+make_interval(mins=>inv.turn_duration_minutes)
  ) returning * into r;
  update public.word_siege_series_invites set status='accepted',game_id=r.id,responded_at=now() where id=inv.id;
  update public.profiles set presence_status='in_game',last_seen_at=now(),updated_at=now() where id in(inv.sender_id,inv.receiver_id);
  return r;
end
$$;
revoke all on function public.respond_word_siege_series_invite_v1(uuid,boolean) from public,anon;
grant execute on function public.respond_word_siege_series_invite_v1(uuid,boolean) to authenticated,service_role;

select pg_notify('pgrst','reload schema');
