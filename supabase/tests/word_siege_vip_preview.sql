-- Package 7 targeted preview tests.
-- Canonical scoring/validation only; all fixtures and function changes roll back.

begin;

do $$
declare
  p1 uuid := gen_random_uuid();
  p2 uuid := gen_random_uuid();
  g uuid;
  p jsonb;
  before_board jsonb;
  after_board jsonb;
  before_score integer;
  after_score integer;
begin
  insert into auth.users(id, aud, role, email, created_at, updated_at, raw_user_meta_data)
  values
    (p1, 'authenticated', 'authenticated', 'word-siege-vip-preview-1@example.invalid', now(), now(), jsonb_build_object('display_name', 'VIP Preview One')),
    (p2, 'authenticated', 'authenticated', 'word-siege-vip-preview-2@example.invalid', now(), now(), jsonb_build_object('display_name', 'VIP Preview Two'));

  perform set_config('request.jwt.claim.sub', p1::text, true);

  -- Valid first move TAM across center 2K.
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (
    p1, p2, 'playing', 'tr', p1,
    private.word_siege_new_board_v1(), 'ABCDEFG', 'TAMXXXX', 'YYYYYYY'
  ) returning id into g;

  select board, player_one_word_score into before_board, before_score
  from public.word_siege_games where id = g;

  p := private.word_siege_preview_move_v1(
    g,
    '[{"index":39,"rack_index":0},{"index":40,"rack_index":1},{"index":41,"rack_index":2}]'::jsonb,
    true
  );

  if coalesce((p ->> 'valid')::boolean, false) is not true then
    raise exception 'test_valid_preview_rejected:%', p;
  end if;
  if (p ->> 'base_word_score')::integer <> 4 then
    raise exception 'test_base_score_expected_4:%', p;
  end if;
  if (p ->> 'word_score')::integer <> 8 then
    raise exception 'test_word_score_expected_8:%', p;
  end if;
  if (p ->> 'bonus_score')::integer <> 4 then
    raise exception 'test_bonus_expected_4:%', p;
  end if;
  if (p ->> 'area_score')::integer <> 3 then
    raise exception 'test_area_expected_3:%', p;
  end if;
  if (p ->> 'total_score')::integer <> 11 then
    raise exception 'test_total_expected_11:%', p;
  end if;

  select board, player_one_word_score into after_board, after_score
  from public.word_siege_games where id = g;
  if before_board <> after_board or before_score <> after_score then
    raise exception 'test_preview_must_not_mutate_game';
  end if;

  -- Invalid first move MAKALEB must never produce a valid score preview.
  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack
  ) values (
    p1, p2, 'playing', 'tr', p1,
    private.word_siege_new_board_v1(), 'ABCDEFG', 'MAKALEB', 'YYYYYYY'
  ) returning id into g;

  p := private.word_siege_preview_move_v1(
    g,
    '[{"index":37,"rack_index":0},{"index":38,"rack_index":1},{"index":39,"rack_index":2},{"index":40,"rack_index":3},{"index":41,"rack_index":4},{"index":42,"rack_index":5},{"index":43,"rack_index":6}]'::jsonb,
    true
  );

  if coalesce((p ->> 'valid')::boolean, true) is not false then
    raise exception 'test_invalid_preview_was_valid:%', p;
  end if;
  if position('MAKALEB' in coalesce(p ->> 'reason', '')) = 0 then
    raise exception 'test_invalid_reason_missing_word:%', p;
  end if;
end $$;

rollback;
