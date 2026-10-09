-- Server-owned Taht Saati: 19:00 inclusive to 22:00 exclusive in Europe/Istanbul.
-- Existing XP and balances are untouched; Workshop x1.5 is replaced by x2, never stacked.

CREATE OR REPLACE FUNCTION public.finish_atelier_tournament_v1(p_event timestamp with time zone, p_stage integer, p_score integer, p_words integer, p_tasks integer, p_transcript text[])
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  u uuid := auth.uid();
  r public.atelier_tournament_runs%rowtype;
  secs integer;
  max_tasks integer;
  m integer;
  x integer;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select * into r from public.atelier_tournament_runs
  where user_id = u and event_start = p_event and stage = p_stage for update;
  if not found then raise exception 'no_open_run'; end if;
  if r.finished_at is not null then
    return jsonb_build_object('xp', r.xp, 'score', r.score, 'already_saved', true);
  end if;
  secs := case p_stage when 1 then 60 when 2 then 120 else 180 end;
  max_tasks := case p_stage when 1 then 6 when 2 then 15 else 24 end;
  if p_score is null or p_words is null or p_tasks is null
    or p_score < 0 or p_score > secs / 60 * 6000
    or p_words < 0 or p_words > secs / 60 * 100
    or p_tasks < 0 or p_tasks > max_tasks
    or p_tasks > p_words * 3 or p_score > p_words * 130 + p_tasks * 50
  then raise exception 'implausible_score'; end if;
  if p_transcript is null or cardinality(p_transcript) <> p_words
    or (select count(distinct word) from unnest(p_transcript) word) <> p_words
    or exists (
      select 1 from unnest(p_transcript) word
      where word is null or length(word) not between 3 and 7
        or not private.word_siege_word_allowed_v1(word, r.language)
    )
    or p_score > coalesce((select sum(length(word) * 10) from unnest(p_transcript) word), 0)
      + p_tasks * 50 + p_words * 60
  then raise exception 'invalid_transcript'; end if;
  if now() < r.started_at + make_interval(secs => secs - 2) then raise exception 'round_not_finished'; end if;
  if now() > p_event + make_interval(secs => p_stage * 600 + 15) then raise exception 'stage_expired'; end if;
  m := case when private.throne_hour_active(p_event) then 200 else 150 end;
  x := least(120, 35 + p_score / (secs / 60 * 20)) * m / 100;
  update public.atelier_tournament_runs
  set finished_at = now(), score = p_score, words = p_words, tasks = p_tasks, xp = x
  where user_id = u and event_start = p_event and stage = p_stage;
  perform private.throne_award_xp(u, 'atelier_tournament', p_event::text || ':' || p_stage, 'atelier', x, 1, 0, p_tasks);
  return jsonb_build_object('xp', x, 'score', p_score, 'already_saved', false);
end
$function$;

CREATE OR REPLACE FUNCTION public.get_atelier_tournament_v1()
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  u uuid := auth.uid();
  e timestamptz := private.atelier_event_start(now());
  s integer := floor(extract(epoch from (now() - e)) / 600)::integer + 1;
  rows jsonb;
  winners jsonb;
  previous timestamptz;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select jsonb_agg(to_jsonb(t)) into rows from (
    select row_number() over(order by count(*) desc, sum(r.score) desc, max(r.finished_at), user_id)::int rank,
      user_id, coalesce(max(p.display_name), 'Oyuncu') name, sum(r.score)::int score, count(*)::int stages
    from public.atelier_tournament_runs r join public.profiles p on p.id = r.user_id
    where event_start = e and r.finished_at is not null
    group by user_id order by count(*) desc, sum(r.score) desc, max(r.finished_at), user_id limit 50
  ) t;
  select max(event_start) into previous from public.atelier_tournament_runs
  where event_start + interval '30 minutes 15 seconds' <= now() and finished_at is not null;
  select jsonb_agg(to_jsonb(t)) into winners from (
    select row_number() over(order by count(*) desc, sum(r.score) desc, max(r.finished_at), r.user_id)::int rank,
      coalesce(p.display_name, 'Oyuncu') name, sum(r.score)::int score, count(*)::int stages
    from public.atelier_tournament_runs r join public.profiles p on p.id = r.user_id
    where r.event_start = previous and r.finished_at is not null
    group by r.user_id, p.display_name
    order by count(*) desc, sum(r.score) desc, max(r.finished_at), r.user_id limit 3
  ) t;
  return jsonb_build_object(
    'server_time', now(), 'event_start', e, 'next_start', private.atelier_event_next(now()), 'active', s <= 3,
    'stage', least(s, 3), 'stage_ends', e + make_interval(secs => least(s, 3) * 600),
    'multiplier', case when private.throne_hour_active(e) then 2.0 else 1.5 end,
    'rows', coalesce(rows, '[]'::jsonb), 'winners', coalesce(winners, '[]'::jsonb),
    'my_stages', coalesce((
      select jsonb_agg(jsonb_build_object('stage', stage, 'finished', finished_at is not null, 'xp', xp))
      from public.atelier_tournament_runs where event_start = e and user_id = u
    ), '[]'::jsonb)
  );
