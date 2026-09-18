-- Son Harf Premier: three rounds, ten accepted words per player per round,
-- round clocks 15 -> 13 -> 11 seconds, and one-time opening clock activation.
-- Also removes the retired quiz hook and prevents normal adaptive bots from dispatching
-- into the expert-only function.

alter table public.game_rooms
  add column if not exists host_round_words integer not null default 0,
  add column if not exists guest_round_words integer not null default 0,
  add column if not exists opening_turn_started_at timestamptz;

do $constraints$
begin
  if not exists (
    select 1 from pg_constraint
    where conrelid='public.game_rooms'::regclass
      and conname='game_rooms_host_round_words_check'
  ) then
    alter table public.game_rooms
      add constraint game_rooms_host_round_words_check
      check (host_round_words between 0 and 10);
  end if;
  if not exists (
    select 1 from pg_constraint
    where conrelid='public.game_rooms'::regclass
      and conname='game_rooms_guest_round_words_check'
  ) then
    alter table public.game_rooms
      add constraint game_rooms_guest_round_words_check
      check (guest_round_words between 0 and 10);
  end if;
end
$constraints$;

-- Reconstruct counters only for live rooms created before this migration.
with live as (
  select r.id, r.host_id, r.guest_id, r.is_bot, greatest(coalesce(r.round_word_count,0),0) as n
  from public.game_rooms r
  where r.status in ('playing','final','sudden_death','paused')
),
counts as (
  select
    l.id,
    least(10, count(w.id) filter (
      where w.player_id=l.host_id and not coalesce(w.is_bot,false)
    ))::integer as host_words,
    least(10, count(w.id) filter (
      where (l.is_bot and coalesce(w.is_bot,false))
         or (not l.is_bot and w.player_id=l.guest_id)
    ))::integer as guest_words
  from live l
  left join lateral (
    select gw.id, gw.player_id, gw.is_bot
    from public.game_words gw
    where gw.room_id=l.id
    order by gw.id desc
    limit l.n
  ) w on true
  group by l.id
)
update public.game_rooms r
set host_round_words=c.host_words,
    guest_round_words=c.guest_words
from counts c
where r.id=c.id;

create or replace function public.sonharf_turn_seconds_for_round_v1(p_round_no integer)
returns integer
language sql
immutable
set search_path='pg_catalog'
as $function$
  select case
    when greatest(coalesce(p_round_no,1),1)=1 then 15
    when greatest(coalesce(p_round_no,1),1)=2 then 13
    else 11
  end
$function$;

create or replace function public.sonharf_turn_deadline_for_round_v1(p_round_no integer)
returns timestamptz
language sql
volatile
set search_path='pg_catalog','public'
as $function$
  select clock_timestamp() + make_interval(
    secs => public.sonharf_turn_seconds_for_round_v1(p_round_no)
  )
$function$;

revoke all on function public.sonharf_turn_seconds_for_round_v1(integer) from public, anon, authenticated;
revoke all on function public.sonharf_turn_deadline_for_round_v1(integer) from public, anon, authenticated;
grant execute on function public.sonharf_turn_seconds_for_round_v1(integer) to service_role;
grant execute on function public.sonharf_turn_deadline_for_round_v1(integer) to service_role;

create or replace function public.activate_premier_opening_turn_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  u uuid := auth.uid();
begin
  if u is null then raise exception 'unauthorized'; end if;

  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if u<>r.host_id and u is distinct from r.guest_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;

  -- Only the untouched opening turn can be reset, and only once. This prevents
  -- either client from extending an already-running competitive turn.
  if r.round_no=1
     and r.round_word_count=0
     and r.host_round_words=0
     and r.guest_round_words=0
     and not r.bot_turn
     and r.opening_turn_started_at is null then
    update public.game_rooms
    set opening_turn_started_at=clock_timestamp(),
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(1)
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

revoke all on function public.activate_premier_opening_turn_v1(uuid) from public, anon;
grant execute on function public.activate_premier_opening_turn_v1(uuid) to authenticated, service_role;

