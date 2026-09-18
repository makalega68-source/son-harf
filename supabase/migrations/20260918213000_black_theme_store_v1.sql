-- Black Theme v1
-- Cosmetic only: no score, rating, economy or gameplay-power effect.
-- Black Theme is a separate product; legacy arena themes never grant it automatically.

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

-- Old themes remain as historical ownership metadata only.
update public.shop_items
set active = false
where id in ('theme_dark_arena', 'theme_monster_blue', 'theme_aurora', 'theme_neon', 'theme_midnight')
  and kind = 'game_theme';

-- Retired themes must not remain visually equipped. Ownership rows are preserved.
update public.user_equipped_cosmetics
set game_theme_id = null
where game_theme_id in ('theme_dark_arena', 'theme_monster_blue', 'theme_aurora', 'theme_neon', 'theme_midnight');
