-- Rival + Word Arena friend challenges v2
-- Adds friend-to-Arena invites, same-rival rematches and Arena-aware arch-rival stats.
-- No purchasable power, rating bonus or match advantage is introduced.

alter table public.word_arena_rooms
  add column if not exists rematch_of uuid references public.word_arena_rooms(id) on delete set null,
  add column if not exists host_rematch_at timestamptz,
  add column if not exists guest_rematch_at timestamptz,
  add column if not exists rematch_room_id uuid references public.word_arena_rooms(id) on delete set null;

create index if not exists word_arena_rooms_rematch_of_idx
  on public.word_arena_rooms(rematch_of)
  where rematch_of is not null;

create index if not exists word_arena_rooms_rematch_room_idx
  on public.word_arena_rooms(rematch_room_id)
  where rematch_room_id is not null;

create table if not exists public.word_arena_invites (
  id uuid primary key default gen_random_uuid(),
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check(language in ('tr','en')),
  status text not null default 'pending' check(status in ('pending','accepted','declined','expired')),
  room_id uuid references public.word_arena_rooms(id) on delete set null,
  expires_at timestamptz not null default (now()+interval '2 minutes'),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check(sender_id<>receiver_id)
);

create index if not exists word_arena_invites_receiver_pending_idx
  on public.word_arena_invites(receiver_id,created_at desc)
  where status='pending';

create index if not exists word_arena_invites_sender_idx
  on public.word_arena_invites(sender_id,created_at desc);

create index if not exists word_arena_invites_room_idx
  on public.word_arena_invites(room_id)
  where room_id is not null;

alter table public.word_arena_invites enable row level security;
revoke all on public.word_arena_invites from anon,authenticated;

create or replace function public.invite_friend_to_word_arena_v1(
  p_friend_id uuid,
  p_language text default 'tr'
)
returns table(
  invite_id uuid,
  receiver_id uuid,
  expires_at timestamptz
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(trim(coalesce(p_language,'tr')))='en' then 'en' else 'tr' end;
  v_presence text;
  inv public.word_arena_invites;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if p_friend_id is null or p_friend_id=v_uid then raise exception 'invalid_friend'; end if;
  if not public.are_friends(v_uid,p_friend_id) then raise exception 'not_friends'; end if;

  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=v_uid and b.blocked_id=p_friend_id)
       or (b.blocker_id=p_friend_id and b.blocked_id=v_uid)
  ) then
    raise exception 'blocked_relationship';
  end if;

  if exists(
    select 1 from public.game_rooms g
    where g.status in ('playing','quiz','final','sudden_death')
      and (g.host_id in (v_uid,p_friend_id) or g.guest_id in (v_uid,p_friend_id))
  ) or exists(
    select 1 from public.word_arena_rooms a
    where a.status='playing'
      and a.ends_at>now()
      and (a.host_id in (v_uid,p_friend_id) or a.guest_id in (v_uid,p_friend_id))
  ) then
    raise exception 'player_already_in_game';
  end if;

  select p.presence_status into v_presence
  from public.profiles p
  where p.id=p_friend_id;

  if v_presence is null then raise exception 'friend_not_found'; end if;

  update public.word_arena_invites wai
  set status='expired',responded_at=now()
  where wai.status='pending'
    and (
      wai.expires_at<now()
      or (wai.sender_id=v_uid and wai.receiver_id=p_friend_id)
    );

  insert into public.word_arena_invites(
    sender_id,receiver_id,language,expires_at
  )
  values(
    v_uid,
    p_friend_id,
    v_lang,
    case when v_presence='online'
      then now()+interval '2 minutes'
      else now()+interval '24 hours'
    end
  )
  returning * into inv;

  insert into public.notification_outbox(user_id,kind,payload)
  values(
    p_friend_id,
    'word_arena_invite',
    jsonb_build_object(
      'invite_id',inv.id,
      'sender_id',v_uid,
      'language',v_lang
    )
  );

  return query
  select inv.id,inv.receiver_id,inv.expires_at;
end
$$;

