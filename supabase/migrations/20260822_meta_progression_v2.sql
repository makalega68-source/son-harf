-- Son Harf meta progression v2: season, daily play streak, cup, records, titles and season pass.
-- User-facing currency is Son Coin; the legacy diamonds column remains the stable storage field.

alter table public.profiles add column if not exists selected_title text not null default 'ÇAYLAK';

create table if not exists public.season_pass_entitlements (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  product_id text not null,
  status text not null default 'active',
  expires_at timestamptz not null,
  updated_at timestamptz not null default now()
);
alter table public.season_pass_entitlements enable row level security;
drop policy if exists season_pass_self_select on public.season_pass_entitlements;
create policy season_pass_self_select on public.season_pass_entitlements for select to authenticated using ((select auth.uid())=user_id);

create table if not exists public.season_reward_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  season_id text not null,
  tier integer not null check (tier between 1 and 10),
  premium boolean not null default false,
  reward_diamonds integer not null,
  claimed_at timestamptz not null default now(),
  primary key(user_id,season_id,tier,premium)
);
alter table public.season_reward_claims enable row level security;
drop policy if exists season_reward_self_select on public.season_reward_claims;
create policy season_reward_self_select on public.season_reward_claims for select to authenticated using ((select auth.uid())=user_id);

create or replace function public.get_meta_progress_v2()
returns table(
  season_id text, season_name text, season_day integer, season_days integer,
  season_xp integer, season_level integer, season_progress integer, season_target integer,
  daily_play_streak integer, best_daily_play_streak integer,
  unique_words integer, longest_word text, longest_word_length integer,
  highest_score integer, best_streak integer,
  cup_points integer, cup_rank integer, cup_qualified boolean, cup_active boolean,
  selected_title text, available_titles integer, season_pass_active boolean,
  free_claimed_tiers integer[], premium_claimed_tiers integer[]
)
language sql security definer set search_path=public,pg_temp as $$
with recursive
me as (select p.* from public.profiles p where p.id=(select auth.uid())),
season as (
  select to_char(current_date,'YYYY-MM') sid,
         to_char(current_date,'FMMonth YYYY') sname,
         extract(day from current_date)::int sday,
         extract(day from (date_trunc('month',current_date)+interval '1 month - 1 day'))::int sdays,
         date_trunc('month',current_date)::date sstart,
         (date_trunc('month',current_date)+interval '1 month')::date send
),
season_games as (
  select g.* from public.game_rooms g, season s
  where g.status='finished' and coalesce(g.finished_at,g.created_at)::date>=s.sstart and coalesce(g.finished_at,g.created_at)::date<s.send
    and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
),
season_calc as (select (count(*)*35 + count(*) filter(where winner_id=(select auth.uid()))*85)::int xp from season_games),
play_days as (
  select distinct coalesce(g.finished_at,g.created_at)::date d from public.game_rooms g
  where g.status='finished' and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
),
ordered_days as (select d,d-(row_number() over(order by d))::int grp from play_days),
groups as (select min(d) first_d,max(d) last_d,count(*)::int n from ordered_days group by grp),
streaks as (
  select coalesce(max(n) filter(where last_d in (current_date,current_date-1)),0)::int current_n,
         coalesce(max(n),0)::int best_n from groups
),
word_stats as (
  select count(distinct gw.normalized_word)::int unique_n,
         coalesce((array_agg(gw.word order by length(gw.word) desc,gw.created_at asc))[1],'') longest
  from public.game_words gw where gw.player_id=(select auth.uid()) and coalesce(gw.is_bot,false)=false
),
score_stats as (
  select coalesce(max(case when g.host_id=(select auth.uid()) then g.host_score else g.guest_score end),0)::int hi
  from public.game_rooms g where g.status='finished' and (g.host_id=(select auth.uid()) or g.guest_id=(select auth.uid()))
),
week_games as (
  select g.* from public.game_rooms g where g.status='finished' and coalesce(g.finished_at,g.created_at)>=date_trunc('week',now())
),
cup_scores as (
  select p.id user_id,(count(wg.id)+count(wg.id) filter(where wg.winner_id=p.id)*2)::int pts
  from public.profiles p left join week_games wg on (wg.host_id=p.id or wg.guest_id=p.id) group by p.id
),
cup_ranked as (select user_id,pts,row_number() over(order by pts desc,user_id)::int rnk from cup_scores),
claims as (
  select coalesce(array_agg(tier order by tier) filter(where premium=false),'{}'::integer[]) free_tiers,
         coalesce(array_agg(tier order by tier) filter(where premium=true),'{}'::integer[]) prem_tiers
  from public.season_reward_claims c,season s where c.user_id=(select auth.uid()) and c.season_id=s.sid
)
select s.sid,s.sname,s.sday,s.sdays,
       sc.xp,greatest(1,(sc.xp/300)+1)::int,(sc.xp%300)::int,300::int,
       st.current_n,st.best_n,ws.unique_n,ws.longest,length(ws.longest)::int,
       ss.hi,coalesce(m.best_streak,0)::int,
       coalesce(cr.pts,0),coalesce(cr.rnk,0),coalesce(cr.rnk<=16,false),extract(isodow from current_date)::int in (6,7),
       m.selected_title,
       (1 + (coalesce(m.wins,0)>=5)::int + (coalesce(m.wins,0)>=20)::int + (coalesce(m.wins,0)>=50)::int + (coalesce(m.wins,0)>=100)::int + (ws.unique_n>=250)::int + (coalesce(m.best_streak,0)>=10)::int)::int,
       exists(select 1 from public.season_pass_entitlements e where e.user_id=(select auth.uid()) and e.status='active' and e.expires_at>now()),
       c.free_tiers,c.prem_tiers
