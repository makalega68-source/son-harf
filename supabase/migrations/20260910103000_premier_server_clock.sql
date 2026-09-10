-- Keep the visible 20-second Premier countdown aligned with the authoritative
-- database clock. Device wall clocks can differ from the server by several
-- seconds, so the client anchors its countdown from this server-derived value.
create or replace function public.get_premier_turn_clock_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
  r public.game_rooms;
  remaining_ms bigint := 0;
begin
  if u is null then raise exception 'unauthorized'; end if;

  select * into r
  from public.game_rooms
  where id = p_room_id;

  if r.id is null then raise exception 'room_not_found'; end if;
  if u <> r.host_id and u is distinct from r.guest_id then raise exception 'not_participant'; end if;

  if r.turn_deadline is not null then
    remaining_ms := greatest(
      0,
      floor(extract(epoch from (r.turn_deadline - clock_timestamp())) * 1000)::bigint
    );
  end if;

  return jsonb_build_object(
    'remaining_ms', remaining_ms,
    'server_now', clock_timestamp(),
    'turn_deadline', r.turn_deadline
  );
end;
$$;

revoke all on function public.get_premier_turn_clock_v1(uuid) from public, anon;
grant execute on function public.get_premier_turn_clock_v1(uuid) to authenticated, service_role;

select pg_notify('pgrst', 'reload schema');
