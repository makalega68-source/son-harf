-- Bil Bakalim appears after each 10 accepted words in every classic duel mode.
create or replace function public.submit_word_v3_legacy(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  before_room public.game_rooms;
  r public.game_rooms;
  clean text;
begin
  select * into before_room from public.game_rooms where id=p_room_id;
  if before_room.id is null then raise exception 'room_not_found'; end if;

  clean:=public.normalize_game_word(before_room.language,p_word);
  if not public.sonharf_word_allowed(before_room.language,clean) then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;

  if before_room.game_mode='expert' then
    r:=public.submit_word_expert_v1(p_room_id,p_word);
  else
    r:=public.submit_word_normal_v3(p_room_id,p_word);
  end if;

  if r.valid_word_count>before_room.valid_word_count
     and r.valid_word_count>0
     and mod(r.valid_word_count,10)=0
     and r.status='playing' then
    r:=public.start_bilbakalim_round_v1(r.id,r.valid_word_count);
  end if;

  return r;
end
$$;

revoke all on function public.submit_word_v3_legacy(uuid,text) from public,anon;
grant execute on function public.submit_word_v3_legacy(uuid,text) to authenticated,service_role;
