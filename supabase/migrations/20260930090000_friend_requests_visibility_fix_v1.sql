-- Friend requests never reached the other player.
--
-- The read policy "friendships pro and series scoped read v2" calls
-- public.has_series_game_access_v1(), but execute on that helper was revoked from
-- `authenticated` (20260919043000_word_siege_series_game_v1.sql). Postgres checks function
-- permissions for the whole policy expression, so every SELECT on public.friendships
-- failed with "permission denied for function has_series_game_access_v1" (HTTP 403):
-- nobody saw incoming requests, friend lists were empty and the "EKLE" button never
-- turned into "BEKLİYOR".
--
-- The helper only answers true/false for a user id and is already SECURITY DEFINER.
grant execute on function public.has_series_game_access_v1(uuid) to authenticated;

-- Because the list could not be read, players pressed "EKLE" again on people who were
-- already friends, and the old upsert turned an accepted friendship back into "pending".
-- Now: an accepted friendship is left alone, and if the other player had already sent a
-- request, sending one back simply accepts it.
create or replace function public.send_friend_request(p_friend_id uuid)
returns void
language plpgsql
security definer
set search_path to 'public'
as $$
declare
  v_uid uuid := auth.uid();
  v_existing public.friendships;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if p_friend_id is null then raise exception 'friend_required'; end if;
  if p_friend_id = v_uid then raise exception 'cannot_friend_self'; end if;
  if exists (
    select 1 from public.user_blocks
    where (blocker_id = v_uid and blocked_id = p_friend_id)
       or (blocker_id = p_friend_id and blocked_id = v_uid)
  ) then raise exception 'blocked_relationship'; end if;

  select * into v_existing
  from public.friendships
  where user_id = least(v_uid, p_friend_id)
    and friend_id = greatest(v_uid, p_friend_id)
  for update;

  if v_existing.user_id is not null then
    if v_existing.status = 'accepted' then
      return;
    end if;
    if v_existing.status = 'pending' and v_existing.requested_by = p_friend_id then
      update public.friendships
      set status = 'accepted', updated_at = now()
      where user_id = v_existing.user_id and friend_id = v_existing.friend_id;
      return;
    end if;
  end if;

  insert into public.friendships(user_id, friend_id, status, requested_by)
  values (least(v_uid, p_friend_id), greatest(v_uid, p_friend_id), 'pending', v_uid)
  on conflict (user_id, friend_id)
  do update set status = 'pending', requested_by = excluded.requested_by, updated_at = now()
  where public.friendships.status <> 'accepted';
end;
$$;

revoke all on function public.send_friend_request(uuid) from public, anon;
grant execute on function public.send_friend_request(uuid) to authenticated, service_role;
