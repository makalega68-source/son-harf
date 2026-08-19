create or replace function public.request_rematch(p_room_id uuid)
returns public.game_rooms
language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; me uuid:=auth.uid(); new_room public.game_rooms;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or me not in (r.host_id,r.guest_id) then raise exception 'room_not_found'; end if;
  if r.status <> 'finished' then raise exception 'match_not_finished'; end if;

  select * into new_room from public.game_rooms where rematch_of=r.id order by created_at desc limit 1;
  if new_room.id is not null then return new_room; end if;

  if me=r.host_id then update public.game_rooms set host_rematch=true where id=r.id returning * into r;
  else update public.game_rooms set guest_rematch=true where id=r.id returning * into r; end if;

  if r.host_rematch and r.guest_rematch then
    insert into public.game_rooms(code,host_id,guest_id,status,language,current_player_id,turn_deadline,rematch_of)
    values (upper(substr(md5(random()::text),1,6)),r.host_id,r.guest_id,'playing',r.language,r.host_id,now()+interval '45 seconds',r.id)
    returning * into new_room;
    update public.profiles set presence_status='in_game' where id in (r.host_id,r.guest_id);
    return new_room;
  end if;
  return r;
end $$;

grant execute on function public.request_rematch(uuid) to authenticated;
