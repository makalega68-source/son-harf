-- Son Harf AI takes turns with the player: after the AI won the player's last AI match it plays
-- this one softer (lower skill, more misses); after a player win it plays sharper. The existing
-- level-based skill and the "ease off when ahead" rubber band stay as they are.
do $migration$
declare
  v_def text := pg_get_functiondef('public.bot_take_turn_normal_v1'::regproc);
begin
  if v_def like '%v_ai_won_last%' then
    return;
  end if;
  v_def := replace(v_def, '  v_miss numeric := 0;', '  v_miss numeric := 0;
  v_ai_won_last boolean;');
  v_def := replace(v_def, '  if total_value < 6 then v_skill := v_skill - .12; end if;', '  if total_value < 6 then v_skill := v_skill - .12; end if;
  select case when lr.winner_is_bot then true when lr.winner_id is not null then false end
    into v_ai_won_last
  from public.game_rooms lr
  where lr.host_id = r.host_id and lr.is_bot and lr.id <> r.id and lr.status = ''finished''
  order by coalesce(lr.finished_at, lr.created_at) desc
  limit 1;
  if v_ai_won_last is true then
    v_skill := v_skill - .16;
  elsif v_ai_won_last is false then
    v_skill := v_skill + .08;
  end if;');
  if v_def not like '%v_ai_won_last%into%' and v_def not like '%into v_ai_won_last%' then
    raise exception 'sonharf_ai_take_turns_patch_failed';
  end if;
  execute v_def;
end;
$migration$;
