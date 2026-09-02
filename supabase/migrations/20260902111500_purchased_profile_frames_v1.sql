-- Purchased 2D Avatar Frame pack: curated adult/premium variants for Son Harf.
-- Cosmetic only: these items grant no score, timer, rating, joker or matchmaking advantage.

update public.shop_items
set active = false
where id in ('frame_neon', 'frame_gold');

insert into public.shop_items(
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
) values
  ('frame_asset_gold', 'profile_frame', 'Altın Hat', 'Gold Line',
   'Sıcak metalik profil çerçevesi.', 'Warm metallic profile frame.', 260, false, true, 110),
  ('frame_asset_mint', 'profile_frame', 'Buz Mint', 'Ice Mint',
   'Temiz ve modern mint profil çerçevesi.', 'Clean modern mint profile frame.', 220, false, true, 120),
  ('frame_asset_purple', 'profile_frame', 'Mor Spektrum', 'Violet Spectrum',
   'Premium mor profil vurgusu.', 'Premium violet profile accent.', 280, false, true, 130),
  ('frame_asset_green', 'profile_frame', 'Zümrüt Hat', 'Emerald Line',
   'Dengeli zümrüt profil çerçevesi.', 'Balanced emerald profile frame.', 240, false, true, 140)
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
