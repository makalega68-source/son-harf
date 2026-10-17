-- Son Harf AI matches only advance while the player is in the room, and the stale-room sweep
-- skips them, so a match the player walked away from stayed "Sıra sende" forever. A real-time
-- 15-second game left untouched for 15 minutes is over: the AI takes it. AI matches do not
-- touch wins, losses or rating (sonharf_apply_finish), so nothing else changes.
create or replace function private.sweep_sonharf_abandoned_bot_rooms_v1()
returns integer
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $function$
declare
  r public.game_rooms;
  v_count integer := 0;
begin
  for r in
    select g.*
    from public.game_rooms g
    where coalesce(g.is_bot, false)
      and g.status in ('playing', 'final', 'sudden_death')
      and coalesce(g.last_action_at, g.created_at) < clock_timestamp() - interval '15 minutes'
      and coalesce(g.turn_deadline, g.created_at) < clock_timestamp() - interval '15 minutes'
    for update skip locked
  loop
    begin
      perform public.sonharf_finish_room(r.id, null, true, 'abandoned_by_player');
      v_count := v_count + 1;
    exception when others then
      raise warning 'sweep_sonharf_abandoned_bot_rooms_v1 room %: %', r.id, sqlerrm;
    end;
  end loop;
  return v_count;
end
$function$;

revoke all on function private.sweep_sonharf_abandoned_bot_rooms_v1() from public, anon, authenticated;

select cron.schedule('sonharf_abandoned_bot_sweep_v1', '*/5 * * * *',
  'select private.sweep_sonharf_abandoned_bot_rooms_v1();');
