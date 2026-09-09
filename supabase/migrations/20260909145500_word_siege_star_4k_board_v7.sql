-- Word Siege board bonus v7
-- New games receive a gold 4K center and exactly one random three-star reward cell.
-- The authoritative server awards the star once per move (+25), never once per cross-word.
-- Existing games are intentionally left untouched.

create or replace function private.word_siege_new_board_v1()
returns jsonb
language plpgsql
volatile
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_star_index integer;
begin
  select i into v_star_index
  from generate_series(0, 224) i
  where i <> 112
    and i not in (0,7,14,105,119,210,217,224)
    and i not in (20,24,76,80,84,88,136,140,144,148,200,204)
    and i not in (16,28,32,42,48,56,64,70,154,160,168,176,182,192,196,208)
    and i not in (3,11,36,38,45,52,59,92,96,98,102,108,116,122,126,128,132,165,172,179,186,188,213,221)
  order by random()
  limit 1;

  return (
    select jsonb_agg(
      jsonb_build_object(
        'letter', null,
        'owner', 0,
        'bonus', case
          when i = 112 then '4K'
          when i = v_star_index then '3Y'
          when i in (0,7,14,105,119,210,217,224) then '3K'
          when i in (20,24,76,80,84,88,136,140,144,148,200,204) then '3H'
          when i in (16,28,32,42,48,56,64,70,154,160,168,176,182,192,196,208) then '2K'
          when i in (3,11,36,38,45,52,59,92,96,98,102,108,116,122,126,128,132,165,172,179,186,188,213,221) then '2H'
          else null
        end,
        'bonus_used', false
      ) order by i
    )
    from generate_series(0, 224) i
  );
end
$$;

