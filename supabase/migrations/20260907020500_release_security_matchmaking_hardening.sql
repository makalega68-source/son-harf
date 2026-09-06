-- Son Harf release hardening (2026-09-07)
-- Closes anonymous timeout-state mutation and enforces the approved 15-second
-- real-player matchmaking window before bot fallback.

create or replace function public.claim_turn_timeout_v2(
  p_room_id uuid,
  p_expected_player_id uuid,
  p_expected_deadline timestamptz
)
returns public.game_rooms
language plpgsql
security definer
set search_path=pg_catalog,public,pg_temp
as $$
declare
  r public.game_rooms;
  timed_out_player uuid;
  penalty int;
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if v_uid is distinct from r.host_id and v_uid is distinct from r.guest_id then
    raise exception 'not_participant';
  end if;
  if r.status not in ('playing','final','sudden_death') then return r; end if;
  if r.turn_deadline is null then return r; end if;
  if r.current_player_id is distinct from p_expected_player_id then return r; end if;
  if r.turn_deadline is distinct from p_expected_deadline then return r; end if;
  if r.turn_deadline>=now() then return r; end if;

  timed_out_player:=r.current_player_id;
  penalty:=case when r.status='final' then 2 else 1 end;

  if r.status='sudden_death' then
    update public.game_rooms
    set status='finished',
        winner_id=case when timed_out_player=host_id then guest_id else host_id end,
        finished_at=now(),turn_deadline=null,last_event='turn_expired',
        last_event_player_id=timed_out_player
    where id=r.id returning * into r;
    return r;
  end if;

  if timed_out_player=r.host_id then
    update public.game_rooms set host_score=host_score-penalty,host_streak=0 where id=r.id;
  elsif timed_out_player=r.guest_id then
    update public.game_rooms set guest_score=guest_score-penalty,guest_streak=0 where id=r.id;
  else
    return r;
  end if;

  update public.game_rooms
  set current_player_id=case when timed_out_player=host_id then guest_id else host_id end,
      turn_deadline=now()+interval '45 seconds',last_event='turn_expired',
      last_event_player_id=timed_out_player,
      final_moves_remaining=case
        when status='final' and final_moves_remaining>0 then final_moves_remaining-1
        else final_moves_remaining
      end
  where id=r.id returning * into r;
  return r;
end;
$$;

revoke all on function public.claim_turn_timeout_v2(uuid,uuid,timestamptz) from public,anon;
grant execute on function public.claim_turn_timeout_v2(uuid,uuid,timestamptz) to authenticated,service_role;

create or replace function public.poll_random_matchmaking_v2()
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  q public.matchmaking_queue;
  r public.game_rooms;
  generated_code text;
  v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into q from public.matchmaking_queue where user_id=v_uid for update;
  if q.user_id is null then return null; end if;
  if q.status='matched' and q.room_id is not null then
    select * into r from public.game_rooms where id=q.room_id;
    return r;
  end if;

  if q.status='waiting' then
    update public.matchmaking_queue set heartbeat_at=now() where user_id=v_uid;
    if q.queued_at<=now()-interval '15 seconds' then
      generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
      insert into public.game_rooms(
        code,host_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,
        bot_turn,room_type,game_mode,host_last_seen_at
      )
      values(
        generated_code,v_uid,'playing',v_uid,public.sonharf_turn_deadline(q.game_mode),
        q.language,true,case when q.language='tr' then 'KelimeBot' else 'WordBot' end,
        false,'bot',q.game_mode,now()
      ) returning * into r;

      update public.matchmaking_queue
      set status='matched',room_id=r.id,heartbeat_at=now()
      where user_id=v_uid and status='waiting';

      if not found then
        delete from public.game_rooms where id=r.id and host_id=v_uid and is_bot=true;
        select gr.* into r
        from public.matchmaking_queue mq
        left join public.game_rooms gr on gr.id=mq.room_id
        where mq.user_id=v_uid;
        return r;
      end if;

      update public.profiles set presence_status='in_game',last_seen_at=now() where id=v_uid;
      return r;
    end if;
  end if;
  return null;
end;
$$;

revoke all on function public.poll_random_matchmaking_v2() from public,anon;
grant execute on function public.poll_random_matchmaking_v2() to authenticated,service_role;

select pg_notify('pgrst','reload schema');
