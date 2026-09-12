-- Keep Premium analysis aligned with the authoritative Kelime Tahtı scoring contract.
-- Word points are permanent; territory points are current owned cells * 2.
-- Fail closed if the live/source function no longer contains the exact legacy expressions.

do $migration$
declare
  v_definition text;
  v_old_my constant text := 'case when v_uid=r.player_one_id then r.player_one_area_score else r.player_two_area_score end my_area_score';
  v_old_opp constant text := 'case when v_uid=r.player_one_id then r.player_two_area_score else r.player_one_area_score end opp_area_score';
  v_new_my constant text := 'case when v_uid=r.player_one_id then coalesce(array_length(r.player_one_area,1),0)*2 else coalesce(array_length(r.player_two_area,1),0)*2 end my_area_score';
  v_new_opp constant text := 'case when v_uid=r.player_one_id then coalesce(array_length(r.player_two_area,1),0)*2 else coalesce(array_length(r.player_one_area,1),0)*2 end opp_area_score';
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
