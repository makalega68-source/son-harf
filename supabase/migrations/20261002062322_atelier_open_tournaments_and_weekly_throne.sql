-- Istanbul schedules, append-only XP, weekly missions and three-stage open tournaments.
create table public.throne_xp_events (
 user_id uuid not null references public.profiles(id) on delete cascade,
 source text not null, source_id text not null, game text not null check(game in('siege','last_letter','atelier')),
 xp integer not null check(xp between 0 and 2000), rounds integer not null default 0, wins integer not null default 0,
 tasks integer not null default 0, earned_at timestamptz not null default now(),
 week_start date not null default date_trunc('week',now() at time zone 'Europe/Istanbul')::date,
 primary key(user_id,source,source_id)
);
create index throne_xp_week_user on public.throne_xp_events(week_start,user_id,game);
alter table public.throne_xp_events enable row level security;
revoke all on public.throne_xp_events from public,anon,authenticated;
create policy throne_xp_own on public.throne_xp_events for select to authenticated using((select auth.uid())=user_id);
grant select on public.throne_xp_events to authenticated;
create table public.atelier_tournament_runs (
 user_id uuid not null references public.profiles(id) on delete cascade,
 event_start timestamptz not null, language text not null check(language in('tr','en')),
 stage integer not null check(stage between 1 and 3), started_at timestamptz not null default now(),
 finished_at timestamptz, score integer not null default 0, words integer not null default 0,
 tasks integer not null default 0, xp integer not null default 0,
 primary key(user_id,event_start,stage),
 check(score between 0 and 18000),check(words between 0 and 300),check(tasks between 0 and 24)
);
create index atelier_tournament_event_board on public.atelier_tournament_runs(event_start,user_id) where finished_at is not null;
alter table public.atelier_tournament_runs enable row level security;
revoke all on public.atelier_tournament_runs from public,anon,authenticated;
create policy atelier_tournament_own on public.atelier_tournament_runs for select to authenticated using((select auth.uid())=user_id);
grant select on public.atelier_tournament_runs to authenticated;

create function private.atelier_event_start(p_now timestamptz)
returns timestamptz language sql stable set search_path='' as $$
 select max(s) from (
 select ((p_now at time zone 'Europe/Istanbul')::date+d+make_interval(hours=>h)) at time zone 'Europe/Istanbul' s
 from generate_series(-1,0) d cross join generate_series(0,23) h where h%2=0 or h=19
 ) t where s<=p_now
$$;
create function private.atelier_event_next(p_now timestamptz)
returns timestamptz language sql stable set search_path='' as $$
 select min(s) from (
 select ((p_now at time zone 'Europe/Istanbul')::date+d+make_interval(hours=>h)) at time zone 'Europe/Istanbul' s
 from generate_series(0,1) d cross join generate_series(0,23) h where h%2=0 or h=19
 ) t where s>p_now
$$;
create function private.throne_award_xp(p_uid uuid,p_source text,p_id text,p_game text,p_xp integer,p_rounds integer default 0,p_wins integer default 0,p_tasks integer default 0)
returns void language plpgsql security definer set search_path='' as $$
declare w date:=date_trunc('week',now() at time zone 'Europe/Istanbul')::date; n integer; v record;
begin
 if p_uid is null then return; end if;
 -- Serialize all awards for this player, including automatic mission bonuses.
 perform pg_advisory_xact_lock(hashtextextended(p_uid::text,0));
 insert into public.throne_xp_events(user_id,source,source_id,game,xp,rounds,wins,tasks,week_start)
 values(p_uid,p_source,p_id,p_game,p_xp,p_rounds,p_wins,p_tasks,w) on conflict do nothing;
 get diagnostics n=row_count; if n=0 then return; end if;
 for v in
 select game,sum(rounds) rounds,sum(wins) wins,sum(tasks) tasks,sum(xp) xp
 from public.throne_xp_events where user_id=p_uid and week_start=w and source<>'mission' group by game
 loop
 if v.rounds>=5 then insert into public.throne_xp_events(user_id,source,source_id,game,xp,week_start)
 values(p_uid,'mission',w||':'||v.game||':rounds',v.game,100,w) on conflict do nothing; end if;
 if (v.game='atelier' and v.tasks>=12) or (v.game<>'atelier' and v.wins>=3) then
 insert into public.throne_xp_events(user_id,source,source_id,game,xp,week_start)
 values(p_uid,'mission',w||':'||v.game||':mastery',v.game,150,w) on conflict do nothing; end if;
 if v.xp>=1000 then insert into public.throne_xp_events(user_id,source,source_id,game,xp,week_start)
 values(p_uid,'mission',w||':'||v.game||':xp',v.game,200,w) on conflict do nothing; end if;
 end loop;
