
-- Son Harf unified competition / recovery fixes v1
-- 2026-08-29

-- 1) Functions that generate room codes use pgcrypto from the extensions schema.
alter function public.create_room_normal_v1(text)
  set search_path to public, extensions, pg_temp;
alter function public.join_random_matchmaking_v2(text,text)
  set search_path to public, extensions, pg_temp;
alter function public.request_rematch(uuid)
  set search_path to public, extensions, pg_temp;
alter function public.respond_game_invite(uuid,boolean)
  set search_path to public, extensions, pg_temp;

-- 2) A waiting Team Arena lobby must not trap the user outside every other game.
-- A live 2v2 match still remains mutually exclusive.
create or replace function private.team_arena_playing_v1(p_user_id uuid)
returns boolean
language sql
security definer
set search_path=''
as $$
  select exists(
    select 1
    from public.team_arena_members m
    join public.team_arena_rooms r on r.id=m.room_id
    where m.user_id=p_user_id
      and r.status='playing'
      and r.ends_at>clock_timestamp()
  );
$$;

create or replace function private.release_team_arena_lobbies_for_user_v1(p_user_id uuid)
returns integer
language plpgsql
security definer
set search_path=''
as $$
declare
  v_changed integer:=0;
  v_rows integer:=0;
begin
  if p_user_id is null then return 0; end if;

  perform private.cleanup_team_arena_v1();

  -- If the user owns a waiting lobby, switching modes closes that lobby cleanly.
  update public.team_arena_rooms r
  set status='cancelled',
      finished_at=coalesce(r.finished_at,clock_timestamp())
  where r.host_id=p_user_id
    and r.status='lobby';

  get diagnostics v_rows=row_count;
  v_changed:=v_changed+v_rows;

  -- If the user was a guest in somebody else's waiting lobby, leave the seat.
  delete from public.team_arena_members m
  using public.team_arena_rooms r
  where m.room_id=r.id
    and m.user_id=p_user_id
    and r.status='lobby'
    and r.host_id<>p_user_id;

  get diagnostics v_rows=row_count;
  v_changed:=v_changed+v_rows;

  -- Pending invitations belonging to released/cancelled lobby membership are no longer actionable.
  update public.team_arena_invites i
  set status='expired',
      responded_at=coalesce(i.responded_at,clock_timestamp())
  where i.status='pending'
    and (
      i.receiver_id=p_user_id
      or i.sender_id=p_user_id
      or exists(
        select 1
        from public.team_arena_rooms r
        where r.id=i.room_id and r.status<>'lobby'
      )
    );

  return v_changed;
end
$$;

create or replace function private.prevent_queue_during_team_arena_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if new.status='waiting' then
    if private.team_arena_playing_v1(new.user_id) then
      raise exception 'team_arena_active';
    end if;
    perform private.release_team_arena_lobbies_for_user_v1(new.user_id);
  end if;
  return new;
end
$$;

create or replace function private.prevent_room_during_team_arena_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if private.team_arena_playing_v1(new.host_id)
     or (new.guest_id is not null and private.team_arena_playing_v1(new.guest_id))
  then
    raise exception 'team_arena_active';
  end if;

  perform private.release_team_arena_lobbies_for_user_v1(new.host_id);
  if new.guest_id is not null then
    perform private.release_team_arena_lobbies_for_user_v1(new.guest_id);
  end if;
  return new;
end
$$;

create or replace function private.prevent_daily_during_team_arena_v1()
returns trigger
language plpgsql
security definer
set search_path=''
as $$
begin
  if new.status='playing' then
    if private.team_arena_playing_v1(new.user_id) then
      raise exception 'team_arena_active';
    end if;
    perform private.release_team_arena_lobbies_for_user_v1(new.user_id);
  end if;
  return new;
end
$$;

revoke all on function private.team_arena_playing_v1(uuid) from public,anon,authenticated;
revoke all on function private.release_team_arena_lobbies_for_user_v1(uuid) from public,anon,authenticated;

