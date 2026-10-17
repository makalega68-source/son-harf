-- Test champion: an admin-set override that makes one player the throne owner until ends_at,
-- for testing the weekly gold package. The real weekly ranking is untouched and takes over
-- again when the override ends.
create table if not exists private.throne_test_champion (
  user_id uuid primary key references auth.users(id) on delete cascade,
  ends_at timestamptz not null,
  created_at timestamptz not null default now()
);
revoke all on private.throne_test_champion from public, anon, authenticated;

create or replace function private.throne_champion_at(p_at timestamptz)
returns uuid language sql stable security definer set search_path to ''
as $function$
 select coalesce(
  (select t.user_id from private.throne_test_champion t where p_at < t.ends_at order by t.created_at desc limit 1),
  (select e.user_id from public.throne_xp_events e
    join public.profiles p on p.id=e.user_id
    where e.week_start=date_trunc('week',p_at at time zone 'Europe/Istanbul')::date-7
    group by e.user_id order by sum(e.xp) desc,min(e.earned_at),e.user_id limit 1))
$function$;

create or replace function private.throne_test_champion_ends(p_user_id uuid)
returns timestamptz language sql stable security definer set search_path to ''
as $function$
 select t.ends_at from private.throne_test_champion t where t.user_id=p_user_id and now()<t.ends_at
$function$;
grant execute on function private.throne_test_champion_ends(uuid) to authenticated;

create or replace function public.get_public_profile_frame_v2(p_user_id uuid)
returns table(user_id uuid, profile_frame_id text, expires_at timestamptz, server_time timestamptz)
language sql stable set search_path to ''
as $function$
 select f.user_id,f.profile_frame_id,
  case when f.profile_frame_id='frame_throne_champion' then
   coalesce(private.throne_test_champion_ends(f.user_id),
    (date_trunc('week',now() at time zone 'Europe/Istanbul')+interval '7 days') at time zone 'Europe/Istanbul')
  else null end,now()
 from public.get_public_profile_frame_v1(p_user_id) f
$function$;

-- The owner's own test week (makalega68), started 2026-10-04.
insert into private.throne_test_champion(user_id, ends_at)
values ('88f2f70f-5c18-49ab-8f71-ac468a69f251', timestamptz '2026-10-11 20:35:46+00')
on conflict (user_id) do nothing;
