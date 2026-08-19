-- Son Harf final resilience layer: bot fallback + 60-second reconnect window.
-- Keeps authoritative scoring/rules on the server.

do $$
declare c record;
begin
  for c in
    select conname from pg_constraint
    where conrelid='public.game_rooms'::regclass and contype='c'
      and pg_get_constraintdef(oid) ilike '%status%'
  loop execute format('alter table public.game_rooms drop constraint %I',c.conname); end loop;
end $$;

alter table public.game_rooms
  add column if not exists is_bot boolean not null default false,
  add column if not exists bot_name text,
  add column if not exists host_last_seen timestamptz not null default now(),
  add column if not exists guest_last_seen timestamptz not null default now(),
  add column if not exists disconnected_player_id uuid references public.profiles(id) on delete set null,
  add column if not exists reconnect_deadline timestamptz,
  add column if not exists status_before_pause text,
  add column if not exists bot_turn boolean not null default false;

alter table public.game_rooms add constraint game_rooms_status_final_check
check(status in ('waiting','playing','quiz','final','sudden_death','paused','finished','cancelled'));

alter table public.game_words alter column player_id drop not null;
alter table public.game_words add column if not exists is_bot boolean not null default false;
alter table public.trivia_rounds add column if not exists bot_attempted boolean not null default false;

create or replace function public.prevent_room_trivia_repeat()
returns trigger language plpgsql set search_path=public as $$
declare lang text; replacement bigint;
begin
  if exists(select 1 from public.trivia_rounds where room_id=new.room_id and question_id=new.question_id) then
    select language into lang from public.game_rooms where id=new.room_id;
    select q.id into replacement from public.trivia_questions q
    where q.language=lang and q.active
      and not exists(select 1 from public.trivia_rounds tr where tr.room_id=new.room_id and tr.question_id=q.id)
    order by random() limit 1;
    if replacement is not null then new.question_id:=replacement; end if;
  end if;
  return new;
end $$;
drop trigger if exists trivia_no_repeat_in_room on public.trivia_rounds;
create trigger trivia_no_repeat_in_room before insert on public.trivia_rounds
for each row execute function public.prevent_room_trivia_repeat();

-- Prefer real people; after 10 seconds fall back to a clearly-labelled bot.
create or replace function public.poll_random_matchmaking()
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare q public.matchmaking_queue; r public.game_rooms; generated_code text; attempts int:=0;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into q from public.matchmaking_queue where user_id=auth.uid() for update;
  if q.user_id is null then return null; end if;
  if q.status='matched' and q.room_id is not null then
    select * into r from public.game_rooms where id=q.room_id; return r;
  end if;
  if q.status='waiting' then
    update public.matchmaking_queue set heartbeat_at=now() where user_id=auth.uid();
    if q.queued_at<=now()-interval '10 seconds' then
      loop
        attempts:=attempts+1; generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
        begin
          insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,host_last_seen,guest_last_seen,bot_turn)
          values(generated_code,auth.uid(),null,'playing',auth.uid(),now()+interval '45 seconds',q.language,true,
            case when q.language='tr' then 'KelimeBot' else 'WordBot' end,now(),now(),false)
          returning * into r; exit;
        exception when unique_violation then if attempts>=8 then raise; end if; end;
      end loop;
      update public.matchmaking_queue set status='matched',room_id=r.id,heartbeat_at=now() where user_id=auth.uid();
      update public.profiles set presence_status='in_game',last_seen_at=now() where id=auth.uid();
      return r;
    end if;
  end if;
  return null;
end $$;
grant execute on function public.poll_random_matchmaking() to authenticated;

