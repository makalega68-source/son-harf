-- Unified Pro live alignment: 20s authoritative turns, server-side boosters, dynamic catalog, permanent inventory.

alter table public.profiles add column if not exists preferred_language text not null default 'tr';
alter table public.profiles drop constraint if exists profiles_preferred_language_check;
alter table public.profiles add constraint profiles_preferred_language_check check (preferred_language in ('tr','en'));

alter table public.shop_items
  add column if not exists available_from timestamptz not null default now(),
  add column if not exists available_until timestamptz,
  add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table public.shop_items drop constraint if exists shop_items_availability_window_check;
alter table public.shop_items add constraint shop_items_availability_window_check check (available_until is null or available_until > available_from);
create index if not exists idx_shop_items_sale_window on public.shop_items(active,available_from,available_until);

alter table public.user_inventory
  add column if not exists quantity integer not null default 1,
  add column if not exists is_equipped boolean not null default false;
alter table public.user_inventory drop constraint if exists user_inventory_item_id_fkey;
alter table public.user_inventory add constraint user_inventory_item_id_fkey foreign key(item_id) references public.shop_items(id) on delete restrict;
create index if not exists user_inventory_item_idx on public.user_inventory(item_id);

alter table public.vip_joker_wallet add column if not exists multiplier_count integer not null default 2;
alter table public.vip_joker_wallet alter column hint_count set default 5;
alter table public.vip_joker_wallet alter column swap_count set default 3;
alter table public.vip_joker_wallet alter column multiplier_count set default 2;
alter table public.vip_joker_wallet drop constraint if exists vip_joker_wallet_multiplier_count_check;
alter table public.vip_joker_wallet add constraint vip_joker_wallet_multiplier_count_check check (multiplier_count >= 0);
insert into public.vip_joker_wallet(user_id,freezer_count,swap_count,hint_count,streak_shield_count,multiplier_count,updated_at)
select id,0,3,5,0,2,now() from public.profiles
on conflict(user_id) do nothing;

create table if not exists public.premier_booster_state(
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  turn_deadline timestamptz not null,
  required_override text,
  multiplier_armed boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key(room_id,user_id)
);
alter table public.premier_booster_state enable row level security;
drop policy if exists premier_booster_state_read_own on public.premier_booster_state;
create policy premier_booster_state_read_own on public.premier_booster_state for select to authenticated using (user_id=(select auth.uid()));

create or replace function public.set_preferred_language_v1(p_language text)
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); lang text:=lower(trim(coalesce(p_language,'')));
begin
 if u is null then raise exception 'unauthorized'; end if;
 if lang not in ('tr','en') then raise exception 'invalid_language'; end if;
 update public.profiles set preferred_language=lang,updated_at=now() where id=u;
 if not found then raise exception 'profile_not_found'; end if;
 return jsonb_build_object('success',true,'preferred_language',lang);
end $$;
revoke all on function public.set_preferred_language_v1(text) from public,anon;
grant execute on function public.set_preferred_language_v1(text) to authenticated,service_role;

create or replace function public.sonharf_turn_deadline(p_mode text)
returns timestamptz language sql volatile set search_path='public' as $$ select clock_timestamp()+interval '20 seconds' $$;

create or replace function public.premier_effective_required_v1(p_room_id uuid,p_user_id uuid,p_default text)
returns text language sql stable security definer set search_path='public','pg_temp' as $$
 select coalesce((select required_override from public.premier_booster_state where room_id=p_room_id and user_id=p_user_id and turn_deadline=(select turn_deadline from public.game_rooms where id=p_room_id)),p_default)
$$;
revoke all on function public.premier_effective_required_v1(uuid,uuid,text) from public,anon,authenticated;
grant execute on function public.premier_effective_required_v1(uuid,uuid,text) to service_role;

create or replace function public.premier_multiplier_armed_v1(p_room_id uuid,p_user_id uuid)
returns boolean language sql stable security definer set search_path='public','pg_temp' as $$
 select coalesce((select multiplier_armed from public.premier_booster_state where room_id=p_room_id and user_id=p_user_id and turn_deadline=(select turn_deadline from public.game_rooms where id=p_room_id)),false)
