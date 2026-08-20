-- Gameplay recovery: keep strict structure/turn/last-letter/duplicate checks,
-- but do not let the undersized seed dictionary block ordinary words.

create table if not exists public.dictionary_review_queue (
  id bigserial primary key,
  language text not null,
  normalized_word text not null,
  raw_word text not null,
  first_seen_user_id uuid references public.profiles(id) on delete set null,
  first_seen_room_id uuid references public.game_rooms(id) on delete set null,
  use_count integer not null default 1,
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(language, normalized_word)
);

alter table public.dictionary_review_queue enable row level security;
revoke all on public.dictionary_review_queue from anon, authenticated;

create or replace function public.submit_word_v3(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  r public.game_rooms;
  clean_word text;
  previous_word text;
  expected_first text;
  actual_first text;
  next_player uuid;
  streak_value int;
  add_points int:=3;
  round_winner uuid;
  next_round int;
  next_starter uuid;
  qid bigint;
  dictionary_known boolean:=false;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status not in ('playing','sudden_death') then raise exception 'room_not_playing'; end if;

  if r.is_bot then
    if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
    if r.bot_turn or r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
  else
    if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
    if r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
  end if;

  if r.turn_deadline is not null and r.turn_deadline < now() then
    return public.claim_turn_timeout(p_room_id);
  end if;

  clean_word:=public.normalize_game_word(r.language,p_word);
  if char_length(clean_word)<2 or char_length(clean_word)>40 then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;
  if (r.language='tr' and clean_word !~ '^[a-zçğıöşü]+$') or
     (r.language='en' and clean_word !~ '^[a-z]+$') then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;

  select exists(
    select 1 from public.dictionary_words d
    where d.language=r.language and d.normalized_word=clean_word and d.active
  ) into dictionary_known;

  if not dictionary_known then
    if char_length(clean_word)<3 then
      return public.switch_turn_after_failure(p_room_id,'not_in_dictionary');
    end if;
    insert into public.dictionary_review_queue(
      language, normalized_word, raw_word, first_seen_user_id, first_seen_room_id
    ) values (
      r.language, clean_word, trim(p_word), auth.uid(), r.id
    )
    on conflict(language,normalized_word) do update
      set use_count=public.dictionary_review_queue.use_count+1,
          updated_at=now();
  end if;

  select normalized_word into previous_word
  from public.game_words where room_id=p_room_id order by id desc limit 1;

  if previous_word is not null then
    expected_first:=right(previous_word,1);
    actual_first:=left(clean_word,1);
    if actual_first<>expected_first then
      return public.switch_turn_after_failure(p_room_id,'wrong_start_letter');
    end if;
  end if;

  if exists(select 1 from public.game_words where room_id=p_room_id and normalized_word=clean_word) then
    return public.switch_turn_after_failure(p_room_id,'word_already_used');
  end if;

  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot)
  values(p_room_id,auth.uid(),trim(p_word),clean_word,false);

  if r.status='sudden_death' then
    update public.profiles set valid_words=valid_words+1 where id=auth.uid();
    return public.sonharf_finish_room(r.id,auth.uid(),false,'sudden_death_word');
  end if;

  if auth.uid()=r.host_id then
    streak_value:=r.host_streak+1;
    if streak_value%5=0 then add_points:=6; end if;
    update public.game_rooms
    set host_score=host_score+add_points,
        host_round_score=host_round_score+add_points,
        host_streak=streak_value
    where id=r.id;
  else
    streak_value:=r.guest_streak+1;
    if streak_value%5=0 then add_points:=6; end if;
    update public.game_rooms
    set guest_score=guest_score+add_points,
        guest_round_score=guest_round_score+add_points,
        guest_streak=streak_value
    where id=r.id;
  end if;

  update public.profiles
  set valid_words=valid_words+1,
      best_streak=greatest(best_streak,streak_value),
      word_storms=word_storms+case when streak_value%5=0 then 1 else 0 end
  where id=auth.uid();

  next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
  update public.game_rooms
  set valid_word_count=valid_word_count+1,
      round_word_count=round_word_count+1,
      current_player_id=case when r.is_bot and auth.uid()=r.host_id then null else next_player end,
      bot_turn=case when r.is_bot and auth.uid()=r.host_id then true else false end,
      turn_deadline=case when r.is_bot and auth.uid()=r.host_id then null else now()+interval '45 seconds' end,
      last_event=case when not dictionary_known then 'provisional_word' when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,
      last_event_player_id=auth.uid()
  where id=r.id returning * into r;

  if r.round_word_count=5 then
    select id into qid from public.trivia_questions
    where language=r.language and active order by random() limit 1;
    if qid is not null then
      insert into public.trivia_rounds(room_id,milestone,bonus_points,question_id,reveal_at)
      values(r.id,(r.round_no-1)*10+5,3,qid,now()+interval '3 seconds')
      on conflict(room_id,milestone) do nothing;
      update public.game_rooms
      set status='quiz',turn_deadline=null,last_event='quiz_started'
      where id=r.id returning * into r;
      return r;
    end if;
  end if;

  if r.round_word_count>=10 then
    round_winner:=case
      when r.host_round_score>r.guest_round_score then r.host_id
      when r.guest_round_score>r.host_round_score then r.guest_id
      else null
    end;

    update public.profiles
    set total_rounds=total_rounds+1,
        rounds_won=rounds_won+case when id=round_winner then 1 else 0 end
    where id=r.host_id or (not r.is_bot and id=r.guest_id);

    update public.game_rooms
    set host_rounds=host_rounds+case when round_winner=host_id then 1 else 0 end,
        guest_rounds=guest_rounds+case
          when (not is_bot and round_winner=guest_id) or
               (is_bot and guest_round_score>host_round_score) then 1 else 0 end
    where id=r.id returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'match_finished');
      else
        update public.game_rooms
        set status='sudden_death',round_word_count=0,host_round_score=0,guest_round_score=0,
            current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',
            last_event='sudden_death_started'
        where id=r.id returning * into r;
        return r;
      end if;
    else
      next_round:=r.round_no+1;
      if r.is_bot then
        update public.game_rooms
        set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,
            host_streak=0,guest_streak=0,
            current_player_id=case when next_round%2=1 then host_id else null end,
            bot_turn=(next_round%2=0),
            turn_deadline=case when next_round%2=1 then now()+interval '45 seconds' else null end,
            last_event='round_started'
        where id=r.id returning * into r;
      else
        next_starter:=case when next_round%2=1 then r.host_id else r.guest_id end;
        update public.game_rooms
        set round_no=next_round,round_word_count=0,host_round_score=0,guest_round_score=0,
            host_streak=0,guest_streak=0,current_player_id=next_starter,
            turn_deadline=now()+interval '45 seconds',last_event='round_started'
        where id=r.id returning * into r;
      end if;
    end if;
  end if;

  return r;
end;
$$;

revoke all on function public.submit_word_v3(uuid,text) from public, anon;
grant execute on function public.submit_word_v3(uuid,text) to authenticated;
revoke all on function public.claim_turn_timeout(uuid) from public, anon;
grant execute on function public.claim_turn_timeout(uuid) to authenticated;
revoke all on function public.bot_take_turn(uuid) from public, anon;
grant execute on function public.bot_take_turn(uuid) to authenticated;
select pg_notify('pgrst','reload schema');