create or replace function public.get_incoming_word_arena_invites_v1()
returns table(
  invite_id uuid,
  sender_id uuid,
  receiver_id uuid,
  language text,
  expires_at timestamptz,
  created_at timestamptz
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  update public.word_arena_invites wai
  set status='expired',responded_at=now()
  where wai.receiver_id=v_uid
    and wai.status='pending'
    and wai.expires_at<now();

  return query
  select
    wai.id,
    wai.sender_id,
    wai.receiver_id,
    wai.language,
    wai.expires_at,
    wai.created_at
  from public.word_arena_invites wai
  where wai.receiver_id=v_uid
    and wai.status='pending'
    and wai.expires_at>=now()
  order by wai.created_at
  limit 10;
end
$$;

create or replace function public.respond_word_arena_invite_v1(
  p_invite_id uuid,
  p_accept boolean
)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  inv public.word_arena_invites;
  r public.word_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into inv
  from public.word_arena_invites
  where id=p_invite_id
  for update;

  if inv.id is null then raise exception 'invite_not_found'; end if;
  if inv.receiver_id<>v_uid then raise exception 'not_invite_receiver'; end if;
  if inv.status<>'pending' then raise exception 'invite_not_pending'; end if;

  if inv.expires_at<now() then
    update public.word_arena_invites
    set status='expired',responded_at=now()
    where id=inv.id;
    raise exception 'invite_expired';
  end if;

  if not p_accept then
    update public.word_arena_invites
    set status='declined',responded_at=now()
    where id=inv.id;
    return jsonb_build_object('status','declined');
  end if;

  if not public.are_friends(inv.sender_id,inv.receiver_id) then
    raise exception 'not_friends';
  end if;

  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=inv.sender_id and b.blocked_id=inv.receiver_id)
       or (b.blocker_id=inv.receiver_id and b.blocked_id=inv.sender_id)
  ) then
    raise exception 'blocked_relationship';
  end if;

  if exists(
    select 1 from public.game_rooms g
    where g.status in ('playing','quiz','final','sudden_death')
      and (
        g.host_id in (inv.sender_id,inv.receiver_id)
        or g.guest_id in (inv.sender_id,inv.receiver_id)
      )
  ) or exists(
    select 1 from public.word_arena_rooms a
    where a.status='playing'
      and a.ends_at>now()
      and (
        a.host_id in (inv.sender_id,inv.receiver_id)
        or a.guest_id in (inv.sender_id,inv.receiver_id)
      )
  ) then
    raise exception 'player_already_in_game';
  end if;

  insert into public.word_arena_rooms(
    host_id,guest_id,language,letters,status,starts_at,ends_at
  )
  values(
    inv.sender_id,
    inv.receiver_id,
    inv.language,
    public.word_arena_letter_set_v1(inv.language),
    'playing',
    now()+interval '3 seconds',
    now()+interval '63 seconds'
  )
  returning * into r;

  insert into public.word_arena_queue(
    user_id,language,status,room_id,queued_at,heartbeat_at
  )
  values
    (r.host_id,r.language,'matched',r.id,now(),now()),
    (r.guest_id,r.language,'matched',r.id,now(),now())
  on conflict(user_id) do update set
    language=excluded.language,
    status='matched',
    room_id=excluded.room_id,
    queued_at=excluded.queued_at,
    heartbeat_at=excluded.heartbeat_at;

  update public.profiles
  set presence_status='in_game',last_seen_at=now()
  where id in (r.host_id,r.guest_id);

  update public.word_arena_invites
  set status='accepted',room_id=r.id,responded_at=now()
  where id=inv.id;

  update public.word_arena_invites
  set status='expired',responded_at=now()
  where id<>inv.id
    and status='pending'
    and (
      sender_id in (r.host_id,r.guest_id)
      or receiver_id in (r.host_id,r.guest_id)
    );

  return jsonb_build_object('status','matched','room_id',r.id);
end
$$;