create or replace function public.submit_word_normal_v3(p_room_id uuid,p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  clean_word text;
  previous_word text;
  expected_first text;
  actual_first text;
  next_player uuid;
  streak_value integer;
  add_points integer:=3;
  round_winner uuid;
  next_round integer;
  next_starter uuid;
  dictionary_known boolean:=false;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.game_mode='expert' then raise exception 'expert_room_use_expert_submit'; end if;
  if r.status not in ('playing','sudden_death') then raise exception 'room_not_playing'; end if;

  if r.is_bot then
    if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
    if r.bot_turn or r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
  else
    if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
    if r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
  end if;

  if r.status<>'sudden_death' then
    if auth.uid()=r.host_id and r.host_round_words>=10 then raise exception 'round_word_quota_reached'; end if;
    if auth.uid()=r.guest_id and r.guest_round_words>=10 then raise exception 'round_word_quota_reached'; end if;
  end if;

  if r.turn_deadline is not null and r.turn_deadline<clock_timestamp() then
    return public.claim_turn_timeout(p_room_id);
  end if;

  clean_word:=public.normalize_game_word(r.language,p_word);
  if char_length(clean_word)<2 or char_length(clean_word)>40 then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;
  if (r.language='tr' and clean_word !~ '^[a-zçğıöşü]+$')
     or (r.language='en' and clean_word !~ '^[a-z]+$') then
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
      language,normalized_word,raw_word,first_seen_user_id,first_seen_room_id
    )
    values(r.language,clean_word,trim(p_word),auth.uid(),r.id)
    on conflict(language,normalized_word) do update
      set use_count=public.dictionary_review_queue.use_count+1,
          updated_at=clock_timestamp();
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id=p_room_id
  order by id desc
  limit 1;

  if previous_word is not null then
    expected_first:=public.premier_effective_required_v1(r.id,auth.uid(),right(previous_word,1));
    actual_first:=left(clean_word,1);
    if actual_first<>expected_first then
      return public.switch_turn_after_failure(p_room_id,'wrong_start_letter');
    end if;
  end if;

  if exists(
    select 1 from public.game_words
    where room_id=p_room_id and normalized_word=clean_word
  ) then
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
    if public.premier_multiplier_armed_v1(r.id,auth.uid()) then add_points:=add_points*2; end if;
    update public.game_rooms
    set host_score=host_score+add_points,
        host_round_score=host_round_score+add_points,
        host_streak=streak_value,
        host_round_words=least(10,host_round_words+1)
    where id=r.id;
  else
    streak_value:=r.guest_streak+1;
    if streak_value%5=0 then add_points:=6; end if;
    if public.premier_multiplier_armed_v1(r.id,auth.uid()) then add_points:=add_points*2; end if;
    update public.game_rooms
    set guest_score=guest_score+add_points,
        guest_round_score=guest_round_score+add_points,
        guest_streak=streak_value,
        guest_round_words=least(10,guest_round_words+1)
    where id=r.id;
  end if;

  update public.profiles
  set valid_words=valid_words+1,
      best_streak=greatest(best_streak,streak_value),
      word_storms=word_storms+case when streak_value%5=0 then 1 else 0 end
  where id=auth.uid();

  delete from public.premier_booster_state
  where room_id=r.id and user_id=auth.uid();

  update public.game_rooms
  set valid_word_count=valid_word_count+1,
      round_word_count=round_word_count+1,
      last_event=case
        when not dictionary_known then 'provisional_word'
        when streak_value%5=0 then 'streak_bonus'
        else 'valid_word'
      end,
      last_event_player_id=auth.uid()
  where id=r.id
  returning * into r;

  if r.host_round_words>=10 and r.guest_round_words>=10 then
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
          when (not is_bot and round_winner=guest_id)
            or (is_bot and guest_round_score>host_round_score) then 1
          else 0
        end
    where id=r.id
    returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(
          r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'match_finished'
        );
      else
        update public.game_rooms
        set status='sudden_death',
            round_word_count=0,
            host_round_words=0,
            guest_round_words=0,
            host_round_score=0,
            guest_round_score=0,
            current_player_id=host_id,
            bot_turn=false,
            turn_deadline=public.sonharf_turn_deadline_for_round_v1(3),
            last_event='sudden_death_started'
        where id=r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round:=r.round_no+1;
    if r.is_bot then
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=case when next_round%2=1 then host_id else null end,
          bot_turn=(next_round%2=0),
          turn_deadline=case
            when next_round%2=1 then public.sonharf_turn_deadline_for_round_v1(next_round)
            else null
          end,
          last_event='round_started'
      where id=r.id
      returning * into r;
    else
      next_starter:=case when next_round%2=1 then r.host_id else r.guest_id end;
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=next_starter,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(next_round),
          last_event='round_started'
      where id=r.id
      returning * into r;
    end if;
    return r;
  end if;

  if r.is_bot then
    if r.guest_round_words>=10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

