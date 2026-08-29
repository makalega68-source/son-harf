-- Team Arena 2v2 v1
-- Friend-lobby-only 4-player social mode.
-- No rating, league, Son Coin, club points, tournament points or profile match rewards.
-- This file is intentionally zz-prefixed so it runs after Word Arena/Daily Arena dependencies.

create table if not exists public.team_arena_rooms (
  id uuid primary key default gen_random_uuid(),
  host_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check(language in ('tr','en')),
  status text not null default 'lobby' check(status in ('lobby','playing','finished','cancelled')),
  letters text,
  starts_at timestamptz,
  ends_at timestamptz,
  expires_at timestamptz not null default (now()+interval '10 minutes'),
  team_a_score integer not null default 0 check(team_a_score>=0),
  team_b_score integer not null default 0 check(team_b_score>=0),
  winner_team integer check(winner_team in (1,2)),
  result_applied boolean not null default false,
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  check(
    (status='lobby' and starts_at is null and ends_at is null and letters is null)
    or status<>'lobby'
  )
);

create table if not exists public.team_arena_members (
  room_id uuid not null references public.team_arena_rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  team integer not null check(team in (1,2)),
  seat integer not null check(seat in (1,2)),
  ready boolean not null default false,
  joined_at timestamptz not null default now(),
  primary key(room_id,user_id),
  unique(room_id,team,seat)
);

