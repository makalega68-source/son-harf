-- Exact duel scoring requested for the live word match.
-- Correct word: +10. Failed move/word: -5.
-- Streak/action metadata remains available for visual feedback but adds no extra score.

create or replace function public.trg_sonharf_action_streak()
returns trigger
language plpgsql
set search_path=public,pg_temp
as $$
declare
  v_streak integer;
begin
  -- A round boundary is not a failed move; preserve an active correct-answer streak.
  if new.round_no > old.round_no then
    if new.host_streak = 0 and old.host_streak > 0 then new.host_streak := old.host_streak; end if;
    if new.guest_streak = 0 and old.guest_streak > 0 then new.guest_streak := old.guest_streak; end if;
  end if;

  if new.host_streak = old.host_streak + 1 then
    v_streak := new.host_streak;
    new.host_score := old.host_score + 10;
    new.host_round_score := old.host_round_score + 10;
    if v_streak >= 3 then
      new.action_seq := old.action_seq + 1;
      new.last_action_streak := v_streak;
      new.last_action_bonus := 0;
      new.last_action_player_id := new.host_id;
      new.last_action_is_bot := false;
      new.last_action_at := now();
    end if;
  elsif new.guest_streak = old.guest_streak + 1 then
    v_streak := new.guest_streak;
    new.guest_score := old.guest_score + 10;
    new.guest_round_score := old.guest_round_score + 10;
    if v_streak >= 3 then
      new.action_seq := old.action_seq + 1;
      new.last_action_streak := v_streak;
      new.last_action_bonus := 0;
      new.last_action_player_id := case when new.is_bot then null else new.guest_id end;
      new.last_action_is_bot := new.is_bot;
      new.last_action_at := now();
    end if;
  else
    -- Existing authoritative failure/timeout functions perform a negative score update.
    -- Normalize that negative move to exactly -5 without changing their turn/bot logic.
    if new.host_score < old.host_score then
      new.host_score := old.host_score - 5;
      new.host_round_score := old.host_round_score - 5;
    end if;
    if new.guest_score < old.guest_score then
      new.guest_score := old.guest_score - 5;
      new.guest_round_score := old.guest_round_score - 5;
    end if;
  end if;

  return new;
end;
$$;

revoke all on function public.trg_sonharf_action_streak() from public;
select pg_notify('pgrst','reload schema');
