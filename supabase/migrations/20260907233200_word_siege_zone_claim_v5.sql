-- Word Siege v5 / 3: whole-zone claim and legacy-zone normalization functions.

create or replace function private.word_siege_claim_zones_v5(
  p_before_board jsonb,
  p_after_board jsonb,
  p_owner integer,
  p_placements jsonb
)
returns table(
  board jsonb,
  flipped_zone_ids integer[],
  neutral_zones integer,
  opponent_zones integer,
  gained_zone_points integer
)
language plpgsql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_board jsonb := p_after_board;
  v_zone integer;
  v_previous_owner integer;
  v_zone_row integer;
  v_zone_col integer;
  v_row integer;
  v_col integer;
  v_index integer;
  v_flipped integer[] := '{}'::integer[];
  v_neutral integer := 0;
  v_opponent integer := 0;
  v_points integer := 0;
begin
  foreach v_zone in array private.word_siege_touched_zones_v5(p_placements) loop
    v_previous_owner := private.word_siege_zone_owner_v5(p_before_board, v_zone);
    if v_previous_owner = p_owner then continue; end if;

    if v_previous_owner = 0 then v_neutral := v_neutral + 1;
    else v_opponent := v_opponent + 1;
    end if;

    v_zone_row := (v_zone / 5) * 3;
    v_zone_col := (v_zone % 5) * 3;
    for v_row in v_zone_row..(v_zone_row + 2) loop
      for v_col in v_zone_col..(v_zone_col + 2) loop
        v_index := v_row * 15 + v_col;
        v_board := jsonb_set(
          v_board,
          array[v_index::text],
          (v_board -> v_index) || jsonb_build_object('owner', p_owner),
          false
        );
      end loop;
    end loop;

    v_flipped := array_append(v_flipped, v_zone);
    v_points := v_points + private.word_siege_zone_value_v5(v_zone);
  end loop;

  return query select v_board, v_flipped, v_neutral, v_opponent, v_points;
end
$$;

create or replace function private.word_siege_normalize_zones_v5(p_board jsonb)
returns jsonb
language plpgsql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_board jsonb := p_board;
  v_zone integer;
  v_owner integer;
  v_zone_row integer;
  v_zone_col integer;
  v_row integer;
  v_col integer;
  v_index integer;
begin
  if jsonb_typeof(p_board) <> 'array' or jsonb_array_length(p_board) <> 225 then return p_board; end if;

  for v_zone in 0..24 loop
    v_owner := private.word_siege_zone_owner_v5(p_board, v_zone);
    v_zone_row := (v_zone / 5) * 3;
    v_zone_col := (v_zone % 5) * 3;
    for v_row in v_zone_row..(v_zone_row + 2) loop
      for v_col in v_zone_col..(v_zone_col + 2) loop
        v_index := v_row * 15 + v_col;
        v_board := jsonb_set(
          v_board,
          array[v_index::text],
          (v_board -> v_index) || jsonb_build_object('owner', v_owner),
          false
        );
      end loop;
    end loop;
  end loop;

  return v_board;
end
$$;

revoke all on function private.word_siege_claim_zones_v5(jsonb, jsonb, integer, jsonb) from public, anon, authenticated;
revoke all on function private.word_siege_normalize_zones_v5(jsonb) from public, anon, authenticated;
