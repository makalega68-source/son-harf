-- Sign-up fix. ensure_profile_frame_default_v1 ran BEFORE INSERT on profiles and inserted the
-- default frame into user_inventory before the profile row existed, so the inventory foreign key
-- failed and every new account was rejected ("Unknown Error" on KAYIT OL).
-- The BEFORE trigger now only fills the column; the frame is added to the inventory AFTER the
-- profile row exists. Moderator grants can never block a sign-up either.

create or replace function public.ensure_profile_frame_default_v1()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
begin
  new.default_profile_frame_id := coalesce(new.default_profile_frame_id, public.profile_frame_default_v1(new.gender));
  if tg_op = 'UPDATE' then
    insert into public.user_inventory(user_id, item_id) values (new.id, new.default_profile_frame_id) on conflict do nothing;
  end if;
  return new;
end;
$$;

create or replace function public.grant_profile_frame_default_inventory_v1()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
begin
  if new.default_profile_frame_id is not null then
    insert into public.user_inventory(user_id, item_id) values (new.id, new.default_profile_frame_id) on conflict do nothing;
  end if;
  return new;
end;
$$;
drop trigger if exists trg_grant_profile_frame_default_inventory_v1 on public.profiles;
create trigger trg_grant_profile_frame_default_inventory_v1
after insert on public.profiles
for each row execute function public.grant_profile_frame_default_inventory_v1();

create or replace function public.moderator_profile_grant_v1()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  begin
    perform public.enroll_first_moderator_internal_v1(new.id);
    perform public.grant_moderator_access_internal_v1(new.id);
  exception when others then
    raise warning 'moderator grant skipped for %: %', new.id, sqlerrm;
  end;
  return new;
end;
$$;

-- Frames for profiles created while the bug was live (none should be missing, but make sure).
insert into public.user_inventory(user_id, item_id)
select p.id, p.default_profile_frame_id from public.profiles p
where p.default_profile_frame_id is not null
on conflict do nothing;
