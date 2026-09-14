-- Premium cosmetic refresh: retire the neon keyboard, keep historical ownership intact,
-- and expose only runtime-supported presentation products. No match state or score is changed.

update public.shop_items
set active = false
where id = 'keyboard_neon' and kind = 'keyboard_theme';

-- Do not delete legacy purchases. Only clear the removed skin from the active presentation slot.
update public.user_equipped_cosmetics
set keyboard_theme_id = null, updated_at = now()
where keyboard_theme_id = 'keyboard_neon';

insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order)
values
  ('name_cyan', 'name_style', 'Siyan İmza', 'Cyan Signature', 'Sakin siyan tonuyla rafine profil imzası.', 'A refined profile signature in calm cyan.', 220, false, true, 30),
  ('name_sapphire', 'name_style', 'Safir İmza', 'Sapphire Signature', 'Derin safir vurgulu prestijli isim stili.', 'A prestigious name style with a deep sapphire accent.', 260, false, true, 31),
  ('name_amethyst', 'name_style', 'Ametist İmza', 'Amethyst Signature', 'Yumuşak ametist tonunda seçkin profil imzası.', 'An elevated profile signature in soft amethyst.', 280, false, true, 32),
  ('name_aurelia', 'name_style', 'Aurelia İmza', 'Aurelia Signature', 'Şampanya altını tonunda sınırlı prestij imzası.', 'A limited prestige signature in champagne gold.', 340, false, true, 33),
  ('keyboard_crystal', 'keyboard_theme', 'Kristal Tuşlar', 'Crystal Keys', 'Yüksek kontrastlı, berrak kristal klavye görünümü.', 'A high-contrast, clear crystal keyboard presentation.', 360, false, true, 36),
  ('keyboard_obsidian', 'keyboard_theme', 'Obsidyen Tuşlar', 'Obsidian Keys', 'Füme siyah ve şampanya altınıyla rafine klavye görünümü.', 'A refined keyboard in graphite black and champagne gold.', 420, false, true, 37)
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