create table if not exists public.team_arena_invites (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.team_arena_rooms(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  team integer not null check(team in (1,2)),
  seat integer not null check(seat in (1,2)),
  status text not null default 'pending' check(status in ('pending','accepted','declined','expired')),
  expires_at timestamptz not null,
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check(sender_id<>receiver_id)
);

create table if not exists public.team_arena_words (
  id bigint generated always as identity primary key,
  room_id uuid not null references public.team_arena_rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  team integer not null check(team in (1,2)),
  word text not null,
  normalized_word text not null,
  base_points integer not null check(base_points>=0),
  combo integer not null default 1 check(combo>=1),
  created_at timestamptz not null default now(),
  unique(room_id,team,normalized_word)
);

alter table public.team_arena_rooms enable row level security;
alter table public.team_arena_members enable row level security;
alter table public.team_arena_invites enable row level security;
alter table public.team_arena_words enable row level security;

revoke all on public.team_arena_rooms from anon,authenticated;
revoke all on public.team_arena_members from anon,authenticated;
revoke all on public.team_arena_invites from anon,authenticated;
revoke all on public.team_arena_words from anon,authenticated;

create index if not exists team_arena_rooms_host_status_idx
  on public.team_arena_rooms(host_id,status,created_at desc);
create index if not exists team_arena_members_user_idx
  on public.team_arena_members(user_id,room_id);
create index if not exists team_arena_invites_receiver_pending_idx
  on public.team_arena_invites(receiver_id,created_at desc)
  where status='pending';
create unique index if not exists team_arena_invites_pending_seat_uidx
  on public.team_arena_invites(room_id,team,seat)
  where status='pending';
create index if not exists team_arena_invites_sender_idx
  on public.team_arena_invites(sender_id,created_at desc);
create index if not exists team_arena_words_room_created_idx
  on public.team_arena_words(room_id,created_at,id);
create index if not exists team_arena_words_user_idx
  on public.team_arena_words(user_id,room_id);

create schema if not exists private;
revoke all on schema private from public,anon;
grant usage on schema private to authenticated;

CREATE OR REPLACE FUNCTION private.cancel_team_arena_lobby_v1(p_room_id uuid)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.host_id<>v_uid then raise exception 'team_arena_host_required'; end if;
  if r.status<>'lobby' then raise exception 'team_arena_lobby_closed'; end if;

  update public.team_arena_rooms
  set status='cancelled'
  where id=r.id;

  update public.team_arena_invites
  set status='expired',responded_at=clock_timestamp()
  where room_id=r.id and status='pending';

  return true;
end
$function$;

CREATE OR REPLACE FUNCTION private.cleanup_team_arena_v1()
 RETURNS integer
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_count int:=0;
begin
  update public.team_arena_rooms r
  set status='cancelled'
  where r.status='lobby'
    and r.expires_at<=clock_timestamp();

  get diagnostics v_count=row_count;

  update public.team_arena_invites i
  set status='expired',responded_at=clock_timestamp()
  where i.status='pending'
    and (
      i.expires_at<=clock_timestamp()
      or exists(
        select 1 from public.team_arena_rooms r
        where r.id=i.room_id and r.status<>'lobby'
      )
    );

  return v_count;
end
$function$;

CREATE OR REPLACE FUNCTION private.create_team_arena_v1(p_language text DEFAULT 'tr'::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(trim(coalesce(p_language,'tr')))='en' then 'en' else 'tr' end;
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  if private.team_arena_user_active_v1(v_uid,null) then
    raise exception 'team_arena_already_active';
  end if;
  if private.team_arena_other_mode_active_v1(v_uid) then
    raise exception 'player_already_in_game';
  end if;

  update public.matchmaking_queue
  set status='cancelled',heartbeat_at=clock_timestamp()
  where user_id=v_uid and status='waiting';

  update public.word_arena_queue
  set status='cancelled',heartbeat_at=clock_timestamp()
  where user_id=v_uid and status='waiting';

  insert into public.team_arena_rooms(host_id,language)
  values(v_uid,v_lang)
  returning * into r;

  insert into public.team_arena_members(room_id,user_id,team,seat,ready)
  values(r.id,v_uid,1,1,true);

  return jsonb_build_object(
    'room_id',r.id,
    'status',r.status,
    'language',r.language,
    'expires_at',r.expires_at
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.finish_team_arena_internal_v1(p_room_id uuid)
 RETURNS team_arena_rooms
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  r public.team_arena_rooms;
  v_a int:=0;
  v_b int:=0;
begin
  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.status='finished' then return r; end if;
  if r.status<>'playing' then raise exception 'team_arena_not_active'; end if;
  if clock_timestamp()<r.ends_at then return r; end if;

  select coalesce(sum(w.base_points),0)::int into v_a
  from public.team_arena_words w
  where w.room_id=r.id and w.team=1;

  select coalesce(sum(w.base_points),0)::int into v_b
  from public.team_arena_words w
  where w.room_id=r.id and w.team=2;

  update public.team_arena_rooms tr
  set status='finished',
      team_a_score=v_a,
      team_b_score=v_b,
      winner_team=case when v_a>v_b then 1 when v_b>v_a then 2 else null end,
      result_applied=true,
      finished_at=clock_timestamp()
  where tr.id=r.id
  returning tr.* into r;

  update public.profiles p
  set presence_status='online',
      last_seen_at=clock_timestamp()
  where p.id in (
    select m.user_id
    from public.team_arena_members m
    where m.room_id=r.id
  );

  return r;
end
$function$;

CREATE OR REPLACE FUNCTION private.get_incoming_team_arena_invites_v1()
 RETURNS TABLE(invite_id uuid, room_id uuid, sender_id uuid, language text, team integer, seat integer, expires_at timestamp with time zone, created_at timestamp with time zone)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  return query
  select
    i.id,
    i.room_id,
    i.sender_id,
    r.language,
    i.team,
    i.seat,
    i.expires_at,
    i.created_at
  from public.team_arena_invites i
  join public.team_arena_rooms r on r.id=i.room_id
  where i.receiver_id=v_uid
    and i.status='pending'
    and i.expires_at>clock_timestamp()
    and r.status='lobby'
    and r.expires_at>clock_timestamp()
  order by i.created_at
  limit 10;
end
$function$;

CREATE OR REPLACE FUNCTION private.get_my_active_team_arena_v1()
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  select tr.* into r
  from public.team_arena_members m
  join public.team_arena_rooms tr on tr.id=m.room_id
  where m.user_id=v_uid
    and (
      (tr.status='lobby' and tr.expires_at>clock_timestamp())
      or (tr.status='playing' and tr.ends_at>clock_timestamp())
    )
  order by
    case tr.status when 'playing' then 0 else 1 end,
    tr.created_at desc
  limit 1;

  if r.id is null then
    return jsonb_build_object('active',false);
  end if;

  return jsonb_build_object(
    'active',true,
    'room_id',r.id,
    'status',r.status
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.get_team_arena_members_v1(p_room_id uuid)
 RETURNS TABLE(user_id uuid, display_name text, team integer, seat integer, ready boolean, is_host boolean, presence_status text)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if not exists(
    select 1 from public.team_arena_members m
    where m.room_id=p_room_id and m.user_id=v_uid
  ) then raise exception 'team_arena_not_participant'; end if;

  return query
  select
    m.user_id,
    p.display_name,
    m.team,
    m.seat,
    m.ready,
    r.host_id=m.user_id,
    coalesce(p.presence_status,'offline')
  from public.team_arena_members m
  join public.profiles p on p.id=m.user_id
  join public.team_arena_rooms r on r.id=m.room_id
  where m.room_id=p_room_id
  order by m.team,m.seat;
end
$function$;

CREATE OR REPLACE FUNCTION private.get_team_arena_room_v1(p_room_id uuid)
 RETURNS TABLE(room_id uuid, host_id uuid, language text, status text, letters text, starts_at timestamp with time zone, ends_at timestamp with time zone, expires_at timestamp with time zone, team_a_score integer, team_b_score integer, winner_team integer, my_team integer, is_host boolean, member_count bigint, ready_count bigint)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
  v_team int;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  select m.team into v_team
  from public.team_arena_members m
  where m.room_id=p_room_id and m.user_id=v_uid;

  if v_team is null then raise exception 'team_arena_not_participant'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;

  if r.status='playing' and clock_timestamp()>=r.ends_at then
    r:=private.finish_team_arena_internal_v1(r.id);
  end if;

  return query
  select
    r.id,
    r.host_id,
    r.language,
    r.status,
    case when r.status in ('playing','finished') then r.letters else null end,
    r.starts_at,
    r.ends_at,
    r.expires_at,
    r.team_a_score,
    r.team_b_score,
    r.winner_team,
    v_team,
    r.host_id=v_uid,
    (select count(*)::bigint from public.team_arena_members m where m.room_id=r.id),
    (select count(*)::bigint from public.team_arena_members m where m.room_id=r.id and m.ready);
end
$function$;

CREATE OR REPLACE FUNCTION private.get_team_arena_words_v1(p_room_id uuid)
 RETURNS TABLE(user_id uuid, display_name text, team integer, word text, normalized_word text, base_points integer, combo integer, created_at timestamp with time zone)
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  v_team int;
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select m.team into v_team
  from public.team_arena_members m
  where m.room_id=p_room_id and m.user_id=v_uid;

  if v_team is null then raise exception 'team_arena_not_participant'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.status='playing' and clock_timestamp()>=r.ends_at then
    r:=private.finish_team_arena_internal_v1(r.id);
  end if;

  return query
  select
    w.user_id,
    p.display_name,
    w.team,
    w.word,
    w.normalized_word,
    w.base_points,
    w.combo,
    w.created_at
  from public.team_arena_words w
  join public.profiles p on p.id=w.user_id
  where w.room_id=r.id
    and (r.status='finished' or w.team=v_team)
  order by w.created_at,w.id;
end
$function$;

CREATE OR REPLACE FUNCTION private.invite_friend_to_team_arena_v1(p_room_id uuid, p_friend_id uuid, p_team integer)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
  v_seat int;
  v_presence text;
  inv public.team_arena_invites;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if p_friend_id is null or p_friend_id=v_uid then raise exception 'invalid_friend'; end if;
  if p_team not in (1,2) then raise exception 'invalid_team'; end if;

  perform private.cleanup_team_arena_v1();

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.host_id<>v_uid then raise exception 'team_arena_host_required'; end if;
  if r.status<>'lobby' or r.expires_at<=clock_timestamp() then
    raise exception 'team_arena_lobby_closed';
  end if;
  if not public.are_friends(v_uid,p_friend_id) then raise exception 'not_friends'; end if;

  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=v_uid and b.blocked_id=p_friend_id)
       or (b.blocker_id=p_friend_id and b.blocked_id=v_uid)
  ) then raise exception 'blocked_relationship'; end if;

  select p.presence_status into v_presence
  from public.profiles p
  where p.id=p_friend_id;

  if v_presence is null then raise exception 'friend_not_found'; end if;
  if v_presence<>'online' then raise exception 'friend_offline'; end if;

  if private.team_arena_user_active_v1(p_friend_id,p_room_id) then
    raise exception 'friend_team_arena_active';
  end if;
  if private.team_arena_other_mode_active_v1(p_friend_id) then
    raise exception 'friend_in_game';
  end if;

  if exists(
    select 1 from public.team_arena_members m
    where m.room_id=r.id and m.user_id=p_friend_id
  ) then raise exception 'friend_already_joined'; end if;

  update public.team_arena_invites i
  set status='expired',responded_at=clock_timestamp()
  where i.status='pending'
    and i.room_id=r.id
    and i.receiver_id=p_friend_id;

  select s.seat into v_seat
  from (values(1),(2)) s(seat)
  where not exists(
    select 1 from public.team_arena_members m
    where m.room_id=r.id and m.team=p_team and m.seat=s.seat
  )
  and not exists(
    select 1 from public.team_arena_invites i
    where i.room_id=r.id
      and i.team=p_team
      and i.seat=s.seat
      and i.status='pending'
      and i.expires_at>clock_timestamp()
  )
  order by s.seat
  limit 1;

  if v_seat is null then raise exception 'team_full'; end if;

  insert into public.team_arena_invites(
    room_id,sender_id,receiver_id,team,seat,expires_at
  )
  values(
    r.id,v_uid,p_friend_id,p_team,v_seat,
    least(r.expires_at,clock_timestamp()+interval '5 minutes')
  )
  returning * into inv;

  insert into public.notification_outbox(user_id,kind,payload)
  values(
    p_friend_id,
    'team_arena_invite',
    jsonb_build_object(
      'invite_id',inv.id,
      'room_id',r.id,
      'sender_id',v_uid,
      'language',r.language,
      'team',inv.team,
      'seat',inv.seat
    )
  );

  return jsonb_build_object(
    'invite_id',inv.id,
    'room_id',inv.room_id,
    'receiver_id',inv.receiver_id,
    'team',inv.team,
    'seat',inv.seat,
    'expires_at',inv.expires_at
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.leave_team_arena_lobby_v1(p_room_id uuid)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.status<>'lobby' then raise exception 'team_arena_lobby_closed'; end if;
  if r.host_id=v_uid then raise exception 'team_arena_host_must_cancel'; end if;

  delete from public.team_arena_members
  where room_id=r.id and user_id=v_uid;

  if not found then raise exception 'team_arena_not_participant'; end if;
  return true;
end
$function$;

CREATE OR REPLACE FUNCTION private.prevent_daily_during_team_arena_v1()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
  if new.status='playing'
     and private.team_arena_user_active_v1(new.user_id,null)
  then
    raise exception 'team_arena_active';
  end if;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION private.prevent_queue_during_team_arena_v1()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
  if new.status='waiting'
     and private.team_arena_user_active_v1(new.user_id,null)
  then
    raise exception 'team_arena_active';
  end if;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION private.prevent_room_during_team_arena_v1()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
begin
  if private.team_arena_user_active_v1(new.host_id,null)
     or (new.guest_id is not null and private.team_arena_user_active_v1(new.guest_id,null))
  then
    raise exception 'team_arena_active';
  end if;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION private.respond_team_arena_invite_v1(p_invite_id uuid, p_accept boolean)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  inv public.team_arena_invites;
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  select i.* into inv
  from public.team_arena_invites i
  where i.id=p_invite_id
  for update;

  if inv.id is null then raise exception 'team_arena_invite_not_found'; end if;
  if inv.receiver_id<>v_uid then raise exception 'not_invite_receiver'; end if;
  if inv.status<>'pending' then raise exception 'invite_not_pending'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=inv.room_id
  for update;

  if r.id is null or r.status<>'lobby' or r.expires_at<=clock_timestamp() then
    update public.team_arena_invites
    set status='expired',responded_at=clock_timestamp()
    where id=inv.id;
    raise exception 'team_arena_lobby_closed';
  end if;

  if not p_accept then
    update public.team_arena_invites
    set status='declined',responded_at=clock_timestamp()
    where id=inv.id;
    return jsonb_build_object('status','declined');
  end if;

  if not public.are_friends(inv.sender_id,inv.receiver_id) then raise exception 'not_friends'; end if;
  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=inv.sender_id and b.blocked_id=inv.receiver_id)
       or (b.blocker_id=inv.receiver_id and b.blocked_id=inv.sender_id)
  ) then raise exception 'blocked_relationship'; end if;

  if private.team_arena_user_active_v1(v_uid,r.id) then
    raise exception 'team_arena_already_active';
  end if;
  if private.team_arena_other_mode_active_v1(v_uid) then
    raise exception 'player_already_in_game';
  end if;

  if exists(
    select 1 from public.team_arena_members m
    where m.room_id=r.id and m.team=inv.team and m.seat=inv.seat
  ) then raise exception 'team_slot_taken'; end if;

  insert into public.team_arena_members(room_id,user_id,team,seat,ready)
  values(r.id,v_uid,inv.team,inv.seat,false)
  on conflict(room_id,user_id) do nothing;

  update public.team_arena_invites
  set status='accepted',responded_at=clock_timestamp()
  where id=inv.id;

  update public.team_arena_invites
  set status='expired',responded_at=clock_timestamp()
  where id<>inv.id
    and receiver_id=v_uid
    and status='pending';

  update public.matchmaking_queue
  set status='cancelled',heartbeat_at=clock_timestamp()
  where user_id=v_uid and status='waiting';

  update public.word_arena_queue
  set status='cancelled',heartbeat_at=clock_timestamp()
  where user_id=v_uid and status='waiting';

  if (
    select count(*) from public.team_arena_members m where m.room_id=r.id
  )>=4 then
    update public.team_arena_invites
    set status='expired',responded_at=clock_timestamp()
    where room_id=r.id and status='pending';
  end if;

  return jsonb_build_object(
    'status','joined',
    'room_id',r.id,
    'team',inv.team,
    'seat',inv.seat
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.set_team_arena_ready_v1(p_room_id uuid, p_ready boolean)
 RETURNS boolean
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.cleanup_team_arena_v1();

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.status<>'lobby' or r.expires_at<=clock_timestamp() then
    raise exception 'team_arena_lobby_closed';
  end if;

  update public.team_arena_members m
  set ready=coalesce(p_ready,false)
  where m.room_id=r.id and m.user_id=v_uid;

  if not found then raise exception 'team_arena_not_participant'; end if;
  return coalesce(p_ready,false);
end
$function$;

CREATE OR REPLACE FUNCTION private.start_team_arena_v1(p_room_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
  v_members int;
  v_ready int;
  v_team_a int;
  v_team_b int;
  v_user uuid;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform pg_advisory_xact_lock(88220031);
  perform private.cleanup_team_arena_v1();

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.host_id<>v_uid then raise exception 'team_arena_host_required'; end if;
  if r.status<>'lobby' or r.expires_at<=clock_timestamp() then
    raise exception 'team_arena_lobby_closed';
  end if;

  select
    count(*)::int,
    count(*) filter(where m.ready)::int,
    count(*) filter(where m.team=1)::int,
    count(*) filter(where m.team=2)::int
  into v_members,v_ready,v_team_a,v_team_b
  from public.team_arena_members m
  where m.room_id=r.id;

  if v_members<>4 or v_team_a<>2 or v_team_b<>2 then
    raise exception 'team_arena_needs_four_players';
  end if;
  if v_ready<>4 then raise exception 'team_arena_not_all_ready'; end if;

  for v_user in
    select m.user_id from public.team_arena_members m where m.room_id=r.id
  loop
    if private.team_arena_other_mode_active_v1(v_user) then
      raise exception 'player_already_in_game';
    end if;
    if private.team_arena_user_active_v1(v_user,r.id) then
      raise exception 'team_arena_already_active';
    end if;
  end loop;

  update public.matchmaking_queue q
  set status='cancelled',heartbeat_at=clock_timestamp()
  where q.user_id in (
    select m.user_id from public.team_arena_members m where m.room_id=r.id
  ) and q.status='waiting';

  update public.word_arena_queue q
  set status='cancelled',heartbeat_at=clock_timestamp()
  where q.user_id in (
    select m.user_id from public.team_arena_members m where m.room_id=r.id
  ) and q.status='waiting';

  update public.team_arena_invites
  set status='expired',responded_at=clock_timestamp()
  where room_id=r.id and status='pending';

  update public.team_arena_rooms tr
  set status='playing',
      letters=public.word_arena_letter_set_v1(r.language),
      starts_at=clock_timestamp()+interval '3 seconds',
      ends_at=clock_timestamp()+interval '63 seconds',
      expires_at=clock_timestamp()+interval '63 seconds'
  where tr.id=r.id
  returning tr.* into r;

  update public.profiles p
  set presence_status='in_game',
      last_seen_at=clock_timestamp()
  where p.id in (
    select m.user_id from public.team_arena_members m where m.room_id=r.id
  );

  return jsonb_build_object(
    'status','playing',
    'room_id',r.id,
    'starts_at',r.starts_at,
    'ends_at',r.ends_at
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.submit_team_arena_word_v1(p_room_id uuid, p_word text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_uid uuid:=auth.uid();
  r public.team_arena_rooms;
  v_team int;
  v_word text:=trim(coalesce(p_word,''));
  v_norm text;
  v_len int;
  v_combo int:=1;
  v_prev public.team_arena_words;
  v_points int;
  v_team_score int;
  v_other_score int;
  v_word_count int;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select m.team into v_team
  from public.team_arena_members m
  where m.room_id=p_room_id and m.user_id=v_uid;

  if v_team is null then raise exception 'team_arena_not_participant'; end if;

  select tr.* into r
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if r.id is null then raise exception 'team_arena_room_not_found'; end if;
  if r.status<>'playing' then
    return jsonb_build_object('accepted',false,'status',r.status);
  end if;

  if clock_timestamp()>=r.ends_at then
    r:=private.finish_team_arena_internal_v1(r.id);
    return jsonb_build_object('accepted',false,'status','finished');
  end if;
  if clock_timestamp()<r.starts_at then raise exception 'team_arena_not_started'; end if;

  v_norm:=public.normalize_game_word(r.language,v_word);
  v_len:=char_length(v_norm);

  if v_len<3 or v_len>10 then raise exception 'team_arena_word_length'; end if;
  if not public.arena_word_fits_letters_v1(v_norm,r.letters) then
    raise exception 'team_arena_letters_mismatch';
  end if;
  if not exists(
    select 1
    from public.dictionary_words d
    where d.language=r.language
      and d.active
      and d.normalized_word=v_norm
  ) then raise exception 'team_arena_invalid_word'; end if;

  if exists(
    select 1 from public.team_arena_words w
    where w.room_id=r.id
      and w.team=v_team
      and w.normalized_word=v_norm
  ) then raise exception 'team_arena_team_duplicate_word'; end if;

  select w.* into v_prev
  from public.team_arena_words w
  where w.room_id=r.id and w.user_id=v_uid
  order by w.created_at desc,w.id desc
  limit 1;

  if v_prev.id is not null
     and clock_timestamp()-v_prev.created_at<=interval '8 seconds'
  then
    v_combo:=least(v_prev.combo+1,99);
  end if;

  v_points:=
    v_len
    +greatest(0,v_len-4)*2
    +least(greatest(v_combo-1,0),4);

  insert into public.team_arena_words(
    room_id,user_id,team,word,normalized_word,base_points,combo
  )
  values(r.id,v_uid,v_team,v_word,v_norm,v_points,v_combo);

  select coalesce(sum(w.base_points),0)::int,count(*)::int
  into v_team_score,v_word_count
  from public.team_arena_words w
  where w.room_id=r.id and w.team=v_team;

  select coalesce(sum(w.base_points),0)::int
  into v_other_score
  from public.team_arena_words w
  where w.room_id=r.id and w.team<>v_team;

  update public.team_arena_rooms tr
  set team_a_score=case when v_team=1 then v_team_score else v_other_score end,
      team_b_score=case when v_team=2 then v_team_score else v_other_score end
  where tr.id=r.id;

  return jsonb_build_object(
    'accepted',true,
    'status','playing',
    'word',v_word,
    'normalized_word',v_norm,
    'base_points',v_points,
    'combo',v_combo,
    'team',v_team,
    'team_score',v_team_score,
    'opponent_score',v_other_score,
    'team_word_count',v_word_count
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.team_arena_other_mode_active_v1(p_user_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
  select
    exists(
      select 1 from public.game_rooms g
      where g.status in ('playing','quiz','final','sudden_death')
        and (g.host_id=p_user_id or g.guest_id=p_user_id)
    )
    or exists(
      select 1 from public.word_arena_rooms a
      where a.status='playing'
        and a.ends_at>clock_timestamp()
        and (a.host_id=p_user_id or a.guest_id=p_user_id)
    )
    or exists(
      select 1 from public.daily_arena_runs d
      where d.user_id=p_user_id
        and d.status='playing'
        and d.ends_at>clock_timestamp()
    )
$function$;

CREATE OR REPLACE FUNCTION private.team_arena_user_active_v1(p_user_id uuid, p_exclude_room uuid DEFAULT NULL::uuid)
 RETURNS boolean
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
  select exists(
    select 1
    from public.team_arena_members m
    join public.team_arena_rooms r on r.id=m.room_id
    where m.user_id=p_user_id
      and (p_exclude_room is null or r.id<>p_exclude_room)
      and (
        (r.status='lobby' and r.expires_at>clock_timestamp())
        or (r.status='playing' and r.ends_at>clock_timestamp())
      )
  )
$function$;

CREATE OR REPLACE FUNCTION public.cancel_team_arena_lobby_v1(p_room_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.cancel_team_arena_lobby_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.create_team_arena_v1(p_language text DEFAULT 'tr'::text)
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.create_team_arena_v1(p_language); $function$;

CREATE OR REPLACE FUNCTION public.get_incoming_team_arena_invites_v1()
 RETURNS TABLE(invite_id uuid, room_id uuid, sender_id uuid, language text, team integer, seat integer, expires_at timestamp with time zone, created_at timestamp with time zone)
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select * from private.get_incoming_team_arena_invites_v1(); $function$;

CREATE OR REPLACE FUNCTION public.get_my_active_team_arena_v1()
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.get_my_active_team_arena_v1(); $function$;

CREATE OR REPLACE FUNCTION public.get_team_arena_members_v1(p_room_id uuid)
 RETURNS TABLE(user_id uuid, display_name text, team integer, seat integer, ready boolean, is_host boolean, presence_status text)
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select * from private.get_team_arena_members_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.get_team_arena_room_v1(p_room_id uuid)
 RETURNS TABLE(room_id uuid, host_id uuid, language text, status text, letters text, starts_at timestamp with time zone, ends_at timestamp with time zone, expires_at timestamp with time zone, team_a_score integer, team_b_score integer, winner_team integer, my_team integer, is_host boolean, member_count bigint, ready_count bigint)
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select * from private.get_team_arena_room_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.get_team_arena_words_v1(p_room_id uuid)
 RETURNS TABLE(user_id uuid, display_name text, team integer, word text, normalized_word text, base_points integer, combo integer, created_at timestamp with time zone)
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select * from private.get_team_arena_words_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.invite_friend_to_team_arena_v1(p_room_id uuid, p_friend_id uuid, p_team integer)
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.invite_friend_to_team_arena_v1(p_room_id,p_friend_id,p_team); $function$;

CREATE OR REPLACE FUNCTION public.leave_team_arena_lobby_v1(p_room_id uuid)
 RETURNS boolean
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.leave_team_arena_lobby_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.respond_team_arena_invite_v1(p_invite_id uuid, p_accept boolean)
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.respond_team_arena_invite_v1(p_invite_id,p_accept); $function$;

CREATE OR REPLACE FUNCTION public.set_team_arena_ready_v1(p_room_id uuid, p_ready boolean)
 RETURNS boolean
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.set_team_arena_ready_v1(p_room_id,p_ready); $function$;

CREATE OR REPLACE FUNCTION public.start_team_arena_v1(p_room_id uuid)
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.start_team_arena_v1(p_room_id); $function$;

CREATE OR REPLACE FUNCTION public.submit_team_arena_word_v1(p_room_id uuid, p_word text)
 RETURNS jsonb
 LANGUAGE sql
 SET search_path TO ''
AS $function$ select private.submit_team_arena_word_v1(p_room_id,p_word); $function$;

drop trigger if exists trg_prevent_daily_arena_during_team_arena_v1 on public.daily_arena_runs;
CREATE TRIGGER trg_prevent_daily_arena_during_team_arena_v1 BEFORE INSERT ON public.daily_arena_runs FOR EACH ROW EXECUTE FUNCTION private.prevent_daily_during_team_arena_v1();

drop trigger if exists trg_prevent_game_room_during_team_arena_v1 on public.game_rooms;
CREATE TRIGGER trg_prevent_game_room_during_team_arena_v1 BEFORE INSERT ON public.game_rooms FOR EACH ROW EXECUTE FUNCTION private.prevent_room_during_team_arena_v1();

drop trigger if exists trg_prevent_matchmaking_queue_during_team_arena_v1 on public.matchmaking_queue;
CREATE TRIGGER trg_prevent_matchmaking_queue_during_team_arena_v1 BEFORE INSERT OR UPDATE OF status ON public.matchmaking_queue FOR EACH ROW EXECUTE FUNCTION private.prevent_queue_during_team_arena_v1();

drop trigger if exists trg_prevent_word_arena_queue_during_team_arena_v1 on public.word_arena_queue;
CREATE TRIGGER trg_prevent_word_arena_queue_during_team_arena_v1 BEFORE INSERT OR UPDATE OF status ON public.word_arena_queue FOR EACH ROW EXECUTE FUNCTION private.prevent_queue_during_team_arena_v1();

drop trigger if exists trg_prevent_word_arena_room_during_team_arena_v1 on public.word_arena_rooms;
CREATE TRIGGER trg_prevent_word_arena_room_during_team_arena_v1 BEFORE INSERT ON public.word_arena_rooms FOR EACH ROW EXECUTE FUNCTION private.prevent_room_during_team_arena_v1();

-- Keep privileged implementation out of the exposed public API.
revoke all on function private.cancel_team_arena_lobby_v1(uuid) from public,anon,authenticated;
revoke all on function private.cleanup_team_arena_v1() from public,anon,authenticated;
revoke all on function private.create_team_arena_v1(text) from public,anon,authenticated;
revoke all on function private.finish_team_arena_internal_v1(uuid) from public,anon,authenticated;
revoke all on function private.get_incoming_team_arena_invites_v1() from public,anon,authenticated;
revoke all on function private.get_my_active_team_arena_v1() from public,anon,authenticated;
revoke all on function private.get_team_arena_members_v1(uuid) from public,anon,authenticated;
revoke all on function private.get_team_arena_room_v1(uuid) from public,anon,authenticated;
revoke all on function private.get_team_arena_words_v1(uuid) from public,anon,authenticated;
revoke all on function private.invite_friend_to_team_arena_v1(uuid,uuid,integer) from public,anon,authenticated;
revoke all on function private.leave_team_arena_lobby_v1(uuid) from public,anon,authenticated;
revoke all on function private.prevent_daily_during_team_arena_v1() from public,anon,authenticated;
revoke all on function private.prevent_queue_during_team_arena_v1() from public,anon,authenticated;
revoke all on function private.prevent_room_during_team_arena_v1() from public,anon,authenticated;
revoke all on function private.respond_team_arena_invite_v1(uuid,boolean) from public,anon,authenticated;
revoke all on function private.set_team_arena_ready_v1(uuid,boolean) from public,anon,authenticated;
revoke all on function private.start_team_arena_v1(uuid) from public,anon,authenticated;
revoke all on function private.submit_team_arena_word_v1(uuid,text) from public,anon,authenticated;
revoke all on function private.team_arena_other_mode_active_v1(uuid) from public,anon,authenticated;
revoke all on function private.team_arena_user_active_v1(uuid,uuid) from public,anon,authenticated;
grant execute on function private.create_team_arena_v1(text) to authenticated;
grant execute on function private.invite_friend_to_team_arena_v1(uuid,uuid,integer) to authenticated;
grant execute on function private.get_incoming_team_arena_invites_v1() to authenticated;
grant execute on function private.respond_team_arena_invite_v1(uuid,boolean) to authenticated;
grant execute on function private.get_team_arena_room_v1(uuid) to authenticated;
grant execute on function private.get_team_arena_members_v1(uuid) to authenticated;
grant execute on function private.set_team_arena_ready_v1(uuid,boolean) to authenticated;
grant execute on function private.start_team_arena_v1(uuid) to authenticated;
grant execute on function private.submit_team_arena_word_v1(uuid,text) to authenticated;
grant execute on function private.get_team_arena_words_v1(uuid) to authenticated;
grant execute on function private.cancel_team_arena_lobby_v1(uuid) to authenticated;
grant execute on function private.leave_team_arena_lobby_v1(uuid) to authenticated;
grant execute on function private.get_my_active_team_arena_v1() to authenticated;

revoke all on function public.create_team_arena_v1(text) from public,anon;
revoke all on function public.invite_friend_to_team_arena_v1(uuid,uuid,integer) from public,anon;
revoke all on function public.get_incoming_team_arena_invites_v1() from public,anon;
revoke all on function public.respond_team_arena_invite_v1(uuid,boolean) from public,anon;
revoke all on function public.get_team_arena_room_v1(uuid) from public,anon;
revoke all on function public.get_team_arena_members_v1(uuid) from public,anon;
revoke all on function public.set_team_arena_ready_v1(uuid,boolean) from public,anon;
revoke all on function public.start_team_arena_v1(uuid) from public,anon;
revoke all on function public.submit_team_arena_word_v1(uuid,text) from public,anon;
revoke all on function public.get_team_arena_words_v1(uuid) from public,anon;
revoke all on function public.cancel_team_arena_lobby_v1(uuid) from public,anon;
revoke all on function public.leave_team_arena_lobby_v1(uuid) from public,anon;
revoke all on function public.get_my_active_team_arena_v1() from public,anon;
grant execute on function public.create_team_arena_v1(text) to authenticated;
grant execute on function public.invite_friend_to_team_arena_v1(uuid,uuid,integer) to authenticated;
grant execute on function public.get_incoming_team_arena_invites_v1() to authenticated;
grant execute on function public.respond_team_arena_invite_v1(uuid,boolean) to authenticated;
grant execute on function public.get_team_arena_room_v1(uuid) to authenticated;
grant execute on function public.get_team_arena_members_v1(uuid) to authenticated;
grant execute on function public.set_team_arena_ready_v1(uuid,boolean) to authenticated;
grant execute on function public.start_team_arena_v1(uuid) to authenticated;
grant execute on function public.submit_team_arena_word_v1(uuid,text) to authenticated;
grant execute on function public.get_team_arena_words_v1(uuid) to authenticated;
grant execute on function public.cancel_team_arena_lobby_v1(uuid) to authenticated;
grant execute on function public.leave_team_arena_lobby_v1(uuid) to authenticated;
grant execute on function public.get_my_active_team_arena_v1() to authenticated;
