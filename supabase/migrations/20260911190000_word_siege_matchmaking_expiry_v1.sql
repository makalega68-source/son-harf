-- Kelime Tahtı: prevent stale waiting rows from trapping matchmaking forever.
-- Existing playing/finished games are untouched. Waiting rows older than 30 minutes
-- are cancelled because the client already falls back to practice after 15 seconds.

update public.word_siege_games
set status = 'cancelled',
    last_action = 'matchmaking_expired',
    updated_at = clock_timestamp()
where status = 'waiting'
  and greatest(coalesce(updated_at, created_at), created_at) < clock_timestamp() - interval '30 minutes';

create or replace function private.find_or_create_word_siege_game_v2(
  p_language text default 'tr'::text,
  p_turn_duration_hours integer default 12
)
returns public.word_siege_games
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $function$
declare
  v_uid uuid := auth.uid();
  v_language text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_duration smallint;
  v_bag text;
  v_rack text;
  v_active_count integer;
  v_now timestamptz := clock_timestamp();
  r public.word_siege_games;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not exists(select 1 from public.profiles p where p.id=v_uid) then raise exception 'word_siege_profile_required'; end if;
  if p_turn_duration_hours not in (12,72) then raise exception 'word_siege_invalid_turn_duration'; end if;
  v_duration := p_turn_duration_hours::smallint;

  perform pg_advisory_xact_lock(hashtextextended('word_siege:'||v_language||':'||v_duration::text,0));

  update public.word_siege_games
     set status='cancelled',
         last_action='matchmaking_expired',
         updated_at=v_now
   where status='waiting'
     and greatest(coalesce(updated_at, created_at), created_at) < v_now - interval '30 minutes';

  select count(*)::integer into v_active_count
    from public.word_siege_games g
   where g.status in ('waiting','playing')
     and v_uid in (g.player_one_id,g.player_two_id);
  if v_active_count>=10 then raise exception 'word_siege_active_limit'; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting'
     and g.player_one_id=v_uid
     and g.language=v_language
     and g.turn_duration_hours=v_duration
   order by g.created_at
   limit 1;
  if r.id is not null then return r; end if;

  select * into r
    from public.word_siege_games g
   where g.status='waiting'
     and g.language=v_language
     and g.turn_duration_hours=v_duration
     and g.player_one_id<>v_uid
     and not exists(
       select 1 from public.user_blocks b
        where (b.blocker_id=v_uid and b.blocked_id=g.player_one_id)
           or (b.blocker_id=g.player_one_id and b.blocked_id=v_uid)
     )
   order by g.created_at
   for update skip locked
   limit 1;

  if r.id is not null then
    update public.word_siege_games
       set player_two_id=v_uid,
           player_two_rack=substring(r.bag from 1 for 7),
           bag=substring(r.bag from 8),
           status='playing',
           current_player_id=r.player_one_id,
           turn_started_at=v_now,
           turn_deadline=v_now+make_interval(hours=>v_duration),
           last_action='game_started',
           last_action_player_id=null,
           updated_at=v_now
     where id=r.id
     returning * into r;
    return r;
  end if;

  v_bag := private.word_siege_new_bag_v1(v_language);
  v_rack := substring(v_bag from 1 for 7);
  v_bag := substring(v_bag from 8);

  insert into public.word_siege_games(
    player_one_id,language,turn_duration_hours,board,bag,player_one_rack,last_action
  ) values(
    v_uid,v_language,v_duration,private.word_siege_new_board_v1(),v_bag,v_rack,'waiting_for_opponent'
  ) returning * into r;
  return r;
end
$function$;