end $$;

create function private.throne_game_result() returns trigger language plpgsql security definer set search_path='' as $$
declare a uuid;b uuid;winner uuid;g text;qualified boolean;
begin
 if tg_table_name='game_rooms' then
 qualified:=new.status='finished' and new.stats_applied and new.valid_word_count>=2;
 a:=new.host_id;b:=new.guest_id;winner:=new.winner_id;g:='last_letter';
 else
 qualified:=new.status='finished' and new.result_applied and new.move_count>=2 and new.player_one_word_score+new.player_two_word_score>0;
 a:=new.player_one_id;b:=new.player_two_id;winner:=new.winner_id;g:='siege';
 end if;
 if qualified then
 perform private.throne_award_xp(a,'match',new.id::text,g,35+case when a=winner then 85 else 0 end,1,case when a=winner then 1 else 0 end);
 perform private.throne_award_xp(b,'match',new.id::text,g,35+case when b=winner then 85 else 0 end,1,case when b=winner then 1 else 0 end);
 end if; return new;
end $$;
create trigger throne_last_letter_result after update of status,stats_applied on public.game_rooms for each row execute function private.throne_game_result();
create trigger throne_siege_result after update of status,result_applied on public.word_siege_games for each row execute function private.throne_game_result();
create function private.throne_atelier_result() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.finished_at is not null and old.finished_at is null then
 perform private.throne_award_xp(new.user_id,'daily_atelier',new.day||':'||new.language||':'||new.duration_seconds,'atelier',
 least(120,35+new.score/greatest(20,new.duration_seconds/60*20)),1,0,new.tasks);
 end if; return new;
end $$;
create trigger throne_atelier_daily_result after update of finished_at on public.atelier_daily_runs for each row execute function private.throne_atelier_result();

create function public.start_atelier_tournament_v1(p_language text)
returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid(); e timestamptz:=private.atelier_event_start(now());
 s integer:=floor(extract(epoch from(now()-e))/600)::integer+1; secs integer; lang text:=case when p_language='en' then 'en' else 'tr' end; n integer;
begin
 if u is null then raise exception 'unauthorized'; end if;
 if s>3 then raise exception 'tournament_closed'; end if;
 secs:=case s when 1 then 60 when 2 then 120 else 180 end;
 if now()+make_interval(secs=>secs+10)>e+make_interval(secs=>s*600) then raise exception 'stage_entry_closed'; end if;
 if s>1 and not exists(select 1 from public.atelier_tournament_runs where user_id=u and event_start=e and stage=s-1 and finished_at is not null and language=lang) then raise exception 'previous_stage_required'; end if;
 insert into public.atelier_tournament_runs(user_id,event_start,language,stage) values(u,e,lang,s) on conflict do nothing;
 get diagnostics n=row_count; if n=0 then raise exception 'stage_already_started'; end if;
 return jsonb_build_object('event_start',e,'stage',s,'seconds',secs,'seed_key',e::text||':'||s||':'||lang,
 'multiplier',case when extract(hour from e at time zone 'Europe/Istanbul') in(19,22) then 3.0 else 1.5 end);
