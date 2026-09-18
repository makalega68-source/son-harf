-- PRO friend-list authority without breaking incoming requests/invites.
-- Accepted friendship rows are visible to the participant only when the account is PRO/VIP.
-- Pending friendship rows remain visible to participants so incoming request handling still works.

create or replace function public.can_use_pro_friend_list_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select p_user_id is not null and exists (
    select 1
    from public.profiles p
    where p.id = p_user_id
      and p.is_vip = true
  );
$$;

revoke all on function public.can_use_pro_friend_list_v1(uuid) from public, anon;
grant execute on function public.can_use_pro_friend_list_v1(uuid) to authenticated, service_role;

alter table public.friendships enable row level security;
drop policy if exists "friendships participants read" on public.friendships;
drop policy if exists "friendships pro accepted read v1" on public.friendships;
create policy "friendships pro accepted read v1"
on public.friendships
for select
to authenticated
using (
  (user_id = auth.uid() or friend_id = auth.uid())
  and (
    status = 'pending'
    or public.can_use_pro_friend_list_v1(auth.uid())
  )
);

create or replace function public.get_vip_friendships()
returns setof public.friendships
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if not public.can_use_pro_friend_list_v1(auth.uid()) then raise exception 'pro_friend_list_required'; end if;
  return query
  select f.*
  from public.friendships f
  where f.user_id = auth.uid() or f.friend_id = auth.uid();
end;
$$;

revoke all on function public.get_vip_friendships() from public, anon;
grant execute on function public.get_vip_friendships() to authenticated, service_role;

-- Prevent non-PRO users from creating a friendship relationship by calling the RPC directly.
-- Receiving/responding to an incoming request is intentionally not blocked.
create or replace function public.enforce_pro_friend_request_sender_v1()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'pg_temp'
as $$
begin
  if auth.uid() is not null
     and new.status = 'pending'
     and new.requested_by = auth.uid()
     and not public.can_use_pro_friend_list_v1(auth.uid()) then
    raise exception 'pro_friend_list_required';
  end if;
  return new;
end;
$$;

revoke all on function public.enforce_pro_friend_request_sender_v1() from public, anon, authenticated;
grant execute on function public.enforce_pro_friend_request_sender_v1() to service_role;

drop trigger if exists friendships_pro_sender_guard_v1 on public.friendships;
create trigger friendships_pro_sender_guard_v1
before insert or update on public.friendships
for each row execute function public.enforce_pro_friend_request_sender_v1();

-- All of these tables represent invitations initiated from an existing friend relationship.
-- Server/service operations with no auth.uid() are left untouched; normal clients require PRO.
create or replace function public.enforce_pro_friend_game_invite_sender_v1()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'pg_temp'
as $$
begin
  if auth.uid() is not null
     and new.sender_id = auth.uid()
     and not public.can_use_pro_friend_list_v1(auth.uid()) then
    raise exception 'pro_friend_list_required';
  end if;
  return new;
end;
$$;

revoke all on function public.enforce_pro_friend_game_invite_sender_v1() from public, anon, authenticated;
grant execute on function public.enforce_pro_friend_game_invite_sender_v1() to service_role;

do $$
begin
  if to_regclass('public.game_invites') is not null then
    drop trigger if exists game_invites_pro_sender_guard_v1 on public.game_invites;
    create trigger game_invites_pro_sender_guard_v1
      before insert on public.game_invites
      for each row execute function public.enforce_pro_friend_game_invite_sender_v1();
  end if;
  if to_regclass('public.word_arena_invites') is not null then
    drop trigger if exists word_arena_invites_pro_sender_guard_v1 on public.word_arena_invites;
    create trigger word_arena_invites_pro_sender_guard_v1
      before insert on public.word_arena_invites
      for each row execute function public.enforce_pro_friend_game_invite_sender_v1();
  end if;
  if to_regclass('public.word_siege_invites') is not null then
    drop trigger if exists word_siege_invites_pro_sender_guard_v1 on public.word_siege_invites;
    create trigger word_siege_invites_pro_sender_guard_v1
      before insert on public.word_siege_invites
      for each row execute function public.enforce_pro_friend_game_invite_sender_v1();
  end if;
end $$;
