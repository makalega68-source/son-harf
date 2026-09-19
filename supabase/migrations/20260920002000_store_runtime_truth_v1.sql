-- Store runtime truth v1
-- Sell only products with a verified runtime counterpart in this client generation.
-- Existing ownership rows are intentionally preserved.

begin;

-- Server-side storefront truth gate. Google Play products (PRO, Series Game,
-- Letter Table, Score Calculator and Son Coin packs) are not stored in shop_items
-- and are therefore unaffected by this allowlist.
update public.shop_items
set active = false
where active = true
  and not (
    (kind = 'game_theme' and id = 'theme_black')
    or (kind = 'keyboard_theme' and id in (
      'keyboard_crystal',
      'keyboard_obsidian',
      'keyboard_midnight',
      'keyboard_black_gold',
      'keyboard_premium_white'
    ))
    or (kind = 'name_style' and id in (
      'name_cyan',
      'name_sapphire',
      'name_amethyst',
      'name_aurelia'
    ))
  );

-- Prevent a future catalog edit from accidentally re-selling an item before its
-- runtime integration is shipped. Extend this allowlist in the same release that
-- adds the corresponding runtime implementation.
alter table public.shop_items
  drop constraint if exists shop_items_runtime_sale_guard_v1;

alter table public.shop_items
  add constraint shop_items_runtime_sale_guard_v1
  check (
    active = false
    or (kind = 'game_theme' and id = 'theme_black')
    or (kind = 'keyboard_theme' and id in (
      'keyboard_crystal',
      'keyboard_obsidian',
      'keyboard_midnight',
      'keyboard_black_gold',
      'keyboard_premium_white'
    ))
    or (kind = 'name_style' and id in (
      'name_cyan',
      'name_sapphire',
      'name_amethyst',
      'name_aurelia'
    ))
  );

-- The launch-season premium track referenced cosmetics that can be granted to
-- inventory but have no usable renderer/equip path in the current runtime.
-- Convert only those unsupported rewards to a real, immediately usable reward.
-- Keeping the same level/track lets previously affected users claim the new
-- reward because season claim identity also includes reward_type/reward_key.
update public.season_store_rewards
set reward_type = 'son_coin',
    reward_key = '',
    amount = 100
where season_id = 'launch-2026'
  and track = 'premium'
  and reward_type in (
    'title',
    'badge',
    'nameplate',
    'word_effect',
    'victory_effect',
    'vs_intro',
    'final_style'
  );

-- PRO must never expose live ranked assistance. Keep every other entitlement
-- exactly as before while making the fair-play contract server-authoritative.
create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  u uuid := auth.uid();
  profile_vip boolean := false;
  pro boolean := false;
  claimed boolean := false;
  w public.vip_joker_wallet;
  score_access boolean := false;
  letter_access boolean := false;
  series_access boolean := false;
begin
  if u is null then
    raise exception 'unauthorized';
  end if;

  select coalesce(is_vip, false)
    into profile_vip
    from public.profiles
   where id = u;

  pro := profile_vip or public.has_permanent_entitlement_v1(u, 'pro_lifetime');
  score_access := pro or public.has_permanent_entitlement_v1(u, 'score_calculator');
  letter_access := pro or public.has_permanent_entitlement_v1(u, 'letter_table');
  series_access := pro or public.has_permanent_entitlement_v1(u, 'series_game');

  insert into public.vip_joker_wallet(user_id)
  values (u)
  on conflict (user_id) do nothing;

  select *
    into w
    from public.vip_joker_wallet
   where user_id = u;

  select exists(
    select 1
      from public.vip_daily_joker_claims
     where user_id = u
       and claim_date = current_date
  ) into claimed;

  return jsonb_build_object(
    'is_vip', profile_vip,
    'is_pro', pro,
    'daily_jokers_claimed', claimed,
    'freezer_count', coalesce(w.freezer_count, 0),
    'swap_count', coalesce(w.swap_count, 0),
    'hint_count', coalesce(w.hint_count, 0),
    'streak_shield_count', coalesce(w.streak_shield_count, 0),
    'multiplier_count', coalesce(w.multiplier_count, 0),
    'xp_multiplier', 1,
    'diamond_multiplier', 1,
    'rewarded_ad_bypass', pro,
    'used_words_access', pro,
    'direct_messages_access', true,
    'ranked_live_assist', false,
    'post_match_analysis', pro,
    'saved_friend_list', pro,
    'private_rooms', pro,
    'score_calculator_access', score_access,
    'letter_table_access', letter_access,
    'series_game_access', series_access,
    'active_game_limit', case when pro then 50 else 10 end
  );
end
$function$;

commit;
