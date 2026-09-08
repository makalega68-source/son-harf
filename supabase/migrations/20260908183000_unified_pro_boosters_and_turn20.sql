-- SON HARF 1.0.0 UNIFIED PRO
-- Authoritative 20-second turns + Hint / Letter Swap / 2x Score boosters.
-- Existing canonical dictionary, rating, inventory and match state remain server-owned.

alter table public.profiles
  add column if not exists preferred_language text not null default 'tr';
alter table public.profiles drop constraint if exists profiles_preferred_language_check;
alter table public.profiles add constraint profiles_preferred_language_check
  check (preferred_language in ('tr','en'));

alter table public.shop_items
  add column if not exists available_from timestamptz not null default now(),
  add column if not exists available_until timestamptz,
  add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table public.shop_items drop constraint if exists shop_items_availability_window_check;
alter table public.shop_items add constraint shop_items_availability_window_check
  check (available_until is null or available_until > available_from);
create index if not exists idx_shop_items_sale_window
  on public.shop_items(active, available_from, available_until);

alter table public.user_inventory
  add column if not exists quantity integer not null default 1,
  add column if not exists is_equipped boolean not null default false;
alter table public.user_inventory drop constraint if exists user_inventory_item_id_fkey;
alter table public.user_inventory add constraint user_inventory_item_id_fkey
  foreign key (item_id) references public.shop_items(id) on delete restrict;

alter table public.vip_joker_wallet
  add column if not exists multiplier_count integer not null default 0;
alter table public.vip_joker_wallet drop constraint if exists vip_joker_wallet_multiplier_count_check;
alter table public.vip_joker_wallet add constraint vip_joker_wallet_multiplier_count_check
  check (multiplier_count >= 0);

-- Give existing PRO accounts the master-document starter wallet only when they
-- do not already have a wallet. Existing earned balances are never overwritten.
insert into public.vip_joker_wallet(user_id, freezer_count, swap_count, hint_count, streak_shield_count, multiplier_count)
select p.id, 0, 3, 5, 0, 2
from public.profiles p
where coalesce(p.is_vip,false)
on conflict (user_id) do nothing;

create table if not exists public.premier_booster_state (
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  turn_deadline timestamptz not null,
  required_override text,
  multiplier_armed boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (room_id,user_id),
  constraint premier_booster_override_length check (required_override is null or char_length(required_override) between 1 and 3)
);
alter table public.premier_booster_state enable row level security;
drop policy if exists premier_booster_state_read_own on public.premier_booster_state;
create policy premier_booster_state_read_own on public.premier_booster_state
for select to authenticated using (user_id = auth.uid());
revoke all on public.premier_booster_state from anon, authenticated;
grant select on public.premier_booster_state to authenticated;

create or replace function public.set_preferred_language_v1(p_language text)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare v_uid uuid:=auth.uid(); v_language text:=lower(trim(coalesce(p_language,'')));
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if v_language not in ('tr','en') then raise exception 'invalid_language'; end if;
  update public.profiles set preferred_language=v_language,updated_at=now() where id=v_uid;
  if not found then raise exception 'profile_not_found'; end if;
  return jsonb_build_object('success',true,'preferred_language',v_language);
end $$;
revoke all on function public.set_preferred_language_v1(text) from public,anon;
grant execute on function public.set_preferred_language_v1(text) to authenticated,service_role;

-- All Premier turns use one authoritative clock source.
create or replace function public.sonharf_turn_deadline(p_mode text)
returns timestamptz
language sql
stable
set search_path='public'
as $$ select clock_timestamp() + interval '20 seconds' $$;

create or replace function public.premier_effective_required_v1(
  p_room_id uuid,
  p_user_id uuid,
  p_default text,
  p_turn_deadline timestamptz
)
returns text
language sql
stable
security definer
set search_path='public','pg_temp'
as $$
  select coalesce(
    (
      select s.required_override
      from public.premier_booster_state s
      where s.room_id=p_room_id
        and s.user_id=p_user_id
        and s.turn_deadline=p_turn_deadline
        and s.required_override is not null
      limit 1
    ),
    p_default
  )
$$;
revoke all on function public.premier_effective_required_v1(uuid,uuid,text,timestamptz) from public,anon,authenticated;
grant execute on function public.premier_effective_required_v1(uuid,uuid,text,timestamptz) to service_role;

create or replace function public.premier_multiplier_armed_v1(
  p_room_id uuid,
  p_user_id uuid,
  p_turn_deadline timestamptz
)
returns boolean
language sql
stable
security definer
set search_path='public','pg_temp'
as $$
  select exists(
    select 1 from public.premier_booster_state s
    where s.room_id=p_room_id
      and s.user_id=p_user_id
      and s.turn_deadline=p_turn_deadline
      and s.multiplier_armed
  )
