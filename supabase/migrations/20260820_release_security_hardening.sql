-- Release hardening: the Android client requires an authenticated session.
-- Remove accidental anonymous access to SECURITY DEFINER RPCs while
-- preserving authenticated access used by the game client.

revoke execute on function public.cancel_private_room(uuid) from public, anon;
grant execute on function public.cancel_private_room(uuid) to authenticated;

revoke execute on function public.claim_daily_challenge_v1() from public, anon;
grant execute on function public.claim_daily_challenge_v1() to authenticated;

revoke execute on function public.claim_daily_checkin_v1() from public, anon;
grant execute on function public.claim_daily_checkin_v1() to authenticated;

revoke execute on function public.claim_rewarded_ad(text, text) from public, anon;
grant execute on function public.claim_rewarded_ad(text, text) to authenticated;

revoke execute on function public.claim_vip_monthly_diamonds() from public, anon;
grant execute on function public.claim_vip_monthly_diamonds() to authenticated;

revoke execute on function public.create_room(text) from public, anon;
grant execute on function public.create_room(text) to authenticated;

revoke execute on function public.equip_reward_trial() from public, anon;
grant execute on function public.equip_reward_trial() to authenticated;

revoke execute on function public.equip_shop_item(text) from public, anon;
grant execute on function public.equip_shop_item(text) to authenticated;

revoke execute on function public.get_growth_dashboard_v1() from public, anon;
grant execute on function public.get_growth_dashboard_v1() to authenticated;

revoke execute on function public.get_leaderboard_v2(text, text, integer) from public, anon;
grant execute on function public.get_leaderboard_v2(text, text, integer) to authenticated;

revoke execute on function public.get_reward_center_status() from public, anon;
grant execute on function public.get_reward_center_status() to authenticated;

revoke execute on function public.invite_friend_to_private_room(uuid, uuid) from public, anon;
grant execute on function public.invite_friend_to_private_room(uuid, uuid) to authenticated;

revoke execute on function public.log_app_event_v1(text, text) from public, anon;
grant execute on function public.log_app_event_v1(text, text) to authenticated;

revoke execute on function public.open_reward_chest() from public, anon;
grant execute on function public.open_reward_chest() to authenticated;

revoke execute on function public.purchase_shop_item(text) from public, anon;
grant execute on function public.purchase_shop_item(text) to authenticated;

revoke execute on function public.set_bot_difficulty_v1(text) from public, anon;
grant execute on function public.set_bot_difficulty_v1(text) to authenticated;

revoke execute on function public.set_my_matchmaking_mode(text) from public, anon;
grant execute on function public.set_my_matchmaking_mode(text) to authenticated;

revoke execute on function public.set_room_game_mode(uuid, text) from public, anon;
grant execute on function public.set_room_game_mode(uuid, text) to authenticated;

revoke execute on function public.submit_word_v3(uuid, text) from public, anon;
grant execute on function public.submit_word_v3(uuid, text) to authenticated;

-- Pin helper search paths to remove mutable-search-path warnings and avoid
-- object shadowing through caller-controlled schemas.
alter function public.sonharf_required_suffix(public.game_rooms, text)
  set search_path = public, pg_temp;
alter function public.sonharf_word_allowed(text, text)
  set search_path = public, pg_temp;
alter function public.sonharf_word_multiplier(public.game_rooms)
  set search_path = public, pg_temp;
