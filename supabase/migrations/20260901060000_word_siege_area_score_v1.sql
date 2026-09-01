-- Kelime Kusatmasi: cumulative area points, ownership-diff scoring and duplicate move protection.
-- Validation, dictionary and word/bonus scoring rules are intentionally unchanged.

alter table public.word_siege_games
  add column if not exists player_one_area_score integer not null default 0 check (player_one_area_score >= 0),
  add column if not exists player_two_area_score integer not null default 0 check (player_two_area_score >= 0);

alter table public.word_siege_moves
  add column if not exists neutral_captured integer not null default 0 check (neutral_captured >= 0),
  add column if not exists opponent_captured integer not null default 0 check (opponent_captured >= 0),
  add column if not exists area_score integer not null default 0 check (area_score >= 0),
  add column if not exists total_score integer not null default 0 check (total_score >= 0),
  add column if not exists move_number integer,
  add column if not exists request_fingerprint text;

-- Backfill historical Word Siege moves using the old server contract:
-- every placed tile was a newly-owned neutral cell and captured_cells tracked rival cells.
update public.word_siege_moves
set neutral_captured = jsonb_array_length(placed_tiles),
    opponent_captured = captured_cells,
    area_score = jsonb_array_length(placed_tiles) + (captured_cells * 2),
    total_score = word_score + jsonb_array_length(placed_tiles) + (captured_cells * 2)
where area_score = 0
  and total_score = 0;

update public.word_siege_games g
set player_one_area_score = coalesce(s.player_one_area_score, 0),
    player_two_area_score = coalesce(s.player_two_area_score, 0)
from (
  select
    game_id,
    coalesce(sum(area_score) filter (where player_id = g2.player_one_id), 0)::integer as player_one_area_score,
    coalesce(sum(area_score) filter (where player_id = g2.player_two_id), 0)::integer as player_two_area_score
  from public.word_siege_moves m
  join public.word_siege_games g2 on g2.id = m.game_id
  group by game_id
) s
where g.id = s.game_id
  and g.player_one_area_score = 0
  and g.player_two_area_score = 0;

create unique index if not exists word_siege_moves_game_move_number_uidx
  on public.word_siege_moves(game_id, move_number)
  where move_number is not null;

create index if not exists word_siege_moves_request_fingerprint_idx
  on public.word_siege_moves(game_id, player_id, request_fingerprint)
  where request_fingerprint is not null;

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
  select neutral_count, opponent_count, neutral_count + opponent_count * 2
  from counts
$$;

revoke all on function private.word_siege_area_delta_v1(jsonb, jsonb, integer) from public, anon, authenticated;

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
    if (v_board -> v_index ->> 'letter') is not null then
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

  v_main_cells := private.word_siege_collect_cells_v1(
    v_board, p_placements, v_rack, v_anchor, case when p_horizontal then 1 else 9 end
  );
  if exists (select 1 from unnest(v_indices) x where not (x = any(v_main_cells))) then
    raise exception 'word_siege_gap_between_tiles';
  end if;

  select exists (
    select 1 from jsonb_array_elements(v_board) cell where cell ->> 'letter' is not null
  ) into v_existing;
  if not v_existing and not (40 = any(v_indices)) then
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
      v_board, p_placements, v_rack, v_index, case when p_horizontal then 9 else 1 end
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

-- Keep the live deadline/prevalidation pipeline, but make an immediate retried request idempotent.
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
  v_uid uuid := auth.uid();
  v_fingerprint text := md5(p_placements::text || ':' || coalesce(p_horizontal, true)::text);
  v_existing_move_number integer;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  select * into r
  from public.word_siege_games
  where id = p_game_id
  for update;

  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  select m.move_number
  into v_existing_move_number
  from public.word_siege_moves m
  where m.game_id = p_game_id
    and m.player_id = v_uid
    and m.request_fingerprint = v_fingerprint
  order by m.id desc
  limit 1;

  if v_existing_move_number is not null
     and r.move_count = v_existing_move_number
     and r.last_action_player_id = v_uid then
    return r;
  end if;

  r := private.word_siege_prepare_turn_v2(p_game_id);
  if r.status <> 'playing' then return r; end if;
  perform private.word_siege_prevalidate_move_v2(p_game_id, p_placements, p_horizontal);
  r := private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal);
  if r.status = 'playing' then
    r := private.word_siege_arm_next_turn_v2(r.id);
  end if;
  return r;
end
$$;

revoke all on function private.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
revoke all on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
grant execute on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) to authenticated;

-- Winner uses cumulative area points. Current owned area remains a tie-breaker and a live counter only.
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

  v_one_total := r.player_one_word_score + r.player_one_area_score;
  v_two_total := r.player_two_word_score + r.player_two_area_score;
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
