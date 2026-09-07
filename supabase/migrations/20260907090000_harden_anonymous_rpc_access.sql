-- Restore least-privilege RPC access after later CREATE OR REPLACE statements
-- reintroduced PostgreSQL's default PUBLIC execute grant.

revoke all on function public.apply_bomb_duel_deadline_v1() from public, anon, authenticated;
revoke all on function public.claim_daily_goal_v10(text) from public, anon;
revoke all on function public.claim_turn_timeout_v2(uuid, uuid, timestamptz) from public, anon;
revoke all on function public.display_name_available(text) from public, anon;
revoke all on function public.get_daily_goals_v10() from public, anon;
revoke all on function public.get_dictionary_snapshot_v1(text) from public, anon;
revoke all on function public.get_dictionary_snapshot_v2(text) from public, anon;
revoke all on function public.get_dictionary_snapshot_v3(text) from public, anon;
revoke all on function public.get_vip_friendships() from public, anon;
revoke all on function public.grant_default_mascot_inventory() from public, anon, authenticated;
revoke all on function public.grant_legend_profile_frame_v1() from public, anon, authenticated;
revoke all on function public.pause_private_room(uuid) from public, anon;
revoke all on function public.reject_terminal_soft_g_game_word() from public, anon, authenticated;
revoke all on function public.resume_private_room(uuid) from public, anon;
revoke all on function public.set_display_name(text) from public, anon;
revoke all on function public.v7_profiles_xp_bonus_trigger() from public, anon, authenticated;

grant execute on function public.claim_daily_goal_v10(text) to authenticated, service_role;
grant execute on function public.claim_turn_timeout_v2(uuid, uuid, timestamptz) to authenticated, service_role;
grant execute on function public.display_name_available(text) to authenticated, service_role;
grant execute on function public.get_daily_goals_v10() to authenticated, service_role;
grant execute on function public.get_dictionary_snapshot_v1(text) to authenticated, service_role;
grant execute on function public.get_dictionary_snapshot_v2(text) to authenticated, service_role;
grant execute on function public.get_dictionary_snapshot_v3(text) to authenticated, service_role;
grant execute on function public.get_vip_friendships() to authenticated, service_role;
grant execute on function public.pause_private_room(uuid) to authenticated, service_role;
grant execute on function public.resume_private_room(uuid) to authenticated, service_role;
grant execute on function public.set_display_name(text) to authenticated, service_role;

grant execute on function public.apply_bomb_duel_deadline_v1() to service_role;
grant execute on function public.grant_default_mascot_inventory() to service_role;
grant execute on function public.grant_legend_profile_frame_v1() to service_role;
grant execute on function public.reject_terminal_soft_g_game_word() to service_role;
grant execute on function public.v7_profiles_xp_bonus_trigger() to service_role;
