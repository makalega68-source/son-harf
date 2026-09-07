-- Idempotent Google Play RTDN receipt ledger. Service-role only.

create table if not exists public.play_rtdn_events (
  message_id text primary key,
  event_type text not null,
  purchase_token text,
  order_id text,
  event_time timestamptz,
  received_at timestamptz not null default now(),
  processed_at timestamptz,
  processing_error text
);
alter table public.play_rtdn_events enable row level security;
revoke all on public.play_rtdn_events from public,anon,authenticated;
grant select,insert,update on public.play_rtdn_events to service_role;

create or replace function public.claim_play_rtdn_event_v1(
  p_message_id text,
  p_event_type text,
  p_purchase_token text default null,
  p_order_id text default null,
  p_event_time timestamptz default null
)
returns boolean
language plpgsql
security definer
set search_path=''
as $$
declare v_inserted text;
begin
  if nullif(trim(p_message_id),'') is null then raise exception 'invalid_message_id'; end if;
  if nullif(trim(p_event_type),'') is null then raise exception 'invalid_event_type'; end if;
  insert into public.play_rtdn_events(message_id,event_type,purchase_token,order_id,event_time)
  values(trim(p_message_id),trim(p_event_type),nullif(trim(p_purchase_token),''),nullif(trim(p_order_id),''),p_event_time)
  on conflict(message_id) do nothing
  returning message_id into v_inserted;
  return v_inserted is not null;
end $$;
revoke all on function public.claim_play_rtdn_event_v1(text,text,text,text,timestamptz) from public,anon,authenticated;
grant execute on function public.claim_play_rtdn_event_v1(text,text,text,text,timestamptz) to service_role;

create or replace function public.finish_play_rtdn_event_v1(p_message_id text,p_error text default null)
returns void
language sql
security definer
set search_path=''
as $$
  update public.play_rtdn_events
  set processed_at=now(), processing_error=nullif(left(coalesce(p_error,''),500),'')
  where message_id=trim(p_message_id);
$$;
revoke all on function public.finish_play_rtdn_event_v1(text,text) from public,anon,authenticated;
grant execute on function public.finish_play_rtdn_event_v1(text,text) to service_role;
