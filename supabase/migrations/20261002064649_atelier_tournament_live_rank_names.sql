create or replace function public.get_atelier_tournament_v1()
returns jsonb language plpgsql stable security definer set search_path='' as $$
declare u uuid:=auth.uid();e timestamptz:=private.atelier_event_start(now());s integer:=floor(extract(epoch from(now()-e))/600)::integer+1; rows jsonb; winners jsonb; previous timestamptz;
begin
 if u is null then raise exception 'unauthorized'; end if;
 select jsonb_agg(to_jsonb(t)) into rows from (
 select row_number() over(order by count(*) desc,sum(r.score) desc,max(r.finished_at),user_id)::int rank,
 user_id,coalesce(max(p.display_name),'Oyuncu') name,sum(r.score)::int score,count(*)::int stages from public.atelier_tournament_runs r join public.profiles p on p.id=r.user_id
 where event_start=e and r.finished_at is not null group by user_id order by count(*) desc,sum(r.score) desc,max(r.finished_at),user_id limit 50
 ) t;
 select max(event_start) into previous from public.atelier_tournament_runs where event_start+interval '30 minutes'<=now() and finished_at is not null;
 select jsonb_agg(to_jsonb(t)) into winners from (
 select row_number() over(order by count(*) desc,sum(r.score) desc,max(r.finished_at),r.user_id)::int rank,coalesce(p.display_name,'Oyuncu') name,
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
