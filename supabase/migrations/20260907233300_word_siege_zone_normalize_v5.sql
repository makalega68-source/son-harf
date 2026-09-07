-- Word Siege v5 / 4: migrate active 15x15 cell territory to deterministic whole zones.
-- Mixed legacy zones become neutral; letters and bonus state are preserved.

with normalized as (
  select
    g.id,
    private.word_siege_normalize_zones_v5(g.board) as board
  from public.word_siege_games g
  where g.status in ('waiting', 'playing')
    and jsonb_typeof(g.board) = 'array'
    and jsonb_array_length(g.board) = 225
)
update public.word_siege_games g
set board = n.board,
    player_one_area = private.word_siege_zone_count_v5(n.board, 1),
    player_two_area = private.word_siege_zone_count_v5(n.board, 2),
    player_one_area_score = private.word_siege_zone_score_v5(n.board, 1),
    player_two_area_score = private.word_siege_zone_score_v5(n.board, 2),
    player_one_conquest_meter = 0,
    player_two_conquest_meter = 0,
    player_one_onslaught_active = false,
    player_two_onslaught_active = false,
    updated_at = now()
from normalized n
where g.id = n.id;

-- Preserve the v4 dictionary/connectivity/Scrabble implementation as an inner core.
alter function private.submit_word_siege_move_v1(uuid, jsonb, boolean)
  rename to submit_word_siege_move_cell_core_v5;

revoke all on function private.submit_word_siege_move_cell_core_v5(uuid, jsonb, boolean) from public, anon, authenticated;