-- 3) Draws must never penalize both players. A completely empty draw is not a rated match.
create or replace function public.sonharf_apply_finish(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path to public, pg_temp
as $$
declare
  r public.game_rooms;
  v_meaningful boolean;
begin
  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status<>'finished' or r.stats_applied then return r; end if;

  v_meaningful := (r.winner_id is not null or coalesce(r.valid_word_count,0)>0);

  if r.is_bot then
    update public.profiles
    set total_matches=total_matches+case when v_meaningful then 1 else 0 end,
        wins=wins+case when r.winner_id=r.host_id and not r.winner_is_bot then 1 else 0 end,
        losses=losses+case when r.winner_id is not null and (r.winner_is_bot or r.winner_id<>r.host_id) then 1 else 0 end,
        rating=greatest(
          100,
          rating+case
            when r.winner_id is null then 0
            when r.winner_id=r.host_id and not r.winner_is_bot then 18
            else -14
          end
        ),
        presence_status='online',
        last_seen_at=now()
    where id=r.host_id;
  else
    update public.profiles
    set total_matches=total_matches+case when v_meaningful then 1 else 0 end,
        wins=wins+case when r.winner_id is not null and id=r.winner_id then 1 else 0 end,
        losses=losses+case when r.winner_id is not null and id<>r.winner_id then 1 else 0 end,
        rating=greatest(
          100,
          rating+case
            when r.winner_id is null then 0
            when id=r.winner_id then 20
            else -15
          end
        ),
        presence_status='online',
        last_seen_at=now()
    where id in (r.host_id,r.guest_id);
  end if;

  update public.game_rooms
  set stats_applied=true,
      streak_shielded_user_id=null
  where id=r.id
  returning * into r;

  delete from public.profile_photo_access
  where owner_id in (r.host_id,r.guest_id)
     or viewer_id in (r.host_id,r.guest_id);

  return r;
end
$$;

-- 4) One progression route ties all game modes together.
create table if not exists public.unified_mission_claims(
  user_id uuid not null references public.profiles(id) on delete cascade,
  mission_id text not null,
  scope text not null check(scope in ('daily','weekly')),
  period_start date not null,
  reward_coins integer not null default 0 check(reward_coins>=0),
  claimed_at timestamptz not null default clock_timestamp(),
  primary key(user_id,mission_id,period_start)
);

alter table public.unified_mission_claims enable row level security;
revoke all on table public.unified_mission_claims from public,anon,authenticated;

