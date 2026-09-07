-- Word Siege v5 / 6: current-zone final scoring and server 45-second turn clock.

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

revoke all on function private.finish_word_siege_game_v1(uuid, text, uuid) from public, anon, authenticated;
revoke all on function private.word_siege_turn_clock_v5() from public, anon, authenticated;
revoke all on function private.expire_word_siege_turn_v1(uuid) from public, anon, authenticated;
revoke all on function public.expire_word_siege_turn_v1(uuid) from public, anon;
grant execute on function public.expire_word_siege_turn_v1(uuid) to authenticated;

select pg_notify('pgrst', 'reload schema');
