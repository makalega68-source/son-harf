create table if not exists public.daily_checkins (
  user_id uuid not null references public.profiles(id) on delete cascade,
  checkin_date date not null default current_date,
  reward_diamonds integer not null default 40,
  claimed_at timestamptz not null default now(),
  primary key (user_id, checkin_date)
);

create table if not exists public.daily_challenge_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  challenge_date date not null default current_date,
  reward_diamonds integer not null default 30,
  claimed_at timestamptz not null default now(),
  primary key (user_id, challenge_date)
);

create table if not exists public.app_events (
  id bigserial primary key,
  user_id uuid references public.profiles(id) on delete set null,
  event_name text not null check (length(event_name) between 1 and 64),
  event_value text,
  created_at timestamptz not null default now()
);

alter table public.daily_checkins enable row level security;
alter table public.daily_challenge_claims enable row level security;
alter table public.app_events enable row level security;

drop policy if exists daily_checkins_self on public.daily_checkins;
create policy daily_checkins_self on public.daily_checkins for select using (auth.uid() = user_id);
drop policy if exists daily_challenge_self on public.daily_challenge_claims;
create policy daily_challenge_self on public.daily_challenge_claims for select using (auth.uid() = user_id);
drop policy if exists app_events_self_insert on public.app_events;
create policy app_events_self_insert on public.app_events for insert with check (auth.uid() = user_id);
drop policy if exists app_events_self_select on public.app_events;
create policy app_events_self_select on public.app_events for select using (auth.uid() = user_id);

create or replace function public.log_app_event_v1(p_event_name text, p_event_value text default null)
returns void language plpgsql security definer set search_path=public as $$
begin
  if auth.uid() is null then return; end if;
  insert into public.app_events(user_id,event_name,event_value)
  values(auth.uid(), left(coalesce(nullif(trim(p_event_name),''),'unknown'),64), left(p_event_value,240));
end; $$;

grant execute on function public.log_app_event_v1(text,text) to authenticated;

create or replace function public.claim_daily_checkin_v1()
returns integer language plpgsql security definer set search_path=public as $$
declare v_uid uuid := auth.uid(); v_reward integer := 40;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  insert into public.daily_checkins(user_id,checkin_date,reward_diamonds)
  values(v_uid,current_date,v_reward)
  on conflict (user_id,checkin_date) do nothing;
  if not found then return 0; end if;
  update public.profiles set diamonds = coalesce(diamonds,0) + v_reward, updated_at=now() where id=v_uid;
  return v_reward;
end; $$;

grant execute on function public.claim_daily_checkin_v1() to authenticated;

create or replace function public.claim_daily_challenge_v1()
returns integer language plpgsql security definer set search_path=public as $$
declare v_uid uuid := auth.uid(); v_reward integer := 30; v_matches integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select count(*) into v_matches from public.game_rooms
   where status='finished' and coalesce(finished_at,created_at)::date=current_date and (host_id=v_uid or guest_id=v_uid);
  if v_matches < 3 then raise exception 'daily_challenge_incomplete'; end if;
  insert into public.daily_challenge_claims(user_id,challenge_date,reward_diamonds)
  values(v_uid,current_date,v_reward)
  on conflict (user_id,challenge_date) do nothing;
  if not found then return 0; end if;
  update public.profiles set diamonds = coalesce(diamonds,0) + v_reward, updated_at=now() where id=v_uid;
  return v_reward;
end; $$;

grant execute on function public.claim_daily_challenge_v1() to authenticated;

create or replace function public.get_growth_dashboard_v1()
returns table(
  display_name text, xp integer, level integer, level_progress integer, level_target integer,
  current_win_streak integer, best_streak integer, total_matches integer, wins integer, losses integer,
  valid_words integer, matches_today integer, daily_reward integer, daily_claimed boolean,
  daily_challenge_claimed boolean, league_name text, next_title text,
  achievements_unlocked integer, achievement_total integer
) language sql security definer set search_path=public as $$
with me as (select p.* from public.profiles p where p.id=auth.uid()),
ordered as (
  select row_number() over(order by coalesce(g.finished_at,g.created_at) desc) rn, (g.winner_id=auth.uid()) won
  from public.game_rooms g where g.status='finished' and (g.host_id=auth.uid() or g.guest_id=auth.uid())
), first_loss as (select min(rn) rn from ordered where not won),
streak as (select count(*)::int n from ordered where won and rn < coalesce((select rn from first_loss),2147483647)),
calc as (
  select m.*, ((coalesce(m.wins,0)*120)+(coalesce(m.losses,0)*35)+(coalesce(m.valid_words,0)*3)+(coalesce(m.total_rounds,0)*5))::int x from me m
)
select c.display_name, c.x, greatest(1,(c.x/500)+1)::int, (c.x%500)::int, 500::int,
       coalesce((select n from streak),0)::int, coalesce(c.best_streak,0)::int,
       coalesce(c.total_matches,c.wins+c.losses,0)::int, coalesce(c.wins,0)::int, coalesce(c.losses,0)::int,
       coalesce(c.valid_words,0)::int,
       (select count(*)::int from public.game_rooms g where g.status='finished' and coalesce(g.finished_at,g.created_at)::date=current_date and (g.host_id=auth.uid() or g.guest_id=auth.uid())),
       40::int,
       exists(select 1 from public.daily_checkins d where d.user_id=auth.uid() and d.checkin_date=current_date),
       exists(select 1 from public.daily_challenge_claims d where d.user_id=auth.uid() and d.challenge_date=current_date),
       case when c.x>=10000 then 'ELMAS' when c.x>=6000 then 'PLATİN' when c.x>=3000 then 'ALTIN' when c.x>=1200 then 'GÜMÜŞ' else 'BRONZ' end,
       case when coalesce(c.wins,0)>=100 then 'EFSANE' when coalesce(c.wins,0)>=50 then 'USTA' when coalesce(c.wins,0)>=20 then 'DÜELLOCU' when coalesce(c.wins,0)>=5 then 'YÜKSELEN' else 'ÇAYLAK' end,
       ((coalesce(c.total_matches,0)>=1)::int + (coalesce(c.wins,0)>=1)::int + (coalesce(c.wins,0)>=10)::int + (coalesce(c.wins,0)>=50)::int + (coalesce(c.valid_words,0)>=50)::int + (coalesce(c.valid_words,0)>=250)::int + (coalesce(c.best_streak,0)>=5)::int + (coalesce(c.word_storms,0)>=1)::int + (coalesce(c.rating,1000)>=1200)::int + (coalesce(c.total_matches,0)>=100)::int)::int,
       10::int from calc c;
$$;

grant execute on function public.get_growth_dashboard_v1() to authenticated;
create index if not exists app_events_user_created_idx on public.app_events(user_id,created_at desc);
create index if not exists game_rooms_finish_user_idx on public.game_rooms(finished_at desc,host_id,guest_id) where status='finished';
