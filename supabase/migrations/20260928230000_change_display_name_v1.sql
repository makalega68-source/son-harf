-- Players can change their player name (it was locked forever at sign-up).
--  * 2-24 characters: letters (Turkish included), digits, space, dot, dash, underscore.
--  * Unique among players (same comparison as at sign-up), not the placeholder "Oyuncu".
--  * At most once every 24 hours.
-- Gender and e-mail stay locked; only change_display_name_v1 may touch a locked name.

alter table public.profiles add column if not exists display_name_changed_at timestamptz;

create or replace function public.protect_locked_profile_identity()
returns trigger
language plpgsql
set search_path to 'public'
as $$
begin
  if old.identity_locked and (
    (new.display_name is distinct from old.display_name
       and coalesce(current_setting('app.identity_rename', true), '') <> 'on') or
    new.gender is distinct from old.gender or
    new.account_email is distinct from old.account_email or
    new.identity_locked is distinct from old.identity_locked
  ) then
    raise exception 'identity_locked';
  end if;
  return new;
end $$;

create or replace function public.change_display_name_v1(p_name text)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_name text := trim(regexp_replace(coalesce(p_name, ''), '\s+', ' ', 'g'));
  v_last timestamptz;
  v_current text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if char_length(v_name) not between 2 and 24 then raise exception 'invalid_display_name'; end if;
  if v_name !~ '^[A-Za-z0-9ÇĞİÖŞÜçğıöşüÂâÎîÛû ._-]+$' then raise exception 'invalid_display_name_chars'; end if;
  if lower(v_name) = 'oyuncu' or lower(v_name) like 'oyuncu-%' then raise exception 'invalid_display_name'; end if;

  select display_name, display_name_changed_at into v_current, v_last from public.profiles where id = v_uid for update;
  if not found then raise exception 'profile_not_found'; end if;
  if v_current = v_name then
    return jsonb_build_object('changed', false, 'display_name', v_current);
  end if;
  if v_last is not null and v_last > now() - interval '24 hours' then
    raise exception 'display_name_cooldown';
  end if;
  if exists (
    select 1 from public.profiles p
    where p.id <> v_uid and public.normalized_display_name(p.display_name) = public.normalized_display_name(v_name)
  ) then
    raise exception 'display_name_taken';
  end if;

  perform set_config('app.identity_rename', 'on', true);
  update public.profiles
  set display_name = v_name, display_name_changed_at = now(), updated_at = now()
  where id = v_uid;
  perform set_config('app.identity_rename', 'off', true);
  return jsonb_build_object('changed', true, 'display_name', v_name);
end $$;

revoke all on function public.change_display_name_v1(text) from public, anon;
grant execute on function public.change_display_name_v1(text) to authenticated;