$$;
revoke all on function public.premier_multiplier_armed_v1(uuid,uuid) from public,anon,authenticated;
grant execute on function public.premier_multiplier_armed_v1(uuid,uuid) to service_role;

create or replace function public.get_premier_booster_status_v1(p_room_id uuid)
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); r public.game_rooms; w public.vip_joker_wallet; st public.premier_booster_state;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select * into r from public.game_rooms where id=p_room_id;
 if r.id is null or u not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 select * into w from public.vip_joker_wallet where user_id=u;
 delete from public.premier_booster_state where room_id=r.id and user_id=u and turn_deadline is distinct from r.turn_deadline;
 select * into st from public.premier_booster_state where room_id=r.id and user_id=u;
 return jsonb_build_object('hint_count',coalesce(w.hint_count,0),'swap_count',coalesce(w.swap_count,0),'multiplier_count',coalesce(w.multiplier_count,0),'required_override',st.required_override,'multiplier_armed',coalesce(st.multiplier_armed,false),'my_turn',(r.current_player_id=u and not r.bot_turn and r.status in ('playing','final','sudden_death')),'turn_deadline',r.turn_deadline);
end $$;
revoke all on function public.get_premier_booster_status_v1(uuid) from public,anon;
grant execute on function public.get_premier_booster_status_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_hint_v1(p_room_id uuid)
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); r public.game_rooms; last_word text; req text; candidate text; remain int;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null or u not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
 if r.status not in ('playing','final','sudden_death') or r.current_player_id<>u or r.bot_turn then raise exception 'not_your_turn'; end if;
 if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 update public.vip_joker_wallet set hint_count=hint_count-1,updated_at=now() where user_id=u and hint_count>0 returning hint_count into remain;
 if remain is null then raise exception 'no_hint_tokens'; end if;
 select normalized_word into last_word from public.game_words where room_id=r.id order by id desc limit 1;
 req:=case when last_word is null then null else right(last_word,case when r.game_mode='expert' then least(3,greatest(1,r.round_no)) else 1 end) end;
 req:=public.premier_effective_required_v1(r.id,u,req);
 select d.normalized_word into candidate from public.dictionary_words d where d.language=r.language and d.active and coalesce(d.game_allowed,true) and not coalesce(d.is_abbreviation,false) and not coalesce(d.is_proper_noun,false) and (req is null or left(d.normalized_word,char_length(req))=req) and not exists(select 1 from public.game_words gw where gw.room_id=r.id and gw.normalized_word=d.normalized_word) order by abs(char_length(d.normalized_word)-6),random() limit 1;
 if candidate is null then raise exception 'no_hint_candidate'; end if;
 return jsonb_build_object('success',true,'whisper',left(candidate,least(3,char_length(candidate))),'word_length',char_length(candidate),'remaining',remain);
end $$;
revoke all on function public.use_premier_hint_v1(uuid) from public,anon;
grant execute on function public.use_premier_hint_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_swap_v1(p_room_id uuid)
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); r public.game_rooms; last_word text; current_req text; new_req text; remain int;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null or u not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
 if r.status not in ('playing','final','sudden_death') or r.current_player_id<>u or r.bot_turn then raise exception 'not_your_turn'; end if;
 if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
 select normalized_word into last_word from public.game_words where room_id=r.id order by id desc limit 1;
 if last_word is null then raise exception 'swap_not_needed_for_opening'; end if;
 current_req:=right(last_word,case when r.game_mode='expert' then least(3,greatest(1,r.round_no)) else 1 end);
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 update public.vip_joker_wallet set swap_count=swap_count-1,updated_at=now() where user_id=u and swap_count>0 returning swap_count into remain;
 if remain is null then raise exception 'no_swap_tokens'; end if;
 select left(d.normalized_word,case when r.game_mode='expert' then least(3,greatest(1,r.round_no)) else 1 end) into new_req from public.dictionary_words d where d.language=r.language and d.active and coalesce(d.game_allowed,true) and not coalesce(d.is_abbreviation,false) and not coalesce(d.is_proper_noun,false) and left(d.normalized_word,case when r.game_mode='expert' then least(3,greatest(1,r.round_no)) else 1 end)<>current_req group by 1 order by random() limit 1;
 if new_req is null then raise exception 'no_swap_candidate'; end if;
 insert into public.premier_booster_state(room_id,user_id,turn_deadline,required_override,multiplier_armed,updated_at) values(r.id,u,r.turn_deadline,new_req,false,now()) on conflict(room_id,user_id) do update set turn_deadline=excluded.turn_deadline,required_override=excluded.required_override,multiplier_armed=false,updated_at=now();
 return jsonb_build_object('success',true,'required_override',new_req,'remaining',remain);
