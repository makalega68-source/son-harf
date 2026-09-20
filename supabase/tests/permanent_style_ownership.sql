-- Permanent ownership policy after store_runtime_equip_parity.
-- Store rotation may retire a product from sale without revoking an owned runtime-supported style.
-- Globally retired runtime classes (profile frames, mascots, unsupported themes) remain blocked.
begin;
set local lock_timeout='2s';
set local statement_timeout='10s';

do $runtime_policy$
declare
  equip_def text;
  purchase_def text;
  compact_equip text;
  compact_purchase text;
begin
  if not public.is_runtime_supported_shop_item_v1('theme_dark_arena','game_theme') then
    raise exception 'legacy_runtime_theme_not_supported';
  end if;
  if not public.is_runtime_supported_shop_item_v1('theme_black','game_theme') then
    raise exception 'current_black_theme_not_supported';
  end if;
  if public.is_runtime_supported_shop_item_v1('frame_round_golden_avatar','profile_frame') then
    raise exception 'retired_profile_frame_runtime_reopened';
  end if;
  if public.is_runtime_supported_shop_item_v1('mascot_chibi_wizard','mascot') then
    raise exception 'retired_mascot_runtime_reopened';
  end if;

  select pg_get_functiondef('public.equip_shop_item(text)'::regprocedure) into equip_def;
  select pg_get_functiondef('public.purchase_shop_item(text)'::regprocedure) into purchase_def;
  compact_equip := regexp_replace(lower(equip_def),'\s+','','g');
  compact_purchase := regexp_replace(lower(purchase_def),'\s+','','g');

  if compact_equip like '%whereid=p_item_idandactive=true%' then
    raise exception 'store_rotation_still_revokes_owned_runtime_style';
  end if;
  if compact_equip not like '%frompublic.user_inventorywhereuser_id=v_uidanditem_id=p_item_id%' then
    raise exception 'equip_ownership_gate_missing';
  end if;
  if compact_equip not like '%item_runtime_unavailable%' then
    raise exception 'equip_runtime_gate_missing';
  end if;

  if compact_purchase not like '%andactive=true%' then
    raise exception 'purchase_active_gate_missing';
  end if;
  if compact_purchase not like '%item_runtime_unavailable%' then
    raise exception 'purchase_runtime_gate_missing';
  end if;
end
$runtime_policy$;

do $permissions$
begin
  if has_function_privilege('anon','public.equip_shop_item(text)','EXECUTE') then
    raise exception 'anonymous_equip_rpc_exposed';
  end if;
  if has_function_privilege('anon','public.purchase_shop_item(text)','EXECUTE') then
    raise exception 'anonymous_purchase_rpc_exposed';
  end if;
  if has_function_privilege('authenticated','public.is_runtime_supported_shop_item_v1(text,text)','EXECUTE') then
    raise exception 'internal_runtime_policy_exposed';
  end if;
end
$permissions$;
rollback;
