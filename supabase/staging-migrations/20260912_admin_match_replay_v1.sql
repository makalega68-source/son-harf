-- STAGING ONLY. Do not move into supabase/migrations until issue #341 staging gates pass.
-- Read-only admin replay for Classic/Son Harf and current 15x15 Kelime Kusatmasi.
-- Word points are permanent. Final territory points come only from the final board owner snapshot: owned cubes * 2.
-- Historical capture cell indexes are not persisted by word_siege_moves, so replay exposes exact
-- capture counts and deterministic owned-cube counts after each move rather than inventing cell ids.

create or replace function public.admin_match_replay_v1(
  p_match_id uuid,
  p_mode text,
  p_offset integer default 0,
  p_limit integer default 50
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_mode text := lower(trim(coalesce(p_mode,'')));
  v_offset integer := greatest(coalesce(p_offset,0),0);
  v_limit integer := least(greatest(coalesce(p_limit,50),1),100);
  v_total integer := 0;
  v_events jsonb := '[]'::jsonb;
  v_classic public.game_rooms%rowtype;
  v_siege public.word_siege_games%rowtype;
  v_one_owned integer := 0;
  v_two_owned integer := 0;
begin
  if v_uid is null or not public.is_admin() then
    raise exception 'admin_required';
  end if;
  if p_match_id is null then raise exception 'match_id_required'; end if;

  if v_mode in ('classic','son_harf','sonharf') then
    select * into v_classic
    from public.game_rooms
    where id=p_match_id;
    if not found then raise exception 'match_not_found'; end if;

    select count(*)::integer into v_total
    from public.game_words
    where room_id=p_match_id;

    select coalesce(
      jsonb_agg(
        jsonb_build_object(
          'event_id',w.id,
          'player_id',w.player_id,
          'word',w.word,
          'is_bot',w.is_bot,
          'created_at',w.created_at
        ) order by w.id
      ),
      '[]'::jsonb
    ) into v_events
    from (
      select id,player_id,word,is_bot,created_at
      from public.game_words
      where room_id=p_match_id
      order by id
      limit v_limit offset v_offset
    ) w;

    return jsonb_build_object(
      'mode','classic',
      'match',jsonb_build_object(
        'id',v_classic.id,
        'status',v_classic.status,
        'language',v_classic.language,
        'host_id',v_classic.host_id,
        'guest_id',v_classic.guest_id,
        'winner_id',v_classic.winner_id,
        'is_bot',v_classic.is_bot,
        'room_type',v_classic.room_type,
        'game_mode',v_classic.game_mode,
        'room_mode',v_classic.room_mode,
        'created_at',v_classic.created_at,
        'finished_at',v_classic.finished_at,
        'host_score',v_classic.host_score,
        'guest_score',v_classic.guest_score
      ),
      'page',jsonb_build_object(
        'offset',v_offset,'limit',v_limit,'returned',jsonb_array_length(v_events),
        'total',v_total,'has_more',(v_offset+jsonb_array_length(v_events))<v_total
      ),
      'events',v_events
    );
  end if;

  if v_mode in ('siege','word_siege','kelime_kusatmasi','kelime_tahti') then
    select * into v_siege
    from public.word_siege_games
    where id=p_match_id;
    if not found then raise exception 'match_not_found'; end if;

    select
      count(*) filter (where coalesce((cell->>'owner')::integer,0)=1)::integer,
      count(*) filter (where coalesce((cell->>'owner')::integer,0)=2)::integer
    into v_one_owned,v_two_owned
    from jsonb_array_elements(v_siege.board) cell;

    select count(*)::integer into v_total
    from public.word_siege_moves
    where game_id=p_match_id;

    with ordered as (
      select
        m.*,
        row_number() over(order by coalesce(m.move_number::bigint,m.id),m.id) as rn,
        case when m.player_id=v_siege.player_one_id
          then m.neutral_captured+m.opponent_captured else -m.opponent_captured end as p1_cube_delta,
        case when m.player_id=v_siege.player_two_id
          then m.neutral_captured+m.opponent_captured else -m.opponent_captured end as p2_cube_delta,
        sum(case when m.player_id=v_siege.player_one_id
          then m.neutral_captured+m.opponent_captured else -m.opponent_captured end)
          over(order by coalesce(m.move_number::bigint,m.id),m.id rows unbounded preceding) as p1_owned_after,
        sum(case when m.player_id=v_siege.player_two_id
          then m.neutral_captured+m.opponent_captured else -m.opponent_captured end)
          over(order by coalesce(m.move_number::bigint,m.id),m.id rows unbounded preceding) as p2_owned_after,
        sum(case when m.player_id=v_siege.player_one_id then m.word_score else 0 end)
          over(order by coalesce(m.move_number::bigint,m.id),m.id rows unbounded preceding) as p1_word_after,
        sum(case when m.player_id=v_siege.player_two_id then m.word_score else 0 end)
          over(order by coalesce(m.move_number::bigint,m.id),m.id rows unbounded preceding) as p2_word_after
      from public.word_siege_moves m
      where m.game_id=p_match_id
    ), paged as (
      select * from ordered
      where rn>v_offset and rn<=v_offset+v_limit
      order by rn
    )
    select coalesce(
      jsonb_agg(
        jsonb_build_object(
          'move_id',p.id,
          'move_number',p.move_number,
          'player_id',p.player_id,
          'primary_word',p.primary_word,
          'formed_words',to_jsonb(p.formed_words),
          'placed_tiles',p.placed_tiles,
          'word_score',p.word_score,
          'neutral_captured',p.neutral_captured,
          'opponent_captured',p.opponent_captured,
          'ownership_delta',jsonb_build_object(
            'player_one_cubes',p.p1_cube_delta,
            'player_two_cubes',p.p2_cube_delta
          ),
          'ownership_after',jsonb_build_object(
            'player_one_cubes',greatest(p.p1_owned_after,0),
            'player_two_cubes',greatest(p.p2_owned_after,0),
            'player_one_territory_points',greatest(p.p1_owned_after,0)*2,
            'player_two_territory_points',greatest(p.p2_owned_after,0)*2
          ),
          'word_points_after',jsonb_build_object(
            'player_one',p.p1_word_after,
            'player_two',p.p2_word_after
          ),
          'created_at',p.created_at
        ) order by p.rn
      ),
      '[]'::jsonb
    ) into v_events
    from paged p;

    return jsonb_build_object(
      'mode','siege',
      'match',jsonb_build_object(
        'id',v_siege.id,
        'status',v_siege.status,
        'language',v_siege.language,
        'player_one_id',v_siege.player_one_id,
        'player_two_id',v_siege.player_two_id,
        'winner_id',v_siege.winner_id,
        'loser_id',v_siege.loser_id,
        'finish_reason',v_siege.finish_reason,
        'created_at',v_siege.created_at,
        'finished_at',v_siege.finished_at
      ),
      'score_contract',jsonb_build_object(
        'cube_points',2,
        'word_points_permanent',true,
        'final_territory_source','final_board_owner_snapshot'
      ),
      'final_score',jsonb_build_object(
        'player_one_word_points',v_siege.player_one_word_score,
        'player_one_owned_cubes',v_one_owned,
        'player_one_territory_points',v_one_owned*2,
        'player_one_total',v_siege.player_one_word_score+v_one_owned*2,
        'player_two_word_points',v_siege.player_two_word_score,
        'player_two_owned_cubes',v_two_owned,
        'player_two_territory_points',v_two_owned*2,
        'player_two_total',v_siege.player_two_word_score+v_two_owned*2
      ),
      'replay_fidelity',jsonb_build_object(
        'exact_placed_tile_indices',true,
        'exact_historical_capture_cell_indices',false,
        'ownership_counts_reconstructed_from_capture_deltas',true,
        'final_ownership_from_board_snapshot',true
      ),
      'final_board',v_siege.board,
      'page',jsonb_build_object(
        'offset',v_offset,'limit',v_limit,'returned',jsonb_array_length(v_events),
        'total',v_total,'has_more',(v_offset+jsonb_array_length(v_events))<v_total
      ),
      'events',v_events
    );
  end if;

  raise exception 'unsupported_replay_mode';
end
$$;

revoke all on function public.admin_match_replay_v1(uuid,text,integer,integer)
  from public,anon,service_role;
grant execute on function public.admin_match_replay_v1(uuid,text,integer,integer)
  to authenticated;
