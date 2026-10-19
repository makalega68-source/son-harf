alter table public.atelier_daily_runs drop constraint atelier_daily_runs_duration_chk;
alter table public.atelier_daily_runs drop constraint atelier_daily_runs_score_chk;
alter table public.atelier_daily_runs drop constraint atelier_daily_runs_words_chk;
alter table public.atelier_daily_runs drop constraint atelier_daily_runs_tasks_chk;
alter table public.atelier_daily_runs
 add constraint atelier_daily_runs_duration_chk check(duration_seconds in (60,120,180,300)),
 add constraint atelier_daily_runs_score_chk check(score between 0 and 30000),
 add constraint atelier_daily_runs_words_chk check(words between 0 and 500),
 add constraint atelier_daily_runs_tasks_chk check(tasks between 0 and 36);
create or replace function public.atelier_duration_v1(p_duration integer)
returns integer language sql immutable set search_path='' as $$
 select case when p_duration>=300 then 300 when p_duration>=180 then 180 when p_duration>=120 then 120 else 60 end
$$;
create or replace function public.start_atelier_daily_v2(p_language text,p_duration integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
 v_uid uuid:=auth.uid(); v_lang text:=case when p_language='en' then 'en' else 'tr' end;
 v_secs integer:=public.atelier_duration_v1(p_duration); v_day date:=public.atelier_today_v1();
 v_run public.atelier_daily_runs%rowtype;
begin
 if v_uid is null then raise exception 'unauthorized'; end if;
 insert into public.atelier_daily_runs(user_id,language,day,duration_seconds)
 values(v_uid,v_lang,v_day,v_secs) on conflict(user_id,language,day,duration_seconds) do nothing;
 if found then return jsonb_build_object('started',true,'already_played',false,'day',v_day); end if;
 select * into v_run from public.atelier_daily_runs where user_id=v_uid and language=v_lang and day=v_day and duration_seconds=v_secs;
 return jsonb_build_object('started',false,'already_played',true,'day',v_day,'finished',v_run.finished_at is not null,'score',v_run.score);
end $$;
create or replace function public.finish_atelier_daily_v2(p_language text,p_duration integer,p_score integer,p_words integer,p_tasks integer)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
 v_uid uuid:=auth.uid(); v_lang text:=case when p_language='en' then 'en' else 'tr' end;
 v_secs integer:=public.atelier_duration_v1(p_duration); v_day date:=public.atelier_today_v1();
 v_max_tasks integer:=case v_secs when 300 then 36 when 180 then 24 when 120 then 15 else 6 end;
 v_run public.atelier_daily_runs%rowtype; v_rank integer; v_total integer;
begin
 if v_uid is null then raise exception 'unauthorized'; end if;
 if p_score is null or p_words is null or p_tasks is null or p_score<0 or p_score>v_secs/60*6000
 or p_words<0 or p_words>v_secs/60*100 or p_tasks<0 or p_tasks>v_max_tasks then raise exception 'implausible_score'; end if;
 select * into v_run from public.atelier_daily_runs
 where user_id=v_uid and language=v_lang and duration_seconds=v_secs and day in(v_day,v_day-1)
 and finished_at is null and started_at>now()-interval '15 minutes'
 order by started_at desc limit 1 for update;
 if not found then raise exception 'no_open_run'; end if;
 if v_run.started_at>now()-make_interval(secs=>v_secs-2) then raise exception 'round_not_finished'; end if;
 update public.atelier_daily_runs set score=p_score,words=p_words,tasks=p_tasks,finished_at=now()
 where user_id=v_uid and language=v_lang and duration_seconds=v_secs and day=v_run.day;
 select count(*)+1 into v_rank from public.atelier_daily_runs where language=v_lang and day=v_run.day and duration_seconds=v_secs and finished_at is not null and score>p_score;
 select count(*) into v_total from public.atelier_daily_runs where language=v_lang and day=v_run.day and duration_seconds=v_secs and finished_at is not null;
 return jsonb_build_object('rank',v_rank,'total',v_total,'score',p_score);
end $$;
revoke all on function public.atelier_duration_v1(integer) from public,anon;
revoke all on function public.start_atelier_daily_v2(text,integer) from public,anon;
revoke all on function public.finish_atelier_daily_v2(text,integer,integer,integer,integer) from public,anon;
grant execute on function public.atelier_duration_v1(integer) to authenticated;
grant execute on function public.start_atelier_daily_v2(text,integer) to authenticated;
grant execute on function public.finish_atelier_daily_v2(text,integer,integer,integer,integer) to authenticated;