create or replace function private.word_siege_score_word_v1(
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
  v_cell jsonb;
  v_letter text;
  v_bonus text;
  v_letter_score integer;
  v_word_multiplier integer := 1;
  v_total integer := 0;
  v_is_new boolean;
begin
  foreach v_index in array p_cells loop
    v_cell := p_board -> v_index;
    v_letter := private.word_siege_letter_at_v1(p_board, p_placements, p_rack, v_index);
    v_letter_score := private.word_siege_letter_value_v1(v_letter);
    v_is_new := (v_cell ->> 'letter') is null;
    v_bonus := case
      when v_is_new and not coalesce((v_cell ->> 'bonus_used')::boolean, false)
        then v_cell ->> 'bonus'
      else null
    end;
    if v_bonus = '2H' then v_letter_score := v_letter_score * 2; end if;
    if v_bonus = '3H' then v_letter_score := v_letter_score * 3; end if;
    if v_bonus = '2K' then v_word_multiplier := v_word_multiplier * 2; end if;
    if v_bonus = '3K' then v_word_multiplier := v_word_multiplier * 3; end if;
    if v_bonus = '4K' then v_word_multiplier := v_word_multiplier * 4; end if;
    v_total := v_total + v_letter_score;
  end loop;
  return v_total * v_word_multiplier;
end
$$;

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
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_board jsonb;
  v_before_board jsonb;
  v_rack text;
  v_bag text;
  v_owner integer;
  v_other uuid;
  v_indices integer[];
  v_rack_indices integer[];
  v_anchor integer;
  v_main_cells integer[];
  v_cross_cells integer[];
  v_cells integer[];
  v_words text[] := array[]::text[];
  v_word text;
  v_primary text;
  v_score integer := 0;
  v_index integer;
  v_i integer;
  v_distinct integer;
  v_existing boolean;
  v_connected boolean := false;
  v_cell jsonb;
  v_captured integer[] := array[]::integer[];
  v_placed jsonb := '[]'::jsonb;
  v_letter text;
  v_remaining text;
  v_draw text;
  v_next_rack text;
  v_needed integer;
  v_one_area integer;
  v_two_area integer;
  v_neutral_captured integer := 0;
  v_opponent_captured integer := 0;
  v_area_score integer := 0;
  v_request_fingerprint text;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if p_placements is null or jsonb_typeof(p_placements) <> 'array'
     or jsonb_array_length(p_placements) not between 1 and 7 then
    raise exception 'word_siege_invalid_placements';
  end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status <> 'playing' then raise exception 'word_siege_not_playing'; end if;
  if r.current_player_id <> v_uid then raise exception 'word_siege_not_your_turn'; end if;

  v_board := r.board;
  v_before_board := r.board;
  v_bag := r.bag;
  v_owner := case when v_uid = r.player_one_id then 1 else 2 end;
  v_other := case when v_uid = r.player_one_id then r.player_two_id else r.player_one_id end;
  v_rack := case when v_owner = 1 then r.player_one_rack else r.player_two_rack end;
  v_request_fingerprint := md5(p_placements::text || ':' || coalesce(p_horizontal, true)::text);

  select array_agg((e ->> 'index')::integer order by ordinality),
         array_agg((e ->> 'rack_index')::integer order by ordinality)
  into v_indices, v_rack_indices
  from jsonb_array_elements(p_placements) with ordinality as x(e, ordinality);
  if v_indices is null or cardinality(v_indices) <> jsonb_array_length(p_placements) then
    raise exception 'word_siege_invalid_placements';
  end if;
  if exists (select 1 from unnest(v_indices) x where x not between 0 and 224) then
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
    if (v_board -> v_index ->> 'letter') is not null then
      raise exception 'word_siege_cell_occupied';
    end if;
  end loop;

  v_anchor := v_indices[1];
  if cardinality(v_indices) > 1 then
    if p_horizontal and exists (select 1 from unnest(v_indices) x where x / 15 <> v_anchor / 15) then
      raise exception 'word_siege_not_in_one_row';
    end if;
    if not p_horizontal and exists (select 1 from unnest(v_indices) x where x % 15 <> v_anchor % 15) then
      raise exception 'word_siege_not_in_one_column';
    end if;
  end if;

  v_main_cells := private.word_siege_collect_cells_v1(
    v_board, p_placements, v_rack, v_anchor, case when p_horizontal then 1 else 15 end
  );
  if exists (select 1 from unnest(v_indices) x where not (x = any(v_main_cells))) then
    raise exception 'word_siege_gap_between_tiles';
  end if;

  select exists (
    select 1 from jsonb_array_elements(v_board) cell where cell ->> 'letter' is not null
  ) into v_existing;
  if not v_existing and not (112 = any(v_indices)) then
    raise exception 'word_siege_first_word_must_cover_center';
  end if;
  if v_existing and exists (
    select 1 from unnest(v_main_cells) x where (v_board -> x ->> 'letter') is not null
  ) then v_connected := true; end if;

  if cardinality(v_main_cells) > 1 then
    v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_main_cells);
    if not private.word_siege_word_allowed_v1(v_word, r.language) then
      raise exception 'word_siege_invalid_word:%', v_word;
    end if;
    v_words := array_append(v_words, v_word);
    v_primary := v_word;
    v_score := v_score + private.word_siege_score_word_v1(v_board, p_placements, v_rack, v_main_cells);
    foreach v_index in array v_main_cells loop
      v_cell := v_board -> v_index;
      if (v_cell ->> 'letter') is not null
         and coalesce((v_cell ->> 'owner')::integer, 0) not in (0, v_owner) then
        v_board := jsonb_set(
          v_board, array[v_index::text],
          v_cell || jsonb_build_object('owner', v_owner), false
        );
        v_captured := array_append(v_captured, v_index);
      end if;
    end loop;
  end if;

  foreach v_index in array v_indices loop
    v_cross_cells := private.word_siege_collect_cells_v1(
      v_board, p_placements, v_rack, v_index, case when p_horizontal then 15 else 1 end
    );
    if cardinality(v_cross_cells) > 1 then
      v_connected := v_connected or v_existing;
      v_word := private.word_siege_word_from_cells_v1(v_board, p_placements, v_rack, v_cross_cells);
      if not private.word_siege_word_allowed_v1(v_word, r.language) then
        raise exception 'word_siege_invalid_word:%', v_word;
      end if;
      v_words := array_append(v_words, v_word);
      if v_primary is null then v_primary := v_word; end if;
      v_score := v_score + private.word_siege_score_word_v1(v_board, p_placements, v_rack, v_cross_cells);
      foreach v_i in array v_cross_cells loop
        v_cell := v_board -> v_i;
        if (v_cell ->> 'letter') is not null
           and coalesce((v_cell ->> 'owner')::integer, 0) not in (0, v_owner)
           and not (v_i = any(v_captured)) then
          v_board := jsonb_set(
            v_board, array[v_i::text],
            v_cell || jsonb_build_object('owner', v_owner), false
          );
          v_captured := array_append(v_captured, v_i);
        end if;
      end loop;
    end if;
  end loop;

  if cardinality(v_words) = 0 then raise exception 'word_siege_word_required'; end if;
  if v_existing and not v_connected then raise exception 'word_siege_move_must_connect'; end if;

  -- Three-star is a move reward. Count it once for each newly consumed star cell,
  -- outside per-word scoring so a cross-word cannot double-award it.
  v_score := v_score + (
    select count(*)::integer * 25
    from unnest(v_indices) as placed(index_value)
    where coalesce(v_board -> index_value ->> 'bonus', '') = '3Y'
      and not coalesce((v_board -> index_value ->> 'bonus_used')::boolean, false)
      and (v_board -> index_value ->> 'letter') is null
  );

  for v_i in 1..cardinality(v_indices) loop
    v_index := v_indices[v_i];
    v_letter := substring(v_rack from (v_rack_indices[v_i] + 1) for 1);
    v_cell := v_board -> v_index;
    v_board := jsonb_set(
      v_board, array[v_index::text],
      v_cell || jsonb_build_object(
        'letter', v_letter, 'owner', v_owner, 'bonus_used', true
      ), false
    );
    v_placed := v_placed || jsonb_build_array(
      jsonb_build_object('index', v_index, 'letter', v_letter, 'owner', v_owner)
    );
  end loop;

  select d.neutral_captured, d.opponent_captured, d.area_score
  into v_neutral_captured, v_opponent_captured, v_area_score
  from private.word_siege_area_delta_v1(v_before_board, v_board, v_owner) d;

  select coalesce(string_agg(substring(v_rack from (i + 1) for 1), '' order by i), '')
  into v_remaining
  from generate_series(0, char_length(v_rack) - 1) i
  where not (i = any(v_rack_indices));
  v_needed := greatest(0, 7 - char_length(v_remaining));
  v_draw := substring(v_bag from 1 for v_needed);
  v_next_rack := v_remaining || v_draw;
  v_bag := substring(v_bag from (char_length(v_draw) + 1));

  select count(*) filter (where coalesce((cell ->> 'owner')::integer, 0) = 1)::integer,
         count(*) filter (where coalesce((cell ->> 'owner')::integer, 0) = 2)::integer
  into v_one_area, v_two_area
  from jsonb_array_elements(v_board) cell;

  update public.word_siege_games
  set board = v_board,
      bag = v_bag,
      player_one_rack = case when v_owner = 1 then v_next_rack else player_one_rack end,
      player_two_rack = case when v_owner = 2 then v_next_rack else player_two_rack end,
      player_one_word_score = player_one_word_score + case when v_owner = 1 then v_score else 0 end,
      player_two_word_score = player_two_word_score + case when v_owner = 2 then v_score else 0 end,
      player_one_area_score = player_one_area_score + case when v_owner = 1 then v_area_score else 0 end,
      player_two_area_score = player_two_area_score + case when v_owner = 2 then v_area_score else 0 end,
      player_one_area = v_one_area,
      player_two_area = v_two_area,
      current_player_id = v_other,
      consecutive_passes = 0,
      move_count = move_count + 1,
      last_action = 'word:' || v_primary,
      last_action_player_id = v_uid,
      last_move_at = now(),
      updated_at = now()
  where id = r.id
  returning * into r;

  insert into public.word_siege_moves(
    game_id, player_id, primary_word, formed_words, placed_tiles, word_score,
    neutral_captured, opponent_captured, area_score, total_score, captured_cells,
    move_number, request_fingerprint
  ) values (
    r.id, v_uid, v_primary, v_words, v_placed, v_score,
    v_neutral_captured, v_opponent_captured, v_area_score, v_score + v_area_score,
    v_neutral_captured + v_opponent_captured, r.move_count, v_request_fingerprint
  );

  if r.bag = '' and v_next_rack = '' then
    r := private.finish_word_siege_game_v1(r.id, 'rack_empty', null);
  end if;
  return r;
