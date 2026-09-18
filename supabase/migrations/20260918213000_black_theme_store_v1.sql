-- Black Theme v1
-- Cosmetic only: no score, rating, economy or gameplay-power effect.
-- Reuses theme_dark_arena as the canonical server/store key for published-client compatibility.

insert into public.shop_items(
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
)
values (
  'theme_dark_arena',
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

-- Retire alternate/experimental themes from the active storefront.
update public.shop_items
set active = false
where id in ('theme_black', 'theme_monster_blue', 'theme_aurora', 'theme_neon', 'theme_midnight')
  and kind = 'game_theme';

-- Preserve ownership from any earlier test build that used theme_black.
insert into public.user_inventory(user_id, item_id)
select user_id, 'theme_dark_arena'
from public.user_inventory
where item_id = 'theme_black'
on conflict do nothing;

update public.user_equipped_cosmetics
set game_theme_id = 'theme_dark_arena'
where game_theme_id = 'theme_black';