create or replace function public.get_unified_missions_v1()
returns table(
  mission_id text,
  scope text,
  period_start date,
  title_tr text,
  title_en text,
  mode_key text,
  target integer,
  progress integer,
  reward_coins integer,
  completed boolean,
  claimed boolean,
  route_order integer
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_now_local timestamp:=timezone('Europe/Istanbul',clock_timestamp());
  v_today date:=v_now_local::date;
  v_week date:=date_trunc('week',v_now_local)::date;
  v_today_ts timestamptz:=(date_trunc('day',v_now_local) at time zone 'Europe/Istanbul');
  v_week_ts timestamptz:=(date_trunc('week',v_now_local) at time zone 'Europe/Istanbul');
  v_day integer:=extract(isodow from v_now_local)::integer;

  d_duel integer:=0;
  d_feature integer:=0;
  w_duel integer:=0;
  w_word integer:=0;
  w_daily integer:=0;
  w_cipher integer:=0;
  w_bil integer:=0;
  w_team integer:=0;
  w_done integer:=0;
  v_feature_key text;
  v_feature_tr text;
  v_feature_en text;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select count(*)::int into d_duel
  from public.game_rooms g
  where g.status='finished'
    and coalesce(g.finished_at,g.created_at)>=v_today_ts
    and (g.host_id=v_uid or g.guest_id=v_uid)
    and (coalesce(g.valid_word_count,0)>0 or g.winner_id is not null);

  select count(*)::int into w_duel
  from public.game_rooms g
  where g.status='finished'
    and coalesce(g.finished_at,g.created_at)>=v_week_ts
    and (g.host_id=v_uid or g.guest_id=v_uid)
    and (coalesce(g.valid_word_count,0)>0 or g.winner_id is not null);

  select count(*)::int into w_word
  from public.word_arena_rooms a
  where a.status='finished'
    and coalesce(a.finished_at,a.created_at)>=v_week_ts
    and (a.host_id=v_uid or a.guest_id=v_uid);

  select count(*)::int into w_daily
  from public.daily_arena_runs d
  where d.user_id=v_uid
    and d.status='finished'
    and coalesce(d.finished_at,d.created_at)>=v_week_ts;

  select count(*)::int into w_cipher
  from public.daily_cipher_sessions c
  where c.user_id=v_uid
    and c.finished
    and c.challenge_date>=v_week;

  select count(*)::int into w_bil
  from public.app_events e
  where e.user_id=v_uid
    and e.event_name='bil_bakalim_match_finished'
    and e.created_at>=v_week_ts;

  select count(distinct r.id)::int into w_team
  from public.team_arena_rooms r
  join public.team_arena_members m on m.room_id=r.id
  where m.user_id=v_uid
    and r.status='finished'
    and coalesce(r.finished_at,r.created_at)>=v_week_ts;

  v_done :=
    (case when w_duel>=3 then 1 else 0 end)+
    (case when w_word>=2 then 1 else 0 end)+
    (case when w_daily>=1 then 1 else 0 end)+
    (case when w_cipher>=1 then 1 else 0 end)+
    (case when w_bil>=2 then 1 else 0 end)+
    (case when w_team>=1 then 1 else 0 end);

  case v_day
    when 1 then
      v_feature_key:='word_arena'; v_feature_tr:='Kelime Arenası: 1 maç tamamla'; v_feature_en:='Word Arena: finish 1 match';
      select count(*)::int into d_feature from public.word_arena_rooms a
      where a.status='finished' and coalesce(a.finished_at,a.created_at)>=v_today_ts
        and (a.host_id=v_uid or a.guest_id=v_uid);
    when 2 then
      v_feature_key:='bil_bakalim'; v_feature_tr:='Bil Bakalım: 1 maç tamamla'; v_feature_en:='Trivia: finish 1 match';
      select count(*)::int into d_feature from public.app_events e
      where e.user_id=v_uid and e.event_name='bil_bakalim_match_finished' and e.created_at>=v_today_ts;
    when 3 then
      v_feature_key:='daily_arena'; v_feature_tr:='Günlük Arena: resmî koşuyu tamamla'; v_feature_en:='Daily Arena: finish the official run';
      select count(*)::int into d_feature from public.daily_arena_runs d
      where d.user_id=v_uid and d.status='finished' and coalesce(d.finished_at,d.created_at)>=v_today_ts;
    when 4 then
      v_feature_key:='team_arena'; v_feature_tr:='Takım Arenası: 1 maç tamamla'; v_feature_en:='Team Arena: finish 1 match';
      select count(distinct r.id)::int into d_feature
      from public.team_arena_rooms r join public.team_arena_members m on m.room_id=r.id
      where m.user_id=v_uid and r.status='finished' and coalesce(r.finished_at,r.created_at)>=v_today_ts;
    when 5 then
      v_feature_key:='daily_cipher'; v_feature_tr:='Kelime Avı: bugünün bulmacasını tamamla'; v_feature_en:='Word Hunt: finish today''s puzzle';
      select count(*)::int into d_feature from public.daily_cipher_sessions c
      where c.user_id=v_uid and c.finished and c.challenge_date=v_today;
    when 6 then
      v_feature_key:='word_arena'; v_feature_tr:='Kelime Arenası: 1 maç tamamla'; v_feature_en:='Word Arena: finish 1 match';
      select count(*)::int into d_feature from public.word_arena_rooms a
      where a.status='finished' and coalesce(a.finished_at,a.created_at)>=v_today_ts
        and (a.host_id=v_uid or a.guest_id=v_uid);
    else
      v_feature_key:='bil_bakalim'; v_feature_tr:='Bil Bakalım: 1 maç tamamla'; v_feature_en:='Trivia: finish 1 match';
      select count(*)::int into d_feature from public.app_events e
      where e.user_id=v_uid and e.event_name='bil_bakalim_match_finished' and e.created_at>=v_today_ts;
  end case;

  return query
  with mission_rows as (
    select 'daily_duel'::text id,'daily'::text sc,v_today ps,
      'Bugünün Düellosu: 1 maç tamamla'::text tr,'Today''s Duel: finish 1 match'::text en,
      'duel'::text mk,1::int tgt,least(d_duel,1)::int prog,4::int reward,10::int ord
    union all
    select 'daily_featured','daily',v_today,v_feature_tr,v_feature_en,v_feature_key,1,least(d_feature,1),6,20
    union all
    select 'daily_route','daily',v_today,
      'Günlük Rota: iki görevi de bitir','Daily Route: finish both tasks',
      'route',2,(least(d_duel,1)+least(d_feature,1))::int,10,30
    union all
    select 'weekly_duel','weekly',v_week,'Düello Ustası: 3 maç tamamla','Duel Master: finish 3 matches','duel',3,least(w_duel,3),8,100
    union all
    select 'weekly_word_arena','weekly',v_week,'Kelime Arenası: 2 maç tamamla','Word Arena: finish 2 matches','word_arena',2,least(w_word,2),8,110
    union all
    select 'weekly_daily_arena','weekly',v_week,'Günlük Arena: 1 resmî koşu','Daily Arena: 1 official run','daily_arena',1,least(w_daily,1),8,120
    union all
    select 'weekly_cipher','weekly',v_week,'Kelime Avı: 1 günlük bulmaca','Word Hunt: 1 daily puzzle','daily_cipher',1,least(w_cipher,1),8,130
    union all
    select 'weekly_bil','weekly',v_week,'Bil Bakalım: 2 maç tamamla','Trivia: finish 2 matches','bil_bakalim',2,least(w_bil,2),8,140
    union all
    select 'weekly_team','weekly',v_week,'Takım Arenası: 1 maç tamamla','Team Arena: finish 1 match','team_arena',1,least(w_team,1),10,150
    union all
    select 'weekly_route','weekly',v_week,
      'Haftalık Büyük Rota: tüm oyunları tamamla','Weekly Grand Route: complete every mode',
      'route',6,v_done,35,190
  )
  select
    m.id,m.sc,m.ps,m.tr,m.en,m.mk,m.tgt,m.prog,m.reward,
    (m.prog>=m.tgt),
    exists(
      select 1 from public.unified_mission_claims c
      where c.user_id=v_uid and c.mission_id=m.id and c.period_start=m.ps
    ),
    m.ord
  from mission_rows m
  order by m.ord;
end
$$;

create or replace function public.claim_unified_mission_v1(p_mission_id text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  m record;
  v_balance integer:=0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select * into m
  from public.get_unified_missions_v1() x
  where x.mission_id=p_mission_id
  limit 1;

  if m.mission_id is null then raise exception 'mission_not_found'; end if;
  if not m.completed then raise exception 'mission_not_complete'; end if;
  if m.claimed then raise exception 'mission_already_claimed'; end if;

  insert into public.unified_mission_claims(user_id,mission_id,scope,period_start,reward_coins)
  values(v_uid,m.mission_id,m.scope,m.period_start,m.reward_coins)
  on conflict(user_id,mission_id,period_start) do nothing;

  if not found then raise exception 'mission_already_claimed'; end if;

  update public.profiles p
  set diamonds=coalesce(p.diamonds,0)+m.reward_coins
  where p.id=v_uid
  returning p.diamonds into v_balance;

  insert into public.diamond_ledger(user_id,delta,reason)
  values(v_uid,m.reward_coins,'unified_route:'||m.mission_id);

  return jsonb_build_object(
    'success',true,
    'mission_id',m.mission_id,
    'reward_coins',m.reward_coins,
    'balance',v_balance
  );
end
$$;

revoke all on function public.get_unified_missions_v1() from public,anon;
revoke all on function public.claim_unified_mission_v1(text) from public,anon;
grant execute on function public.get_unified_missions_v1() to authenticated;
grant execute on function public.claim_unified_mission_v1(text) to authenticated;
