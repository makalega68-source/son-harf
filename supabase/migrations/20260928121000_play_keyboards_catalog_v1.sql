-- The Google Play keyboards may be verified by verify-play-purchase.
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v4;
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v5;
alter table public.store_catalog_config add constraint store_catalog_enabled_product_guard_v5 check (enabled = false or product_id = any (array[
  'vip_monthly','vip_yearly','season_pass_monthly','series_game','letter_table','score_calculator','pro_lifetime',
  'coins_500','coins_1500','coins_3500','coins_8000',
  'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik','mascot_tekir','mascot_robot','mascot_astronot',
  'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst','profile_frame_pink_blossom','profile_frame_blue_royal',
  'keyboard_black_gold','keyboard_crystal','keyboard_midnight','keyboard_obsidian'
]));
insert into public.store_catalog_config(product_id, enabled, sort_order)
values ('keyboard_black_gold', true, 60), ('keyboard_crystal', true, 61), ('keyboard_midnight', true, 62), ('keyboard_obsidian', true, 63)
on conflict (product_id) do update set enabled = true, updated_at = now();
