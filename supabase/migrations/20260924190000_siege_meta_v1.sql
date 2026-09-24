-- Kelime Kuşatması meta layer (new, versioned functions only; nothing existing changes):
--   * siege match history and rivals (head-to-head, last-10 record) for the match center / social
--   * siege daily missions tied to real siege events, claimed through unified_mission_claims
--     (scope 'daily'; the 'siege_' mission ids never collide with the unified route missions)
--   * a 7-day daily reward cycle on the existing daily_checkins table (one claim per day,
--     shared with claim_daily_checkin_v1 so the two can never both pay out)
--   * other players' equipped profile cosmetics (read-only, blocked players excluded)
-- All functions are SECURITY DEFINER with an empty search_path and act only for auth.uid().

-- ---------------------------------------------------------------- match history
create or replace function public.get_siege_match_history_v1(p_limit integer default 30)
returns table(
  game_id uuid, opponent_id uuid, display_name text, result text,
  my_word_score integer, my_area_score integer, their_word_score integer, their_area_score integer,
  my_cells integer, their_cells integer, finished_at timestamptz
)
language plpgsql stable security definer set search_path = '' as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  return query
  select g.id,
    case when g.player_one_id = v_uid then g.player_two_id else g.player_one_id end,
    coalesce(nullif(trim(p.display_name), ''), 'Oyuncu')::text,
    (case when g.winner_id is null then 'draw' when g.winner_id = v_uid then 'win' else 'loss' end)::text,
    case when g.player_one_id = v_uid then g.player_one_word_score else g.player_two_word_score end,
    case when g.player_one_id = v_uid then g.player_one_area_score else g.player_two_area_score end,
    case when g.player_one_id = v_uid then g.player_two_word_score else g.player_one_word_score end,
    case when g.player_one_id = v_uid then g.player_two_area_score else g.player_one_area_score end,
    case when g.player_one_id = v_uid then g.player_one_area else g.player_two_area end,
    case when g.player_one_id = v_uid then g.player_two_area else g.player_one_area end,
    coalesce(g.finished_at, g.updated_at)
  from public.word_siege_games g
  left join public.profiles p on p.id = case when g.player_one_id = v_uid then g.player_two_id else g.player_one_id end
  where g.status = 'finished'
    and g.player_two_id is not null
    and (g.player_one_id = v_uid or g.player_two_id = v_uid)
  order by coalesce(g.finished_at, g.updated_at) desc
  limit least(greatest(coalesce(p_limit, 30), 1), 100);
end $$;

-- ---------------------------------------------------------------- rivals
create or replace function public.get_siege_rivals_v1(p_limit integer default 20)
returns table(
  opponent_id uuid, display_name text, matches integer, wins integer, losses integer, draws integer,
  last10_wins integer, last10_losses integer, last_played_at timestamptz, is_friend boolean
)
language plpgsql stable security definer set search_path = '' as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  return query
  with duels as (
    select case when g.player_one_id = v_uid then g.player_two_id else g.player_one_id end as rival,
      g.winner_id = v_uid as won,
      g.winner_id is not null and g.winner_id <> v_uid as lost,
      g.winner_id is null as drawn,
      coalesce(g.finished_at, g.updated_at) as played_at
    from public.word_siege_games g
    where g.status = 'finished' and g.player_two_id is not null
      and (g.player_one_id = v_uid or g.player_two_id = v_uid)
  ), ranked as (
    select d.*, row_number() over (partition by d.rival order by d.played_at desc) as rn from duels d
  )
  select r.rival,
    coalesce(nullif(trim(p.display_name), ''), 'Oyuncu')::text,
    count(*)::int,
    count(*) filter (where r.won)::int,
    count(*) filter (where r.lost)::int,
    count(*) filter (where r.drawn)::int,
    count(*) filter (where r.won and r.rn <= 10)::int,
    count(*) filter (where r.lost and r.rn <= 10)::int,
    max(r.played_at),
    exists (
      select 1 from public.friendships f
      where f.user_id = least(v_uid, r.rival) and f.friend_id = greatest(v_uid, r.rival) and f.status = 'accepted'
    )
  from ranked r
  left join public.profiles p on p.id = r.rival
  where not exists (
    select 1 from public.user_blocks b
    where (b.blocker_id = v_uid and b.blocked_id = r.rival) or (b.blocker_id = r.rival and b.blocked_id = v_uid)
  )
  group by r.rival, p.display_name
  order by count(*) desc, max(r.played_at) desc
  limit least(greatest(coalesce(p_limit, 20), 1), 50);
end $$;

