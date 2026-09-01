-- Bil Bakalim numeric estimate v2
-- One numeric estimate per player, 10 second deadline, nearest answer wins +10,
-- ties / double no-answer award zero. Correct/opponent answers remain hidden until resolution.

create or replace function public.start_bilbakalim_round_v1(p_room_id uuid, p_milestone integer)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  r public.game_rooms;
  qid bigint;
  correct_value bigint;
  bot_est bigint;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status<>'playing' then return r; end if;
  if not public.sonharf_config_enabled('trivia_enabled',true) then return r; end if;
  if exists(select 1 from public.trivia_rounds where room_id=r.id and milestone=p_milestone) then return r; end if;

  select q.id,a.correct_value
    into qid,correct_value
  from public.trivia_questions q
  join public.bilbakalim_answers a on a.question_id=q.id
  where q.language=r.language
    and q.active
    and q.question_kind='bil_bakalim'
  order by random()
  limit 1;

  if qid is null then return r; end if;
  if r.is_bot then
    bot_est:=greatest(0,round(correct_value*(1+((random()*2-1)*(0.06+random()*0.20))))::bigint);
  end if;

  insert into public.trivia_rounds(
    room_id,milestone,bonus_points,question_id,reveal_at,answer_deadline,
    exact_bonus,bot_answer,resume_status,resume_current_player_id,resume_bot_turn
  ) values(
    r.id,p_milestone,10,qid,clock_timestamp(),clock_timestamp()+interval '10 seconds',
    0,bot_est,'playing',r.current_player_id,r.bot_turn
  );

  update public.game_rooms
  set status='quiz',current_player_id=null,bot_turn=false,turn_deadline=null,
      last_event='bilbakalim_started',last_event_player_id=null
  where id=r.id returning * into r;
  return r;
end
$$;

-- Legacy normal/bot paths used to insert world_estimate rounds directly.
-- Normalize those inserts into the same Bil Bakalim numeric-estimate contract
-- without rewriting the multiplayer/word engines.
create or replace function private.normalize_bilbakalim_round_v2()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
declare
  v_kind text;
  v_language text;
  v_qid bigint;
  v_correct bigint;
  v_is_bot boolean;
begin
  select q.question_kind into v_kind from public.trivia_questions q where q.id=new.question_id;
  if v_kind not in ('world_estimate','bil_bakalim') then return new; end if;

  select g.language,coalesce(g.is_bot,false) into v_language,v_is_bot
  from public.game_rooms g where g.id=new.room_id;

  if v_kind<>'bil_bakalim' then
    select q.id,a.correct_value into v_qid,v_correct
    from public.trivia_questions q
    join public.bilbakalim_answers a on a.question_id=q.id
    where q.language=v_language and q.active and q.question_kind='bil_bakalim'
    order by random() limit 1;
    if v_qid is null then raise exception 'bilbakalim_question_unavailable'; end if;
    new.question_id:=v_qid;
  else
    select a.correct_value into v_correct from public.bilbakalim_answers a where a.question_id=new.question_id;
  end if;

  new.bonus_points:=10;
  new.reveal_at:=clock_timestamp();
  new.answer_deadline:=clock_timestamp()+interval '10 seconds';
  new.exact_bonus:=0;
  if v_is_bot and new.bot_answer is null and v_correct is not null then
    new.bot_answer:=greatest(0,round(v_correct*(1+((random()*2-1)*(0.06+random()*0.20))))::bigint);
  end if;
  return new;
end
$$;

drop trigger if exists bilbakalim_numeric_round_v2 on public.trivia_rounds;
create trigger bilbakalim_numeric_round_v2
before insert on public.trivia_rounds
for each row execute function private.normalize_bilbakalim_round_v2();

create or replace function public.answer_trivia_v3(p_round_id uuid, p_answer_index integer)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  q public.trivia_rounds;
  r public.game_rooms;
  my_answer bigint:=p_answer_index::bigint;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into q from public.trivia_rounds where id=p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id for update;
  if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
  if q.resolved_at is not null then return r; end if;
  if not exists(select 1 from public.trivia_questions tq where tq.id=q.question_id and tq.question_kind='bil_bakalim') then
    raise exception 'not_bilbakalim_round';
  end if;
  if q.answer_deadline is not null and clock_timestamp()>q.answer_deadline then
    return public.claim_estimate_timeout_v1(q.id);
  end if;
  if my_answer<0 or my_answer>2147483647 then raise exception 'invalid_numeric_estimate'; end if;

  -- First valid estimate is final. A repeated client request cannot change it.
  insert into public.trivia_answers(round_id,player_id,answer_index,is_correct)
  values(q.id,auth.uid(),my_answer,false)
  on conflict(round_id,player_id) do nothing;

  return public.resolve_bilbakalim_round_v1(q.id,false);
end
$$;

