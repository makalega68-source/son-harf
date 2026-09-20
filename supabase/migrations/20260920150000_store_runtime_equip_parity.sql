-- Store runtime/equipment parity.
-- Store rotation may hide a previously purchased cosmetic, but ownership is permanent.
-- Only products with a verified live runtime counterpart may be purchased or equipped.

begin;

create or replace function public.is_runtime_supported_shop_item_v1(
  p_item_id text,
  p_kind text
)
returns boolean
language sql
immutable
set search_path to ''
as $function$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena')
    when 'profile_frame' then false
    when 'name_style' then p_item_id in ('name_cyan','name_sapphire','name_amethyst','name_aurelia')
    when 'keyboard_theme' then p_item_id in (
      'keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold','keyboard_premium_white'
    )
    when 'victory_effect' then p_item_id='victory_crown'
    when 'emoji_pack' then p_item_id='emoji_vip'
    else false
  end
$function$;

revoke all on function public.is_runtime_supported_shop_item_v1(text,text) from public,anon,authenticated;
grant execute on function public.is_runtime_supported_shop_item_v1(text,text) to service_role;

-- Catalog defense-in-depth: an unsupported row cannot accidentally become sellable later.
alter table public.shop_items
  drop constraint if exists shop_items_active_runtime_supported_v1;
alter table public.shop_items
  add constraint shop_items_active_runtime_supported_v1
  check (not active or public.is_runtime_supported_shop_item_v1(id,kind));

-- Retired classes keep purchase/inventory history, but must not remain silently equipped.
update public.user_equipped_cosmetics
set profile_frame_id=null,
    mascot_id=null,
    updated_at=now()
where profile_frame_id is not null or mascot_id is not null;

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path to 'public','pg_temp'
as $function$
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

  if not public.is_runtime_supported_shop_item_v1(v_item.id,v_item.kind) then
    raise exception 'item_runtime_unavailable';
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
$function$;

revoke all on function public.purchase_shop_item(text) from public,anon;
grant execute on function public.purchase_shop_item(text) to authenticated,service_role;

create or replace function public.equip_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_item public.shop_items%rowtype;
  v_owned boolean := false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  -- Do not require active=true here: a store rotation must not revoke a supported purchase.
  select * into v_item from public.shop_items where id=p_item_id;
  if not found then raise exception 'item_not_found'; end if;

  if not public.is_runtime_supported_shop_item_v1(v_item.id,v_item.kind) then
    raise exception 'item_runtime_unavailable';
  end if;

  select exists(
    select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id
  ) into v_owned;
  if not v_owned then raise exception 'not_owned'; end if;

  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics
  set name_style_id=case when v_item.kind='name_style' then p_item_id else name_style_id end,
      game_theme_id=case when v_item.kind='game_theme' then p_item_id else game_theme_id end,
      keyboard_theme_id=case when v_item.kind='keyboard_theme' then p_item_id else keyboard_theme_id end,
      victory_effect_id=case when v_item.kind='victory_effect' then p_item_id else victory_effect_id end,
      emoji_pack_id=case when v_item.kind='emoji_pack' then p_item_id else emoji_pack_id end,
      updated_at=now()
  where user_id=v_uid;

  return jsonb_build_object('success',true,'item_id',p_item_id,'kind',v_item.kind);
end
$function$;

revoke all on function public.equip_shop_item(text) from public,anon;
grant execute on function public.equip_shop_item(text) to authenticated,service_role;

commit;
