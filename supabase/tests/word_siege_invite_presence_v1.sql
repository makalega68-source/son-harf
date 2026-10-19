-- Transactional regression: no invites, friendships, games or profile changes survive.
begin;
do $test$
declare
  participants uuid[];
  sender uuid;
  receiver uuid;
  invite_id uuid;
  room public.word_siege_games;
begin
  select array_agg(id) into participants from (
    select p.id from public.profiles p
    where not exists (
      select 1 from public.word_siege_games g
      where g.status in ('waiting','playing') and p.id in (g.player_one_id,g.player_two_id)
    ) order by p.id limit 2
  ) eligible;
  if cardinality(participants) <> 2 then raise exception 'Two available profiles required'; end if;
  sender := participants[1]; receiver := participants[2];
  update public.profiles set presence_status='online' where id in(sender,receiver);
  delete from public.user_blocks where
    (blocker_id=sender and blocked_id=receiver) or (blocker_id=receiver and blocked_id=sender);
  insert into public.friendships(user_id,friend_id,requested_by,status)
  values(least(sender,receiver),greatest(sender,receiver),sender,'accepted')
  on conflict(user_id,friend_id) do update set status='accepted';
  insert into public.word_siege_invites(sender_id,receiver_id,language)
  values(sender,receiver,'tr') returning id into invite_id;
  perform set_config('request.jwt.claim.sub', receiver::text, true);
  perform set_config('request.jwt.claims', jsonb_build_object('sub',receiver,'role','authenticated')::text, true);
  room := public.respond_word_siege_invite_v1(invite_id,true);
  if room.status <> 'playing' or room.player_one_id <> sender or room.player_two_id <> receiver then
    raise exception 'Acceptance did not create the correct two-player room';
  end if;
  if length(room.player_one_rack) <> 7 or length(room.player_two_rack) <> 7 then
    raise exception 'Missing player rack';
  end if;
  if (select game_id from public.word_siege_invites where id=invite_id) <> room.id then
    raise exception 'Accepted invite does not point to its room';
  end if;
  if (select presence_status from public.profiles where id=sender) <> 'online' then
    raise exception 'Sender was falsely marked inside a game';
  end if;
  if (select presence_status from public.profiles where id=receiver) <> 'in_game' then
    raise exception 'Accepting player was not marked inside the game';
  end if;
end
$test$;
rollback;
