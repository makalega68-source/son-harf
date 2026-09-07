-- Reduce exposed SECURITY DEFINER RPC surface without changing gameplay semantics.
-- Trigger functions never need REST/API EXECUTE grants. Authenticated player RPCs reject anon at the privilege layer.

-- Trigger-only functions: keep executable by database trigger machinery, not API roles.
revoke all on function public.apply_bomb_duel_deadline_v1() from public,anon,authenticated;
revoke all on function public.grant_default_mascot_inventory() from public,anon,authenticated;
revoke all on function public.grant_legend_profile_frame_v1() from public,anon,authenticated;
revoke all on function public.reject_terminal_soft_g_game_word() from public,anon,authenticated;
revoke all on function public.v7_profiles_xp_bonus_trigger() from public,anon,authenticated;
grant execute on function public.apply_bomb_duel_deadline_v1() to service_role;
grant execute on function public.grant_default_mascot_inventory() to service_role;
grant execute on function public.grant_legend_profile_frame_v1() to service_role;
grant execute on function public.reject_terminal_soft_g_game_word() to service_role;
grant execute on function public.v7_profiles_xp_bonus_trigger() to service_role;

-- Authenticated player mutations: their bodies already bind writes to auth.uid(); remove anon exposure too.
revoke all on function public.claim_daily_goal_v10(text) from public,anon;
grant execute on function public.claim_daily_goal_v10(text) to authenticated,service_role;
revoke all on function public.set_display_name(text) from public,anon;
grant execute on function public.set_display_name(text) to authenticated,service_role;
