-- Son Harf: bilingual gameplay, scoring, streaks, quizzes, final phase, privacy and blocking

-- ------------------------------------------------------------
-- Profile privacy / social safety
-- ------------------------------------------------------------
alter table public.profiles
  add column if not exists avatar_visibility text not null default 'hidden'
    check (avatar_visibility in ('hidden','vip','match','selected')),
  add column if not exists allow_match_chat boolean not null default true;

create table if not exists public.profile_photo_access (
  owner_id uuid not null references public.profiles(id) on delete cascade,
  viewer_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (owner_id, viewer_id),
  check (owner_id <> viewer_id)
);

create table if not exists public.user_blocks (
  blocker_id uuid not null references public.profiles(id) on delete cascade,
  blocked_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  check (blocker_id <> blocked_id)
);

alter table public.profile_photo_access enable row level security;
alter table public.user_blocks enable row level security;

create policy "photo access owner manage" on public.profile_photo_access
for all to authenticated
using (owner_id = auth.uid())
with check (owner_id = auth.uid());

create policy "blocks owner manage" on public.user_blocks
for all to authenticated
using (blocker_id = auth.uid())
with check (blocker_id = auth.uid());

-- ------------------------------------------------------------
-- Language-aware dictionary and trivia
-- ------------------------------------------------------------
create table if not exists public.dictionary_words (
  id bigint generated always as identity primary key,
  language text not null check (language in ('tr','en')),
  word text not null,
  normalized_word text not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  unique(language, normalized_word)
);

