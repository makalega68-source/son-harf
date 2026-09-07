-- Stabilize the live classic duel loop after Bil Bakalim was removed.
-- 1) A disabled trivia feature must never push an active duel into status='quiz'.
-- 2) Normal-mode long words earn a small server-authoritative bonus.
-- 3) A normal timeout awards +1 to the opponent without subtracting earned score.

update public.app_config
set value = 'false'::jsonb
where key = 'trivia_enabled';

create or replace function public.skip_disabled_quiz_transition_v1()
returns trigger
language plpgsql
security definer
set search_path to 'public','pg_temp'
as $$
begin
  if new.status = 'quiz'
     and old.status <> 'quiz'
     and not public.sonharf_config_enabled('trivia_enabled', false) then
    -- The word submit already selected the next authoritative turn before the
    -- obsolete quiz transition. Preserve that exact turn/deadline instead of
    -- unmounting the duel or manufacturing a new timer.
    new.status := case
      when old.status in ('playing','final','sudden_death','paused') then old.status
      else 'playing'
    end;
    new.current_player_id := old.current_player_id;
    new.bot_turn := old.bot_turn;
    new.turn_deadline := old.turn_deadline;
    new.last_event := 'bilbakalim_skipped';
    new.last_event_player_id := null;

    update public.trivia_rounds
    set resolved_at = coalesce(resolved_at, clock_timestamp()),
        result_until = coalesce(result_until, clock_timestamp())
    where room_id = new.id
      and resolved_at is null;
  end if;

  return new;
end
$$;

revoke all on function public.skip_disabled_quiz_transition_v1() from public, anon;

drop trigger if exists aaa_skip_disabled_quiz_transition_v1 on public.game_rooms;
create trigger aaa_skip_disabled_quiz_transition_v1
before update of status on public.game_rooms
for each row execute function public.skip_disabled_quiz_transition_v1();

-- Recover any room that entered quiz immediately before this guard was installed.
with latest_round as (
  select distinct on (q.room_id)
    q.room_id,
    q.resume_status,
    q.resume_current_player_id,
    coalesce(q.resume_bot_turn, false) as resume_bot_turn,
    q.resume_turn_remaining_ms
  from public.trivia_rounds q
  join public.game_rooms r on r.id = q.room_id and r.status = 'quiz'
  order by q.room_id, q.reveal_at desc
)
update public.game_rooms r
set status = case
      when q.resume_status in ('playing','final','sudden_death') then q.resume_status
      else 'playing'
    end,
    current_player_id = case
      when q.resume_bot_turn then null
      else coalesce(q.resume_current_player_id, r.host_id)
    end,
    bot_turn = q.resume_bot_turn,
    turn_deadline = case
      when q.resume_bot_turn then null
      else public.sonharf_turn_deadline(r.game_mode)
    end,
    last_event = 'bilbakalim_removed',
    last_event_player_id = null
from latest_round q
where r.id = q.room_id and r.status = 'quiz';

update public.game_rooms
set status = 'playing',
    current_player_id = coalesce(current_player_id, host_id),
    bot_turn = false,
    turn_deadline = public.sonharf_turn_deadline(game_mode),
    last_event = 'bilbakalim_removed',
    last_event_player_id = null
where status = 'quiz';

update public.trivia_rounds
set resolved_at = coalesce(resolved_at, clock_timestamp()),
    result_until = coalesce(result_until, clock_timestamp())
where resolved_at is null;

-- Small tactical long-word bonus for classic normal mode.
-- Existing exact scoring remains authoritative (+10 correct); this trigger only
-- adds +1/+2/+3 before the normal score update, so the exact-scoring trigger
-- preserves the bonus while round results also see it.
create or replace function public.award_normal_long_word_bonus_v1()
returns trigger
language plpgsql
security definer
set search_path to 'public','pg_temp'
as $$
declare
  r public.game_rooms;
  v_len integer;
  v_bonus integer := 0;
begin
  select * into r
  from public.game_rooms
  where id = new.room_id
  for update;

  if r.id is null
     or r.status <> 'playing'
     or coalesce(r.game_mode, 'normal') = 'expert' then
    return new;
  end if;

  v_len := char_length(coalesce(new.normalized_word, new.word, ''));
  v_bonus := case
    when v_len >= 10 then 3
    when v_len >= 8 then 2
    when v_len >= 6 then 1
    else 0
  end;

  if v_bonus = 0 then
    return new;
  end if;

  if coalesce(new.is_bot, false) then
    update public.game_rooms
    set guest_score = guest_score + v_bonus,
        guest_round_score = guest_round_score + v_bonus
    where id = r.id;
  elsif new.player_id = r.host_id then
    update public.game_rooms
    set host_score = host_score + v_bonus,
        host_round_score = host_round_score + v_bonus
    where id = r.id;
  elsif new.player_id = r.guest_id then
    update public.game_rooms
    set guest_score = guest_score + v_bonus,
        guest_round_score = guest_round_score + v_bonus
    where id = r.id;
  end if;

  return new;
end
$$;

revoke all on function public.award_normal_long_word_bonus_v1() from public, anon;

drop trigger if exists game_words_normal_long_word_bonus_v1 on public.game_words;
create trigger game_words_normal_long_word_bonus_v1
after insert on public.game_words
for each row execute function public.award_normal_long_word_bonus_v1();

-- Timeout comeback mechanic: preserve already-earned score and award the other
-- side +1. Sudden death keeps its existing decisive timeout behavior.
create or replace function public.claim_turn_timeout_core_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public','pg_temp'
as $$
declare
  r public.game_rooms;
  timed uuid;
begin
  select * into r
  from public.game_rooms
  where id = p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status not in ('playing','sudden_death')
     or r.turn_deadline is null
     or r.turn_deadline >= now() then
    return r;
  end if;

  timed := r.current_player_id;
  if timed is null then return r; end if;

  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then
    raise exception 'not_participant';
  end if;

  if r.status = 'sudden_death' then
    return public.sonharf_finish_room(
      r.id,
      case
        when r.is_bot then null
        else case when timed = r.host_id then r.guest_id else r.host_id end
      end,
      r.is_bot and timed = r.host_id,
      'turn_expired'
    );
  end if;

  if timed = r.host_id then
    update public.game_rooms
    set host_streak = 0,
        guest_score = guest_score + 1,
        guest_round_score = guest_round_score + 1,
        current_player_id = case when r.is_bot then null else r.guest_id end,
        bot_turn = r.is_bot,
        turn_deadline = case when r.is_bot then null else public.sonharf_turn_deadline(r.game_mode) end,
        last_event = 'turn_expired',
        last_event_player_id = timed
    where id = r.id
    returning * into r;
  else
    update public.game_rooms
    set guest_streak = 0,
        host_score = host_score + 1,
        host_round_score = host_round_score + 1,
        current_player_id = r.host_id,
        bot_turn = false,
        turn_deadline = public.sonharf_turn_deadline(r.game_mode),
        last_event = 'turn_expired',
        last_event_player_id = timed
    where id = r.id
    returning * into r;
  end if;

  return r;
end
$$;

revoke all on function public.claim_turn_timeout_core_v1(uuid) from public, anon, authenticated;

grant execute on function public.claim_turn_timeout(uuid) to authenticated;

select pg_notify('pgrst','reload schema');
