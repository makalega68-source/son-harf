-- Kelime Kusatmasi: 12/72 hour per-turn asynchronous deadlines.
-- Extends the existing word_siege_games lifecycle; no parallel game system.

alter table public.word_siege_games
  add column if not exists turn_duration_hours smallint not null default 12,
  add column if not exists turn_started_at timestamptz,
  add column if not exists turn_deadline timestamptz,
  add column if not exists loser_id uuid references public.profiles(id) on delete set null;

alter table public.word_siege_games
  drop constraint if exists word_siege_games_turn_duration_hours_check;
alter table public.word_siege_games
  add constraint word_siege_games_turn_duration_hours_check
  check (turn_duration_hours in (12, 72));

-- Existing waiting games enter the 12-hour pool. Existing playing games, if any,
-- receive a fresh bounded turn window from migration time rather than being
-- retroactively timed out by old timestamps.
update public.word_siege_games
set turn_duration_hours = case when turn_duration_hours in (12, 72) then turn_duration_hours else 12 end
where turn_duration_hours not in (12, 72);

update public.word_siege_games
set turn_started_at = coalesce(turn_started_at, now()),
    turn_deadline = coalesce(turn_deadline, now() + make_interval(hours => turn_duration_hours))
where status = 'playing'
  and current_player_id is not null
  and turn_deadline is null;

create index if not exists word_siege_games_waiting_duration_idx
  on public.word_siege_games(language, turn_duration_hours, created_at)
  where status = 'waiting';
create index if not exists word_siege_games_deadline_idx
  on public.word_siege_games(turn_deadline)
  where status = 'playing' and turn_deadline is not null;

create or replace function private.word_siege_finalize_timeout_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_loser uuid;
  v_winner uuid;
begin
  select * into r
  from public.word_siege_games
  where id = p_game_id
  for update;

  if r.id is null then return null; end if;
  if r.status <> 'playing' or r.current_player_id is null or r.turn_deadline is null then
    return r;
  end if;
  if clock_timestamp() < r.turn_deadline then return r; end if;

  v_loser := r.current_player_id;
  v_winner := case when v_loser = r.player_one_id then r.player_two_id else r.player_one_id end;

  -- The status predicate makes this idempotent even when a client refresh and
  -- the server sweep race with each other.
  update public.word_siege_games
  set status = 'finished',
      current_player_id = null,
      winner_id = v_winner,
      loser_id = v_loser,
      finish_reason = 'timeout',
      result_applied = true,
      last_action = 'timeout',
      last_action_player_id = v_loser,
      last_move_at = coalesce(last_move_at, turn_started_at),
      turn_started_at = null,
      turn_deadline = null,
      finished_at = clock_timestamp(),
      updated_at = clock_timestamp()
  where id = r.id
    and status = 'playing'
    and current_player_id = v_loser
    and turn_deadline is not null
    and clock_timestamp() >= turn_deadline
  returning * into r;

  if r.id is null then
    select * into r from public.word_siege_games where id = p_game_id;
  end if;
  return r;
end
$$;

create or replace function private.word_siege_prepare_turn_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  select * into r
  from public.word_siege_games
  where id = p_game_id
  for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  if r.status = 'playing' and r.turn_deadline is not null and clock_timestamp() >= r.turn_deadline then
    r := private.word_siege_finalize_timeout_v2(r.id);
    return r;
  end if;

  if r.status <> 'playing' then raise exception 'word_siege_not_playing'; end if;
  if r.current_player_id <> v_uid then raise exception 'word_siege_not_your_turn'; end if;
  return r;
end
$$;

create or replace function private.word_siege_arm_next_turn_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_now timestamptz := clock_timestamp();
begin
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if r.status = 'playing' and r.current_player_id is not null then
    update public.word_siege_games
    set turn_started_at = v_now,
        turn_deadline = v_now + make_interval(hours => turn_duration_hours),
        updated_at = v_now
    where id = r.id
    returning * into r;
  else
    update public.word_siege_games
    set turn_started_at = null, turn_deadline = null
    where id = r.id
    returning * into r;
  end if;
  return r;
end
$$;

