revoke execute on function public.has_pro_access_v1(uuid) from anon, public;
revoke execute on function public.get_premium_entitlements_v2() from anon, public;
revoke execute on function public.get_series_friendships_v2() from anon, public;
grant execute on function public.has_pro_access_v1(uuid) to authenticated, service_role;
grant execute on function public.get_premium_entitlements_v2() to authenticated, service_role;
grant execute on function public.get_series_friendships_v2() to authenticated, service_role;
