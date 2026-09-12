-- Enforce the same sale window at purchase time that authenticated catalog reads already enforce.
-- This definition intentionally preserves the current production purchase semantics, including
-- admin test purchases and owner unlimited-diamond purchases. Only the availability predicate changes.

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_uid uuid:=auth.uid();
  v_item public.shop_items%rowtype;
  v_balance integer;
  v_vip boolean;
  v_admin_free boolean:=false;
  v_owner_unlimited boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select * into v_item
  from public.shop_items
  where id=p_item_id
    and active=true
    and available_from<=now()
    and (available_until is null or available_until>now());

  if not found then raise exception 'item_not_found'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;

  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;

  select coalesce(a.free_test_purchases,false) into v_admin_free
  from public.admin_users a where a.user_id=v_uid;
  v_admin_free:=coalesce(v_admin_free,false) and public.is_admin();

  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=v_uid and o.active and o.unlimited_diamonds
  ) into v_owner_unlimited;

  if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;

  if not v_admin_free and not v_owner_unlimited then
    if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_item.diamond_price,updated_at=now() where id=v_uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  end if;

  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);

  if v_admin_free then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'test_free_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  elsif v_owner_unlimited then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'owner_unlimited_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  end if;

  return jsonb_build_object(
    'success',true,
    'item_id',p_item_id,
    'diamonds',case when v_admin_free or v_owner_unlimited then v_balance else v_balance-v_item.diamond_price end,
    'admin_test_free',v_admin_free,
    'owner_unlimited',v_owner_unlimited
  );
end
$$;

revoke all on function public.purchase_shop_item(text) from public, anon;
grant execute on function public.purchase_shop_item(text) to authenticated, service_role;

select pg_notify('pgrst','reload schema');
