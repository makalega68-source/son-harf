-- Restore authenticated access to the existing duel recovery RPCs, then advance
-- bot rooms that were stranded by a lost one-shot client callback.
revoke all on function public.claim_turn_timeout(uuid) from public, anon;
grant execute on function public.claim_turn_timeout(uuid) to authenticated;

revoke all on function public.bot_take_turn(uuid) from public, anon;
grant execute on function public.bot_take_turn(uuid) to authenticated;

revoke all on function public.forfeit_room(uuid) from public, anon;
grant execute on function public.forfeit_room(uuid) to authenticated;

do $$
declare
  stuck_room record;
begin
  for stuck_room in
    select id
    from public.game_rooms
    where is_bot
      and bot_turn
      and status in ('playing','final','sudden_death')
    order by created_at
  loop
    perform public.bot_take_turn(stuck_room.id);
  end loop;
end
$$;

select pg_notify('pgrst','reload schema');
