-- Store runtime truth v3
-- Profile frames are globally retired. Preserve historical ownership, but never sell,
-- auto-equip or newly grant a retired frame through lifetime PRO.

begin;

update public.shop_items
set active = false
where kind = 'profile_frame' and active = true;

alter table public.shop_items
  drop constraint if exists shop_items_no_active_profile_frames_v3;

alter table public.shop_items
  add constraint shop_items_no_active_profile_frames_v3
  check (not (kind = 'profile_frame' and active = true));

-- Retirement is an active-runtime decision, not an ownership deletion.
-- Keep user_inventory and purchase history intact while clearing stale equipped state.
update public.user_equipped_cosmetics
set profile_frame_id = null,
    updated_at = now()
where profile_frame_id is not null;

update public.profiles
set default_profile_frame_id = null,
    updated_at = now()
where default_profile_frame_id is not null;

create or replace function public.apply_verified_premium_purchase_v1(
  p_user_id uuid,
  p_product_id text,
  p_purchase_token text,
  p_order_id text default null,
  p_play_state text default null,
  p_acknowledgement_state text default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_purchase_id uuid;
  v_purchase_user_id uuid;
  v_purchase_product_id text;
  v_inserted boolean := false;
  v_key text;
  v_balance integer;
begin
  if p_user_id is null or nullif(trim(p_purchase_token),'') is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if p_product_id not in ('series_game','letter_table','score_calculator','pro_lifetime') then
    raise exception 'unsupported_product';
  end if;
  if not exists(select 1 from public.profiles where id = p_user_id) then
    raise exception 'profile_not_found';
  end if;

  v_key := case p_product_id
    when 'series_game' then 'series_game'
    when 'letter_table' then 'letter_table'
    when 'score_calculator' then 'score_calculator'
    when 'pro_lifetime' then 'pro_lifetime'
  end;

  insert into public.purchases(
    user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at,
    purchase_type,play_state,acknowledgement_state,last_checked_at,expires_at
  ) values (
    p_user_id,p_product_id,trim(p_purchase_token),nullif(trim(p_order_id),''),'verified',now(),now(),
    'one_time',p_play_state,p_acknowledgement_state,now(),null
  )
  on conflict(purchase_token) do nothing
  returning id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id;
  v_inserted := found;

  if not v_inserted then
    select id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id
    from public.purchases
    where purchase_token = trim(p_purchase_token)
    for update;
    if v_purchase_id is null then raise exception 'purchase_reconciliation_race'; end if;
    if v_purchase_user_id <> p_user_id then raise exception 'purchase_token_user_mismatch'; end if;
    if v_purchase_product_id <> p_product_id then raise exception 'purchase_token_product_mismatch'; end if;
  end if;

  update public.purchases
  set order_id = coalesce(nullif(trim(p_order_id),''),order_id),
      status = 'verified',
      play_state = p_play_state,
      acknowledgement_state = coalesce(p_acknowledgement_state,acknowledgement_state),
      last_checked_at = now()
  where id = v_purchase_id;

  insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
  values(p_user_id,v_key,'play',trim(p_purchase_token),'active',null,now())
  on conflict(user_id,entitlement_key,source_type,source_id)
  do update set status='active',expires_at=null,updated_at=now();

  if p_product_id = 'pro_lifetime' then
    -- Lifetime PRO permanently owns the premium feature bundle. Retired profile-frame
    -- cosmetics are intentionally not granted or equipped here.
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
    select p_user_id,k,'pro_bundle',trim(p_purchase_token),'active',null,now()
    from unnest(array['series_game','letter_table','score_calculator']) k
    on conflict(user_id,entitlement_key,source_type,source_id)
    do update set status='active',expires_at=null,updated_at=now();

    update public.profiles
    set is_vip=true,updated_at=now()
    where id=p_user_id;

    if v_inserted then
      update public.profiles
      set diamonds=coalesce(diamonds,0)+100,updated_at=now()
      where id=p_user_id
      returning diamonds into v_balance;
      insert into public.diamond_ledger(user_id,delta,reason)
      values(p_user_id,100,'google_play_pro_lifetime:'||trim(p_purchase_token));
    end if;
  end if;

  if v_balance is null then select diamonds into v_balance from public.profiles where id=p_user_id; end if;
  return jsonb_build_object(
    'success',true,
    'already_processed',not v_inserted,
    'purchase_id',v_purchase_id,
    'product_id',p_product_id,
    'entitlement_key',v_key,
    'son_coin_granted',case when p_product_id='pro_lifetime' and v_inserted then 100 else 0 end,
    'son_coin_balance',v_balance
  );
end
$$;

revoke all on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) from public,anon,authenticated;
grant execute on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) to service_role;

commit;
