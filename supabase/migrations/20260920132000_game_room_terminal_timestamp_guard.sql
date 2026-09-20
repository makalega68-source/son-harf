-- Terminal Son Harf rooms must always carry a completion timestamp regardless of which
-- matchmaking, maintenance, admin, recovery, or gameplay RPC closes the room.
create or replace function public.ensure_game_room_terminal_timestamp_v1()
returns trigger
language plpgsql
set search_path to 'public', 'pg_temp'
as $function$
begin
  if new.status in ('finished','cancelled') and new.finished_at is null then
    new.finished_at := now();
  end if;
  return new;
end
$function$;

drop trigger if exists trg_game_room_terminal_timestamp_v1 on public.game_rooms;
create trigger trg_game_room_terminal_timestamp_v1
before insert or update of status, finished_at on public.game_rooms
for each row
execute function public.ensure_game_room_terminal_timestamp_v1();

-- Historical terminal rows predate the invariant. Exact cleanup time is not recoverable,
-- so use the latest recorded room activity instead of fabricating a current completion time.
update public.game_rooms
set finished_at = greatest(
  created_at,
  coalesce(last_action_at, created_at),
  coalesce(host_last_seen_at, created_at),
  coalesce(guest_last_seen_at, created_at)
)
where status in ('finished','cancelled')
  and finished_at is null;
