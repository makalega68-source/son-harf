-- Word Siege v5 / 2: deterministic 15x15 -> 25 zone helpers.

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

revoke all on function private.word_siege_zone_value_v5(integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_owner_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_count_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_zone_score_v5(jsonb, integer) from public, anon, authenticated;
revoke all on function private.word_siege_touched_zones_v5(jsonb) from public, anon, authenticated;
