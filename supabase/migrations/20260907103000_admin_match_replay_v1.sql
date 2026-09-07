-- Admin-only deterministic match replay payloads for Classic and Word Siege.
-- Read-only: no gameplay state is mutated.

create or replace function public.admin_match_replay_v1(p_match_id uuid)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_room public.game_rooms%rowtype;
  v_siege public.word_siege_games%rowtype;
  v_events jsonb;
begin
  if v_uid is null or not public.is_admin(v_uid) then raise exception 'admin_required'; end if;
  if p_match_id is null then raise exception 'match_id_required'; end if;

  select * into v_room from public.game_rooms where id=p_match_id;
  if found then
    select coalesce(jsonb_agg(jsonb_build_object(
      'seq',w.id,
      'player_id',w.player_id,
      'word',w.word,
      'normalized_word',w.normalized_word,
      'is_bot',w.is_bot,
      'created_at',w.created_at
    ) order by w.id),'[]'::jsonb)
    into v_events
    from public.game_words w where w.room_id=p_match_id;

    return jsonb_build_object(
      'match_type','classic',
      'match_id',v_room.id,
      'status',v_room.status,
      'language',v_room.language,
      'host_id',v_room.host_id,
      'guest_id',v_room.guest_id,
      'winner_id',v_room.winner_id,
      'winner_is_bot',v_room.winner_is_bot,
      'host_score',v_room.host_score,
      'guest_score',v_room.guest_score,
      'host_rounds',v_room.host_rounds,
      'guest_rounds',v_room.guest_rounds,
      'is_bot',v_room.is_bot,
      'created_at',v_room.created_at,
      'finished_at',v_room.finished_at,
      'events',v_events
    );
  end if;

  select * into v_siege from public.word_siege_games where id=p_match_id;
  if found then
    select coalesce(jsonb_agg(jsonb_build_object(
      'seq',coalesce(m.move_number,m.id::integer),
      'move_id',m.id,
      'player_id',m.player_id,
      'primary_word',m.primary_word,
      'formed_words',m.formed_words,
      'placed_tiles',m.placed_tiles,
      'word_score',m.word_score,
      'captured_cells',m.captured_cells,
      'neutral_captured',m.neutral_captured,
      'opponent_captured',m.opponent_captured,
      'area_score',m.area_score,
      'total_score',m.total_score,
      'created_at',m.created_at
    ) order by coalesce(m.move_number,m.id::integer),m.id),'[]'::jsonb)
    into v_events
    from public.word_siege_moves m where m.game_id=p_match_id;

    return jsonb_build_object(
      'match_type','word_siege',
      'match_id',v_siege.id,
      'status',v_siege.status,
      'language',v_siege.language,
      'player_one_id',v_siege.player_one_id,
      'player_two_id',v_siege.player_two_id,
      'winner_id',v_siege.winner_id,
      'loser_id',v_siege.loser_id,
      'player_one_word_score',v_siege.player_one_word_score,
      'player_two_word_score',v_siege.player_two_word_score,
      'player_one_area_score',v_siege.player_one_area_score,
      'player_two_area_score',v_siege.player_two_area_score,
      'move_count',v_siege.move_count,
      'finish_reason',v_siege.finish_reason,
      'created_at',v_siege.created_at,
      'finished_at',v_siege.finished_at,
      'events',v_events
    );
  end if;

  raise exception 'match_not_found';
end $$;

revoke all on function public.admin_match_replay_v1(uuid) from public,anon;
grant execute on function public.admin_match_replay_v1(uuid) to authenticated,service_role;