create or replace function public.request_word_arena_rematch_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  r public.word_arena_rooms;
  nr public.word_arena_rooms;
  v_other_requested timestamptz;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into r
  from public.word_arena_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'arena_room_not_found'; end if;
  if v_uid not in (r.host_id,r.guest_id) then raise exception 'arena_not_participant'; end if;
  if r.status<>'finished' or not r.result_applied then raise exception 'match_not_finished'; end if;

  if r.rematch_room_id is not null then
    return jsonb_build_object('status','matched','room_id',r.rematch_room_id);
  end if;

  if v_uid=r.host_id then
    update public.word_arena_rooms
    set host_rematch_at=now()
    where id=r.id
    returning * into r;
    v_other_requested:=r.guest_rematch_at;
  else
    update public.word_arena_rooms
    set guest_rematch_at=now()
    where id=r.id
    returning * into r;
    v_other_requested:=r.host_rematch_at;
  end if;

  if v_other_requested is null
     or v_other_requested<now()-interval '2 minutes'
  then
    return jsonb_build_object('status','waiting');
  end if;

  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=r.host_id and b.blocked_id=r.guest_id)
       or (b.blocker_id=r.guest_id and b.blocked_id=r.host_id)
  ) then
    raise exception 'blocked_relationship';
  end if;

  if exists(
    select 1 from public.game_rooms g
    where g.status in ('playing','quiz','final','sudden_death')
      and (
        g.host_id in (r.host_id,r.guest_id)
        or g.guest_id in (r.host_id,r.guest_id)
      )
  ) or exists(
    select 1 from public.word_arena_rooms a
    where a.id<>r.id
      and a.status='playing'
      and a.ends_at>now()
      and (
        a.host_id in (r.host_id,r.guest_id)
        or a.guest_id in (r.host_id,r.guest_id)
      )
  ) then
    raise exception 'player_already_in_game';
  end if;

  insert into public.word_arena_rooms(
    host_id,guest_id,language,letters,status,starts_at,ends_at,rematch_of
  )
  values(
    r.host_id,
    r.guest_id,
    r.language,
    public.word_arena_letter_set_v1(r.language),
    'playing',
    now()+interval '3 seconds',
    now()+interval '63 seconds',
    r.id
  )
  returning * into nr;

  update public.word_arena_rooms
  set rematch_room_id=nr.id
  where id=r.id;

  insert into public.word_arena_queue(
    user_id,language,status,room_id,queued_at,heartbeat_at
  )
  values
    (nr.host_id,nr.language,'matched',nr.id,now(),now()),
    (nr.guest_id,nr.language,'matched',nr.id,now(),now())
  on conflict(user_id) do update set
    language=excluded.language,
    status='matched',
    room_id=excluded.room_id,
    queued_at=excluded.queued_at,
    heartbeat_at=excluded.heartbeat_at;

  update public.profiles
  set presence_status='in_game',last_seen_at=now()
  where id in (nr.host_id,nr.guest_id);

  return jsonb_build_object('status','matched','room_id',nr.id);
end
$$;

create or replace function public.poll_word_arena_rematch_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  r public.word_arena_rooms;
  v_mine timestamptz;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into r
  from public.word_arena_rooms
  where id=p_room_id;

  if r.id is null then raise exception 'arena_room_not_found'; end if;
  if v_uid not in (r.host_id,r.guest_id) then raise exception 'arena_not_participant'; end if;

  if r.rematch_room_id is not null then
    return jsonb_build_object('status','matched','room_id',r.rematch_room_id);
  end if;

  v_mine:=case
    when v_uid=r.host_id then r.host_rematch_at
    else r.guest_rematch_at
  end;

  if v_mine is null then
    return jsonb_build_object('status','idle');
  end if;

  if v_mine<now()-interval '2 minutes' then
    return jsonb_build_object('status','expired');
  end if;

  return jsonb_build_object('status','waiting');
end
$$;

