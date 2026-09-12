-- Run only after supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql
-- has been applied to an isolated staging/dev branch. Everything below rolls back.

begin;

insert into public.profiles(id,display_name,diamonds)
values ('00000000-0000-4000-8000-000000000340'::uuid,'RTDN Staging Fixture',0);

insert into public.shop_items(id,kind,name_tr,name_en,diamond_price,active)
values ('__rtdn_style_fixture__','style','RTDN Test Stil','RTDN Test Style',0,false);

insert into public.store_product_grants(product_id,grant_type,grant_key,amount)
values ('__rtdn_style_pack__','style','__rtdn_style_fixture__',0);

do $$
declare
  v_uid constant uuid := '00000000-0000-4000-8000-000000000340'::uuid;
  v_json jsonb;
  v_count integer;
  v_balance integer;
  v_reversed integer;
  v_shortfall integer;
  v_claim boolean;
  v_status text;
begin
  -- Same purchase token must never double-grant coin.
  v_json := public.apply_verified_play_purchase_v2(
    v_uid,'coins_500','staging-coin-token-340','GPA.staging.coin',null,'PURCHASED','ACKNOWLEDGED'
  );
  if (v_json->>'son_coin_granted')::integer <> 500 then raise exception 'first_coin_grant_failed'; end if;

  v_json := public.apply_verified_play_purchase_v2(
    v_uid,'coins_500','staging-coin-token-340','GPA.staging.coin',null,'PURCHASED','ACKNOWLEDGED'
  );
  if coalesce((v_json->>'son_coin_granted')::integer,-1) <> 0 or (v_json->>'already_processed')::boolean is not true then
    raise exception 'duplicate_coin_grant_not_idempotent';
  end if;

  select diamonds into v_balance from public.profiles where id=v_uid;
  if v_balance <> 500 then raise exception 'duplicate_coin_balance_changed:%',v_balance; end if;

  select count(*) into v_count
  from public.play_purchase_grants
  where purchase_token='staging-coin-token-340' and grant_type='son_coin';
  if v_count <> 1 then raise exception 'coin_provenance_count:%',v_count; end if;

  -- Simulate spending most of the purchased balance. Refund must clamp at zero and record shortfall.
  update public.profiles set diamonds=120 where id=v_uid;
  v_json := public.reconcile_play_entitlement_v2(
    'staging-coin-token-340','VOIDED:REFUND',null,true
  );
  select diamonds into v_balance from public.profiles where id=v_uid;
  if v_balance <> 0 then raise exception 'coin_refund_negative_or_nonzero:%',v_balance; end if;

  select reversed_amount,reversal_shortfall into v_reversed,v_shortfall
  from public.play_purchase_grants
  where purchase_token='staging-coin-token-340' and grant_type='son_coin';
  if v_reversed <> 120 or v_shortfall <> 380 then
    raise exception 'coin_refund_recovery_mismatch:%/%',v_reversed,v_shortfall;
  end if;

  select count(*) into v_count
  from public.diamond_ledger
  where user_id=v_uid and reason='google_play_reversal:staging-coin-token-340:son_coin';
  if v_count <> 1 then raise exception 'coin_refund_ledger_count:%',v_count; end if;

  -- Repeating the same reversal must be a no-op.
  perform public.reconcile_play_entitlement_v2('staging-coin-token-340','VOIDED:REFUND',null,true);
  select diamonds into v_balance from public.profiles where id=v_uid;
  select count(*) into v_count
  from public.diamond_ledger
  where user_id=v_uid and reason='google_play_reversal:staging-coin-token-340:son_coin';
  if v_balance <> 0 or v_count <> 1 then raise exception 'repeat_refund_not_idempotent'; end if;

  -- Two Play purchases that grant the same Style establish two active provenance rows.
  perform public.apply_verified_play_purchase_v2(
    v_uid,'__rtdn_style_pack__','staging-style-token-a','GPA.staging.style.a',null,'PURCHASED','ACKNOWLEDGED'
  );
  perform public.apply_verified_play_purchase_v2(
    v_uid,'__rtdn_style_pack__','staging-style-token-b','GPA.staging.style.b',null,'PURCHASED','ACKNOWLEDGED'
  );

  select count(*) into v_count
  from public.play_purchase_grants
  where user_id=v_uid and grant_type='style' and grant_key='__rtdn_style_fixture__'
    and reversed_at is null and owns_inventory;
  if v_count <> 2 then raise exception 'style_provenance_chain_count:%',v_count; end if;

  perform public.reconcile_play_entitlement_v2('staging-style-token-a','VOIDED:REFUND',null,true);
  if not exists(
    select 1 from public.user_inventory
    where user_id=v_uid and item_id='__rtdn_style_fixture__'
  ) then raise exception 'style_removed_while_other_play_purchase_active'; end if;

  perform public.reconcile_play_entitlement_v2('staging-style-token-b','VOIDED:REFUND',null,true);
  if exists(
    select 1 from public.user_inventory
    where user_id=v_uid and item_id='__rtdn_style_fixture__'
  ) then raise exception 'style_not_removed_after_last_play_provenance_reversed'; end if;

  -- RTDN message lease/dedupe: concurrent duplicate is busy, failed event can retry, successful
  -- completion dedupes permanently.
  v_claim := public.claim_play_rtdn_event_v2('staging-msg-340','voided_purchase','staging-coin-token-340',null,now());
  if v_claim is not true then raise exception 'first_rtdn_claim_failed'; end if;
  v_claim := public.claim_play_rtdn_event_v2('staging-msg-340','voided_purchase','staging-coin-token-340',null,now());
  if v_claim is not false then raise exception 'concurrent_rtdn_claim_not_blocked'; end if;

  perform public.finish_play_rtdn_event_v2('staging-msg-340','simulated transient error');
  v_claim := public.claim_play_rtdn_event_v2('staging-msg-340','voided_purchase','staging-coin-token-340',null,now());
  if v_claim is not true then raise exception 'failed_rtdn_event_not_reclaimable'; end if;
  perform public.finish_play_rtdn_event_v2('staging-msg-340',null);
  v_claim := public.claim_play_rtdn_event_v2('staging-msg-340','voided_purchase','staging-coin-token-340',null,now());
  if v_claim is not false then raise exception 'completed_rtdn_event_not_deduped'; end if;

  -- Subscription behavior must remain delegated to v1 for active/grace/canceled/expired/revoked.
  perform public.apply_verified_play_purchase_v2(
    v_uid,'vip_monthly','staging-vip-token-340','GPA.staging.vip',now()+interval '2 days',
    'SUBSCRIPTION_STATE_ACTIVE','ACKNOWLEDGED'
  );
  if not (select is_vip from public.profiles where id=v_uid) then raise exception 'vip_active_regression'; end if;

  perform public.reconcile_play_entitlement_v2(
    'staging-vip-token-340','SUBSCRIPTION_STATE_IN_GRACE_PERIOD',now()+interval '2 days',false
  );
  select status into v_status from public.subscriptions where user_id=v_uid;
  if v_status <> 'grace' or not (select is_vip from public.profiles where id=v_uid) then raise exception 'vip_grace_regression'; end if;

  perform public.reconcile_play_entitlement_v2(
    'staging-vip-token-340','SUBSCRIPTION_STATE_CANCELED',now()+interval '1 day',false
  );
  select status into v_status from public.subscriptions where user_id=v_uid;
  if v_status <> 'canceled' or not (select is_vip from public.profiles where id=v_uid) then raise exception 'vip_canceled_regression'; end if;

  perform public.reconcile_play_entitlement_v2(
    'staging-vip-token-340','SUBSCRIPTION_STATE_EXPIRED',now()-interval '1 minute',false
  );
  select status into v_status from public.subscriptions where user_id=v_uid;
  if v_status <> 'expired' or (select is_vip from public.profiles where id=v_uid) then raise exception 'vip_expired_regression'; end if;

  perform public.apply_verified_play_purchase_v2(
    v_uid,'vip_monthly','staging-vip-token-340','GPA.staging.vip',now()+interval '2 days',
    'SUBSCRIPTION_STATE_ACTIVE','ACKNOWLEDGED'
  );
  perform public.reconcile_play_entitlement_v2(
    'staging-vip-token-340','SUBSCRIPTION_STATE_ACTIVE',now()+interval '2 days',true
  );
  select status into v_status from public.subscriptions where user_id=v_uid;
  if v_status <> 'revoked' or (select is_vip from public.profiles where id=v_uid) then raise exception 'vip_revoke_regression'; end if;
end
$$;

rollback;
