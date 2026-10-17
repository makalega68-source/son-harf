-- Production RPCs and RLS, synthetic players only. Every effect is rolled back.
begin;
do $setup$
declare a uuid:=gen_random_uuid();b uuid:=gen_random_uuid();c uuid:=gen_random_uuid();g uuid;
begin
 -- Fill only the rollback fixture's legacy tester slots so players below have ordinary free accounts.
 insert into public.moderator_accounts(email,note,auto_enrolled)
 select gen_random_uuid()::text||'@fixture-slot.invalid','rollback test slot',true from generate_series(1,5);
 insert into auth.users(id,email) values(a,a||'@social-test.invalid'),(b,b||'@social-test.invalid'),(c,c||'@social-test.invalid');
 insert into public.word_siege_games(player_one_id,player_two_id,status,board,bag,player_one_rack)
 values(a,b,'finished',private.word_siege_new_board_v1(),'','KALEMİR') returning id into g;
 perform set_config('social_test.ids',jsonb_build_object('a',a,'b',b,'c',c,'game',g)::text,true);
 perform set_config('request.jwt.claims',jsonb_build_object('sub',a,'role','authenticated','is_anonymous',false)::text,true);
 perform set_config('request.jwt.claim.sub',a::text,true);
end $setup$;
set local role authenticated;
do $sender$
declare ids jsonb:=current_setting('social_test.ids')::jsonb;a uuid:=(ids->>'a')::uuid;b uuid:=(ids->>'b')::uuid;r jsonb;code text;
begin
 if (select is_vip from public.profiles where id=a) or (public.get_vip_entitlements_v7()->>'series_game_access')::boolean then raise exception 'fixture_is_not_free';end if;
 if not public.can_use_pro_friend_list_v1(a) or public.can_use_pro_friend_list_v1(b) then raise exception 'free_social_owner_scope';end if;
 if not (public.get_vip_entitlements_v7()->>'saved_friend_list')::boolean then raise exception 'free_social_entitlement';end if;
 code:=public.get_my_invite_code_v1();
 if code !~ '^[A-F0-9]{12}$' or code<>public.get_my_invite_code_v1() then raise exception 'unstable_code';end if;
 perform set_config('social_test.code',code,true);
 r:=public.request_word_siege_rematch_v1((ids->>'game')::uuid);
 if r->>'receiver_id'<>b::text or r->>'rematch_of'<>ids->>'game' or r->>'status'<>'pending' then raise exception 'invalid_rematch';end if;
 perform set_config('social_test.invite',r->>'id',true);
 begin perform public.request_word_siege_rematch_v1((ids->>'game')::uuid);raise exception 'duplicate_accepted';
 exception when others then if sqlerrm<>'word_siege_invite_pending' then raise;end if;end;
end $sender$;
do $receiver$
declare ids jsonb:=current_setting('social_test.ids')::jsonb;b uuid:=(ids->>'b')::uuid;a uuid:=(ids->>'a')::uuid;g public.word_siege_games;
begin
 perform set_config('request.jwt.claims',jsonb_build_object('sub',b,'role','authenticated','is_anonymous',false)::text,true);
 perform set_config('request.jwt.claim.sub',b::text,true);
 if not exists(select 1 from public.get_social_inbox_v1() where kind='rematch' and target_id=current_setting('social_test.invite')::uuid) then raise exception 'rematch_inbox_missing';end if;
 g:=public.respond_word_siege_invite_v1(current_setting('social_test.invite')::uuid,true);
 if g.status<>'playing' or g.player_one_id<>a or g.player_two_id<>b then raise exception 'rematch_not_playable';end if;
 perform set_config('social_test.new_game',g.id::text,true);
 update public.social_activity set read_at=now() where user_id=b;
 if exists(select 1 from public.get_social_inbox_v1() where read_at is null) then raise exception 'read_not_saved';end if;
 if public.use_player_invite_code_v1(lower(current_setting('social_test.code')))<>a then raise exception 'invite_code_target';end if;
end $receiver$;
do $owner$
declare ids jsonb:=current_setting('social_test.ids')::jsonb;a uuid:=(ids->>'a')::uuid;b uuid:=(ids->>'b')::uuid;
begin
 perform set_config('request.jwt.claims',jsonb_build_object('sub',a,'role','authenticated','is_anonymous',false)::text,true);
 perform set_config('request.jwt.claim.sub',a::text,true);
 if exists(select 1 from public.social_activity where user_id<>a) then raise exception 'inbox_rls_leak';end if;
 if not exists(select 1 from public.get_social_inbox_v1() where kind='your_turn' and target_id=current_setting('social_test.new_game')::uuid) then raise exception 'turn_inbox_missing';end if;
 if not exists(select 1 from public.get_social_inbox_v1() where kind='friend_request' and actor_id=b) then raise exception 'friend_event_missing';end if;
 perform public.respond_friend_request(b,true);
 if not exists(select 1 from public.friendships where status='accepted') then raise exception 'free_friend_accept';end if;
 begin perform public.get_my_invite_code_v1();exception when others then raise exception 'code_lost_after_friend_accept';end;
end $owner$;
do $outsider$
declare ids jsonb:=current_setting('social_test.ids')::jsonb;c uuid:=(ids->>'c')::uuid;
begin
 perform set_config('request.jwt.claims',jsonb_build_object('sub',c,'role','authenticated','is_anonymous',false)::text,true);
 perform set_config('request.jwt.claim.sub',c::text,true);
 if exists(select 1 from public.get_social_inbox_v1()) or exists(select 1 from public.player_invite_codes) then raise exception 'foreign_social_data_leaked';end if;
 update public.social_activity set read_at=now();
 if found then raise exception 'foreign_read_marker';end if;
 begin perform public.request_word_siege_rematch_v1((ids->>'game')::uuid);raise exception 'outsider_rematch_allowed';
 exception when others then if sqlerrm<>'word_siege_rematch_unavailable' then raise;end if;end;
 begin perform public.respond_word_siege_invite_v1(current_setting('social_test.invite')::uuid,true);raise exception 'outsider_accept_allowed';
 exception when others then if sqlerrm<>'word_siege_not_invite_receiver' then raise;end if;end;
 if has_function_privilege('anon','public.get_my_invite_code_v1()','execute') or has_table_privilege('authenticated','public.social_activity','insert') or has_column_privilege('authenticated','public.social_activity','kind','update') then raise exception 'social_privilege_leak';end if;
 perform set_config('request.jwt.claims',jsonb_build_object('sub',c,'role','authenticated','is_anonymous',true)::text,true);
 if public.can_use_pro_friend_list_v1(c) then raise exception 'anonymous_social_allowed';end if;
 begin perform public.get_my_invite_code_v1();raise exception 'anonymous_code_allowed';
 exception when others then if sqlerrm<>'not_authenticated' then raise;end if;end;
end $outsider$;
reset role;
rollback;
select 'PASS: free social, stable invite code, nonfriend rematch/acceptance, persistent events, read markers, ownership RLS and anonymous rejection' as verification;
