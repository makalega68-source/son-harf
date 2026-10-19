-- Additive Son Harf features. Existing matches, balances and entitlements are unchanged.
create table public.last_letter_daily_runs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  day date not null,
  language text not null check (language in ('tr','en')),
  start_word text not null,
  words text[] not null default '{}' check (cardinality(words) <= 100),
  started_at timestamptz not null,
  ends_at timestamptz not null,
  finished boolean not null default false,
  unique(user_id, day, language)
);
alter table public.last_letter_daily_runs enable row level security;
create policy "daily runs own read" on public.last_letter_daily_runs for select to authenticated
  using (user_id = (select auth.uid()));
revoke all on public.last_letter_daily_runs from public, anon, authenticated;
grant select on public.last_letter_daily_runs to authenticated;

create table public.last_letter_hint_usage (
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  used smallint not null check (used between 0 and 3),
  primary key(room_id,user_id)
);
create index last_letter_hint_usage_user_idx on public.last_letter_hint_usage(user_id);
alter table public.last_letter_hint_usage enable row level security;
create policy "fair hints own read" on public.last_letter_hint_usage for select to authenticated
  using (user_id = (select auth.uid()));
revoke all on public.last_letter_hint_usage from public, anon, authenticated;
grant select on public.last_letter_hint_usage to authenticated;

create function private.last_letter_daily_v1(
  p_action text, p_language text, p_run_id uuid, p_word text
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_me uuid := auth.uid();
  v_day date := (clock_timestamp() at time zone 'Europe/Istanbul')::date;
  v_now timestamptz;
  v_run public.last_letter_daily_runs%rowtype;
  v_check record;
  v_pool text[];
  v_start text;
begin
  if v_me is null then raise exception 'not_authenticated'; end if;
  if p_language not in ('tr','en') or p_language is null then raise exception 'invalid_language'; end if;
  if p_action not in ('status','start','word','finish') or p_action is null then raise exception 'invalid_action'; end if;
  v_pool := case p_language when 'tr' then
    array['kalem','masa','kitap','deniz','bahar','yol','orman','umut','güneş','ırmak','çiçek','rüya']
    else array['apple','river','stone','earth','light','dream','cloud','leaf','forest','moon','water','star'] end;
  v_start := v_pool[1 + mod(v_day - date '2026-01-01', cardinality(v_pool))];
  if p_action = 'start' then
    v_now := clock_timestamp();
    insert into public.last_letter_daily_runs(user_id,day,language,start_word,started_at,ends_at)
      values(v_me,v_day,p_language,v_start,v_now,v_now + interval '60 seconds')
      on conflict(user_id,day,language) do nothing;
  end if;
  if p_action in ('status','start') then
    select * into v_run from public.last_letter_daily_runs
      where user_id=v_me and day=v_day and language=p_language;
    if not found then
      return jsonb_build_object('day',v_day,'language',p_language,'start_word',v_start,
        'words','[]'::jsonb,'finished',false,'server_now',clock_timestamp());
    end if;
  else
    select * into v_run from public.last_letter_daily_runs
      where id=p_run_id and user_id=v_me and language=p_language for update;
    if not found then raise exception 'daily_run_not_found'; end if;
    v_now := clock_timestamp();
    if not v_run.finished and v_run.ends_at > v_now then
      if p_action = 'finish' then
        update public.last_letter_daily_runs set finished=true where id=v_run.id returning * into v_run;
      else
        select * into v_check from public.validate_game_word_v2(coalesce(p_word,''),p_language);
        if not v_check.valid then raise exception '%',v_check.reason; end if;
        if left(v_check.normalized_word,1) <> right(coalesce(v_run.words[cardinality(v_run.words)],v_run.start_word),1) then
          raise exception 'wrong_start_letter';
        end if;
        if v_check.normalized_word = v_run.start_word or v_check.normalized_word = any(v_run.words) then
          raise exception 'word_already_used';
        end if;
        if cardinality(v_run.words) >= 100 then raise exception 'daily_word_limit'; end if;
        update public.last_letter_daily_runs set words=array_append(words,v_check.normalized_word)
          where id=v_run.id returning * into v_run;
      end if;
    end if;
  end if;
  return jsonb_build_object('id',v_run.id,'day',v_run.day,'language',v_run.language,
    'start_word',v_run.start_word,'words',to_jsonb(v_run.words),
    'started_at',v_run.started_at,'ends_at',v_run.ends_at,
    'finished',v_run.finished or v_run.ends_at <= clock_timestamp(),'server_now',clock_timestamp());
end;
$$;
revoke all on function private.last_letter_daily_v1(text,text,uuid,text) from public,anon;
grant execute on function private.last_letter_daily_v1(text,text,uuid,text) to authenticated;
create function public.last_letter_daily_v1(
  p_action text default 'status', p_language text default 'tr', p_run_id uuid default null, p_word text default null
) returns jsonb language sql security invoker set search_path = '' as $$
  select private.last_letter_daily_v1(p_action,p_language,p_run_id,p_word);
$$;
revoke all on function public.last_letter_daily_v1(text,text,uuid,text) from public,anon;
grant execute on function public.last_letter_daily_v1(text,text,uuid,text) to authenticated;

create function private.claim_last_letter_fair_hint_v1(p_room_id uuid,p_min_used integer)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_me uuid := auth.uid();
  v_room public.game_rooms%rowtype;
  v_used integer;
  v_min integer := greatest(0,least(3,coalesce(p_min_used,0)));
begin
  if v_me is null then raise exception 'not_authenticated'; end if;
  select * into v_room from public.game_rooms where id=p_room_id for update;
  if not found or (v_me <> v_room.host_id and v_me is distinct from v_room.guest_id) then
    raise exception 'not_room_member';
  end if;
  if v_room.is_bot then raise exception 'bot_hint_uses_existing_entitlement'; end if;
  if v_room.status not in ('playing','final','sudden_death') or v_room.current_player_id is distinct from v_me
    or v_room.turn_deadline is null or v_room.turn_deadline <= clock_timestamp() then raise exception 'not_your_turn'; end if;
  if v_min >= 3 then raise exception 'hint_limit_reached'; end if;
  insert into public.last_letter_hint_usage(room_id,user_id,used) values(p_room_id,v_me,v_min+1)
    on conflict(room_id,user_id) do update set used=greatest(public.last_letter_hint_usage.used,v_min)+1
      where greatest(public.last_letter_hint_usage.used,v_min)<3
    returning used into v_used;
  if v_used is null then raise exception 'hint_limit_reached'; end if;
  return jsonb_build_object('used',v_used,'left',3-v_used);
end;
$$;
revoke all on function private.claim_last_letter_fair_hint_v1(uuid,integer) from public,anon;
grant execute on function private.claim_last_letter_fair_hint_v1(uuid,integer) to authenticated;
create function public.claim_last_letter_fair_hint_v1(p_room_id uuid,p_min_used integer default 0)
returns jsonb language sql security invoker set search_path = '' as $$
  select private.claim_last_letter_fair_hint_v1(p_room_id,p_min_used);
$$;
revoke all on function public.claim_last_letter_fair_hint_v1(uuid,integer) from public,anon;
grant execute on function public.claim_last_letter_fair_hint_v1(uuid,integer) to authenticated;
