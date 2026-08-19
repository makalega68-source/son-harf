-- Private profile-photo storage. Files use <user_uuid>/avatar.jpg and <user_uuid>/thumb.jpg.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('profile-photos','profile-photos',false,5242880,array['image/jpeg','image/png','image/webp'])
on conflict(id) do update set public=false,file_size_limit=5242880,allowed_mime_types=excluded.allowed_mime_types;

create or replace function public.can_view_profile_photo(p_owner uuid,p_viewer uuid)
returns boolean language sql stable security definer set search_path=public as $$
  select p_owner=p_viewer
  or exists(select 1 from public.profile_photo_access a where a.owner_id=p_owner and a.viewer_id=p_viewer)
  or exists(
    select 1 from public.profiles p
    where p.id=p_owner and (
      (p.avatar_visibility='vip' and exists(select 1 from public.profiles v where v.id=p_viewer and v.is_vip=true))
      or (p.avatar_visibility='match' and exists(
        select 1 from public.game_rooms r where r.status in ('playing','quiz','final','sudden_death')
          and ((r.host_id=p_owner and r.guest_id=p_viewer) or (r.guest_id=p_owner and r.host_id=p_viewer))
      ))
      or (p.avatar_visibility='selected' and exists(select 1 from public.profile_photo_access a where a.owner_id=p_owner and a.viewer_id=p_viewer))
    )
  );
$$;
grant execute on function public.can_view_profile_photo(uuid,uuid) to authenticated;

drop policy if exists profile_photos_owner_insert on storage.objects;
create policy profile_photos_owner_insert on storage.objects for insert to authenticated
with check(bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists profile_photos_owner_update on storage.objects;
create policy profile_photos_owner_update on storage.objects for update to authenticated
using(bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text)
with check(bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists profile_photos_owner_delete on storage.objects;
create policy profile_photos_owner_delete on storage.objects for delete to authenticated
using(bucket_id='profile-photos' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists profile_photos_controlled_read on storage.objects;
create policy profile_photos_controlled_read on storage.objects for select to authenticated
using(bucket_id='profile-photos' and public.can_view_profile_photo(((storage.foldername(name))[1])::uuid,auth.uid()));
