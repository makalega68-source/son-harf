-- Store season reward tracks layered on the existing competitive season.
-- No reward type can grant ranked power.

create table if not exists public.season_store_rewards (
  season_id text not null references public.competitive_seasons(id) on delete cascade,
  level integer not null check (level between 1 and 100),
  track text not null check (track in ('free','premium')),
  reward_type text not null check (reward_type in (
    'son_coin','profile_frame','badge','title','nameplate','victory_effect','word_effect','vs_intro','final_style'
  )),
  reward_key text not null default '',
  amount integer not null default 0 check (amount>=0),
  sort_order integer not null default 0,
  primary key(season_id,level,track,reward_type,reward_key)
);
alter table public.season_store_rewards enable row level security;
drop policy if exists season_store_rewards_read on public.season_store_rewards;
create policy season_store_rewards_read on public.season_store_rewards
for select to authenticated using (true);
grant select on public.season_store_rewards to authenticated;
revoke insert,update,delete on public.season_store_rewards from anon,authenticated;

create table if not exists public.season_store_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  season_id text not null references public.competitive_seasons(id) on delete cascade,
  level integer not null,
  track text not null check (track in ('free','premium')),
  reward_type text not null,
  reward_key text not null default '',
  claimed_at timestamptz not null default now(),
  primary key(user_id,season_id,level,track,reward_type,reward_key)
);
alter table public.season_store_claims enable row level security;
drop policy if exists season_store_claims_read_own on public.season_store_claims;
create policy season_store_claims_read_own on public.season_store_claims
for select to authenticated using ((select auth.uid())=user_id);
grant select on public.season_store_claims to authenticated;
revoke insert,update,delete on public.season_store_claims from anon,authenticated;

-- Keep the catalog constraint aligned with all visual-only Style kinds supported by equipment/trials.
alter table public.shop_items drop constraint if exists shop_items_kind_check;
alter table public.shop_items add constraint shop_items_kind_check check (kind in (
  'profile_frame','name_style','game_theme','keyboard_theme','victory_effect','emoji_pack','mascot',
  'avatar_background','nameplate','badge','title','vs_intro','word_effect','emote'
));

-- Seed the current season only when it exists. Existing competitive rewards remain untouched.
insert into public.season_store_rewards(season_id,level,track,reward_type,reward_key,amount,sort_order)
select s.id,v.level,v.track,v.reward_type,v.reward_key,v.amount,v.sort_order
from public.competitive_seasons s
cross join (values
  (1,'free','son_coin','',50,10),
  (1,'premium','son_coin','',100,20),
  (3,'free','son_coin','',75,30),
  (3,'premium','profile_frame','frame_asset_blue_season',0,40),
  (5,'free','son_coin','',100,50),
  (5,'premium','title','season_word_master',0,60),
  (7,'free','son_coin','',125,70),
  (7,'premium','badge','season_blue_badge',0,80),
  (10,'free','son_coin','',150,90),
  (10,'premium','nameplate','season_blue_nameplate',0,100),
  (13,'premium','word_effect','season_blue_word_fx',0,110),
  (16,'premium','victory_effect','season_blue_victory_fx',0,120),
  (20,'premium','vs_intro','season_blue_vs_intro',0,130),
  (25,'premium','final_style','season_blue_final_style',0,140)
) as v(level,track,reward_type,reward_key,amount,sort_order)
where s.starts_at<=now() and s.ends_at>now()
on conflict do nothing;

-- Catalog-only Style rows for the seeded season collection. They are not sold for SC.
insert into public.shop_items(id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order)
values
 ('frame_asset_blue_season','profile_frame','Sezon Mavi Çerçeve','Season Blue Frame','Sezon premium yolundan kazanılır.','Earned from the premium season track.',0,false,false,610),
 ('season_word_master','title','Sezon Kelime Ustası','Season Word Master','Sezon prestij unvanı.','Season prestige title.',0,false,false,620),
 ('season_blue_badge','badge','Sezon Mavi Rozet','Season Blue Badge','Sezon koleksiyon rozeti.','Season collection badge.',0,false,false,630),
 ('season_blue_nameplate','nameplate','Sezon Mavi Nameplate','Season Blue Nameplate','Sezon profil plakası.','Season profile nameplate.',0,false,false,640),
 ('season_blue_word_fx','word_effect','Sezon Kelime Efekti','Season Word Effect','Yalnız görsel kelime gönderme efekti.','Visual-only word send effect.',0,false,false,650),
 ('season_blue_victory_fx','victory_effect','Sezon Zafer Efekti','Season Victory Effect','Yalnız görsel zafer efekti.','Visual-only victory effect.',0,false,false,660),
 ('season_blue_vs_intro','vs_intro','Sezon VS Intro','Season VS Intro','Yalnız görsel VS girişi.','Visual-only VS intro.',0,false,false,670),
 ('season_blue_final_style','name_style','Sezon Final Style','Season Final Style','Sezon final koleksiyon ödülü.','Season finale collection reward.',0,false,false,680)
on conflict(id) do update set
 name_tr=excluded.name_tr,name_en=excluded.name_en,
 description_tr=excluded.description_tr,description_en=excluded.description_en,
 diamond_price=0,vip_only=false,active=false,sort_order=excluded.sort_order;

update public.shop_items set rarity='SEASON',collection_key='season_current'
where id in (
 'frame_asset_blue_season','season_word_master','season_blue_badge','season_blue_nameplate',
 'season_blue_word_fx','season_blue_victory_fx','season_blue_vs_intro','season_blue_final_style'
);

