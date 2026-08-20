create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text := btrim(coalesce(new.raw_user_meta_data->>'display_name',''));
  v_gender text := lower(btrim(coalesce(new.raw_user_meta_data->>'gender','')));
  v_valid_gender text;
begin
  v_valid_gender := case v_gender
    when 'kadin' then 'kadın'
    when 'diger' then 'diğer'
    when 'kadın' then 'kadın'
    when 'diğer' then 'diğer'
    when 'erkek' then 'erkek'
    else null
  end;
  insert into public.profiles (id, display_name, gender, account_email, identity_locked)
  values (
    new.id,
    case when length(v_name) between 2 and 24 then v_name else 'Oyuncu-' || upper(substr(replace(new.id::text, '-', ''), 1, 4)) end,
    v_valid_gender,
    lower(new.email),
    (length(v_name) between 2 and 24 and v_valid_gender is not null)
  )
  on conflict (id) do update set
    display_name = case when coalesce(public.profiles.identity_locked,false) then public.profiles.display_name else excluded.display_name end,
    gender = case when coalesce(public.profiles.identity_locked,false) then public.profiles.gender else excluded.gender end,
    account_email = coalesce(public.profiles.account_email, excluded.account_email),
    identity_locked = public.profiles.identity_locked or excluded.identity_locked,
    updated_at = now();
  return new;
end;
$$;

create or replace function public.complete_profile_identity_v2(p_display_name text, p_gender text)
returns public.profiles
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_email text := lower(coalesce(auth.jwt()->>'email',''));
  v_gender text := lower(btrim(p_gender));
  v_row public.profiles;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select * into v_row from public.profiles where id=v_uid for update;
  if not found then raise exception 'profile_not_found'; end if;
  if coalesce(v_row.identity_locked,false) then return v_row; end if;
  if length(btrim(p_display_name)) < 2 or length(btrim(p_display_name)) > 24 then raise exception 'invalid_display_name'; end if;
  if v_gender not in ('erkek','kadın','kadin','diğer','diger') then raise exception 'invalid_gender'; end if;
  update public.profiles set
    display_name=btrim(p_display_name),
    gender=case v_gender when 'kadin' then 'kadın' when 'diger' then 'diğer' else v_gender end,
    account_email=nullif(v_email,''),
    identity_locked=true,
    updated_at=now()
  where id=v_uid returning * into v_row;
  return v_row;
end;
$$;

revoke all on function public.complete_profile_identity_v2(text,text) from public, anon;
grant execute on function public.complete_profile_identity_v2(text,text) to authenticated;
