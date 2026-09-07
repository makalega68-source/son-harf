-- Word Siege v5: zone-based conquest, fortress zones, Conquest Meter and 45-second turn clock.
-- IMPORTANT: the canonical word validator / Scrabble scorer remains the v4 core. This migration wraps
-- that validated move and replaces only the old cell-by-cell territory layer.

alter table public.word_siege_games
  add column if not exists player_one_conquest_meter integer not null default 0,
  add column if not exists player_two_conquest_meter integer not null default 0,
  add column if not exists player_one_onslaught_active boolean not null default false,
  add column if not exists player_two_onslaught_active boolean not null default false,
  add column if not exists turn_started_at timestamptz,
  add column if not exists turn_deadline timestamptz;

alter table public.word_siege_games drop constraint if exists word_siege_games_player_one_conquest_meter_v5_check;
alter table public.word_siege_games drop constraint if exists word_siege_games_player_two_conquest_meter_v5_check;
alter table public.word_siege_games
  add constraint word_siege_games_player_one_conquest_meter_v5_check check (player_one_conquest_meter between 0 and 2),
  add constraint word_siege_games_player_two_conquest_meter_v5_check check (player_two_conquest_meter between 0 and 2);

alter table public.word_siege_moves
  add column if not exists raw_word_score integer not null default 0,
  add column if not exists zones_flipped integer[] not null default '{}'::integer[],
  add column if not exists onslaught_triggered boolean not null default false,
  add column if not exists onslaught_consumed boolean not null default false;

create or replace function private.word_siege_zone_value_v5(p_zone_id integer)
returns integer
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  select case when p_zone_id in (6, 8, 12, 16, 18) then 4 else 2 end
$$;

create or replace function private.word_siege_zone_owner_v5(p_board jsonb, p_zone_id integer)
returns integer
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  with bounds as (
    select (p_zone_id / 5) * 3 as first_row, (p_zone_id % 5) * 3 as first_col
  ), owners as (
    select coalesce((p_board -> ((b.first_row + ro) * 15 + (b.first_col + co)) ->> 'owner')::integer, 0) as owner
    from bounds b
    cross join generate_series(0, 2) ro
    cross join generate_series(0, 2) co
  )
  select case
    when min(owner) = max(owner) and min(owner) in (1, 2) then min(owner)
    else 0
  end
  from owners
$$;

create or replace function private.word_siege_zone_count_v5(p_board jsonb, p_owner integer)
returns integer
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  select count(*)::integer
  from generate_series(0, 24) z
  where private.word_siege_zone_owner_v5(p_board, z) = p_owner
$$;

create or replace function private.word_siege_zone_score_v5(p_board jsonb, p_owner integer)
returns integer
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  select coalesce(sum(private.word_siege_zone_value_v5(z)), 0)::integer
  from generate_series(0, 24) z
  where private.word_siege_zone_owner_v5(p_board, z) = p_owner
$$;

create or replace function private.word_siege_touched_zones_v5(p_placements jsonb)
returns integer[]
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  with placed as (
    select (e ->> 'index')::integer as i
    from jsonb_array_elements(coalesce(p_placements, '[]'::jsonb)) e
    where (e ->> 'index')::integer between 0 and 224
  ), touched_cells as (
    select i from placed
    union select i - 15 from placed where i / 15 > 0
    union select i + 15 from placed where i / 15 < 14
    union select i - 1 from placed where i % 15 > 0
    union select i + 1 from placed where i % 15 < 14
  ), zones as (
    select distinct (((i / 15) / 3) * 5 + ((i % 15) / 3))::integer as zone_id
    from touched_cells
    where i between 0 and 224
  )
  select coalesce(array_agg(zone_id order by zone_id), '{}'::integer[]) from zones
$$;

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

-- Normalize active legacy cell territory into deterministic whole zones. Letters and bonuses are untouched.
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
from lateral (select private.word_siege_normalize_zones_v5(g.board) as board) n
where g.status in ('waiting', 'playing')
  and jsonb_typeof(g.board) = 'array'
  and jsonb_array_length(g.board) = 225;

