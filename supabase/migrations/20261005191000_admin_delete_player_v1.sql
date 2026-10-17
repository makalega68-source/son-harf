-- Admin panel: permanently delete a player account. Never the caller, never an admin, never a
-- club owner (clubs.owner_id is ON DELETE RESTRICT). The e-mail also leaves the moderator list,
-- so a re-registration starts as a normal new player. Everything else cascades or is nulled.
create or replace function public.admin_delete_player_v1(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'auth', 'pg_temp'
as $function$
declare
  v_email text;
  v_name text;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_user_id is null then raise exception 'player_required'; end if;
  if p_user_id = auth.uid() then raise exception 'cannot_delete_self'; end if;
  if exists (select 1 from public.admin_users where user_id = p_user_id) then
    raise exception 'remove_admin_first';
  end if;
  if exists (select 1 from public.clubs c where c.owner_id = p_user_id) then
    raise exception 'player_owns_club';
  end if;
  select lower(u.email), p.display_name into v_email, v_name
  from auth.users u left join public.profiles p on p.id = u.id
  where u.id = p_user_id;
  if v_email is null and v_name is null then raise exception 'player_not_found'; end if;

  insert into public.admin_audit_log(admin_id, action, target_type, target_id, before_data, after_data)
  values (auth.uid(), 'player_delete_v1', 'player', p_user_id::text,
          jsonb_build_object('email', v_email, 'display_name', v_name), null);

  -- A re-registration with the same e-mail must start as a normal new player.
  delete from public.moderator_accounts where email = v_email;
  delete from auth.users where id = p_user_id;
end;
$function$;

revoke all on function public.admin_delete_player_v1(uuid) from public, anon;
grant execute on function public.admin_delete_player_v1(uuid) to authenticated;
