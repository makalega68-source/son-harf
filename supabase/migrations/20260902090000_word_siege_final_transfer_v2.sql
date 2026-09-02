-- Final Kelime Kusatmasi scoring: every newly gained cube transfers 2 points.
-- Cumulative area-score columns remain non-negative earned-point ledgers; the visible/net score is
-- word_score + own_earned_cube_points - rival_earned_cube_points.

create or replace function private.word_siege_area_delta_v1(
  p_before_board jsonb,
  p_after_board jsonb,
  p_owner integer
)
returns table(neutral_captured integer, opponent_captured integer, area_score integer)
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  with ownership as (
    select
      i,
      coalesce((p_before_board -> i ->> 'owner')::integer, 0) as before_owner,
      coalesce((p_after_board -> i ->> 'owner')::integer, 0) as after_owner
    from generate_series(0, 80) i
  ), counts as (
    select
      count(*) filter (where before_owner = 0 and after_owner = p_owner)::integer as neutral_count,
      count(*) filter (where before_owner not in (0, p_owner) and after_owner = p_owner)::integer as opponent_count
    from ownership
  )
  select neutral_count, opponent_count, (neutral_count + opponent_count) * 2
  from counts
$$;

revoke all on function private.word_siege_area_delta_v1(jsonb, jsonb, integer) from public, anon, authenticated;

create or replace function private.finish_word_siege_game_v1(
  p_game_id uuid,
  p_reason text,
  p_forfeit_winner uuid default null
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_winner uuid;
  v_loser uuid;
  v_one_total integer;
  v_two_total integer;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status = 'finished' then return r; end if;
  if r.status <> 'playing' then raise exception 'word_siege_not_playing'; end if;

  v_one_total := r.player_one_word_score + r.player_one_area_score - r.player_two_area_score;
  v_two_total := r.player_two_word_score + r.player_two_area_score - r.player_one_area_score;
  if p_forfeit_winner is not null then
    if p_forfeit_winner not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_invalid_winner'; end if;
    v_winner := p_forfeit_winner;
  else
    v_winner := case
      when v_one_total > v_two_total then r.player_one_id
      when v_two_total > v_one_total then r.player_two_id
      when r.player_one_area > r.player_two_area then r.player_one_id
      when r.player_two_area > r.player_one_area then r.player_two_id
      else null
    end;
  end if;
  v_loser := case
    when v_winner is null then null
    when v_winner = r.player_one_id then r.player_two_id
    else r.player_one_id
  end;

  update public.word_siege_games
  set status = 'finished',
      current_player_id = null,
      winner_id = v_winner,
      loser_id = v_loser,
      finish_reason = left(coalesce(p_reason, 'completed'), 40),
      result_applied = true,
      turn_started_at = null,
      turn_deadline = null,
      finished_at = clock_timestamp(),
      updated_at = clock_timestamp()
  where id = r.id
  returning * into r;
  return r;
end
$$;

revoke all on function private.finish_word_siege_game_v1(uuid, text, uuid) from public, anon, authenticated;
select pg_notify('pgrst', 'reload schema');