-- ---------------------------------------------------------------- daily missions
create or replace function public.get_siege_missions_v1()
returns table(
  mission_id text, title_tr text, title_en text, target integer, progress integer,
  reward_coins integer, completed boolean, claimed boolean, period_start date
)
language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_today date := (timezone('Europe/Istanbul', clock_timestamp()))::date;
  v_from timestamptz := (date_trunc('day', timezone('Europe/Istanbul', clock_timestamp())) at time zone 'Europe/Istanbul');
  v_finished integer := 0;
  v_cells integer := 0;
  v_words integer := 0;
  v_best_control integer := 0;
  v_challenges integer := 0;
  v_rematches integer := 0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select count(*)::int into v_finished from public.word_siege_games g
  where g.status = 'finished' and g.player_two_id is not null
    and (g.player_one_id = v_uid or g.player_two_id = v_uid)
    and coalesce(g.finished_at, g.updated_at) >= v_from;

  select coalesce(sum(m.captured_cells), 0)::int, count(*)::int into v_cells, v_words
  from public.word_siege_moves m
  where m.player_id = v_uid and m.created_at >= v_from;

  -- Best map control (share of the 225 cells) in any match touched today.
  select coalesce(max(case when g.player_one_id = v_uid then g.player_one_area else g.player_two_area end * 100 / 225), 0)::int
  into v_best_control
  from public.word_siege_games g
  where (g.player_one_id = v_uid or g.player_two_id = v_uid) and g.updated_at >= v_from;

  select count(*)::int into v_challenges from public.word_siege_invites i
  where i.sender_id = v_uid and i.created_at >= v_from;

  -- A rematch: a match started today against someone already played before today.
  select count(*)::int into v_rematches from public.word_siege_games g
  where g.player_two_id is not null and (g.player_one_id = v_uid or g.player_two_id = v_uid)
    and g.created_at >= v_from
    and exists (
      select 1 from public.word_siege_games h
      where h.status = 'finished' and h.created_at < v_from
        and ((h.player_one_id = g.player_one_id and h.player_two_id = g.player_two_id)
          or (h.player_one_id = g.player_two_id and h.player_two_id = g.player_one_id))
    );

  return query
  with defs(mid, ttr, ten, tgt, prog, reward, ord) as (
    values
      ('siege_complete_2', '2 kuşatma maçı tamamla', 'Finish 2 siege matches', 2, v_finished, 30, 1),
      ('siege_capture_8', '8 bölge ele geçir', 'Capture 8 cells', 8, v_cells, 25, 2),
      ('siege_control_60', 'Bir maçta %60 harita kontrolüne ulaş', 'Reach 60% map control in a match', 60, v_best_control, 40, 3),
      ('siege_words_6', '6 geçerli kelime oluştur', 'Play 6 valid words', 6, v_words, 20, 4),
      ('siege_challenge', 'Bir arkadaşına meydan oku', 'Challenge a friend', 1, v_challenges, 20, 5),
      ('siege_rematch', 'Rövanş maçı oyna', 'Play a rematch', 1, v_rematches, 25, 6)
  )
  select d.mid::text, d.ttr::text, d.ten::text, d.tgt, least(d.prog, d.tgt), d.reward,
    d.prog >= d.tgt,
    exists (select 1 from public.unified_mission_claims c
            where c.user_id = v_uid and c.mission_id = d.mid and c.period_start = v_today),
    v_today
  from defs d order by d.ord;
end $$;

create or replace function public.claim_siege_mission_v1(p_mission_id text)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_uid uuid := auth.uid(); m record; v_balance integer := 0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select * into m from public.get_siege_missions_v1() x where x.mission_id = p_mission_id limit 1;
  if m.mission_id is null then raise exception 'mission_not_found'; end if;
  if not m.completed then raise exception 'mission_not_complete'; end if;
  if m.claimed then raise exception 'mission_already_claimed'; end if;
  insert into public.unified_mission_claims(user_id, mission_id, scope, period_start, reward_coins)
  values (v_uid, m.mission_id, 'daily', m.period_start, m.reward_coins)
  on conflict (user_id, mission_id, period_start) do nothing;
  if not found then raise exception 'mission_already_claimed'; end if;
  update public.profiles p set diamonds = coalesce(p.diamonds, 0) + m.reward_coins where p.id = v_uid
  returning p.diamonds into v_balance;
  insert into public.diamond_ledger(user_id, delta, reason) values (v_uid, m.reward_coins, 'siege_mission:' || m.mission_id);
  return jsonb_build_object('success', true, 'mission_id', m.mission_id, 'reward_coins', m.reward_coins, 'balance', v_balance);
end $$;

