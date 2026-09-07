-- Keep existing Android clients compatible while making bot continuation server-authoritative.
-- Human-vs-human behavior is unchanged; bot_take_turn runs only when is_bot && bot_turn.

create or replace function public.submit_word_v3_core_v1(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'pg_catalog','public','private','pg_temp'
as $$
declare r public.game_rooms; v_check record;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id=p_room_id;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() not in (r.host_id,r.guest_id) then raise exception 'not_participant'; end if;
  select * into v_check from private.validate_dictionary_word_v1(p_word,r.language) limit 1;
  if not coalesce(v_check.valid,false) then
    return public.switch_turn_after_failure(
      p_room_id,
      case v_check.reason
        when 'abbreviation_not_allowed' then 'abbreviation_not_allowed'
        when 'proper_noun_not_allowed' then 'proper_noun_not_allowed'
        else 'not_in_dictionary'
      end
    );
  end if;
  if r.language='tr' and right(v_check.normalized_word,1)='ğ' then
    return public.switch_turn_after_failure(p_room_id,'ends_with_soft_g');
  end if;
  return public.submit_word_v3_legacy(p_room_id,p_word);
end
$$;

create or replace function public.submit_word_v3(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare r public.game_rooms;
begin
  r := public.submit_word_v3_core_v1(p_room_id,p_word);
  if r.is_bot and r.bot_turn and r.status in ('playing','sudden_death') then
    r := public.bot_take_turn(r.id);
  end if;
  return r;
end
$$;

create or replace function public.claim_turn_timeout_core_v1(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare r public.game_rooms; timed uuid;
begin
  select * into r from public.game_rooms where id=p_room_id for update;
  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status not in ('playing','sudden_death') or r.turn_deadline is null or r.turn_deadline>=now() then return r; end if;
  timed:=r.current_player_id;
  if timed is null then return r; end if;
  if auth.uid()<>r.host_id and auth.uid()<>r.guest_id then raise exception 'not_participant'; end if;
  if r.status='sudden_death' then
    return public.sonharf_finish_room(
      r.id,
      case when r.is_bot then null else case when timed=r.host_id then r.guest_id else r.host_id end end,
      r.is_bot and timed=r.host_id,
      'turn_expired'
    );
  end if;
  if timed=r.host_id then
    update public.game_rooms
      set host_score=host_score-1,
          host_round_score=host_round_score-1,
          host_streak=0,
          current_player_id=case when r.is_bot then null else r.guest_id end,
          bot_turn=r.is_bot,
          turn_deadline=case when r.is_bot then null else public.sonharf_turn_deadline(r.game_mode) end,
          last_event='turn_expired',
          last_event_player_id=timed
      where id=r.id returning * into r;
  else
    update public.game_rooms
      set guest_score=guest_score-1,
          guest_round_score=guest_round_score-1,
          guest_streak=0,
          current_player_id=r.host_id,
          bot_turn=false,
          turn_deadline=public.sonharf_turn_deadline(r.game_mode),
          last_event='turn_expired',
          last_event_player_id=timed
      where id=r.id returning * into r;
  end if;
  return r;
end
$$;

create or replace function public.claim_turn_timeout(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare r public.game_rooms;
begin
  r := public.claim_turn_timeout_core_v1(p_room_id);
  if r.is_bot and r.bot_turn and r.status in ('playing','sudden_death') then
    r := public.bot_take_turn(r.id);
  end if;
  return r;
end
$$;

revoke all on function public.submit_word_v3_core_v1(uuid,text) from public,anon,authenticated;
revoke all on function public.claim_turn_timeout_core_v1(uuid) from public,anon,authenticated;
revoke all on function public.submit_word_v3(uuid,text) from public,anon,authenticated;
revoke all on function public.claim_turn_timeout(uuid) from public,anon,authenticated;
grant execute on function public.submit_word_v3(uuid,text) to authenticated;
grant execute on function public.claim_turn_timeout(uuid) to authenticated;
