-- Keep Premium analysis aligned with the authoritative Kelime Tahtı scoring contract.
-- Word points are permanent; territory points are current owned cells * 2.
-- Current ownership is reconstructed from move capture deltas, matching WordSiegeFinalRules.
-- Fail closed if the live/source function no longer contains the exact legacy expressions.

do $migration$
declare
  v_definition text;
  v_old_my constant text := 'case when v_uid=r.player_one_id then r.player_one_area_score else r.player_two_area_score end my_area_score';
  v_old_opp constant text := 'case when v_uid=r.player_one_id then r.player_two_area_score else r.player_one_area_score end opp_area_score';
  v_new_my constant text := 'case when v_uid=r.player_one_id then greatest(0,coalesce((select sum(case when m.player_id=r.player_one_id then coalesce(m.neutral_captured,0)+coalesce(m.opponent_captured,0) else -coalesce(m.opponent_captured,0) end) from public.word_siege_moves m where m.game_id=r.id),0))*2 else greatest(0,coalesce((select sum(case when m.player_id=r.player_two_id then coalesce(m.neutral_captured,0)+coalesce(m.opponent_captured,0) else -coalesce(m.opponent_captured,0) end) from public.word_siege_moves m where m.game_id=r.id),0))*2 end my_area_score';
  v_new_opp constant text := 'case when v_uid=r.player_one_id then greatest(0,coalesce((select sum(case when m.player_id=r.player_two_id then coalesce(m.neutral_captured,0)+coalesce(m.opponent_captured,0) else -coalesce(m.opponent_captured,0) end) from public.word_siege_moves m where m.game_id=r.id),0))*2 else greatest(0,coalesce((select sum(case when m.player_id=r.player_one_id then coalesce(m.neutral_captured,0)+coalesce(m.opponent_captured,0) else -coalesce(m.opponent_captured,0) end) from public.word_siege_moves m where m.game_id=r.id),0))*2 end opp_area_score';
begin
  if to_regprocedure('public.get_vip_match_analysis_v1(uuid,text)') is null then
    raise exception 'get_vip_match_analysis_v1_missing';
  end if;

  select pg_get_functiondef('public.get_vip_match_analysis_v1(uuid,text)'::regprocedure)
    into v_definition;

  if position(v_old_my in v_definition) = 0 or position(v_old_opp in v_definition) = 0 then
    raise exception 'get_vip_match_analysis_v1_source_drift';
  end if;

  v_definition := replace(v_definition, v_old_my, v_new_my);
  v_definition := replace(v_definition, v_old_opp, v_new_opp);
  execute v_definition;
end
$migration$;

revoke all on function public.get_vip_match_analysis_v1(uuid,text) from public, anon;
grant execute on function public.get_vip_match_analysis_v1(uuid,text) to authenticated;

-- A small server-authoritative index for the Profile analysis launcher.
-- Only terminal matches are returned. Classic bot matches are excluded because the analysis RPC
-- intentionally supports competitive human matches only.
create or replace function public.get_vip_recent_completed_matches_v1(p_limit integer default 12)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
  v_limit integer := least(greatest(coalesce(p_limit, 12), 1), 30);
  v_result jsonb;
begin
  if v_uid is null then
    raise exception 'unauthorized';
  end if;

  select coalesce(is_vip, false)
    into v_vip
    from public.profiles
    where id = v_uid;

  if not v_vip then
    raise exception 'vip_required';
  end if;

  with completed as (
    select
      g.id as match_id,
      'classic'::text as mode,
      coalesce(g.finished_at, g.created_at) as completed_at,
      case when g.host_id = v_uid then g.guest_id else g.host_id end as opponent_id
    from public.game_rooms g
    where g.status = 'finished'
      and coalesce(g.is_bot, false) = false
      and v_uid in (g.host_id, g.guest_id)

    union all

    select
      a.id,
      'arena'::text,
      coalesce(a.finished_at, a.ends_at, a.created_at),
      case when a.host_id = v_uid then a.guest_id else a.host_id end
    from public.word_arena_rooms a
    where a.status = 'finished'
      and a.result_applied
      and v_uid in (a.host_id, a.guest_id)

    union all

    select
      s.id,
      'siege'::text,
      coalesce(s.finished_at, s.created_at),
      case when s.player_one_id = v_uid then s.player_two_id else s.player_one_id end
    from public.word_siege_games s
    where s.status = 'finished'
      and v_uid in (s.player_one_id, s.player_two_id)
  ), limited as (
    select *
    from completed
    order by completed_at desc nulls last, match_id
    limit v_limit
  )
  select coalesce(
    jsonb_agg(
      jsonb_build_object(
        'match_id', match_id,
        'mode', mode,
        'completed_at', completed_at,
        'opponent_id', opponent_id
      )
      order by completed_at desc nulls last, match_id
    ),
    '[]'::jsonb
  ) into v_result
  from limited;

  return v_result;
end
$$;

revoke all on function public.get_vip_recent_completed_matches_v1(integer) from public, anon;
grant execute on function public.get_vip_recent_completed_matches_v1(integer) to authenticated;