create or replace function public.submit_word_expert_v1(p_room_id uuid,p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  clean_word text;
  previous_word text;
  expected text;
  next_player uuid;
  streak_value integer;
  multiplier integer;
  add_points integer;
  round_winner uuid;
  next_round integer;
  next_starter uuid;
  dictionary_known boolean:=false;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.game_mode<>'expert' then raise exception 'not_expert_room'; end if;
  if r.status not in ('playing','sudden_death') then raise exception 'room_not_playing'; end if;

  if r.is_bot then
    if auth.uid()<>r.host_id or r.bot_turn or r.current_player_id<>auth.uid() then
      raise exception 'not_your_turn';
    end if;
  else
    if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
    if r.current_player_id<>auth.uid() then raise exception 'not_your_turn'; end if;
  end if;

  if r.status<>'sudden_death' then
    if auth.uid()=r.host_id and r.host_round_words>=10 then raise exception 'round_word_quota_reached'; end if;
    if auth.uid()=r.guest_id and r.guest_round_words>=10 then raise exception 'round_word_quota_reached'; end if;
  end if;

  if r.turn_deadline is not null and r.turn_deadline<clock_timestamp() then
    return public.claim_turn_timeout(p_room_id);
  end if;

  clean_word:=public.normalize_game_word(r.language,p_word);
  if char_length(clean_word)<2 or char_length(clean_word)>40 then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;
  if (r.language='tr' and clean_word !~ '^[a-zçğıöşü]+$')
     or (r.language='en' and clean_word !~ '^[a-z]+$') then
    return public.switch_turn_after_failure(p_room_id,'invalid_word');
  end if;
  if not public.sonharf_word_allowed(r.language,clean_word) then
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
      language,normalized_word,raw_word,first_seen_user_id,first_seen_room_id
    )
    values(r.language,clean_word,trim(p_word),auth.uid(),r.id)
    on conflict(language,normalized_word) do update
      set use_count=public.dictionary_review_queue.use_count+1,
          updated_at=clock_timestamp();
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id=p_room_id
  order by id desc
  limit 1;

  if previous_word is not null then
    expected:=public.premier_effective_required_v1(
      r.id,auth.uid(),right(previous_word,least(3,greatest(1,r.round_no)))
    );
    if left(clean_word,char_length(expected))<>expected then
      return public.switch_turn_after_failure(p_room_id,'wrong_start_letter');
    end if;
  end if;

  if exists(
    select 1 from public.game_words
    where room_id=p_room_id and normalized_word=clean_word
  ) then
    return public.switch_turn_after_failure(p_room_id,'word_already_used');
  end if;

  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot)
  values(p_room_id,auth.uid(),trim(p_word),clean_word,false);

  if r.status='sudden_death' then
    update public.profiles set valid_words=valid_words+1 where id=auth.uid();
    return public.sonharf_finish_room(r.id,auth.uid(),false,'sudden_death_word');
  end if;

  multiplier:=least(3,greatest(1,r.round_no));
  streak_value:=case
    when auth.uid()=r.host_id then r.host_streak+1
    else r.guest_streak+1
  end;
  add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end;
  if public.premier_multiplier_armed_v1(r.id,auth.uid()) then add_points:=add_points*2; end if;

  if auth.uid()=r.host_id then
    update public.game_rooms
    set host_score=host_score+add_points,
        host_round_score=host_round_score+add_points,
        host_streak=streak_value,
        host_round_words=least(10,host_round_words+1)
    where id=r.id;
  else
    update public.game_rooms
    set guest_score=guest_score+add_points,
        guest_round_score=guest_round_score+add_points,
        guest_streak=streak_value,
        guest_round_words=least(10,guest_round_words+1)
    where id=r.id;
  end if;

  update public.profiles
  set valid_words=valid_words+1,
      best_streak=greatest(best_streak,streak_value),
      word_storms=word_storms+case when streak_value%5=0 then 1 else 0 end
  where id=auth.uid();

  delete from public.premier_booster_state
  where room_id=r.id and user_id=auth.uid();

  update public.game_rooms
  set valid_word_count=valid_word_count+1,
      round_word_count=round_word_count+1,
      last_event=case
        when multiplier=3 then 'expert_x3'
        when multiplier=2 then 'expert_x2'
        when not dictionary_known then 'provisional_word'
        when streak_value%5=0 then 'streak_bonus'
        else 'valid_word'
      end,
      last_event_player_id=auth.uid()
  where id=r.id
  returning * into r;

  if r.host_round_words>=10 and r.guest_round_words>=10 then
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
          when (not is_bot and round_winner=guest_id)
            or (is_bot and guest_round_score>host_round_score) then 1
          else 0
        end
    where id=r.id
    returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'expert_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(
          r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'expert_finished'
        );
      elsif r.host_score>r.guest_score then
        return public.sonharf_finish_room(r.id,r.host_id,false,'expert_score_tiebreak');
      elsif r.guest_score>r.host_score then
        return public.sonharf_finish_room(
          r.id,case when r.is_bot then null else r.guest_id end,r.is_bot,'expert_score_tiebreak'
        );
      else
        update public.game_rooms
        set status='sudden_death',
            round_word_count=0,
            host_round_words=0,
            guest_round_words=0,
            host_round_score=0,
            guest_round_score=0,
            current_player_id=host_id,
            bot_turn=false,
            turn_deadline=public.sonharf_turn_deadline_for_round_v1(3),
            last_event='sudden_death_started'
        where id=r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round:=r.round_no+1;
    if r.is_bot then
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=case when next_round%2=1 then host_id else null end,
          bot_turn=(next_round%2=0),
          turn_deadline=case
            when next_round%2=1 then public.sonharf_turn_deadline_for_round_v1(next_round)
            else null
          end,
          last_event='round_started'
      where id=r.id
      returning * into r;
    else
      next_starter:=case when next_round%2=1 then r.host_id else r.guest_id end;
      update public.game_rooms
      set round_no=next_round,
          round_word_count=0,
          host_round_words=0,
          guest_round_words=0,
          host_round_score=0,
          guest_round_score=0,
          host_streak=0,
          guest_streak=0,
          current_player_id=next_starter,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(next_round),
          last_event='round_started'
      where id=r.id
      returning * into r;
    end if;
    return r;
  end if;

  if r.is_bot then
    if r.guest_round_words>=10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

