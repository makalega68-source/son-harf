-- Package 7: read-only Word Siege move preview.
-- Uses the same validation/scoring helpers as the real submit path.

create or replace function private.word_siege_score_word_base_v1(
  p_board jsonb,
  p_placements jsonb,
  p_rack text,
  p_cells integer[]
)
returns integer
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_index integer;
  v_total integer := 0;
begin
  foreach v_index in array p_cells loop
    v_total := v_total + private.word_siege_letter_value_v1(
      private.word_siege_letter_at_v1(p_board, p_placements, p_rack, v_index)
    );
  end loop;
  return v_total;
end
$$;

revoke all on function private.word_siege_score_word_base_v1(jsonb, jsonb, text, integer[]) from public, anon, authenticated;

create or replace function private.word_siege_preview_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean
)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_owner integer;
  v_rack text;
  v_board jsonb;
  v_indices integer[];
  v_anchor integer;
  v_main_cells integer[];
  v_cross_cells integer[];
  v_words text[] := array[]::text[];
  v_word text;
  v_index integer;
  v_i integer;
  v_cell jsonb;
  v_word_score integer := 0;
  v_base_score integer := 0;
  v_bonus_score integer := 0;
  v_before_area integer := 0;
  v_after_area integer := 0;
  v_area_delta integer := 0;
  v_captured integer[] := array[]::integer[];
  v_new_area integer[] := array[]::integer[];
  v_bonus_cells integer[] := array[]::integer[];
  v_letter text;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  -- Package 3 canonical validation pass: no state mutation occurs here.
  perform private.word_siege_prevalidate_move_v2(p_game_id, p_placements, p_horizontal);

  select * into r from public.word_siege_games where id = p_game_id;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  v_owner := case when v_uid = r.player_one_id then 1 else 2 end;
  v_rack := case when v_owner = 1 then r.player_one_rack else r.player_two_rack end;
  v_board := r.board;
  v_before_area := case when v_owner = 1 then r.player_one_area else r.player_two_area end;

  select array_agg((e ->> 'index')::integer order by ordinality)
    into v_indices
  from jsonb_array_elements(p_placements) with ordinality as x(e, ordinality);
  v_anchor := v_indices[1];

  v_main_cells := private.word_siege_collect_cells_v1(
    v_board, p_placements, v_rack, v_anchor, case when p_horizontal then 1 else 9 end
  );

  if cardinality(v_main_cells) > 1 then
    v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_main_cells);
    v_words := array_append(v_words, v_word);
    v_word_score := v_word_score + private.word_siege_score_word_v1(v_board, p_placements, v_rack, v_main_cells);
    v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board, p_placements, v_rack, v_main_cells);
    foreach v_index in array v_main_cells loop
      v_cell := v_board -> v_index;
      if (v_cell ->> 'letter') is not null
         and coalesce((v_cell ->> 'owner')::integer, 0) not in (0, v_owner) then
        v_board := jsonb_set(v_board, array[v_index::text], v_cell || jsonb_build_object('owner', v_owner), false);
        if not (v_index = any(v_captured)) then v_captured := array_append(v_captured, v_index); end if;
      end if;
    end loop;
  end if;

  foreach v_index in array v_indices loop
    v_cross_cells := private.word_siege_collect_cells_v1(
      v_board, p_placements, v_rack, v_index, case when p_horizontal then 9 else 1 end
    );
    if cardinality(v_cross_cells) > 1 then
      v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_cross_cells);
      v_words := array_append(v_words, v_word);
      v_word_score := v_word_score + private.word_siege_score_word_v1(v_board, p_placements, v_rack, v_cross_cells);
      v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board, p_placements, v_rack, v_cross_cells);
      foreach v_i in array v_cross_cells loop
        v_cell := v_board -> v_i;
        if (v_cell ->> 'letter') is not null
           and coalesce((v_cell ->> 'owner')::integer, 0) not in (0, v_owner)
           and not (v_i = any(v_captured)) then
          v_board := jsonb_set(v_board, array[v_i::text], v_cell || jsonb_build_object('owner', v_owner), false);
          v_captured := array_append(v_captured, v_i);
        end if;
      end loop;
    end if;
  end loop;

  foreach v_index in array v_indices loop
    v_cell := v_board -> v_index;
    v_letter := private.word_siege_letter_at_v1(r.board, p_placements, v_rack, v_index);
    if not coalesce((r.board -> v_index ->> 'bonus_used')::boolean, false)
       and (r.board -> v_index ->> 'bonus') is not null then
      v_bonus_cells := array_append(v_bonus_cells, v_index);
    end if;
    v_board := jsonb_set(
      v_board,
      array[v_index::text],
      v_cell || jsonb_build_object('letter', v_letter, 'owner', v_owner, 'bonus_used', true),
      false
    );
  end loop;

  select count(*)::integer into v_after_area
  from jsonb_array_elements(v_board) cell
  where coalesce((cell ->> 'owner')::integer, 0) = v_owner;
  v_area_delta := greatest(0, v_after_area - v_before_area);
  v_bonus_score := greatest(0, v_word_score - v_base_score);

  select coalesce(array_agg(i order by i), array[]::integer[]) into v_new_area
  from generate_series(0, 80) i
  where coalesce((r.board -> i ->> 'owner')::integer, 0) <> v_owner
    and coalesce((v_board -> i ->> 'owner')::integer, 0) = v_owner;

  return jsonb_build_object(
    'valid', true,
    'formed_words', to_jsonb(v_words),
    'base_word_score', v_base_score,
    'word_score', v_word_score,
    'bonus_score', v_bonus_score,
    'area_score', v_area_delta,
    'area_cells', v_area_delta,
    'captured_cells', cardinality(v_captured),
    'bonus_cells', cardinality(v_bonus_cells),
    'preview_cells', to_jsonb(v_new_area),
    'total_score', v_word_score + v_area_delta
  );
exception
  when others then
    return jsonb_build_object('valid', false, 'reason', sqlerrm);
end
$$;

revoke all on function private.word_siege_preview_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;

create or replace function public.preview_word_siege_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean
)
returns jsonb
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
  select private.word_siege_preview_move_v1(p_game_id, p_placements, p_horizontal)
$$;

revoke all on function public.preview_word_siege_move_v1(uuid, jsonb, boolean) from public, anon;
grant execute on function public.preview_word_siege_move_v1(uuid, jsonb, boolean) to authenticated;
