-- Server-authoritative Bomb Duel timer.
-- Existing RPCs may still assign legacy 45-second deadlines; this guard clamps every active
-- word turn to 7 seconds, or 5 seconds after tactical hard-ending letters J/V/F.

create or replace function public.apply_bomb_duel_deadline_v1()
returns trigger
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_last_word text;
  v_last_char text;
  v_seconds integer:=7;
begin
  if new.status in ('playing','final','sudden_death')
     and new.turn_deadline is not null
     and new.current_player_id is not null then

    select normalized_word
      into v_last_word
    from public.game_words
    where room_id=new.id
    order by id desc
    limit 1;

    v_last_char:=right(coalesce(v_last_word,''),1);

    if new.language='tr' and v_last_char in ('j','v','f') then
      v_seconds:=5;
    else
      v_seconds:=7;
    end if;

    -- Only replace legacy/overlong deadlines. This preserves a legitimately shorter remaining timer
    -- during unrelated room updates such as heartbeat/presence changes.
    if new.turn_deadline > clock_timestamp() + interval '8 seconds' then
      new.turn_deadline:=clock_timestamp() + make_interval(secs=>v_seconds);
    end if;
  end if;

  return new;
end
$$;

drop trigger if exists game_rooms_bomb_duel_deadline_v1 on public.game_rooms;
create trigger game_rooms_bomb_duel_deadline_v1
before insert or update of status,current_player_id,turn_deadline on public.game_rooms
for each row execute function public.apply_bomb_duel_deadline_v1();

-- Clamp already-active turns so installed clients immediately enter the new high-tempo rules.
update public.game_rooms
set turn_deadline=clock_timestamp()+interval '7 seconds'
where status in ('playing','final','sudden_death')
  and current_player_id is not null
  and turn_deadline is not null
  and turn_deadline > clock_timestamp()+interval '8 seconds';

select pg_notify('pgrst','reload schema');