from me m cross join season s cross join season_calc sc cross join streaks st cross join word_stats ws cross join score_stats ss
left join cup_ranked cr on cr.user_id=m.id cross join claims c;
$$;
revoke all on function public.get_meta_progress_v2() from public,anon;
grant execute on function public.get_meta_progress_v2() to authenticated;

create or replace function public.set_selected_title_v1(p_title text)
returns text language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_wins integer; v_best integer; v_unique integer; v_title text:=upper(trim(p_title));
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select coalesce(wins,0),coalesce(best_streak,0) into v_wins,v_best from public.profiles where id=v_uid;
  select count(distinct normalized_word)::int into v_unique from public.game_words where player_id=v_uid and coalesce(is_bot,false)=false;
  if not (v_title='ÇAYLAK' or (v_title='YÜKSELEN' and v_wins>=5) or (v_title='DÜELLOCU' and v_wins>=20) or (v_title='USTA' and v_wins>=50) or (v_title='EFSANE' and v_wins>=100) or (v_title='KELİME AVCISI' and v_unique>=250) or (v_title='SERİ USTASI' and v_best>=10)) then raise exception 'title_locked'; end if;
  update public.profiles set selected_title=v_title,updated_at=now() where id=v_uid;
  return v_title;
end $$;
revoke all on function public.set_selected_title_v1(text) from public,anon;
grant execute on function public.set_selected_title_v1(text) to authenticated;

