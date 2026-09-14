-- Club creation RPC boundary hardening.
-- The public endpoint is the only callable contract for a client. The internal
-- helper remains SECURITY DEFINER but is no longer exposed through PostgREST.

create or replace function public.create_club_v1(
  p_name text,
  p_tag text,
  p_description text default ''
)
returns uuid
language sql
security definer
set search_path=''
as $$
  select private.create_club_v1(p_name, p_tag, p_description);
$$;

revoke all on function private.create_club_v1(text, text, text)
  from public, anon, authenticated;

revoke all on function public.create_club_v1(text, text, text)
  from public, anon;
grant execute on function public.create_club_v1(text, text, text)
  to authenticated;
