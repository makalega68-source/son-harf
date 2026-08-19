-- Son Harf production autopilot / resilience layer
-- Safe to run after the 20260819 game, social, friends and matchmaking migrations.

create table if not exists public.app_config (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz not null default now()
);

insert into public.app_config(key,value) values
 ('maintenance_mode','false'::jsonb),
 ('matchmaking_enabled','true'::jsonb),
 ('chat_enabled','true'::jsonb),
 ('trivia_enabled','true'::jsonb),
 ('turn_seconds','45'::jsonb),
 ('valid_word_points','3'::jsonb),
 ('invalid_word_points','-1'::jsonb),
 ('streak_every','5'::jsonb),
 ('streak_bonus','3'::jsonb)
on conflict (key) do nothing;

create table if not exists public.system_events (
  id bigint generated always as identity primary key,
  severity text not null check (severity in ('info','warning','error','critical')),
  source text not null,
  event_type text not null,
  room_id uuid null references public.game_rooms(id) on delete set null,
  user_id uuid null references auth.users(id) on delete set null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists system_events_created_idx on public.system_events(created_at desc);
create index if not exists system_events_severity_idx on public.system_events(severity,created_at desc);

create table if not exists public.admin_audit_log (
  id bigint generated always as identity primary key,
  admin_id uuid null references auth.users(id) on delete set null,
  action text not null,
  target_type text,
  target_id text,
  before_data jsonb,
  after_data jsonb,
  created_at timestamptz not null default now()
);

-- A trivia question may appear only once in the same match.
create unique index if not exists trivia_rounds_room_question_unique
  on public.trivia_rounds(room_id, question_id);

-- Keep operational queries cheap as the game grows.
create index if not exists game_rooms_status_deadline_idx on public.game_rooms(status, turn_deadline);
create index if not exists game_words_room_created_idx on public.game_words(room_id, created_at);
create index if not exists chat_messages_room_created_idx on public.chat_messages(room_id, created_at);
create index if not exists trivia_questions_language_active_idx on public.trivia_questions(language, active);
create index if not exists dictionary_words_language_word_idx on public.dictionary_words(language, normalized_word);

-- Profile photo metadata. Binary image data belongs in Storage, never Postgres.
alter table public.profiles add column if not exists avatar_path text;
alter table public.profiles add column if not exists avatar_thumb_path text;
alter table public.profiles add column if not exists avatar_updated_at timestamptz;

-- User-facing reports are de-duplicated by reporter + reported user.
-- Re-reporting updates context/reason but cannot inflate the distinct reporter count.
create unique index if not exists player_reports_unique_reporter_target
  on public.player_reports(reporter_id, reported_id);

-- Generic maintenance function. Intended for scheduled execution (e.g. pg_cron when enabled)
-- and safe manual execution from the operations panel.
create or replace function public.run_game_maintenance()
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_stale_queue int := 0;
  v_expired_invites int := 0;
  v_old_waiting int := 0;
begin
  -- Remove abandoned matchmaking entries.
  delete from public.matchmaking_queue
   where status = 'waiting' and created_at < now() - interval '5 minutes';
  get diagnostics v_stale_queue = row_count;

  -- Expire old game invitations.
  update public.game_invites
     set status = 'expired'
   where status = 'pending' and expires_at < now();
  get diagnostics v_expired_invites = row_count;

  -- Close friend/private rooms that were never joined.
  update public.game_rooms
     set status = 'finished', last_event = 'abandoned_waiting_room'
   where status = 'waiting' and created_at < now() - interval '30 minutes';
  get diagnostics v_old_waiting = row_count;

  insert into public.system_events(severity,source,event_type,details)
  values ('info','maintenance','maintenance_run',jsonb_build_object(
    'stale_queue_removed',v_stale_queue,
    'expired_invites',v_expired_invites,
    'abandoned_rooms_closed',v_old_waiting
  ));

  return jsonb_build_object(
    'ok',true,
    'stale_queue_removed',v_stale_queue,
    'expired_invites',v_expired_invites,
    'abandoned_rooms_closed',v_old_waiting,
    'at',now()
  );
end $$;

-- Lightweight health snapshot for an admin/operations dashboard.
create or replace function public.system_health_snapshot()
returns jsonb
language sql
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'at', now(),
    'active_matches', (select count(*) from public.game_rooms where status in ('playing','quiz','final','sudden_death')),
    'waiting_matches', (select count(*) from public.game_rooms where status='waiting'),
    'matchmaking_waiting', (select count(*) from public.matchmaking_queue where status='waiting'),
    'online_players', (select count(*) from public.profiles where presence_status='online' and last_seen_at > now()-interval '3 minutes'),
    'players_in_game', (select count(*) from public.profiles where presence_status='in_game' and last_seen_at > now()-interval '3 minutes'),
    'recent_errors', (select count(*) from public.system_events where severity in ('error','critical') and created_at > now()-interval '1 hour'),
    'tr_questions_tr', (select count(*) from public.trivia_questions where language='tr' and active=true),
    'tr_questions_en', (select count(*) from public.trivia_questions where language='en' and active=true),
    'dictionary_tr', (select count(*) from public.dictionary_words where language='tr'),
    'dictionary_en', (select count(*) from public.dictionary_words where language='en')
  );
$$;

-- Heartbeat prevents users who force-close the app from appearing online forever.
create or replace function public.touch_presence(p_status text default 'online')
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_status not in ('online','offline','in_game') then raise exception 'invalid_presence'; end if;
  update public.profiles set presence_status=p_status,last_seen_at=now() where id=auth.uid();
end $$;

grant execute on function public.run_game_maintenance() to authenticated;
grant execute on function public.touch_presence(text) to authenticated;

-- Do not expose system/audit tables to normal clients.
alter table public.system_events enable row level security;
alter table public.admin_audit_log enable row level security;
alter table public.app_config enable row level security;

-- Public clients may read feature flags but may never modify them.
drop policy if exists app_config_read on public.app_config;
create policy app_config_read on public.app_config for select to authenticated using (true);

-- Optional pg_cron scheduling. It is deliberately guarded so this migration also works
-- on projects where pg_cron is not enabled. Supabase Scheduled Jobs can call the same RPC.
do $$
begin
  if exists(select 1 from pg_extension where extname='pg_cron') then
    if not exists(select 1 from cron.job where jobname='son_harf_game_maintenance') then
      perform cron.schedule('son_harf_game_maintenance','*/5 * * * *','select public.run_game_maintenance();');
    end if;
  end if;
exception when others then
  insert into public.system_events(severity,source,event_type,details)
  values ('warning','migration','cron_not_scheduled',jsonb_build_object('message',sqlerrm));
end $$;
