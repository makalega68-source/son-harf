-- Paket 3: Kelimelik-style word validation for Kelime Kusatmasi.
-- Validate every newly formed main/cross word before the existing gameplay mutation path.

create or replace function private.word_siege_prevalidate_move_v2(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_board jsonb;
  v_rack text;
  v_owner integer;
  v_indices integer[];
  v_rack_indices integer[];
  v_anchor integer;
  v_main_cells integer[];
  v_cross_cells integer[];
  v_word text;
  v_index integer;
  v_distinct integer;
  v_existing boolean;
  v_connected boolean := false;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if p_placements is null or jsonb_typeof(p_placements) <> 'array'
     or jsonb_array_length(p_placements) not between 1 and 7 then
    raise exception 'word_siege_invalid_placements';
  end if;

  select * into r
  from public.word_siege_games
  where id = p_game_id
  for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status <> 'playing' then raise exception 'word_siege_not_playing'; end if;
  if r.current_player_id <> v_uid then raise exception 'word_siege_not_your_turn'; end if;

  v_board := r.board;
  v_owner := case when v_uid = r.player_one_id then 1 else 2 end;
  v_rack := case when v_owner = 1 then r.player_one_rack else r.player_two_rack end;

  select array_agg((e ->> 'index')::integer order by ordinality),
         array_agg((e ->> 'rack_index')::integer order by ordinality)
  into v_indices, v_rack_indices
  from jsonb_array_elements(p_placements) with ordinality as x(e, ordinality);

  if v_indices is null or cardinality(v_indices) <> jsonb_array_length(p_placements) then
    raise exception 'word_siege_invalid_placements';
  end if;
  if exists (select 1 from unnest(v_indices) x where x not between 0 and 80) then
    raise exception 'word_siege_invalid_cell';
  end if;
  if exists (
    select 1 from unnest(v_rack_indices) x
    where x < 0 or x >= char_length(coalesce(v_rack, ''))
  ) then raise exception 'word_siege_invalid_rack_tile'; end if;

  select count(distinct x)::integer into v_distinct from unnest(v_indices) x;
  if v_distinct <> cardinality(v_indices) then raise exception 'word_siege_duplicate_cell'; end if;
  select count(distinct x)::integer into v_distinct from unnest(v_rack_indices) x;
  if v_distinct <> cardinality(v_rack_indices) then raise exception 'word_siege_duplicate_rack_tile'; end if;

  foreach v_index in array v_indices loop
    if nullif(v_board -> v_index ->> 'letter', '') is not null then
      raise exception 'word_siege_cell_occupied';
    end if;
  end loop;

  v_anchor := v_indices[1];
  if cardinality(v_indices) > 1 then
    if p_horizontal and exists (select 1 from unnest(v_indices) x where x / 9 <> v_anchor / 9) then
      raise exception 'word_siege_not_in_one_row';
    end if;
    if not p_horizontal and exists (select 1 from unnest(v_indices) x where x % 9 <> v_anchor % 9) then
      raise exception 'word_siege_not_in_one_column';
    end if;
  end if;

  -- Maximum uninterrupted main word: walk until the first empty cell / board edge.
  v_main_cells := private.word_siege_collect_cells_v1(
    v_board, p_placements, v_rack, v_anchor,
    case when p_horizontal then 1 else 9 end
  );
  if exists (select 1 from unnest(v_indices) x where not (x = any(v_main_cells))) then
    raise exception 'word_siege_gap_between_tiles';
  end if;

  select exists (
    select 1 from jsonb_array_elements(v_board) cell
    where nullif(cell ->> 'letter', '') is not null
  ) into v_existing;

  if not v_existing and not (40 = any(v_indices)) then
    raise exception 'word_siege_first_word_must_cover_center';
  end if;

  if v_existing and exists (
    select 1 from unnest(v_main_cells) x
    where nullif(v_board -> x ->> 'letter', '') is not null
  ) then
    v_connected := true;
  end if;

  if cardinality(v_main_cells) > 1 then
    v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_main_cells);
    if not private.word_siege_word_allowed_v1(v_word, r.language) then
      raise exception 'word_siege_invalid_word:%', v_word;
    end if;
  end if;

  -- Every newly placed tile must also validate the maximum uninterrupted cross word.
  foreach v_index in array v_indices loop
    v_cross_cells := private.word_siege_collect_cells_v1(
      v_board, p_placements, v_rack, v_index,
      case when p_horizontal then 9 else 1 end
    );
    if cardinality(v_cross_cells) > 1 then
      v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_cross_cells);
      if not private.word_siege_word_allowed_v1(v_word, r.language) then
        raise exception 'word_siege_invalid_word:%', v_word;
      end if;
      if v_existing then v_connected := true; end if;
    end if;
  end loop;

  if cardinality(v_main_cells) <= 1 and not exists (
    select 1
    from unnest(v_indices) x
    where cardinality(private.word_siege_collect_cells_v1(
      v_board, p_placements, v_rack, x,
      case when p_horizontal then 9 else 1 end
    )) > 1
  ) then
    raise exception 'word_siege_word_required';
  end if;

  if v_existing and not v_connected then
    raise exception 'word_siege_move_must_connect';
  end if;
end
$$;

revoke all on function private.word_siege_prevalidate_move_v2(uuid, jsonb, boolean) from public, anon, authenticated;

-- Public RPC now performs a complete validation pass before entering the existing
-- score / territory / bonus / rack / bag mutation function. The private function
-- remains inaccessible to clients and retains the established gameplay mechanics.
create or replace function public.submit_word_siege_move_v1(
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
  r public.word_siege_games;
begin
  perform private.word_siege_prevalidate_move_v2(p_game_id, p_placements, p_horizontal);
  r := private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal);
  return r;
end
$$;

revoke all on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
grant execute on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) to authenticated;
