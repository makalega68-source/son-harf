-- Mascot store v1
-- Seven mascot characters are sold as permanent Google Play one-time products
-- (680 TL each, price set in Play Console). A verified purchase grants a
-- permanent store entitlement keyed by the product id; ordinary players own none.

begin;

-- Only explicitly sellable products may be enabled; the guard now includes the mascots.
alter table public.store_catalog_config
  drop constraint if exists store_catalog_enabled_product_guard_v2;
alter table public.store_catalog_config
  drop constraint if exists store_catalog_enabled_product_guard_v3;
alter table public.store_catalog_config
  add constraint store_catalog_enabled_product_guard_v3
  check (enabled = false or product_id = any (array[
    'vip_monthly','vip_yearly','season_pass_monthly','series_game','letter_table','score_calculator','pro_lifetime',
    'coins_500','coins_1500','coins_3500','coins_8000',
    'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik',
    'mascot_tekir','mascot_robot','mascot_astronot'
  ]));

insert into public.store_catalog_config(product_id, enabled, badge_tr, badge_en, sort_order, updated_at)
values
  ('mascot_klasik', true, 'MASKOT', 'MASCOT', 30, now()),
  ('mascot_pembe', true, 'MASKOT', 'MASCOT', 31, now()),
  ('mascot_mavi_seytancik', true, 'MASKOT', 'MASCOT', 32, now()),
  ('mascot_kirmizi_seytancik', true, 'MASKOT', 'MASCOT', 33, now()),
  ('mascot_tekir', true, 'MASKOT', 'MASCOT', 34, now()),
  ('mascot_robot', true, 'MASKOT', 'MASCOT', 35, now()),
  ('mascot_astronot', true, 'MASKOT', 'MASCOT', 36, now())
on conflict (product_id) do update
set enabled = excluded.enabled,
    badge_tr = excluded.badge_tr,
    badge_en = excluded.badge_en,
    sort_order = excluded.sort_order,
    updated_at = now();

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
  if p_product_id not in (
    'series_game','letter_table','score_calculator','pro_lifetime',
    'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik',
    'mascot_tekir','mascot_robot','mascot_astronot'
  ) then
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
    -- Each mascot is its own permanent entitlement, keyed by its product id.
    else case when p_product_id like 'mascot\_%' escape '\' then p_product_id end
  end;
  if v_key is null then
    raise exception 'unsupported_product';
  end if;

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

-- The signed-in player's owned mascots (entitlement keys such as 'mascot_tekir').
create or replace function public.get_my_mascots_v1()
returns text[]
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(array_agg(distinct e.entitlement_key order by e.entitlement_key), '{}'::text[])
  from public.store_entitlements e
  where e.user_id = auth.uid()
    and e.status = 'active'
    and e.entitlement_key like 'mascot\_%' escape '\'
    and (e.expires_at is null or e.expires_at > now());
$$;

revoke all on function public.get_my_mascots_v1() from public, anon;
grant execute on function public.get_my_mascots_v1() to authenticated;

commit;
