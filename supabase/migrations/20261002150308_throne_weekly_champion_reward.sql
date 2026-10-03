-- Withdraw new sales; historical paid ownership remains restorable.
update public.store_catalog_config set enabled=false,updated_at=now() where product_id='profile_frame_gold_crest';
update public.shop_items set active=false where id='profile_frame_gold_crest';

-- Read-time reward: exactly the preceding completed week, never the current leader.
-- No permanent inventory grant and no scheduler delay at the Monday boundary.
create or replace function private.throne_champion_at(p_at timestamptz)
returns uuid language sql stable security definer set search_path='' as $$
 select e.user_id from public.throne_xp_events e
 join public.profiles p on p.id=e.user_id
 where e.week_start=date_trunc('week',p_at at time zone 'Europe/Istanbul')::date-7
 group by e.user_id order by sum(e.xp) desc,min(e.earned_at),e.user_id limit 1
$$;
revoke all on function private.throne_champion_at(timestamptz) from public,anon,authenticated;

create or replace function public.get_public_profile_frame_v1(p_user_id uuid)
returns table(user_id uuid,profile_frame_id text)
language sql stable security definer set search_path='' as $$
 select p.id,case when private.throne_champion_at(now())=p.id then 'frame_throne_champion'
 else s.id end
 from public.profiles p
 left join public.user_equipped_cosmetics e on e.user_id=p.id
 left join public.shop_items s on s.id=e.profile_frame_id and s.kind='profile_frame'
 where auth.uid() is not null and p.id=p_user_id limit 1
$$;
revoke all on function public.get_public_profile_frame_v1(uuid) from public,anon;
grant execute on function public.get_public_profile_frame_v1(uuid) to authenticated;

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
 select jsonb_build_object('user_id',p.id,'name',p.display_name,'xp',t.xp,'week_start',w-7,'expires_at',(w+7)::timestamp at time zone 'Europe/Istanbul','avatar_path',case when p.avatar_visibility='hidden' then null else p.avatar_path end,'gender',p.gender,'avatar_visibility',p.avatar_visibility) into previous from totals t join public.profiles p on p.id=t.user_id order by t.xp desc,t.first_at,t.user_id limit 1;
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
