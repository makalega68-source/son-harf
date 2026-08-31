-- Stable 10-second duel/bonus timers, deterministic bonus results, and live admin controls.

create or replace function public.sonharf_config_enabled(
  p_key text,
  p_default boolean default true
)
returns boolean
language sql
stable
security definer
set search_path=public,pg_temp
as $$
  select coalesce(
    (
      select case
        when jsonb_typeof(c.value)='boolean' then (c.value #>> '{}')::boolean
        else p_default
      end
      from public.app_config c
      where c.key=p_key
    ),
    p_default
  )
$$;

revoke all on function public.sonharf_config_enabled(text,boolean) from public,anon;
grant execute on function public.sonharf_config_enabled(text,boolean) to authenticated;

create or replace function public.sonharf_turn_deadline(p_mode text)
returns timestamptz
language sql
set search_path=public
as $$
  select clock_timestamp() + interval '10 seconds'
$$;

create or replace function public.apply_bomb_duel_deadline_v1()
returns trigger
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  if new.status in ('playing','final','sudden_death')
     and new.turn_deadline is not null
     and new.current_player_id is not null
     and new.turn_deadline > clock_timestamp() + interval '11 seconds' then
    new.turn_deadline:=clock_timestamp() + interval '10 seconds';
  end if;
  return new;
end
$$;

create or replace function public.start_bilbakalim_round_v1(
  p_room_id uuid,
  p_milestone integer
)
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
    and coalesce(q.option_a,'')<>''
    and coalesce(q.option_b,'')<>''
    and coalesce(q.option_c,'')<>''
    and coalesce(q.option_d,'')<>''
  order by random()
  limit 1;

  if qid is null then return r; end if;
  if r.is_bot then
    bot_est:=greatest(0,round(correct_value*(1+((random()*2-1)*(0.06+random()*0.20))))::bigint);
  end if;

  insert into public.trivia_rounds(
    room_id,milestone,bonus_points,question_id,reveal_at,answer_deadline,
    exact_bonus,bot_answer,resume_status,resume_current_player_id,resume_bot_turn
  )
  values(
    r.id,p_milestone,3,qid,clock_timestamp(),clock_timestamp()+interval '10 seconds',
    0,bot_est,'playing',r.current_player_id,r.bot_turn
  );

  update public.game_rooms
  set status='quiz',
      current_player_id=null,
      bot_turn=false,
      turn_deadline=null,
      last_event='bilbakalim_started',
      last_event_player_id=null
  where id=r.id
  returning * into r;

  return r;
end
$$;

create or replace function public.answer_trivia_v3(
  p_round_id uuid,
  p_answer_index integer
)
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
  select * into q from public.trivia_rounds where id=p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id for update;
  if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
  if q.resolved_at is not null then return r; end if;
  if q.answer_deadline is not null and clock_timestamp()>q.answer_deadline then
    return public.claim_estimate_timeout_v1(q.id);
  end if;
  if my_answer < 0 or not exists(
    select 1
    from public.trivia_questions tq
    where tq.id=q.question_id
      and my_answer::text in (tq.option_a,tq.option_b,tq.option_c,tq.option_d)
  ) then
    raise exception 'invalid_trivia_option';
  end if;

  insert into public.trivia_answers(round_id,player_id,answer_index,is_correct)
  values(q.id,auth.uid(),my_answer,false)
  on conflict(round_id,player_id) do nothing;

  return public.resolve_bilbakalim_round_v1(q.id,false);
end
$$;

create or replace function public.resolve_bilbakalim_round_v1(
  p_round_id uuid,
  p_timeout boolean default false
)
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
  award int;
begin
  select * into q from public.trivia_rounds where id=p_round_id for update;
  if q.id is null then raise exception 'quiz_not_found'; end if;
  select * into r from public.game_rooms where id=q.room_id for update;
  if q.resolved_at is not null then return r; end if;
  if p_timeout and q.answer_deadline is not null and clock_timestamp()<q.answer_deadline then return r; end if;

  select correct_value into v_correct from public.bilbakalim_answers where question_id=q.question_id;
  if v_correct is null then
    select correct_index into v_correct from public.trivia_questions where id=q.question_id;
  end if;

  select answer_index into host_est from public.trivia_answers where round_id=q.id and player_id=r.host_id;
  if not r.is_bot then
    select answer_index into guest_est from public.trivia_answers where round_id=q.id and player_id=r.guest_id;
  else
    bot_est:=q.bot_answer;
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
      host_dist:=abs(host_est-v_correct);
      bot_dist:=abs(bot_est-v_correct);
      if host_dist<bot_dist then side:='host';
      elsif bot_dist<host_dist then side:='bot';
      else side:='tie';
      end if;
    end if;
  else
    if host_est is not null and guest_est is null then side:='host';
    elsif guest_est is not null and host_est is null then side:='guest';
    elsif host_est is not null and guest_est is not null then
      host_dist:=abs(host_est-v_correct);
      guest_dist:=abs(guest_est-v_correct);
      if host_dist<guest_dist then side:='host';
      elsif guest_dist<host_dist then side:='guest';
      else side:='tie';
      end if;
    end if;
  end if;

  update public.trivia_answers
  set is_correct=(answer_index=v_correct)
  where round_id=q.id;

  award:=greatest(1,q.bonus_points);
  if side='host' then
    update public.game_rooms set host_score=host_score+award,host_round_score=host_round_score+award where id=r.id;
  elsif side='guest' or side='bot' then
    update public.game_rooms set guest_score=guest_score+award,guest_round_score=guest_round_score+award where id=r.id;
  elsif side='tie' then
    update public.game_rooms
    set host_score=host_score+award,host_round_score=host_round_score+award,
        guest_score=guest_score+award,guest_round_score=guest_round_score+award
    where id=r.id;
  end if;

  update public.trivia_rounds
  set host_answer=host_est,
      guest_answer=guest_est,
      bot_answer=bot_est,
      correct_answer=v_correct,
      winner_side=side,
      winner_id=case when side='host' then r.host_id when side='guest' then r.guest_id else null end,
      winner_distance=case
        when side='host' then host_dist
        when side='guest' then guest_dist
        when side='bot' then bot_dist
        when side='tie' then coalesce(host_dist,guest_dist,bot_dist)
        else null
      end,
      resolved_at=clock_timestamp(),
      result_until=clock_timestamp()+interval '5 seconds'
  where id=q.id;

  update public.game_rooms
  set status='quiz',
      current_player_id=null,
      bot_turn=false,
      turn_deadline=null,
      last_event='bilbakalim_result',
      last_event_player_id=case when side='host' then r.host_id when side='guest' then r.guest_id else null end
  where id=r.id
  returning * into r;

  return r;
end
$$;

create or replace function public.join_random_matchmaking(p_language text)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then
    raise exception 'maintenance_mode';
  end if;
  if not public.sonharf_config_enabled('matchmaking_enabled',true) and not public.is_admin() then
    raise exception 'matchmaking_disabled';
  end if;
  return public.join_random_matchmaking_v2(p_language,public.get_game_mode_v1());
end
$$;

revoke all on function public.join_random_matchmaking_v2(text,text) from public,anon,authenticated;
revoke all on function public.join_random_matchmaking(text) from public,anon;
grant execute on function public.join_random_matchmaking(text) to authenticated;

create or replace function public.enforce_new_game_admin_controls_v1()
returns trigger
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then
    raise exception 'maintenance_mode';
  end if;
  return new;
end
$$;

drop trigger if exists enforce_new_game_admin_controls_v1 on public.game_rooms;
create trigger enforce_new_game_admin_controls_v1
before insert on public.game_rooms
for each row execute function public.enforce_new_game_admin_controls_v1();

drop trigger if exists enforce_new_invite_admin_controls_v1 on public.game_invites;
create trigger enforce_new_invite_admin_controls_v1
before insert on public.game_invites
for each row execute function public.enforce_new_game_admin_controls_v1();

drop policy if exists "chat messages admin control" on public.chat_messages;
create policy "chat messages admin control"
on public.chat_messages
as restrictive
for insert
to authenticated
with check (
  public.sonharf_config_enabled('chat_enabled',true)
  and not public.sonharf_config_enabled('maintenance_mode',false)
);

select pg_notify('pgrst','reload schema');
