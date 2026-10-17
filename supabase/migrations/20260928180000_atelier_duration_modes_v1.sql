-- Kelime Atölyesi 1- and 2-minute modes.
--  * The round no longer ends when a task set is done: tasks come in sets of three (2 sets in
--    1 minute, 5 in 2 minutes) and the round runs to the clock; the highest score wins.
--  * The daily race is played once per day in each duration, with its own board per duration.
--  * v1 functions keep working for older builds as the 1-minute race.

alter table public.atelier_daily_runs add column if not exists duration_seconds integer not null default 60;

do $$
declare
  c record;
begin
  -- Old bounds were for a 3-task round; drop them and set bounds for up to 15 tasks.
  for c in
    select conname from pg_constraint
    where conrelid = 'public.atelier_daily_runs'::regclass and contype = 'c'
      and (pg_get_constraintdef(oid) ilike '%score%' or pg_get_constraintdef(oid) ilike '%words%' or pg_get_constraintdef(oid) ilike '%tasks%')
  loop
    execute format('alter table public.atelier_daily_runs drop constraint %I', c.conname);
  end loop;
end
$$;

alter table public.atelier_daily_runs
  add constraint atelier_daily_runs_duration_chk check (duration_seconds in (60, 120)),
  add constraint atelier_daily_runs_score_chk check (score between 0 and 12000),
  add constraint atelier_daily_runs_words_chk check (words between 0 and 200),
  add constraint atelier_daily_runs_tasks_chk check (tasks between 0 and 15);

alter table public.atelier_daily_runs drop constraint if exists atelier_daily_runs_pkey;
alter table public.atelier_daily_runs add primary key (user_id, language, day, duration_seconds);

drop index if exists public.atelier_daily_runs_board_idx;
create index if not exists atelier_daily_runs_board_v2_idx
  on public.atelier_daily_runs(language, day, duration_seconds, score desc) where finished_at is not null;

create or replace function public.atelier_duration_v1(p_duration integer)
returns integer language sql immutable set search_path = '' as $$
  select case when p_duration >= 120 then 120 else 60 end
$$;

-- Start today's official run for one duration (or report the one already played).
create or replace function public.start_atelier_daily_v2(p_language text, p_duration integer)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_secs integer := public.atelier_duration_v1(p_duration);
  v_day date := public.atelier_today_v1();
  v_run public.atelier_daily_runs%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_run from public.atelier_daily_runs
  where user_id = v_uid and language = v_lang and day = v_day and duration_seconds = v_secs;
  if found then
    return jsonb_build_object('started', false, 'already_played', true, 'day', v_day,
      'finished', v_run.finished_at is not null, 'score', v_run.score);
  end if;
  insert into public.atelier_daily_runs(user_id, language, day, duration_seconds) values (v_uid, v_lang, v_day, v_secs);
  return jsonb_build_object('started', true, 'already_played', false, 'day', v_day);
end;
$$;

-- Finish today's official run for one duration with the final score.
create or replace function public.finish_atelier_daily_v2(p_language text, p_duration integer, p_score integer, p_words integer, p_tasks integer)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_secs integer := public.atelier_duration_v1(p_duration);
  v_day date := public.atelier_today_v1();
  v_max_score integer := case when v_secs = 120 then 12000 else 6000 end;
  v_max_words integer := case when v_secs = 120 then 200 else 100 end;
  v_max_tasks integer := case when v_secs = 120 then 15 else 6 end;
  v_rank integer;
  v_total integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_score is null or p_score < 0 or p_score > v_max_score or p_words < 0 or p_words > v_max_words
     or p_tasks < 0 or p_tasks > v_max_tasks then
    raise exception 'implausible_score';
  end if;
  update public.atelier_daily_runs
  set score = p_score, words = p_words, tasks = p_tasks, finished_at = now()
  where user_id = v_uid and language = v_lang and duration_seconds = v_secs
    and day in (v_day, v_day - 1)            -- a run started just before midnight still counts
    and finished_at is null
    and started_at > now() - interval '15 minutes';
  if not found then raise exception 'no_open_run'; end if;

  select count(*) + 1 into v_rank from public.atelier_daily_runs
  where language = v_lang and day = v_day and duration_seconds = v_secs and finished_at is not null and score > p_score;
  select count(*) into v_total from public.atelier_daily_runs
  where language = v_lang and day = v_day and duration_seconds = v_secs and finished_at is not null;
  return jsonb_build_object('rank', v_rank, 'total', v_total, 'score', p_score);
end;
$$;

-- Leaderboard for one duration: p_scope 'daily' (today) or 'weekly' (this week's total).
create or replace function public.get_atelier_board_v2(p_language text, p_scope text, p_duration integer)
returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when p_language = 'en' then 'en' else 'tr' end;
  v_secs integer := public.atelier_duration_v1(p_duration);
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
    where r.language = v_lang and r.finished_at is not null and r.duration_seconds = v_secs
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
  from public.atelier_daily_runs r
  where r.user_id = v_uid and r.language = v_lang and r.day = v_day and r.duration_seconds = v_secs;

  return jsonb_build_object('scope', p_scope, 'day', v_day, 'rows', v_rows, 'me', v_me, 'total', v_total,
    'today', coalesce(v_today, jsonb_build_object('started', false, 'finished', false, 'score', 0)));
end;
$$;

-- Older builds: the v1 calls are the 1-minute race.
create or replace function public.start_atelier_daily_v1(p_language text)
returns jsonb language sql security definer set search_path = '' as $$
  select public.start_atelier_daily_v2(p_language, 60)
$$;

create or replace function public.finish_atelier_daily_v1(p_language text, p_score integer, p_words integer, p_tasks integer)
returns jsonb language sql security definer set search_path = '' as $$
  select public.finish_atelier_daily_v2(p_language, 60, p_score, p_words, p_tasks)
$$;

create or replace function public.get_atelier_board_v1(p_language text, p_scope text default 'daily')
returns jsonb language sql stable security definer set search_path = '' as $$
  select public.get_atelier_board_v2(p_language, p_scope, 60)
$$;

revoke all on function public.atelier_duration_v1(integer) from public, anon;
revoke all on function public.start_atelier_daily_v2(text, integer) from public, anon;
revoke all on function public.finish_atelier_daily_v2(text, integer, integer, integer, integer) from public, anon;
revoke all on function public.get_atelier_board_v2(text, text, integer) from public, anon;
grant execute on function public.atelier_duration_v1(integer) to authenticated;
grant execute on function public.start_atelier_daily_v2(text, integer) to authenticated;
grant execute on function public.finish_atelier_daily_v2(text, integer, integer, integer, integer) to authenticated;
grant execute on function public.get_atelier_board_v2(text, text, integer) to authenticated;
