-- Word Siege v5 bridge / 1: bring live 9x9 server state to the canonical 15x15 geometry
-- before the zone-conquest migrations run. This is intentionally idempotent for already-15x15 rows.

create or replace function private.word_siege_new_board_v1()
returns jsonb
language sql
immutable
set search_path = pg_catalog, public, private, pg_temp
as $$
  select jsonb_agg(
    jsonb_build_object(
      'letter', null,
      'owner', 0,
      'bonus', case
        when i in (0,7,14,105,119,210,217,224) then '3K'
        when i in (20,24,76,80,84,88,136,140,144,148,200,204) then '3H'
        when i in (16,28,32,42,48,56,64,70,112,154,160,168,176,182,192,196,208) then '2K'
        when i in (3,11,36,38,45,52,59,92,96,98,102,108,116,122,126,128,132,165,172,179,186,188,213,221) then '2H'
        else null
      end,
      'bonus_used', false
    ) order by i
  )
  from generate_series(0, 224) i
$$;

-- Remap historical move coordinates while the associated game still identifies itself as 81-cell.
update public.word_siege_moves m
set placed_tiles = (
  select coalesce(jsonb_agg(
    tile || jsonb_build_object(
      'index', ((((tile ->> 'index')::integer / 9) + 3) * 15) + (((tile ->> 'index')::integer % 9) + 3)
    ) order by ordinality
  ), '[]'::jsonb)
  from jsonb_array_elements(m.placed_tiles) with ordinality as t(tile, ordinality)
)
where exists (
  select 1 from public.word_siege_games g
  where g.id = m.game_id and jsonb_array_length(g.board) = 81
);

-- Drop legacy 81-cell checks BEFORE rewriting board rows. Production currently enforces these checks.
do $$
declare c record;
begin
  for c in
    select conname, pg_get_constraintdef(oid) as definition
    from pg_constraint
    where conrelid = 'public.word_siege_games'::regclass and contype = 'c'
  loop
    if c.definition like '%player_one_area%81%'
       or c.definition like '%player_two_area%81%'
       or c.definition like '%jsonb_array_length(board)%81%' then
      execute format('alter table public.word_siege_games drop constraint %I', c.conname);
    end if;
  end loop;
end $$;

-- Preserve existing letters/owners while centering legacy 9x9 games inside the new 15x15 board.
update public.word_siege_games g
set board = (
  with fresh as (select private.word_siege_new_board_v1() as board)
  select jsonb_agg(
    case
      when (i / 15) between 3 and 11 and (i % 15) between 3 and 11 then
        (fresh.board -> i) || jsonb_build_object(
          'letter', g.board -> ((((i / 15) - 3) * 9) + ((i % 15) - 3)) -> 'letter',
          'owner', coalesce(g.board -> ((((i / 15) - 3) * 9) + ((i % 15) - 3)) -> 'owner', '0'::jsonb),
          'bonus_used', coalesce(g.board -> ((((i / 15) - 3) * 9) + ((i % 15) - 3)) -> 'bonus_used', 'false'::jsonb)
        )
      else fresh.board -> i
    end
    order by i
  )
  from fresh, generate_series(0, 224) i
)
where jsonb_typeof(g.board) = 'array' and jsonb_array_length(g.board) = 81;

alter table public.word_siege_games drop constraint if exists word_siege_games_player_one_area_v4_check;
alter table public.word_siege_games drop constraint if exists word_siege_games_player_two_area_v4_check;
alter table public.word_siege_games drop constraint if exists word_siege_games_board_v4_check;
alter table public.word_siege_games
  add constraint word_siege_games_player_one_area_v4_check check (player_one_area between 0 and 225),
  add constraint word_siege_games_player_two_area_v4_check check (player_two_area between 0 and 225),
  add constraint word_siege_games_board_v4_check check (jsonb_typeof(board) = 'array' and jsonb_array_length(board) = 225);

create or replace function private.word_siege_collect_cells_v1(
  p_board jsonb,
  p_placements jsonb,
  p_rack text,
  p_anchor integer,
  p_delta integer
)
returns integer[]
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_start integer := p_anchor;
  v_current integer;
  v_next integer;
  v_cells integer[] := array[]::integer[];
begin
  if p_delta not in (1, 15) or p_anchor not between 0 and 224 then return v_cells; end if;
  if private.word_siege_letter_at_v1(p_board, p_placements, p_rack, p_anchor) is null then return v_cells; end if;

  loop
    v_next := v_start - p_delta;
    exit when v_next < 0;
    exit when p_delta = 1 and (v_next / 15) <> (v_start / 15);
    exit when private.word_siege_letter_at_v1(p_board, p_placements, p_rack, v_next) is null;
    v_start := v_next;
  end loop;

  v_current := v_start;
  loop
    exit when v_current > 224;
    exit when p_delta = 1 and (v_current / 15) <> (v_start / 15);
    exit when private.word_siege_letter_at_v1(p_board, p_placements, p_rack, v_current) is null;
    v_cells := array_append(v_cells, v_current);
    v_next := v_current + p_delta;
    exit when v_next > 224;
    exit when p_delta = 1 and (v_next / 15) <> (v_current / 15);
    v_current := v_next;
  end loop;
  return v_cells;
end
$$;

create or replace function private.word_siege_word_allowed_v1(p_word text, p_language text)
returns boolean
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text := case when lower(coalesce(p_language, 'tr')) = 'en' then 'en' else 'tr' end;
  v_normalized text;
begin
  v_normalized := public.normalize_game_word(v_language, trim(coalesce(p_word, '')));
  if char_length(v_normalized) not between 2 and 15 then return false; end if;
  if v_language = 'tr' and (left(v_normalized, 1) = 'ğ' or right(v_normalized, 1) = 'ğ') then return false; end if;
  return exists (
    select 1 from public.dictionary_words d
    where d.language = v_language and d.active and d.normalized_word = v_normalized
  );
end
$$;

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
    select i,
      coalesce((p_before_board -> i ->> 'owner')::integer, 0) as before_owner,
      coalesce((p_after_board -> i ->> 'owner')::integer, 0) as after_owner
    from generate_series(0, 224) i
  ), counts as (
    select
      count(*) filter (where before_owner = 0 and after_owner = p_owner)::integer as neutral_count,
      count(*) filter (where before_owner not in (0, p_owner) and after_owner = p_owner)::integer as opponent_count
    from ownership
  )
  select neutral_count, opponent_count, (neutral_count + opponent_count) * 2 from counts
$$;

revoke all on function private.word_siege_new_board_v1() from public, anon, authenticated;
revoke all on function private.word_siege_collect_cells_v1(jsonb, jsonb, text, integer, integer) from public, anon, authenticated;
revoke all on function private.word_siege_word_allowed_v1(text, text) from public, anon, authenticated;
revoke all on function private.word_siege_area_delta_v1(jsonb, jsonb, integer) from public, anon, authenticated;