$$;
revoke all on function public.premier_multiplier_armed_v1(uuid,uuid,timestamptz) from public,anon,authenticated;
grant execute on function public.premier_multiplier_armed_v1(uuid,uuid,timestamptz) to service_role;

create or replace function public.get_premier_booster_status_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid(); r public.game_rooms; w public.vip_joker_wallet; s public.premier_booster_state;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into r from public.game_rooms where id=p_room_id;
  if r.id is null or v_uid not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  select * into w from public.vip_joker_wallet where user_id=v_uid;
  delete from public.premier_booster_state where room_id=p_room_id and user_id=v_uid and turn_deadline is distinct from r.turn_deadline;
  select * into s from public.premier_booster_state where room_id=p_room_id and user_id=v_uid;
  return jsonb_build_object(
    'hint_count',coalesce(w.hint_count,0),
    'swap_count',coalesce(w.swap_count,0),
    'multiplier_count',coalesce(w.multiplier_count,0),
    'required_override',s.required_override,
    'multiplier_armed',coalesce(s.multiplier_armed,false),
    'my_turn',r.current_player_id=v_uid and not coalesce(r.bot_turn,false),
    'turn_deadline',r.turn_deadline
  );
end $$;
revoke all on function public.get_premier_booster_status_v1(uuid) from public,anon;
grant execute on function public.get_premier_booster_status_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_hint_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid(); r public.game_rooms; previous_word text; required_token text; candidate text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or v_uid not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') or r.current_player_id<>v_uid or coalesce(r.bot_turn,false) then raise exception 'not_your_turn'; end if;
  if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.vip_joker_wallet set hint_count=hint_count-1,updated_at=now()
  where user_id=v_uid and hint_count>0;
  if not found then raise exception 'no_hint_tokens'; end if;

  select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
  required_token:=case
    when previous_word is null then null
    when r.game_mode='expert' then right(previous_word,least(3,greatest(1,r.round_no)))
    else right(previous_word,1)
  end;
  required_token:=public.premier_effective_required_v1(r.id,v_uid,required_token,r.turn_deadline);

  select d.normalized_word into candidate
  from public.dictionary_words d
  where d.language=r.language and d.active and coalesce(d.game_allowed,true)
    and not coalesce(d.is_abbreviation,false) and not coalesce(d.is_proper_noun,false)
    and char_length(d.normalized_word) between 2 and 15
    and (required_token is null or left(d.normalized_word,char_length(required_token))=required_token)
    and not exists(select 1 from public.game_words gw where gw.room_id=r.id and gw.normalized_word=d.normalized_word)
  order by abs(char_length(d.normalized_word)-6),random()
  limit 1;
  if candidate is null then raise exception 'hint_not_available'; end if;

  return jsonb_build_object(
    'success',true,
    'whisper',left(candidate,least(3,char_length(candidate))),
    'word_length',char_length(candidate),
    'remaining',(select hint_count from public.vip_joker_wallet where user_id=v_uid)
  );
end $$;
revoke all on function public.use_premier_hint_v1(uuid) from public,anon;
grant execute on function public.use_premier_hint_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_swap_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid(); r public.game_rooms; previous_word text; old_token text; new_token text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or v_uid not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') or r.current_player_id<>v_uid or coalesce(r.bot_turn,false) then raise exception 'not_your_turn'; end if;
  if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
  select normalized_word into previous_word from public.game_words where room_id=r.id order by id desc limit 1;
  if previous_word is null then raise exception 'swap_not_needed_for_opening'; end if;
  old_token:=case when r.game_mode='expert' then right(previous_word,least(3,greatest(1,r.round_no))) else right(previous_word,1) end;

  select left(d.normalized_word,1) into new_token
  from public.dictionary_words d
  where d.language=r.language and d.active and coalesce(d.game_allowed,true)
    and not coalesce(d.is_abbreviation,false) and not coalesce(d.is_proper_noun,false)
    and char_length(d.normalized_word) between 2 and 15
    and left(d.normalized_word,1)<>left(old_token,1)
    and not exists(select 1 from public.game_words gw where gw.room_id=r.id and gw.normalized_word=d.normalized_word)
  group by left(d.normalized_word,1)
  order by random()
  limit 1;
  if new_token is null then raise exception 'swap_not_available'; end if;

  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.vip_joker_wallet set swap_count=swap_count-1,updated_at=now()
  where user_id=v_uid and swap_count>0;
  if not found then raise exception 'no_swap_tokens'; end if;

  insert into public.premier_booster_state(room_id,user_id,turn_deadline,required_override,multiplier_armed,updated_at)
  values(r.id,v_uid,r.turn_deadline,new_token,false,now())
  on conflict(room_id,user_id) do update
    set turn_deadline=excluded.turn_deadline,
        required_override=excluded.required_override,
        multiplier_armed=case when public.premier_booster_state.turn_deadline=excluded.turn_deadline then public.premier_booster_state.multiplier_armed else false end,
        updated_at=now();

  return jsonb_build_object('success',true,'required_override',new_token,'remaining',(select swap_count from public.vip_joker_wallet where user_id=v_uid));
