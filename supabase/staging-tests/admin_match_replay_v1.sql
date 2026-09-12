-- Run only after supabase/staging-migrations/20260912_admin_match_replay_v1.sql
-- has been applied to an isolated Supabase staging/dev branch. Everything below rolls back.

begin;

do $$
begin
  if has_function_privilege('anon','public.admin_match_replay_v1(uuid,text,integer,integer)','execute') then
    raise exception 'anon_replay_execute_open';
  end if;
  if has_function_privilege('service_role','public.admin_match_replay_v1(uuid,text,integer,integer)','execute') then
    raise exception 'service_role_replay_execute_open';
  end if;
  if not has_function_privilege('authenticated','public.admin_match_replay_v1(uuid,text,integer,integer)','execute') then
    raise exception 'authenticated_replay_execute_missing';
  end if;
end
$$;

insert into auth.users(
  id,aud,role,email,email_confirmed_at,raw_app_meta_data,raw_user_meta_data,
  is_sso_user,is_anonymous,created_at,updated_at
) values
  ('00000000-0000-4000-8000-000000000341'::uuid,'authenticated','authenticated','makalega68@gmail.com',now(),'{}'::jsonb,'{"display_name":"Replay Admin","gender":"erkek"}'::jsonb,false,false,now(),now()),
  ('00000000-0000-4000-8000-000000003411'::uuid,'authenticated','authenticated','staging-player-one-341@example.invalid',now(),'{}'::jsonb,'{"display_name":"Replay P1","gender":"erkek"}'::jsonb,false,false,now(),now()),
  ('00000000-0000-4000-8000-000000003412'::uuid,'authenticated','authenticated','staging-player-two-341@example.invalid',now(),'{}'::jsonb,'{"display_name":"Replay P2","gender":"kadın"}'::jsonb,false,false,now(),now());

insert into public.admin_users(user_id,role)
values ('00000000-0000-4000-8000-000000000341'::uuid,'admin');

do $$
declare
  v_game constant uuid := '00000000-0000-4000-8000-000000003419'::uuid;
  v_p1 constant uuid := '00000000-0000-4000-8000-000000003411'::uuid;
  v_p2 constant uuid := '00000000-0000-4000-8000-000000003412'::uuid;
  v_board jsonb;
begin
  select jsonb_agg(
    jsonb_build_object(
      'letter',case when i between 0 and 4 then 'A' else null end,
      'owner',case when i between 0 and 3 then 1 when i=4 then 2 else 0 end,
      'bonus',null,
      'bonus_used',false
    ) order by i
  ) into v_board
  from generate_series(0,224) i;

  insert into public.word_siege_games(
    id,player_one_id,player_two_id,status,language,board,bag,player_one_rack,player_two_rack,
    player_one_word_score,player_two_word_score,player_one_area,player_two_area,move_count,
    winner_id,loser_id,result_applied,finished_at,finish_reason,
    player_one_area_score,player_two_area_score
  ) values (
    v_game,v_p1,v_p2,'finished','tr',v_board,'','','',15,7,4,1,3,
    v_p1,v_p2,true,now(),'staging_fixture',99,77
  );

  insert into public.word_siege_moves(
    game_id,player_id,primary_word,formed_words,placed_tiles,word_score,captured_cells,
    neutral_captured,opponent_captured,area_score,total_score,move_number,request_fingerprint
  ) values
    (v_game,v_p1,'ADA',array['ADA'],
      '[{"index":112,"letter":"A","owner":1},{"index":113,"letter":"D","owner":1}]'::jsonb,
      10,2,2,0,4,14,1,'staging-341-1'),
    (v_game,v_p2,'DAR',array['DAR'],
      '[{"index":114,"letter":"R","owner":2},{"index":115,"letter":"A","owner":2}]'::jsonb,
      7,3,2,1,6,13,2,'staging-341-2'),
    (v_game,v_p1,'ARA',array['ARA'],
      '[{"index":116,"letter":"A","owner":1}]'::jsonb,
      5,3,1,2,6,11,3,'staging-341-3');
