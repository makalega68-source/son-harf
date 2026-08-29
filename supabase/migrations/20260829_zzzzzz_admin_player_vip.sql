-- Admin player search + VIP management (2026-08-29)

create or replace function public.admin_search_players_v1(p_query text)
returns table(
  user_id uuid,
  email text,
  display_name text,
  is_vip boolean,
  diamonds integer,
  rating integer,
  last_seen_at timestamptz,
  is_owner_account boolean
)
language plpgsql
security definer
set search_path=public,auth,pg_temp
as $$
declare
  q text:=lower(trim(coalesce(p_query,'')));
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if char_length(q) < 2 then return; end if;
  return query
  select p.id,u.email::text,p.display_name,p.is_vip,p.diamonds,p.rating,p.last_seen_at,
         exists(select 1 from public.owner_game_accounts o where o.user_id=p.id and o.active)
  from public.profiles p
  join auth.users u on u.id=p.id
  where lower(coalesce(u.email,'')) like '%'||q||'%'
     or lower(p.display_name) like '%'||q||'%'
  order by
    case when lower(coalesce(u.email,''))=q or lower(p.display_name)=q then 0 else 1 end,
    p.last_seen_at desc nulls last
  limit 20;
end
$$;

create or replace function public.admin_set_player_vip_v1(p_user_id uuid, p_enabled boolean)
returns jsonb
language plpgsql
security definer
set search_path=public,auth,pg_temp
as $$
declare
  v_before boolean;
  v_email text;
  v_owner_locked boolean;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select p.is_vip,u.email::text into v_before,v_email
  from public.profiles p join auth.users u on u.id=p.id
  where p.id=p_user_id;
  if not found then raise exception 'user_not_found'; end if;

  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=p_user_id and o.active and o.lifetime_vip
  ) into v_owner_locked;

  if v_owner_locked and not coalesce(p_enabled,false) then
    raise exception 'owner_lifetime_vip_locked';
  end if;

  update public.profiles
  set is_vip=coalesce(p_enabled,false),updated_at=now()
  where id=p_user_id;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(
    auth.uid(),'set_player_vip','profile',p_user_id::text,
    jsonb_build_object('email',v_email,'is_vip',v_before),
    jsonb_build_object('email',v_email,'is_vip',coalesce(p_enabled,false))
  );

  return jsonb_build_object('success',true,'user_id',p_user_id,'is_vip',coalesce(p_enabled,false));
end
$$;

revoke all on function public.admin_search_players_v1(text) from public,anon;
revoke all on function public.admin_set_player_vip_v1(uuid,boolean) from public,anon;
grant execute on function public.admin_search_players_v1(text) to authenticated;
grant execute on function public.admin_set_player_vip_v1(uuid,boolean) to authenticated;

select pg_notify('pgrst','reload schema');