-- Keep the existing validated v4 move engine intact and wrap it with the new conquest layer.
alter function private.submit_word_siege_move_v1(uuid, jsonb, boolean)
  rename to submit_word_siege_move_cell_core_v5;

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
  r_before public.word_siege_games;
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_owner integer;
  v_before_meter integer;
  v_before_onslaught boolean;
  v_raw_score integer := 0;
  v_final_score integer := 0;
  v_board jsonb;
  v_flipped integer[] := '{}'::integer[];
  v_neutral integer := 0;
  v_opponent integer := 0;
  v_gained_points integer := 0;
  v_meter integer := 0;
  v_onslaught boolean := false;
  v_triggered boolean := false;
  v_one_area integer := 0;
  v_two_area integer := 0;
  v_one_zone_score integer := 0;
  v_two_zone_score integer := 0;
  v_one_total integer := 0;
  v_two_total integer := 0;
  v_winner uuid;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r_before from public.word_siege_games where id = p_game_id for update;
  if r_before.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r_before.player_one_id, r_before.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  v_owner := case when v_uid = r_before.player_one_id then 1 else 2 end;
  v_before_meter := case when v_owner = 1 then r_before.player_one_conquest_meter else r_before.player_two_conquest_meter end;
  v_before_onslaught := case when v_owner = 1 then r_before.player_one_onslaught_active else r_before.player_two_onslaught_active end;

  -- Canonical dictionary validation, connectivity, rack handling and Scrabble scoring happen here unchanged.
  r := private.submit_word_siege_move_cell_core_v5(p_game_id, p_placements, p_horizontal);

  v_raw_score := case
    when v_owner = 1 then greatest(0, r.player_one_word_score - r_before.player_one_word_score)
    else greatest(0, r.player_two_word_score - r_before.player_two_word_score)
  end;
  v_final_score := v_raw_score * case when v_before_onslaught then 2 else 1 end;

  select c.board, c.flipped_zone_ids, c.neutral_zones, c.opponent_zones, c.gained_zone_points
  into v_board, v_flipped, v_neutral, v_opponent, v_gained_points
  from private.word_siege_claim_zones_v5(r_before.board, r.board, v_owner, p_placements) c;

  v_meter := greatest(0, least(2, v_before_meter));
  v_onslaught := false; -- any previously armed onslaught is consumed by this valid word
  if cardinality(v_flipped) > 0 then
    v_meter := v_meter + 1;
    if v_meter >= 3 then
      v_meter := 0;
      v_onslaught := true;
      v_triggered := true;
    end if;
  end if;

  v_one_area := private.word_siege_zone_count_v5(v_board, 1);
  v_two_area := private.word_siege_zone_count_v5(v_board, 2);
  v_one_zone_score := private.word_siege_zone_score_v5(v_board, 1);
  v_two_zone_score := private.word_siege_zone_score_v5(v_board, 2);

  update public.word_siege_games
  set board = v_board,
      player_one_word_score = case
        when v_owner = 1 then r_before.player_one_word_score + v_final_score
        else player_one_word_score
      end,
      player_two_word_score = case
        when v_owner = 2 then r_before.player_two_word_score + v_final_score
        else player_two_word_score
      end,
      player_one_area = v_one_area,
      player_two_area = v_two_area,
      player_one_area_score = v_one_zone_score,
      player_two_area_score = v_two_zone_score,
      player_one_conquest_meter = case when v_owner = 1 then v_meter else player_one_conquest_meter end,
      player_two_conquest_meter = case when v_owner = 2 then v_meter else player_two_conquest_meter end,
      player_one_onslaught_active = case when v_owner = 1 then v_onslaught else player_one_onslaught_active end,
      player_two_onslaught_active = case when v_owner = 2 then v_onslaught else player_two_onslaught_active end,
      turn_started_at = case when status = 'playing' then now() else null end,
      turn_deadline = case when status = 'playing' then now() + interval '45 seconds' else null end,
      updated_at = now()
  where id = p_game_id
  returning * into r;

  update public.word_siege_moves
  set raw_word_score = v_raw_score,
      word_score = v_final_score,
      neutral_captured = v_neutral,
      opponent_captured = v_opponent,
      area_score = v_gained_points,
      total_score = v_final_score + v_gained_points,
      captured_cells = cardinality(v_flipped),
      zones_flipped = v_flipped,
      onslaught_triggered = v_triggered,
      onslaught_consumed = v_before_onslaught
  where game_id = p_game_id
    and player_id = v_uid
    and move_number = r.move_count;

  -- The v4 core may have finished a rack-empty game before zone post-processing. Correct only that
  -- score-derived result using the new permanent-word + current-zone contract.
  if r.status = 'finished' and r.finish_reason = 'rack_empty' then
    v_one_total := r.player_one_word_score + r.player_one_area_score;
    v_two_total := r.player_two_word_score + r.player_two_area_score;
    v_winner := case
      when v_one_total > v_two_total then r.player_one_id
      when v_two_total > v_one_total then r.player_two_id
      when r.player_one_area_score > r.player_two_area_score then r.player_one_id
      when r.player_two_area_score > r.player_one_area_score then r.player_two_id
      when r.player_one_area > r.player_two_area then r.player_one_id
      when r.player_two_area > r.player_one_area then r.player_two_id
      else null
    end;
    update public.word_siege_games
    set winner_id = v_winner,
        loser_id = case
          when v_winner is null then null
          when v_winner = player_one_id then player_two_id
          else player_one_id
        end,
        turn_started_at = null,
        turn_deadline = null,
        updated_at = now()
    where id = p_game_id
    returning * into r;
  end if;

  return r;