end $$;

create function public.finish_atelier_tournament_v1(p_event timestamptz,p_stage integer,p_score integer,p_words integer,p_tasks integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid(); r public.atelier_tournament_runs%rowtype; secs integer; max_tasks integer; m integer; x integer;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select * into r from public.atelier_tournament_runs where user_id=u and event_start=p_event and stage=p_stage for update;
 if not found then raise exception 'no_open_run'; end if;
 if r.finished_at is not null then return jsonb_build_object('xp',r.xp,'score',r.score,'already_saved',true); end if;
 secs:=case p_stage when 1 then 60 when 2 then 120 else 180 end;
 max_tasks:=case p_stage when 1 then 6 when 2 then 15 else 24 end;
 if p_score is null or p_words is null or p_tasks is null or p_score<0 or p_score>secs/60*6000 or p_words<0 or p_words>secs/60*100 or p_tasks<0 or p_tasks>max_tasks
 or p_tasks>p_words*3 or p_score>p_words*130+p_tasks*50 then raise exception 'implausible_score'; end if;
 if now()<r.started_at+make_interval(secs=>secs-2) then raise exception 'round_not_finished'; end if;
 if now()>p_event+make_interval(secs=>p_stage*600+15) then raise exception 'stage_expired'; end if;
 m:=case when extract(hour from p_event at time zone 'Europe/Istanbul') in(19,22) then 300 else 150 end;
 x:=least(120,35+p_score/(secs/60*20))*m/100;
 update public.atelier_tournament_runs set finished_at=now(),score=p_score,words=p_words,tasks=p_tasks,xp=x where user_id=u and event_start=p_event and stage=p_stage;
 perform private.throne_award_xp(u,'atelier_tournament',p_event::text||':'||p_stage,'atelier',x,1,0,p_tasks);
 return jsonb_build_object('xp',x,'score',p_score,'already_saved',false);
end $$;

create function public.get_atelier_tournament_v1()
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=auth.uid();e timestamptz:=private.atelier_event_start(now());s integer:=floor(extract(epoch from(now()-e))/600)::integer+1; rows jsonb; winners jsonb; previous timestamptz;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select jsonb_agg(to_jsonb(t)) into rows from (
 select row_number() over(order by count(*) desc,sum(score) desc,max(finished_at),user_id)::int rank,
 user_id,sum(score)::int score,count(*)::int stages from public.atelier_tournament_runs
 where event_start=e and finished_at is not null group by user_id order by count(*) desc,sum(score) desc,max(finished_at),user_id limit 50
 ) t;
 select max(event_start) into previous from public.atelier_tournament_runs where event_start+interval '30 minutes'<=now() and finished_at is not null;
 select jsonb_agg(to_jsonb(t)) into winners from (
 select row_number() over(order by count(*) desc,sum(r.score) desc,max(r.finished_at),r.user_id)::int rank,p.display_name name,
 sum(r.score)::int score,count(*)::int stages
 from public.atelier_tournament_runs r join public.profiles p on p.id=r.user_id
 where r.event_start=previous and r.finished_at is not null group by r.user_id,p.display_name
 order by count(*) desc,sum(r.score) desc,max(r.finished_at),r.user_id limit 3
 ) t;
 return jsonb_build_object('server_time',now(),'event_start',e,'next_start',private.atelier_event_next(now()),'active',s<=3,
 'stage',least(s,3),'stage_ends',e+make_interval(secs=>least(s,3)*600),
 'multiplier',case when extract(hour from e at time zone 'Europe/Istanbul') in(19,22) then 3.0 else 1.5 end,
 'rows',coalesce(rows,'[]'::jsonb),'winners',coalesce(winners,'[]'::jsonb),
 'my_stages',coalesce((select jsonb_agg(jsonb_build_object('stage',stage,'finished',finished_at is not null,'xp',xp)) from public.atelier_tournament_runs where event_start=e and user_id=u),'[]'::jsonb));
end $$;

create function public.get_throne_week_v1()
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=auth.uid();w date:=date_trunc('week',now() at time zone 'Europe/Istanbul')::date;rows jsonb;previous jsonb;me jsonb;missions jsonb;breakdown jsonb;
begin
 if u is null then raise exception 'unauthorized'; end if;
 with totals as(select user_id,sum(xp)::bigint xp,min(earned_at) first_at from public.throne_xp_events where week_start=w group by user_id),
 ranked as(select *,row_number() over(order by xp desc,first_at,user_id)::int rank from totals)
 select (select jsonb_agg(to_jsonb(t)) from(select r.rank,r.user_id,p.display_name name,r.xp,p.avatar_path,p.gender,p.avatar_visibility from ranked r join public.profiles p on p.id=r.user_id where rank<=50 order by rank)t),
 (select jsonb_build_object('rank',rank,'xp',xp) from ranked where user_id=u) into rows,me;
 with totals as(select user_id,sum(xp)::bigint xp,min(earned_at) first_at from public.throne_xp_events where week_start=w-7 group by user_id)
 select jsonb_build_object('name',p.display_name,'xp',t.xp,'week_start',w-7) into previous from totals t join public.profiles p on p.id=t.user_id order by t.xp desc,t.first_at,t.user_id limit 1;
 select jsonb_agg(jsonb_build_object('game',g,'xp',coalesce(x.xp,0),'rounds',coalesce(x.rounds,0),'wins',coalesce(x.wins,0),'tasks',coalesce(x.tasks,0))) into breakdown
 from unnest(array['siege','last_letter','atelier'])g left join lateral(
 select sum(xp) xp,sum(rounds) rounds,sum(wins) wins,sum(tasks) tasks from public.throne_xp_events where user_id=u and week_start=w and game=g)x on true;
 select jsonb_agg(jsonb_build_object('game',g,'id',m.id,'target',m.target,'reward',m.reward,'progress',least(m.target,coalesce(case m.id when 'rounds' then x.rounds when 'mastery' then case when g='atelier' then x.tasks else x.wins end else x.xp end,0)),
 'awarded',exists(select 1 from public.throne_xp_events where user_id=u and source='mission' and source_id=w||':'||g||':'||m.id))) into missions
 from unnest(array['siege','last_letter','atelier'])g
 cross join lateral(values('rounds',5,100),('mastery',case when g='atelier' then 12 else 3 end,150),('xp',1000,200))m(id,target,reward)
 left join lateral(select sum(rounds) rounds,sum(wins) wins,sum(tasks) tasks,sum(xp) xp from public.throne_xp_events where user_id=u and week_start=w and game=g and source<>'mission')x on true;
 return jsonb_build_object('server_time',now(),'week_start',w,'reset_at',(w+7)::timestamp at time zone 'Europe/Istanbul','rows',coalesce(rows,'[]'::jsonb),'me',coalesce(me,jsonb_build_object('rank',0,'xp',0)),
 'previous_owner',previous,'missions',coalesce(missions,'[]'::jsonb),'breakdown',coalesce(breakdown,'[]'::jsonb));
end $$;
revoke all on function private.atelier_event_start(timestamptz),private.atelier_event_next(timestamptz),private.throne_award_xp(uuid,text,text,text,integer,integer,integer,integer),private.throne_game_result(),private.throne_atelier_result() from public,anon,authenticated;
revoke all on function public.start_atelier_tournament_v1(text),public.finish_atelier_tournament_v1(timestamptz,integer,integer,integer,integer),public.get_atelier_tournament_v1(),public.get_throne_week_v1() from public,anon;
grant execute on function public.start_atelier_tournament_v1(text),public.finish_atelier_tournament_v1(timestamptz,integer,integer,integer,integer),public.get_atelier_tournament_v1(),public.get_throne_week_v1() to authenticated;
