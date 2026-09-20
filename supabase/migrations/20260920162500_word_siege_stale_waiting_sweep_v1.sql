begin;

create or replace function private.cleanup_stale_word_siege_waiting_v1()
returns integer
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $function$
declare
  v_count integer := 0;
begin
  update public.word_siege_games
  set status='cancelled',
      last_action=case when game_mode='series' then 'series_matchmaking_expired' else 'matchmaking_expired' end,
      last_action_player_id=null,
      updated_at=clock_timestamp(),
      finished_at=coalesce(finished_at,clock_timestamp()),
      finish_reason=coalesce(finish_reason,'matchmaking_expired')
  where status='waiting'
    and greatest(coalesce(updated_at,created_at),created_at) < clock_timestamp()-interval '30 minutes';

  get diagnostics v_count = row_count;
  return v_count;
end
$function$;

revoke all on function private.cleanup_stale_word_siege_waiting_v1() from public;
revoke all on function private.cleanup_stale_word_siege_waiting_v1() from anon;
revoke all on function private.cleanup_stale_word_siege_waiting_v1() from authenticated;

create or replace function private.sweep_word_siege_timeouts_v2()
returns integer
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $function$
declare
  v_game_id uuid;
  v_count integer := 0;
begin
  v_count := private.cleanup_stale_word_siege_waiting_v1();

  for v_game_id in
    select g.id
    from public.word_siege_games g
    where g.status='playing'
      and g.turn_deadline is not null
      and clock_timestamp()>=g.turn_deadline
    order by g.turn_deadline
    for update skip locked
  loop
    perform private.word_siege_finalize_timeout_v2(v_game_id);
    v_count:=v_count+1;
  end loop;

  return v_count;
end
$function$;

-- Clean existing stale waiting rows immediately; the existing every-minute cron
-- continues to call sweep_word_siege_timeouts_v2 afterwards.
select private.cleanup_stale_word_siege_waiting_v1();

commit;
