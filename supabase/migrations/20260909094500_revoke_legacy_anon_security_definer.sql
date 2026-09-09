-- Security hardening: remove unnecessary anonymous execution from legacy SECURITY DEFINER RPCs.
-- Current clients use get_dictionary_snapshot_v4, which is SECURITY INVOKER and authenticated-only.
-- Keep authenticated/service_role access for backwards compatibility with older signed-in clients.

revoke execute on function public.equip_default_game_theme() from public, anon;
grant execute on function public.equip_default_game_theme() to authenticated, service_role;

revoke execute on function public.get_dictionary_snapshot_v3(text) from public, anon;
grant execute on function public.get_dictionary_snapshot_v3(text) to authenticated, service_role;
alter function public.get_dictionary_snapshot_v3(text)
  set search_path = pg_catalog, public, pg_temp;

select pg_notify('pgrst', 'reload schema');
