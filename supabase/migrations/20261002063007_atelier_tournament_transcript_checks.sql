drop function public.finish_atelier_tournament_v1(timestamptz,integer,integer,integer,integer);
create function public.finish_atelier_tournament_v1(p_event timestamptz,p_stage integer,p_score integer,p_words integer,p_tasks integer,p_transcript text[])
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
 if p_transcript is null or cardinality(p_transcript)<>p_words or
 (select count(distinct word) from unnest(p_transcript)word)<>p_words or
 exists(select 1 from unnest(p_transcript)word where word is null or length(word) not between 3 and 7 or not private.word_siege_word_allowed_v1(word,r.language)) or
 p_score>coalesce((select sum(length(word)*10) from unnest(p_transcript)word),0)+p_tasks*50+p_words*60
 then raise exception 'invalid_transcript'; end if;
 if now()<r.started_at+make_interval(secs=>secs-2) then raise exception 'round_not_finished'; end if;
 if now()>p_event+make_interval(secs=>p_stage*600+15) then raise exception 'stage_expired'; end if;
 m:=case when extract(hour from p_event at time zone 'Europe/Istanbul') in(19,22) then 300 else 150 end;
 x:=least(120,35+p_score/(secs/60*20))*m/100;
 update public.atelier_tournament_runs set finished_at=now(),score=p_score,words=p_words,tasks=p_tasks,xp=x where user_id=u and event_start=p_event and stage=p_stage;
 perform private.throne_award_xp(u,'atelier_tournament',p_event::text||':'||p_stage,'atelier',x,1,0,p_tasks);
 return jsonb_build_object('xp',x,'score',p_score,'already_saved',false);
end $$;

revoke all on function public.finish_atelier_tournament_v1(timestamptz,integer,integer,integer,integer,text[]) from public,anon;
grant execute on function public.finish_atelier_tournament_v1(timestamptz,integer,integer,integer,integer,text[]) to authenticated;
create or replace function public.get_throne_week_v1()
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=auth.uid();w date:=date_trunc('week',now() at time zone 'Europe/Istanbul')::date;rows jsonb;previous jsonb;me jsonb;missions jsonb;breakdown jsonb;
begin
 if u is null then raise exception 'unauthorized'; end if;
 with totals as(select user_id,sum(xp)::bigint xp,min(earned_at) first_at from public.throne_xp_events where week_start=w group by user_id),
 ranked as(select *,row_number() over(order by xp desc,first_at,user_id)::int rank from totals)
 select (select jsonb_agg(to_jsonb(t)) from(select r.rank,r.user_id,p.display_name name,r.xp,case when p.avatar_visibility='hidden' then null else p.avatar_path end avatar_path,p.gender,p.avatar_visibility from ranked r join public.profiles p on p.id=r.user_id where rank<=50 order by rank)t),
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