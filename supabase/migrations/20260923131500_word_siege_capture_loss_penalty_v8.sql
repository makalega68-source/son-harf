-- Word Siege v8: +2 to the capturer per newly won cube; -1 to the rival per rival-owned cube lost.
-- Word points remain permanent. Ownership counts remain unchanged and continue to drive territory control.

-- Rebuild the territory score ledger for existing games from authoritative move history.
update public.word_siege_games g
set player_one_area_score = greatest(0, coalesce((
      select sum(case
        when m.player_id = g.player_one_id then coalesce(m.area_score, 0)
        else -coalesce(m.opponent_captured, 0)
      end)::integer
      from public.word_siege_moves m
      where m.game_id = g.id
    ), 0)),
    player_two_area_score = greatest(0, coalesce((
      select sum(case
        when m.player_id = g.player_two_id then coalesce(m.area_score, 0)
        else -coalesce(m.opponent_captured, 0)
      end)::integer
      from public.word_siege_moves m
      where m.game_id = g.id
    ), 0));

do $patch_submit$
declare
  v_def text;
  v_src text;
begin
  select pg_get_functiondef(
    'private.submit_word_siege_move_v1(uuid,jsonb,boolean)'::regprocedure
  ) into v_def;
  if v_def is null then
    raise exception 'word_siege_submit_function_missing';
  end if;

  v_def := replace(
    v_def,
    'player_one_area_score = player_one_area_score + case when v_owner = 1 then v_area_score else 0 end,',
    'player_one_area_score = greatest(0, player_one_area_score + case when v_owner = 1 then v_area_score else -v_opponent_captured end),'
  );
  v_def := replace(
    v_def,
    'player_two_area_score = player_two_area_score + case when v_owner = 2 then v_area_score else 0 end,',
    'player_two_area_score = greatest(0, player_two_area_score + case when v_owner = 2 then v_area_score else -v_opponent_captured end),'
  );
  execute v_def;

  select p.prosrc into v_src
  from pg_proc p
  join pg_namespace n on n.oid = p.pronamespace
  where n.nspname = 'private'
    and p.proname = 'submit_word_siege_move_v1'
    and p.oid = 'private.submit_word_siege_move_v1(uuid,jsonb,boolean)'::regprocedure;

  if v_src not like '%player_one_area_score = greatest(0, player_one_area_score + case when v_owner = 1 then v_area_score else -v_opponent_captured end)%'
     or v_src not like '%player_two_area_score = greatest(0, player_two_area_score + case when v_owner = 2 then v_area_score else -v_opponent_captured end)%' then
    raise exception 'word_siege_capture_loss_patch_failed';
  end if;
end
$patch_submit$;

revoke all on function private.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
select pg_notify('pgrst', 'reload schema');
