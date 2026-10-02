-- All fixtures and entitlement changes roll back.
begin;
do $test$
declare a uuid;b uuid;w date:='2099-01-05';v jsonb;f text;max_xp bigint;n integer;cw date:=date_trunc('week',now() at time zone 'Europe/Istanbul')::date;
begin
 select id into a from public.profiles where not is_vip and not public.has_permanent_entitlement_v1(id,'pro_lifetime') order by id limit 1;
 select id into b from public.profiles where id<>a order by id limit 1;
 if a is null or b is null then raise exception 'Two testable profiles required';end if;
 perform set_config('request.jwt.claim.sub',a::text,true);
 perform set_config('request.jwt.claims',jsonb_build_object('sub',a,'role','authenticated')::text,true);
 insert into public.throne_xp_events(user_id,source,source_id,game,xp,earned_at,week_start)values
 (a,'test_throne_reward','a_previous','siege',2000,'2099-01-01T10:00:00Z',w-7),
 (b,'test_throne_reward','b_previous','atelier',2000,'2099-01-01T11:00:00Z',w-7),
 (b,'test_throne_reward','b_next','last_letter',2000,'2099-01-06T11:00:00Z',w);
 if private.throne_champion_at(w::timestamp at time zone 'Europe/Istanbul')<>a then raise exception 'tie_break_or_week_start';end if;
 if private.throne_champion_at(((w+7)::timestamp at time zone 'Europe/Istanbul')-interval '1 millisecond')<>a then raise exception 'premature_expiry';end if;
 if private.throne_champion_at((w+7)::timestamp at time zone 'Europe/Istanbul')<>b then raise exception 'weekly_handoff';end if;
 if private.throne_champion_at((w+14)::timestamp at time zone 'Europe/Istanbul') is not null then raise exception 'empty_week_reused_reward';end if;
 select coalesce(max(xp),0) into max_xp from(select sum(xp) xp from public.throne_xp_events where week_start=cw-7 group by user_id)t;
 for n in 0..(max_xp/2000)::integer loop
 insert into public.throne_xp_events(user_id,source,source_id,game,xp,week_start)values(a,'test_throne_reward','current-'||n,'siege',2000,cw-7);
 end loop;
 select profile_frame_id into f from public.get_public_profile_frame_v1(a);
 if f<>'frame_throne_champion' then raise exception 'champion_not_visible';end if;
 v:=public.get_throne_week_v1();
 if v->'previous_owner'->>'user_id'<>a::text or (v->'previous_owner'->>'expires_at')::timestamptz<>(cw+7)::timestamp at time zone 'Europe/Istanbul' then raise exception 'reward_metadata';end if;
 if exists(select 1 from public.store_catalog_config where product_id='profile_frame_gold_crest' and enabled) then raise exception 'gold_still_for_sale';end if;
 if has_function_privilege('anon','public.get_public_profile_frame_v1(uuid)','execute') or has_function_privilege('authenticated','private.throne_champion_at(timestamptz)','execute') then raise exception 'reward_permissions';end if;
 update public.profiles set is_vip=true where id=a;
 v:=public.get_vip_entitlements_v7();
 if not (v->>'score_calculator_access')::boolean or not (v->>'letter_table_access')::boolean or not (v->>'series_game_access')::boolean or not (v->>'private_rooms')::boolean or not (v->>'post_match_analysis')::boolean or not (v->>'saved_friend_list')::boolean or not (v->>'used_words_access')::boolean or not (v->>'rewarded_ad_bypass')::boolean or (v->>'active_game_limit')::int<>50 then raise exception 'missing_pro_benefit';end if;
 -- Reaching the participant check proves the premium gate passed, without creating a match.
 begin perform public.preview_word_siege_move_pro_v1('00000000-0000-0000-0000-000000000000','[]',true);raise exception 'unexpected_preview';
 exception when others then if sqlerrm<>'word_siege_not_participant' then raise;end if;end;
 begin perform public.get_word_siege_letter_table_v1('00000000-0000-0000-0000-000000000000');raise exception 'unexpected_letter_table';
 exception when others then if sqlerrm<>'word_siege_not_participant' then raise;end if;end;
end $test$;
rollback;
