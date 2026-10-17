-- Admins wear the admin crest (the weekly throne frame still wins) and every client can
-- tell who the admins are, so a small admin badge shows beside their names.
create or replace function public.get_public_profile_frame_v1(p_user_id uuid)
returns table(user_id uuid, profile_frame_id text)
language sql stable security definer set search_path to ''
as $function$
 select p.id,
  case when private.throne_champion_at(now())=p.id then 'frame_throne_champion'
       when exists(select 1 from public.admin_users a where a.user_id=p.id and a.role='admin') then 'frame_admin'
       else s.id end
 from public.profiles p
 left join public.user_equipped_cosmetics e on e.user_id=p.id
 left join public.shop_items s on s.id=e.profile_frame_id and s.kind='profile_frame'
 where auth.uid() is not null and p.id=p_user_id limit 1
$function$;

create or replace function public.get_admin_ids_v1()
returns setof uuid
language sql stable security definer set search_path to ''
as $function$
 select a.user_id from public.admin_users a where auth.uid() is not null and a.role='admin'
$function$;
revoke all on function public.get_admin_ids_v1() from public, anon;
grant execute on function public.get_admin_ids_v1() to authenticated;
