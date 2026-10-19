-- Two Kelime Tahtı games were opened between the same two players within four seconds.
-- The app retried "find game" (its response could not be read), and each retry either
-- joined or created another game. Matchmaking is now idempotent:
--   * a game that just started for this player (no moves yet, < 2 minutes old) is
--     returned again instead of opening a new one;
--   * a waiting game is not joined when the two players already share an active
--     classic game.
create or replace function private.find_or_create_word_siege_game_v2(p_language text default 'tr'::text, p_turn_duration_hours integer default 12)
returns public.word_siege_games
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private', 'pg_temp'
as $function$
declare
  v_uid uuid:=auth.uid();
  v_language text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_duration smallint;
  v_bag text;
  v_rack text;
  v_active_count integer;
  v_now timestamptz:=clock_timestamp();
  r public.word_siege_games;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  if not exists(select 1 from public.profiles p where p.id=v_uid) then raise exception 'word_siege_profile_required'; end if;
  if p_turn_duration_hours not in (12,24,72) then raise exception 'word_siege_invalid_turn_duration'; end if;
  v_duration:=p_turn_duration_hours::smallint;
  perform pg_advisory_xact_lock(hashtextextended('word_siege:classic:'||v_language||':'||v_duration::text,0));

  update public.word_siege_games set status='cancelled',last_action='matchmaking_expired',updated_at=v_now
   where status='waiting' and game_mode='classic'
     and greatest(coalesce(updated_at,created_at),created_at)<v_now-interval '30 minutes';

  -- Retry of a request that already matched: hand back that fresh game.
  select * into r from public.word_siege_games g
   where g.status='playing' and g.game_mode='classic' and g.move_count=0
     and g.last_action='game_started'
     and v_uid in (g.player_one_id,g.player_two_id)
     and g.language=v_language and g.turn_duration_hours=v_duration
     and g.turn_started_at>v_now-interval '2 minutes'
   order by g.turn_started_at desc limit 1;
  if r.id is not null then return r; end if;

  select count(*)::integer into v_active_count from public.word_siege_games g
   where g.status in ('waiting','playing') and v_uid in(g.player_one_id,g.player_two_id);
  if v_active_count>=public.word_siege_active_limit_v1(v_uid) then raise exception 'word_siege_active_limit'; end if;

  select * into r from public.word_siege_games g
   where g.status='waiting' and g.game_mode='classic' and g.player_one_id=v_uid
     and g.language=v_language and g.turn_duration_hours=v_duration
   order by g.created_at limit 1;
  if r.id is not null then return r; end if;

  select * into r from public.word_siege_games g
   where g.status='waiting' and g.game_mode='classic' and g.language=v_language
     and g.turn_duration_hours=v_duration and g.player_one_id<>v_uid
     and not exists(select 1 from public.user_blocks b
       where (b.blocker_id=v_uid and b.blocked_id=g.player_one_id)
          or (b.blocker_id=g.player_one_id and b.blocked_id=v_uid))
     and not exists(select 1 from public.word_siege_games x
       where x.game_mode='classic' and x.status='playing'
         and ((x.player_one_id=v_uid and x.player_two_id=g.player_one_id)
           or (x.player_two_id=v_uid and x.player_one_id=g.player_one_id)))
   order by g.created_at for update skip locked limit 1;

  if r.id is not null then
    update public.word_siege_games
       set player_two_id=v_uid,player_two_rack=substring(r.bag from 1 for 7),bag=substring(r.bag from 8),
           status='playing',current_player_id=r.player_one_id,turn_started_at=v_now,
           turn_deadline=v_now+make_interval(hours=>v_duration),last_action='game_started',
           last_action_player_id=null,updated_at=v_now
     where id=r.id returning * into r;
    return r;
  end if;

  v_bag:=private.word_siege_new_bag_v1(v_language);
  v_rack:=substring(v_bag from 1 for 7);
  v_bag:=substring(v_bag from 8);
  insert into public.word_siege_games(player_one_id,language,turn_duration_hours,game_mode,board,bag,player_one_rack,last_action)
  values(v_uid,v_language,v_duration,'classic',private.word_siege_new_board_v1(),v_bag,v_rack,'waiting_for_opponent')
  returning * into r;
  return r;
end
$function$;