create or replace function public.resolve_bilbakalim_round_v1(p_round_id uuid, p_timeout boolean default false)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  q public.trivia_rounds;
  r public.game_rooms;
  v_correct bigint;
  host_est bigint;
  guest_est bigint;
  bot_est bigint;
  host_dist bigint;
  guest_dist bigint;
  bot_dist bigint;
  side text:='none';
  award int:=10;
begin
  select * into q from public.trivia_rounds where id=p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id for update;
  if q.resolved_at is not null then return r; end if;
  if p_timeout and q.answer_deadline is not null and clock_timestamp()<q.answer_deadline then return r; end if;

  select a.correct_value into v_correct from public.bilbakalim_answers a where a.question_id=q.question_id;
  if v_correct is null then raise exception 'bilbakalim_correct_answer_missing'; end if;

  select answer_index into host_est from public.trivia_answers where round_id=q.id and player_id=r.host_id;
  if r.is_bot then
    bot_est:=q.bot_answer;
  else
    select answer_index into guest_est from public.trivia_answers where round_id=q.id and player_id=r.guest_id;
  end if;

  if not p_timeout then
    if r.is_bot and host_est is null then return r; end if;
    if not r.is_bot and (host_est is null or guest_est is null) then
      update public.game_rooms set last_event='bilbakalim_waiting' where id=r.id returning * into r;
      return r;
    end if;
  end if;

  if r.is_bot then
    if host_est is null and bot_est is not null then side:='bot';
    elsif host_est is not null and bot_est is null then side:='host';
    elsif host_est is not null and bot_est is not null then
      host_dist:=abs(host_est-v_correct); bot_dist:=abs(bot_est-v_correct);
      if host_dist<bot_dist then side:='host'; elsif bot_dist<host_dist then side:='bot'; else side:='tie'; end if;
    end if;
  else
    if host_est is not null and guest_est is null then side:='host';
    elsif guest_est is not null and host_est is null then side:='guest';
    elsif host_est is not null and guest_est is not null then
      host_dist:=abs(host_est-v_correct); guest_dist:=abs(guest_est-v_correct);
      if host_dist<guest_dist then side:='host'; elsif guest_dist<host_dist then side:='guest'; else side:='tie'; end if;
    end if;
  end if;

  update public.trivia_answers set is_correct=(answer_index=v_correct) where round_id=q.id;

  -- tie and double no-answer deliberately award zero.
  if side='host' then
    update public.game_rooms set host_score=host_score+award,host_round_score=host_round_score+award where id=r.id;
  elsif side='guest' or side='bot' then
    update public.game_rooms set guest_score=guest_score+award,guest_round_score=guest_round_score+award where id=r.id;
  end if;

  update public.trivia_rounds
  set bonus_points=10,
      host_answer=host_est,guest_answer=guest_est,bot_answer=bot_est,correct_answer=v_correct,
      winner_side=side,
      winner_id=case when side='host' then r.host_id when side='guest' then r.guest_id else null end,
      winner_distance=case when side='host' then host_dist when side='guest' then guest_dist when side='bot' then bot_dist when side='tie' then coalesce(host_dist,guest_dist,bot_dist) else null end,
      resolved_at=clock_timestamp(),result_until=clock_timestamp()+interval '3 seconds'
  where id=q.id;

  update public.game_rooms
  set status='quiz',current_player_id=null,bot_turn=false,turn_deadline=null,
      last_event='bilbakalim_result',
      last_event_player_id=case when side='host' then r.host_id when side='guest' then r.guest_id else null end
  where id=r.id returning * into r;
  return r;
end
$$;

-- No answer is represented by absence of a trivia_answers row; timeout resolution
-- treats it as the farthest possible answer without inventing a fake number.
create or replace function public.claim_estimate_timeout_v1(p_round_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare q public.trivia_rounds; r public.game_rooms;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into q from public.trivia_rounds where id=p_round_id;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id;
  if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
  return public.resolve_bilbakalim_round_v1(q.id,true);
end
$$;

-- Defense in depth: answers are private until the server resolves the round.
alter table public.trivia_answers enable row level security;
drop policy if exists "trivia answers private until resolved" on public.trivia_answers;
create policy "trivia answers private until resolved"
on public.trivia_answers for select to authenticated
using (
  player_id=auth.uid()
  or exists(
    select 1 from public.trivia_rounds q
    where q.id=trivia_answers.round_id
      and q.resolved_at is not null
      and public.is_room_participant(q.room_id,auth.uid())
  )
);

revoke all on function private.normalize_bilbakalim_round_v2() from public,anon,authenticated;
revoke all on function public.start_bilbakalim_round_v1(uuid,integer) from public,anon;
revoke all on function public.answer_trivia_v3(uuid,integer) from public,anon;
revoke all on function public.resolve_bilbakalim_round_v1(uuid,boolean) from public,anon,authenticated;
revoke all on function public.claim_estimate_timeout_v1(uuid) from public,anon;
grant execute on function public.start_bilbakalim_round_v1(uuid,integer) to authenticated;
grant execute on function public.answer_trivia_v3(uuid,integer) to authenticated;
grant execute on function public.claim_estimate_timeout_v1(uuid) to authenticated;
