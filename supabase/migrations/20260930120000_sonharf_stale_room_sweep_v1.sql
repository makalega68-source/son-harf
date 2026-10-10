-- Son Harf: the server finishes or advances abandoned live duels.
--
-- A human Son Harf duel only moved on when a client claimed the timeout. When both players left,
-- the room stayed 'playing' forever (e.g. 90237b7a-…: its reconnect deadline passed an hour
-- earlier) and enforce_single_active_room() then kept both players out of every new match.
--
-- Every minute this sweep looks at human rooms whose turn or reconnect deadline passed more than
-- five seconds ago (live clients claim at the deadline itself, so it never races them) and:
--   1. clears a disconnect mark when that player is heartbeating again (heartbeat_room() only
--      clears it when the *opponent* is present);
--   2. like heartbeat_room(), marks the player whose turn expired as disconnected (usual 60-second
--      grace) when they have sent no heartbeat for 30 seconds, so an absent player's match ends by
--      reconnect_timeout instead of passing the turn back and forth forever;
--   3. runs the unchanged public.claim_turn_timeout(): it passes an expired turn, or finishes the
--      match once the reconnect deadline passed. That function checks auth.uid(), which a cron job
--      has none of, so the sweep acts as the waiting participant through the transaction-local
--      request.jwt.claim.sub setting (reset afterwards). No game rule is copied here.
-- Bot rooms are left alone: returning players resume them through resume_premier_bot_match_v1,
-- which must not be charged for offline time.

create or replace function private.sweep_sonharf_stale_rooms_v1()
returns integer
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $$
declare
  r public.game_rooms;
  v_now timestamptz;
  v_actor uuid;
  v_disconnected_seen timestamptz;
  v_current_seen timestamptz;
  v_count integer := 0;
begin
  for r in
    select g.*
    from public.game_rooms g
    where g.status in ('playing', 'final', 'sudden_death')
      and not coalesce(g.is_bot, false)
      and g.guest_id is not null
      and (
        g.turn_deadline < clock_timestamp() - interval '5 seconds'
        or (g.disconnected_player_id is not null
            and g.reconnect_deadline < clock_timestamp() - interval '5 seconds')
      )
    order by coalesce(g.reconnect_deadline, g.turn_deadline)
    for update skip locked
  loop
    begin
      v_now := clock_timestamp();

      -- 1. The "disconnected" player is back: clear the mark (as heartbeat_room does).
      if r.disconnected_player_id is not null then
        v_disconnected_seen := case when r.disconnected_player_id = r.host_id
          then r.host_last_seen_at else r.guest_last_seen_at end;
        if v_disconnected_seen >= v_now - interval '12 seconds' then
          update public.game_rooms
          set disconnected_player_id = null,
              reconnect_deadline = null,
              last_event = 'player_reconnected'
          where id = r.id
          returning * into r;
        end if;
      end if;

      -- 2. The expired turn belongs to a player who is gone: start the usual reconnect grace.
      if r.disconnected_player_id is null
         and r.current_player_id is not null
         and r.turn_deadline < v_now - interval '5 seconds' then
        v_current_seen := case when r.current_player_id = r.host_id
          then r.host_last_seen_at else r.guest_last_seen_at end;
        if v_current_seen is null or v_current_seen < v_now - interval '30 seconds' then
          update public.game_rooms
          set disconnected_player_id = r.current_player_id,
              reconnect_deadline = v_now + interval '60 seconds',
              last_event = 'player_disconnected'
          where id = r.id
          returning * into r;
          v_count := v_count + 1;
          continue;
        end if;
      end if;

      -- 3. Pass the expired turn / finish after the reconnect deadline, with the live rules.
      v_actor := case when r.current_player_id = r.host_id then r.guest_id else r.host_id end;
      perform set_config('request.jwt.claim.sub', v_actor::text, true);
      perform public.claim_turn_timeout(r.id);
      perform set_config('request.jwt.claim.sub', '', true);
      v_count := v_count + 1;
    exception when others then
      perform set_config('request.jwt.claim.sub', '', true);
      raise warning 'sweep_sonharf_stale_rooms_v1 room %: %', r.id, sqlerrm;
    end;
  end loop;

  return v_count;
end
$$;

revoke all on function private.sweep_sonharf_stale_rooms_v1() from public, anon, authenticated;

do $$
begin
  if exists (select 1 from pg_extension where extname = 'pg_cron') then
    if exists (select 1 from cron.job where jobname = 'sonharf_stale_room_sweep_v1') then
      perform cron.unschedule('sonharf_stale_room_sweep_v1');
    end if;
    perform cron.schedule(
      'sonharf_stale_room_sweep_v1',
      '* * * * *',
      'select private.sweep_sonharf_stale_rooms_v1();'
    );
  end if;
end
$$;