create or replace function public.bot_take_turn_normal_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  previous_word text;
  expected text;
  chosen public.dictionary_words;
  streak_value integer;
  add_points integer:=3;
  next_round integer;
  difficulty text:='normal';
  requested text:='normal';
  rating_value integer:=1000;
  total_value integer:=0;
  wins_value integer:=0;
  candidate_offset integer;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
  if r.game_mode='expert' then raise exception 'expert_bot_use_expert_turn'; end if;
  if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') or not r.bot_turn then return r; end if;

  if r.status<>'sudden_death' and r.guest_round_words>=10 then
    update public.game_rooms
    set current_player_id=host_id,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
    return r;
  end if;

  select coalesce(p.rating,1000),
         coalesce(p.total_matches,0),
         coalesce(p.wins,0),
         coalesce(p.bot_difficulty,'normal')
  into rating_value,total_value,wins_value,requested
  from public.profiles p
  where p.id=r.host_id;

  difficulty:=public.sonharf_adaptive_bot_tier_v1(
    rating_value,total_value,wins_value,requested
  );

  select normalized_word into previous_word
  from public.game_words
  where room_id=r.id
  order by id desc
  limit 1;
  expected:=case when previous_word is null then null else right(previous_word,1) end;
  candidate_offset:=floor(random()*192)::integer;

  with pool as materialized (
    select d.*
    from public.dictionary_words d
    where d.language=r.language
      and d.active
      and d.game_allowed
      and not d.is_abbreviation
      and not d.is_proper_noun
      and (expected is null or left(d.normalized_word,1)=expected)
      and public.sonharf_bot_word_allowed(d.language,d.normalized_word)
      and not exists(
        select 1 from public.game_words w
        where w.room_id=r.id and w.normalized_word=d.normalized_word
      )
    order by d.normalized_word
    offset candidate_offset limit 128
  )
  select p.* into chosen
  from pool p
  order by
    case when difficulty='expert' then abs(char_length(p.normalized_word)-7)
         else abs(char_length(p.normalized_word)-6) end,
    random()
  limit 1;

  if chosen.id is null then
    select d.* into chosen
    from public.dictionary_words d
    where d.language=r.language
      and d.active
      and d.game_allowed
      and not d.is_abbreviation
      and not d.is_proper_noun
      and (expected is null or left(d.normalized_word,1)=expected)
      and public.sonharf_bot_word_allowed(d.language,d.normalized_word)
      and not exists(
        select 1 from public.game_words w
        where w.room_id=r.id and w.normalized_word=d.normalized_word
      )
    order by d.normalized_word
    limit 1;
  end if;

  if chosen.id is null then
    return public.sonharf_finish_room(r.id,r.host_id,false,'bot_no_word');
  end if;

  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot)
  values(r.id,null,chosen.word,chosen.normalized_word,true);

  if r.status='sudden_death' then
    return public.sonharf_finish_room(r.id,null,true,'sudden_death_word');
  end if;

  streak_value:=r.guest_streak+1;
  if streak_value%5=0 then add_points:=6; end if;

  update public.game_rooms
  set guest_score=guest_score+add_points,
      guest_round_score=guest_round_score+add_points,
      guest_streak=streak_value,
      guest_round_words=least(10,guest_round_words+1),
      valid_word_count=valid_word_count+1,
      round_word_count=round_word_count+1,
      last_event=case when streak_value%5=0 then 'streak_bonus' else 'valid_word' end,
      last_event_player_id=null
  where id=r.id
  returning * into r;

  if r.host_round_words>=10 and r.guest_round_words>=10 then
    update public.profiles
    set total_rounds=total_rounds+1,
        rounds_won=rounds_won+case when r.host_round_score>r.guest_round_score then 1 else 0 end
    where id=r.host_id;

    update public.game_rooms
    set host_rounds=host_rounds+case when host_round_score>guest_round_score then 1 else 0 end,
        guest_rounds=guest_rounds+case when guest_round_score>host_round_score then 1 else 0 end
    where id=r.id
    returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'match_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(r.id,null,true,'match_finished');
      else
        update public.game_rooms
        set status='sudden_death',
            round_word_count=0,
            host_round_words=0,
            guest_round_words=0,
            host_round_score=0,
            guest_round_score=0,
            current_player_id=host_id,
            bot_turn=false,
            turn_deadline=public.sonharf_turn_deadline_for_round_v1(3),
            last_event='sudden_death_started'
        where id=r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round:=r.round_no+1;
    update public.game_rooms
    set round_no=next_round,
        round_word_count=0,
        host_round_words=0,
        guest_round_words=0,
        host_round_score=0,
        guest_round_score=0,
        host_streak=0,
        guest_streak=0,
        current_player_id=case when next_round%2=1 then host_id else null end,
        bot_turn=(next_round%2=0),
        turn_deadline=case
          when next_round%2=1 then public.sonharf_turn_deadline_for_round_v1(next_round)
          else null
        end,
        last_event='round_started'
    where id=r.id
    returning * into r;
    return r;
  end if;

  if r.host_round_words>=10 then
    update public.game_rooms
    set current_player_id=null,
        bot_turn=true,
        turn_deadline=null
    where id=r.id
    returning * into r;
  else
    update public.game_rooms
    set current_player_id=host_id,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

