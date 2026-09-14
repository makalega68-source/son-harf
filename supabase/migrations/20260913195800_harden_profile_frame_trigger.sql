-- Trigger helper is never an RPC surface.
revoke all on function public.ensure_profile_frame_default_v1() from public, anon, authenticated;
grant execute on function public.ensure_profile_frame_default_v1() to service_role;
ëŽ