create table if not exists public.trivia_questions (
  id bigint generated always as identity primary key,
  language text not null check (language in ('tr','en')),
  question text not null,
  option_a text not null,
  option_b text not null,
  option_c text not null,
  option_d text not null,
  correct_index smallint not null check (correct_index between 0 and 3),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.trivia_rounds (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  milestone integer not null,
  bonus_points integer not null,
  question_id bigint not null references public.trivia_questions(id),
  reveal_at timestamptz not null,
  winner_id uuid references public.profiles(id),
  resolved_at timestamptz,
  created_at timestamptz not null default now(),
  unique(room_id, milestone)
);

create table if not exists public.trivia_answers (
  id bigint generated always as identity primary key,
  round_id uuid not null references public.trivia_rounds(id) on delete cascade,
  player_id uuid not null references public.profiles(id) on delete cascade,
  answer_index smallint not null check (answer_index between 0 and 3),
  is_correct boolean not null,
  created_at timestamptz not null default now(),
  unique(round_id, player_id)
);

alter table public.dictionary_words enable row level security;
alter table public.trivia_questions enable row level security;
alter table public.trivia_rounds enable row level security;
alter table public.trivia_answers enable row level security;

create policy "dictionary authenticated read" on public.dictionary_words
for select to authenticated using (active);

create policy "trivia questions participant read" on public.trivia_questions
for select to authenticated using (active);

create policy "trivia rounds participant read" on public.trivia_rounds
for select to authenticated using (public.is_room_participant(room_id, auth.uid()));

create policy "trivia answers participant read" on public.trivia_answers
for select to authenticated using (
  exists (
    select 1 from public.trivia_rounds q
    where q.id = round_id and public.is_room_participant(q.room_id, auth.uid())
  )
);

-- ------------------------------------------------------------
-- Room state / score model
-- ------------------------------------------------------------
alter table public.game_rooms
  drop constraint if exists game_rooms_status_check;

alter table public.game_rooms
  add constraint game_rooms_status_check
  check (status in ('waiting','playing','quiz','final','sudden_death','finished','cancelled'));

alter table public.game_rooms
  add column if not exists language text not null default 'tr' check (language in ('tr','en')),
  add column if not exists host_score integer not null default 0,
  add column if not exists guest_score integer not null default 0,
  add column if not exists host_streak integer not null default 0,
  add column if not exists guest_streak integer not null default 0,
  add column if not exists valid_word_count integer not null default 0,
  add column if not exists final_moves_remaining integer not null default 0,
  add column if not exists last_event text,
  add column if not exists last_event_player_id uuid references public.profiles(id) on delete set null;

-- Starter dictionary entries for immediate smoke tests. Production dictionary should be bulk imported.
insert into public.dictionary_words(language, word, normalized_word) values
('tr','kalem','kalem'),('tr','metafizik','metafizik'),('tr','kalça','kalça'),('tr','arapça','arapça'),
('tr','masa','masa'),('tr','araba','araba'),('tr','armut','armut'),('tr','telefon','telefon'),
('en','apple','apple'),('en','elephant','elephant'),('en','table','table'),('en','eagle','eagle'),
('en','energy','energy'),('en','yellow','yellow'),('en','water','water'),('en','rabbit','rabbit')
on conflict (language, normalized_word) do nothing;

insert into public.trivia_questions(language, question, option_a, option_b, option_c, option_d, correct_index) values
('tr','Türkiye''nin başkenti hangisidir?','İstanbul','Ankara','İzmir','Bursa',1),
('tr','Dünya''nın en büyük okyanusu hangisidir?','Atlas','Hint','Pasifik','Arktik',2),
('tr','Bir yılda kaç ay vardır?','10','11','12','13',2),
('en','What is the capital of the United Kingdom?','London','Paris','Rome','Madrid',0),
('en','Which planet is known as the Red Planet?','Venus','Mars','Jupiter','Mercury',1),
('en','How many days are in a leap year?','364','365','366','367',2)
on conflict do nothing;

-- ------------------------------------------------------------
-- Helpers
-- ------------------------------------------------------------
create or replace function public.normalize_game_word(p_language text, p_word text)
returns text
language plpgsql immutable
as $$
declare
  w text;
begin
  w := trim(p_word);
  if p_language = 'tr' then
    -- Turkish lowercase handling that preserves dotless i semantics reasonably in Postgres locale-independent setups.
    w := translate(w, 'IİÇĞÖŞÜ', 'ıiçğöşü');
    w := lower(w);
  else
    w := lower(w);
  end if;
  return w;
end;
$$;

create or replace function public.switch_turn_after_failure(p_room_id uuid, p_reason text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  next_player uuid;
  penalty int;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.current_player_id then raise exception 'not_your_turn'; end if;

  penalty := case when r.status = 'final' then 2 else 1 end;

  if auth.uid() = r.host_id then
    update public.game_rooms
      set host_score = host_score - penalty,
          host_streak = 0
      where id = r.id;
  else
    update public.game_rooms
      set guest_score = guest_score - penalty,
          guest_streak = 0
      where id = r.id;
  end if;

  if r.status = 'sudden_death' then
    update public.game_rooms
       set status = 'finished',
           winner_id = case when auth.uid() = r.host_id then r.guest_id else r.host_id end,
           finished_at = now(),
           turn_deadline = null,
           last_event = p_reason,
           last_event_player_id = auth.uid()
     where id = r.id
     returning * into r;
    return r;
  end if;

  next_player := case when auth.uid() = r.host_id then r.guest_id else r.host_id end;
  update public.game_rooms
     set current_player_id = next_player,
         turn_deadline = now() + interval '45 seconds',
         last_event = p_reason,
         last_event_player_id = auth.uid(),
         final_moves_remaining = case when status = 'final' and final_moves_remaining > 0 then final_moves_remaining - 1 else final_moves_remaining end
   where id = r.id
   returning * into r;

  if r.status = 'final' and r.final_moves_remaining <= 0 then
    update public.game_rooms
       set status = case when host_score = guest_score then 'sudden_death' else 'finished' end,
           winner_id = case when host_score > guest_score then host_id when guest_score > host_score then guest_id else null end,
           finished_at = case when host_score = guest_score then null else now() end,
           turn_deadline = case when host_score = guest_score then now() + interval '45 seconds' else null end
     where id = r.id
     returning * into r;
  end if;

  return r;
end;
$$;

grant execute on function public.switch_turn_after_failure(uuid, text) to authenticated;

-- ------------------------------------------------------------
-- Create / join room with language
-- ------------------------------------------------------------
drop function if exists public.create_room();
create or replace function public.create_room(p_language text default 'tr')
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  generated_code text;
  attempts int := 0;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;

  loop
    attempts := attempts + 1;
    generated_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
    begin
      insert into public.game_rooms(code, host_id, status, current_player_id, turn_deadline, language)
      values (generated_code, auth.uid(), 'waiting', auth.uid(), now() + interval '45 seconds', p_language)
      returning * into r;
      exit;
    exception when unique_violation then
      if attempts >= 8 then raise; end if;
    end;
  end loop;
  return r;
end;
$$;
grant execute on function public.create_room(text) to authenticated;

create or replace function public.join_room_by_code(p_code text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
begin
  update public.game_rooms
    set guest_id = auth.uid(), status = 'playing', current_player_id = host_id, turn_deadline = now() + interval '45 seconds'
    where code = upper(trim(p_code)) and status = 'waiting' and guest_id is null and host_id <> auth.uid()
      and not exists (
        select 1 from public.user_blocks b
        where (b.blocker_id = host_id and b.blocked_id = auth.uid())
           or (b.blocker_id = auth.uid() and b.blocked_id = host_id)
      )
    returning * into r;
  if r.id is null then raise exception 'room_not_available'; end if;
  return r;
end;
$$;
grant execute on function public.join_room_by_code(text) to authenticated;

-- ------------------------------------------------------------
-- Word submission with scoring, dictionary validation and phases
-- ------------------------------------------------------------
create or replace function public.submit_word(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  clean_word text;
  previous_word text;
  expected_first text;
  actual_first text;
  next_player uuid;
  streak_value int;
  add_points int := 3;
  next_valid_count int;
  milestone int;
  bonus int;
  qid bigint;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;

  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status not in ('playing','final','sudden_death') then raise exception 'room_not_playing'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  if r.current_player_id <> auth.uid() then raise exception 'not_your_turn'; end if;

  if r.turn_deadline is not null and r.turn_deadline < now() then
    return public.switch_turn_after_failure(p_room_id, 'turn_expired');
  end if;

  clean_word := public.normalize_game_word(r.language, p_word);

  if char_length(clean_word) < 2 or char_length(clean_word) > 40 then
    return public.switch_turn_after_failure(p_room_id, 'invalid_word');
  end if;

  if (r.language = 'tr' and clean_word !~ '^[a-zçğıöşü]+$')
     or (r.language = 'en' and clean_word !~ '^[a-z]+$') then
    return public.switch_turn_after_failure(p_room_id, 'invalid_word');
  end if;

  if not exists (
    select 1 from public.dictionary_words d
    where d.language = r.language and d.normalized_word = clean_word and d.active
  ) then
    return public.switch_turn_after_failure(p_room_id, 'not_in_dictionary');
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id = p_room_id
  order by id desc
  limit 1;

  if previous_word is not null then
    expected_first := right(previous_word, 1);
    actual_first := left(clean_word, 1);
    if actual_first <> expected_first then
      return public.switch_turn_after_failure(p_room_id, 'wrong_start_letter');
    end if;
  end if;

  if exists(select 1 from public.game_words where room_id = p_room_id and normalized_word = clean_word) then
    return public.switch_turn_after_failure(p_room_id, 'word_already_used');
  end if;

  insert into public.game_words(room_id, player_id, word, normalized_word)
  values (p_room_id, auth.uid(), trim(p_word), clean_word);

  if auth.uid() = r.host_id then
    streak_value := r.host_streak + 1;
    if streak_value % 5 = 0 then add_points := add_points + 3; end if;
    update public.game_rooms
       set host_score = host_score + add_points,
           host_streak = streak_value
     where id = r.id;
  else
    streak_value := r.guest_streak + 1;
    if streak_value % 5 = 0 then add_points := add_points + 3; end if;
    update public.game_rooms
       set guest_score = guest_score + add_points,
           guest_streak = streak_value
     where id = r.id;
  end if;

  next_valid_count := r.valid_word_count + 1;
  next_player := case when auth.uid() = r.host_id then r.guest_id else r.host_id end;

  update public.game_rooms
     set valid_word_count = next_valid_count,
         current_player_id = next_player,
         turn_deadline = now() + interval '45 seconds',
         last_event = case when streak_value % 5 = 0 then 'streak_bonus' else 'valid_word' end,
         last_event_player_id = auth.uid(),
         final_moves_remaining = case when status = 'final' and final_moves_remaining > 0 then final_moves_remaining - 1 else final_moves_remaining end
   where id = r.id
   returning * into r;

  -- Every 15 valid words: general knowledge bonus. 6, 12, 24, ...
  if r.status = 'playing' and next_valid_count % 15 = 0 then
    milestone := next_valid_count;
    bonus := (6 * power(2, (milestone / 15) - 1))::int;

    select id into qid
    from public.trivia_questions
    where language = r.language and active
    order by random()
    limit 1;

    if qid is not null then
      insert into public.trivia_rounds(room_id, milestone, bonus_points, question_id, reveal_at)
      values (r.id, milestone, bonus, qid, now() + interval '3 seconds')
      on conflict (room_id, milestone) do nothing;

      update public.game_rooms
         set status = 'quiz', turn_deadline = null, last_event = 'quiz_started'
       where id = r.id
       returning * into r;
    end if;
  end if;

  if r.status = 'final' and r.final_moves_remaining <= 0 then
    update public.game_rooms
       set status = case when host_score = guest_score then 'sudden_death' else 'finished' end,
           winner_id = case when host_score > guest_score then host_id when guest_score > host_score then guest_id else null end,
           finished_at = case when host_score = guest_score then null else now() end,
           turn_deadline = case when host_score = guest_score then now() + interval '45 seconds' else null end
     where id = r.id
     returning * into r;
  end if;

  return r;
end;
$$;
grant execute on function public.submit_word(uuid, text) to authenticated;

-- ------------------------------------------------------------
-- Timeout claim: any participant may advance an actually expired turn
-- ------------------------------------------------------------
create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty int;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;
  if r.turn_deadline is null or r.turn_deadline >= now() then return r; end if;

  timed_out_player := r.current_player_id;
  penalty := case when r.status = 'final' then 2 else 1 end;

  if r.status = 'sudden_death' then
    update public.game_rooms
       set status = 'finished',
           winner_id = case when timed_out_player = host_id then guest_id else host_id end,
           finished_at = now(), turn_deadline = null,
           last_event = 'turn_expired', last_event_player_id = timed_out_player
     where id = r.id returning * into r;
    return r;
  end if;

  if timed_out_player = r.host_id then
    update public.game_rooms
       set host_score = host_score - penalty, host_streak = 0
     where id = r.id;
  else
    update public.game_rooms
       set guest_score = guest_score - penalty, guest_streak = 0
     where id = r.id;
  end if;

  update public.game_rooms
     set current_player_id = case when timed_out_player = host_id then guest_id else host_id end,
         turn_deadline = now() + interval '45 seconds',
         last_event = 'turn_expired', last_event_player_id = timed_out_player,
         final_moves_remaining = case when status = 'final' and final_moves_remaining > 0 then final_moves_remaining - 1 else final_moves_remaining end
   where id = r.id returning * into r;

  return r;
end;
$$;
grant execute on function public.claim_turn_timeout(uuid) to authenticated;

-- ------------------------------------------------------------
-- Trivia answering: 3-second read lock, first correct answer wins
-- ------------------------------------------------------------
create or replace function public.answer_trivia(p_round_id uuid, p_answer_index int)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  q public.trivia_rounds;
  room public.game_rooms;
  correct_idx int;
  is_correct boolean;
begin
  select * into q from public.trivia_rounds where id = p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;

  select * into room from public.game_rooms where id = q.room_id for update;
  if auth.uid() <> room.host_id and auth.uid() <> room.guest_id then raise exception 'not_participant'; end if;
  if q.resolved_at is not null then return room; end if;
  if now() < q.reveal_at then raise exception 'answers_locked'; end if;

  select correct_index into correct_idx from public.trivia_questions where id = q.question_id;
  is_correct := p_answer_index = correct_idx;

  insert into public.trivia_answers(round_id, player_id, answer_index, is_correct)
  values (q.id, auth.uid(), p_answer_index, is_correct)
  on conflict (round_id, player_id) do nothing;

  if is_correct and q.winner_id is null then
    update public.trivia_rounds
       set winner_id = auth.uid(), resolved_at = now()
     where id = q.id and winner_id is null
     returning * into q;

    if q.winner_id = auth.uid() then
      if auth.uid() = room.host_id then
        update public.game_rooms set host_score = host_score + q.bonus_points where id = room.id;
      else
        update public.game_rooms set guest_score = guest_score + q.bonus_points where id = room.id;
      end if;
    end if;
  end if;

  -- A correct answer ends the quiz immediately. Wrong answers leave it open for the opponent.
  select * into q from public.trivia_rounds where id = p_round_id;
  if q.winner_id is not null then
    if q.milestone >= 45 then
      update public.game_rooms
         set status = 'final', final_moves_remaining = 6,
             current_player_id = q.winner_id,
             turn_deadline = now() + interval '45 seconds',
             last_event = 'final_started'
       where id = room.id returning * into room;
    else
      update public.game_rooms
         set status = 'playing',
             current_player_id = q.winner_id,
             turn_deadline = now() + interval '45 seconds',
             last_event = 'quiz_won', last_event_player_id = q.winner_id
       where id = room.id returning * into room;
    end if;
  end if;

  return room;
end;
$$;
grant execute on function public.answer_trivia(uuid, int) to authenticated;

-- ------------------------------------------------------------
-- Block / photo privacy RPCs
-- ------------------------------------------------------------
create or replace function public.block_user(p_blocked_id uuid)
returns void language sql security definer set search_path = public as $$
  insert into public.user_blocks(blocker_id, blocked_id)
  values (auth.uid(), p_blocked_id)
  on conflict do nothing;
$$;
grant execute on function public.block_user(uuid) to authenticated;

create or replace function public.unblock_user(p_blocked_id uuid)
returns void language sql security definer set search_path = public as $$
  delete from public.user_blocks where blocker_id = auth.uid() and blocked_id = p_blocked_id;
$$;
grant execute on function public.unblock_user(uuid) to authenticated;

create or replace function public.set_photo_access(p_viewer_id uuid, p_allowed boolean)
returns void language plpgsql security definer set search_path = public as $$
begin
  if p_allowed then
    insert into public.profile_photo_access(owner_id, viewer_id)
    values (auth.uid(), p_viewer_id)
    on conflict do nothing;
  else
    delete from public.profile_photo_access where owner_id = auth.uid() and viewer_id = p_viewer_id;
  end if;
end;
$$;
grant execute on function public.set_photo_access(uuid, boolean) to authenticated;

-- Chat: blocked users cannot exchange messages.
drop policy if exists "chat participant insert" on public.chat_messages;
create policy "chat participant insert" on public.chat_messages
for insert to authenticated
with check (
  sender_id = auth.uid()
  and public.is_room_participant(room_id, auth.uid())
  and not exists (
    select 1
    from public.game_rooms r
    join public.user_blocks b on (
      (b.blocker_id = auth.uid() and b.blocked_id = case when r.host_id = auth.uid() then r.guest_id else r.host_id end)
      or
      (b.blocked_id = auth.uid() and b.blocker_id = case when r.host_id = auth.uid() then r.guest_id else r.host_id end)
    )
    where r.id = room_id
  )
);

create index if not exists dictionary_language_word_idx on public.dictionary_words(language, normalized_word);
create index if not exists blocks_blocked_idx on public.user_blocks(blocked_id);
create index if not exists trivia_rounds_room_idx on public.trivia_rounds(room_id, milestone);