create or replace function public.bot_take_turn(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare
  r public.game_rooms; clean_word text; previous_word text; expected_first text;
  streak_value int; add_points int:=3; next_valid_count int; h_delta int; g_delta int;
  milestone int; bonus int; qid bigint;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if not r.is_bot or r.host_id<>auth.uid() then raise exception 'not_bot_match'; end if;
  if r.status not in ('playing','final','sudden_death') or not r.bot_turn then return r; end if;

  select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
  expected_first:=case when previous_word is null then null else right(previous_word,1) end;
  select d.normalized_word into clean_word from public.dictionary_words d
  where d.language=r.language and d.active and (expected_first is null or left(d.normalized_word,1)=expected_first)
    and not exists(select 1 from public.game_words w where w.room_id=r.id and w.normalized_word=d.normalized_word)
  order by random() limit 1;

  if clean_word is null then
    if r.status='sudden_death' then
      update public.game_rooms set status='finished',winner_id=host_id,finished_at=now(),turn_deadline=null,bot_turn=false,last_event='bot_failed'
      where id=r.id returning * into r; return r;
    end if;
    update public.game_rooms set guest_score=guest_score-1,guest_streak=0,current_player_id=host_id,bot_turn=false,
      turn_deadline=now()+interval '45 seconds',last_event='bot_failed' where id=r.id returning * into r; return r;
  end if;

  insert into public.game_words(room_id,player_id,word,normalized_word,is_bot) values(r.id,null,clean_word,clean_word,true);
  streak_value:=r.guest_streak+1; if streak_value%5=0 then add_points:=6; end if;
  next_valid_count:=r.valid_word_count+1;
  update public.game_rooms set guest_score=guest_score+add_points,guest_streak=streak_value,valid_word_count=next_valid_count,
    round_word_count=next_valid_count%10,current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',
    last_event=case when streak_value%5=0 then 'bot_streak_bonus' else 'bot_valid_word' end
  where id=r.id returning * into r;

  if next_valid_count in (10,20,30) then
    h_delta:=r.host_score-r.host_round_start_score; g_delta:=r.guest_score-r.guest_round_start_score;
    update public.game_rooms set
      host_rounds=host_rounds+case when h_delta>g_delta then 1 else 0 end,
      guest_rounds=guest_rounds+case when g_delta>h_delta then 1 else 0 end,
      host_round_start_score=host_score,guest_round_start_score=guest_score,round_word_count=0,
      round_no=case when valid_word_count<30 then round_no+1 else 3 end
    where id=r.id returning * into r;
  end if;

  if next_valid_count%15=0 and r.status='playing' then
    milestone:=next_valid_count; bonus:=(6*power(2,(milestone/15)-1))::int;
    select q.id into qid from public.trivia_questions q
    where q.language=r.language and q.active
      and not exists(select 1 from public.trivia_rounds tr where tr.room_id=r.id and tr.question_id=q.id)
    order by random() limit 1;
    if qid is not null then
      insert into public.trivia_rounds(room_id,milestone,bonus_points,question_id,reveal_at)
      values(r.id,milestone,bonus,qid,now()+interval '3 seconds') on conflict(room_id,milestone) do nothing;
      update public.game_rooms set status='quiz',turn_deadline=null,last_event='quiz_started' where id=r.id returning * into r;
      return r;
    end if;
  end if;

  if next_valid_count>=30 and r.status='playing' then
    update public.game_rooms set
      status=case when host_score=guest_score then 'sudden_death' else 'finished' end,
      winner_id=case when host_score>guest_score then host_id else null end,
      finished_at=case when host_score=guest_score then null else now() end,
      bot_turn=false,current_player_id=case when host_score=guest_score then host_id else null end,
      turn_deadline=case when host_score=guest_score then now()+interval '45 seconds' else null end,
      last_event=case when host_score=guest_score then 'sudden_death_started' else 'match_finished' end
    where id=r.id returning * into r;
  end if;
  return r;
end $$;
grant execute on function public.bot_take_turn(uuid) to authenticated;

create or replace function public.finish_bot_quiz_without_bonus(p_room_id uuid,p_round_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; tr public.trivia_rounds;
begin
  select * into tr from public.trivia_rounds where id=p_round_id for update;
  update public.trivia_rounds set resolved_at=now() where id=tr.id and resolved_at is null;
  select * into r from public.game_rooms where id=p_room_id for update;
  if tr.milestone>=30 then
    update public.game_rooms set status=case when host_score=guest_score then 'sudden_death' else 'finished' end,
      winner_id=case when host_score>guest_score then host_id else null end,
      finished_at=case when host_score=guest_score then null else now() end,
      current_player_id=case when host_score=guest_score then host_id else null end,bot_turn=false,
      turn_deadline=case when host_score=guest_score then now()+interval '45 seconds' else null end,last_event='quiz_no_winner'
    where id=r.id returning * into r;
  else
    update public.game_rooms set status='playing',current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',last_event='quiz_no_winner'
    where id=r.id returning * into r;
  end if;
  return r;
end $$;

create or replace function public.bot_answer_trivia(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; tr public.trivia_rounds; bot_correct boolean; human_wrong boolean;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or not r.is_bot or r.host_id<>auth.uid() then raise exception 'not_bot_match'; end if;
  if r.status<>'quiz' then return r; end if;
  select * into tr from public.trivia_rounds where room_id=r.id and resolved_at is null order by milestone desc limit 1 for update;
  if tr.id is null or now()<tr.reveal_at or tr.bot_attempted then return r; end if;
  update public.trivia_rounds set bot_attempted=true where id=tr.id;
  bot_correct:=random()<0.62;
  if not bot_correct then
    select exists(select 1 from public.trivia_answers where round_id=tr.id and player_id=r.host_id and not is_correct) into human_wrong;
    if human_wrong then return public.finish_bot_quiz_without_bonus(r.id,tr.id); end if;
    return r;
  end if;

  update public.trivia_rounds set winner_id=null,resolved_at=now() where id=tr.id;
  update public.game_rooms set guest_score=guest_score+tr.bonus_points where id=r.id returning * into r;
  if tr.milestone>=30 then
    update public.game_rooms set status=case when host_score=guest_score then 'sudden_death' else 'finished' end,
      winner_id=case when host_score>guest_score then host_id else null end,
      finished_at=case when host_score=guest_score then null else now() end,
      current_player_id=case when host_score=guest_score then host_id else null end,bot_turn=false,
      turn_deadline=case when host_score=guest_score then now()+interval '45 seconds' else null end,
      last_event=case when host_score=guest_score then 'sudden_death_started' else 'bot_quiz_won' end
    where id=r.id returning * into r;
  else
    update public.game_rooms set status='playing',current_player_id=host_id,bot_turn=false,turn_deadline=now()+interval '45 seconds',last_event='bot_quiz_won'
    where id=r.id returning * into r;
  end if;
  return r;
end $$;
grant execute on function public.bot_answer_trivia(uuid) to authenticated;

create or replace function public.answer_trivia_v3(p_round_id uuid,p_answer_index int)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; tr public.trivia_rounds; human_wrong boolean;
begin
  r:=public.answer_trivia_v2(p_round_id,p_answer_index);
  if not r.is_bot or r.status<>'quiz' then return r; end if;
  select * into tr from public.trivia_rounds where id=p_round_id;
  if tr.resolved_at is not null then return r; end if;
  select exists(select 1 from public.trivia_answers where round_id=tr.id and player_id=r.host_id and not is_correct) into human_wrong;
  if tr.bot_attempted and human_wrong then return public.finish_bot_quiz_without_bonus(r.id,tr.id); end if;
  return r;
end $$;
grant execute on function public.answer_trivia_v3(uuid,int) to authenticated;

create or replace function public.submit_word_v3(p_room_id uuid,p_word text)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare before_room public.game_rooms; r public.game_rooms;
begin
  select * into before_room from public.game_rooms where id=p_room_id;
  r:=public.submit_word_v2(p_room_id,p_word);
  if r.is_bot and r.valid_word_count>before_room.valid_word_count and r.status in ('playing','final','sudden_death') then
    update public.game_rooms set current_player_id=null,bot_turn=true,turn_deadline=null where id=r.id returning * into r;
  end if;
  return r;
end $$;
grant execute on function public.submit_word_v3(uuid,text) to authenticated;

create or replace function public.heartbeat_room(p_room_id uuid)
returns public.game_rooms language plpgsql security definer set search_path=public as $$
declare r public.game_rooms; me uuid:=auth.uid(); opponent uuid; opponent_seen timestamptz;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if me<>r.host_id and (r.guest_id is null or me<>r.guest_id) then raise exception 'not_participant'; end if;
  if r.is_bot then return r; end if;

  if me=r.host_id then
    update public.game_rooms set host_last_seen=now() where id=r.id; opponent:=r.guest_id; opponent_seen:=r.guest_last_seen;
  else
    update public.game_rooms set guest_last_seen=now() where id=r.id; opponent:=r.host_id; opponent_seen:=r.host_last_seen;
  end if;
  select * into r from public.game_rooms where id=p_room_id;

  if r.status='paused' then
    if r.disconnected_player_id=me and r.reconnect_deadline>=now() then
      update public.game_rooms set
        status=coalesce(status_before_pause,'playing'),disconnected_player_id=null,reconnect_deadline=null,
        turn_deadline=case when coalesce(status_before_pause,'playing')='quiz' then null else now()+interval '45 seconds' end,
        status_before_pause=null,last_event='player_reconnected'
      where id=r.id returning * into r; return r;
    elsif r.reconnect_deadline<now() then
      update public.game_rooms set status='finished',winner_id=case when disconnected_player_id=host_id then guest_id else host_id end,
        finished_at=now(),turn_deadline=null,last_event='disconnect_forfeit'
      where id=r.id returning * into r; return r;
    end if;
    return r;
  end if;

  if r.status in ('playing','quiz','final','sudden_death') and opponent is not null and opponent_seen<now()-interval '15 seconds' then
    update public.game_rooms set status_before_pause=status,status='paused',disconnected_player_id=opponent,
      reconnect_deadline=now()+interval '60 seconds',turn_deadline=null,last_event='opponent_disconnected'
    where id=r.id returning * into r;
  end if;
  return r;
end $$;
grant execute on function public.heartbeat_room(uuid) to authenticated;

create index if not exists game_rooms_reconnect_idx on public.game_rooms(status,reconnect_deadline);
