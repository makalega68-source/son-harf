-- Son Harf bot balance: scoring is 10 points per word (plus a small long-word bonus), so the
-- bot's rubber band now measures the lead in words, and the normal bot prefers everyday
-- 5-7 letter words instead of chasing long-word bonuses. Only the tier/skill maths changes.

begin;

create or replace function public.bot_take_turn_normal_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  r public.game_rooms;
  previous_word text;
  expected text;
  v_letter text;
  v_letter_count integer := 0;
  v_max_count integer := 1;
  v_offset integer := 0;
  v_choice_id public.dictionary_words.id%type;
  chosen public.dictionary_words;
  streak_value integer;
  add_points integer := 3;
  next_round integer;
  difficulty text := 'normal';
  requested text := 'normal';
  rating_value integer := 1000;
  total_value integer := 0;
  wins_value integer := 0;
  v_lead integer := 0;
  v_lead_words numeric := 0;
  v_skill numeric := .56;
  v_miss numeric := 0;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
  if r.game_mode = 'expert' then raise exception 'expert_bot_use_expert_turn'; end if;
  if auth.uid() <> r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing', 'final', 'sudden_death') or not r.bot_turn then return r; end if;

  if r.status <> 'sudden_death' and r.guest_round_words >= 10 then
    update public.game_rooms
    set current_player_id = host_id,
        bot_turn = false,
        turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id = r.id
    returning * into r;
    return r;
  end if;

  select coalesce(p.rating, 1000), coalesce(p.total_matches, 0), coalesce(p.wins, 0), coalesce(p.bot_difficulty, 'normal')
  into rating_value, total_value, wins_value, requested
  from public.profiles p
  where p.id = r.host_id;

  difficulty := public.sonharf_adaptive_bot_tier_v1(rating_value, total_value, wins_value, requested);

  -- Skill: tier base, gentler for beginners, rubber-banded on the round and match score.
  v_lead := coalesce(r.guest_round_score, 0) - coalesce(r.host_round_score, 0);
  v_skill := case when difficulty = 'expert' then .82 else .56 end;
  if total_value < 6 then v_skill := v_skill - .12; end if;
  -- A correct word is worth about 10 points, so the lead is measured in words.
  v_lead_words := v_lead / 10.0;
  v_skill := v_skill
    - greatest(-3, least(3, v_lead_words)) * .09
    - (coalesce(r.guest_rounds, 0) - coalesce(r.host_rounds, 0)) * .06;
  v_skill := greatest(.12, least(.95, v_skill));

  -- A human-like slip: never in sudden death, never when the player has no turns left this round.
  if r.status <> 'sudden_death' and r.host_round_words < 10 then
    v_miss := .015 + (1 - v_skill) * .07 + case when v_lead_words >= 2 then .06 else 0 end;
    if random() < v_miss then
      update public.game_rooms
      set guest_score = guest_score - 1,
          guest_round_score = guest_round_score - 1,
          guest_streak = 0,
          current_player_id = host_id,
          bot_turn = false,
          turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event = 'bot_missed',
          last_event_player_id = null
      where id = r.id
      returning * into r;
      return r;
    end if;
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id = r.id
  order by id desc
  limit 1;
  expected := case when previous_word is null then null else right(previous_word, 1) end;

  -- The opening word of a round starts from a random, well-populated letter.
  v_letter := expected;
  if v_letter is null then
    select o.letter into v_letter
    from public.sonharf_letter_openings o
    where o.language = r.language and o.word_count >= 800
    order by random()
    limit 1;
  end if;
  -- Always a concrete letter, so the prefix index is used even with a cached generic plan.
  v_letter := coalesce(v_letter, 'a');

  select coalesce(max(o.word_count), 1) into v_max_count
  from public.sonharf_letter_openings o
  where o.language = r.language;
  select coalesce(max(o.word_count), 0) into v_letter_count
  from public.sonharf_letter_openings o
  where o.language = r.language and o.letter = v_letter;
  v_offset := floor(random() * greatest(0, least(v_letter_count, 4000) - 220))::integer;

  -- Take a cheap index slice first, then run the costlier word filters only on that slice.
  with slice as materialized (
    select d.id, d.language, d.normalized_word
    from public.dictionary_words d
    where d.language = r.language
      and d.active
      and d.game_allowed
      and not d.is_abbreviation
      and not d.is_proper_noun
      and left(d.normalized_word, 1) = v_letter
    order by d.normalized_word
    offset v_offset limit 220
  ),
  pool as (
    select s.id, s.normalized_word
    from slice s
    where public.sonharf_bot_word_allowed(s.language, s.normalized_word)
      and not exists (
        select 1 from public.game_words w
        where w.room_id = r.id and w.normalized_word = s.normalized_word
      )
  ),
  scored as (
    select p.id,
      -- How hard the next player's letter is: fewer words starting with it is better.
      1 - ln(1 + coalesce(o.word_count, 0)) / ln(1 + greatest(v_max_count, 1)) as trap,
      -- The long-word bonus the server awards (6+, 8+, 10+ letters).
      -- The normal bot plays everyday-length words like a person; the expert reaches for bonuses.
      case
        when difficulty = 'expert' then
          case
            when char_length(p.normalized_word) >= 10 then 1.0
            when char_length(p.normalized_word) >= 8 then .8
            when char_length(p.normalized_word) >= 6 then .55
            when char_length(p.normalized_word) >= 4 then .25
            else .05
          end
        else
          case
            when char_length(p.normalized_word) between 5 and 7 then .7
            when char_length(p.normalized_word) in (4, 8) then .45
            when char_length(p.normalized_word) >= 9 then .2
            else .1
          end
      end as length_value
    from pool p
    left join public.sonharf_letter_openings o
      on o.language = r.language and o.letter = right(p.normalized_word, 1)
  )
  select s.id into v_choice_id
  from scored s
  order by v_skill * (.55 * s.trap + .45 * s.length_value)
    + (1 - v_skill) * .9 * random()
    + .12 * random() desc
  limit 1;

  if v_choice_id is not null then
    select * into chosen from public.dictionary_words where id = v_choice_id;
  end if;

  if chosen.id is null then
    select d.* into chosen
    from public.dictionary_words d
    where d.language = r.language
      and d.active
      and d.game_allowed
      and not d.is_abbreviation
      and not d.is_proper_noun
      and (expected is null or left(d.normalized_word, 1) = expected)
      and public.sonharf_bot_word_allowed(d.language, d.normalized_word)
      and not exists (
        select 1 from public.game_words w
        where w.room_id = r.id and w.normalized_word = d.normalized_word
      )
    order by d.normalized_word
    limit 1;
  end if;

  if chosen.id is null then
    return public.sonharf_finish_room(r.id, r.host_id, false, 'bot_no_word');
  end if;

  insert into public.game_words(room_id, player_id, word, normalized_word, is_bot)
  values (r.id, null, chosen.word, chosen.normalized_word, true);

  if r.status = 'sudden_death' then
    return public.sonharf_finish_room(r.id, null, true, 'sudden_death_word');
  end if;

  streak_value := r.guest_streak + 1;
  if streak_value % 5 = 0 then add_points := 6; end if;

  update public.game_rooms
  set guest_score = guest_score + add_points,
      guest_round_score = guest_round_score + add_points,
      guest_streak = streak_value,
      guest_round_words = least(10, guest_round_words + 1),
      valid_word_count = valid_word_count + 1,
      round_word_count = round_word_count + 1,
      last_event = case when streak_value % 5 = 0 then 'streak_bonus' else 'valid_word' end,
      last_event_player_id = null
  where id = r.id
  returning * into r;

  if r.host_round_words >= 10 and r.guest_round_words >= 10 then
    update public.profiles
    set total_rounds = total_rounds + 1,
        rounds_won = rounds_won + case when r.host_round_score > r.guest_round_score then 1 else 0 end
    where id = r.host_id;

    update public.game_rooms
    set host_rounds = host_rounds + case when host_round_score > guest_round_score then 1 else 0 end,
        guest_rounds = guest_rounds + case when guest_round_score > host_round_score then 1 else 0 end
    where id = r.id
    returning * into r;

    if r.round_no >= 3 then
      if r.host_rounds > r.guest_rounds then
        return public.sonharf_finish_room(r.id, r.host_id, false, 'match_finished');
      elsif r.guest_rounds > r.host_rounds then
        return public.sonharf_finish_room(r.id, null, true, 'match_finished');
      else
        update public.game_rooms
        set status = 'sudden_death',
            round_word_count = 0,
            host_round_words = 0,
            guest_round_words = 0,
            host_round_score = 0,
            guest_round_score = 0,
            current_player_id = host_id,
            bot_turn = false,
            turn_deadline = public.sonharf_turn_deadline_for_round_v1(3),
            last_event = 'sudden_death_started'
        where id = r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round := r.round_no + 1;
    update public.game_rooms
    set round_no = next_round,
        round_word_count = 0,
        host_round_words = 0,
        guest_round_words = 0,
        host_round_score = 0,
        guest_round_score = 0,
        host_streak = 0,
        guest_streak = 0,
        current_player_id = case when next_round % 2 = 1 then host_id else null end,
        bot_turn = (next_round % 2 = 0),
        turn_deadline = case
          when next_round % 2 = 1 then public.sonharf_turn_deadline_for_round_v1(next_round)
          else null
        end,
        last_event = 'round_started'
    where id = r.id
    returning * into r;
    return r;
  end if;

  if r.host_round_words >= 10 then
    update public.game_rooms
    set current_player_id = null,
        bot_turn = true,
        turn_deadline = null
    where id = r.id
    returning * into r;
  else
    update public.game_rooms
    set current_player_id = host_id,
        bot_turn = false,
        turn_deadline = public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id = r.id
    returning * into r;
  end if;

  return r;
end
$$;

-- Only reachable through public.bot_take_turn (which checks the caller), as before.
revoke all on function public.bot_take_turn_normal_v1(uuid) from public, anon, authenticated;
grant execute on function public.bot_take_turn_normal_v1(uuid) to service_role;

commit;
