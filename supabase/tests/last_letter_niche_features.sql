-- Regression checks run in a transaction; all fixtures are rolled back.
begin;
do $$
declare ids uuid[]; rid uuid := gen_random_uuid();
begin
  select array_agg(id) into ids from (select id from public.profiles order by created_at limit 3) p;
  if cardinality(ids)<3 then raise exception 'test_requires_three_existing_profiles'; end if;
  perform set_config('test.user_a',ids[1]::text,true);
  perform set_config('test.user_b',ids[2]::text,true);
  perform set_config('test.user_c',ids[3]::text,true);
  perform set_config('test.room',rid::text,true);
  insert into public.game_rooms(id,code,host_id,guest_id,status,language,current_player_id,turn_deadline,is_bot)
    values(rid,upper(left(replace(rid::text,'-',''),6)),ids[1],ids[2],'playing','tr',ids[1],clock_timestamp()+interval '15 seconds',false);
  if has_function_privilege('anon','public.last_letter_daily_v1(text,text,uuid,text)','EXECUTE') then raise exception 'anonymous_daily_access'; end if;
  if has_function_privilege('anon','public.claim_last_letter_fair_hint_v1(uuid,integer)','EXECUTE') then raise exception 'anonymous_hint_access'; end if;
end $$;
set local role authenticated;
do $$
declare a jsonb; again jsonb; b jsonb; next jsonb; w text; required text; rid uuid := current_setting('test.room')::uuid;
begin
  perform set_config('request.jwt.claim.sub',current_setting('test.user_a'),true);
  a := public.last_letter_daily_v1('start','tr');
  again := public.last_letter_daily_v1('start','tr');
  if a->>'id' <> again->>'id' or a->>'ends_at' <> again->>'ends_at' then raise exception 'daily_restart_reset_clock'; end if;
  perform set_config('test.daily_a',a->>'id',true);
  required := right(a->>'start_word',1);
  select value into w from unnest(array['masa','ada','para','zar','resim','limon','nar','taş','şal','kalem']) value
    where left(value,1)=required and value<>a->>'start_word' limit 1;
  if w is null then raise exception 'no_test_chain_word_for_%',required; end if;
  next := public.last_letter_daily_v1('word','tr',(a->>'id')::uuid,w);
  if jsonb_array_length(next->'words')<>1 then raise exception 'daily_word_not_accepted'; end if;
  begin
    perform public.last_letter_daily_v1('word','tr',(a->>'id')::uuid,w);
    raise exception 'duplicate_word_accepted';
  exception when others then
    if sqlerrm not in ('word_already_used','wrong_start_letter') then raise; end if;
  end;
  perform set_config('request.jwt.claim.sub',current_setting('test.user_b'),true);
  if exists(select 1 from public.last_letter_daily_runs where id=(a->>'id')::uuid) then raise exception 'daily_rls_leak'; end if;
  begin
    perform public.last_letter_daily_v1('word','tr',(a->>'id')::uuid,w);
    raise exception 'foreign_daily_run_mutated';
  exception when others then
    if sqlerrm<>'daily_run_not_found' then raise; end if;
  end;
  b := public.last_letter_daily_v1('start','tr');
  if a->>'start_word' <> b->>'start_word' or a->>'day' <> b->>'day' then raise exception 'daily_start_not_shared'; end if;
  if a->>'id' = b->>'id' then raise exception 'daily_attempt_shared_between_accounts'; end if;
  perform set_config('test.daily_b',b->>'id',true);
  perform set_config('request.jwt.claim.sub',current_setting('test.user_a'),true);
  next := public.last_letter_daily_v1('finish','tr',(a->>'id')::uuid);
  if not (next->>'finished')::boolean then raise exception 'daily_finish_not_applied'; end if;
  again := public.last_letter_daily_v1('start','tr');
  if again->>'id' <> a->>'id' or not (again->>'finished')::boolean then raise exception 'daily_second_attempt_allowed'; end if;
  next := public.claim_last_letter_fair_hint_v1(rid,0);
  if (next->>'used')::int<>1 then raise exception 'first_hint_count'; end if;
  next := public.claim_last_letter_fair_hint_v1(rid,0);
  if (next->>'used')::int<>2 then raise exception 'hint_counter_reset'; end if;
  next := public.claim_last_letter_fair_hint_v1(rid,0);
  if (next->>'used')::int<>3 or (next->>'left')::int<>0 then raise exception 'third_hint_count'; end if;
  begin
    perform public.claim_last_letter_fair_hint_v1(rid,0);
    raise exception 'fourth_hint_allowed';
  exception when others then
    if sqlerrm<>'hint_limit_reached' then raise; end if;
  end;
  perform set_config('request.jwt.claim.sub',current_setting('test.user_c'),true);
  begin
    perform public.claim_last_letter_fair_hint_v1(rid,0);
    raise exception 'outsider_hint_allowed';
  exception when others then
    if sqlerrm<>'not_room_member' then raise; end if;
  end;
  perform set_config('request.jwt.claim.sub',current_setting('test.user_b'),true);
  if exists(select 1 from public.last_letter_hint_usage where room_id=rid) then raise exception 'hint_rls_leak'; end if;
  begin
    perform public.claim_last_letter_fair_hint_v1(rid,0);
    raise exception 'out_of_turn_hint_allowed';
  exception when others then
    if sqlerrm<>'not_your_turn' then raise; end if;
  end;
end $$;
reset role;
update public.last_letter_daily_runs set ends_at=clock_timestamp()-interval '1 second'
  where id=current_setting('test.daily_b')::uuid;
update public.game_rooms set current_player_id=current_setting('test.user_b')::uuid,
  turn_deadline=clock_timestamp()+interval '15 seconds' where id=current_setting('test.room')::uuid;
set local role authenticated;
do $$
declare v jsonb; rid uuid := current_setting('test.room')::uuid;
begin
  perform set_config('request.jwt.claim.sub',current_setting('test.user_b'),true);
  v := public.last_letter_daily_v1('word','tr',current_setting('test.daily_b')::uuid,'kalem');
  if not (v->>'finished')::boolean or jsonb_array_length(v->'words')<>0 then raise exception 'expired_daily_accepts_words'; end if;
  v := public.claim_last_letter_fair_hint_v1(rid,0);
  if (v->>'used')::int<>1 then raise exception 'opponents_hint_quota_not_independent'; end if;
end $$;
rollback;
select 'PASS: one daily attempt, shared seed, server deadline, dictionary checks, RLS isolation, three hints, independent opponent quota' as validation;
