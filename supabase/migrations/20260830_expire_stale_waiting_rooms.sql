-- Expire abandoned waiting rooms so they cannot be restored as fake live matches.
update public.game_rooms
set status='finished',
    last_event='stale_waiting_expired',
    turn_deadline=null,
    current_player_id=null,
    bot_turn=false
where status='waiting'
  and guest_id is null
  and created_at < now() - interval '2 minutes';

delete from public.matchmaking_queue q
using public.game_rooms r
where q.room_id=r.id
  and r.status='finished'
  and r.last_event='stale_waiting_expired';

select pg_notify('pgrst','reload schema');