-- Son Harf game stabilization foundation
-- 2026-09-03
-- Goals:
-- 1) stale timeout claims must never penalize a later turn,
-- 2) private rooms can be paused for long-form social play,
-- 3) social friend surfaces can enforce VIP/PRO entitlement server-side.

alter table public.game_rooms
  add column if not exists room_mode text not null default 'ranked'
    check (room_mode in ('ranked','private')),
  add column if not exists paused_at timestamptz,
  add column if not exists paused_by uuid references public.profiles(id) on delete set null,
  add column if not exists paused_turn_player_id uuid references public.profiles(id) on delete set null,
  add column if not exists paused_turn_remaining_ms bigint;

alter table public.game_rooms
  drop constraint if exists game_rooms_status_check;

alter table public.game_rooms
  add constraint game_rooms_status_check
  check (status in ('waiting','playing','quiz','final','sudden_death','paused','finished','cancelled'));

-- Exact-turn timeout claim. The caller must identify the turn/deadline that expired.
-- A delayed request from an older turn becomes a no-op instead of penalizing the new turn.
create or replace function public.claim_turn_timeout_v2(
  p_room_id uuid,
  p_expected_player_id uuid,
  p_expected_deadline timestamptz
)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty int;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;

  if r.status not in ('playing','final','sudden_death') then return r; end if;
  if r.turn_deadline is null then return r; end if;

  -- Critical stale-request guards.
  if r.current_player_id is distinct from p_expected_player_id then return r; end if;
  if r.turn_deadline is distinct from p_expected_deadline then return r; end if;
  if r.turn_deadline >= now() then return r; end if;

  timed_out_player := r.current_player_id;
  penalty := case when r.status = 'final' then 2 else 1 end;

  if r.status = 'sudden_death' then
    update public.game_rooms
       set status = 'finished',
           winner_id = case when timed_out_player = host_id then guest_id else host_id end,
           finished_at = now(),
           turn_deadline = null,
           last_event = 'turn_expired',
           last_event_player_id = timed_out_player
     where id = r.id
     returning * into r;
    return r;
  end if;

  if timed_out_player = r.host_id then
    update public.game_rooms
       set host_score = host_score - penalty,
           host_streak = 0
     where id = r.id;
  elsif timed_out_player = r.guest_id then
    update public.game_rooms
       set guest_score = guest_score - penalty,
           guest_streak = 0
     where id = r.id;
  else
    -- Never guess which side should be penalized.
    return r;
  end if;

  update public.game_rooms
     set current_player_id = case when timed_out_player = host_id then guest_id else host_id end,
         turn_deadline = now() + interval '45 seconds',
         last_event = 'turn_expired',
         last_event_player_id = timed_out_player,
         final_moves_remaining = case
           when status = 'final' and final_moves_remaining > 0 then final_moves_remaining - 1
           else final_moves_remaining
         end
   where id = r.id
   returning * into r;

  return r;
end;
$$;

grant execute on function public.claim_turn_timeout_v2(uuid, uuid, timestamptz) to authenticated;

-- Private rooms are social/non-ranked. Either participant can pause; while paused there is no deadline.
create or replace function public.pause_private_room(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  remaining_ms bigint;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  if r.room_mode <> 'private' then raise exception 'private_room_required'; end if;
  if r.status = 'paused' then return r; end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;

  remaining_ms := case
    when r.turn_deadline is null then null
    else greatest(0, (extract(epoch from (r.turn_deadline - now())) * 1000)::bigint)
  end;

  update public.game_rooms
     set status = 'paused',
         paused_at = now(),
         paused_by = auth.uid(),
         paused_turn_player_id = current_player_id,
         paused_turn_remaining_ms = remaining_ms,
         turn_deadline = null,
         last_event = 'private_room_paused',
         last_event_player_id = auth.uid()
   where id = r.id
   returning * into r;

  return r;
end;
$$;

grant execute on function public.pause_private_room(uuid) to authenticated;

create or replace function public.resume_private_room(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  resume_status text;
  resume_deadline timestamptz;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  if r.room_mode <> 'private' then raise exception 'private_room_required'; end if;
  if r.status <> 'paused' then return r; end if;

  -- Private rooms resume as normal word play unless they were already in a final/sudden state.
  -- Existing score/final counters remain untouched.
  resume_status := case
    when r.final_moves_remaining > 0 then 'final'
    else 'playing'
  end;

  resume_deadline := now() + make_interval(secs => greatest(5, coalesce(r.paused_turn_remaining_ms, 45000) / 1000)::int);

  update public.game_rooms
     set status = resume_status,
         current_player_id = coalesce(paused_turn_player_id, current_player_id),
         turn_deadline = resume_deadline,
         paused_at = null,
         paused_by = null,
         paused_turn_player_id = null,
         paused_turn_remaining_ms = null,
         last_event = 'private_room_resumed',
         last_event_player_id = auth.uid()
   where id = r.id
   returning * into r;

  return r;
end;
$$;

grant execute on function public.resume_private_room(uuid) to authenticated;

-- VIP-only social list gate. The UI will call this instead of reading the full friendships table directly.
create or replace function public.get_vip_friendships()
returns setof public.friendships
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.is_vip = true
  ) then
    raise exception 'vip_required';
  end if;

  return query
  select f.*
  from public.friendships f
  where f.user_id = auth.uid() or f.friend_id = auth.uid();
end;
$$;

grant execute on function public.get_vip_friendships() to authenticated;
