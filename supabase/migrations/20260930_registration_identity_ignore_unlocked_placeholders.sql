create or replace function public.complete_profile_identity_v2(p_display_name text, p_gender text)
returns public.profiles
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_uid uuid := auth.uid();
  v_email text;
  v_name text := trim(coalesce(p_display_name, ''));
  v_gender text := lower(trim(coalesce(p_gender, '')));
  v_profile public.profiles%rowtype;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if char_length(v_name) < 2 or char_length(v_name) > 24 then raise exception 'invalid_display_name'; end if;
  if v_gender not in ('kadin','erkek','diger') then raise exception 'invalid_gender'; end if;

  select * into v_profile from public.profiles where id = v_uid for update;

  if v_profile.identity_locked
     and (public.normalized_display_name(v_profile.display_name) <> public.normalized_display_name(v_name)
          or coalesce(v_profile.gender,'') <> v_gender)
  then raise exception 'identity_locked'; end if;

  -- Only finalized identities reserve a player name. Anonymous/unlocked placeholder
  -- profiles must not block a real member from completing registration.
  if exists (
    select 1
    from public.profiles p
    where p.id <> v_uid
      and coalesce(p.identity_locked, false)
      and public.normalized_display_name(p.display_name) = public.normalized_display_name(v_name)
  ) then raise exception 'display_name_taken'; end if;

  select u.email into v_email from auth.users u where u.id = v_uid;

  update public.profiles
  set display_name = v_name,
      gender = v_gender,
      identity_locked = true,
      account_email = coalesce(v_email, account_email),
      updated_at = now()
  where id = v_uid
  returning * into v_profile;

  return v_profile;
end;
$function$;
