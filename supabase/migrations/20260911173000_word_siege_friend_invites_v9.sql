-- Kelime Kuşatması v9: friend invitations backed by authoritative Word Siege game creation.
-- Direct table writes are denied; authenticated clients use the public RPC wrappers only.

create table if not exists public.word_siege_invites (
  id uuid primary key default gen_random_uuid(),
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check (language in ('tr','en')),
  status text not null default 'pending'
    check (status in ('pending','accepted','declined','expired','cancelled')),
  game_id uuid references public.word_siege_games(id) on delete set null,
  expires_at timestamptz not null default (now() + interval '10 minutes'),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check (sender_id <> receiver_id)
);

create index if not exists word_siege_invites_receiver_status_idx
  on public.word_siege_invites(receiver_id, status, expires_at);
create index if not exists word_siege_invites_sender_status_idx
  on public.word_siege_invites(sender_id, status, expires_at);

alter table public.word_siege_invites enable row level security;
revoke all on public.word_siege_invites from anon, authenticated;

drop policy if exists word_siege_invites_participants_read on public.word_siege_invites;
create policy word_siege_invites_participants_read
on public.word_siege_invites
for select
to authenticated
using ((select auth.uid()) in (sender_id, receiver_id));

grant select on public.word_siege_invites to authenticated;

create or replace function public.invite_friend_to_word_siege_v1(
  p_friend_id uuid,
  p_language text default 'tr'
)
returns public.word_siege_invites
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_language text;
  inv public.word_siege_invites;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if p_friend_id is null or p_friend_id = v_uid then raise exception 'word_siege_invalid_friend'; end if;
  v_language := lower(coalesce(p_language, 'tr'));
  if v_language not in ('tr','en') then raise exception 'word_siege_invalid_language'; end if;

  if not exists (select 1 from public.profiles p where p.id = v_uid) then
    raise exception 'word_siege_profile_required';
  end if;
  if not exists (select 1 from public.profiles p where p.id = p_friend_id) then
    raise exception 'word_siege_friend_not_found';
  end if;

  if not exists (
    select 1
    from public.friendships f
    where f.user_id = least(v_uid, p_friend_id)
      and f.friend_id = greatest(v_uid, p_friend_id)
      and f.status = 'accepted'
  ) then raise exception 'word_siege_not_friends'; end if;

  if exists (
    select 1
    from public.user_blocks b
    where (b.blocker_id = v_uid and b.blocked_id = p_friend_id)
       or (b.blocker_id = p_friend_id and b.blocked_id = v_uid)
  ) then raise exception 'word_siege_blocked_relationship'; end if;

  update public.word_siege_invites
  set status = 'expired', responded_at = now()
  where status = 'pending'
    and expires_at < now()
    and (sender_id = v_uid or receiver_id = v_uid or sender_id = p_friend_id or receiver_id = p_friend_id);

  if exists (
    select 1
    from public.word_siege_invites i
    where i.status = 'pending'
      and i.expires_at >= now()
      and ((i.sender_id = v_uid and i.receiver_id = p_friend_id)
        or (i.sender_id = p_friend_id and i.receiver_id = v_uid))
  ) then raise exception 'word_siege_invite_pending'; end if;

  insert into public.word_siege_invites(sender_id, receiver_id, language)
  values (v_uid, p_friend_id, v_language)
  returning * into inv;

  return inv;
end
$$;

create or replace function public.respond_word_siege_invite_v1(
  p_invite_id uuid,
  p_accept boolean
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  inv public.word_siege_invites;
  r public.word_siege_games;
  v_bag text;
  v_one_rack text;
  v_two_rack text;
  v_sender_active integer;
  v_receiver_active integer;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  select * into inv
  from public.word_siege_invites
  where id = p_invite_id
  for update;

  if inv.id is null then raise exception 'word_siege_invite_not_found'; end if;
  if inv.receiver_id <> v_uid then raise exception 'word_siege_not_invite_receiver'; end if;
  if inv.status <> 'pending' then raise exception 'word_siege_invite_not_pending'; end if;

  if inv.expires_at < now() then
    update public.word_siege_invites
    set status = 'expired', responded_at = now()
    where id = inv.id;
    raise exception 'word_siege_invite_expired';
  end if;

  if not coalesce(p_accept, false) then
    update public.word_siege_invites
    set status = 'declined', responded_at = now()
    where id = inv.id;
    return null;
  end if;

  if not exists (
    select 1
    from public.friendships f
    where f.user_id = least(inv.sender_id, inv.receiver_id)
      and f.friend_id = greatest(inv.sender_id, inv.receiver_id)
      and f.status = 'accepted'
  ) then raise exception 'word_siege_not_friends'; end if;

  if exists (
    select 1
    from public.user_blocks b
    where (b.blocker_id = inv.sender_id and b.blocked_id = inv.receiver_id)
       or (b.blocker_id = inv.receiver_id and b.blocked_id = inv.sender_id)
  ) then raise exception 'word_siege_blocked_relationship'; end if;

  perform pg_advisory_xact_lock(
    hashtextextended('word_siege_friend:' || least(inv.sender_id, inv.receiver_id)::text || ':' || greatest(inv.sender_id, inv.receiver_id)::text, 0)
  );

  select count(*)::integer into v_sender_active
  from public.word_siege_games g
  where g.status in ('waiting','playing')
    and inv.sender_id in (g.player_one_id, g.player_two_id);

  select count(*)::integer into v_receiver_active
  from public.word_siege_games g
  where g.status in ('waiting','playing')
    and inv.receiver_id in (g.player_one_id, g.player_two_id);

  if v_sender_active >= 10 or v_receiver_active >= 10 then
    raise exception 'word_siege_active_limit';
  end if;

  v_bag := private.word_siege_new_bag_v1(inv.language);
  v_one_rack := substring(v_bag from 1 for 7);
  v_two_rack := substring(v_bag from 8 for 7);
  v_bag := substring(v_bag from 15);

  insert into public.word_siege_games(
    player_one_id,
    player_two_id,
    status,
    language,
    current_player_id,
    board,
    bag,
    player_one_rack,
    player_two_rack,
    last_action,
    last_action_player_id
  ) values (
    inv.sender_id,
    inv.receiver_id,
    'playing',
    inv.language,
    inv.sender_id,
    private.word_siege_new_board_v1(),
    v_bag,
    v_one_rack,
    v_two_rack,
    'friend_game_started',
    inv.sender_id
  )
  returning * into r;

  update public.word_siege_invites
  set status = 'accepted', game_id = r.id, responded_at = now()
  where id = inv.id;

  update public.profiles
  set presence_status = 'in_game', last_seen_at = now(), updated_at = now()
  where id in (inv.sender_id, inv.receiver_id);

  return r;
end
$$;

revoke all on function public.invite_friend_to_word_siege_v1(uuid,text) from public, anon, authenticated;
revoke all on function public.respond_word_siege_invite_v1(uuid,boolean) from public, anon, authenticated;
grant execute on function public.invite_friend_to_word_siege_v1(uuid,text) to authenticated;
grant execute on function public.respond_word_siege_invite_v1(uuid,boolean) to authenticated;
