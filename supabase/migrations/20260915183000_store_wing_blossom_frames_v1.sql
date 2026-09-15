-- Wing + pink blossom profile frames v1.
-- Visual-only cosmetics sold with Son Coin through the existing server-authoritative purchase RPC.
-- Golden PRO remains a separate entitlement and is intentionally untouched.

insert into public.shop_items (
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order, available_from, available_until
)
values
  ('frame_wing_silver', 'profile_frame', 'Gümüş Kanat', 'Silver Wings',
   'Metalik gümüş kanatlı profil çerçevesi.', 'Metallic silver winged profile frame.',
   180, false, true, 530, now(), null),
  ('frame_wing_blue', 'profile_frame', 'Mavi Kanat', 'Blue Wings',
   'Buz mavisi kanatlı profil çerçevesi.', 'Icy blue winged profile frame.',
   240, false, true, 531, now(), null),
  ('frame_flower_pink_blossom', 'profile_frame', 'Pembe Çiçek Çelengi', 'Pink Blossom Wreath',
   'Pembe çiçekli sarmaşık profil çerçevesi.', 'Pink blossom vine profile frame.',
   220, false, true, 532, now(), null),
  ('frame_wing_pink', 'profile_frame', 'Pembe Kanat', 'Pink Wings',
   'Pembe ve beyaz kanatlı profil çerçevesi.', 'Pink and white winged profile frame.',
   240, false, true, 533, now(), null),
  ('frame_wing_gold', 'profile_frame', 'Altın Kanat', 'Golden Wings',
   'Parlak altın kanatlı profil çerçevesi.', 'Bright gold winged profile frame.',
   260, false, true, 534, now(), null),
  ('frame_wing_aurora', 'profile_frame', 'Aurora Kanat', 'Aurora Wings',
   'Renk geçişli özel kanatlı profil çerçevesi.', 'Premium multicolor winged profile frame.',
   320, false, true, 535, now(), null)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = excluded.diamond_price,
  vip_only = false,
  active = true,
  sort_order = excluded.sort_order,
  available_from = least(coalesce(public.shop_items.available_from, excluded.available_from), excluded.available_from),
  available_until = null;

select pg_notify('pgrst', 'reload schema');
