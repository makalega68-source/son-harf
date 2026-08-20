-- SON HARF action/streak system
-- Server-authoritative bonuses and durable action events for both players.

alter table public.game_rooms
  add column if not exists action_seq bigint not null default 0,
  add column if not exists last_action_streak integer,
  add column if not exists last_action_bonus integer,
  add column if not exists last_action_player_id uuid references public.profiles(id) on delete set null,
  add column if not exists last_action_is_bot boolean not null default false,
  add column if not exists last_action_at timestamptz;

create or replace function public.sonharf_action_bonus(p_streak integer)
returns integer
language sql
immutable
set search_path=public,pg_temp
as $$
  select case
    when p_streak between 3 and 5 then 5
    when p_streak between 6 and 7 then 10
    when p_streak between 8 and 9 then 15
    when p_streak >= 10 then 25
    else 0
  end;
$$;

create or replace function public.trg_sonharf_action_streak()
returns trigger
language plpgsql
set search_path=public,pg_temp
as $$
declare
  v_streak integer;
  v_bonus integer;
  v_existing_increment integer;
  v_target_increment integer;
begin
  -- A round boundary does not break a correct-answer streak. Only an actual
  -- failed word / timeout should reset the streak (those updates keep round_no unchanged).
  if new.round_no > old.round_no then
    if new.host_streak = 0 and old.host_streak > 0 then new.host_streak := old.host_streak; end if;
    if new.guest_streak = 0 and old.guest_streak > 0 then new.guest_streak := old.guest_streak; end if;
  end if;

  if new.host_streak = old.host_streak + 1 then
    v_streak := new.host_streak;
    v_bonus := public.sonharf_action_bonus(v_streak);
    v_existing_increment := new.host_score - old.host_score;
    v_target_increment := 3 + v_bonus;
    new.host_score := old.host_score + v_target_increment;
    new.host_round_score := old.host_round_score + v_target_increment;

    if v_streak >= 3 then
      new.action_seq := old.action_seq + 1;
      new.last_action_streak := v_streak;
      new.last_action_bonus := v_bonus;
      new.last_action_player_id := new.host_id;
      new.last_action_is_bot := false;
      new.last_action_at := now();
    end if;

  elsif new.guest_streak = old.guest_streak + 1 then
    v_streak := new.guest_streak;
    v_bonus := public.sonharf_action_bonus(v_streak);
    v_existing_increment := new.guest_score - old.guest_score;
    v_target_increment := 3 + v_bonus;
    new.guest_score := old.guest_score + v_target_increment;
    new.guest_round_score := old.guest_round_score + v_target_increment;

    if v_streak >= 3 then
      new.action_seq := old.action_seq + 1;
      new.last_action_streak := v_streak;
      new.last_action_bonus := v_bonus;
      new.last_action_player_id := case when new.is_bot then null else new.guest_id end;
      new.last_action_is_bot := new.is_bot;
      new.last_action_at := now();
    end if;
  end if;

  return new;
end;
$$;

drop trigger if exists trg_sonharf_action_streak on public.game_rooms;
create trigger trg_sonharf_action_streak
before update on public.game_rooms
for each row execute function public.trg_sonharf_action_streak();

revoke all on function public.sonharf_action_bonus(integer) from public;
revoke all on function public.trg_sonharf_action_streak() from public;
grant execute on function public.sonharf_action_bonus(integer) to authenticated;
