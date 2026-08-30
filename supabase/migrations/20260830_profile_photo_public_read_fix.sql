-- Fix public profile-photo visibility for duel and competition surfaces.
create or replace function public.can_view_profile_photo(p_owner uuid,p_viewer uuid)
returns boolean
language sql
stable
security definer
set search_path=public
as $$
  select p_owner=p_viewer
  or exists(
    select 1
    from public.profiles p
    where p.id=p_owner
      and p.avatar_visibility='public'
  )
  or exists(
    select 1
    from public.profile_photo_access a
    where a.owner_id=p_owner and a.viewer_id=p_viewer
  )
  or exists(
    select 1
    from public.profiles p
    where p.id=p_owner and (
      (p.avatar_visibility='vip' and exists(
        select 1 from public.profiles v where v.id=p_viewer and v.is_vip=true
      ))
      or (p.avatar_visibility='match' and exists(
        select 1 from public.game_rooms r
        where r.status in ('playing','quiz','final','sudden_death')
          and ((r.host_id=p_owner and r.guest_id=p_viewer)
            or (r.guest_id=p_owner and r.host_id=p_viewer))
      ))
      or (p.avatar_visibility='selected' and exists(
        select 1 from public.profile_photo_access a
        where a.owner_id=p_owner and a.viewer_id=p_viewer
      ))
    )
  );
$$;

grant execute on function public.can_view_profile_photo(uuid,uuid) to authenticated;
select pg_notify('pgrst','reload schema');
