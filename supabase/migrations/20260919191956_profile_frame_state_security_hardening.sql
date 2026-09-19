-- Profile frame state must respect caller RLS and must never be exposed to anonymous clients.
alter view public.profile_frame_state_v1 set (security_invoker = true);
revoke all on public.profile_frame_state_v1 from anon;
grant select on public.profile_frame_state_v1 to authenticated, service_role;

select pg_notify('pgrst','reload schema');