end
$$;

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
  v_before_board jsonb;
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
  v_area_score integer := 0;
  v_neutral_captured integer := 0;
  v_opponent_captured integer := 0;
  v_captured integer[] := array[]::integer[];
  v_new_area integer[] := array[]::integer[];
  v_bonus_cells integer[] := array[]::integer[];
  v_letter text;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  perform private.word_siege_prevalidate_move_v2(p_game_id,p_placements,p_horizontal);
  select * into r from public.word_siege_games where id=p_game_id;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id,r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  v_owner := case when v_uid=r.player_one_id then 1 else 2 end;
  v_rack := case when v_owner=1 then r.player_one_rack else r.player_two_rack end;
  v_board := r.board;
  v_before_board := r.board;
  select array_agg((e ->> 'index')::integer order by ordinality) into v_indices from jsonb_array_elements(p_placements) with ordinality as x(e,ordinality);
  v_anchor := v_indices[1];
  v_main_cells := private.word_siege_collect_cells_v1(v_board,p_placements,v_rack,v_anchor,case when p_horizontal then 1 else 15 end);
  if cardinality(v_main_cells)>1 then
    v_word := private.word_siege_word_from_cells_v1(v_board,p_placements,v_rack,v_main_cells);
    v_words := array_append(v_words,v_word);
    v_word_score := v_word_score + private.word_siege_score_word_v1(v_board,p_placements,v_rack,v_main_cells);
    v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board,p_placements,v_rack,v_main_cells);
    foreach v_index in array v_main_cells loop
      v_cell := v_board -> v_index;
      if (v_cell ->> 'letter') is not null and coalesce((v_cell ->> 'owner')::integer,0) not in (0,v_owner) then
        v_board := jsonb_set(v_board,array[v_index::text],v_cell || jsonb_build_object('owner',v_owner),false);
        if not (v_index=any(v_captured)) then v_captured:=array_append(v_captured,v_index); end if;
      end if;
    end loop;
  end if;
  foreach v_index in array v_indices loop
    v_cross_cells := private.word_siege_collect_cells_v1(v_board,p_placements,v_rack,v_index,case when p_horizontal then 15 else 1 end);
    if cardinality(v_cross_cells)>1 then
      v_word := private.word_siege_word_from_cells_v1(v_board,p_placements,v_rack,v_cross_cells);
      v_words := array_append(v_words,v_word);
      v_word_score := v_word_score + private.word_siege_score_word_v1(v_board,p_placements,v_rack,v_cross_cells);
      v_base_score := v_base_score + private.word_siege_score_word_base_v1(v_board,p_placements,v_rack,v_cross_cells);
      foreach v_i in array v_cross_cells loop
        v_cell := v_board -> v_i;
        if (v_cell ->> 'letter') is not null and coalesce((v_cell ->> 'owner')::integer,0) not in (0,v_owner) and not (v_i=any(v_captured)) then
          v_board := jsonb_set(v_board,array[v_i::text],v_cell || jsonb_build_object('owner',v_owner),false);
          v_captured:=array_append(v_captured,v_i);
        end if;
      end loop;
    end if;
  end loop;

  -- Keep preview identical to submit: star reward applies once per placement/move.
  v_word_score := v_word_score + (
    select count(*)::integer * 25
    from unnest(v_indices) as placed(index_value)
    where coalesce(v_board -> index_value ->> 'bonus', '') = '3Y'
      and not coalesce((v_board -> index_value ->> 'bonus_used')::boolean, false)
      and (v_board -> index_value ->> 'letter') is null
  );

  foreach v_index in array v_indices loop
    v_cell := v_board -> v_index;
    v_letter := private.word_siege_letter_at_v1(r.board,p_placements,v_rack,v_index);
    if not coalesce((r.board -> v_index ->> 'bonus_used')::boolean,false) and (r.board -> v_index ->> 'bonus') is not null then
      v_bonus_cells:=array_append(v_bonus_cells,v_index);
    end if;
    v_board := jsonb_set(v_board,array[v_index::text],v_cell || jsonb_build_object('letter',v_letter,'owner',v_owner,'bonus_used',true),false);
  end loop;
  select d.neutral_captured,d.opponent_captured,d.area_score into v_neutral_captured,v_opponent_captured,v_area_score from private.word_siege_area_delta_v1(v_before_board,v_board,v_owner) d;
  v_bonus_score := greatest(0,v_word_score-v_base_score);
  select coalesce(array_agg(i order by i),array[]::integer[]) into v_new_area from generate_series(0,224) i where coalesce((r.board -> i ->> 'owner')::integer,0)<>v_owner and coalesce((v_board -> i ->> 'owner')::integer,0)=v_owner;
  return jsonb_build_object('valid',true,'formed_words',to_jsonb(v_words),'base_word_score',v_base_score,'word_score',v_word_score,'bonus_score',v_bonus_score,'area_score',v_area_score,'area_cells',v_neutral_captured+v_opponent_captured,'neutral_captured',v_neutral_captured,'opponent_captured',v_opponent_captured,'captured_cells',cardinality(v_captured),'bonus_cells',cardinality(v_bonus_cells),'preview_cells',to_jsonb(v_new_area),'total_score',v_word_score+v_area_score);
exception when others then return jsonb_build_object('valid',false,'reason',sqlerrm);
end
$$;