-- ---------------------------------------------------------------- 7-day daily reward
-- Day N of the cycle follows the streak of consecutive daily claims; missing a day restarts at 1.
-- Rewards are coins only (no competitive power); PRO doubles them, as the old daily check-in did.
create or replace function public.siege_daily_cycle_rewards_v1()
returns integer[] language sql immutable set search_path = '' as $$ select array[30, 40, 50, 60, 80, 100, 150] $$;

create or replace function public.get_daily_reward_cycle_v1()
returns table(cycle_day integer, streak integer, claimed_today boolean, today_reward integer, rewards integer[], vip boolean)
language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
  v_claimed boolean := false;
  v_streak integer := 0;
  v_day date;
  v_mult integer;
  v_rewards integer[] := public.siege_daily_cycle_rewards_v1();
  v_cycle integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select coalesce(p.is_vip, false) into v_vip from public.profiles p where p.id = v_uid;
  v_mult := case when v_vip then 2 else 1 end;
  select exists(select 1 from public.daily_checkins c where c.user_id = v_uid and c.checkin_date = current_date) into v_claimed;
  -- Streak of consecutive claimed days before today.
  v_day := current_date - 1;
  while exists(select 1 from public.daily_checkins c where c.user_id = v_uid and c.checkin_date = v_day) loop
    v_streak := v_streak + 1;
    v_day := v_day - 1;
    exit when v_streak >= 365;
  end loop;
  -- Today's cycle day: the next one if not claimed yet, the one claimed if it was.
  v_cycle := (v_streak % 7) + 1;
  return query select v_cycle, v_streak + case when v_claimed then 1 else 0 end, v_claimed,
    v_rewards[v_cycle] * v_mult,
    array(select r * v_mult from unnest(v_rewards) r),
    v_vip;
end $$;

create or replace function public.claim_daily_reward_cycle_v1()
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_uid uuid := auth.uid(); s record; v_balance integer := 0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select * into s from public.get_daily_reward_cycle_v1();
  if s.claimed_today then
    return jsonb_build_object('success', false, 'reason', 'already_claimed', 'cycle_day', s.cycle_day);
  end if;
  insert into public.daily_checkins(user_id, checkin_date, reward_diamonds)
  values (v_uid, current_date, s.today_reward)
  on conflict (user_id, checkin_date) do nothing;
  if not found then
    return jsonb_build_object('success', false, 'reason', 'already_claimed', 'cycle_day', s.cycle_day);
  end if;
  update public.profiles p set diamonds = coalesce(p.diamonds, 0) + s.today_reward, updated_at = now()
  where p.id = v_uid returning p.diamonds into v_balance;
  insert into public.diamond_ledger(user_id, delta, reason)
  values (v_uid, s.today_reward, 'daily_cycle_day_' || s.cycle_day);
  return jsonb_build_object('success', true, 'cycle_day', s.cycle_day, 'reward', s.today_reward, 'balance', v_balance);
end $$;

-- ---------------------------------------------------------------- public cosmetics
create or replace function public.get_public_cosmetics_v1(p_user_id uuid)
returns table(profile_frame_id text, name_style_id text, badge_id text, title_style_id text, nameplate_id text)
language plpgsql stable security definer set search_path = '' as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if exists (
    select 1 from public.user_blocks b
    where (b.blocker_id = v_uid and b.blocked_id = p_user_id) or (b.blocker_id = p_user_id and b.blocked_id = v_uid)
  ) then return; end if;
  return query
  select e.profile_frame_id, e.name_style_id, e.badge_id, e.title_style_id, e.nameplate_id
  from public.user_equipped_cosmetics e where e.user_id = p_user_id;
end $$;

-- ---------------------------------------------------------------- grants
revoke all on function public.get_siege_match_history_v1(integer) from public, anon;
revoke all on function public.get_siege_rivals_v1(integer) from public, anon;
revoke all on function public.get_siege_missions_v1() from public, anon;
revoke all on function public.claim_siege_mission_v1(text) from public, anon;
revoke all on function public.get_daily_reward_cycle_v1() from public, anon;
revoke all on function public.claim_daily_reward_cycle_v1() from public, anon;
revoke all on function public.get_public_cosmetics_v1(uuid) from public, anon;
grant execute on function public.get_siege_match_history_v1(integer) to authenticated;
grant execute on function public.get_siege_rivals_v1(integer) to authenticated;
grant execute on function public.get_siege_missions_v1() to authenticated;
grant execute on function public.claim_siege_mission_v1(text) to authenticated;
grant execute on function public.get_daily_reward_cycle_v1() to authenticated;
grant execute on function public.claim_daily_reward_cycle_v1() to authenticated;
grant execute on function public.get_public_cosmetics_v1(uuid) to authenticated;