end
$function$;

CREATE OR REPLACE FUNCTION private.throne_atelier_result()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  xp_multiplier integer := case when private.throne_hour_active(now()) then 2 else 1 end;
begin
  if new.finished_at is not null and old.finished_at is null then
    perform private.throne_award_xp(
      new.user_id,
      'daily_atelier',
      new.day || ':' || new.language || ':' || new.duration_seconds,
      'atelier',
      least(120, 35 + new.score / greatest(20, new.duration_seconds / 60 * 20)) * xp_multiplier,
      1, 0, new.tasks
    );
  end if;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION private.throne_game_result()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  a uuid;
  b uuid;
  winner uuid;
  g text;
  qualified boolean;
  xp_multiplier integer := case when private.throne_hour_active(now()) then 2 else 1 end;
begin
  if tg_table_name = 'game_rooms' then
    qualified := new.status = 'finished' and new.stats_applied and new.valid_word_count >= 2;
    a := new.host_id;
    b := new.guest_id;
    winner := new.winner_id;
    g := 'last_letter';
  else
    qualified := new.status = 'finished' and new.result_applied and new.move_count >= 2
      and new.player_one_word_score + new.player_two_word_score > 0;
    a := new.player_one_id;
    b := new.player_two_id;
    winner := new.winner_id;
    g := 'siege';
  end if;

  if qualified then
    perform private.throne_award_xp(a, 'match', new.id::text, g,
      (35 + case when a = winner then 85 else 0 end) * xp_multiplier,
      1, case when a = winner then 1 else 0 end);
    perform private.throne_award_xp(b, 'match', new.id::text, g,
      (35 + case when b = winner then 85 else 0 end) * xp_multiplier,
      1, case when b = winner then 1 else 0 end);
  end if;
  return new;
end
$function$;

CREATE OR REPLACE FUNCTION private.throne_hour_active(p_at timestamp with time zone)
 RETURNS boolean
 LANGUAGE sql
 STABLE
 SET search_path TO ''
AS $function$
  select (p_at at time zone 'Europe/Istanbul')::time >= time '19:00'
     and (p_at at time zone 'Europe/Istanbul')::time <  time '22:00'
$function$;

CREATE OR REPLACE FUNCTION public.start_atelier_tournament_v1(p_language text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  u uuid := auth.uid();
  e timestamptz := private.atelier_event_start(now());
  s integer := floor(extract(epoch from (now() - e)) / 600)::integer + 1;
  secs integer;
  lang text := case when p_language = 'en' then 'en' else 'tr' end;
  n integer;
begin
  if u is null then raise exception 'unauthorized'; end if;
  if s > 3 then raise exception 'tournament_closed'; end if;
  secs := case s when 1 then 60 when 2 then 120 else 180 end;
  if now() + make_interval(secs => secs + 10) > e + make_interval(secs => s * 600) then
    raise exception 'stage_entry_closed';
  end if;
  if s > 1 and not exists (
    select 1 from public.atelier_tournament_runs
    where user_id = u and event_start = e and stage = s - 1 and finished_at is not null and language = lang
  ) then
    raise exception 'previous_stage_required';
  end if;
  insert into public.atelier_tournament_runs(user_id, event_start, language, stage)
  values (u, e, lang, s) on conflict do nothing;
  get diagnostics n = row_count;
  if n = 0 then raise exception 'stage_already_started'; end if;
  return jsonb_build_object(
    'event_start', e, 'stage', s, 'seconds', secs, 'seed_key', e::text || ':' || s || ':' || lang,
    'multiplier', case when private.throne_hour_active(e) then 2.0 else 1.5 end
  );
end
$function$;

revoke all on function private.throne_hour_active(timestamptz), private.throne_game_result(), private.throne_atelier_result() from public, anon, authenticated;
revoke all on function public.start_atelier_tournament_v1(text), public.finish_atelier_tournament_v1(timestamptz, integer, integer, integer, integer, text[]), public.get_atelier_tournament_v1() from public, anon;
grant execute on function public.start_atelier_tournament_v1(text), public.finish_atelier_tournament_v1(timestamptz, integer, integer, integer, integer, text[]), public.get_atelier_tournament_v1() to authenticated;

