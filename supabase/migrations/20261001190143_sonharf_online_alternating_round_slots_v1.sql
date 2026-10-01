CREATE OR REPLACE FUNCTION private.sonharf_consume_missed_slot_v1(p_room_id uuid, p_player_id uuid)
RETURNS public.game_rooms
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
declare
  r public.game_rooms;
  round_winner uuid;
  next_round integer;
  next_starter uuid;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if auth.uid() is null or (auth.uid() is distinct from r.host_id and auth.uid() is distinct from r.guest_id)
     then raise exception 'not_participant'; end if;
  if r.is_bot or r.game_mode='expert' or r.status<>'playing' then return r; end if;
  if p_player_id is distinct from r.current_player_id or
     (p_player_id is distinct from r.host_id and p_player_id is distinct from r.guest_id)
     then raise exception 'not_your_turn'; end if;
  -- Round slots count turns. A rejected/expired turn occupies its slot just like a valid move.
  update public.game_rooms
  set host_round_words=host_round_words+case when p_player_id=host_id then 1 else 0 end,
      guest_round_words=guest_round_words+case when p_player_id=guest_id then 1 else 0 end,
      round_word_count=round_word_count+1
  where id=r.id returning * into r;
  if r.host_round_words>=10 and r.guest_round_words>=10 then
    round_winner:=case
      when r.host_round_score>r.guest_round_score then r.host_id
      when r.guest_round_score>r.host_round_score then r.guest_id
      else null
    end;

    update public.profiles
    set total_rounds=total_rounds+1,
        rounds_won=rounds_won+case when id=round_winner then 1 else 0 end
    where id=r.host_id or (not r.is_bot and id=r.guest_id);

    update public.game_rooms
    set host_rounds=host_rounds+case when round_winner=host_id then 1 else 0 end,
        guest_rounds=guest_rounds+case
          when (not is_bot and round_winner=guest_id)
            or (is_bot and guest_round_score>host_round_score) then 1
          else 0
        end
    where id=r.id
    returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(
          r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'match_finished'
        );
      else
        update public.game_rooms
        set status='sudden_death',
            round_word_count=0,
            host_round_words=0,
            guest_round_words=0,
            host_round_score=0,
            guest_round_score=0,
            current_player_id=host_id,
            bot_turn=false,
            turn_deadline=public.sonharf_turn_deadline_for_round_v1(3),
            last_event='sudden_death_started'
        where id=r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round:=r.round_no+1;
    if r.is_bot then
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=case when next_round%2=1 then host_id else null end,
          bot_turn=(next_round%2=0),
          turn_deadline=case
            when next_round%2=1 then public.sonharf_turn_deadline_for_round_v1(next_round)
            else null
          end,
          last_event='round_started'
      where id=r.id
      returning * into r;
    else
      next_starter:=case when next_round%2=1 then r.host_id else r.guest_id end;
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=next_starter,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(next_round),
          last_event='round_started'
      where id=r.id
      returning * into r;
    end if;
    return r;
  end if;

  return r;
end
$function$;
REVOKE ALL ON FUNCTION private.sonharf_consume_missed_slot_v1(uuid,uuid) FROM public,anon,authenticated;

CREATE OR REPLACE FUNCTION public.switch_turn_after_failure(p_room_id uuid, p_reason text)
 RETURNS game_rooms
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  r public.game_rooms;
  next_player uuid;
  previous_round integer;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;

  if r.is_bot then
    if auth.uid()<>r.host_id or r.bot_turn then raise exception 'not_your_turn'; end if;
  else
    if auth.uid()<>r.current_player_id then raise exception 'not_your_turn'; end if;
  end if;

  if r.status='sudden_death' then
    return public.sonharf_finish_room(
      r.id,
      case when r.is_bot then null
           else case when auth.uid()=r.host_id then r.guest_id else r.host_id end end,
      r.is_bot and auth.uid()=r.host_id,
      p_reason
    );
  end if;

  if auth.uid()=r.host_id then
    update public.game_rooms
    set host_score=host_score-1,
        host_round_score=host_round_score-1,
        host_streak=0
    where id=r.id;
  else
    update public.game_rooms
    set guest_score=guest_score-1,
        guest_round_score=guest_round_score-1,
        guest_streak=0
    where id=r.id;
  end if;

  if not r.is_bot and r.game_mode<>'expert' and r.status='playing' then
    previous_round:=r.round_no;
    r:=private.sonharf_consume_missed_slot_v1(r.id,auth.uid());
    if r.status<>'playing' or r.round_no<>previous_round then return r; end if;
  end if;

  if r.is_bot then
    if r.guest_round_words>=10 and r.host_round_words<10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event=p_reason,
          last_event_player_id=auth.uid()
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null,
          last_event=p_reason,
          last_event_player_id=auth.uid()
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
        last_event=p_reason,
        last_event_player_id=auth.uid()
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

CREATE OR REPLACE FUNCTION public.claim_turn_timeout(p_room_id uuid)
 RETURNS game_rooms
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'pg_temp'
AS $function$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty integer;
  next_player uuid;
  reconnect_winner uuid;
  previous_round integer;
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

  if not r.is_bot and r.game_mode<>'expert' and r.status='playing' then
    update public.game_rooms
    set host_round_score=host_round_score-case when timed_out_player=host_id then penalty else 0 end,
        guest_round_score=guest_round_score-case when timed_out_player=guest_id then penalty else 0 end
    where id=r.id;
    previous_round:=r.round_no;
    r:=private.sonharf_consume_missed_slot_v1(r.id,timed_out_player);
    if r.status<>'playing' or r.round_no<>previous_round then return r; end if;
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
$function$
;