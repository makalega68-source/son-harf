-- Word Siege v5: retain the existing board-score trigger, but make it zone-authoritative.
-- This prevents any board update path from restoring the legacy cell-count score.

create or replace function private.word_siege_sync_area_scores_v1()
returns trigger
language plpgsql
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if jsonb_typeof(new.board) = 'array' and jsonb_array_length(new.board) = 225 then
    new.player_one_area := private.word_siege_zone_count_v5(new.board, 1);
    new.player_two_area := private.word_siege_zone_count_v5(new.board, 2);
    new.player_one_area_score := private.word_siege_zone_score_v5(new.board, 1);
    new.player_two_area_score := private.word_siege_zone_score_v5(new.board, 2);
  end if;
  return new;
end
$$;

revoke all on function private.word_siege_sync_area_scores_v1() from public, anon, authenticated;
