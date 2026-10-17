-- Kelime Atölyesi competition v1.
--  * Günlük Yarış: one official run per player, language and day (Istanbul time). Everyone gets the
--    same starting letters (the client seeds the round from language + day). Starting the run
--    records it, so quitting a bad run and replaying is impossible; the score is accepted only
--    for that started run, within 15 minutes, and within plausible bounds.
--  * Leaderboards: today's top list and this week's total (Monday–Sunday), with the caller's rank.
--  * Weekly rewards: last week's top 10 claim Son Coin once.

create table if not exists public.atelier_daily_runs (
  user_id uuid not null references public.profiles(id) on delete cascade,
  language text not null check (language in ('tr','en')),
  day date not null,
  started_at timestamptz not null default now(),
  finished_at timestamptz,
  score integer not null default 0 check (score between 0 and 3000),
  words integer not null default 0 check (words between 0 and 60),
  tasks integer not null default 0 check (tasks between 0 and 3),
  primary key (user_id, language, day)
);
create index if not exists atelier_daily_runs_board_idx on public.atelier_daily_runs(language, day, score desc) where finished_at is not null;
alter table public.atelier_daily_runs enable row level security;
revoke all on public.atelier_daily_runs from anon, authenticated;

create table if not exists public.atelier_weekly_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  language text not null,
  week_start date not null,
  rank integer not null,
  reward integer not null,
  claimed_at timestamptz not null default now(),
  primary key (user_id, language, week_start)
);
alter table public.atelier_weekly_claims enable row level security;
revoke all on public.atelier_weekly_claims from anon, authenticated;

create or replace function public.atelier_today_v1()
returns date language sql stable set search_path = '' as $$
  select (now() at time zone 'Europe/Istanbul')::date
$$;

-- Start today's official run (or report the one already played).
create or replace function public.start_atelier_daily_v1(p_language text)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_day date := public.atelier_today_v1();
  v_run public.atelier_daily_runs%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_run from public.atelier_daily_runs where user_id = v_uid and language = v_lang and day = v_day;
  if found then
    return jsonb_build_object('started', false, 'already_played', true, 'day', v_day,
      'finished', v_run.finished_at is not null, 'score', v_run.score);
  end if;
  insert into public.atelier_daily_runs(user_id, language, day) values (v_uid, v_lang, v_day);
  return jsonb_build_object('started', true, 'already_played', false, 'day', v_day);
end;
$$;

-- Finish today's official run with the final score.
create or replace function public.finish_atelier_daily_v1(p_language text, p_score integer, p_words integer, p_tasks integer)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_day date := public.atelier_today_v1();
  v_rank integer;
  v_total integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_score is null or p_score < 0 or p_score > 3000 or p_words < 0 or p_words > 60 or p_tasks < 0 or p_tasks > 3 then
    raise exception 'implausible_score';
  end if;
  update public.atelier_daily_runs
  set score = p_score, words = p_words, tasks = p_tasks, finished_at = now()
  where user_id = v_uid and language = v_lang
    and day in (v_day, v_day - 1)            -- a run started just before midnight still counts
    and finished_at is null
    and started_at > now() - interval '15 minutes';
  if not found then raise exception 'no_open_run'; end if;

  select count(*) + 1 into v_rank from public.atelier_daily_runs
  where language = v_lang and day = v_day and finished_at is not null and score > p_score;
  select count(*) into v_total from public.atelier_daily_runs
  where language = v_lang and day = v_day and finished_at is not null;
  return jsonb_build_object('rank', v_rank, 'total', v_total, 'score', p_score);
end;
$$;

