-- Son Harf final 3-round gameplay wrapper.
-- 10 valid words per round, 30 valid words per match.

alter table public.game_rooms
  add column if not exists host_round_start_score integer not null default 0,
  add column if not exists guest_round_start_score integer not null default 0;

create or replace function public.submit_word_v2(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  before_room public.game_rooms;
  r public.game_rooms;
  h_delta integer;
  g_delta integer;
begin
  select * into before_room from public.game_rooms where id = p_room_id;
  if before_room.id is null then raise exception 'room_not_found'; end if;

  r := public.submit_word(p_room_id, p_word);

  -- Count rounds only when a valid word was actually accepted.
  if r.valid_word_count > before_room.valid_word_count then
    update public.game_rooms
       set round_word_count = valid_word_count % 10
     where id = r.id
     returning * into r;

    if r.valid_word_count in (10,20,30) then
      h_delta := r.host_score - r.host_round_start_score;
      g_delta := r.guest_score - r.guest_round_start_score;

      update public.game_rooms
         set host_rounds = host_rounds + case when h_delta > g_delta then 1 else 0 end,
             guest_rounds = guest_rounds + case when g_delta > h_delta then 1 else 0 end,
             host_round_start_score = host_score,
             guest_round_start_score = guest_score,
             round_word_count = 0,
             round_no = case when valid_word_count < 30 then round_no + 1 else 3 end
       where id = r.id
       returning * into r;
    end if;
  end if;

  -- If milestone 30 did not open a quiz because the pool is empty, end immediately.
  if r.valid_word_count >= 30 and r.status = 'playing' then
    update public.game_rooms
       set status = case when host_score = guest_score then 'sudden_death' else 'finished' end,
           winner_id = case when host_score > guest_score then host_id when guest_score > host_score then guest_id else null end,
           finished_at = case when host_score = guest_score then null else now() end,
           turn_deadline = case when host_score = guest_score then now() + interval '45 seconds' else null end,
           last_event = case when host_score = guest_score then 'sudden_death_started' else 'match_finished' end
     where id = r.id
     returning * into r;
  end if;

  return r;
end;
$$;

grant execute on function public.submit_word_v2(uuid,text) to authenticated;

create or replace function public.answer_trivia_v2(p_round_id uuid, p_answer_index int)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
  tr public.trivia_rounds;
begin
  r := public.answer_trivia(p_round_id, p_answer_index);
  select * into tr from public.trivia_rounds where id = p_round_id;

  if tr.milestone >= 30 and tr.resolved_at is not null then
    update public.game_rooms
       set status = case when host_score = guest_score then 'sudden_death' else 'finished' end,
           winner_id = case when host_score > guest_score then host_id when guest_score > host_score then guest_id else null end,
           finished_at = case when host_score = guest_score then null else now() end,
           turn_deadline = case when host_score = guest_score then now() + interval '45 seconds' else null end,
           last_event = case when host_score = guest_score then 'sudden_death_started' else 'match_finished' end
     where id = r.id
     returning * into r;
  end if;
  return r;
end;
$$;

grant execute on function public.answer_trivia_v2(uuid,int) to authenticated;
