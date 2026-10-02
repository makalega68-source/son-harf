-- Server clock metadata prevents an offline client cache extending the weekly reward.
create or replace function public.get_public_profile_frame_v2(p_user_id uuid)
returns table(user_id uuid,profile_frame_id text,expires_at timestamptz,server_time timestamptz)
language sql stable security invoker set search_path='' as $$
 select f.user_id,f.profile_frame_id,
 case when f.profile_frame_id='frame_throne_champion' then
 (date_trunc('week',now() at time zone 'Europe/Istanbul')+interval '7 days') at time zone 'Europe/Istanbul'
 else null end,now()
 from public.get_public_profile_frame_v1(p_user_id) f
$$;
revoke all on function public.get_public_profile_frame_v2(uuid) from public,anon;
grant execute on function public.get_public_profile_frame_v2(uuid) to authenticated;
