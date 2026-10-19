-- Cikcik (budgie) joins the mascot shop as a permanent Google Play one-time product.
-- Every allowlist that names the sellable mascots gains 'mascot_cikcik'.
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v5;
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v6;
alter table public.store_catalog_config add constraint store_catalog_enabled_product_guard_v6
  check (enabled = false or product_id = any (array[
    'vip_monthly','vip_yearly','season_pass_monthly','series_game','letter_table','score_calculator','pro_lifetime',
    'coins_500','coins_1500','coins_3500','coins_8000',
    'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik',
    'mascot_tekir','mascot_robot','mascot_astronot','mascot_cikcik',
    'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
    'profile_frame_pink_blossom','profile_frame_blue_royal',
    'keyboard_black_gold','keyboard_crystal','keyboard_midnight','keyboard_obsidian'
  ]));

insert into public.store_catalog_config(product_id, enabled, badge_tr, badge_en, sort_order, updated_at)
values ('mascot_cikcik', true, 'MASKOT', 'MASCOT', 37, now())
on conflict (product_id) do update
set enabled = excluded.enabled, badge_tr = excluded.badge_tr, badge_en = excluded.badge_en,
    sort_order = excluded.sort_order, updated_at = now();

-- Add the product to the function allowlists without retyping their bodies.
do $$
declare d text;
begin
  for d in
    select pg_get_functiondef(p.oid) from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname in (
      'apply_verified_premium_purchase_v1', 'mascot_id_is_playable_v1', 'grant_moderator_access_internal_v1')
  loop
    if position('mascot_cikcik' in d) = 0 then
      execute replace(d, '''mascot_astronot''', '''mascot_astronot'', ''mascot_cikcik''');
    end if;
  end loop;
  select pg_get_functiondef(p.oid) into d from pg_proc p join pg_namespace n on n.oid = p.pronamespace
  where n.nspname = 'public' and p.proname = 'ensure_mascot_progress_v1';
  if d is not null and position('mascot_cikcik' in d) = 0 then
    execute replace(d, 'when ''mascot_astronot'' then ''Nova''', 'when ''mascot_astronot'' then ''Nova'' when ''mascot_cikcik'' then ''Cikcik''');
  end if;
end $$;
