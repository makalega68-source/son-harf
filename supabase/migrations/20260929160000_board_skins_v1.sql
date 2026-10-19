-- Siege board skins. Stone Keep is the free default board (no product); River Valley, Frost Citadel
-- and Obsidian Bastion are cosmetic board looks sold for Son Coin. A skin changes only the look of
-- the Word Siege board: no gameplay, scoring or matchmaking effect.

alter table public.shop_items drop constraint if exists shop_items_kind_check;
alter table public.shop_items add constraint shop_items_kind_check check (kind = any (array[
  'profile_frame','name_style','game_theme','keyboard_theme','victory_effect','emoji_pack','mascot',
  'avatar_background','nameplate','badge','title','vs_intro','word_effect','emote','mascot_hat','board_skin'
]));

create or replace function public.is_runtime_supported_shop_item_v1(p_item_id text, p_kind text)
returns boolean language sql immutable set search_path to '' as $$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena','theme_walnut_ivory')
    when 'profile_frame' then p_item_id in (
      'frame_round_starter_blue','frame_round_starter_neutral','frame_round_pearl','frame_round_ocean',
      'frame_round_rose','frame_round_lilac','frame_round_botanic','frame_round_golden_avatar',
      'profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
      'profile_frame_pink_blossom','profile_frame_blue_royal'
    )
    when 'name_style' then p_item_id in ('name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset')
    when 'keyboard_theme' then p_item_id in (
      'keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold',
      'keyboard_premium_white','keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple'
    )
    when 'victory_effect' then p_item_id = 'victory_crown'
    when 'emoji_pack' then p_item_id = 'emoji_vip'
    when 'mascot_hat' then p_item_id in ('hat_wizard','hat_beret','hat_flower','hat_top')
    when 'board_skin' then p_item_id in ('board_river_valley','board_frost_citadel','board_obsidian')
    else false
  end
$$;

alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v4;
alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v5;
alter table public.shop_items add constraint shop_items_runtime_sale_guard_v5 check (
  (active = false)
  or ((kind = 'game_theme') and (id = any (array['theme_black','theme_walnut_ivory'])))
  or ((kind = 'keyboard_theme') and (id = any (array['keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold','keyboard_premium_white','keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple'])))
  or ((kind = 'name_style') and (id = any (array['name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset'])))
  or ((kind = 'victory_effect') and (id = 'victory_crown'))
  or ((kind = 'mascot_hat') and (id = any (array['hat_wizard','hat_beret','hat_flower','hat_top'])))
  or ((kind = 'profile_frame') and (diamond_price > 0) and (id = any (array['frame_round_starter_blue','frame_round_starter_neutral','frame_round_pearl','frame_round_ocean','frame_round_rose','frame_round_lilac','frame_round_botanic'])))
  or ((kind = 'board_skin') and (id = any (array['board_river_valley','board_frost_citadel','board_obsidian'])))
);

insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order, rarity, metadata)
values
  ('board_river_valley', 'board_skin', 'Vadi Nehri Tahtası', 'River Valley Board',
   'Kuşatma tahtası yeşil bir vadide, çevresinde akan bir dereyle kurulur.', 'The siege board sits in a green valley with a stream running around it.',
   3600, false, true, 400, 'EPIC', '{"economy_tier":"epic"}'::jsonb),
  ('board_frost_citadel', 'board_skin', 'Kar Kalesi Tahtası', 'Frost Citadel Board',
   'Kuşatma tahtası karlı bir dağ kalesine taşınır.', 'The siege board moves to a snowy mountain citadel.',
   3800, false, true, 410, 'EPIC', '{"economy_tier":"epic"}'::jsonb),
  ('board_obsidian', 'board_skin', 'Obsidyen Kale Tahtası', 'Obsidian Bastion Board',
   'Kuşatma tahtası gece karanlığında, altın işlemeli bir burçta kurulur.', 'The siege board is set in a gold-inlaid bastion at night.',
   4200, false, true, 420, 'EPIC', '{"economy_tier":"epic"}'::jsonb)
on conflict (id) do update set
  kind = excluded.kind, name_tr = excluded.name_tr, name_en = excluded.name_en,
  description_tr = excluded.description_tr, description_en = excluded.description_en,
  diamond_price = excluded.diamond_price, active = true, sort_order = excluded.sort_order,
  rarity = excluded.rarity, metadata = coalesce(public.shop_items.metadata, '{}'::jsonb) || excluded.metadata;
