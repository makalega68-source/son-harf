-- The "first five sign-ins" live-test programme is closed: new accounts start as normal
-- players. Emails already on the moderator list keep their access.
create or replace function public.enroll_first_moderator_internal_v1(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_email text;
begin
  select lower(email) into v_email from auth.users where id = p_user_id;
  if v_email is null or v_email in ('makalega68@gmail.com', 'makalega58@gmail.com') then return false; end if;
  if exists (select 1 from public.moderator_accounts where email = v_email) then
    perform public.grant_moderator_access_internal_v1(p_user_id);
    return true;
  end if;
  return false;
end;
$function$;