create or replace function public.claim_season_reward_v1(p_tier integer,p_premium boolean default false)
returns integer language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_sid text:=to_char(current_date,'YYYY-MM'); v_xp integer; v_level integer; v_reward integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if p_tier<1 or p_tier>10 then raise exception 'invalid_tier'; end if;
  select (count(*)*35 + count(*) filter(where winner_id=v_uid)*85)::int into v_xp from public.game_rooms
    where status='finished' and coalesce(finished_at,created_at)>=date_trunc('month',now()) and coalesce(finished_at,created_at)<date_trunc('month',now())+interval '1 month' and (host_id=v_uid or guest_id=v_uid);
  v_level:=greatest(1,(coalesce(v_xp,0)/300)+1);
  if v_level<p_tier then raise exception 'tier_locked'; end if;
  if p_premium and not exists(select 1 from public.season_pass_entitlements where user_id=v_uid and status='active' and expires_at>now()) then raise exception 'season_pass_required'; end if;
  v_reward:=case when p_premium then 40+p_tier*10 else 20+p_tier*5 end;
  insert into public.season_reward_claims(user_id,season_id,tier,premium,reward_diamonds) values(v_uid,v_sid,p_tier,p_premium,v_reward) on conflict do nothing;
  if not found then return 0; end if;
  update public.profiles set diamonds=coalesce(diamonds,0)+v_reward,updated_at=now() where id=v_uid;
  if to_regclass('public.diamond_ledger') is not null then insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_reward,case when p_premium then 'season_premium_reward' else 'season_free_reward' end); end if;
  return v_reward;
end $$;
revoke all on function public.claim_season_reward_v1(integer,boolean) from public,anon;
grant execute on function public.claim_season_reward_v1(integer,boolean) to authenticated;

create or replace function public.apply_verified_play_purchase_v1(
  p_user_id uuid,p_product_id text,p_purchase_token text,p_order_id text default null,p_expires_at timestamptz default null
) returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_inserted uuid; v_delta integer:=0; v_balance integer; v_inventory_item text;
begin
  if p_user_id is null or p_purchase_token is null or length(trim(p_purchase_token))<8 then raise exception 'invalid_purchase'; end if;
  if not exists(select 1 from public.profiles where id=p_user_id) then raise exception 'profile_not_found'; end if;
  insert into public.purchases(user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at) values(p_user_id,p_product_id,p_purchase_token,nullif(trim(p_order_id),''),'verified',now(),now()) on conflict(purchase_token) do nothing returning id into v_inserted;
  if v_inserted is null then return jsonb_build_object('success',true,'already_processed',true,'product_id',p_product_id); end if;
  if p_product_id in ('vip_monthly','vip_yearly') then
    if p_expires_at is null or p_expires_at<=now() then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.subscriptions(user_id,product_id,status,expires_at,updated_at) values(p_user_id,p_product_id,'active',p_expires_at,now()) on conflict(user_id) do update set product_id=excluded.product_id,status='active',expires_at=greatest(coalesce(public.subscriptions.expires_at,'epoch'::timestamptz),excluded.expires_at),updated_at=now();
    update public.profiles set is_vip=true where id=p_user_id;
  elsif p_product_id='season_pass_monthly' then
    if p_expires_at is null or p_expires_at<=now() then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.season_pass_entitlements(user_id,product_id,status,expires_at,updated_at) values(p_user_id,p_product_id,'active',p_expires_at,now()) on conflict(user_id) do update set product_id=excluded.product_id,status='active',expires_at=greatest(public.season_pass_entitlements.expires_at,excluded.expires_at),updated_at=now();
  elsif p_product_id='coins_500' then v_delta:=500;
  elsif p_product_id='coins_1500' then v_delta:=1500;
  elsif p_product_id='theme_neon' then v_inventory_item:='theme_neon';
  else raise exception 'unsupported_product'; end if;
  if v_delta>0 then update public.profiles set diamonds=coalesce(diamonds,0)+v_delta where id=p_user_id returning diamonds into v_balance; insert into public.diamond_ledger(user_id,delta,reason) values(p_user_id,v_delta,'google_play_purchase');
  elsif v_inventory_item is not null then insert into public.user_inventory(user_id,item_id) values(p_user_id,v_inventory_item) on conflict(user_id,item_id) do nothing; end if;
  return jsonb_build_object('success',true,'already_processed',false,'product_id',p_product_id,'diamonds_granted',v_delta,'diamond_balance',v_balance,'inventory_item',v_inventory_item,'expires_at',p_expires_at);
end $$;
revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from public,anon,authenticated;
grant execute on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) to service_role;
