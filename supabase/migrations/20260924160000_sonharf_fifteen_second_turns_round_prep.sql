-- Son Harf turns: a steady 15-second clock and a 20-second break before every new round.
--
-- * The legacy Bomb Duel trigger (game_rooms_bomb_duel_deadline_v1) still clamped every turn
--   deadline longer than 11 s down to 10 s in all rooms, although no Bomb Duel rooms exist any
--   more. Players saw the clock start at 15 and jump to 10. The trigger is removed.
-- * Every round uses the same 15-second turn (previously 15 / 13 / 11).
-- * When a new round (or sudden death) starts, the first turn deadline gets 20 extra seconds of
--   preparation. Bot rooms whose new round starts with the bot are paused by the client.

begin;

drop trigger if exists game_rooms_bomb_duel_deadline_v1 on public.game_rooms;

create or replace function public.sonharf_turn_seconds_for_round_v1(p_round_no integer)
returns integer
language sql
immutable
set search_path to 'pg_catalog'
as $$
  select 15
$$;

create or replace function public.sonharf_round_prep_deadline_v1()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
begin
  if new.turn_deadline is not null
     and new.status in ('playing', 'final', 'sudden_death')
     and (
       new.round_no > old.round_no
       or (new.status = 'sudden_death' and old.status is distinct from 'sudden_death')
     ) then
    new.turn_deadline := new.turn_deadline + interval '20 seconds';
  end if;
  return new;
end
$$;

revoke all on function public.sonharf_round_prep_deadline_v1() from public, anon, authenticated;

drop trigger if exists game_rooms_round_prep_v1 on public.game_rooms;
create trigger game_rooms_round_prep_v1
before update of round_no, status, turn_deadline on public.game_rooms
for each row execute function public.sonharf_round_prep_deadline_v1();

commit;
