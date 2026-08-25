create or replace function public.refund_eve_ai_free_quota(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_day date := current_date;
begin
  if p_user_id is null then
    return;
  end if;

  perform pg_advisory_xact_lock(hashtext('eve_ai_free_quota:' || v_day::text));

  update public.eve_ai_daily_usage
     set request_count = greatest(request_count - 1, 0),
         updated_at = now()
   where usage_day = v_day
     and user_id = p_user_id;

  delete from public.eve_ai_daily_usage
   where usage_day = v_day
     and user_id = p_user_id
     and request_count <= 0;
end;
$$;

revoke all on function public.refund_eve_ai_free_quota(uuid) from public;
revoke all on function public.refund_eve_ai_free_quota(uuid) from anon;
revoke all on function public.refund_eve_ai_free_quota(uuid) from authenticated;
grant execute on function public.refund_eve_ai_free_quota(uuid) to service_role;
