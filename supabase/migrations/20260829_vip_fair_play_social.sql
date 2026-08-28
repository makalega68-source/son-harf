-- VIP fair-play guardrails and free friend messaging.
-- Paid membership must not grant match power, rating protection or competitive jokers.

update public.vip_joker_wallet
set freezer_count=0,
    swap_count=0,
    hint_count=0,
    streak_shield_count=0,
    updated_at=now();

create or replace function public.claim_vip_daily_jokers_v7()
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  return jsonb_build_object(
    'success',true,'disabled',true,'reason','fair_play',
    'freezer_count',0,'swap_count',0,'hint_count',0,'streak_shield_count',0
  );
end $$;
revoke all on function public.claim_vip_daily_jokers_v7() from public, anon;
grant execute on function public.claim_vip_daily_jokers_v7() to authenticated;

create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  return jsonb_build_object(
    'is_vip',coalesce(v_vip,false),
    'daily_jokers_claimed',true,
    'freezer_count',0,'swap_count',0,'hint_count',0,'streak_shield_count',0,
    'xp_multiplier',1,'diamond_multiplier',1,
    'rewarded_ad_bypass',coalesce(v_vip,false),
    'used_words_access',coalesce(v_vip,false),
    'direct_messages_access',true
  );
end $$;
revoke all on function public.get_vip_entitlements_v7() from public, anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated;

create or replace function public.claim_optional_reward_v7(
  p_reward_type text,
  p_ad_response_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_day date := (timezone('utc',now()))::date;
  v_used int;
  v_limit int;
  v_trial text;
  v_balance int;
  v_vip boolean;
  v_proof text;
  v_amount int;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_reward_type not in ('diamonds','chest','trial') then raise exception 'invalid_reward_type'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  v_limit := case p_reward_type when 'diamonds' then 3 when 'chest' then 2 else 1 end;
  select count(*) into v_used from public.rewarded_ad_claims
   where user_id=v_uid and reward_date=v_day and reward_type=p_reward_type;
  if v_used>=v_limit then raise exception 'daily_limit_reached'; end if;

  if v_vip then
    v_proof := format('vip:%s:%s:%s:%s',v_uid,p_reward_type,v_day,v_used+1);
  else
    if nullif(trim(p_ad_response_id),'') is null then raise exception 'missing_ad_proof'; end if;
    v_proof := trim(p_ad_response_id);
  end if;
  if exists(select 1 from public.rewarded_ad_claims where user_id=v_uid and ad_response_id=v_proof) then
    raise exception 'ad_already_claimed';
  end if;

  insert into public.reward_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  if p_reward_type='diamonds' then
    v_amount := 10;
    update public.profiles set diamonds=coalesce(diamonds,0)+v_amount
      where id=v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason)
      values(v_uid,v_amount,case when v_vip then 'vip_ad_free_reward' else 'rewarded_ad' end);
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,diamonds_awarded)
      values(v_uid,p_reward_type,v_proof,v_day,v_amount);
    return jsonb_build_object('success',true,'reward_type','diamonds','diamonds_awarded',v_amount,'diamonds',v_balance,'vip_bypass',v_vip);
  elsif p_reward_type='chest' then
    update public.reward_wallet set chest_keys=chest_keys+1,updated_at=now() where user_id=v_uid;
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,chest_keys_awarded)
      values(v_uid,p_reward_type,v_proof,v_day,1);
    return jsonb_build_object('success',true,'reward_type','chest','chest_keys_awarded',1,'vip_bypass',v_vip);
  else
    select id into v_trial from public.shop_items where active=true and vip_only=true order by random() limit 1;
    if v_trial is null then raise exception 'trial_item_unavailable'; end if;
    update public.reward_wallet set trial_item_id=v_trial,trial_expires_at=now()+interval '24 hours',updated_at=now()
      where user_id=v_uid;
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,trial_item_id)
      values(v_uid,p_reward_type,v_proof,v_day,v_trial);
    return jsonb_build_object('success',true,'reward_type','trial','trial_item_id',v_trial,'trial_expires_at',now()+interval '24 hours','vip_bypass',v_vip);
  end if;
end $$;
revoke all on function public.claim_optional_reward_v7(text,text) from public, anon;
grant execute on function public.claim_optional_reward_v7(text,text) to authenticated;

drop policy if exists direct_messages_insert_friends_vip on public.direct_messages;
drop policy if exists direct_messages_insert_friends on public.direct_messages;
create policy direct_messages_insert_friends
on public.direct_messages for insert to authenticated
with check (
  sender_id=(select auth.uid())
  and public.are_friends(sender_id,receiver_id)
  and char_length(btrim(body)) between 1 and 300
);

drop policy if exists direct_messages_select_friends on public.direct_messages;
create policy direct_messages_select_friends
on public.direct_messages for select to authenticated
using (
  ((select auth.uid())=sender_id or (select auth.uid())=receiver_id)
  and public.are_friends(sender_id,receiver_id)
);

drop policy if exists direct_messages_delete_own on public.direct_messages;
create policy direct_messages_delete_own
on public.direct_messages for delete to authenticated
using (sender_id=(select auth.uid()));
