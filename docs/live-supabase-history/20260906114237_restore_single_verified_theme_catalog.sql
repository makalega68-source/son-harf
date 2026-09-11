-- AUDIT-ONLY LIVE SNAPSHOT. DO NOT EXECUTE FROM THIS PATH.
-- Live version: 20260906114237
-- Live name: restore_single_verified_theme_catalog

update public.shop_items set active = false where id = 'theme_aurora' and kind = 'game_theme';
