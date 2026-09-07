-- Ensure PostgREST's public Word Siege submit RPC resolves to the v5 zone-conquest wrapper.
-- The private v4 implementation was renamed in the preceding migration to preserve it as the
-- canonical dictionary/connectivity/Scrabble-scoring core. Recreate this SQL wrapper after that
-- rename so its function body is explicitly rebound to the new private wrapper.

create or replace function public.submit_word_siege_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns public.word_siege_games
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
  select private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal)
$$;

revoke all on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon;
grant execute on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) to authenticated;

select pg_notify('pgrst', 'reload schema');
