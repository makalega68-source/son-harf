-- Son Harf: friends list, presence and direct game invitations

alter table public.profiles
  add column if not exists last_seen_at timestamptz,
  add column if not exists presence_status text not null default 'offline'
    check (presence_status in ('offline','online','in_game'));

create table if not exists public.friendships (
  user_id uuid not null references public.profiles(id) on delete cascade,
  friend_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending','accepted','declined','blocked')),
  requested_by uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, friend_id),
  check (user_id <> friend_id)
);

create table if not exists public.game_invites (
  id uuid primary key default gen_random_uuid(),
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check (language in ('tr','en')),
  status text not null default 'pending' check (status in ('pending','accepted','declined','expired','cancelled')),
  room_id uuid references public.game_rooms(id) on delete set null,
  expires_at timestamptz not null default (now() + interval '2 minutes'),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check (sender_id <> receiver_id)
);

alter table public.friendships enable row level security;
alter table public.game_invites enable row level security;

create policy "friendships participants read" on public.friendships
for select to authenticated
using (user_id = auth.uid() or friend_id = auth.uid());

create policy "game invites participants read" on public.game_invites
for select to authenticated
using (sender_id = auth.uid() or receiver_id = auth.uid());

create or replace function public.set_presence(p_status text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_status not in ('offline','online','in_game') then raise exception 'invalid_presence'; end if;
  update public.profiles
  set presence_status = p_status,
      last_seen_at = now(),
      updated_at = now()
  where id = auth.uid();
end;
$$;
grant execute on function public.set_presence(text) to authenticated;

create or replace function public.send_friend_request(p_friend_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_friend_id = auth.uid() then raise exception 'cannot_friend_self'; end if;
  if exists (
    select 1 from public.user_blocks
    where (blocker_id = auth.uid() and blocked_id = p_friend_id)
       or (blocker_id = p_friend_id and blocked_id = auth.uid())
  ) then raise exception 'blocked_relationship'; end if;

  insert into public.friendships(user_id, friend_id, status, requested_by)
  values (least(auth.uid(), p_friend_id), greatest(auth.uid(), p_friend_id), 'pending', auth.uid())
  on conflict (user_id, friend_id)
  do update set status = 'pending', requested_by = excluded.requested_by, updated_at = now();
end;
$$;
grant execute on function public.send_friend_request(uuid) to authenticated;

create or replace function public.respond_friend_request(p_friend_id uuid, p_accept boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  a uuid := least(auth.uid(), p_friend_id);
  b uuid := greatest(auth.uid(), p_friend_id);
begin
  update public.friendships
  set status = case when p_accept then 'accepted' else 'declined' end,
      updated_at = now()
  where user_id = a and friend_id = b
    and status = 'pending'
    and requested_by <> auth.uid();
  if not found then raise exception 'friend_request_not_found'; end if;
end;
$$;
grant execute on function public.respond_friend_request(uuid, boolean) to authenticated;

create or replace function public.remove_friend(p_friend_id uuid)
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.friendships
  where user_id = least(auth.uid(), p_friend_id)
    and friend_id = greatest(auth.uid(), p_friend_id);
$$;
grant execute on function public.remove_friend(uuid) to authenticated;

create or replace function public.invite_friend_to_game(p_friend_id uuid, p_language text)
returns public.game_invites
language plpgsql
security definer
set search_path = public
as $$
declare
  inv public.game_invites;
  f public.friendships;
  p public.profiles;
begin
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;

  select * into f from public.friendships
  where user_id = least(auth.uid(), p_friend_id)
    and friend_id = greatest(auth.uid(), p_friend_id)
    and status = 'accepted';
  if f.user_id is null then raise exception 'not_friends'; end if;

  if exists (
    select 1 from public.user_blocks
    where (blocker_id = auth.uid() and blocked_id = p_friend_id)
       or (blocker_id = p_friend_id and blocked_id = auth.uid())
  ) then raise exception 'blocked_relationship'; end if;

  select * into p from public.profiles where id = p_friend_id;
  if p.id is null then raise exception 'friend_not_found'; end if;
  if p.presence_status = 'in_game' then raise exception 'friend_in_game'; end if;
  if p.presence_status <> 'online' then raise exception 'friend_offline'; end if;

  update public.game_invites
  set status = 'expired', responded_at = now()
  where receiver_id = p_friend_id and status = 'pending' and expires_at < now();

  insert into public.game_invites(sender_id, receiver_id, language)
  values (auth.uid(), p_friend_id, p_language)
  returning * into inv;
  return inv;
end;
$$;
grant execute on function public.invite_friend_to_game(uuid, text) to authenticated;

create or replace function public.respond_game_invite(p_invite_id uuid, p_accept boolean)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  inv public.game_invites;
  r public.game_rooms;
  generated_code text;
  attempts int := 0;
begin
  select * into inv from public.game_invites where id = p_invite_id for update;
  if inv.id is null then raise exception 'invite_not_found'; end if;
  if inv.receiver_id <> auth.uid() then raise exception 'not_invite_receiver'; end if;
  if inv.status <> 'pending' then raise exception 'invite_not_pending'; end if;
  if inv.expires_at < now() then
    update public.game_invites set status='expired', responded_at=now() where id=inv.id;
    raise exception 'invite_expired';
  end if;

  if not p_accept then
    update public.game_invites set status='declined', responded_at=now() where id=inv.id;
    return null;
  end if;

  if exists (
    select 1 from public.game_rooms
    where status in ('playing','quiz','final','sudden_death')
      and (host_id in (inv.sender_id, inv.receiver_id) or guest_id in (inv.sender_id, inv.receiver_id))
  ) then raise exception 'player_already_in_game'; end if;

  loop
    attempts := attempts + 1;
    generated_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
    begin
      insert into public.game_rooms(code, host_id, guest_id, status, current_player_id, turn_deadline, language)
      values (generated_code, inv.sender_id, inv.receiver_id, 'playing', inv.sender_id, now() + interval '45 seconds', inv.language)
      returning * into r;
      exit;
    exception when unique_violation then
      if attempts >= 8 then raise; end if;
    end;
  end loop;

  update public.game_invites
  set status='accepted', room_id=r.id, responded_at=now()
  where id=inv.id;

  update public.profiles set presence_status='in_game', last_seen_at=now()
  where id in (inv.sender_id, inv.receiver_id);

  return r;
end;
$$;
grant execute on function public.respond_game_invite(uuid, boolean) to authenticated;

create index if not exists friendships_user_status_idx on public.friendships(user_id, status);
create index if not exists friendships_friend_status_idx on public.friendships(friend_id, status);
create index if not exists game_invites_receiver_status_idx on public.game_invites(receiver_id, status, expires_at);
create index if not exists profiles_presence_idx on public.profiles(presence_status, last_seen_at);
