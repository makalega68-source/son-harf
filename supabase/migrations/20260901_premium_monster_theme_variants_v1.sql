-- Premium color variants for the purchased Monster layout.
-- Cosmetic only: no score, rating, timer, joker or match-power benefits.

insert into public.shop_items(
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
) values
(
  'theme_monster_charcoal_ivory',
  'game_theme',
  'Füme Fildişi',
  'Charcoal Ivory',
  'Koyu füme yüzeyler, krem-fildişi tipografi ve şampanya vurgularıyla premium Monster renk paketi.',
  'Premium Monster color pack with charcoal surfaces, ivory typography and champagne accents.',
  750,
  false,
  true,
  36
),
(
  'theme_monster_sapphire_ice',
  'game_theme',
  'Safir Buz',
  'Sapphire Ice',
  'Derin safir yüzeyler, buz beyazı tipografi ve camgöbeği vurgularıyla premium Monster renk paketi.',
  'Premium Monster color pack with deep sapphire surfaces, ice-white typography and glacial cyan accents.',
  900,
  false,
  true,
  37
)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = excluded.diamond_price,
  vip_only = excluded.vip_only,
  active = true,
  sort_order = excluded.sort_order;

-- Keep the three purchased-layout variants active. Older retired themes remain retired.
update public.shop_items
set active = false
where kind = 'game_theme'
  and id not in (
    'theme_monster_blue',
    'theme_monster_charcoal_ivory',
    'theme_monster_sapphire_ice'
  );
