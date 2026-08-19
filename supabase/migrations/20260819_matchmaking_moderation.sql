-- Son Harf: random matchmaking, unique reports, automated chat suspension

create table if not exists public.matchmaking_queue (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  language text not null check (language in ('tr','en')),
  status text not null default 'waiting' check (status in ('waiting','matched','cancelled')),
  room_id uuid references public.game_rooms(id) on delete set null,
  queued_at timestamptz not null default now(),
  heartbeat_at timestamptz not null default now()
);

alter table public.matchmaking_queue enable row level security;

drop policy if exists "matchmaking self read" on public.matchmaking_queue;
create policy "matchmaking self read" on public.matchmaking_queue
for select to authenticated using (user_id = auth.uid());

alter table public.profiles
  add column if not exists chat_suspended_until timestamptz,
  add column if not exists chat_strike_level integer not null default 0;

alter table public.player_reports
  add column if not exists message_id bigint references public.chat_messages(id) on delete set null;

-- One reporter can count only once against the same target.
create unique index if not exists player_reports_unique_pair_idx
on public.player_reports(reporter_id, reported_id);

create or replace function public.set_presence(p_status text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if p_status not in ('offline','online','in_game') then raise exception 'invalid_presence'; end if;
  update public.profiles
  set presence_status = p_status, last_seen_at = now(), updated_at = now()
  where id = auth.uid();
end;
$$;
grant execute on function public.set_presence(text) to authenticated;

create or replace function public.join_random_matchmaking(p_language text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  opponent uuid;
  r public.game_rooms;
  generated_code text;
  attempts int := 0;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;

  -- Do not queue while already in an active match.
  if exists (
    select 1 from public.game_rooms
    where status in ('playing','quiz','final','sudden_death')
      and (host_id = auth.uid() or guest_id = auth.uid())
  ) then raise exception 'player_already_in_game'; end if;

  -- Remove stale queue entries older than 90 seconds.
  update public.matchmaking_queue
     set status = 'cancelled'
   where status = 'waiting' and heartbeat_at < now() - interval '90 seconds';

  -- Lock one compatible waiting opponent. Block relationships are excluded.
  select q.user_id into opponent
  from public.matchmaking_queue q
  where q.status = 'waiting'
    and q.language = p_language
    and q.user_id <> auth.uid()
    and q.heartbeat_at >= now() - interval '90 seconds'
    and not exists (
      select 1 from public.user_blocks b
      where (b.blocker_id = auth.uid() and b.blocked_id = q.user_id)
         or (b.blocker_id = q.user_id and b.blocked_id = auth.uid())
    )
  order by q.queued_at
  for update skip locked
  limit 1;

  if opponent is null then
    insert into public.matchmaking_queue(user_id, language, status, queued_at, heartbeat_at)
    values (auth.uid(), p_language, 'waiting', now(), now())
    on conflict (user_id) do update
      set language = excluded.language, status = 'waiting', room_id = null,
          queued_at = case when public.matchmaking_queue.status = 'waiting' and public.matchmaking_queue.language = excluded.language then public.matchmaking_queue.queued_at else now() end,
          heartbeat_at = now();
    update public.profiles set presence_status='online', last_seen_at=now() where id=auth.uid();
    return null;
  end if;

  loop
    attempts := attempts + 1;
    generated_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
    begin
      insert into public.game_rooms(code, host_id, guest_id, status, current_player_id, turn_deadline, language)
      values (generated_code, opponent, auth.uid(), 'playing', opponent, now() + interval '45 seconds', p_language)
      returning * into r;
      exit;
    exception when unique_violation then
      if attempts >= 8 then raise; end if;
    end;
  end loop;

  update public.matchmaking_queue set status='matched', room_id=r.id, heartbeat_at=now()
   where user_id in (opponent, auth.uid());
  insert into public.matchmaking_queue(user_id, language, status, room_id, queued_at, heartbeat_at)
  values (auth.uid(), p_language, 'matched', r.id, now(), now())
  on conflict (user_id) do update set status='matched', room_id=r.id, heartbeat_at=now();

  update public.profiles set presence_status='in_game', last_seen_at=now()
  where id in (opponent, auth.uid());

  return r;
end;
$$;
grant execute on function public.join_random_matchmaking(text) to authenticated;

create or replace function public.poll_random_matchmaking()
returns public.game_rooms
language plpgsql security definer set search_path = public as $$
declare
  q public.matchmaking_queue;
  r public.game_rooms;
begin
  select * into q from public.matchmaking_queue where user_id=auth.uid() for update;
  if q.user_id is null then return null; end if;
  if q.status='matched' and q.room_id is not null then
    select * into r from public.game_rooms where id=q.room_id;
    return r;
  end if;
  if q.status='waiting' then
    update public.matchmaking_queue set heartbeat_at=now() where user_id=auth.uid();
  end if;
  return null;
end;
$$;
grant execute on function public.poll_random_matchmaking() to authenticated;

create or replace function public.cancel_random_matchmaking()
returns void
language sql security definer set search_path = public as $$
  update public.matchmaking_queue set status='cancelled', heartbeat_at=now()
  where user_id=auth.uid() and status='waiting';
$$;
grant execute on function public.cancel_random_matchmaking() to authenticated;

create or replace function public.report_player(p_reported_id uuid, p_reason text, p_room_id uuid default null, p_message_id bigint default null)
returns integer
language plpgsql security definer set search_path = public as $$
declare
  report_count int;
  new_level int;
  suspend_for interval;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_reported_id = auth.uid() then raise exception 'cannot_report_self'; end if;
  if char_length(trim(p_reason)) < 3 then raise exception 'invalid_reason'; end if;

  insert into public.player_reports(reporter_id, reported_id, room_id, message_id, reason)
  values (auth.uid(), p_reported_id, p_room_id, p_message_id, trim(p_reason))
  on conflict (reporter_id, reported_id) do update
    set reason = excluded.reason,
        room_id = coalesce(excluded.room_id, public.player_reports.room_id),
        message_id = coalesce(excluded.message_id, public.player_reports.message_id);

  select count(distinct reporter_id) into report_count
  from public.player_reports where reported_id=p_reported_id;

  if report_count >= 3 then
    select chat_strike_level + 1 into new_level from public.profiles where id=p_reported_id for update;
    suspend_for := case
      when new_level <= 1 then interval '24 hours'
      when new_level = 2 then interval '3 days'
      else interval '7 days'
    end;
    update public.profiles
      set chat_strike_level=new_level,
          chat_suspended_until=greatest(coalesce(chat_suspended_until, now()), now()) + suspend_for
    where id=p_reported_id;
  end if;

  return report_count;
end;
$$;
grant execute on function public.report_player(uuid,text,uuid,bigint) to authenticated;

-- Chat is disabled for suspended users and blocked relationships.
drop policy if exists "chat participant insert" on public.chat_messages;
create policy "chat participant insert" on public.chat_messages
for insert to authenticated
with check (
  sender_id = auth.uid()
  and public.is_room_participant(room_id, auth.uid())
  and coalesce((select chat_suspended_until <= now() from public.profiles where id=auth.uid()), true)
  and not exists (
    select 1 from public.game_rooms r
    join public.user_blocks b on (
      (b.blocker_id = auth.uid() and b.blocked_id = case when r.host_id = auth.uid() then r.guest_id else r.host_id end)
      or
      (b.blocked_id = auth.uid() and b.blocker_id = case when r.host_id = auth.uid() then r.guest_id else r.host_id end)
    )
    where r.id = room_id
  )
);

create index if not exists matchmaking_waiting_idx on public.matchmaking_queue(language,status,queued_at);
