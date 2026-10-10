-- Synthetic player, production RPCs, no surviving fixture data.
begin;
do $test$
declare h uuid:=gen_random_uuid(); rid uuid; r public.game_rooms; before_room public.game_rooms;
  moves integer:=0;
begin
  insert into auth.users(id,email) values(h,h||'@sonharf-slots.invalid');
  perform set_config('request.jwt.claim.sub',h::text,true);
  perform set_config('request.jwt.claims',jsonb_build_object('sub',h,'role','authenticated','is_anonymous',false)::text,true);
  insert into public.game_rooms(code,host_id,status,current_player_id,turn_deadline,
    language,game_mode,is_bot,bot_turn,host_round_words,guest_round_words,round_word_count)
  values('T'||substr(replace(gen_random_uuid()::text,'-',''),1,5),h,'playing',h,
    clock_timestamp()-interval '1 second','tr','normal',true,false,8,8,16)
  returning * into r;
  rid:=r.id;
  r:=public.claim_turn_timeout(rid);
  if r.host_round_words<>9 or r.guest_round_words<>8 or not r.bot_turn then
    raise exception 'ai_timeout_slot_not_consumed'; end if;
  r:=public.bot_take_turn(rid);
  if r.host_round_words<>9 or r.guest_round_words<>9 or r.bot_turn then
    raise exception 'ai_ninth_turn_not_alternating'; end if;
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  if r.host_round_words<>10 or r.guest_round_words<>9 or not r.bot_turn then
    raise exception 'ai_invalid_tenth_slot_not_consumed'; end if;
  r:=public.bot_take_turn(rid);
  if r.round_no<>2 or r.host_round_words<>0 or r.guest_round_words<>0 or not r.bot_turn then
    raise exception 'ai_round_not_closed_at_ten_each'; end if;
  update public.game_rooms set status='finished',finished_at=clock_timestamp(),bot_turn=false,turn_deadline=null where id=rid;
  -- New match: 30 failed human turns + 30 valid AI turns must finish exactly three rounds.
  insert into public.game_rooms(code,host_id,status,current_player_id,turn_deadline,
    language,game_mode,is_bot,bot_turn)
  values('T'||substr(replace(gen_random_uuid()::text,'-',''),1,5),h,'playing',h,
    clock_timestamp()+interval '1 hour','tr','normal',true,false) returning * into r;
  rid:=r.id;
  while r.status='playing' loop
    before_room:=r;
    if r.bot_turn then r:=public.bot_take_turn(rid);
    else r:=public.switch_turn_after_failure(rid,'invalid_word'); end if;
    moves:=moves+1;
    if moves>60 then raise exception 'ai_match_never_finished'; end if;
    if r.round_no=before_room.round_no and r.status='playing' then
      if abs(r.host_round_words-r.guest_round_words)>1 then raise exception 'unequal_turn_budget'; end if;
      if r.bot_turn=before_room.bot_turn then raise exception 'same_player_moved_twice'; end if;
    end if;
  end loop;
  if r.status<>'finished' or not r.winner_is_bot or moves<>60 then
    raise exception 'wrong_three_round_finish:%:%',r.status,moves; end if;
  before_room:=r;
  r:=public.bot_take_turn(rid);
  if r.action_seq<>before_room.action_seq then raise exception 'bot_played_after_finish'; end if;
  if has_function_privilege('authenticated','private.sonharf_consume_missed_slot_v1(uuid,uuid)','execute') then
    raise exception 'private_helper_exposed'; end if;
end $test$;
rollback;
select 'PASS: AI timeout/invalid slots, alternating turns, round boundary, exactly 60 turns across three rounds, finished-room idempotence' as verification;