create or replace function public.bot_take_turn_expert_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  previous_word text;
  expected text;
  chosen public.dictionary_words;
  streak_value integer;
  multiplier integer;
  add_points integer;
  next_round integer;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or not r.is_bot or r.game_mode<>'expert' then
    raise exception 'not_expert_bot_room';
  end if;
  if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','sudden_death') or not r.bot_turn then return r; end if;

  if r.status<>'sudden_death' and r.guest_round_words>=10 then
    update public.game_rooms
    set current_player_id=host_id,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
    return r;
  end if;

  select normalized_word into previous_word
  from public.game_words
  where room_id=r.id
  order by id desc
  limit 1;
  expected:=case
    when previous_word is null then null
    else right(previous_word,least(3,greatest(1,r.round_no)))
  end;

  with candidates as (
    select d.id,
           d.normalized_word,
           right(d.normalized_word,least(3,greatest(1,r.round_no))) as next_prefix
    from public.dictionary_words d
    where d.language=r.language
      and d.active
      and public.sonharf_bot_word_allowed(d.language,d.normalized_word)
      and (expected is null or left(d.normalized_word,char_length(expected))=expected)
      and not exists(
        select 1 from public.game_words w
        where w.room_id=r.id and w.normalized_word=d.normalized_word
      )
    order by abs(char_length(d.normalized_word)-7),random()
    limit 48
  ),
  ranked as (
    select c.id,
      (
        select count(*)
        from public.dictionary_words reply
        where reply.language=r.language
          and reply.active
          and reply.id<>c.id
          and public.sonharf_bot_word_allowed(reply.language,reply.normalized_word)
          and left(reply.normalized_word,char_length(c.next_prefix))=c.next_prefix
          and not exists(
            select 1 from public.game_words used
            where used.room_id=r.id and used.normalized_word=reply.normalized_word
          )
      ) as reply_count
    from candidates c
  )
  select d.* into chosen
  from ranked x
  join public.dictionary_words d on d.id=x.id
  order by x.reply_count asc,abs(char_length(d.normalized_word)-7),random()
  limit 1;

  if chosen.id is null then
    return public.sonharf_finish_room(r.id,r.host_id,false,'bot_no_word');
  end if;

  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot)
  values(r.id,null,chosen.word,chosen.normalized_word,true);

  if r.status='sudden_death' then
    return public.sonharf_finish_room(r.id,null,true,'sudden_death_word');
  end if;

  multiplier:=least(3,greatest(1,r.round_no));
  streak_value:=r.guest_streak+1;
  add_points:=3*multiplier+case when streak_value%5=0 then 3 else 0 end;

  update public.game_rooms
  set guest_score=guest_score+add_points,
      guest_round_score=guest_round_score+add_points,
      guest_streak=streak_value,
      guest_round_words=least(10,guest_round_words+1),
      valid_word_count=valid_word_count+1,
      round_word_count=round_word_count+1,
      last_event=case
        when multiplier=3 then 'expert_x3'
        when multiplier=2 then 'expert_x2'
        when streak_value%5=0 then 'streak_bonus'
        else 'valid_word'
      end,
      last_event_player_id=null
  where id=r.id
  returning * into r;

  if r.host_round_words>=10 and r.guest_round_words>=10 then
    update public.profiles
    set total_rounds=total_rounds+1,
        rounds_won=rounds_won+case when r.host_round_score>r.guest_round_score then 1 else 0 end
    where id=r.host_id;

    update public.game_rooms
    set host_rounds=host_rounds+case when host_round_score>guest_round_score then 1 else 0 end,
        guest_rounds=guest_rounds+case when guest_round_score>host_round_score then 1 else 0 end
    where id=r.id
    returning * into r;

    if r.round_no>=3 then
      if r.host_rounds>r.guest_rounds then
        return public.sonharf_finish_room(r.id,r.host_id,false,'expert_finished');
      elsif r.guest_rounds>r.host_rounds then
        return public.sonharf_finish_room(r.id,null,true,'expert_finished');
      elsif r.host_score>r.guest_score then
        return public.sonharf_finish_room(r.id,r.host_id,false,'expert_score_tiebreak');
      elsif r.guest_score>r.host_score then
        return public.sonharf_finish_room(r.id,null,true,'expert_score_tiebreak');
      else
        update public.game_rooms
        set status='sudden_death',
            round_word_count=0,
            host_round_words=0,
            guest_round_words=0,
            host_round_score=0,
            guest_round_score=0,
            current_player_id=host_id,
            bot_turn=false,
            turn_deadline=public.sonharf_turn_deadline_for_round_v1(3),
            last_event='sudden_death_started'
        where id=r.id
        returning * into r;
        return r;
      end if;
    end if;

    next_round:=r.round_no+1;
    update public.game_rooms
    set round_no=next_round,
        round_word_count=0,
        host_round_words=0,
        guest_round_words=0,
        host_round_score=0,
        guest_round_score=0,
        host_streak=0,
        guest_streak=0,
        current_player_id=case when next_round%2=1 then host_id else null end,
        bot_turn=(next_round%2=0),
        turn_deadline=case
          when next_round%2=1 then public.sonharf_turn_deadline_for_round_v1(next_round)
          else null
        end,
        last_event='round_started'
    where id=r.id
    returning * into r;
    return r;
  end if;

  if r.host_round_words>=10 then
    update public.game_rooms
    set current_player_id=null,
        bot_turn=true,
        turn_deadline=null
    where id=r.id
    returning * into r;
  else
    update public.game_rooms
    set current_player_id=host_id,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

