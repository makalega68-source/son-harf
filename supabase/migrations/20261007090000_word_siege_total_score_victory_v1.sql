-- Kelime Kuşatması: the winner is the player with the higher total (word points + cube points),
-- as every rule text in the app and the practice mode already say. The server picked the winner
-- by cube count alone, so a player with more points could be shown a defeat.
-- Equal totals: more cubes wins; equal again is a draw. Forfeits keep their explicit winner.
do $migration$
declare
  def text;
  patched text;
begin
  def := pg_get_functiondef('private.finish_word_siege_game_v1(uuid,text,uuid)'::regprocedure);
  if def like '%v_one_total%' then return; end if;
  patched := replace(def,
    $old$    v_winner := case
      when r.player_one_area > r.player_two_area then r.player_one_id
      when r.player_two_area > r.player_one_area then r.player_two_id
      else null
    end;$old$,
    $new$    -- Total score decides (word points + cube points); cubes break a tie.
    v_winner := case
      when (greatest(0, r.player_one_word_score) + greatest(0, r.player_one_area_score))
         > (greatest(0, r.player_two_word_score) + greatest(0, r.player_two_area_score)) then r.player_one_id
      when (greatest(0, r.player_two_word_score) + greatest(0, r.player_two_area_score))
         > (greatest(0, r.player_one_word_score) + greatest(0, r.player_one_area_score)) then r.player_two_id
      when r.player_one_area > r.player_two_area then r.player_one_id
      when r.player_two_area > r.player_one_area then r.player_two_id
      else null
    end;
    -- v_one_total marker: total-score victory applied.$new$);
  if patched = def then raise exception 'winner rule not found in finish_word_siege_game_v1'; end if;
  execute patched;
end
$migration$;
