alter table public.profiles add column if not exists account_email text;
alter table public.profiles add column if not exists gender text;
alter table public.profiles add column if not exists identity_locked boolean not null default false;
create unique index if not exists profiles_account_email_unique on public.profiles (lower(account_email)) where account_email is not null;

create or replace function public.complete_profile_identity(p_display_name text, p_gender text, p_email text)
returns public.profiles language plpgsql security definer set search_path=public as $$
declare v_uid uuid:=auth.uid(); v_row public.profiles;
begin
 if v_uid is null then raise exception 'not_authenticated'; end if;
 select * into v_row from public.profiles where id=v_uid for update;
 if not found then raise exception 'profile_not_found'; end if;
 if coalesce(v_row.identity_locked,false) then raise exception 'identity_locked'; end if;
 if length(btrim(p_display_name))<2 or length(btrim(p_display_name))>24 then raise exception 'invalid_display_name'; end if;
 if lower(btrim(p_gender)) not in ('erkek','kadın','kadin','diğer','diger') then raise exception 'invalid_gender'; end if;
 if p_email is null or p_email !~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$' then raise exception 'invalid_email'; end if;
 update public.profiles set display_name=btrim(p_display_name), gender=case lower(btrim(p_gender)) when 'kadin' then 'kadın' when 'diger' then 'diğer' else lower(btrim(p_gender)) end, account_email=lower(btrim(p_email)), identity_locked=true, updated_at=now() where id=v_uid returning * into v_row;
 return v_row;
exception when unique_violation then raise exception 'email_already_used';
end $$;
grant execute on function public.complete_profile_identity(text,text,text) to authenticated;

create or replace function public.set_avatar_visibility(p_hidden boolean)
returns public.profiles language plpgsql security definer set search_path=public as $$
declare v_row public.profiles; begin
 if auth.uid() is null then raise exception 'not_authenticated'; end if;
 update public.profiles set avatar_visibility=case when p_hidden then 'hidden' else 'public' end,updated_at=now() where id=auth.uid() returning * into v_row; return v_row;
end $$;
grant execute on function public.set_avatar_visibility(boolean) to authenticated;

create or replace function public.set_avatar_path(p_path text)
returns public.profiles language plpgsql security definer set search_path=public as $$
declare v_row public.profiles; begin
 if auth.uid() is null then raise exception 'not_authenticated'; end if;
 if p_path is null or p_path not like auth.uid()::text||'/%' then raise exception 'invalid_avatar_path'; end if;
 update public.profiles set avatar_path=p_path,avatar_thumb_path=p_path,avatar_updated_at=now(),updated_at=now() where id=auth.uid() returning * into v_row; return v_row;
end $$;
grant execute on function public.set_avatar_path(text) to authenticated;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('avatars','avatars',false,524288,array['image/webp','image/jpeg','image/png'])
on conflict(id) do update set public=false,file_size_limit=524288,allowed_mime_types=array['image/webp','image/jpeg','image/png'];

drop policy if exists avatars_owner_select on storage.objects;
drop policy if exists avatars_owner_insert on storage.objects;
drop policy if exists avatars_owner_update on storage.objects;
drop policy if exists avatars_owner_delete on storage.objects;
create policy avatars_owner_select on storage.objects for select to authenticated using(bucket_id='avatars' and (storage.foldername(name))[1]=auth.uid()::text);
create policy avatars_owner_insert on storage.objects for insert to authenticated with check(bucket_id='avatars' and (storage.foldername(name))[1]=auth.uid()::text);
create policy avatars_owner_update on storage.objects for update to authenticated using(bucket_id='avatars' and (storage.foldername(name))[1]=auth.uid()::text) with check(bucket_id='avatars' and (storage.foldername(name))[1]=auth.uid()::text);
create policy avatars_owner_delete on storage.objects for delete to authenticated using(bucket_id='avatars' and (storage.foldername(name))[1]=auth.uid()::text);