end $$;
revoke all on function public.use_premier_swap_v1(uuid) from public,anon;
grant execute on function public.use_premier_swap_v1(uuid) to authenticated,service_role;

create or replace function public.use_premier_multiplier_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare v_uid uuid:=auth.uid(); r public.game_rooms; already_armed boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null or v_uid not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
  if r.status not in ('playing','final','sudden_death') or r.current_player_id<>v_uid or coalesce(r.bot_turn,false) then raise exception 'not_your_turn'; end if;
  if r.turn_deadline is null or r.turn_deadline<=now() then raise exception 'turn_expired'; end if;
  select exists(select 1 from public.premier_booster_state s where s.room_id=r.id and s.user_id=v_uid and s.turn_deadline=r.turn_deadline and s.multiplier_armed) into already_armed;
  if already_armed then raise exception 'multiplier_already_armed'; end if;
  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.vip_joker_wallet set multiplier_count=multiplier_count-1,updated_at=now()
  where user_id=v_uid and multiplier_count>0;
  if not found then raise exception 'no_multiplier_tokens'; end if;

  insert into public.premier_booster_state(room_id,user_id,turn_deadline,required_override,multiplier_armed,updated_at)
  values(r.id,v_uid,r.turn_deadline,null,true,now())
  on conflict(room_id,user_id) do update
    set turn_deadline=excluded.turn_deadline,
        required_override=case when public.premier_booster_state.turn_deadline=excluded.turn_deadline then public.premier_booster_state.required_override else null end,
        multiplier_armed=true,
        updated_at=now();
  return jsonb_build_object('success',true,'multiplier_armed',true,'remaining',(select multiplier_count from public.vip_joker_wallet where user_id=v_uid));
end $$;
revoke all on function public.use_premier_multiplier_v1(uuid) from public,anon;
grant execute on function public.use_premier_multiplier_v1(uuid) to authenticated,service_role;

-- Daily PRO pack: the three master-document consumables. This keeps direct table
-- writes closed; only the server can grant them.
create or replace function public.claim_vip_daily_jokers_v7()
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid:=auth.uid(); v_vip boolean:=false; v_new_claim boolean:=false; w public.vip_joker_wallet;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  if not coalesce(v_vip,false) then raise exception 'not_vip'; end if;
  insert into public.vip_daily_joker_claims(user_id,claim_date) values(v_uid,current_date)
  on conflict(user_id,claim_date) do nothing returning true into v_new_claim;
  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  if coalesce(v_new_claim,false) then
    update public.vip_joker_wallet
      set hint_count=hint_count+1,swap_count=swap_count+1,multiplier_count=multiplier_count+1,updated_at=now()
      where user_id=v_uid;
  end if;
  select * into w from public.vip_joker_wallet where user_id=v_uid;
  return jsonb_build_object(
    'success',true,'already_claimed',not coalesce(v_new_claim,false),
    'freezer_count',w.freezer_count,'swap_count',w.swap_count,'hint_count',w.hint_count,
    'streak_shield_count',w.streak_shield_count,'multiplier_count',w.multiplier_count
  );
end $$;
revoke all on function public.claim_vip_daily_jokers_v7() from public,anon;
grant execute on function public.claim_vip_daily_jokers_v7() to authenticated,service_role;

create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare v_uid uuid:=auth.uid(); v_vip boolean:=false; v_claimed boolean:=false; w public.vip_joker_wallet;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  insert into public.vip_joker_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  select * into w from public.vip_joker_wallet where user_id=v_uid;
  select exists(select 1 from public.vip_daily_joker_claims where user_id=v_uid and claim_date=current_date) into v_claimed;
  return jsonb_build_object(
    'is_vip',v_vip,'daily_jokers_claimed',v_claimed,
    'freezer_count',w.freezer_count,'swap_count',w.swap_count,'hint_count',w.hint_count,
    'streak_shield_count',w.streak_shield_count,'multiplier_count',w.multiplier_count,
    'xp_multiplier',1,'diamond_multiplier',1,'rewarded_ad_bypass',false,
    'used_words_access',true,'direct_messages_access',true,'ranked_live_assist',true,
    'post_match_analysis',v_vip,'saved_friend_list',v_vip,'private_rooms',v_vip
  );