-- Keep the existing result path but persist loser/deadline semantics for every
-- finish reason (normal, passes, rack empty, forfeit).
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

  v_one_total := r.player_one_word_score + r.player_one_area;
  v_two_total := r.player_two_word_score + r.player_two_area;
  if p_forfeit_winner is not null then
    if p_forfeit_winner not in (r.player_one_id, r.player_two_id) then
      raise exception 'word_siege_invalid_winner';
    end if;
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

create or replace function private.find_or_create_word_siege_game_v2(
  p_language text default 'tr',
  p_turn_duration_hours integer default 12
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_language text := case when lower(coalesce(p_language, 'tr')) = 'en' then 'en' else 'tr' end;
  v_duration smallint;
  v_bag text;
  v_rack text;
  v_active_count integer;
  v_now timestamptz := clock_timestamp();
  r public.word_siege_games;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not exists (select 1 from public.profiles p where p.id = v_uid) then
    raise exception 'word_siege_profile_required';
  end if;
  if p_turn_duration_hours not in (12, 72) then raise exception 'word_siege_invalid_turn_duration'; end if;
  v_duration := p_turn_duration_hours::smallint;

  perform pg_advisory_xact_lock(hashtextextended('word_siege:' || v_language || ':' || v_duration::text, 0));

  select count(*)::integer into v_active_count
  from public.word_siege_games g
  where g.status in ('waiting', 'playing')
    and v_uid in (g.player_one_id, g.player_two_id);
  if v_active_count >= 10 then raise exception 'word_siege_active_limit'; end if;

  select * into r
  from public.word_siege_games g
  where g.status = 'waiting'
    and g.player_one_id = v_uid
    and g.language = v_language
    and g.turn_duration_hours = v_duration
  order by g.created_at
  limit 1;
  if r.id is not null then return r; end if;

  select * into r
  from public.word_siege_games g
  where g.status = 'waiting'
    and g.language = v_language
    and g.turn_duration_hours = v_duration
    and g.player_one_id <> v_uid
    and not exists (
      select 1 from public.user_blocks b
      where (b.blocker_id = v_uid and b.blocked_id = g.player_one_id)
         or (b.blocker_id = g.player_one_id and b.blocked_id = v_uid)
    )
  order by g.created_at
  for update skip locked
  limit 1;

  if r.id is not null then
    update public.word_siege_games
    set player_two_id = v_uid,
        player_two_rack = substring(r.bag from 1 for 7),
        bag = substring(r.bag from 8),
        status = 'playing',
        current_player_id = r.player_one_id,
        turn_started_at = v_now,
        turn_deadline = v_now + make_interval(hours => v_duration),
        last_action = 'game_started',
        last_action_player_id = null,
        updated_at = v_now
    where id = r.id
    returning * into r;
    return r;
  end if;

  v_bag := private.word_siege_new_bag_v1(v_language);
  v_rack := substring(v_bag from 1 for 7);
  v_bag := substring(v_bag from 8);
  insert into public.word_siege_games(
    player_one_id, language, turn_duration_hours, board, bag, player_one_rack, last_action
  ) values (
    v_uid, v_language, v_duration, private.word_siege_new_board_v1(), v_bag, v_rack, 'waiting_for_opponent'
  )
  returning * into r;
  return r;
end
$$;

create or replace function public.find_or_create_word_siege_game_v2(
  p_language text default 'tr',
  p_turn_duration_hours integer default 12
)
returns public.word_siege_games
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$ select private.find_or_create_word_siege_game_v2(p_language, p_turn_duration_hours) $$;

-- Legacy clients remain compatible and enter the 12-hour pool instead of
-- creating an unlimited-time queue after this migration.
create or replace function public.find_or_create_word_siege_game_v1(p_language text default 'tr')
returns public.word_siege_games
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$ select private.find_or_create_word_siege_game_v2(p_language, 12) $$;

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
  r := private.word_siege_prepare_turn_v2(p_game_id);
  if r.status <> 'playing' then return r; end if;
  perform private.word_siege_prevalidate_move_v2(p_game_id, p_placements, p_horizontal);
  r := private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal);
  if r.status = 'playing' then r := private.word_siege_arm_next_turn_v2(r.id); end if;
  return r;
end
$$;

