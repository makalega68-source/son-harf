-- STAGING ONLY. Do not move into supabase/migrations until issue #340 staging gates pass.
-- Google Play grant provenance, constraint-safe subscription lifecycle, non-negative clawback,
-- and retry-safe RTDN dedupe.

create table if not exists public.play_purchase_grants (
  purchase_token text not null,
  user_id uuid not null references public.profiles(id) on delete cascade,
  product_id text not null,
  grant_type text not null check (grant_type in ('son_coin','style')),
  grant_key text not null default '',
  amount integer not null default 0 check (amount >= 0),
  owns_inventory boolean not null default false,
  granted_at timestamptz not null default now(),
  reversed_at timestamptz,
  reversed_amount integer not null default 0 check (reversed_amount >= 0),
  reversal_shortfall integer not null default 0 check (reversal_shortfall >= 0),
  primary key (purchase_token, grant_type, grant_key)
);

alter table public.play_purchase_grants enable row level security;
revoke all on public.play_purchase_grants from public, anon, authenticated;
grant select, insert, update, delete on public.play_purchase_grants to service_role;

create unique index if not exists diamond_ledger_play_reversal_unique
  on public.diamond_ledger(user_id, reason)
  where reason like 'google_play_reversal:%';

-- Preserve the live purchase-v2 API while recording grant provenance. Purchases.status has its
-- own vocabulary (pending/verified/rejected/refunded), store_entitlements has Play lifecycle
-- states, and the legacy subscriptions table uses British `cancelled` plus `inactive`.
create or replace function public.apply_verified_play_purchase_v2(
  p_user_id uuid,
  p_product_id text,
  p_purchase_token text,
  p_order_id text default null,
  p_expires_at timestamptz default null,
  p_play_state text default null,
  p_acknowledgement_state text default null
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_purchase_id uuid;
  v_purchase_user_id uuid;
  v_purchase_product_id text;
  v_inserted boolean := false;
  v_delta integer := 0;
  v_balance integer;
  v_grant record;
  v_entitlement_status text := 'active';
  v_subscription_status text := 'active';
  v_is_vip boolean := false;
  v_is_season boolean := false;
  v_style_inserted text;
  v_play_owns_style boolean := false;
begin
  if p_user_id is null or nullif(trim(p_purchase_token),'') is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if nullif(trim(p_product_id),'') is null then raise exception 'invalid_product'; end if;
  if not exists(select 1 from public.profiles where id=p_user_id) then raise exception 'profile_not_found'; end if;

  v_is_vip := p_product_id in ('vip_monthly','vip_yearly');
  v_is_season := p_product_id in ('season_pass','season_pass_monthly');

  if p_play_state='SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then v_entitlement_status:='grace';
  elsif p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD','SUBSCRIPTION_STATE_PAUSED') then v_entitlement_status:='hold';
  elsif p_play_state='SUBSCRIPTION_STATE_CANCELED' then
    v_entitlement_status:=case when p_expires_at is not null and p_expires_at>now() then 'canceled' else 'expired' end;
  elsif p_play_state in ('SUBSCRIPTION_STATE_EXPIRED','SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED') then v_entitlement_status:='expired';
  elsif p_play_state='SUBSCRIPTION_STATE_PENDING' then v_entitlement_status:='pending';
  else v_entitlement_status:='active';
  end if;

  v_subscription_status := case v_entitlement_status
    when 'active' then 'active'
    when 'grace' then 'grace'
    when 'canceled' then 'cancelled'
    when 'expired' then 'expired'
    else 'inactive'
  end;

  insert into public.purchases(
    user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at,purchase_type,
    play_state,acknowledgement_state,last_checked_at,expires_at
  ) values (
    p_user_id,p_product_id,trim(p_purchase_token),nullif(trim(p_order_id),''),'verified',now(),now(),
    case when v_is_vip or v_is_season then 'subscription' else 'one_time' end,
    p_play_state,p_acknowledgement_state,now(),p_expires_at
  )
  on conflict(purchase_token) do nothing
  returning id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id;
  v_inserted := found;

  if not v_inserted then
    select id,user_id,product_id
      into v_purchase_id,v_purchase_user_id,v_purchase_product_id
      from public.purchases
      where purchase_token=trim(p_purchase_token)
      for update;
    if v_purchase_id is null then raise exception 'purchase_reconciliation_race'; end if;
    if v_purchase_user_id<>p_user_id then raise exception 'purchase_token_user_mismatch'; end if;
    if v_purchase_product_id<>p_product_id then raise exception 'purchase_token_product_mismatch'; end if;
  end if;

  update public.purchases
    set order_id=coalesce(nullif(trim(p_order_id),''),order_id),
        status='verified',
        play_state=p_play_state,
        acknowledgement_state=coalesce(p_acknowledgement_state,acknowledgement_state),
        last_checked_at=now(),
        expires_at=coalesce(p_expires_at,expires_at),
        revoked_at=null
    where id=v_purchase_id;

  if v_is_vip then
    if p_expires_at is null then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.subscriptions(user_id,product_id,status,expires_at,updated_at)
      values(p_user_id,p_product_id,v_subscription_status,p_expires_at,now())
      on conflict(user_id) do update
      set product_id=excluded.product_id,status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
      values(p_user_id,'vip','play',trim(p_purchase_token),v_entitlement_status,p_expires_at,now())
      on conflict(user_id,entitlement_key,source_type,source_id) do update
      set status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    update public.profiles
      set is_vip=(v_entitlement_status in ('active','grace','canceled') and p_expires_at>now()),updated_at=now()
      where id=p_user_id;

  elsif v_is_season then
    if p_expires_at is null then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.season_pass_entitlements(user_id,product_id,status,expires_at,updated_at)
      values(p_user_id,p_product_id,v_entitlement_status,p_expires_at,now())
      on conflict(user_id) do update
      set product_id=excluded.product_id,status=excluded.status,expires_at=excluded.expires_at,updated_at=now();
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
      values(p_user_id,'season_pass','play',trim(p_purchase_token),v_entitlement_status,p_expires_at,now())
      on conflict(user_id,entitlement_key,source_type,source_id) do update
      set status=excluded.status,expires_at=excluded.expires_at,updated_at=now();

  else
    if p_product_id='coins_500' then v_delta:=500;
    elsif p_product_id='coins_1500' then v_delta:=1500;
    elsif p_product_id='coins_3500' then v_delta:=3500;
    elsif p_product_id='coins_8000' then v_delta:=8000;
    elsif p_product_id='theme_neon' then
      if v_inserted then
        v_style_inserted := null;
        insert into public.user_inventory(user_id,item_id)
          values(p_user_id,'theme_neon')
          on conflict(user_id,item_id) do nothing
          returning item_id into v_style_inserted;
        if v_style_inserted is not null then
          v_play_owns_style := true;
        else
          select exists(
            select 1 from public.play_purchase_grants pg
            where pg.user_id=p_user_id and pg.grant_type='style' and pg.grant_key='theme_neon'
              and pg.reversed_at is null and pg.owns_inventory
          ) into v_play_owns_style;
        end if;
        insert into public.play_purchase_grants(
          purchase_token,user_id,product_id,grant_type,grant_key,amount,owns_inventory
        ) values(trim(p_purchase_token),p_user_id,p_product_id,'style','theme_neon',0,v_play_owns_style)
        on conflict do nothing;
      end if;
    elsif exists(select 1 from public.store_product_grants where product_id=p_product_id) then
      null;
    else
      raise exception 'unsupported_product';
    end if;

    if v_inserted then
      for v_grant in
        select grant_type,grant_key,amount
        from public.store_product_grants
        where product_id=p_product_id
      loop
        if v_grant.grant_type='son_coin' then
          v_delta:=v_delta+v_grant.amount;
        elsif v_grant.grant_type='style' then
          v_style_inserted := null;
          v_play_owns_style := false;
          insert into public.user_inventory(user_id,item_id)
            values(p_user_id,v_grant.grant_key)
            on conflict(user_id,item_id) do nothing
            returning item_id into v_style_inserted;
          if v_style_inserted is not null then
            v_play_owns_style := true;
          else
            select exists(
              select 1 from public.play_purchase_grants pg
              where pg.user_id=p_user_id and pg.grant_type='style' and pg.grant_key=v_grant.grant_key
                and pg.reversed_at is null and pg.owns_inventory
            ) into v_play_owns_style;
          end if;
          insert into public.play_purchase_grants(
            purchase_token,user_id,product_id,grant_type,grant_key,amount,owns_inventory
          ) values(trim(p_purchase_token),p_user_id,p_product_id,'style',v_grant.grant_key,0,v_play_owns_style)
          on conflict do nothing;
        end if;
      end loop;

      if v_delta>0 then
        update public.profiles
          set diamonds=coalesce(diamonds,0)+v_delta,updated_at=now()
          where id=p_user_id
          returning diamonds into v_balance;
        insert into public.diamond_ledger(user_id,delta,reason)
          values(p_user_id,v_delta,'google_play_purchase:'||trim(p_purchase_token));
        insert into public.play_purchase_grants(
          purchase_token,user_id,product_id,grant_type,grant_key,amount,owns_inventory
        ) values(trim(p_purchase_token),p_user_id,p_product_id,'son_coin','',v_delta,false)
        on conflict do nothing;
      end if;
    else
      select diamonds into v_balance from public.profiles where id=p_user_id;
      v_delta:=0;
    end if;
  end if;

  return jsonb_build_object(
    'success',true,
    'already_processed',not v_inserted,
    'purchase_id',v_purchase_id,
    'product_id',p_product_id,
    'son_coin_granted',v_delta,
    'son_coin_balance',v_balance,
    'entitlement_status',case when v_is_vip or v_is_season then v_entitlement_status else null end,
    'expires_at',p_expires_at
  );
end
$$;

revoke all on function public.apply_verified_play_purchase_v2(uuid,text,text,text,timestamptz,text,text)
  from public,anon,authenticated;
grant execute on function public.apply_verified_play_purchase_v2(uuid,text,text,text,timestamptz,text,text)
  to service_role;

-- Constraint-safe successor body for the existing subscription reconciliation API.
create or replace function public.reconcile_play_entitlement_v1(
  p_purchase_token text,
  p_play_state text,
  p_expires_at timestamptz default null,
  p_revoke boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_purchase public.purchases%rowtype;
  v_entitlement_status text;
  v_purchase_status text;
  v_subscription_status text;
  v_vip_active boolean;
begin
  if nullif(trim(p_purchase_token),'') is null then raise exception 'invalid_purchase_token'; end if;
  select * into v_purchase
    from public.purchases
    where purchase_token=trim(p_purchase_token)
    for update;
  if not found then return jsonb_build_object('success',true,'known',false); end if;

  v_entitlement_status := case
    when p_revoke then 'revoked'
    when p_play_state='SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then 'grace'
    when p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD','SUBSCRIPTION_STATE_PAUSED') then 'hold'
    when p_play_state='SUBSCRIPTION_STATE_CANCELED' and coalesce(p_expires_at,v_purchase.expires_at)>now() then 'canceled'
    when p_play_state in ('SUBSCRIPTION_STATE_EXPIRED','SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED') then 'expired'
    when p_play_state='SUBSCRIPTION_STATE_ACTIVE' then 'active'
    else 'pending'
  end;
  v_purchase_status := case when p_revoke then 'refunded' else 'verified' end;
  v_subscription_status := case v_entitlement_status
    when 'active' then 'active'
    when 'grace' then 'grace'
    when 'canceled' then 'cancelled'
    when 'expired' then 'expired'
    else 'inactive'
  end;

  update public.purchases
    set play_state=p_play_state,
        status=v_purchase_status,
        expires_at=coalesce(p_expires_at,expires_at),
        revoked_at=case when p_revoke then coalesce(revoked_at,now()) else revoked_at end,
        last_checked_at=now()
    where id=v_purchase.id;

  if v_purchase.product_id in ('vip_monthly','vip_yearly') then
    update public.subscriptions
      set status=v_subscription_status,
          expires_at=coalesce(p_expires_at,expires_at),
          updated_at=now()
      where user_id=v_purchase.user_id;
    update public.store_entitlements
      set status=v_entitlement_status,
          expires_at=coalesce(p_expires_at,expires_at),
          updated_at=now()
      where user_id=v_purchase.user_id
        and entitlement_key='vip'
        and source_type='play'
        and source_id=trim(p_purchase_token);
    select exists(
      select 1 from public.subscriptions
      where user_id=v_purchase.user_id
        and status in ('active','grace','cancelled')
        and expires_at>now()
    ) into v_vip_active;
    update public.profiles set is_vip=v_vip_active,updated_at=now() where id=v_purchase.user_id;

  elsif v_purchase.product_id in ('season_pass','season_pass_monthly') then
    update public.season_pass_entitlements
      set status=v_entitlement_status,
          expires_at=coalesce(p_expires_at,expires_at),
          updated_at=now()
      where user_id=v_purchase.user_id;
    update public.store_entitlements
      set status=v_entitlement_status,
          expires_at=coalesce(p_expires_at,expires_at),
          updated_at=now()
      where user_id=v_purchase.user_id
        and entitlement_key='season_pass'
        and source_type='play'
        and source_id=trim(p_purchase_token);
  end if;

  return jsonb_build_object(
    'success',true,
    'known',true,
    'status',v_entitlement_status,
    'purchase_status',v_purchase_status,
    'product_id',v_purchase.product_id
  );
end
$$;

revoke all on function public.reconcile_play_entitlement_v1(text,text,timestamptz,boolean)
  from public,anon,authenticated;
grant execute on function public.reconcile_play_entitlement_v1(text,text,timestamptz,boolean)
  to service_role;

-- Lower-bound policy: Son Coin can never become negative. If a user has already spent part of a
-- refunded grant, recover only the available balance and persist the unrecovered amount as
-- reversal_shortfall for fraud/support audit. Reprocessing the same token is idempotent.
create or replace function public.reconcile_play_entitlement_v2(
  p_purchase_token text,
  p_play_state text,
  p_expires_at timestamptz default null,
  p_revoke boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_purchase public.purchases%rowtype;
  v_grant public.play_purchase_grants%rowtype;
  v_reversed_count integer := 0;
  v_recovered_total integer := 0;
  v_shortfall_total integer := 0;
  v_balance integer;
  v_recovered integer;
begin
  if nullif(trim(p_purchase_token),'') is null then raise exception 'invalid_purchase_token'; end if;

  select * into v_purchase
    from public.purchases
    where purchase_token=trim(p_purchase_token)
    for update;
  if not found then return jsonb_build_object('success',true,'known',false); end if;

  if v_purchase.purchase_type='subscription'
     or v_purchase.product_id in ('vip_monthly','vip_yearly','season_pass','season_pass_monthly') then
    return public.reconcile_play_entitlement_v1(trim(p_purchase_token),p_play_state,p_expires_at,p_revoke);
  end if;

  update public.purchases
    set play_state=p_play_state,
        status=case when p_revoke then 'refunded' else status end,
        revoked_at=case when p_revoke then coalesce(revoked_at,now()) else revoked_at end,
        last_checked_at=now()
    where id=v_purchase.id;

  if not p_revoke then
    return jsonb_build_object(
      'success',true,'known',true,
      'status',(select status from public.purchases where id=v_purchase.id),
      'product_id',v_purchase.product_id,
      'reversed_grants',0,
      'recovered_son_coin',0,
      'reversal_shortfall',0
    );
  end if;

  for v_grant in
    select * from public.play_purchase_grants
    where purchase_token=trim(p_purchase_token) and reversed_at is null
    order by grant_type,grant_key
    for update
  loop
    if v_grant.grant_type='son_coin' and v_grant.amount>0 then
      select greatest(coalesce(diamonds,0),0)
        into v_balance
        from public.profiles
        where id=v_purchase.user_id
        for update;
      if not found then raise exception 'profile_not_found'; end if;

      v_recovered := least(v_balance,v_grant.amount);
      update public.profiles
        set diamonds=greatest(0,coalesce(diamonds,0)-v_grant.amount),updated_at=now()
        where id=v_purchase.user_id
        returning diamonds into v_balance;

      if v_recovered>0 then
        insert into public.diamond_ledger(user_id,delta,reason)
          values(v_purchase.user_id,-v_recovered,'google_play_reversal:'||trim(p_purchase_token)||':son_coin')
          on conflict do nothing;
      end if;

      update public.play_purchase_grants
        set reversed_at=now(),
            reversed_amount=v_recovered,
            reversal_shortfall=v_grant.amount-v_recovered
        where purchase_token=v_grant.purchase_token
          and grant_type=v_grant.grant_type
          and grant_key=v_grant.grant_key;

      v_recovered_total := v_recovered_total + v_recovered;
      v_shortfall_total := v_shortfall_total + (v_grant.amount-v_recovered);

    elsif v_grant.grant_type='style' then
      update public.play_purchase_grants
        set reversed_at=now(),reversed_amount=0,reversal_shortfall=0
        where purchase_token=v_grant.purchase_token
          and grant_type=v_grant.grant_type
          and grant_key=v_grant.grant_key;

      if v_grant.owns_inventory and not exists(
        select 1 from public.play_purchase_grants other
        where other.user_id=v_purchase.user_id
          and other.grant_type='style'
          and other.grant_key=v_grant.grant_key
          and other.reversed_at is null
          and other.owns_inventory
      ) then
        delete from public.user_inventory i
          where i.user_id=v_purchase.user_id and i.item_id=v_grant.grant_key;
      end if;
    end if;

    v_reversed_count := v_reversed_count+1;
  end loop;

  if v_balance is null then
    select greatest(coalesce(diamonds,0),0) into v_balance from public.profiles where id=v_purchase.user_id;
  end if;

  return jsonb_build_object(
    'success',true,
    'known',true,
    'status','refunded',
    'product_id',v_purchase.product_id,
    'reversed_grants',v_reversed_count,
    'recovered_son_coin',v_recovered_total,
    'reversal_shortfall',v_shortfall_total,
    'son_coin_balance',v_balance
  );
end
$$;

revoke all on function public.reconcile_play_entitlement_v2(text,text,timestamptz,boolean)
  from public,anon,authenticated;
grant execute on function public.reconcile_play_entitlement_v2(text,text,timestamptz,boolean)
  to service_role;

create table if not exists public.play_rtdn_events (
  message_id text primary key,
  event_type text not null,
  purchase_token text,
  order_id text,
  event_time timestamptz,
  received_at timestamptz not null default now(),
  processing_started_at timestamptz,
  processed_at timestamptz,
  processing_error text,
  attempt_count integer not null default 0 check (attempt_count >= 0)
);

alter table public.play_rtdn_events enable row level security;
revoke all on public.play_rtdn_events from public,anon,authenticated;
grant select,insert,update on public.play_rtdn_events to service_role;

-- Retry-safe claim: completed-success messages dedupe forever; failed or abandoned attempts may be
-- reclaimed. A five-minute lease prevents two concurrent deliveries from processing at once.
create or replace function public.claim_play_rtdn_event_v2(
  p_message_id text,
  p_event_type text,
  p_purchase_token text default null,
  p_order_id text default null,
  p_event_time timestamptz default null
)
returns boolean
language plpgsql
security definer
set search_path=''
as $$
declare
  v_event public.play_rtdn_events%rowtype;
  v_inserted text;
begin
  if nullif(trim(p_message_id),'') is null then raise exception 'invalid_message_id'; end if;
  if nullif(trim(p_event_type),'') is null then raise exception 'invalid_event_type'; end if;

  insert into public.play_rtdn_events(
    message_id,event_type,purchase_token,order_id,event_time,processing_started_at,attempt_count
  ) values(
    trim(p_message_id),trim(p_event_type),nullif(trim(p_purchase_token),''),
    nullif(trim(p_order_id),''),p_event_time,now(),1
  )
  on conflict(message_id) do nothing
  returning message_id into v_inserted;
  if v_inserted is not null then return true; end if;

  select * into v_event
    from public.play_rtdn_events
    where message_id=trim(p_message_id)
    for update;

  if v_event.processed_at is not null and v_event.processing_error is null then return false; end if;
  if v_event.processed_at is null
     and v_event.processing_started_at is not null
     and v_event.processing_started_at > now()-interval '5 minutes' then
    return false;
  end if;

  update public.play_rtdn_events
    set processing_started_at=now(),
        processed_at=null,
        processing_error=null,
        attempt_count=attempt_count+1,
        event_type=trim(p_event_type),
        purchase_token=coalesce(nullif(trim(p_purchase_token),''),purchase_token),
        order_id=coalesce(nullif(trim(p_order_id),''),order_id),
        event_time=coalesce(p_event_time,event_time)
    where message_id=trim(p_message_id);
  return true;
end
$$;

revoke all on function public.claim_play_rtdn_event_v2(text,text,text,text,timestamptz)
  from public,anon,authenticated;
grant execute on function public.claim_play_rtdn_event_v2(text,text,text,text,timestamptz)
  to service_role;

create or replace function public.finish_play_rtdn_event_v2(
  p_message_id text,
  p_error text default null
)
returns void
language sql
security definer
set search_path=''
as $$
  update public.play_rtdn_events
  set processed_at=now(),
      processing_error=nullif(left(coalesce(p_error,''),500),'')
  where message_id=trim(p_message_id);
$$;

revoke all on function public.finish_play_rtdn_event_v2(text,text)
  from public,anon,authenticated;
grant execute on function public.finish_play_rtdn_event_v2(text,text)
  to service_role;
