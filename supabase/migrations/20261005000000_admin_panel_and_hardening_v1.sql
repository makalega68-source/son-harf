-- Admin panel support and security hardening.

-- 1. App crashes and errors go to system_events (which the admin error list already reads).
--    Capped sizes, at most 30 reports per player per day, anonymous callers ignored.
create or replace function public.report_client_error_v1(
  p_kind text, p_message text, p_stack text, p_app_version text, p_device text, p_occurred_at timestamptz)
returns void language plpgsql security definer set search_path to ''
as $function$
declare u uuid := auth.uid();
begin
  if u is null then return; end if;
  if (select count(*) from public.system_events e
      where e.user_id = u and e.source = 'android' and e.created_at > now() - interval '1 day') >= 30 then return; end if;
  insert into public.system_events(severity, source, event_type, user_id, details)
  values ('error', 'android', left(coalesce(nullif(p_kind, ''), 'crash'), 40), u,
          jsonb_build_object('message', left(coalesce(p_message, ''), 2000), 'stack', left(coalesce(p_stack, ''), 12000),
                             'app_version', left(coalesce(p_app_version, ''), 40), 'device', left(coalesce(p_device, ''), 120),
                             'occurred_at', least(coalesce(p_occurred_at, now()), now())));
end $function$;
revoke all on function public.report_client_error_v1(text, text, text, text, text, timestamptz) from public, anon;
grant execute on function public.report_client_error_v1(text, text, text, text, text, timestamptz) to authenticated;

-- 2. Admins can make or remove admins (never the last one, never themselves).
create or replace function public.admin_set_admin_v1(p_user_id uuid, p_enabled boolean)
returns void language plpgsql security definer set search_path to ''
as $function$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_user_id is null or p_user_id = auth.uid() then raise exception 'invalid_target'; end if;
  if p_enabled then
    insert into public.admin_users(user_id, role) values (p_user_id, 'admin')
    on conflict (user_id) do update set role = 'admin';
  else
    delete from public.admin_users where user_id = p_user_id;
    if not exists (select 1 from public.admin_users where role = 'admin') then raise exception 'last_admin'; end if;
  end if;
  insert into public.admin_audit_log(admin_id, action, target_type, target_id, before_data, after_data)
  values (auth.uid(), 'admin_set_admin_v1', 'user', p_user_id::text, null, jsonb_build_object('enabled', p_enabled));
end $function$;
revoke all on function public.admin_set_admin_v1(uuid, boolean) from public, anon;
grant execute on function public.admin_set_admin_v1(uuid, boolean) to authenticated;

-- 3. Admins can make a player the test throne owner for 1-7 days, or end it (0 days).
create or replace function public.admin_set_test_champion_v1(p_user_id uuid, p_days integer)
returns timestamptz language plpgsql security definer set search_path to ''
as $function$
declare v_end timestamptz;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_user_id is null then raise exception 'invalid_target'; end if;
  if coalesce(p_days, 0) <= 0 then
    delete from private.throne_test_champion where user_id = p_user_id;
    v_end := null;
  else
    v_end := now() + make_interval(days => least(p_days, 7));
    insert into private.throne_test_champion(user_id, ends_at) values (p_user_id, v_end)
    on conflict (user_id) do update set ends_at = excluded.ends_at, created_at = now();
  end if;
  insert into public.admin_audit_log(admin_id, action, target_type, target_id, before_data, after_data)
  values (auth.uid(), 'admin_set_test_champion_v1', 'user', p_user_id::text, null, jsonb_build_object('days', p_days));
  return v_end;
end $function$;
revoke all on function public.admin_set_test_champion_v1(uuid, integer) from public, anon;
grant execute on function public.admin_set_test_champion_v1(uuid, integer) to authenticated;

-- 4. Hardening: trigger functions are never called over the API.
revoke execute on function public.grant_profile_frame_default_inventory_v1() from public, anon, authenticated;
revoke execute on function public.moderator_list_grant_v1() from public, anon, authenticated;
revoke execute on function public.moderator_new_item_grant_v1() from public, anon, authenticated;
revoke execute on function public.moderator_profile_grant_v1() from public, anon, authenticated;
-- A fixed search path for the word normaliser (advisor: function_search_path_mutable).
alter function public.normalize_game_word(text, text) set search_path = public, extensions, pg_catalog;
