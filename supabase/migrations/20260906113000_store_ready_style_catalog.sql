-- Store-ready cosmetic catalogue. The active theme is the runtime-verified dark arena.
-- Prices are Son Coin only; no product grants competitive power.

update public.shop_items
set active = false
where id = 'theme_monster_blue'
  and kind = 'game_theme';

insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order)
values ('theme_dark_arena', 'game_theme', 'Gece Arenası', 'Night Arena', 'Siyah, füme ve altın tonlarda yoğun maç görünümü.', 'A high-contrast match look in black, graphite and gold.', 600, false, true, 40)
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

update public.shop_items
set diamond_price = case id
  when 'theme_dark_arena' then 600
  when 'name_cyan' then 180
  when 'keyboard_neon' then 350
  when 'victory_crown' then 600
  when 'emoji_vip' then 300
  when 'frame_asset_red' then 120
  when 'frame_asset_mint' then 220
  when 'frame_asset_green' then 240
  when 'frame_asset_gold' then 260
  when 'frame_asset_purple' then 280
  else diamond_price
end
where id in (
  'theme_dark_arena','name_cyan','keyboard_neon','victory_crown','emoji_vip',
  'frame_asset_red','frame_asset_mint','frame_asset_green','frame_asset_gold','frame_asset_purple'
);