-- Adaptive difficulty must never route a normal-mode room into an expert-only
-- function. Expert mode stays explicit; normal mode adapts inside the normal bot.
create or replace function public.bot_take_turn(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
begin
  select * into r from public.game_rooms where id=p_room_id;
  if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
  if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;

  if r.game_mode='expert' then
    return public.bot_take_turn_expert_v1(p_room_id);
  end if;
  return public.bot_take_turn_normal_v1(p_room_id);
end
$function$;

create or replace function public.switch_turn_after_failure(p_room_id uuid,p_reason text)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  next_player uuid;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;

  if r.is_bot then
    if auth.uid()<>r.host_id or r.bot_turn then raise exception 'not_your_turn'; end if;
  else
    if auth.uid()<>r.current_player_id then raise exception 'not_your_turn'; end if;
  end if;

  if r.status='sudden_death' then
    return public.sonharf_finish_room(
      r.id,
      case when r.is_bot then null
           else case when auth.uid()=r.host_id then r.guest_id else r.host_id end end,
      r.is_bot and auth.uid()=r.host_id,
      p_reason
    );
  end if;

  if auth.uid()=r.host_id then
    update public.game_rooms
    set host_score=host_score-1,
        host_round_score=host_round_score-1,
        host_streak=0
    where id=r.id;
  else
    update public.game_rooms
    set guest_score=guest_score-1,
        guest_round_score=guest_round_score-1,
        guest_streak=0
    where id=r.id;
  end if;

  if r.is_bot then
    if r.guest_round_words>=10 and r.host_round_words<10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event=p_reason,
          last_event_player_id=auth.uid()
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null,
          last_event=p_reason,
          last_event_player_id=auth.uid()
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
        last_event=p_reason,
        last_event_player_id=auth.uid()
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty integer;
  next_player uuid;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;

  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid()<>r.host_id and (r.guest_id is null or auth.uid()<>r.guest_id) then
    raise exception 'not_participant';
  end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;
  if r.turn_deadline is null or r.turn_deadline>=clock_timestamp() then return r; end if;

  timed_out_player:=r.current_player_id;
  penalty:=case when r.status='final' then 2 else 1 end;

  if r.status='sudden_death' then
    update public.game_rooms
    set status='finished',
        winner_id=case
          when r.is_bot and timed_out_player=r.host_id then null
          when timed_out_player=r.host_id then r.guest_id
          else r.host_id
        end,
        winner_is_bot=(r.is_bot and timed_out_player=r.host_id),
        finished_at=clock_timestamp(),
        turn_deadline=null,
        bot_turn=false,
        last_event='turn_expired',
        last_event_player_id=timed_out_player
    where id=r.id
    returning * into r;
    return r;
  end if;

  if timed_out_player=r.host_id then
    update public.game_rooms
    set host_score=host_score-penalty,
        host_streak=0
    where id=r.id;
  elsif timed_out_player=r.guest_id then
    update public.game_rooms
    set guest_score=guest_score-penalty,
        guest_streak=0
    where id=r.id;
  end if;

  if r.is_bot and timed_out_player=r.host_id then
    if r.guest_round_words>=10 and r.host_round_words<10 then
      update public.game_rooms
      set current_player_id=host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
          last_event='turn_expired',
          last_event_player_id=timed_out_player,
          final_moves_remaining=case
            when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
            else final_moves_remaining
          end
      where id=r.id
      returning * into r;
    else
      update public.game_rooms
      set current_player_id=null,
          bot_turn=true,
          turn_deadline=null,
          last_event='turn_expired',
          last_event_player_id=timed_out_player,
          final_moves_remaining=case
            when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
            else final_moves_remaining
          end
      where id=r.id
      returning * into r;
    end if;
  else
    next_player:=case
      when timed_out_player=r.host_id then r.guest_id
      else r.host_id
    end;
    if next_player=r.host_id and r.host_round_words>=10 then next_player:=r.guest_id; end if;
    if next_player=r.guest_id and r.guest_round_words>=10 then next_player:=r.host_id; end if;
    update public.game_rooms
    set current_player_id=next_player,
        bot_turn=false,
        turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no),
        last_event='turn_expired',
        last_event_player_id=timed_out_player,
        final_moves_remaining=case
          when r.status='final' and final_moves_remaining>0 then final_moves_remaining-1
          else final_moves_remaining
        end
    where id=r.id
    returning * into r;
  end if;

  return r;
