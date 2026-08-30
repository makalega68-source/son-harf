-- Fix Bil Bakalim choice data, 3-point bonus, and stale quiz results.

update public.trivia_questions q
set
  option_a = case
    when q.answer_unit in ('yıl','year') then greatest(1, a.correct_value - 20)::text
    else greatest(1, round(a.correct_value * 0.55))::bigint::text
  end,
  option_b = case
    when q.answer_unit in ('yıl','year') then greatest(1, a.correct_value - 5)::text
    else greatest(1, round(a.correct_value * 0.80))::bigint::text
  end,
  option_c = a.correct_value::text,
  option_d = case
    when q.answer_unit in ('yıl','year') then (a.correct_value + 15)::text
    else greatest(1, round(a.correct_value * 1.45))::bigint::text
  end,
  correct_index = 2
from public.bilbakalim_answers a
where a.question_id = q.id
  and (coalesce(q.option_a,'')='' or coalesce(q.option_b,'')='' or coalesce(q.option_c,'')='' or coalesce(q.option_d,'')='');

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
    r.id,p_milestone,3,qid,now(),now()+interval '20 seconds',
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

with latest as (
  select distinct on (tr.room_id)
    tr.room_id,tr.resume_status,tr.resume_current_player_id,tr.resume_bot_turn
  from public.trivia_rounds tr
  where tr.resolved_at is not null
    and coalesce(tr.result_until,tr.resolved_at) < now()
  order by tr.room_id,tr.created_at desc
)
update public.game_rooms r
set status=coalesce(l.resume_status,'playing'),
    current_player_id=case when coalesce(l.resume_bot_turn,false) then null else l.resume_current_player_id end,
    bot_turn=coalesce(l.resume_bot_turn,false),
    turn_deadline=case when coalesce(l.resume_bot_turn,false) then null else clock_timestamp()+interval '7 seconds' end,
    last_event='bilbakalim_finished',
    last_event_player_id=null
from latest l
where r.id=l.room_id and r.status='quiz';

select pg_notify('pgrst','reload schema');