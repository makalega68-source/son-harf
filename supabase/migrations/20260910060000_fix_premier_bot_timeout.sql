-- Keep Premier playable after an expired player turn, including bot matches.
create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty int;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and (r.guest_id is null or auth.uid() <> r.guest_id) then
    raise exception 'not_participant';
  end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;
  if r.turn_deadline is null or r.turn_deadline >= clock_timestamp() then return r; end if;

  timed_out_player := r.current_player_id;
  penalty := case when r.status = 'final' then 2 else 1 end;

  if r.status = 'sudden_death' then
    update public.game_rooms
    set status = 'finished',
        winner_id = case
          when r.is_bot and timed_out_player = r.host_id then null
          when timed_out_player = r.host_id then r.guest_id
          else r.host_id
        end,
        winner_is_bot = (r.is_bot and timed_out_player = r.host_id),
        finished_at = clock_timestamp(), turn_deadline = null, bot_turn = false,
        last_event = 'turn_expired', last_event_player_id = timed_out_player
    where id = r.id returning * into r;
    return r;
  end if;

  update public.game_rooms
  set host_score = case when timed_out_player = r.host_id then host_score - penalty else host_score end,
      guest_score = case when timed_out_player = r.guest_id then guest_score - penalty else guest_score end,
      host_streak = case when timed_out_player = r.host_id then 0 else host_streak end,
      guest_streak = case when timed_out_player = r.guest_id then 0 else guest_streak end,
      current_player_id = case
        when r.is_bot and timed_out_player = r.host_id then null
        when timed_out_player = r.host_id then r.guest_id
        else r.host_id
      end,
      bot_turn = (r.is_bot and timed_out_player = r.host_id),
      turn_deadline = case
        when r.is_bot and timed_out_player = r.host_id then null
        else public.sonharf_turn_deadline(r.game_mode)
      end,
      last_event = 'turn_expired',
      last_event_player_id = timed_out_player,
      final_moves_remaining = case
        when r.status = 'final' and final_moves_remaining > 0 then final_moves_remaining - 1
        else final_moves_remaining
      end
  where id = r.id returning * into r;

  return r;
end;
$$;

revoke all on function public.claim_turn_timeout(uuid) from public, anon;
grant execute on function public.claim_turn_timeout(uuid) to authenticated, service_role;
select pg_notify('pgrst', 'reload schema');
