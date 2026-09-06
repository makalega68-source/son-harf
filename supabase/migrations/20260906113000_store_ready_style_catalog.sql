-- Store-ready cosmetic catalogue: every active item has a corresponding mobile runtime.
-- Prices are Son Coin only; no product grants competitive power.
update public.shop_items
set active = true,
    diamond_price = 720,
    name_tr = 'Aurora Arena',
    name_en = 'Aurora Arena',
    description_tr = 'Mor-kuzey ışıkları görünümü; oyun alanında anında uygulanır.',
    description_en = 'Aurora-light appearance applied instantly to the game board.',
    sort_order = 40
where id = 'theme_aurora'
  and kind = 'game_theme';

update public.shop_items
set diamond_price = case id
  when 'theme_monster_blue' then 600
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
  'theme_monster_blue','name_cyan','keyboard_neon','victory_crown','emoji_vip',
  'frame_asset_red','frame_asset_mint','frame_asset_green','frame_asset_gold','frame_asset_purple'
);