end $$;
revoke all on function public.use_premier_swap_v1(uuid) from public,anon;
grant execute on function public.use_premier_swap_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_multiplier_v1(p_room_id uuid)
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); r public.game_rooms; remain int; already boolean:=false;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select * into r from public.game_rooms where id=p_room_id for update;
 if r.id is null or u not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
 if r.status not in ('playing','final','sudden_death') or r.current_player_id<>u or r.bot_turn then raise exception 'not_your_turn'; end if;
 if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
 select coalesce(multiplier_armed,false) into already from public.premier_booster_state where room_id=r.id and user_id=u and turn_deadline=r.turn_deadline;
 if already then raise exception 'multiplier_already_armed'; end if;
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 update public.vip_joker_wallet set multiplier_count=multiplier_count-1,updated_at=now() where user_id=u and multiplier_count>0 returning multiplier_count into remain;
 if remain is null then raise exception 'no_multiplier_tokens'; end if;
 insert into public.premier_booster_state(room_id,user_id,turn_deadline,multiplier_armed,updated_at) values(r.id,u,r.turn_deadline,true,now()) on conflict(room_id,user_id) do update set turn_deadline=excluded.turn_deadline,multiplier_armed=true,updated_at=now();
 return jsonb_build_object('success',true,'multiplier_armed',true,'remaining',remain);
end $$;
revoke all on function public.use_premier_multiplier_v1(uuid) from public,anon;
grant execute on function public.use_premier_multiplier_v1(uuid) to authenticated,service_role;

do $patch$
declare f text;
begin
 f:=pg_get_functiondef('public.submit_word_normal_v3(uuid,text)'::regprocedure);
 if position('premier_effective_required_v1' in f)=0 then
   if position('expected_first:=right(previous_word,1);' in f)=0 then raise exception 'normal_required_hook_missing'; end if;
   f:=replace(f,'expected_first:=right(previous_word,1);','expected_first:=public.premier_effective_required_v1(r.id,auth.uid(),right(previous_word,1));');
 end if;
 if position('premier_multiplier_armed_v1' in f)=0 then
   f:=replace(f,'if streak_value%5=0 then add_points:=6; end if;','if streak_value%5=0 then add_points:=6; end if; if public.premier_multiplier_armed_v1(r.id,auth.uid()) then add_points:=add_points*2; end if;');
   if position('premier_multiplier_armed_v1' in f)=0 then raise exception 'normal_multiplier_hook_missing'; end if;
 end if;
 if position('delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid();' in f)=0 then
   f:=replace(f,'next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;','delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid(); next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;');
 end if;
 f:=replace(f,$q$interval '45 seconds'$q$,$q$interval '20 seconds'$q$);
 execute f;

 f:=pg_get_functiondef('public.submit_word_expert_v1(uuid,text)'::regprocedure);
 if position('premier_effective_required_v1' in f)=0 then
   if position('expected:=right(previous_word,least(3,greatest(1,r.round_no)));' in f)=0 then raise exception 'expert_required_hook_missing'; end if;
   f:=replace(f,'expected:=right(previous_word,least(3,greatest(1,r.round_no)));','expected:=public.premier_effective_required_v1(r.id,auth.uid(),right(previous_word,least(3,greatest(1,r.round_no))));');
 end if;
 if position('premier_multiplier_armed_v1' in f)=0 then
   if position('add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end;' in f)=0 then raise exception 'expert_multiplier_hook_missing'; end if;
   f:=replace(f,'add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end;','add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end; if public.premier_multiplier_armed_v1(r.id,auth.uid()) then add_points:=add_points*2; end if;');
 end if;
 if position('delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid();' in f)=0 then
   f:=replace(f,'next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;','delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid(); next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;');
 end if;
 f:=replace(f,$q$interval '45 seconds'$q$,$q$interval '20 seconds'$q$);
 execute f;
