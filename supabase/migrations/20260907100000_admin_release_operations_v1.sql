-- Missing admin fallback operations: match review, competition overview, dictionary release state.
-- All operations are authenticated admin-only and keep gameplay authority in existing functions.

create or replace function public.admin_recent_match_analysis_v1(p_limit integer default 30,p_mode text default null)
returns table(
  match_id uuid,user_id uuid,opponent_id uuid,mode text,completed_at timestamptz,
  best_word text,longest_word text,word_count integer,avg_response_ms integer,
  highest_move_score integer,territory_gained integer,territory_lost integer,
  turning_point jsonb,score_breakdown jsonb
)
language plpgsql
security definer
set search_path=''
as $$
begin
  if auth.uid() is null or not public.is_admin(auth.uid()) then raise exception 'admin_required'; end if;
  return query
  select m.match_id,m.user_id,m.opponent_id,m.mode,m.completed_at,m.best_word,m.longest_word,
         m.word_count,m.avg_response_ms,m.highest_move_score,m.territory_gained,m.territory_lost,
         m.turning_point,m.score_breakdown
  from public.match_analysis_snapshots m
  where p_mode is null or m.mode=trim(p_mode)
  order by m.completed_at desc,m.match_id
  limit greatest(1,least(coalesce(p_limit,30),100));
end $$;
revoke all on function public.admin_recent_match_analysis_v1(integer,text) from public,anon;
grant execute on function public.admin_recent_match_analysis_v1(integer,text) to authenticated,service_role;

create or replace function public.admin_competition_overview_v1()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare v_season text; v_tournament uuid;
begin
  if auth.uid() is null or not public.is_admin(auth.uid()) then raise exception 'admin_required'; end if;
  v_season:=public.ensure_competitive_season_v1(now());
  v_tournament:=public.ensure_weekly_tournament_v1();
  return jsonb_build_object(
    'active_season',(select to_jsonb(s) from public.competitive_seasons s where s.id=v_season),
    'recent_seasons',(select coalesce(jsonb_agg(to_jsonb(x) order by x.starts_at desc),'[]'::jsonb) from (select * from public.competitive_seasons order by starts_at desc limit 12) x),
    'active_tournament',(select to_jsonb(t) from public.weekly_tournaments t where t.id=v_tournament),
    'recent_tournaments',(select coalesce(jsonb_agg(to_jsonb(x) order by x.starts_at desc),'[]'::jsonb) from (select * from public.weekly_tournaments order by starts_at desc limit 12) x),
    'active_tournament_entries',(select count(*) from public.weekly_tournament_entries e where e.tournament_id=v_tournament),
    'active_season_players',(select count(*) from public.season_player_stats ps where ps.season_id=v_season)
  );
end $$;
revoke all on function public.admin_competition_overview_v1() from public,anon;
grant execute on function public.admin_competition_overview_v1() to authenticated,service_role;

create or replace function public.admin_dictionary_release_state_v1()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
begin
  if auth.uid() is null or not public.is_admin(auth.uid()) then raise exception 'admin_required'; end if;
  return coalesce((
    select jsonb_agg(jsonb_build_object(
      'language',l.language,
      'live_rows',l.live_rows,
      'game_rows',l.game_rows,
      'active_release_id',s.active_release_id,
      'active_version',a.version,
      'active_sha256',a.snapshot_sha256,
      'active_word_count',a.word_count,
      'active_source_manifest',a.source_manifest,
      'previous_release_id',s.previous_release_id,
      'previous_version',p.version,
      'previous_sha256',p.snapshot_sha256,
      'updated_at',s.updated_at
    ) order by l.language)
    from (
      select w.language,count(*)::bigint live_rows,
             count(*) filter(where w.active and coalesce(w.game_allowed,true) and not coalesce(w.is_abbreviation,false)
               and not coalesce(w.is_proper_noun,false) and char_length(w.normalized_word) between 2 and 12)::bigint game_rows
      from public.dictionary_words w where w.language in ('tr','en') group by w.language
    ) l
    left join public.dictionary_release_state s on s.language=l.language
    left join public.dictionary_release_snapshots a on a.id=s.active_release_id
    left join public.dictionary_release_snapshots p on p.id=s.previous_release_id
  ),'[]'::jsonb);
end $$;
revoke all on function public.admin_dictionary_release_state_v1() from public,anon;
grant execute on function public.admin_dictionary_release_state_v1() to authenticated,service_role;
