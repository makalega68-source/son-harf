-- Source parity for migration already applied to production on 2026-09-04.
-- Keep this file in git so a fresh environment can reproduce the live purchase authority.

update public.store_catalog_config
set enabled=false, updated_at=now()
where product_id in ('premium_style_pack','season_pack','vip_welcome_pack');

create table if not exists public.store_product_grants (
  product_id text not null,
  grant_type text not null check (grant_type in ('son_coin','style','season_pass','vip_welcome_marker')),
  grant_key text not null default '',
  amount integer not null default 0 check (amount>=0),
  primary key(product_id,grant_type,grant_key)
);
alter table public.store_product_grants enable row level security;
revoke all on public.store_product_grants from anon,authenticated;
grant select,insert,update,delete on public.store_product_grants to service_role;
insert into public.store_product_grants(product_id,grant_type,grant_key,amount) values
 ('starter_style_pack','son_coin','',800),('starter_style_pack','style','frame_starter',0)
on conflict(product_id,grant_type,grant_key) do update set amount=excluded.amount;

create or replace function public.apply_verified_play_purchase_v2(
  p_user_id uuid,p_product_id text,p_purchase_token text,p_order_id text default null,
  p_expires_at timestamptz default null,p_play_state text default null,p_acknowledgement_state text default null
)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
  v_purchase_id uuid; v_purchase_user_id uuid; v_purchase_product_id text;
  v_inserted boolean:=false; v_delta integer:=0; v_balance integer; v_grant record;
  v_status text:='verified'; v_entitlement_status text:='active'; v_is_vip boolean:=false; v_is_season boolean:=false;
begin
  if p_user_id is null or nullif(trim(p_purchase_token),'') is null or length(trim(p_purchase_token))<8 then raise exception 'invalid_purchase'; end if;
  if nullif(trim(p_product_id),'') is null then raise exception 'invalid_product'; end if;
  if not exists(select 1 from public.profiles where id=p_user_id) then raise exception 'profile_not_found'; end if;
  v_is_vip:=p_product_id in ('vip_monthly','vip_yearly');
  v_is_season:=p_product_id in ('season_pass','season_pass_monthly');
  if p_play_state='SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then v_entitlement_status:='grace';
  elsif p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD','SUBSCRIPTION_STATE_PAUSED') then v_entitlement_status:='hold';
  elsif p_play_state='SUBSCRIPTION_STATE_CANCELED' then v_entitlement_status:=case when p_expires_at is not null and p_expires_at>now() then 'canceled' else 'expired' end;
  elsif p_play_state='SUBSCRIPTION_STATE_EXPIRED' then v_entitlement_status:='expired';
  elsif p_play_state='SUBSCRIPTION_STATE_PENDING' then v_entitlement_status:='pending'; end if;

  insert into public.purchases(user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at,purchase_type,play_state,acknowledgement_state,last_checked_at,expires_at)
  values(p_user_id,p_product_id,trim(p_purchase_token),nullif(trim(p_order_id),''),v_status,now(),now(),case when v_is_vip or v_is_season then 'subscription' else 'one_time' end,p_play_state,p_acknowledgement_state,now(),p_expires_at)
  on conflict(purchase_token) do nothing returning id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id;
  v_inserted:=found;
  if not v_inserted then
    select id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id from public.purchases where purchase_token=trim(p_purchase_token) for update;
    if v_purchase_id is null then raise exception 'purchase_reconciliation_race'; end if;
    if v_purchase_user_id<>p_user_id then raise exception 'purchase_token_user_mismatch'; end if;
    if v_purchase_product_id<>p_product_id then raise exception 'purchase_token_product_mismatch'; end if;
  end if;

  update public.purchases set order_id=coalesce(nullif(trim(p_order_id),''),order_id),status=v_status,play_state=p_play_state,acknowledgement_state=coalesce(p_acknowledgement_state,acknowledgement_state),last_checked_at=now(),expires_at=coalesce(p_expires_at,expires_at) where id=v_purchase_id;

  if v_is_vip then
    if p_expires_at is null then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.subscriptions(user_id,product_id,status,expires_at,updated_at) values(p_user_id,p_product_id,v_entitlement_status,p_expires_at,now())
    on conflict(user_id) do update set product_id=excluded.product_id,status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
    values(p_user_id,'vip','play',trim(p_purchase_token),v_entitlement_status,p_expires_at,now())
    on conflict(user_id,entitlement_key,source_type,source_id) do update set status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    update public.profiles set is_vip=(v_entitlement_status in ('active','grace','canceled') and p_expires_at>now()),updated_at=now() where id=p_user_id;
  elsif v_is_season then
    if p_expires_at is null then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.season_pass_entitlements(user_id,product_id,status,expires_at,updated_at) values(p_user_id,p_product_id,v_entitlement_status,p_expires_at,now())
    on conflict(user_id) do update set product_id=excluded.product_id,status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
    values(p_user_id,'season_pass','play',trim(p_purchase_token),v_entitlement_status,p_expires_at,now())
    on conflict(user_id,entitlement_key,source_type,source_id) do update set status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
  else
    if p_product_id='coins_500' then v_delta:=500;
    elsif p_product_id='coins_1500' then v_delta:=1500;
    elsif p_product_id='coins_3500' then v_delta:=3500;
    elsif p_product_id='coins_8000' then v_delta:=8000;
    elsif p_product_id='theme_neon' then
      if v_inserted then insert into public.user_inventory(user_id,item_id) values(p_user_id,'theme_neon') on conflict(user_id,item_id) do nothing; end if;
    elsif exists(select 1 from public.store_product_grants where product_id=p_product_id) then null;
    else raise exception 'unsupported_product'; end if;
    if v_inserted then
      for v_grant in select grant_type,grant_key,amount from public.store_product_grants where product_id=p_product_id loop
        if v_grant.grant_type='son_coin' then v_delta:=v_delta+v_grant.amount;
        elsif v_grant.grant_type='style' then insert into public.user_inventory(user_id,item_id) values(p_user_id,v_grant.grant_key) on conflict(user_id,item_id) do nothing; end if;
      end loop;
      if v_delta>0 then
        update public.profiles set diamonds=coalesce(diamonds,0)+v_delta,updated_at=now() where id=p_user_id returning diamonds into v_balance;
        insert into public.diamond_ledger(user_id,delta,reason) values(p_user_id,v_delta,'google_play_purchase:'||trim(p_purchase_token));
      end if;
    else select diamonds into v_balance from public.profiles where id=p_user_id; v_delta:=0; end if;
  end if;
  return jsonb_build_object('success',true,'already_processed',not v_inserted,'purchase_id',v_purchase_id,'product_id',p_product_id,'son_coin_granted',v_delta,'son_coin_balance',v_balance,'entitlement_status',case when v_is_vip or v_is_season then v_entitlement_status else null end,'expires_at',p_expires_at);
