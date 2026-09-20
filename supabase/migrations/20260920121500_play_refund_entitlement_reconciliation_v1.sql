begin;

create or replace function public.reconcile_play_entitlement_v1(
  p_purchase_token text,
  p_play_state text,
  p_expires_at timestamptz default null,
  p_revoke boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_purchase public.purchases%rowtype;
  v_status text;
  v_vip_active boolean := false;
begin
  select *
    into v_purchase
    from public.purchases
   where purchase_token = trim(p_purchase_token)
   for update;

  if not found then
    return jsonb_build_object('success', true, 'known', false);
  end if;

  v_status := case
    when p_revoke then 'revoked'
    when p_play_state = 'PURCHASED' then 'verified'
    when p_play_state = 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then 'grace'
    when p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD', 'SUBSCRIPTION_STATE_PAUSED') then 'hold'
    when p_play_state = 'SUBSCRIPTION_STATE_CANCELED'
      and coalesce(p_expires_at, v_purchase.expires_at) > now() then 'canceled'
    when p_play_state in ('SUBSCRIPTION_STATE_EXPIRED', 'SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED') then 'expired'
    when p_play_state = 'SUBSCRIPTION_STATE_ACTIVE' then 'active'
    else 'pending'
  end;

  update public.purchases
     set play_state = p_play_state,
         status = v_status,
         expires_at = coalesce(p_expires_at, expires_at),
         revoked_at = case when p_revoke then now() else revoked_at end,
         last_checked_at = now()
   where id = v_purchase.id;

  if v_purchase.product_id in ('vip_monthly', 'vip_yearly') then
    update public.subscriptions
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id;

    update public.store_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id
       and entitlement_key = 'vip'
       and source_type = 'play'
       and source_id = trim(p_purchase_token);

    select exists(
      select 1
        from public.subscriptions
       where user_id = v_purchase.user_id
         and status in ('active', 'grace', 'canceled')
         and expires_at > now()
    ) into v_vip_active;

    update public.profiles
       set is_vip = (
         v_vip_active
         or public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')
       ),
       updated_at = now()
     where id = v_purchase.user_id;

  elsif v_purchase.product_id in ('season_pass', 'season_pass_monthly') then
    update public.season_pass_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id;

    update public.store_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id
       and entitlement_key = 'season_pass'
       and source_type = 'play'
       and source_id = trim(p_purchase_token);

  elsif v_purchase.product_id in ('series_game', 'letter_table', 'score_calculator', 'pro_lifetime') then
    if p_revoke then
      -- Revoke only grants created by this refunded Google Play token. Other valid purchases,
      -- subscriptions, or lifetime receipts remain authoritative and continue to grant access.
      update public.store_entitlements
         set status = 'revoked',
             updated_at = now()
       where user_id = v_purchase.user_id
         and source_id = trim(p_purchase_token)
         and (
           (
             source_type = 'play'
             and entitlement_key = v_purchase.product_id
           )
           or (
             v_purchase.product_id = 'pro_lifetime'
             and source_type = 'pro_bundle'
             and entitlement_key in ('series_game', 'letter_table', 'score_calculator')
           )
         );

      if v_purchase.product_id = 'pro_lifetime' then
        select exists(
          select 1
            from public.subscriptions
           where user_id = v_purchase.user_id
             and status in ('active', 'grace', 'canceled')
             and expires_at > now()
        ) into v_vip_active;

        update public.profiles
           set is_vip = (
             v_vip_active
             or public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')
           ),
           updated_at = now()
         where id = v_purchase.user_id;
      end if;
    end if;
  end if;

  return jsonb_build_object(
    'success', true,
    'known', true,
    'status', v_status,
    'product_id', v_purchase.product_id
  );
end
$function$;

commit;
