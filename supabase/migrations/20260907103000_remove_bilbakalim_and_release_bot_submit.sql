-- Bil Bakalım is removed from the live duel loop. Word validation must return
-- before bot thinking begins so the player's feedback is not delayed.

update public.app_config
set value = 'false'::jsonb
where key = 'trivia_enabled';

create or replace function public.submit_word_v3_legacy(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  before_room public.game_rooms;
  r public.game_rooms;
  clean text;
begin
  select * into before_room from public.game_rooms where id = p_room_id;
  if before_room.id is null then raise exception 'room_not_found'; end if;

  clean := public.normalize_game_word(before_room.language, p_word);
  if not public.sonharf_word_allowed(before_room.language, clean) then
    return public.switch_turn_after_failure(p_room_id, 'invalid_word');
  end if;

  if before_room.game_mode = 'expert' then
    r := public.submit_word_expert_v1(p_room_id, p_word);
  else
    r := public.submit_word_normal_v3(p_room_id, p_word);
  end if;

  return r;
end
$function$;

create or replace function public.submit_word_v3(p_room_id uuid, p_word text)
returns public.game_rooms
language sql
security definer
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select public.submit_word_v3_core_v1(p_room_id, p_word);
$function$;

-- Resume a quiz that may have started immediately before the feature switch.
with latest_round as (
  select distinct on (q.room_id)
    q.room_id,
    q.resume_status,
    q.resume_current_player_id,
    coalesce(q.resume_bot_turn, false) as resume_bot_turn,
    q.resume_turn_remaining_ms
  from public.trivia_rounds q
  join public.game_rooms r on r.id = q.room_id and r.status = 'quiz'
  order by q.room_id, q.reveal_at desc
)
update public.game_rooms r
set status = case
      when q.resume_status in ('playing', 'final', 'sudden_death') then q.resume_status
      else 'playing'
    end,
    current_player_id = case
      when q.resume_bot_turn then null
      else coalesce(q.resume_current_player_id, r.host_id)
    end,
    bot_turn = q.resume_bot_turn,
    turn_deadline = case
      when q.resume_bot_turn then null
      else clock_timestamp() + make_interval(
        secs => greatest(1, least(120000, coalesce(q.resume_turn_remaining_ms, 45000))) / 1000.0
      )
    end,
    last_event = 'bilbakalim_removed',
    last_event_player_id = null
from latest_round q
where r.id = q.room_id and r.status = 'quiz';

update public.game_rooms
set status = 'playing',
    current_player_id = coalesce(current_player_id, host_id),
    bot_turn = false,
    turn_deadline = public.sonharf_turn_deadline(game_mode),
    last_event = 'bilbakalim_removed',
    last_event_player_id = null
where status = 'quiz';

update public.trivia_rounds
set resolved_at = coalesce(resolved_at, clock_timestamp()),
    result_until = coalesce(result_until, clock_timestamp())
where resolved_at is null;

revoke all on function public.submit_word_v3_legacy(uuid, text) from public, anon;
revoke all on function public.submit_word_v3(uuid, text) from public, anon;
grant execute on function public.submit_word_v3_legacy(uuid, text) to authenticated, service_role;
grant execute on function public.submit_word_v3(uuid, text) to authenticated, service_role;
