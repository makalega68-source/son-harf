-- Paket 3 targeted validation regression tests.
-- Self-contained fixtures; the transaction rolls back all auth/profile/game data.

begin;

do $$
declare
  p1 uuid := gen_random_uuid();
  p2 uuid := gen_random_uuid();
  g uuid;
  b jsonb;
  before_game jsonb;
  after_game jsonb;
  before_moves bigint;
  after_moves bigint;
begin
  insert into auth.users(id, aud, role, email, created_at, updated_at)
  values
    (p1, 'authenticated', 'authenticated', 'word-siege-test-1@example.invalid', now(), now()),
    (p2, 'authenticated', 'authenticated', 'word-siege-test-2@example.invalid', now(), now());
  insert into public.profiles(id, display_name)
  values (p1, 'WS Test One'), (p2, 'WS Test Two');

  perform set_config('request.jwt.claim.sub', p1::text, true);

  -- 1) ARA + Ç = ARAÇ -> kabul.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{38,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{39,letter}', '"R"'::jsonb);
  b := jsonb_set(b, '{40,letter}', '"A"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (p1, p2, 'playing', 'tr', p1, b, 'EEEEEEE', 'ÇEEEEEE', 'EEEEEEE')
  returning id into g;
  perform public.submit_word_siege_move_v1(g, '[{"index":41,"rack_index":0}]'::jsonb, true);
  if not exists (
    select 1 from public.word_siege_moves
    where game_id = g and primary_word = 'ARAÇ' and 'ARAÇ' = any(formed_words)
  ) then raise exception 'test_1_arac_accept_failed'; end if;

  -- 2) MAKALE + B = MAKALEB -> reddet.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{36,letter}', '"M"'::jsonb);
  b := jsonb_set(b, '{37,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{38,letter}', '"K"'::jsonb);
  b := jsonb_set(b, '{39,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{40,letter}', '"L"'::jsonb);
  b := jsonb_set(b, '{41,letter}', '"E"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (p1, p2, 'playing', 'tr', p1, b, 'EEEEEEE', 'BEEEEEE', 'EEEEEEE')
  returning id into g;
  begin
    perform public.submit_word_siege_move_v1(g, '[{"index":42,"rack_index":0}]'::jsonb, true);
    raise exception 'test_2_expected_rejection';
  exception when others then
    if position('word_siege_invalid_word:MAKALEB' in sqlerrm) = 0 then raise; end if;
  end;

  -- 3) Mevcut A + T/M = TAM -> uygun şartlarda kabul.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{40,letter}', '"A"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (p1, p2, 'playing', 'tr', p1, b, 'EEEEEEE', 'TMEEEEE', 'EEEEEEE')
  returning id into g;
  perform public.submit_word_siege_move_v1(
    g,
    '[{"index":39,"rack_index":0},{"index":41,"rack_index":1}]'::jsonb,
    true
  );
  if not exists (
    select 1 from public.word_siege_moves where game_id = g and primary_word = 'TAM'
  ) then raise exception 'test_3_tam_accept_failed'; end if;

  -- 4) Ana kelime TAM geçerli, yeni T taşının dikey çaprazı ĞT geçersiz -> reddet.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{40,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{30,letter}', '"Ğ"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (p1, p2, 'playing', 'tr', p1, b, 'EEEEEEE', 'TMEEEEE', 'EEEEEEE')
  returning id into g;
  begin
    perform public.submit_word_siege_move_v1(
      g,
      '[{"index":39,"rack_index":0},{"index":41,"rack_index":1}]'::jsonb,
      true
    );
    raise exception 'test_4_expected_cross_rejection';
  exception when others then
    if position('word_siege_invalid_word:ĞT' in sqlerrm) = 0 then raise; end if;
  end;

  -- 5) İlk hamle değilken tahtadan kopuk AT -> reddet.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{0,letter}', '"A"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (p1, p2, 'playing', 'tr', p1, b, 'EEEEEEE', 'ATEEEEE', 'EEEEEEE')
  returning id into g;
  begin
    perform public.submit_word_siege_move_v1(
      g,
      '[{"index":39,"rack_index":0},{"index":40,"rack_index":1}]'::jsonb,
      true
    );
    raise exception 'test_5_expected_disconnected_rejection';
  exception when others then
    if position('word_siege_move_must_connect' in sqlerrm) = 0 then raise; end if;
  end;

  -- 6) Geçersiz hamlede board/score/area/turn/rack/bag/move history değişmemeli.
  b := private.word_siege_new_board_v1();
  b := jsonb_set(b, '{36,letter}', '"M"'::jsonb);
  b := jsonb_set(b, '{37,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{38,letter}', '"K"'::jsonb);
  b := jsonb_set(b, '{39,letter}', '"A"'::jsonb);
  b := jsonb_set(b, '{40,letter}', '"L"'::jsonb);
  b := jsonb_set(b, '{41,letter}', '"E"'::jsonb);
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack,
    player_one_word_score, player_two_word_score, player_one_area, player_two_area,
    move_count
  ) values (
    p1, p2, 'playing', 'tr', p1,
    b, 'QWERTYU', 'BASDFGH', 'ZXCVBNM',
    17, 13, 4, 3, 9
  ) returning id into g;

  select to_jsonb(x), (select count(*) from public.word_siege_moves where game_id = g)
  into before_game, before_moves
  from (
    select board, bag, player_one_rack, player_two_rack,
           player_one_word_score, player_two_word_score,
           player_one_area, player_two_area, current_player_id, move_count
    from public.word_siege_games where id = g
  ) x;

  begin
    perform public.submit_word_siege_move_v1(g, '[{"index":42,"rack_index":0}]'::jsonb, true);
    raise exception 'test_6_expected_rejection';
  exception when others then
    if position('word_siege_invalid_word:MAKALEB' in sqlerrm) = 0 then raise; end if;
  end;

  select to_jsonb(x), (select count(*) from public.word_siege_moves where game_id = g)
  into after_game, after_moves
  from (
    select board, bag, player_one_rack, player_two_rack,
           player_one_word_score, player_two_word_score,
           player_one_area, player_two_area, current_player_id, move_count
    from public.word_siege_games where id = g
  ) x;

  if before_game is distinct from after_game or before_moves <> after_moves then
    raise exception 'test_6_invalid_move_mutated_state';
  end if;
end
$$;

rollback;
