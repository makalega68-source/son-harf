begin;

-- Async 12/72-hour lifecycle regression. Everything is rolled back.
-- Isolate matchmaking from any real waiting game while keeping live data untouched after rollback.
update public.word_siege_games set status='cancelled' where status='waiting';

insert into auth.users(id, aud, role, email, encrypted_password, email_confirmed_at, created_at, updated_at)
values
 ('a1200000-0000-4000-8000-000000000001','authenticated','authenticated','async12-a@example.test','',now(),now(),now()),
 ('a1200000-0000-4000-8000-000000000002','authenticated','authenticated','async12-b@example.test','',now(),now(),now()),
 ('a7200000-0000-4000-8000-000000000001','authenticated','authenticated','async72-a@example.test','',now(),now(),now()),
 ('a7200000-0000-4000-8000-000000000002','authenticated','authenticated','async72-b@example.test','',now(),now(),now()),
 ('a1200000-0000-4000-8000-000000000003','authenticated','authenticated','async12-c@example.test','',now(),now(),now()),
 ('a7200000-0000-4000-8000-000000000003','authenticated','authenticated','async72-c@example.test','',now(),now(),now())
on conflict (id) do nothing;

insert into public.profiles(id, display_name)
values
 ('a1200000-0000-4000-8000-000000000001','Async 12 A'),
 ('a1200000-0000-4000-8000-000000000002','Async 12 B'),
 ('a7200000-0000-4000-8000-000000000001','Async 72 A'),
 ('a7200000-0000-4000-8000-000000000002','Async 72 B'),
 ('a1200000-0000-4000-8000-000000000003','Async 12 C'),
 ('a7200000-0000-4000-8000-000000000003','Async 72 C')
on conflict (id) do nothing;

-- Pool separation: a 72h seeker must not join a 12h waiting game.
select set_config('request.jwt.claim.sub','a1200000-0000-4000-8000-000000000001',true);
select set_config('request.jwt.claim.role','authenticated',true);
set local role authenticated;
select public.find_or_create_word_siege_game_v2('tr',12);
reset role;

select set_config('request.jwt.claim.sub','a7200000-0000-4000-8000-000000000001',true);
set local role authenticated;
select public.find_or_create_word_siege_game_v2('tr',72);
reset role;

do $$
begin
  if (select count(*) from public.word_siege_games where status='waiting' and player_one_id in ('a1200000-0000-4000-8000-000000000001','a7200000-0000-4000-8000-000000000001')) <> 2 then
    raise exception 'async_duration_pools_mixed';
  end if;
end $$;

-- A second 12h player joins only the 12h queue and arms a 12h server deadline.
select set_config('request.jwt.claim.sub','a1200000-0000-4000-8000-000000000002',true);
set local role authenticated;
select public.find_or_create_word_siege_game_v2('tr',12);
reset role;

do $$
declare g public.word_siege_games;
begin
  select * into g from public.word_siege_games
  where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12;
  if g.status <> 'playing' or g.player_two_id <> 'a1200000-0000-4000-8000-000000000002' then raise exception 'async_12_match_failed'; end if;
  if g.current_player_id <> g.player_one_id then raise exception 'async_12_first_turn_failed'; end if;
  if abs(extract(epoch from (g.turn_deadline-g.turn_started_at))-43200) > 2 then raise exception 'async_12_deadline_failed'; end if;
end $$;

-- Pass is a legal turn change and must start a fresh full deadline for the rival.
select set_config('request.jwt.claim.sub','a1200000-0000-4000-8000-000000000001',true);
set local role authenticated;
select public.pass_word_siege_turn_v1((select id from public.word_siege_games where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12));
reset role;

do $$
declare g public.word_siege_games;
begin
  select * into g from public.word_siege_games where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12;
  if g.current_player_id <> 'a1200000-0000-4000-8000-000000000002' then raise exception 'async_pass_turn_failed'; end if;
  if g.consecutive_passes <> 1 then raise exception 'async_pass_count_failed'; end if;
  if abs(extract(epoch from (g.turn_deadline-g.turn_started_at))-43200) > 2 then raise exception 'async_pass_deadline_failed'; end if;
end $$;

-- Force the current player's server deadline into the past. A late pass must NOT execute;
-- the same RPC atomically finalizes timeout instead.
update public.word_siege_games
set turn_started_at=clock_timestamp()-interval '13 hours', turn_deadline=clock_timestamp()-interval '1 hour'
where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12;

select set_config('request.jwt.claim.sub','a1200000-0000-4000-8000-000000000002',true);
set local role authenticated;
select public.pass_word_siege_turn_v1((select id from public.word_siege_games where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12));
reset role;

do $$
declare g public.word_siege_games; first_finished timestamptz; gid uuid;
begin
  select * into g from public.word_siege_games where player_one_id='a1200000-0000-4000-8000-000000000001' and turn_duration_hours=12;
  gid := g.id;
  if g.status <> 'finished' then raise exception 'async_timeout_status_failed'; end if;
  if g.winner_id <> 'a1200000-0000-4000-8000-000000000001' then raise exception 'async_timeout_winner_failed'; end if;
  if g.loser_id <> 'a1200000-0000-4000-8000-000000000002' then raise exception 'async_timeout_loser_failed'; end if;
  if g.finish_reason <> 'timeout' or g.last_action <> 'timeout' then raise exception 'async_timeout_reason_failed'; end if;
  if g.current_player_id is not null or g.turn_deadline is not null then raise exception 'async_timeout_turn_not_cleared'; end if;
  if g.consecutive_passes <> 1 then raise exception 'async_late_pass_was_applied'; end if;
  first_finished := g.finished_at;
  perform private.word_siege_finalize_timeout_v2(gid);
  select * into g from public.word_siege_games where id=gid;
  if g.finished_at is distinct from first_finished then raise exception 'async_timeout_not_idempotent'; end if;
end $$;

-- 72h pool also matches only itself and arms a 72h deadline.
select set_config('request.jwt.claim.sub','a7200000-0000-4000-8000-000000000002',true);
set local role authenticated;
select public.find_or_create_word_siege_game_v2('tr',72);
reset role;

do $$
declare g public.word_siege_games;
begin
  select * into g from public.word_siege_games where player_one_id='a7200000-0000-4000-8000-000000000001' and turn_duration_hours=72;
  if g.status <> 'playing' or g.player_two_id <> 'a7200000-0000-4000-8000-000000000002' then raise exception 'async_72_match_failed'; end if;
  if abs(extract(epoch from (g.turn_deadline-g.turn_started_at))-259200) > 2 then raise exception 'async_72_deadline_failed'; end if;
end $$;

-- A non-participant cannot see or refresh another player's match. RLS may deliberately
-- collapse this to not_found instead of revealing that the game exists.
select set_config('request.jwt.claim.sub','a1200000-0000-4000-8000-000000000003',true);
set local role authenticated;
do $$
begin
  begin
    perform public.refresh_word_siege_game_v2((select id from public.word_siege_games where player_one_id='a7200000-0000-4000-8000-000000000001' and turn_duration_hours=72));
    raise exception 'async_nonparticipant_refresh_was_allowed';
  exception when others then
    if position('word_siege_not_participant' in sqlerrm)=0 and position('word_siege_not_found' in sqlerrm)=0 then raise; end if;
  end;
end $$;
reset role;

rollback;
