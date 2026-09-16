-- G4.1 Maç İçi: lives + shrinking turn + speed / hard-letter / long-word bonuses.
--
-- Design: purely additive. The existing scoring functions
-- (submit_word, switch_turn_after_failure, claim_turn_timeout,
-- bot_take_turn_normal_v1 / _expert_v1) stay unchanged; three triggers
-- layer G4.1 on top so we don't risk touching that intricate state
-- machine. The rules apply equally to the bot ("bot da aynı kurallarla
-- oynar") because the trigger reads game_words regardless of is_bot.

set search_path = public, pg_temp;

-- ---------------------------------------------------------------------
-- 1. New columns on game_rooms.
-- ---------------------------------------------------------------------
alter table public.game_rooms
    add column if not exists host_lives smallint not null default 3,
    add column if not exists guest_lives smallint not null default 3,
    add column if not exists turn_shrink_step smallint not null default 0;

-- Backfill: existing playing rooms get the default 3.
update public.game_rooms
   set host_lives = 3
 where host_lives is null;
update public.game_rooms
   set guest_lives = 3
 where guest_lives is null;

-- ---------------------------------------------------------------------
-- 2. Helper functions.
-- ---------------------------------------------------------------------

-- Turn duration in seconds by shrink step. The floor is 8s per spec:
--   step 0 -> 15s, 1 -> 12s, 2 -> 10s, 3+ -> 8s
create or replace function public.sonharf_g41_turn_seconds(p_step int)
returns int
language sql
immutable
as $$
    select case
        when coalesce(p_step, 0) <= 0 then 15
        when p_step = 1 then 12
        when p_step = 2 then 10
        else 8
    end;
$$;

-- Speed bonus 0..5, linearly scaled by remaining time.
-- p_deadline: original turn deadline. p_base_seconds: full turn length.
create or replace function public.sonharf_g41_speed_bonus(
    p_deadline timestamptz,
    p_base_seconds int
)
returns int
language plpgsql
immutable
as $$
declare
    v_remaining numeric;
    v_frac numeric;
begin
    if p_deadline is null or p_base_seconds is null or p_base_seconds <= 0 then
        return 0;
    end if;
    v_remaining := extract(epoch from (p_deadline - now()));
    if v_remaining <= 0 then return 0; end if;
    v_frac := least(v_remaining / p_base_seconds, 1);
    return greatest(0, least(5, floor(v_frac * 5)::int));
end;
$$;

-- Hard-letter terminal bonus. Turkish list: J F V C H (Ğ is NOT included
-- and Ğ-terminal is otherwise blocked). English list: Q X Z J.
create or replace function public.sonharf_g41_hard_letter_bonus(
    p_word text,
    p_language text
)
returns int
language sql
immutable
as $$
    select case
        when p_word is null or length(p_word) = 0 then 0
        when p_language = 'tr' and right(p_word, 1) in ('j','f','v','c','h') then 3
        when p_language = 'en' and right(p_word, 1) in ('q','x','z','j') then 3
        else 0
    end;
$$;

-- Long word bonus: 2 for 7+, 4 for 9+.
create or replace function public.sonharf_g41_long_word_bonus(p_word text)
returns int
language sql
immutable
as $$
    select case
        when p_word is null then 0
        when char_length(p_word) >= 9 then 4
        when char_length(p_word) >= 7 then 2
        else 0
    end;
$$;

-- ---------------------------------------------------------------------
-- 3. After-insert trigger on game_words: apply the three bonuses to the
--    player who just played. Also increments turn_shrink_step every 5
--    valid words so the NEXT turn is shorter.
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g41_apply_word_bonuses()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    r public.game_rooms;
    v_bonus int := 0;
    v_base_seconds int;
    v_valid_after int;
    v_next_step int;
begin
    select * into r from public.game_rooms where id = new.room_id;
    if r.id is null then return new; end if;

    -- valid_word_count reflects the count BEFORE this insert; the calling
    -- function will increment it right after. We take that into account
    -- for the shrink step calculation below.
    v_base_seconds := public.sonharf_g41_turn_seconds(r.turn_shrink_step);
    v_bonus := v_bonus + public.sonharf_g41_speed_bonus(r.turn_deadline, v_base_seconds);
    v_bonus := v_bonus + public.sonharf_g41_hard_letter_bonus(new.normalized_word, r.language);
    v_bonus := v_bonus + public.sonharf_g41_long_word_bonus(new.normalized_word);

    -- Attribute the bonus to whoever played the word. Bot moves get the
    -- guest column; player moves route by player_id.
    if v_bonus > 0 then
        if new.is_bot then
            update public.game_rooms
               set guest_score = guest_score + v_bonus
             where id = r.id;
        elsif new.player_id = r.host_id then
            update public.game_rooms
               set host_score = host_score + v_bonus
             where id = r.id;
        else
            update public.game_rooms
               set guest_score = guest_score + v_bonus
             where id = r.id;
        end if;
    end if;

    -- Shrink the next turn every 5 valid words. Step is bounded by
    -- sonharf_g41_turn_seconds's floor (8s at step >= 3).
    v_valid_after := coalesce(r.valid_word_count, 0) + 1;
    v_next_step := least(3, v_valid_after / 5);
    if v_next_step <> r.turn_shrink_step then
        update public.game_rooms
           set turn_shrink_step = v_next_step
         where id = r.id;
    end if;

    return new;
end;
$$;

drop trigger if exists game_words_apply_g41_bonuses on public.game_words;
create trigger game_words_apply_g41_bonuses
    after insert on public.game_words
    for each row execute function public.sonharf_g41_apply_word_bonuses();

-- ---------------------------------------------------------------------
-- 4. Life decrement + match finish on failure.
--    switch_turn_after_failure and claim_turn_timeout already set
--    last_event to 'turn_expired' when a turn expires. That's our hook.
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g41_handle_failure()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_reason text;
    v_who uuid;
    v_finished boolean := false;
begin
    v_reason := new.last_event;
    v_who := new.last_event_player_id;

    -- React to any failure reason set by switch_turn_after_failure or
    -- claim_turn_timeout. The existing functions set last_event to the
    -- caller-supplied reason string ('invalid_word', 'not_in_dictionary',
    -- 'wrong_start_letter', 'word_already_used', 'turn_expired'). Anything
    -- else (e.g. 'valid_word', 'streak_bonus', 'quiz_started', 'sudden_death_*',
    -- 'round_started') is a success or state signal and must not cost a life.
    if v_reason is null or v_reason not in (
        'invalid_word', 'not_in_dictionary', 'wrong_start_letter',
        'word_already_used', 'turn_expired'
    ) then
        return new;
    end if;
    -- Same failure repeated on the same player in a single update: skip.
    if old.last_event is not null and old.last_event = v_reason
        and old.last_event_player_id is not distinct from v_who then
        return new;
    end if;
    if v_who is null then return new; end if;

    -- Decrement whoever just failed.
    if v_who = new.host_id then
        new.host_lives := greatest(0, new.host_lives - 1);
        v_finished := new.host_lives <= 0;
        if v_finished then
            new.status := 'finished';
            new.winner_id := new.guest_id;
            new.finished_at := coalesce(new.finished_at, now());
            new.turn_deadline := null;
        end if;
    elsif v_who = new.guest_id then
        new.guest_lives := greatest(0, new.guest_lives - 1);
        v_finished := new.guest_lives <= 0;
        if v_finished then
            new.status := 'finished';
            new.winner_id := new.host_id;
            new.finished_at := coalesce(new.finished_at, now());
            new.turn_deadline := null;
        end if;
    end if;

    return new;
end;
$$;

drop trigger if exists game_rooms_g41_handle_failure on public.game_rooms;
-- BEFORE UPDATE so we can rewrite fields (lives, status, winner_id) in
-- the same row-write, avoiding a recursive UPDATE from within the trigger.
create trigger game_rooms_g41_handle_failure
    before update on public.game_rooms
    for each row execute function public.sonharf_g41_handle_failure();

-- ---------------------------------------------------------------------
-- 5. Shrinking turn deadline. When the caller extends the deadline
--    with the legacy 45s literal (or any longer value) and the room
--    has advanced past the first shrink step, we cap it at the G4.1
--    length. This lets the existing functions stay untouched.
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g41_cap_turn_deadline()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_cap_seconds int;
    v_cap_deadline timestamptz;
begin
    -- Only cap deadlines that were just set (moved forward from OLD).
    if new.turn_deadline is null then return new; end if;
    if old.turn_deadline is not null and new.turn_deadline <= old.turn_deadline then
        return new;
    end if;
    if new.status not in ('playing', 'final', 'sudden_death') then
        return new;
    end if;

    v_cap_seconds := public.sonharf_g41_turn_seconds(new.turn_shrink_step);
    v_cap_deadline := now() + make_interval(secs => v_cap_seconds);
    if new.turn_deadline > v_cap_deadline then
        new.turn_deadline := v_cap_deadline;
    end if;
    return new;
end;
$$;

drop trigger if exists game_rooms_g41_cap_turn_deadline on public.game_rooms;
create trigger game_rooms_g41_cap_turn_deadline
    before update on public.game_rooms
    for each row execute function public.sonharf_g41_cap_turn_deadline();

-- ---------------------------------------------------------------------
-- 6. Reset lives + shrink step on round start. When round_no advances,
--    both players regain their 3 lives and the turn shrinks reset so
--    a long match doesn't bottom out mid-round.
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g41_reset_on_round_start()
returns trigger
language plpgsql
as $$
begin
    if new.round_no is distinct from old.round_no and new.round_no > 1 then
        new.host_lives := 3;
        new.guest_lives := 3;
        new.turn_shrink_step := 0;
    end if;
    return new;
end;
$$;

drop trigger if exists game_rooms_g41_reset_on_round on public.game_rooms;
create trigger game_rooms_g41_reset_on_round
    before update on public.game_rooms
    for each row execute function public.sonharf_g41_reset_on_round_start();

select pg_notify('pgrst', 'reload schema');
