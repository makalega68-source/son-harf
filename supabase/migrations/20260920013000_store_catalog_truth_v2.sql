-- Store catalog truth v2
-- Keep Google Play verification fail-closed against an explicit catalog and retire stale bundles.

begin;

-- Every currently sellable Google Play product is explicitly enabled. This removes
-- implicit allow-by-absence behavior once verify-play-purchase is updated to fail closed.
insert into public.store_catalog_config(product_id, enabled, badge_tr, badge_en, sort_order, updated_at)
values
  ('vip_monthly', true, null, null, 10, now()),
  ('vip_yearly', true, 'EN AVANTAJLI', 'BEST VALUE', 11, now()),
  ('season_pass_monthly', true, null, null, 12, now()),
  ('series_game', true, null, null, 13, now()),
  ('letter_table', true, null, null, 14, now()),
  ('score_calculator', true, null, null, 15, now()),
  ('pro_lifetime', true, null, null, 16, now()),
  ('coins_500', true, 'MINI', 'MINI', 20, now()),
  ('coins_1500', true, 'EN POPÜLER', 'POPULAR', 21, now()),
  ('coins_3500', true, 'EN İYİ DEĞER', 'BEST VALUE', 22, now()),
  ('coins_8000', true, 'MEGA', 'MEGA', 23, now())
on conflict (product_id) do update
set enabled = excluded.enabled,
    sort_order = excluded.sort_order,
    updated_at = now();

-- Historical product IDs remain recognizable in receipts/grant tables, but they are
-- not sellable in the current client generation. Keeping rows (rather than deleting
-- grants) preserves auditability and existing server-side ownership.
insert into public.store_catalog_config(product_id, enabled, badge_tr, badge_en, sort_order, updated_at)
values
  ('season_pass', false, null, null, 90, now()),
  ('starter_style_pack', false, null, null, 91, now()),
  ('premium_style_pack', false, null, null, 92, now()),
  ('season_pack', false, null, null, 93, now()),
  ('vip_welcome_pack', false, null, null, 94, now()),
  ('theme_neon', false, null, null, 95, now()),
  ('profile_frame_ocean', false, null, null, 100, now()),
  ('profile_frame_botanic', false, null, null, 101, now()),
  ('profile_frame_lilac', false, null, null, 102, now()),
  ('profile_frame_rose', false, null, null, 103, now()),
  ('profile_frame_pink_blossom', false, null, null, 104, now()),
  ('profile_frame_blue_royal', false, null, null, 105, now()),
  ('profile_frame_amethyst', false, null, null, 106, now()),
  ('profile_frame_emerald', false, null, null, 107, now())
on conflict (product_id) do update
set enabled = false,
    updated_at = now();

-- Database-level sale guard: an operator or future migration cannot accidentally
-- re-enable a historical/unknown Play product without explicitly changing this guard.
alter table public.store_catalog_config
  drop constraint if exists store_catalog_enabled_product_guard_v2;

alter table public.store_catalog_config
  add constraint store_catalog_enabled_product_guard_v2
  check (
    enabled = false
    or product_id in (
      'vip_monthly',
      'vip_yearly',
      'season_pass_monthly',
      'series_game',
      'letter_table',
      'score_calculator',
      'pro_lifetime',
      'coins_500',
      'coins_1500',
      'coins_3500',
      'coins_8000'
    )
  );

-- Bundles may stay in history, but an offer is not active if any member is no longer
-- an active, currently saleable shop item. The purchase RPC already rejects such a
-- bundle; this also makes the persisted catalog truthful instead of relying on UI hiding.
update public.store_bundles b
set active = false
where b.active = true
  and exists (
    select 1
    from unnest(b.item_ids) as item_id
    where not exists (
      select 1
      from public.shop_items s
      where s.id = item_id
        and s.active = true
        and s.kind in ('profile_frame', 'name_style', 'keyboard_theme', 'game_theme')
        and s.available_from <= now()
        and (s.available_until is null or s.available_until > now())
    )
  );

commit;
