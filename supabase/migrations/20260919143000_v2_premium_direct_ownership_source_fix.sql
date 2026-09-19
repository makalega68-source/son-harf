-- Direct ownership means an individually verified Google Play purchase.
-- PRO bundle-derived effective access must never be rendered as SATIN ALINDI.
create or replace function public.get_premium_entitlements_v2()
returns jsonb
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
  profile_vip boolean := false;
  pro boolean := false;
  series_direct boolean := false;
  letter_direct boolean := false;
  score_direct boolean := false;
  claimed boolean := false;
  w public.vip_joker_wallet;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into profile_vip from public.profiles where id=u;
  pro := public.has_permanent_entitlement_v1(u,'pro_lifetime');

  select exists(select 1 from public.store_entitlements where user_id=u and entitlement_key='series_game' and source_type='play' and status='active' and (expires_at is null or expires_at>now())) into series_direct;
  select exists(select 1 from public.store_entitlements where user_id=u and entitlement_key='letter_table' and source_type='play' and status='active' and (expires_at is null or expires_at>now())) into letter_direct;
  select exists(select 1 from public.store_entitlements where user_id=u and entitlement_key='score_calculator' and source_type='play' and status='active' and (expires_at is null or expires_at>now())) into score_direct;

  insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
  select * into w from public.vip_joker_wallet where user_id=u;
  select exists(select 1 from public.vip_daily_joker_claims where user_id=u and claim_date=current_date) into claimed;

  return jsonb_build_object(
    'is_vip',profile_vip,
    'is_pro',pro,
    'series_game_direct_owned',series_direct,
    'letter_table_direct_owned',letter_direct,
    'score_calculator_direct_owned',score_direct,
    'series_game_access',series_direct or pro,
    'letter_table_access',letter_direct or pro,
    'score_calculator_access',score_direct or pro,
    'daily_jokers_claimed',claimed,
    'freezer_count',coalesce(w.freezer_count,0),
    'swap_count',coalesce(w.swap_count,0),
    'hint_count',coalesce(w.hint_count,0),
    'streak_shield_count',coalesce(w.streak_shield_count,0),
    'multiplier_count',coalesce(w.multiplier_count,0),
    'xp_multiplier',1,
    'diamond_multiplier',1,
    'rewarded_ad_bypass',pro,
    'used_words_access',pro,
    'direct_messages_access',true,
    'ranked_live_assist',false,
    'post_match_analysis',pro,
    'saved_friend_list',pro,
    'private_rooms',pro,
    'active_game_limit',case when pro then 50 else 10 end
  );
end
$$;
grant execute on function public.get_premium_entitlements_v2() to authenticated;