create or replace function public.cancel_word_arena_rematch_v1(p_room_id uuid)
returns boolean
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  r public.word_arena_rooms;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into r
  from public.word_arena_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'arena_room_not_found'; end if;
  if v_uid not in (r.host_id,r.guest_id) then raise exception 'arena_not_participant'; end if;
  if r.rematch_room_id is not null then return false; end if;

  if v_uid=r.host_id then
    update public.word_arena_rooms
    set host_rematch_at=null
    where id=r.id;
  else
    update public.word_arena_rooms
    set guest_rematch_at=null
    where id=r.id;
  end if;

  return true;
end
$$;

create or replace function public.get_arch_rival_v1()
returns table(
  opponent_id uuid,
  display_name text,
  matches integer,
  wins integer,
  losses integer,
  my_points integer,
  their_points integer,
  last_played_at timestamptz
)
language sql
security definer
set search_path=public,pg_temp
as $$
with duels as (
  select
    case when g.host_id=(select auth.uid()) then g.guest_id else g.host_id end opponent_id,
    (g.winner_id=(select auth.uid())) won,
    (g.winner_id is not null and g.winner_id<>(select auth.uid())) lost,
    case when g.host_id=(select auth.uid()) then g.host_score else g.guest_score end my_points,
    case when g.host_id=(select auth.uid()) then g.guest_score else g.host_score end their_points,
    coalesce(g.finished_at,g.created_at) played_at
  from public.game_rooms g
  where g.status='finished'
    and coalesce(g.is_bot,false)=false
    and g.host_id is not null
    and g.guest_id is not null
    and (
      g.host_id=(select auth.uid())
      or g.guest_id=(select auth.uid())
    )

  union all

  select
    case when a.host_id=(select auth.uid()) then a.guest_id else a.host_id end opponent_id,
    (a.winner_id=(select auth.uid())) won,
    (a.winner_id is not null and a.winner_id<>(select auth.uid())) lost,
    case when a.host_id=(select auth.uid()) then a.host_score else a.guest_score end my_points,
    case when a.host_id=(select auth.uid()) then a.guest_score else a.host_score end their_points,
    coalesce(a.finished_at,a.ends_at,a.created_at) played_at
  from public.word_arena_rooms a
  where a.status='finished'
    and a.result_applied
    and (
      a.host_id=(select auth.uid())
      or a.guest_id=(select auth.uid())
    )
),
agg as (
  select
    d.opponent_id,
    count(*)::int matches,
    count(*) filter(where d.won)::int wins,
    count(*) filter(where d.lost)::int losses,
    coalesce(sum(d.my_points),0)::int my_points,
    coalesce(sum(d.their_points),0)::int their_points,
    max(d.played_at) last_played_at
  from duels d
  where d.opponent_id is not null
  group by d.opponent_id
)
select
  a.opponent_id,
  p.display_name,
  a.matches,
  a.wins,
  a.losses,
  a.my_points,
  a.their_points,
  a.last_played_at
from agg a
join public.profiles p on p.id=a.opponent_id
order by a.matches desc,a.last_played_at desc
limit 1
$$;

revoke all on function public.invite_friend_to_word_arena_v1(uuid,text) from public,anon;
revoke all on function public.get_incoming_word_arena_invites_v1() from public,anon;
revoke all on function public.respond_word_arena_invite_v1(uuid,boolean) from public,anon;
revoke all on function public.request_word_arena_rematch_v1(uuid) from public,anon;
revoke all on function public.poll_word_arena_rematch_v1(uuid) from public,anon;
revoke all on function public.cancel_word_arena_rematch_v1(uuid) from public,anon;
revoke all on function public.get_arch_rival_v1() from public,anon;

grant execute on function public.invite_friend_to_word_arena_v1(uuid,text) to authenticated;
grant execute on function public.get_incoming_word_arena_invites_v1() to authenticated;
grant execute on function public.respond_word_arena_invite_v1(uuid,boolean) to authenticated;
grant execute on function public.request_word_arena_rematch_v1(uuid) to authenticated;
grant execute on function public.poll_word_arena_rematch_v1(uuid) to authenticated;
grant execute on function public.cancel_word_arena_rematch_v1(uuid) to authenticated;
grant execute on function public.get_arch_rival_v1() to authenticated;
