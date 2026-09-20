begin;

-- Historical repair only: two legacy classic Word Siege matchmaking-expiry rows
-- were cancelled before terminal metadata was written. Current cleanup logic
-- already writes both fields for new expiries.
do $block$
declare
  v_target_count integer := 0;
begin
  select count(*)
  into v_target_count
  from public.word_siege_games
  where status = 'cancelled'
    and last_action = 'matchmaking_expired'
    and player_two_id is null
    and finished_at is null
    and finish_reason is null
    and created_at < timestamptz '2026-09-19 00:00:00+00'
    and updated_at <= timestamptz '2026-09-18 14:55:12.780809+00';

  if v_target_count > 2 then
    raise exception 'Refusing legacy Word Siege expiry repair: expected at most 2 rows, found %', v_target_count;
  end if;

  update public.word_siege_games
  set finished_at = updated_at,
      finish_reason = 'matchmaking_expired'
  where status = 'cancelled'
    and last_action = 'matchmaking_expired'
    and player_two_id is null
    and finished_at is null
    and finish_reason is null
    and created_at < timestamptz '2026-09-19 00:00:00+00'
    and updated_at <= timestamptz '2026-09-18 14:55:12.780809+00';
end
$block$;

commit;
