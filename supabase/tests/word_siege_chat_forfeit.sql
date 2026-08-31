begin;

-- Package 5/6 regression coverage for the already-shared chat + forfeit infrastructure.
do $$
declare
  p1 uuid := gen_random_uuid();
  p2 uuid := gen_random_uuid();
  gid uuid;
  g public.word_siege_games;
  msg_count integer;
begin
  insert into auth.users(id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
  values
    (p1, 'authenticated', 'authenticated', 'package5-p1-' || p1 || '@example.test', '', now(), now(), now()),
    (p2, 'authenticated', 'authenticated', 'package5-p2-' || p2 || '@example.test', '', now(), now(), now());

  insert into public.word_siege_games(
    player_one_id, player_two_id, status, language, current_player_id,
    board, bag, player_one_rack, player_two_rack, last_action
  ) values (
    p1, p2, 'playing', 'tr', p1,
    private.word_siege_new_board_v1(), 'ABCDEFG', 'KALEMTR', 'MASASİN', 'game_started'
  ) returning id into gid;

  perform set_config('request.jwt.claim.sub', p1::text, true);
  perform set_config('request.jwt.claim.role', 'authenticated', true);
  set local role authenticated;

  -- Chat stays inside the match infrastructure and uses the participant RLS path.
  insert into public.word_siege_messages(game_id, sender_id, body)
  values (gid, p1, 'Paket 5 sohbet testi');

  select count(*) into msg_count
  from public.word_siege_messages
  where game_id = gid and sender_id = p1 and body = 'Paket 5 sohbet testi';

  if msg_count <> 1 then
    raise exception 'package5_chat_insert_failed';
  end if;

  -- Forfeit must finish the multiplayer game atomically through the existing result path.
  select * into g from public.forfeit_word_siege_game_v1(gid);

  if g.status <> 'finished' then
    raise exception 'package5_forfeit_status_failed';
  end if;
  if g.current_player_id is not null then
    raise exception 'package5_forfeit_turn_not_cleared';
  end if;
  if g.winner_id <> p2 then
    raise exception 'package5_forfeit_winner_failed';
  end if;
  if g.finish_reason <> 'forfeit' then
    raise exception 'package5_forfeit_reason_failed';
  end if;
  if not g.result_applied then
    raise exception 'package5_forfeit_result_not_applied';
  end if;
  if g.last_action <> 'forfeit' or g.last_action_player_id <> p1 then
    raise exception 'package5_forfeit_action_failed';
  end if;
end $$;

rollback;
