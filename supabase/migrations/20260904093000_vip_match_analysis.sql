-- VIP post-match analysis. Completed-match data only; never exposes ranked live assistance.

create or replace function public.get_vip_match_analysis_v1(p_match_id uuid,p_mode text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_vip boolean:=false;
  v_mode text:=lower(trim(coalesce(p_mode,'')));
  v_result jsonb;
  v_opponent uuid;
  v_completed_at timestamptz;
  v_best_word text;
  v_longest_word text;
  v_fast integer;
  v_slow integer;
  v_avg integer;
  v_word_count integer:=0;
  v_avg_len numeric(6,2);
  v_high integer;
  v_critical integer:=0;
  v_gained integer:=0;
  v_lost integer:=0;
  v_turning jsonb:='{}'::jsonb;
  v_breakdown jsonb:='{}'::jsonb;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  if not v_vip then raise exception 'vip_required'; end if;

  if v_mode='classic' then
    if not exists(
      select 1 from public.game_rooms r
      where r.id=p_match_id and r.status='finished' and coalesce(r.is_bot,false)=false
        and v_uid in (r.host_id,r.guest_id)
    ) then raise exception 'completed_match_not_available'; end if;

    with room as (
      select r.*,
        case when v_uid=r.host_id then r.guest_id else r.host_id end opponent_id,
        case when v_uid=r.host_id then r.host_score else r.guest_score end my_final_score,
        case when v_uid=r.host_id then r.guest_score else r.host_score end opponent_final_score
      from public.game_rooms r where r.id=p_match_id
    ), ordered as (
      select w.*,
        row_number() over(order by w.id)::int rn,
        lag(w.created_at) over(order by w.id) prev_at
      from public.game_words w where w.room_id=p_match_id
    ), grouped as (
      select o.*,((o.rn-1)/10)::int round_index
      from ordered o
    ), scored as (
      select g.*,
        row_number() over(partition by g.round_index,g.player_id order by g.rn)::int player_streak
      from grouped g
    ), enriched as (
      select s.*,
        case when s.rn>30 then 0 when s.player_streak%5=0 then 6 else 3 end move_score,
        greatest(0,(extract(epoch from (s.created_at-coalesce(s.prev_at,(select created_at from room))))*1000)::int) response_ms
      from scored s
    ), mine as (
      select * from enriched where player_id=v_uid
    ), valid_timing as (
      select * from mine where response_ms between 0 and 45000
    )
    select
      (select opponent_id from room),
      coalesce((select finished_at from room),(select created_at from room)),
      (select word from mine order by move_score desc,char_length(normalized_word) desc,created_at,id limit 1),
      (select word from mine order by char_length(normalized_word) desc,created_at,id limit 1),
      (select min(response_ms) from valid_timing),
      (select max(response_ms) from valid_timing),
      (select round(avg(response_ms))::int from valid_timing),
      (select count(*)::int from mine),
      (select round(avg(char_length(normalized_word))::numeric,2) from mine),
      (select max(move_score)::int from mine),
      (select count(*)::int from valid_timing where response_ms>=35000),
      jsonb_build_object(
        'mode','classic',
        'final_score',(select my_final_score from room),
        'opponent_final_score',(select opponent_final_score from room),
        'reconstructed_word_points',coalesce((select sum(move_score) from mine),0),
        'other_score_events',greatest(0,(select my_final_score from room)-coalesce((select sum(move_score) from mine),0))
      ),
      coalesce((select jsonb_build_object('sequence',rn,'word',word,'points',move_score,'response_ms',response_ms) from mine order by move_score desc,response_ms asc nulls last limit 1),'{}'::jsonb),
      jsonb_build_object(
        'mode','classic',
        'words',coalesce((
          select jsonb_agg(jsonb_build_object(
            'word',e.word,'player_id',e.player_id,'sequence',e.rn,'round',least(e.round_index+1,4),
            'response_ms',case when e.response_ms between 0 and 45000 then e.response_ms else null end,
            'start_letter',left(e.normalized_word,1),'end_letter',right(e.normalized_word,1),
            'points',e.move_score,'sudden_death',e.rn>30
          ) order by e.rn) from enriched e
        ),'[]'::jsonb)
      )
    into v_opponent,v_completed_at,v_best_word,v_longest_word,v_fast,v_slow,v_avg,
         v_word_count,v_avg_len,v_high,v_critical,v_breakdown,v_turning,v_result;

  elsif v_mode='arena' then
    if not exists(
      select 1 from public.word_arena_rooms r
      where r.id=p_match_id and r.status='finished' and r.result_applied and v_uid in (r.host_id,r.guest_id)
    ) then raise exception 'completed_match_not_available'; end if;

    with room as (
      select r.*,
        case when v_uid=r.host_id then r.guest_id else r.host_id end opponent_id,
        case when v_uid=r.host_id then r.host_score else r.guest_score end my_final_score,
        case when v_uid=r.host_id then r.guest_score else r.host_score end opponent_final_score
      from public.word_arena_rooms r where r.id=p_match_id
    ), enriched as (
      select w.*,
        w.base_points + case when not exists(
          select 1 from public.word_arena_words o
          where o.room_id=w.room_id and o.user_id<>w.user_id and o.normalized_word=w.normalized_word
        ) then w.base_points else 0 end contribution,
        greatest(0,(extract(epoch from (w.created_at-coalesce(
          lag(w.created_at) over(partition by w.user_id order by w.created_at,w.id),
          (select starts_at from room)
        )))*1000)::int) response_ms
      from public.word_arena_words w where w.room_id=p_match_id
    ), mine as (select * from enriched where user_id=v_uid)
    select
      (select opponent_id from room),
      coalesce((select finished_at from room),(select ends_at from room)),
      (select word from mine order by contribution desc,char_length(normalized_word) desc,created_at,id limit 1),
      (select word from mine order by char_length(normalized_word) desc,created_at,id limit 1),
      (select min(response_ms) from mine),
      (select max(response_ms) from mine),
      (select round(avg(response_ms))::int from mine),
      (select count(*)::int from mine),
      (select round(avg(char_length(normalized_word))::numeric,2) from mine),
      (select max(contribution)::int from mine),
      (select count(*)::int from mine where created_at >= (select ends_at-interval '10 seconds' from room)),
      jsonb_build_object(
        'mode','arena','final_score',(select my_final_score from room),
        'opponent_final_score',(select opponent_final_score from room),
        'base_points',coalesce((select sum(base_points) from mine),0),
        'unique_word_bonus',coalesce((select sum(contribution-base_points) from mine),0)
      ),
      coalesce((select jsonb_build_object('word',word,'points',contribution,'response_ms',response_ms) from mine order by contribution desc,response_ms asc limit 1),'{}'::jsonb),
      jsonb_build_object(
        'mode','arena',
        'words',coalesce((
          select jsonb_agg(jsonb_build_object(
            'word',e.word,'player_id',e.user_id,'response_ms',e.response_ms,'combo',e.combo,
            'start_letter',left(e.normalized_word,1),'end_letter',right(e.normalized_word,1),
            'base_points',e.base_points,'points',e.contribution
          ) order by e.created_at,e.id) from enriched e
        ),'[]'::jsonb)
      )
    into v_opponent,v_completed_at,v_best_word,v_longest_word,v_fast,v_slow,v_avg,
         v_word_count,v_avg_len,v_high,v_critical,v_breakdown,v_turning,v_result;

  elsif v_mode in ('siege','word_siege') then
    v_mode:='siege';
    if not exists(
      select 1 from public.word_siege_games r
      where r.id=p_match_id and r.status='finished' and v_uid in (r.player_one_id,r.player_two_id)
    ) then raise exception 'completed_match_not_available'; end if;

    with room as (
      select r.*,
        case when v_uid=r.player_one_id then r.player_two_id else r.player_one_id end opponent_id,
        case when v_uid=r.player_one_id then r.player_one_word_score else r.player_two_word_score end my_word_score,
        case when v_uid=r.player_one_id then r.player_one_area_score else r.player_two_area_score end my_area_score,
        case when v_uid=r.player_one_id then r.player_two_word_score else r.player_one_word_score end opp_word_score,
        case when v_uid=r.player_one_id then r.player_two_area_score else r.player_one_area_score end opp_area_score
      from public.word_siege_games r where r.id=p_match_id
    ), seq as (
      select m.*,lag(m.created_at) over(order by m.move_number,m.id) prev_at,
        greatest(0,(extract(epoch from (m.created_at-coalesce(
          lag(m.created_at) over(order by m.move_number,m.id),(select created_at from room)
        )))*1000)::int) response_ms
      from public.word_siege_moves m where m.game_id=p_match_id
    ), mine as (select * from seq where player_id=v_uid), opp as (select * from seq where player_id<>v_uid),
    my_words as (
      select unnest(m.formed_words) word from mine m
    )
    select
      (select opponent_id from room),
      coalesce((select finished_at from room),(select created_at from room)),
      (select primary_word from mine order by word_score desc,total_score desc,move_number limit 1),
      (select word from my_words order by char_length(word) desc,word limit 1),
      (select min(response_ms) from mine),
      (select max(response_ms) from mine),
      (select round(avg(response_ms))::int from mine),
      (select coalesce(sum(cardinality(formed_words)),0)::int from mine),
      (select round(avg(char_length(word))::numeric,2) from my_words),
      (select max(total_score)::int from mine),
      0,
      coalesce((select sum(neutral_captured+opponent_captured)::int from mine),0),
      coalesce((select sum(opponent_captured)::int from opp),0),
      jsonb_build_object(
        'mode','siege',
        'word_score',(select my_word_score from room),'area_score',(select my_area_score from room),
        'total_score',(select my_word_score+my_area_score from room),
        'opponent_word_score',(select opp_word_score from room),'opponent_area_score',(select opp_area_score from room),
        'opponent_total_score',(select opp_word_score+opp_area_score from room)
      ),
      coalesce((select jsonb_build_object(
        'move_number',move_number,'word',primary_word,'word_score',word_score,
        'area_score',area_score,'total_score',total_score,'captured_cells',captured_cells
      ) from mine order by total_score desc,opponent_captured desc,move_number limit 1),'{}'::jsonb),
      jsonb_build_object(
        'mode','siege',
        'moves',coalesce((select jsonb_agg(jsonb_build_object(
          'player_id',s.player_id,'move_number',s.move_number,'word',s.primary_word,
          'formed_words',to_jsonb(s.formed_words),'response_ms',s.response_ms,
          'start_letter',left(s.primary_word,1),'end_letter',right(s.primary_word,1),
          'word_score',s.word_score,'area_score',s.area_score,'total_score',s.total_score,
          'neutral_captured',s.neutral_captured,'opponent_captured',s.opponent_captured
        ) order by s.move_number,s.id) from seq s),'[]'::jsonb)
      )
    into v_opponent,v_completed_at,v_best_word,v_longest_word,v_fast,v_slow,v_avg,
         v_word_count,v_avg_len,v_high,v_critical,v_gained,v_lost,v_breakdown,v_turning,v_result;
  else
    raise exception 'unsupported_analysis_mode';
  end if;

  insert into public.match_analysis_snapshots(
    match_id,user_id,opponent_id,mode,completed_at,best_word,longest_word,
    fastest_response_ms,slowest_response_ms,avg_response_ms,word_count,avg_word_length,
    highest_move_score,critical_time_responses,territory_gained,territory_lost,turning_point,score_breakdown
  ) values (
    p_match_id,v_uid,v_opponent,v_mode,v_completed_at,v_best_word,v_longest_word,
    v_fast,v_slow,v_avg,coalesce(v_word_count,0),v_avg_len,v_high,coalesce(v_critical,0),
    coalesce(v_gained,0),coalesce(v_lost,0),coalesce(v_turning,'{}'::jsonb),coalesce(v_breakdown,'{}'::jsonb)
  ) on conflict(match_id,user_id) do update set
    opponent_id=excluded.opponent_id,mode=excluded.mode,completed_at=excluded.completed_at,
    best_word=excluded.best_word,longest_word=excluded.longest_word,
    fastest_response_ms=excluded.fastest_response_ms,slowest_response_ms=excluded.slowest_response_ms,
    avg_response_ms=excluded.avg_response_ms,word_count=excluded.word_count,
    avg_word_length=excluded.avg_word_length,highest_move_score=excluded.highest_move_score,
    critical_time_responses=excluded.critical_time_responses,territory_gained=excluded.territory_gained,
    territory_lost=excluded.territory_lost,turning_point=excluded.turning_point,score_breakdown=excluded.score_breakdown;

  return coalesce(v_result,'{}'::jsonb) || jsonb_build_object(
    'match_id',p_match_id,'mode',v_mode,'opponent_id',v_opponent,'completed_at',v_completed_at,
    'best_word',v_best_word,'longest_word',v_longest_word,'fastest_response_ms',v_fast,
    'slowest_response_ms',v_slow,'avg_response_ms',v_avg,'word_count',coalesce(v_word_count,0),
    'avg_word_length',v_avg_len,'highest_move_score',v_high,'critical_time_responses',coalesce(v_critical,0),
    'territory_gained',coalesce(v_gained,0),'territory_lost',coalesce(v_lost,0),
    'turning_point',coalesce(v_turning,'{}'::jsonb),'score_breakdown',coalesce(v_breakdown,'{}'::jsonb)
  );
end
$$;
revoke all on function public.get_vip_match_analysis_v1(uuid,text) from public,anon;
grant execute on function public.get_vip_match_analysis_v1(uuid,text) to authenticated;

create or replace function public.get_vip_rival_analysis_v1(p_opponent_id uuid)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_vip boolean:=false;
  v_total integer:=0;
  v_wins integer:=0;
  v_losses integer:=0;
  v_draws integer:=0;
  v_avg numeric(10,2);
  v_last timestamptz;
  v_last_five jsonb:='[]'::jsonb;
  v_current integer:=0;
  v_best integer:=0;
  v_row record;
  v_marked boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_opponent_id is null or p_opponent_id=v_uid then raise exception 'invalid_target'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  if not v_vip then raise exception 'vip_required'; end if;

  with matches as (
    select coalesce(g.finished_at,g.created_at) played_at,
      case when g.winner_id is null then 'draw' when g.winner_id=v_uid then 'win' else 'loss' end result,
      case when g.host_id=v_uid then g.host_score else g.guest_score end my_score,
      case when g.host_id=v_uid then g.guest_score else g.host_score end their_score,
      'classic'::text mode
    from public.game_rooms g
    where g.status='finished' and coalesce(g.is_bot,false)=false
      and ((g.host_id=v_uid and g.guest_id=p_opponent_id) or (g.host_id=p_opponent_id and g.guest_id=v_uid))
    union all
    select coalesce(a.finished_at,a.ends_at,a.created_at),
      case when a.winner_id is null then 'draw' when a.winner_id=v_uid then 'win' else 'loss' end,
      case when a.host_id=v_uid then a.host_score else a.guest_score end,
      case when a.host_id=v_uid then a.guest_score else a.host_score end,
      'arena'::text
    from public.word_arena_rooms a
    where a.status='finished' and a.result_applied
      and ((a.host_id=v_uid and a.guest_id=p_opponent_id) or (a.host_id=p_opponent_id and a.guest_id=v_uid))
    union all
    select coalesce(s.finished_at,s.created_at),
      case when s.winner_id is null then 'draw' when s.winner_id=v_uid then 'win' else 'loss' end,
      case when s.player_one_id=v_uid then s.player_one_word_score+s.player_one_area_score else s.player_two_word_score+s.player_two_area_score end,
      case when s.player_one_id=v_uid then s.player_two_word_score+s.player_two_area_score else s.player_one_word_score+s.player_one_area_score end,
      'siege'::text
    from public.word_siege_games s
    where s.status='finished'
      and ((s.player_one_id=v_uid and s.player_two_id=p_opponent_id) or (s.player_one_id=p_opponent_id and s.player_two_id=v_uid))
  )
  select count(*)::int,count(*) filter(where result='win')::int,count(*) filter(where result='loss')::int,
         count(*) filter(where result='draw')::int,round(avg(my_score)::numeric,2),max(played_at),
         coalesce((select jsonb_agg(jsonb_build_object('result',q.result,'my_score',q.my_score,'their_score',q.their_score,'mode',q.mode,'played_at',q.played_at) order by q.played_at desc)
                   from (select * from matches order by played_at desc limit 5) q),'[]'::jsonb)
  into v_total,v_wins,v_losses,v_draws,v_avg,v_last,v_last_five
  from matches;

  if v_total=0 then raise exception 'rival_history_not_available'; end if;

  for v_row in
    with matches as (
      select coalesce(g.finished_at,g.created_at) played_at,case when g.winner_id=v_uid then 'win' when g.winner_id is null then 'draw' else 'loss' end result
      from public.game_rooms g where g.status='finished' and coalesce(g.is_bot,false)=false
        and ((g.host_id=v_uid and g.guest_id=p_opponent_id) or (g.host_id=p_opponent_id and g.guest_id=v_uid))
      union all
      select coalesce(a.finished_at,a.ends_at,a.created_at),case when a.winner_id=v_uid then 'win' when a.winner_id is null then 'draw' else 'loss' end
      from public.word_arena_rooms a where a.status='finished' and a.result_applied
        and ((a.host_id=v_uid and a.guest_id=p_opponent_id) or (a.host_id=p_opponent_id and a.guest_id=v_uid))
      union all
      select coalesce(s.finished_at,s.created_at),case when s.winner_id=v_uid then 'win' when s.winner_id is null then 'draw' else 'loss' end
      from public.word_siege_games s where s.status='finished'
        and ((s.player_one_id=v_uid and s.player_two_id=p_opponent_id) or (s.player_one_id=p_opponent_id and s.player_two_id=v_uid))
    ) select result from matches order by played_at
  loop
    if v_row.result='win' then v_current:=v_current+1; v_best:=greatest(v_best,v_current); else v_current:=0; end if;
  end loop;

  select coalesce(arch_rival,false) into v_marked
  from public.player_relationship_marks where user_id=v_uid and other_user_id=p_opponent_id;

  return jsonb_build_object(
    'opponent_id',p_opponent_id,'total_matches',v_total,'wins',v_wins,'losses',v_losses,'draws',v_draws,
    'average_score',v_avg,'last_five',v_last_five,'last_match_at',v_last,'longest_win_streak',v_best,
    'arch_rival',coalesce(v_marked,false)
  );
end
$$;
revoke all on function public.get_vip_rival_analysis_v1(uuid) from public,anon;
grant execute on function public.get_vip_rival_analysis_v1(uuid) to authenticated;
