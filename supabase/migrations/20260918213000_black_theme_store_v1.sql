-- Black Theme v1
-- Cosmetic only: no score, rating, economy or gameplay-power effect.
-- Replaces obsolete visible arena-theme products while preserving prior ownership.

insert into public.shop_items(
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
)
values (
  'theme_black',
  'game_theme',
  'Black Theme',
  'Black Theme',
  'Siyah ve grafit yüzeyler; elektrik mavi, turkuaz ve eflatun premium vurgular.',
  'Black and graphite surfaces with electric blue, turquoise and purple premium accents.',
  600,
  false,
  true,
  40
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

-- Old theme rows remain as historical metadata only and are no longer offered for sale.
update public.shop_items
set active = false
where id in ('theme_dark_arena', 'theme_monster_blue', 'theme_aurora')
  and kind = 'game_theme';

-- Existing Night Arena owners receive Black Theme so paid ownership is never lost.
insert into public.user_inventory(user_id, item_id)
select user_id, 'theme_black'
from public.user_inventory
where item_id = 'theme_dark_arena'
on conflict do nothing;

-- If the retired theme was equipped, migrate the slot to the real Black Theme.
update public.user_equipped_cosmetics
set game_theme_id = 'theme_black'
where game_theme_id = 'theme_dark_arena';
