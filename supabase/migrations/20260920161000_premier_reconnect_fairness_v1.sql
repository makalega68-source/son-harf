begin;

create or replace function public.get_premier_reconnect_clock_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  u uuid := auth.uid();
  r public.game_rooms;
  remaining_ms bigint := 0;
begin
  if u is null then raise exception 'unauthorized'; end if;

  select * into r
  from public.game_rooms
  where id=p_room_id;

  if r.id is null then raise exception 'room_not_found'; end if;
  if u<>r.host_id and u is distinct from r.guest_id then raise exception 'not_participant'; end if;

  if r.reconnect_deadline is not null then
    remaining_ms := greatest(
      0,
      floor(extract(epoch from (r.reconnect_deadline-clock_timestamp()))*1000)::bigint
    );
  end if;

  return jsonb_build_object(
    'remaining_ms',remaining_ms,
    'reconnect_deadline',r.reconnect_deadline
  );
end
$function$;

revoke all on function public.get_premier_reconnect_clock_v1(uuid) from public;
revoke all on function public.get_premier_reconnect_clock_v1(uuid) from anon;
grant execute on function public.get_premier_reconnect_clock_v1(uuid) to authenticated;

create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty integer;
  next_player uuid;
  reconnect_winner uuid;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid()<>r.host_id and (r.guest_id is null or auth.uid()<>r.guest_id) then
    raise exception 'not_participant';
  end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;

  -- A network disconnect has its own authoritative 60-second recovery contract.
  -- Never let the normal 15-second turn timeout defeat that grace period.
  if not r.is_bot
     and r.disconnected_player_id is not null
     and r.reconnect_deadline is not null then
    if r.reconnect_deadline <= clock_timestamp() then
      reconnect_winner := case
        when r.disconnected_player_id=r.host_id then r.guest_id
        else r.host_id
      end;
      return public.sonharf_finish_room(r.id,reconnect_winner,false,'reconnect_timeout');
    end if;

    if r.disconnected_player_id=r.current_player_id then
      return r;
    end if;
  end if;

  if r.turn_deadline is null or r.turn_deadline>=clock_timestamp() then return r; end if;

  timed_out_player:=r.current_player_id;
  penalty:=case when r.status='final' then 2 else 1 end;

  if r.status='sudden_death' then
    update public.game_rooms
    set status='finished',
        winner_id=case
          when r.is_bot and timed_out_player=r.host_id then null
          when timed_out_player=r.host_id then r.guest_id
          else r.host_id
        end,
        winner_is_bot=(r.is_bot and timed_out_player=r.host_id),
        finished_at=clock_timestamp(),
        turn_deadline=null,
        bot_turn=false,
        last_event='turn_expired',
        last_event_player_id=timed_out_player
    where id=r.id
    returning * into r;
    return r;
  end if;

  if timed_out_player=r.host_id then
    update public.game_rooms
    set host_score=host_score-penalty,
        host_streak=0
    where id=r.id;
  elsif timed_out_player=r.guest_id then
    update public.game_rooms
    set guest_score=guest_score-penalty,
        guest_streak=0
    where id=r.id;
  end if;

  if r.is_bot and timed_out_player=r.host_id then
    if r.guest_round_words>=10 and r.host_round_words<10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event='turn_expired',
          last_event_player_id=timed_out_player,
          final_moves_remaining=case
            when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
            else final_moves_remaining
          end
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null,
          last_event='turn_expired',
          last_event_player_id=timed_out_player,
          final_moves_remaining=case
            when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
            else final_moves_remaining
          end
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case
      when timed_out_player=r.host_id then r.guest_id
      else r.host_id
    end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
        last_event='turn_expired',
        last_event_player_id=timed_out_player,
        final_moves_remaining=case
          when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
          else final_moves_remaining
        end
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

commit;
