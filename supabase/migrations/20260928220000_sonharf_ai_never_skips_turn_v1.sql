-- Son Harf AI no longer "misses" a turn on purpose. A deliberate miss handed the turn straight
-- back to the player with no word on the board, which players saw as being made to play twice.
-- The AI's level is still matched through its word choice.
do $migration$
declare
  v_def text;
begin
  v_def := pg_get_functiondef('public.bot_take_turn_normal_v1(uuid)'::regprocedure);
  if position('if random() < v_miss then' in v_def) = 0 then
    raise exception 'ai miss branch not found';
  end if;
  execute replace(v_def, 'if random() < v_miss then', 'if false and random() < v_miss then');
end
$migration$;