end
$$;

select set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000341',true);
set local role authenticated;

do $$
declare
  v_game constant uuid := '00000000-0000-4000-8000-000000003419'::uuid;
  v_result jsonb;
  v_event jsonb;
begin
  v_result := public.admin_match_replay_v1(v_game,'siege',0,2);

  if v_result->>'mode' <> 'siege' then raise exception 'replay_mode_mismatch'; end if;
  if (v_result#>>'{page,total}')::integer <> 3 then raise exception 'replay_total_mismatch'; end if;
  if (v_result#>>'{page,returned}')::integer <> 2 then raise exception 'replay_page_returned_mismatch'; end if;
  if (v_result#>>'{page,has_more}')::boolean is not true then raise exception 'replay_has_more_missing'; end if;
  if (v_result#>>'{score_contract,cube_points}')::integer <> 2 then raise exception 'cube_points_contract_mismatch'; end if;
  if (v_result#>>'{score_contract,word_points_permanent}')::boolean is not true then raise exception 'word_points_contract_mismatch'; end if;
  if (v_result#>>'{replay_fidelity,exact_historical_capture_cell_indices}')::boolean is not false then
    raise exception 'historical_capture_fidelity_overstated';
  end if;

  if (v_result#>>'{final_score,player_one_owned_cubes}')::integer <> 4
     or (v_result#>>'{final_score,player_two_owned_cubes}')::integer <> 1 then
    raise exception 'final_board_ownership_mismatch';
  end if;
  if (v_result#>>'{final_score,player_one_territory_points}')::integer <> 8
     or (v_result#>>'{final_score,player_two_territory_points}')::integer <> 2 then
    raise exception 'final_territory_score_mismatch';
  end if;
  if (v_result#>>'{final_score,player_one_total}')::integer <> 23
     or (v_result#>>'{final_score,player_two_total}')::integer <> 9 then
    raise exception 'final_total_score_mismatch';
  end if;
  if jsonb_array_length(v_result->'final_board') <> 225 then raise exception 'final_board_size_mismatch'; end if;

  v_event := v_result->'events'->0;
  if (v_event#>>'{ownership_after,player_one_cubes}')::integer <> 2
     or (v_event#>>'{ownership_after,player_two_cubes}')::integer <> 0
     or (v_event#>>'{word_points_after,player_one}')::integer <> 10 then
    raise exception 'move_one_reconstruction_mismatch';
  end if;

  v_event := v_result->'events'->1;
  if (v_event#>>'{ownership_after,player_one_cubes}')::integer <> 1
     or (v_event#>>'{ownership_after,player_two_cubes}')::integer <> 3
     or (v_event#>>'{word_points_after,player_two}')::integer <> 7 then
    raise exception 'move_two_reconstruction_mismatch';
  end if;

  v_result := public.admin_match_replay_v1(v_game,'kelime_kusatmasi',2,500);
  if (v_result#>>'{page,limit}')::integer <> 100 then raise exception 'replay_limit_not_capped'; end if;
  if (v_result#>>'{page,returned}')::integer <> 1 then raise exception 'second_page_returned_mismatch'; end if;
  v_event := v_result->'events'->0;
  if (v_event#>>'{ownership_after,player_one_cubes}')::integer <> 4
     or (v_event#>>'{ownership_after,player_two_cubes}')::integer <> 1
     or (v_event#>>'{word_points_after,player_one}')::integer <> 15 then
    raise exception 'move_three_reconstruction_mismatch';
  end if;

  perform set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000003411',true);
  begin
    perform public.admin_match_replay_v1(v_game,'siege',0,10);
    raise exception 'non_admin_replay_allowed';
  exception when others then
    if sqlerrm='non_admin_replay_allowed' then raise; end if;
    if position('admin_required' in sqlerrm)=0 then
      raise exception 'unexpected_non_admin_error:%',sqlerrm;
    end if;
  end;
end
$$;

rollback;