create or replace function public.pass_word_siege_turn_v1(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare r public.word_siege_games;
begin
  r := private.word_siege_prepare_turn_v2(p_game_id);
  if r.status <> 'playing' then return r; end if;
  r := private.pass_word_siege_turn_v1(p_game_id);
  if r.status = 'playing' then r := private.word_siege_arm_next_turn_v2(r.id); end if;
  return r;
end
$$;

create or replace function public.exchange_word_siege_tiles_v1(p_game_id uuid, p_rack_indices jsonb)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare r public.word_siege_games;
begin
  r := private.word_siege_prepare_turn_v2(p_game_id);
  if r.status <> 'playing' then return r; end if;
  r := private.exchange_word_siege_tiles_v1(p_game_id, p_rack_indices);
  if r.status = 'playing' then r := private.word_siege_arm_next_turn_v2(r.id); end if;
  return r;
end
$$;

create or replace function public.forfeit_word_siege_game_v1(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status = 'playing' and r.turn_deadline is not null and clock_timestamp() >= r.turn_deadline then
    return private.word_siege_finalize_timeout_v2(r.id);
  end if;
  return private.forfeit_word_siege_game_v1(r.id);
end
$$;

create or replace function public.refresh_word_siege_game_v2(p_game_id uuid)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  if r.status = 'playing' and r.turn_deadline is not null and clock_timestamp() >= r.turn_deadline then
    r := private.word_siege_finalize_timeout_v2(r.id);
  end if;
  return r;
end
$$;

create or replace function public.refresh_my_word_siege_games_v2()
returns setof public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_game_id uuid;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  for v_game_id in
    select g.id
    from public.word_siege_games g
    where g.status = 'playing'
      and g.turn_deadline is not null
      and clock_timestamp() >= g.turn_deadline
      and v_uid in (g.player_one_id, g.player_two_id)
    order by g.turn_deadline
  loop
    perform private.word_siege_finalize_timeout_v2(v_game_id);
  end loop;

  return query
  select g.*
  from public.word_siege_games g
  where v_uid in (g.player_one_id, g.player_two_id)
    and g.status <> 'cancelled'
  order by coalesce(g.updated_at, g.created_at) desc;
end
$$;

create or replace function private.sweep_word_siege_timeouts_v2()
returns integer
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_game_id uuid;
  v_count integer := 0;
begin
  for v_game_id in
    select g.id
    from public.word_siege_games g
    where g.status = 'playing'
      and g.turn_deadline is not null
      and clock_timestamp() >= g.turn_deadline
    order by g.turn_deadline
    for update skip locked
  loop
    perform private.word_siege_finalize_timeout_v2(v_game_id);
    v_count := v_count + 1;
  end loop;
  return v_count;
end
$$;

revoke all on function private.word_siege_finalize_timeout_v2(uuid) from public, anon, authenticated;
revoke all on function private.word_siege_prepare_turn_v2(uuid) from public, anon, authenticated;
revoke all on function private.word_siege_arm_next_turn_v2(uuid) from public, anon, authenticated;
revoke all on function private.find_or_create_word_siege_game_v2(text, integer) from public, anon, authenticated;
revoke all on function private.sweep_word_siege_timeouts_v2() from public, anon, authenticated;

revoke all on function public.find_or_create_word_siege_game_v2(text, integer) from public, anon, authenticated;
revoke all on function public.refresh_word_siege_game_v2(uuid) from public, anon, authenticated;
revoke all on function public.refresh_my_word_siege_games_v2() from public, anon, authenticated;
grant execute on function public.find_or_create_word_siege_game_v2(text, integer) to authenticated;
grant execute on function public.refresh_word_siege_game_v2(uuid) to authenticated;
grant execute on function public.refresh_my_word_siege_games_v2() to authenticated;

-- True backend timeout finalization. The mutating RPC guard remains authoritative,
-- so a cron run delayed by seconds can never make a late move valid.
do $$
begin
  begin
    create extension if not exists pg_cron;
  exception when others then
    raise notice 'pg_cron unavailable; client refresh guards still enforce deadlines: %', sqlerrm;
  end;
end
$$;

do $$
begin
  if exists (select 1 from pg_extension where extname = 'pg_cron') then
    perform cron.unschedule(jobid)
    from cron.job
    where jobname = 'word_siege_timeout_sweep_v2';
    perform cron.schedule(
      'word_siege_timeout_sweep_v2',
      '* * * * *',
      'select private.sweep_word_siege_timeouts_v2();'
    );
  end if;
end
$$;