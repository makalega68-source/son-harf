-- Profile Frames V2: four permanent Google Play cosmetics.
-- Historical frame renderer/products stay retired. These IDs are new and isolated.

insert into public.shop_items(
  id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order
) values
('profile_frame_pink_blossom','profile_frame','Pembe Çiçek Premium','Pink Blossom Premium','Çiçek detaylı premium profil çerçevesi. Google Play tek ödeme.','Premium floral profile frame. One-time Google Play purchase.',0,false,true,210),
('profile_frame_blue_royal','profile_frame','Mavi Royal Premium','Blue Royal Premium','Mavi desenli premium profil çerçevesi. Google Play tek ödeme.','Premium blue patterned profile frame. One-time Google Play purchase.',0,false,true,220),
('profile_frame_amethyst','profile_frame','Ametist Fantastik','Amethyst Fantasy','Mor fantastik premium profil çerçevesi. Google Play tek ödeme.','Premium amethyst fantasy profile frame. One-time Google Play purchase.',0,false,true,230),
('profile_frame_emerald','profile_frame','Zümrüt Fantastik','Emerald Fantasy','Zümrüt fantastik premium profil çerçevesi. Google Play tek ödeme.','Premium emerald fantasy profile frame. One-time Google Play purchase.',0,false,true,240)
on conflict(id) do update set
  kind=excluded.kind,
  name_tr=excluded.name_tr,
  name_en=excluded.name_en,
  description_tr=excluded.description_tr,
  description_en=excluded.description_en,
  diamond_price=0,
  vip_only=false,
  active=true,
  sort_order=excluded.sort_order;

-- apply_verified_play_purchase_v2 already supports data-driven style grants.
insert into public.store_product_grants(product_id,grant_type,grant_key,amount) values
('profile_frame_pink_blossom','style','profile_frame_pink_blossom',0),
('profile_frame_blue_royal','style','profile_frame_blue_royal',0),
('profile_frame_amethyst','style','profile_frame_amethyst',0),
('profile_frame_emerald','style','profile_frame_emerald',0)
on conflict(product_id,grant_type,grant_key) do update set amount=excluded.amount;

-- Never allow the zero-diamond shop RPC to bypass Google Play for these products.
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

  if p_item_id in (
    'profile_frame_pink_blossom',
    'profile_frame_blue_royal',
    'profile_frame_amethyst',
    'profile_frame_emerald'
  ) then
    raise exception 'google_play_required';
  end if;

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

-- Optional catalog gate used by the purchase verification Edge Function.
insert into public.store_catalog_config(product_id,enabled,updated_at) values
('profile_frame_pink_blossom',true,now()),
('profile_frame_blue_royal',true,now()),
('profile_frame_amethyst',true,now()),
('profile_frame_emerald',true,now())
on conflict(product_id) do update set enabled=true,updated_at=now();

select pg_notify('pgrst','reload schema');
