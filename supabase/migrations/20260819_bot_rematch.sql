-- Instant rematch for server-controlled bot games.
create or replace function public.restart_bot_match(p_room_id uuid)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare old_room public.game_rooms; r public.game_rooms; generated_code text; attempts int:=0;
begin
  select * into old_room from public.game_rooms where id=p_room_id for update;
  if old_room.id is null or not old_room.is_bot or old_room.host_id<>auth.uid() then raise exception 'not_bot_match'; end if;
  if old_room.status<>'finished' then raise exception 'match_not_finished'; end if;
  loop
    attempts:=attempts+1;
    generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
    begin
      insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,rematch_of,host_last_seen,guest_last_seen,bot_turn)
      values(generated_code,auth.uid(),null,'playing',auth.uid(),now()+interval '45 seconds',old_room.language,true,
        coalesce(old_room.bot_name,case when old_room.language='tr' then 'KelimeBot' else 'WordBot' end),old_room.id,now(),now(),false)
      returning * into r;
      exit;
    exception when unique_violation then if attempts>=8 then raise; end if; end;
  end loop;
  update public.profiles set presence_status='in_game',last_seen_at=now() where id=auth.uid();
  return r;
end $$;
grant execute on function public.restart_bot_match(uuid) to authenticated;