-- Word Siege v5 / 5: wrap the canonical v4 move engine with zone ownership and Yıkım Hamlesi.

create or replace function private.submit_word_siege_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r_before public.word_siege_games;
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_owner integer;
  v_before_meter integer;
  v_before_onslaught boolean;
  v_raw_score integer := 0;
  v_final_score integer := 0;
  v_board jsonb;
  v_flipped integer[] := '{}'::integer[];
  v_neutral integer := 0;
  v_opponent integer := 0;
  v_gained_points integer := 0;
  v_meter integer := 0;
  v_onslaught boolean := false;
  v_triggered boolean := false;
  v_one_area integer := 0;
  v_two_area integer := 0;
  v_one_zone_score integer := 0;
  v_two_zone_score integer := 0;
  v_one_total integer := 0;
  v_two_total integer := 0;
  v_winner uuid;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  select * into r_before
  from public.word_siege_games
  where id = p_game_id
  for update;

  if r_before.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r_before.player_one_id, r_before.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  v_owner := case when v_uid = r_before.player_one_id then 1 else 2 end;
  v_before_meter := case when v_owner = 1 then r_before.player_one_conquest_meter else r_before.player_two_conquest_meter end;
  v_before_onslaught := case when v_owner = 1 then r_before.player_one_onslaught_active else r_before.player_two_onslaught_active end;

  -- Dictionary validation, rack validation, connectivity and Scrabble scoring remain canonical here.
  r := private.submit_word_siege_move_cell_core_v5(p_game_id, p_placements, p_horizontal);

  v_raw_score := case
    when v_owner = 1 then greatest(0, r.player_one_word_score - r_before.player_one_word_score)
    else greatest(0, r.player_two_word_score - r_before.player_two_word_score)
  end;
  v_final_score := v_raw_score * case when v_before_onslaught then 2 else 1 end;

  select c.board, c.flipped_zone_ids, c.neutral_zones, c.opponent_zones, c.gained_zone_points
  into v_board, v_flipped, v_neutral, v_opponent, v_gained_points
  from private.word_siege_claim_zones_v5(r_before.board, r.board, v_owner, p_placements) c;

  v_meter := greatest(0, least(2, v_before_meter));
  v_onslaught := false;
  if cardinality(v_flipped) > 0 then
    v_meter := v_meter + 1;
    if v_meter >= 3 then
      v_meter := 0;
      v_onslaught := true;
      v_triggered := true;
    end if;
  end if;

  v_one_area := private.word_siege_zone_count_v5(v_board, 1);
  v_two_area := private.word_siege_zone_count_v5(v_board, 2);
  v_one_zone_score := private.word_siege_zone_score_v5(v_board, 1);
  v_two_zone_score := private.word_siege_zone_score_v5(v_board, 2);

  update public.word_siege_games
  set board = v_board,
      player_one_word_score = case
        when v_owner = 1 then r_before.player_one_word_score + v_final_score
        else player_one_word_score
      end,
      player_two_word_score = case
        when v_owner = 2 then r_before.player_two_word_score + v_final_score
        else player_two_word_score
      end,
      player_one_area = v_one_area,
      player_two_area = v_two_area,
      player_one_area_score = v_one_zone_score,
      player_two_area_score = v_two_zone_score,
      player_one_conquest_meter = case when v_owner = 1 then v_meter else player_one_conquest_meter end,
      player_two_conquest_meter = case when v_owner = 2 then v_meter else player_two_conquest_meter end,
      player_one_onslaught_active = case when v_owner = 1 then v_onslaught else player_one_onslaught_active end,
      player_two_onslaught_active = case when v_owner = 2 then v_onslaught else player_two_onslaught_active end,
      turn_started_at = case when status = 'playing' then now() else null end,
      turn_deadline = case when status = 'playing' then now() + interval '45 seconds' else null end,
      updated_at = now()
  where id = p_game_id
  returning * into r;

  update public.word_siege_moves
  set raw_word_score = v_raw_score,
      word_score = v_final_score,
      neutral_captured = v_neutral,
      opponent_captured = v_opponent,
      area_score = v_gained_points,
      total_score = v_final_score + v_gained_points,
      captured_cells = cardinality(v_flipped),
      zones_flipped = v_flipped,
      onslaught_triggered = v_triggered,
      onslaught_consumed = v_before_onslaught
  where game_id = p_game_id
    and player_id = v_uid
    and move_number = r.move_count;

  -- The inner core can finish rack-empty before this post-processing step. Re-evaluate only that
  -- score-derived result under permanent-word + current-zone scoring.
  if r.status = 'finished' and r.finish_reason = 'rack_empty' then
    v_one_total := r.player_one_word_score + r.player_one_area_score;
    v_two_total := r.player_two_word_score + r.player_two_area_score;
    v_winner := case
      when v_one_total > v_two_total then r.player_one_id
      when v_two_total > v_one_total then r.player_two_id
      when r.player_one_area_score > r.player_two_area_score then r.player_one_id
      when r.player_two_area_score > r.player_one_area_score then r.player_two_id
      when r.player_one_area > r.player_two_area then r.player_one_id
      when r.player_two_area > r.player_one_area then r.player_two_id
      else null
    end;

    update public.word_siege_games
    set winner_id = v_winner,
        loser_id = case
          when v_winner is null then null
          when v_winner = player_one_id then player_two_id
          else player_one_id
        end,
        turn_started_at = null,
        turn_deadline = null,
        updated_at = now()
    where id = p_game_id
    returning * into r;
  end if;

  return r;
end
$$;

revoke all on function private.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
