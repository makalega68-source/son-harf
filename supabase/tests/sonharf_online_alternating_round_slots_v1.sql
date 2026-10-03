BEGIN;
DO $test$
declare
  h uuid; g uuid; rid uuid; r public.game_rooms;
begin
  select p.id into h from public.profiles p where not exists
    (select 1 from public.game_rooms a where a.status in ('waiting','playing','final','sudden_death','paused')
      and (a.host_id=p.id or a.guest_id=p.id)) order by p.id limit 1;
  select p.id into g from public.profiles p where p.id<>h and not exists
    (select 1 from public.game_rooms a where a.status in ('waiting','playing','final','sudden_death','paused')
      and (a.host_id=p.id or a.guest_id=p.id)) order by p.id limit 1;
  if h is null or g is null then raise exception 'test_players_unavailable'; end if;
  insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,
    host_round_words,guest_round_words,round_word_count,language,game_mode,is_bot)
  values('T'||substr(replace(gen_random_uuid()::text,'-',''),1,5),h,g,'playing',h,
    clock_timestamp()+interval '1 hour',8,8,16,'tr','normal',false)
  returning id into rid;
  perform set_config('request.jwt.claim.sub',h::text,true);
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  if r.current_player_id<>g or r.host_round_words<>9 or r.guest_round_words<>8 then
    raise exception 'failed_turn_did_not_alternate'; end if;
  perform set_config('request.jwt.claim.sub',g::text,true);
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  if r.current_player_id<>h or r.host_round_words<>9 or r.guest_round_words<>9 then
    raise exception 'second_failed_turn_did_not_alternate'; end if;
  perform set_config('request.jwt.claim.sub',h::text,true);
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  if r.current_player_id<>g or r.host_round_words<>10 then
    raise exception 'penultimate_turn_did_not_alternate'; end if;
  perform set_config('request.jwt.claim.sub',g::text,true);
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  if r.round_no<>2 or r.host_round_words<>0 or r.guest_round_words<>0 or r.current_player_id<>g then
    raise exception 'round_did_not_advance'; end if;
  -- The same boundary through timeout, including its score penalty.
  update public.game_rooms set host_round_words=9,guest_round_words=9,
    round_word_count=18,current_player_id=h,last_event='valid_word',turn_deadline=clock_timestamp()-interval '1 second'
    where id=rid;
  perform set_config('request.jwt.claim.sub',g::text,true);
  r:=public.claim_turn_timeout(rid);
  if r.current_player_id<>g or r.host_round_words<>10 then
    raise exception 'timeout_did_not_alternate'; end if;
  update public.game_rooms set last_event='valid_word',turn_deadline=clock_timestamp()-interval '1 second' where id=rid;
  perform set_config('request.jwt.claim.sub',h::text,true);
  r:=public.claim_turn_timeout(rid);
  if r.round_no<>3 or r.host_round_words<>0 or r.guest_round_words<>0 then
    raise exception 'timeout_did_not_advance_round'; end if;
  -- Mix a failed slot with accepted words: neither player gets a repeated turn.
  update public.game_rooms set round_no=1,host_round_words=8,guest_round_words=8,
    round_word_count=16,current_player_id=h,last_event='valid_word',
    turn_deadline=clock_timestamp()+interval '1 hour',opening_turn_started_at=null
    where id=rid;
  perform set_config('request.jwt.claim.sub',h::text,true);
  r:=public.switch_turn_after_failure(rid,'invalid_word');
  perform set_config('request.jwt.claim.sub',g::text,true);
  r:=public.submit_word_v3(rid,'araba');
  if r.current_player_id<>h or r.guest_round_words<>9 then raise exception 'valid_ninth_turn_failed'; end if;
  perform set_config('request.jwt.claim.sub',h::text,true);
  r:=public.submit_word_v3(rid,'armut');
  if r.current_player_id<>g or r.host_round_words<>10 then raise exception 'valid_tenth_turn_failed'; end if;
  perform set_config('request.jwt.claim.sub',g::text,true);
  r:=public.submit_word_v3(rid,'tabak');
  if r.round_no<>2 or r.host_round_words<>0 or r.guest_round_words<>0 then raise exception 'mixed_round_failed'; end if;
end
$test$;

ROLLBACK;
