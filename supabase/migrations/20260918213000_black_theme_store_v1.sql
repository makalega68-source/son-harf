-- Black Theme v1
-- Cosmetic only: no score, rating, economy or gameplay-power effect.
-- Reuses the already-supported theme_dark_arena identifier so older published clients remain compatible.

update public.shop_items
set
  name_tr = 'Black Theme',
  name_en = 'Black Theme',
  description_tr = 'Siyah ve grafit yüzeyler; elektrik mavi, turkuaz ve eflatun premium vurgular.',
  description_en = 'Black and graphite surfaces with electric blue, turquoise and purple premium accents.',
  diamond_price = 600,
  vip_only = false,
  active = true,
  sort_order = 40
where id = 'theme_dark_arena'
  and kind = 'game_theme';

-- Retire every alternate/experimental theme from the active storefront.
update public.shop_items
set active = false
where id in ('theme_black', 'theme_monster_blue', 'theme_aurora', 'theme_neon', 'theme_midnight')
  and kind = 'game_theme';

-- If an earlier test environment ever granted theme_black, preserve that ownership on the canonical id.
insert into public.user_inventory(user_id, item_id)
select user_id, 'theme_dark_arena'
from public.user_inventory
where item_id = 'theme_black'
on conflict do nothing;

update public.user_equipped_cosmetics
set game_theme_id = 'theme_dark_arena'
where game_theme_id = 'theme_black';
