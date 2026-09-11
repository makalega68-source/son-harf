-- AUDIT-ONLY LIVE SNAPSHOT. DO NOT EXECUTE FROM THIS PATH.
-- Live version: 20260906160322
-- Live name: core_duel_bot_server_authority_v5

create or replace function public.submit_word_v5(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare r public.game_rooms;
begin
  r := public.submit_word_v3(p_room_id, p_word);
  if r.is_bot and r.bot_turn and r.status in ('playing','sudden_death') then
    r := public.bot_take_turn(r.id);
  end if;
  return r;
end
$$;

create or replace function public.claim_turn_timeout_v2(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare r public.game_rooms;
begin
  r := public.claim_turn_timeout(p_room_id);
  if r.is_bot and r.bot_turn and r.status in ('playing','sudden_death') then
    r := public.bot_take_turn(r.id);
  end if;
  return r;
end
$$;

revoke all on function public.submit_word_v5(uuid,text) from public,anon,authenticated;
revoke all on function public.claim_turn_timeout_v2(uuid) from public,anon,authenticated;
grant execute on function public.submit_word_v5(uuid,text) to authenticated;
grant execute on function public.claim_turn_timeout_v2(uuid) to authenticated;