create or replace function public.get_store_season_v1()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_season public.competitive_seasons%rowtype;
  v_level integer:=1;
  v_premium boolean:=false;
  v_rewards jsonb:='[]'::jsonb;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_season from public.competitive_seasons
  where starts_at<=now() and ends_at>now()
  order by starts_at desc limit 1;
  if v_season.id is null then return jsonb_build_object('active',false,'rewards','[]'::jsonb); end if;
  if extract(day from (v_season.ends_at-v_season.starts_at)) not between 30 and 45 then
    raise exception 'invalid_season_duration';
  end if;

  -- Reuse the canonical level formula from get_growth_dashboard_v1. There is deliberately
  -- no parallel profiles.level column and this display progression never changes ranked power.
  select greatest(1,(
    (
      coalesce(p.wins,0)*120 +
      coalesce(p.losses,0)*35 +
      coalesce(p.valid_words,0)*3 +
      coalesce(p.total_rounds,0)*5
    ) / 500
  ) + 1)::int
  into v_level
  from public.profiles p
  where p.id=v_uid;

  select exists(
    select 1 from public.season_pass_entitlements e
    where e.user_id=v_uid and e.status in ('active','grace','canceled') and e.expires_at>now()
  ) into v_premium;

  select coalesce(jsonb_agg(jsonb_build_object(
    'level',r.level,'track',r.track,'reward_type',r.reward_type,'reward_key',r.reward_key,'amount',r.amount,
    'unlocked',v_level>=r.level,
    'premium_access',case when r.track='free' then true else v_premium end,
    'claimed',exists(select 1 from public.season_store_claims c
      where c.user_id=v_uid and c.season_id=r.season_id and c.level=r.level and c.track=r.track
        and c.reward_type=r.reward_type and c.reward_key=r.reward_key)
  ) order by r.level,r.sort_order),'[]'::jsonb)
  into v_rewards
  from public.season_store_rewards r where r.season_id=v_season.id;

  return jsonb_build_object(
    'active',true,'season_id',v_season.id,'starts_at',v_season.starts_at,'ends_at',v_season.ends_at,
    'duration_days',extract(day from (v_season.ends_at-v_season.starts_at))::int,
    'level',v_level,'premium_active',v_premium,'rewards',v_rewards
  );
end
$$;
revoke all on function public.get_store_season_v1() from public,anon;
grant execute on function public.get_store_season_v1() to authenticated;

create or replace function public.claim_store_season_reward_v1(
  p_level integer,p_track text,p_reward_type text,p_reward_key text default ''
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_season public.competitive_seasons%rowtype;
  v_reward public.season_store_rewards%rowtype;
  v_level integer:=1;
  v_premium boolean:=false;
  v_balance integer;
  v_item_kind text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_season from public.competitive_seasons
  where starts_at<=now() and ends_at>now() order by starts_at desc limit 1;
  if v_season.id is null then raise exception 'season_inactive'; end if;
  if extract(day from (v_season.ends_at-v_season.starts_at)) not between 30 and 45 then raise exception 'invalid_season_duration'; end if;

  select * into v_reward from public.season_store_rewards
  where season_id=v_season.id and level=p_level and track=lower(trim(p_track))
    and reward_type=p_reward_type and reward_key=coalesce(p_reward_key,'')
  limit 1;
  if v_reward.season_id is null then raise exception 'reward_not_found'; end if;

  -- Lock the profile row and use the same canonical level formula as the Growth dashboard.
  select greatest(1,(
    (
      coalesce(p.wins,0)*120 +
      coalesce(p.losses,0)*35 +
      coalesce(p.valid_words,0)*3 +
      coalesce(p.total_rounds,0)*5
    ) / 500
  ) + 1)::int
  into v_level
  from public.profiles p
  where p.id=v_uid
  for update;

  if v_level<v_reward.level then raise exception 'reward_locked'; end if;
  if v_reward.track='premium' then
    select exists(select 1 from public.season_pass_entitlements e
      where e.user_id=v_uid and e.status in ('active','grace','canceled') and e.expires_at>now()) into v_premium;
    if not v_premium then raise exception 'season_pass_required'; end if;
  end if;

  insert into public.season_store_claims(user_id,season_id,level,track,reward_type,reward_key)
  values(v_uid,v_season.id,v_reward.level,v_reward.track,v_reward.reward_type,v_reward.reward_key)
  on conflict do nothing;
  if not found then raise exception 'already_claimed'; end if;

  if v_reward.reward_type='son_coin' then
    update public.profiles set diamonds=coalesce(diamonds,0)+v_reward.amount,updated_at=now()
    where id=v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason)
    values(v_uid,v_reward.amount,'season_track:'||v_season.id||':'||v_reward.level||':'||v_reward.track);
  else
    select kind into v_item_kind from public.shop_items where id=v_reward.reward_key;
    if v_item_kind is null then raise exception 'season_style_missing'; end if;
    insert into public.user_inventory(user_id,item_id) values(v_uid,v_reward.reward_key)
    on conflict(user_id,item_id) do nothing;
  end if;

  return jsonb_build_object('success',true,'reward_type',v_reward.reward_type,'reward_key',v_reward.reward_key,'amount',v_reward.amount,'balance',v_balance);
end
$$;
revoke all on function public.claim_store_season_reward_v1(integer,text,text,text) from public,anon;
grant execute on function public.claim_store_season_reward_v1(integer,text,text,text) to authenticated;