end $$;
revoke all on function public.get_vip_entitlements_v7() from public,anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated,service_role;

-- Patch the installed scoring functions in place so recent live hardening is
-- preserved. Fail closed if their reviewed anchors no longer match.
do $patch$
declare src text;
begin
  src:=pg_get_functiondef('public.submit_word_normal_v3(uuid,text)'::regprocedure);
  if position('public.premier_effective_required_v1' in src)=0 then
    if position('expected_first:=right(previous_word,1);' in src)=0
       or position('if streak_value%5=0 then add_points:=6; end if;' in src)=0
       or position('next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;' in src)=0 then
      raise exception 'submit_word_normal_v3_unreviewed';
    end if;
    src:=replace(src,'expected_first:=right(previous_word,1);','expected_first:=public.premier_effective_required_v1(r.id,auth.uid(),right(previous_word,1),r.turn_deadline);');
    src:=replace(src,'if streak_value%5=0 then add_points:=6; end if;','if streak_value%5=0 then add_points:=6; end if; if public.premier_multiplier_armed_v1(r.id,auth.uid(),r.turn_deadline) then add_points:=add_points*2; end if;');
    src:=replace(src,'next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;','delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid(); next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;');
    execute src;
  end if;

  src:=pg_get_functiondef('public.submit_word_expert_v1(uuid,text)'::regprocedure);
  if position('public.premier_effective_required_v1' in src)=0 then
    if position('expected:=right(previous_word,least(3,greatest(1,r.round_no)));' in src)=0
       or position('add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end;' in src)=0
       or position('next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;' in src)=0 then
      raise exception 'submit_word_expert_v1_unreviewed';
    end if;
    src:=replace(src,'expected:=right(previous_word,least(3,greatest(1,r.round_no)));','expected:=public.premier_effective_required_v1(r.id,auth.uid(),right(previous_word,least(3,greatest(1,r.round_no))),r.turn_deadline);');
    src:=replace(src,'add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end;','add_points:=3*multiplier + case when streak_value%5=0 then 3 else 0 end; if public.premier_multiplier_armed_v1(r.id,auth.uid(),r.turn_deadline) then add_points:=add_points*2; end if;');
    src:=replace(src,'next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;','delete from public.premier_booster_state where room_id=r.id and user_id=auth.uid(); next_player:=case when auth.uid()=r.host_id then r.guest_id else r.host_id end;');
    execute src;
  end if;
end $patch$;

-- Remove the remaining legacy 45-second turn assignments without replacing
-- whole functions. The callable behaviour stays otherwise byte-for-byte current.
do $turn20$
declare sig regprocedure; src text; patched text;
begin
  foreach sig in array array[
    'public.submit_word_normal_v3(uuid,text)'::regprocedure,
    'public.bot_take_turn_normal_v1(uuid)'::regprocedure,
    'public.claim_turn_timeout_v2(uuid,uuid,timestamptz)'::regprocedure,
    'public.join_room_by_code(text)'::regprocedure,
    'public.request_rematch_legacy_v1(uuid)'::regprocedure,
    'public.respond_game_invite(uuid,boolean)'::regprocedure
  ] loop
    src:=pg_get_functiondef(sig);
    patched:=replace(src, $$interval '45 seconds'$$, $$interval '20 seconds'$$);
    if src<>patched then execute patched; end if;
  end loop;
end $turn20$;

-- Product-approved matchmaking fallback is 15 seconds before the adaptive bot.
do $fallback$
declare src text; patched text;
begin
  src:=pg_get_functiondef('public.poll_random_matchmaking_v2()'::regprocedure);
  patched:=replace(src,$$q.queued_at<=now()-interval '10 seconds'$$,$$q.queued_at<=now()-interval '15 seconds'$$);
  if position($$q.queued_at<=now()-interval '15 seconds'$$ in patched)=0 then raise exception 'poll_random_matchmaking_v2_unreviewed'; end if;
  if src<>patched then execute patched; end if;
end $fallback$;

-- Retired catalog rows remain visible to owners, but only in-window active rows
-- are saleable through the catalog policy.
drop policy if exists shop_items_read on public.shop_items;
create policy shop_items_read on public.shop_items for select to authenticated
using (active and available_from<=now() and (available_until is null or available_until>now()));

-- Preserve owner access to retired purchased items.
drop policy if exists shop_items_owned_read on public.shop_items;
create policy shop_items_owned_read on public.shop_items for select to authenticated
using (exists(select 1 from public.user_inventory i where i.user_id=auth.uid() and i.item_id=shop_items.id));

select pg_notify('pgrst','reload schema');