end
$function$;

create or replace function public.resume_premier_bot_match_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path='public','pg_temp'
as $function$
declare
  r public.game_rooms;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if not r.is_bot or auth.uid()<>r.host_id then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;

  if r.bot_turn then
    return public.bot_take_turn(p_room_id);
  end if;

  if r.status<>'sudden_death'
     and r.host_round_words>=10
     and r.guest_round_words<10 then
    update public.game_rooms
    set current_player_id=null,
        bot_turn=true,
        turn_deadline=null
    where id=r.id
    returning * into r;
    return public.bot_take_turn(p_room_id);
  end if;

  update public.game_rooms
  set current_player_id=r.host_id,
      bot_turn=false,
      turn_deadline=public.sonharf_turn_deadline_for_round_v1(r.round_no)
  where id=r.id
  returning * into r;
  return r;
end
$function$;

-- Private rematches reuse the same room row, so reset the new quota/opening fields.
do $patch_private_rematch$
declare
  f text;
begin
  f:=pg_get_functiondef('public.request_rematch_legacy_v1(uuid)'::regprocedure);
  if position('host_round_words=0' in f)=0 then
    if position('round_word_count=0,' in f)=0 then
      raise exception 'private_rematch_round_reset_hook_missing';
    end if;
    f:=replace(
      f,
      'round_word_count=0,',
      'round_word_count=0, host_round_words=0, guest_round_words=0, opening_turn_started_at=null,'
    );
    execute f;
  end if;
