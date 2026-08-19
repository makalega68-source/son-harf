-- Son Harf playable two-player MVP

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, 'Oyuncu-' || upper(substr(replace(new.id::text, '-', ''), 1, 4)))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

create or replace function public.create_room()
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

  loop
    attempts := attempts + 1;
    generated_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
    begin
      insert into public.game_rooms(code, host_id, status, current_player_id, turn_deadline)
      values (generated_code, auth.uid(), 'waiting', auth.uid(), now() + interval '45 seconds')
      returning * into r;
      exit;
    exception when unique_violation then
      if attempts >= 8 then raise; end if;
    end;
  end loop;

  return r;
end;
$$;

grant execute on function public.create_room() to authenticated;

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
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;

  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status <> 'playing' then raise exception 'room_not_playing'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  if r.current_player_id <> auth.uid() then raise exception 'not_your_turn'; end if;
  if r.turn_deadline is not null and r.turn_deadline < now() then raise exception 'turn_expired'; end if;

  clean_word := lower(trim(p_word));
  if char_length(clean_word) < 2 or char_length(clean_word) > 40 then raise exception 'invalid_word_length'; end if;
  if clean_word !~ '^[a-zçğıöşü]+$' then raise exception 'invalid_characters'; end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id = p_room_id
  order by id desc
  limit 1;

  if previous_word is not null then
    expected_first := right(previous_word, 1);
    actual_first := left(clean_word, 1);
    if actual_first <> expected_first then raise exception 'wrong_start_letter'; end if;
  end if;

  if exists(select 1 from public.game_words where room_id = p_room_id and normalized_word = clean_word) then
    raise exception 'word_already_used';
  end if;

  insert into public.game_words(room_id, player_id, word, normalized_word)
  values (p_room_id, auth.uid(), trim(p_word), clean_word);

  next_player := case when auth.uid() = r.host_id then r.guest_id else r.host_id end;
  update public.game_rooms
  set current_player_id = next_player,
      turn_deadline = now() + interval '45 seconds'
  where id = p_room_id
  returning * into r;

  return r;
end;
$$;

grant execute on function public.submit_word(uuid, text) to authenticated;

create or replace function public.forfeit_room(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  opponent uuid;
begin
  select * into r from public.game_rooms where id = p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() <> r.host_id and auth.uid() <> r.guest_id then raise exception 'not_participant'; end if;
  opponent := case when auth.uid() = r.host_id then r.guest_id else r.host_id end;
  update public.game_rooms
  set status = 'finished', winner_id = opponent, finished_at = now(), turn_deadline = null
  where id = p_room_id
  returning * into r;
  return r;
end;
$$;

grant execute on function public.forfeit_room(uuid) to authenticated;

drop policy if exists "rooms participant update profile-safe" on public.game_rooms;
create policy "rooms participant update profile-safe" on public.game_rooms
for update to authenticated
using (host_id = auth.uid() or guest_id = auth.uid())
with check (host_id = auth.uid() or guest_id = auth.uid());

drop policy if exists "chat participant insert" on public.chat_messages;
create policy "chat participant insert" on public.chat_messages
for insert to authenticated
with check (
  sender_id = auth.uid()
  and public.is_room_participant(room_id, auth.uid())
);

-- Prevent duplicate policy errors if the original permissive policy exists.
drop policy if exists "chat self insert" on public.chat_messages;

create index if not exists game_rooms_code_status_idx on public.game_rooms(code, status);
