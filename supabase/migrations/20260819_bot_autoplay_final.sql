-- Final bot runtime: make bot fallback work without trusting the Android client.
-- Requires 20260819_bot_fallback_reconnect.sql first.

create or replace function public.submit_word_v4(p_room_id uuid,p_word text)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare r public.game_rooms;
begin
  r:=public.submit_word_v3(p_room_id,p_word);
  if r.is_bot and r.bot_turn and r.status in ('playing','final','sudden_death') then
    -- The bot move is still validated and scored entirely on the server.
    r:=public.bot_take_turn(r.id);
  end if;
  return r;
end $$;
grant execute on function public.submit_word_v4(uuid,text) to authenticated;

create or replace function public.answer_trivia_v4(p_round_id uuid,p_answer_index int)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare r public.game_rooms;
begin
  r:=public.answer_trivia_v3(p_round_id,p_answer_index);
  if r.is_bot and r.status='quiz' then
    r:=public.bot_answer_trivia(r.id);
  end if;
  return r;
end $$;
grant execute on function public.answer_trivia_v4(uuid,int) to authenticated;

-- A bot rematch never needs to wait for a second consent signal.
create or replace function public.restart_bot_match(p_room_id uuid)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare old_room public.game_rooms; new_room public.game_rooms; generated_code text; attempts int:=0;
begin
  select * into old_room from public.game_rooms where id=p_room_id for update;
  if old_room.id is null or old_room.host_id<>auth.uid() or not old_room.is_bot then raise exception 'not_bot_match'; end if;
  if old_room.status<>'finished' then raise exception 'match_not_finished'; end if;
  loop
    attempts:=attempts+1; generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
    begin
      insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,host_last_seen,guest_last_seen,bot_turn,rematch_of)
      values(generated_code,old_room.host_id,null,'playing',old_room.host_id,now()+interval '45 seconds',old_room.language,true,coalesce(old_room.bot_name,'KelimeBot'),now(),now(),false,old_room.id)
      returning * into new_room;
      exit;
    exception when unique_violation then if attempts>=8 then raise; end if; end;
  end loop;
  update public.profiles set presence_status='in_game',last_seen_at=now() where id=old_room.host_id;
  return new_room;
end $$;
grant execute on function public.restart_bot_match(uuid) to authenticated;