end
$$;

-- New zone score is current territory, not a cumulative earned-cube ledger.
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
      when r.player_one_area_score > r.player_two_area_score then r.player_one_id
      when r.player_two_area_score > r.player_one_area_score then r.player_two_id
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

create or replace function private.word_siege_turn_clock_v5()
returns trigger
language plpgsql
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if new.status = 'playing' and new.current_player_id is not null and (
      tg_op = 'INSERT'
      or old.status is distinct from new.status
      or old.current_player_id is distinct from new.current_player_id
  ) then
    new.turn_started_at := clock_timestamp();
    new.turn_deadline := clock_timestamp() + interval '45 seconds';
  elsif new.status <> 'playing' or new.current_player_id is null then
    new.turn_started_at := null;
    new.turn_deadline := null;
  end if;
  return new;
end
$$;

drop trigger if exists word_siege_turn_clock_v5 on public.word_siege_games;
create trigger word_siege_turn_clock_v5
before insert or update of status, current_player_id on public.word_siege_games
for each row execute function private.word_siege_turn_clock_v5();

update public.word_siege_games
set turn_started_at = clock_timestamp(),
    turn_deadline = clock_timestamp() + interval '45 seconds'
where status = 'playing' and current_player_id is not null;

create or replace function private.expire_word_siege_turn_v1(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_other uuid;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status <> 'playing' then return r; end if;
  if r.turn_deadline is null or r.turn_deadline > clock_timestamp() then return r; end if;

  v_other := case when r.current_player_id = r.player_one_id then r.player_two_id else r.player_one_id end;
  update public.word_siege_games
  set current_player_id = v_other,
      consecutive_passes = least(2, consecutive_passes + 1),
      move_count = move_count + 1,
      last_action = 'timeout',
      last_action_player_id = r.current_player_id,
      last_move_at = clock_timestamp(),
      updated_at = clock_timestamp()
  where id = p_game_id
  returning * into r;

  if r.consecutive_passes >= 2 then
    r := private.finish_word_siege_game_v1(r.id, 'consecutive_timeouts', null);
  end if;
  return r;
end
$$;

create or replace function public.expire_word_siege_turn_v1(p_game_id uuid)
returns public.word_siege_games
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$ select private.expire_word_siege_turn_v1(p_game_id) $$;

revoke all on function private.word_siege_zone_value_v5(integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_owner_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_count_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_score_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_touched_zones_v5(jsonb) from public, anon, authenticated;
revoke all on function private.word_siege_claim_zones_v5(jsonb, jsonb, integer, jsonb) from public, anon, authenticated;
revoke all on function private.word_siege_normalize_zones_v5(jsonb) from public, anon, authenticated;
revoke all on function private.submit_word_siege_move_cell_core_v5(uuid, jsonb, boolean) from public, anon, authenticated;
revoke all on function private.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon, authenticated;
revoke all on function private.finish_word_siege_game_v1(uuid, text, uuid) from public, anon, authenticated;
revoke all on function private.expire_word_siege_turn_v1(uuid) from public, anon, authenticated;
revoke all on function public.expire_word_siege_turn_v1(uuid) from public, anon;
grant execute on function public.expire_word_siege_turn_v1(uuid) to authenticated;

select pg_notify('pgrst', 'reload schema');
