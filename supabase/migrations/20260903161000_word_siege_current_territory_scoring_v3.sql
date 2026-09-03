-- Word Siege v3 scoring correction.
-- Word points are permanent. Territory is worth 2 points per cube currently owned.
-- Rival captures of neutral cubes must not reduce the other player's score.
-- Capturing one rival-owned cube reduces that rival's visible/final score by exactly 2 points.

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

  v_one_total := r.player_one_word_score + (r.player_one_area * 2);
  v_two_total := r.player_two_word_score + (r.player_two_area * 2);
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
