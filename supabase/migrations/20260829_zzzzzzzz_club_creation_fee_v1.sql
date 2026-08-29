-- Club Creation Fee v1
-- Social anti-spam gate only. This purchase does not grant match power,
-- rating, league, tournament, club-score, or any other competitive advantage.

create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create or replace function private.create_club_v1(
  p_name text,
  p_tag text,
  p_description text default ''
)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_name text:=btrim(coalesce(p_name,''));
  v_tag text:=upper(btrim(coalesce(p_tag,'')));
  v_description text:=btrim(coalesce(p_description,''));
  v_club uuid;
  v_balance integer;
  v_cost constant integer:=1000;
begin
  if v_uid is null then
    raise exception 'unauthorized';
  end if;

  if char_length(v_name) not between 3 and 24 then
    raise exception 'invalid_club_name';
  end if;
  if char_length(v_tag) not between 2 and 6
     or v_tag !~ '^[A-Z0-9ÇĞİÖŞÜ]+$'
  then
    raise exception 'invalid_club_tag';
  end if;
  if char_length(v_description)>180 then
    raise exception 'description_too_long';
  end if;

  select p.diamonds
  into v_balance
  from public.profiles p
  where p.id=v_uid
  for update;

  if not found then
    raise exception 'profile_not_found';
  end if;

  if exists(
    select 1
    from public.club_members cm
    where cm.user_id=v_uid
  ) then
    raise exception 'already_in_club';
  end if;

  if coalesce(v_balance,0)<v_cost then
    raise exception 'insufficient_club_creation_balance';
  end if;

  insert into public.clubs(name,tag,description,owner_id)
  values(v_name,v_tag,v_description,v_uid)
  returning id into v_club;

  insert into public.club_members(club_id,user_id,role)
  values(v_club,v_uid,'owner');

  update public.profiles
  set diamonds=diamonds-v_cost
  where id=v_uid;

  insert into public.diamond_ledger(user_id,delta,reason,item_id)
  values(v_uid,-v_cost,'club_creation:'||v_club::text,null);

  return v_club;
exception
  when unique_violation then
    if exists(
      select 1
      from public.club_members cm
      where cm.user_id=v_uid
    ) then
      raise exception 'already_in_club';
    end if;
    raise exception 'club_name_or_tag_taken';
end
$$;

create or replace function public.create_club_v1(
  p_name text,
  p_tag text,
  p_description text default ''
)
returns uuid
language sql
security invoker
set search_path=''
as $$
  select private.create_club_v1(p_name,p_tag,p_description);
$$;

revoke all on function private.create_club_v1(text,text,text)
  from public,anon,authenticated;
grant execute on function private.create_club_v1(text,text,text)
  to authenticated;

revoke all on function public.create_club_v1(text,text,text)
  from public,anon;
grant execute on function public.create_club_v1(text,text,text)
  to authenticated;
