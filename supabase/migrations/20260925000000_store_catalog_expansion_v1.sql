-- Store catalog expansion v1 (Son Coin cosmetics only; no gameplay effect, no pay-to-win).
-- 1. Turkish names for four live items that still showed English names.
-- 2. Runtime allow-list gains the new client-supported keyboard themes and name styles.
-- 3. Seven new cosmetics, priced in Son Coin, purchased/equipped through the existing
--    purchase_shop_item / equip_shop_item RPCs (server-authoritative ownership and balance).

update public.shop_items set name_tr = 'Siyah Tema' where id = 'theme_black' and name_tr = 'Black Theme';
update public.shop_items set name_tr = 'Siyah Altın Klavye' where id = 'keyboard_black_gold' and name_tr = 'Black Gold Keyboard';
update public.shop_items set name_tr = 'Gece Yarısı Klavye' where id = 'keyboard_midnight' and name_tr = 'Midnight Keyboard';
update public.shop_items set name_tr = 'Premium Beyaz Klavye' where id = 'keyboard_premium_white' and name_tr = 'Premium White Keyboard';

create or replace function public.is_runtime_supported_shop_item_v1(p_item_id text, p_kind text)
returns boolean
language sql
immutable
set search_path to ''
as $function$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena')
    when 'profile_frame' then false
    when 'name_style' then p_item_id in (
      'name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset'
    )
    when 'keyboard_theme' then p_item_id in (
      'keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold','keyboard_premium_white',
      'keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple'
    )
    when 'victory_effect' then p_item_id='victory_crown'
    when 'emoji_pack' then p_item_id='emoji_vip'
    else false
  end
$function$;

alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v1;
alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v2;
alter table public.shop_items add constraint shop_items_runtime_sale_guard_v2 check (
  active = false
  or (kind = 'game_theme' and id = 'theme_black')
  or (kind = 'keyboard_theme' and id = any (array[
    'keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold','keyboard_premium_white',
    'keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple']))
  or (kind = 'name_style' and id = any (array[
    'name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset']))
);

insert into public.shop_items (id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order, rarity)
values
  ('keyboard_sakura', 'keyboard_theme', 'Sakura Klavye', 'Sakura Keyboard',
   'Pembe çiçek tonlarında yumuşak klavye. Yalnızca görünümü değiştirir.', 'Soft blossom-pink keyboard. Appearance only.', 260, false, true, 60, 'RARE'),
  ('keyboard_ocean', 'keyboard_theme', 'Okyanus Klavye', 'Ocean Keyboard',
   'Serin turkuaz ve deniz mavisi tuşlar. Yalnızca görünümü değiştirir.', 'Cool turquoise and sea-blue keys. Appearance only.', 280, false, true, 61, 'RARE'),
  ('keyboard_forest', 'keyboard_theme', 'Orman Klavye', 'Forest Keyboard',
   'Sakin yeşil tonlarda klavye. Yalnızca görünümü değiştirir.', 'Calm green keyboard. Appearance only.', 260, false, true, 62, 'RARE'),
  ('keyboard_royal_purple', 'keyboard_theme', 'Kraliyet Moru Klavye', 'Royal Purple Keyboard',
   'Koyu mor zemin, lavanta vurgular. Yalnızca görünümü değiştirir.', 'Deep purple base with lavender accents. Appearance only.', 380, false, true, 63, 'EPIC'),
  ('name_emerald', 'name_style', 'Zümrüt İmza', 'Emerald Signature',
   'Adın zümrüt yeşiliyle görünür. Yalnızca görünümü değiştirir.', 'Your name shows in emerald green. Appearance only.', 240, false, true, 70, 'RARE'),
  ('name_ruby', 'name_style', 'Yakut İmza', 'Ruby Signature',
   'Adın yakut kırmızısıyla görünür. Yalnızca görünümü değiştirir.', 'Your name shows in ruby red. Appearance only.', 280, false, true, 71, 'RARE'),
  ('name_sunset', 'name_style', 'Gün Batımı İmza', 'Sunset Signature',
   'Adın gün batımı turuncusuyla görünür. Yalnızca görünümü değiştirir.', 'Your name shows in sunset orange. Appearance only.', 300, false, true, 72, 'EPIC')
on conflict (id) do update set
  kind = excluded.kind, name_tr = excluded.name_tr, name_en = excluded.name_en,
  description_tr = excluded.description_tr, description_en = excluded.description_en,
  diamond_price = excluded.diamond_price, vip_only = excluded.vip_only, active = excluded.active,
  sort_order = excluded.sort_order, rarity = excluded.rarity;