end $$;
revoke all on function public.apply_verified_play_purchase_v2(uuid,text,text,text,timestamptz,text,text) from public,anon,authenticated;
grant execute on function public.apply_verified_play_purchase_v2(uuid,text,text,text,timestamptz,text,text) to service_role;

create or replace function public.reconcile_play_entitlement_v1(p_purchase_token text,p_play_state text,p_expires_at timestamptz default null,p_revoke boolean default false)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v_purchase public.purchases%rowtype; v_status text; v_vip_active boolean;
begin
  select * into v_purchase from public.purchases where purchase_token=trim(p_purchase_token) for update;
  if not found then return jsonb_build_object('success',true,'known',false); end if;
  v_status:=case when p_revoke then 'revoked' when p_play_state='SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then 'grace' when p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD','SUBSCRIPTION_STATE_PAUSED') then 'hold' when p_play_state='SUBSCRIPTION_STATE_CANCELED' and coalesce(p_expires_at,v_purchase.expires_at)>now() then 'canceled' when p_play_state in ('SUBSCRIPTION_STATE_EXPIRED','SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED') then 'expired' when p_play_state='SUBSCRIPTION_STATE_ACTIVE' then 'active' else 'pending' end;
  update public.purchases set play_state=p_play_state,status=v_status,expires_at=coalesce(p_expires_at,expires_at),revoked_at=case when p_revoke then now() else revoked_at end,last_checked_at=now() where id=v_purchase.id;
  if v_purchase.product_id in ('vip_monthly','vip_yearly') then
    update public.subscriptions set status=v_status,expires_at=coalesce(p_expires_at,expires_at),updated_at=now() where user_id=v_purchase.user_id;
    update public.store_entitlements set status=v_status,expires_at=coalesce(p_expires_at,expires_at),updated_at=now() where user_id=v_purchase.user_id and entitlement_key='vip' and source_type='play' and source_id=trim(p_purchase_token);
    select exists(select 1 from public.subscriptions where user_id=v_purchase.user_id and status in ('active','grace','canceled') and expires_at>now()) into v_vip_active;
    update public.profiles set is_vip=v_vip_active,updated_at=now() where id=v_purchase.user_id;
  elsif v_purchase.product_id in ('season_pass','season_pass_monthly') then
    update public.season_pass_entitlements set status=v_status,expires_at=coalesce(p_expires_at,expires_at),updated_at=now() where user_id=v_purchase.user_id;
    update public.store_entitlements set status=v_status,expires_at=coalesce(p_expires_at,expires_at),updated_at=now() where user_id=v_purchase.user_id and entitlement_key='season_pass' and source_type='play' and source_id=trim(p_purchase_token);
  end if;
  return jsonb_build_object('success',true,'known',true,'status',v_status,'product_id',v_purchase.product_id);
end $$;
revoke all on function public.reconcile_play_entitlement_v1(text,text,timestamptz,boolean) from public,anon,authenticated;
grant execute on function public.reconcile_play_entitlement_v1(text,text,timestamptz,boolean) to service_role;
