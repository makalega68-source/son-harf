-- One purchasable board-and-letter set. Existing inventory and equipped styles stay untouched.
begin;

create or replace function public.is_runtime_supported_shop_item_v1(p_item_id text, p_kind text)
returns boolean
language sql
immutable
set search_path to ''
as $function$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena','theme_walnut_ivory')
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

insert into public.shop_items (
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
) values (
  'theme_walnut_ivory', 'game_theme', 'Ceviz & Fildişi', 'Walnut & Ivory',
  'Mat ceviz tahta ve fildişi harf taşları. Profil koleksiyonundan kullanılır.',
  'Matte walnut board and ivory letter tiles. Equip from your profile collection.',
  600, false, true, 45
)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = excluded.diamond_price,
  vip_only = excluded.vip_only,
  active = excluded.active,
  sort_order = excluded.sort_order;

commit;
