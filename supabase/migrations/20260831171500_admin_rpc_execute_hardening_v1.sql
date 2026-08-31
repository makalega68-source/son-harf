-- Explicitly remove anonymous/public execution from all admin entry points.
-- Authenticated execution remains required by the app; every admin RPC enforces
-- public.is_admin() server-side before returning privileged data or mutating state.

revoke all on function public.admin_access_v1() from public, anon;
revoke all on function public.admin_capacity_v1() from public, anon;
revoke all on function public.admin_clear_chat_suspension(uuid) from public, anon;
revoke all on function public.admin_close_room(uuid,text) from public, anon;
revoke all on function public.admin_dashboard_v1() from public, anon;
revoke all on function public.admin_game_controls_v1() from public, anon;
revoke all on function public.admin_get_announcement_v1() from public, anon;
revoke all on function public.admin_grant_test_product_v1(text) from public, anon;
revoke all on function public.admin_health_v1() from public, anon;
revoke all on function public.admin_monthly_revenue_v1() from public, anon;
revoke all on function public.admin_owner_accounts_v1() from public, anon;
revoke all on function public.admin_repair_v1(text) from public, anon;
revoke all on function public.admin_run_maintenance() from public, anon;
revoke all on function public.admin_search_players_v1(text) from public, anon;
revoke all on function public.admin_set_announcement_v1(text,boolean) from public, anon;
revoke all on function public.admin_set_config(text,jsonb) from public, anon;
revoke all on function public.admin_set_free_purchases_v1(boolean) from public, anon;
revoke all on function public.admin_set_my_vip_v1(boolean) from public, anon;
revoke all on function public.admin_set_owner_account_v1(text,boolean,boolean,boolean,boolean) from public, anon;
revoke all on function public.admin_set_platform_plan_v1(text) from public, anon;
revoke all on function public.admin_set_player_vip_v1(uuid,boolean) from public, anon;
revoke all on function public.admin_set_product_price_v1(text,bigint,text) from public, anon;
revoke all on function public.admin_system_health() from public, anon;
revoke all on function public.admin_top_products_v1() from public, anon;
revoke all on function public.admin_top_store_items_v1() from public, anon;
revoke all on function public.is_admin() from public, anon;

-- Trigger functions are never legitimate client RPC entry points.
revoke all on function public.enforce_new_game_admin_controls_v1() from public, anon, authenticated;
revoke all on function public.enforce_chat_admin_control_v1() from public, anon, authenticated;

-- Preserve only the roles that need the application-facing admin calls.
grant execute on function public.admin_access_v1() to authenticated, service_role;
grant execute on function public.admin_capacity_v1() to authenticated, service_role;
grant execute on function public.admin_clear_chat_suspension(uuid) to authenticated, service_role;
grant execute on function public.admin_close_room(uuid,text) to authenticated, service_role;
grant execute on function public.admin_dashboard_v1() to authenticated, service_role;
grant execute on function public.admin_game_controls_v1() to authenticated, service_role;
grant execute on function public.admin_get_announcement_v1() to authenticated, service_role;
grant execute on function public.admin_grant_test_product_v1(text) to authenticated, service_role;
grant execute on function public.admin_health_v1() to authenticated, service_role;
grant execute on function public.admin_monthly_revenue_v1() to authenticated, service_role;
grant execute on function public.admin_owner_accounts_v1() to authenticated, service_role;
grant execute on function public.admin_repair_v1(text) to authenticated, service_role;
grant execute on function public.admin_run_maintenance() to authenticated, service_role;
grant execute on function public.admin_search_players_v1(text) to authenticated, service_role;
grant execute on function public.admin_set_announcement_v1(text,boolean) to authenticated, service_role;
grant execute on function public.admin_set_config(text,jsonb) to authenticated, service_role;
grant execute on function public.admin_set_free_purchases_v1(boolean) to authenticated, service_role;
grant execute on function public.admin_set_my_vip_v1(boolean) to authenticated, service_role;
grant execute on function public.admin_set_owner_account_v1(text,boolean,boolean,boolean,boolean) to authenticated, service_role;
grant execute on function public.admin_set_platform_plan_v1(text) to authenticated, service_role;
grant execute on function public.admin_set_player_vip_v1(uuid,boolean) to authenticated, service_role;
grant execute on function public.admin_set_product_price_v1(text,bigint,text) to authenticated, service_role;
grant execute on function public.admin_system_health() to authenticated, service_role;
grant execute on function public.admin_top_products_v1() to authenticated, service_role;
grant execute on function public.admin_top_store_items_v1() to authenticated, service_role;
grant execute on function public.is_admin() to authenticated, service_role;
