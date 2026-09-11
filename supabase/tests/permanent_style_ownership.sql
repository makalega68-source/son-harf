-- Run as database maintainer after permanent_style_ownership migration.
-- Uses an existing owned frame only inside a transaction. Every fixture change is rolled back.
-- Stops quickly rather than blocking a player's purchase or selection.
begin;
set local lock_timeout='2s';
set local statement_timeout='10s';

do $test$
declare
  owner_id uuid;
  product_id text;
  outsider_id uuid := gen_random_uuid();
begin
  select i.user_id, i.item_id into owner_id, product_id
  from public.user_inventory i join public.shop_items s on s.id=i.item_id
  where s.kind='profile_frame' and s.id like 'frame_asset_%'
  limit 1;
  if owner_id is null then raise exception 'no_owned_frame_fixture'; end if;
  update public.shop_items set active=false where id=product_id;
  perform set_config('request.jwt.claim.sub',owner_id::text,true);
  set local role authenticated;
  if not exists(select 1 from public.shop_items where id=product_id) then
    raise exception 'owner_cannot_read_retired_product';
  end if;
  perform public.equip_shop_item(product_id);
  if not exists(select 1 from public.user_equipped_cosmetics where user_id=owner_id and profile_frame_id=product_id) then
    raise exception 'owner_cannot_equip_retired_product';
  end if;
  perform set_config('request.jwt.claim.sub',outsider_id::text,true);
  if exists(select 1 from public.shop_items where id=product_id) then
    raise exception 'retired_product_leaked_to_non_owner';
  end if;
  if exists(select 1 from public.user_inventory where user_id=owner_id) then
    raise exception 'inventory_leaked_to_non_owner';
  end if;
  begin
    perform public.equip_shop_item(product_id);
    raise exception 'non_owner_equip_was_not_rejected';
  exception when others then
    if sqlerrm <> 'not_owned' then raise; end if;
  end;
  reset role;
end
$test$;

do $permissions$
begin
  if has_function_privilege('anon','public.equip_shop_item(text)','EXECUTE') then
    raise exception 'anonymous_equip_rpc_exposed';
  end if;
end
$permissions$;
rollback;
