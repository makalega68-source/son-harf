-- Kelime Kuşatması v8: custom territory-first bonus topology.
-- New games only. Existing persisted boards are intentionally untouched.
-- Keeps the authoritative 15x15 / 225-cell contract and v7 4K + single 3Y rules.

create or replace function private.word_siege_new_board_v1()
returns jsonb
language plpgsql
volatile
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_star_index integer;
begin
  -- Static Kelime Kuşatması tactical zones (33 cells including center):
  -- 3K major siege lanes: 22,106,118,202
  -- 3H watch zones:       34,40,62,72,152,162,184,190
  -- 2K fort zones:        51,53,93,101,123,131,171,173
  -- 2H tactical zones:    18,26,46,58,80,84,140,144,166,178,198,206
  -- center crown:         112 (4K)
  select i into v_star_index
  from generate_series(0, 224) i
  where i <> 112
    and i not in (22,106,118,202)
    and i not in (34,40,62,72,152,162,184,190)
    and i not in (51,53,93,101,123,131,171,173)
    and i not in (18,26,46,58,80,84,140,144,166,178,198,206)
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
          when i in (22,106,118,202) then '3K'
          when i in (34,40,62,72,152,162,184,190) then '3H'
          when i in (51,53,93,101,123,131,171,173) then '2K'
          when i in (18,26,46,58,80,84,140,144,166,178,198,206) then '2H'
          else null
        end,
        'bonus_used', false
      ) order by i
    )
    from generate_series(0, 224) i
  );
end
$$;

revoke all on function private.word_siege_new_board_v1() from public, anon;
grant execute on function private.word_siege_new_board_v1() to authenticated;
