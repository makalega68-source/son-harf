-- Word Siege v5 deadline guard.
-- A 45-second clock must be authoritative on the server, not only a client countdown.
-- If a participant tries to submit/pass/exchange after the deadline, the expired turn is advanced
-- through the same timeout RPC and the attempted late action is not applied.

create or replace function private.word_siege_turn_expired_v5(p_game_id uuid)
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
  select coalesce(
    (
      select g.status = 'playing'
        and g.turn_deadline is not null
        and g.turn_deadline <= clock_timestamp()
      from public.word_siege_games g
      where g.id = p_game_id
    ),
    false
  )
$$;

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
begin
  if private.word_siege_turn_expired_v5(p_game_id) then
    return private.expire_word_siege_turn_v1(p_game_id);
  end if;
  return private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal);
end
$$;

create or replace function public.pass_word_siege_turn_v1(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if private.word_siege_turn_expired_v5(p_game_id) then
    return private.expire_word_siege_turn_v1(p_game_id);
  end if;
  return private.pass_word_siege_turn_v1(p_game_id);
end
$$;

create or replace function public.exchange_word_siege_tiles_v1(
  p_game_id uuid,
  p_rack_indices jsonb
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if private.word_siege_turn_expired_v5(p_game_id) then
    return private.expire_word_siege_turn_v1(p_game_id);
  end if;
  return private.exchange_word_siege_tiles_v1(p_game_id, p_rack_indices);
end
$$;

revoke all on function private.word_siege_turn_expired_v5(uuid) from public, anon, authenticated;
revoke all on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) from public, anon;
revoke all on function public.pass_word_siege_turn_v1(uuid) from public, anon;
revoke all on function public.exchange_word_siege_tiles_v1(uuid, jsonb) from public, anon;
grant execute on function public.submit_word_siege_move_v1(uuid, jsonb, boolean) to authenticated;
grant execute on function public.pass_word_siege_turn_v1(uuid) to authenticated;
grant execute on function public.exchange_word_siege_tiles_v1(uuid, jsonb) to authenticated;

select pg_notify('pgrst', 'reload schema');