-- Leaderboard: p_scope 'daily' (today) or 'weekly' (this week's total).
create or replace function public.get_atelier_board_v1(p_language text, p_scope text default 'daily')
returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_day date := public.atelier_today_v1();
  v_week date := date_trunc('week', v_day)::date;
  v_rows jsonb;
  v_me jsonb;
  v_total integer;
  v_today jsonb;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  with scores as (
    select r.user_id, sum(r.score)::int as score, count(*)::int as days
    from public.atelier_daily_runs r
    where r.language = v_lang and r.finished_at is not null
      and (case when p_scope = 'weekly' then r.day between v_week and v_day else r.day = v_day end)
    group by r.user_id
  ), ranked as (
    select s.*, rank() over (order by s.score desc)::int as rank,
           row_number() over (order by s.score desc, s.user_id)::int as pos
    from scores s
  )
  select
    coalesce((select jsonb_agg(jsonb_build_object(
        'rank', x.rank, 'user_id', x.user_id, 'score', x.score, 'days', x.days,
        'name', coalesce(p.display_name, 'Oyuncu'),
        'avatar_path', case when coalesce(p.avatar_visibility, 'public') = 'hidden' then null else p.avatar_path end,
        'me', x.user_id = v_uid) order by x.pos)
      from ranked x join public.profiles p on p.id = x.user_id where x.pos <= 50), '[]'::jsonb),
    (select jsonb_build_object('rank', x.rank, 'score', x.score) from ranked x where x.user_id = v_uid),
    (select count(*)::int from ranked)
  into v_rows, v_me, v_total;

  select jsonb_build_object('started', true, 'finished', r.finished_at is not null, 'score', r.score)
  into v_today
  from public.atelier_daily_runs r where r.user_id = v_uid and r.language = v_lang and r.day = v_day;

  return jsonb_build_object('scope', p_scope, 'day', v_day, 'rows', v_rows, 'me', v_me, 'total', v_total,
    'today', coalesce(v_today, jsonb_build_object('started', false, 'finished', false, 'score', 0)));
end;
$$;

-- Last week's top 10 claim their Son Coin once.
create or replace function public.claim_atelier_weekly_reward_v1(p_language text)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_week date := (date_trunc('week', public.atelier_today_v1()) - interval '7 days')::date;
  v_rank integer;
  v_reward integer;
  v_balance integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if exists (select 1 from public.atelier_weekly_claims where user_id = v_uid and language = v_lang and week_start = v_week) then
    return jsonb_build_object('claimed', false, 'reason', 'already_claimed');
  end if;
  select x.rank into v_rank from (
    select r.user_id, rank() over (order by sum(r.score) desc)::int as rank
    from public.atelier_daily_runs r
    where r.language = v_lang and r.finished_at is not null and r.day between v_week and v_week + 6
    group by r.user_id
  ) x where x.user_id = v_uid;
  v_reward := case when v_rank = 1 then 200 when v_rank = 2 then 120 when v_rank = 3 then 80
                   when v_rank between 4 and 10 then 30 else 0 end;
  if v_reward = 0 then return jsonb_build_object('claimed', false, 'reason', 'not_ranked', 'rank', v_rank); end if;
  insert into public.atelier_weekly_claims(user_id, language, week_start, rank, reward) values (v_uid, v_lang, v_week, v_rank, v_reward);
  update public.profiles set diamonds = coalesce(diamonds, 0) + v_reward, updated_at = now() where id = v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id, delta, reason) values (v_uid, v_reward, 'atelier_weekly:' || v_lang || ':' || v_week);
  return jsonb_build_object('claimed', true, 'rank', v_rank, 'reward', v_reward, 'balance', v_balance);
end;
$$;

-- Last week's finishing rank for the caller (shown as a claim banner), without claiming.
create or replace function public.get_atelier_weekly_reward_status_v1(p_language text)
returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_week date := (date_trunc('week', public.atelier_today_v1()) - interval '7 days')::date;
  v_rank integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select x.rank into v_rank from (
    select r.user_id, rank() over (order by sum(r.score) desc)::int as rank
    from public.atelier_daily_runs r
    where r.language = v_lang and r.finished_at is not null and r.day between v_week and v_week + 6
    group by r.user_id
  ) x where x.user_id = v_uid;
  return jsonb_build_object('rank', v_rank,
    'reward', case when v_rank = 1 then 200 when v_rank = 2 then 120 when v_rank = 3 then 80 when v_rank between 4 and 10 then 30 else 0 end,
    'claimed', exists (select 1 from public.atelier_weekly_claims where user_id = v_uid and language = v_lang and week_start = v_week));
end;
$$;

revoke all on function public.start_atelier_daily_v1(text) from public, anon;
revoke all on function public.finish_atelier_daily_v1(text, integer, integer, integer) from public, anon;
revoke all on function public.get_atelier_board_v1(text, text) from public, anon;
revoke all on function public.claim_atelier_weekly_reward_v1(text) from public, anon;
revoke all on function public.get_atelier_weekly_reward_status_v1(text) from public, anon;
grant execute on function public.start_atelier_daily_v1(text) to authenticated;
grant execute on function public.finish_atelier_daily_v1(text, integer, integer, integer) to authenticated;
grant execute on function public.get_atelier_board_v1(text, text) to authenticated;
grant execute on function public.claim_atelier_weekly_reward_v1(text) to authenticated;
grant execute on function public.get_atelier_weekly_reward_status_v1(text) to authenticated;