end
$patch_private_rematch$;

-- Repair currently live turns to the correct cap for their current round.
update public.game_rooms
set turn_deadline=case
      when bot_turn then null
      else public.sonharf_turn_deadline_for_round_v1(round_no)
    end,
    opening_turn_started_at=case
      when round_word_count>0 then coalesce(opening_turn_started_at,clock_timestamp())
      else opening_turn_started_at
    end
where status in ('playing','final','sudden_death');

revoke all on function public.submit_word_normal_v3(uuid,text) from public, anon, authenticated;
revoke all on function public.submit_word_expert_v1(uuid,text) from public, anon, authenticated;
revoke all on function public.bot_take_turn_normal_v1(uuid) from public, anon, authenticated;
revoke all on function public.bot_take_turn_expert_v1(uuid) from public, anon, authenticated;
grant execute on function public.submit_word_normal_v3(uuid,text) to service_role;
grant execute on function public.submit_word_expert_v1(uuid,text) to service_role;
grant execute on function public.bot_take_turn_normal_v1(uuid) to service_role;
grant execute on function public.bot_take_turn_expert_v1(uuid) to service_role;

revoke all on function public.bot_take_turn(uuid) from public, anon;
revoke all on function public.switch_turn_after_failure(uuid,text) from public, anon;
revoke all on function public.claim_turn_timeout(uuid) from public, anon;
revoke all on function public.resume_premier_bot_match_v1(uuid) from public, anon;
grant execute on function public.bot_take_turn(uuid) to authenticated, service_role;
grant execute on function public.switch_turn_after_failure(uuid,text) to authenticated, service_role;
grant execute on function public.claim_turn_timeout(uuid) to authenticated, service_role;
grant execute on function public.resume_premier_bot_match_v1(uuid) to authenticated, service_role;

select pg_notify('pgrst','reload schema');