end $patch$;

do $timers$
declare sig regprocedure; f text; arr text[]:=array['public.bot_take_turn_normal_v1(uuid)','public.claim_turn_timeout_v2(uuid,uuid,timestamptz)','public.join_room_by_code(text)','public.request_rematch_legacy_v1(uuid)','public.respond_game_invite(uuid,boolean)','public.restart_bot_match(uuid)','public.restart_bot_match_v2(uuid)']; x text;
begin
 foreach x in array arr loop
   begin
     sig:=x::regprocedure; f:=pg_get_functiondef(sig); f:=replace(f,$q$interval '45 seconds'$q$,$q$interval '20 seconds'$q$); execute f;
   exception when undefined_function then null;
   end;
 end loop;
 f:=pg_get_functiondef('public.poll_random_matchmaking_v2()'::regprocedure);
 f:=replace(f,$q$interval '10 seconds'$q$,$q$interval '15 seconds'$q$);
 execute f;
end $timers$;

create or replace function public.claim_vip_daily_jokers_v7()
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); vip boolean:=false; fresh boolean:=false; w public.vip_joker_wallet;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select coalesce(is_vip,false) into vip from public.profiles where id=u;
 if not vip then raise exception 'not_vip'; end if;
 insert into public.vip_daily_joker_claims(user_id,claim_date) values(u,current_date) on conflict(user_id,claim_date) do nothing returning true into fresh;
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 if coalesce(fresh,false) then update public.vip_joker_wallet set hint_count=hint_count+1,swap_count=swap_count+1,multiplier_count=multiplier_count+1,updated_at=now() where user_id=u; end if;
 select * into w from public.vip_joker_wallet where user_id=u;
 return jsonb_build_object('success',true,'already_claimed',not coalesce(fresh,false),'freezer_count',coalesce(w.freezer_count,0),'swap_count',coalesce(w.swap_count,0),'hint_count',coalesce(w.hint_count,0),'streak_shield_count',coalesce(w.streak_shield_count,0),'multiplier_count',coalesce(w.multiplier_count,0));
end $$;
revoke all on function public.claim_vip_daily_jokers_v7() from public,anon;
grant execute on function public.claim_vip_daily_jokers_v7() to authenticated,service_role;

create or replace function public.get_vip_entitlements_v7()
returns jsonb language plpgsql security definer set search_path='public','pg_temp' as $$
declare u uuid:=auth.uid(); vip boolean:=false; claimed boolean:=false; w public.vip_joker_wallet;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select coalesce(is_vip,false) into vip from public.profiles where id=u;
 insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
 select * into w from public.vip_joker_wallet where user_id=u;
 select exists(select 1 from public.vip_daily_joker_claims where user_id=u and claim_date=current_date) into claimed;
 return jsonb_build_object('is_vip',vip,'daily_jokers_claimed',claimed,'freezer_count',coalesce(w.freezer_count,0),'swap_count',coalesce(w.swap_count,0),'hint_count',coalesce(w.hint_count,0),'streak_shield_count',coalesce(w.streak_shield_count,0),'multiplier_count',coalesce(w.multiplier_count,0),'xp_multiplier',1,'diamond_multiplier',1,'rewarded_ad_bypass',false,'used_words_access',true,'direct_messages_access',true,'ranked_live_assist',true,'post_match_analysis',vip,'saved_friend_list',vip,'private_rooms',vip);
end $$;
revoke all on function public.get_vip_entitlements_v7() from public,anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated,service_role;

drop policy if exists shop_items_read on public.shop_items;
create policy shop_items_read on public.shop_items for select to authenticated using (active=true and available_from<=now() and (available_until is null or available_until>now()));
drop policy if exists shop_items_owned_read on public.shop_items;
create policy shop_items_owned_read on public.shop_items for select to authenticated using (exists(select 1 from public.user_inventory i where i.user_id=(select auth.uid()) and i.item_id=shop_items.id));

select pg_notify('pgrst','reload schema');
