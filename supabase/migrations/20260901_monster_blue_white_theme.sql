-- Purchased Monster layout: retire previous game themes and sell the new blue-white palette.
update public.shop_items
set active = false
where kind = 'game_theme'
  and id <> 'theme_monster_blue';

insert into public.shop_items(
  id,
  kind,
  name_tr,
  name_en,
  description_tr,
  description_en,
  diamond_price,
  vip_only,
  active,
  sort_order
) values (
  'theme_monster_blue',
  'game_theme',
  'Mavi Beyaz Arena',
  'Blue White Arena',
  'Yeni Monster düzeninin premium beyaz-mavi renk paketi.',
  'Premium blue-white color pack for the new Monster layout.',
  600,
  false,
  true,
  35
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
