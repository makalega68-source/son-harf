-- Son Harf Store/VIP production hardening.
-- Backward-compatible: existing profiles.diamonds remains the displayed Son Coin balance.
-- Fair-play invariant: no table or RPC in this migration grants score/time/rating/move power.

-- -----------------------------------------------------------------------------
-- Remote store configuration (never stores or overrides localized Play prices).
-- -----------------------------------------------------------------------------
create table if not exists public.store_catalog_config (
  product_id text primary key,
  enabled boolean not null default true,
  badge_tr text,
  badge_en text,
  sort_order integer not null default 100,
  updated_at timestamptz not null default now()
);
alter table public.store_catalog_config enable row level security;
drop policy if exists store_catalog_config_read on public.store_catalog_config;
create policy store_catalog_config_read on public.store_catalog_config
for select to authenticated using (true);
grant select on public.store_catalog_config to authenticated;

insert into public.store_catalog_config(product_id,badge_tr,badge_en,sort_order) values
 ('vip_monthly',null,null,10),
 ('vip_yearly','EN AVANTAJLI','BEST VALUE',11),
 ('coins_500','MINI','MINI',20),
 ('coins_1500','EN POPÜLER','POPULAR',21),
 ('coins_3500','EN İYİ DEĞER','BEST VALUE',22),
 ('coins_8000','MEGA','MEGA',23),
 ('starter_style_pack',null,null,30),
 ('premium_style_pack',null,null,31),
 ('season_pack',null,null,32),
 ('vip_welcome_pack',null,null,33)
on conflict(product_id) do nothing;

-- -----------------------------------------------------------------------------
-- Generic server-authoritative entitlements and immutable Son Coin ledger.
-- Existing purchases/subscriptions remain source-compatible.
-- -----------------------------------------------------------------------------
create table if not exists public.store_entitlements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  entitlement_key text not null,
  source_type text not null check (source_type in ('play','season','vip','event','admin','legacy')),
  source_id text,
  status text not null default 'active' check (status in ('active','grace','hold','canceled','expired','revoked','pending')),
  starts_at timestamptz not null default now(),
  expires_at timestamptz,
  updated_at timestamptz not null default now(),
  unique(user_id,entitlement_key,source_type,source_id)
);
create index if not exists store_entitlements_user_active_idx
  on public.store_entitlements(user_id,status,expires_at);
alter table public.store_entitlements enable row level security;
drop policy if exists store_entitlements_read_own on public.store_entitlements;
create policy store_entitlements_read_own on public.store_entitlements
for select to authenticated using ((select auth.uid())=user_id);
grant select on public.store_entitlements to authenticated;

-- Strengthen purchase lifecycle without exposing tokens to clients.
alter table public.purchases add column if not exists purchase_type text;
alter table public.purchases add column if not exists play_state text;
alter table public.purchases add column if not exists acknowledgement_state text;
alter table public.purchases add column if not exists last_checked_at timestamptz;
alter table public.purchases add column if not exists expires_at timestamptz;
alter table public.purchases add column if not exists revoked_at timestamptz;
create index if not exists purchases_user_status_idx on public.purchases(user_id,status,created_at desc);

-- Explicit grants: purchase tokens/payment internals remain inaccessible to players.
revoke all on public.purchases from anon, authenticated;

-- Immutable append-only ledger. Existing name is retained for compatibility; it is Son Coin.
create unique index if not exists diamond_ledger_purchase_reason_unique
  on public.diamond_ledger(user_id,reason)
  where reason like 'google_play_purchase:%';

create or replace function public.prevent_son_coin_ledger_mutation_v1()
returns trigger language plpgsql set search_path=public,pg_temp as $$
begin
  raise exception 'son_coin_ledger_is_immutable';
end $$;

drop trigger if exists trg_son_coin_ledger_immutable_update on public.diamond_ledger;
create trigger trg_son_coin_ledger_immutable_update
before update or delete on public.diamond_ledger
for each row execute function public.prevent_son_coin_ledger_mutation_v1();

-- -----------------------------------------------------------------------------
-- Style metadata, secure trials, equipment extensions.
-- -----------------------------------------------------------------------------
alter table public.shop_items add column if not exists rarity text not null default 'STANDARD';
alter table public.shop_items add column if not exists preview_asset_key text;
alter table public.shop_items add column if not exists collection_key text;
alter table public.shop_items add column if not exists trial_mode text;
alter table public.shop_items add column if not exists trial_value integer;

alter table public.shop_items drop constraint if exists shop_items_rarity_check;
alter table public.shop_items add constraint shop_items_rarity_check
check (rarity in ('STANDARD','RARE','EPIC','LEGENDARY','SEASON','EVENT','VIP'));

alter table public.user_equipped_cosmetics add column if not exists avatar_background_id text;
alter table public.user_equipped_cosmetics add column if not exists nameplate_id text;
alter table public.user_equipped_cosmetics add column if not exists badge_id text;
alter table public.user_equipped_cosmetics add column if not exists title_style_id text;
alter table public.user_equipped_cosmetics add column if not exists vs_intro_id text;
alter table public.user_equipped_cosmetics add column if not exists word_effect_id text;
alter table public.user_equipped_cosmetics add column if not exists emote_id text;

create table if not exists public.style_trials (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  item_id text not null references public.shop_items(id) on delete cascade,
  mode text not null check (mode in ('match','minutes')),
  matches_remaining integer,
  expires_at timestamptz,
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  unique(user_id,item_id,started_at)
);
create index if not exists style_trials_active_idx on public.style_trials(user_id,ended_at,expires_at);
alter table public.style_trials enable row level security;
drop policy if exists style_trials_read_own on public.style_trials;
create policy style_trials_read_own on public.style_trials
for select to authenticated using ((select auth.uid())=user_id);
grant select on public.style_trials to authenticated;

-- -----------------------------------------------------------------------------
-- VIP social/privacy and post-match analysis storage.
-- -----------------------------------------------------------------------------
create table if not exists public.player_social_preferences (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  show_online_status boolean not null default false,
  show_last_seen boolean not null default false,
  allow_friend_invites boolean not null default true,
  allow_private_room_invites boolean not null default true,
  updated_at timestamptz not null default now()
);
alter table public.player_social_preferences enable row level security;
drop policy if exists social_preferences_read_own on public.player_social_preferences;
drop policy if exists social_preferences_write_own on public.player_social_preferences;
create policy social_preferences_read_own on public.player_social_preferences
for select to authenticated using ((select auth.uid())=user_id);
create policy social_preferences_write_own on public.player_social_preferences
for all to authenticated using ((select auth.uid())=user_id) with check ((select auth.uid())=user_id);
grant select,insert,update on public.player_social_preferences to authenticated;

create table if not exists public.player_relationship_marks (
  user_id uuid not null references public.profiles(id) on delete cascade,
  other_user_id uuid not null references public.profiles(id) on delete cascade,
  favorite boolean not null default false,
  arch_rival boolean not null default false,
  blocked boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key(user_id,other_user_id),
  check (user_id<>other_user_id)
);
alter table public.player_relationship_marks enable row level security;
drop policy if exists relationship_marks_own on public.player_relationship_marks;
create policy relationship_marks_own on public.player_relationship_marks
for all to authenticated using ((select auth.uid())=user_id) with check ((select auth.uid())=user_id);
grant select,insert,update,delete on public.player_relationship_marks to authenticated;

create table if not exists public.match_analysis_snapshots (
  match_id uuid not null,
  user_id uuid not null references public.profiles(id) on delete cascade,
  opponent_id uuid references public.profiles(id) on delete set null,
  mode text not null,
  completed_at timestamptz not null,
  best_word text,
  longest_word text,
  fastest_response_ms integer,
  slowest_response_ms integer,
  avg_response_ms integer,
  word_count integer not null default 0,
  avg_word_length numeric(6,2),
  highest_move_score integer,
  critical_time_responses integer not null default 0,
  territory_gained integer not null default 0,
  territory_lost integer not null default 0,
  turning_point jsonb,
  score_breakdown jsonb not null default '{}'::jsonb,
  primary key(match_id,user_id)
);
create index if not exists match_analysis_user_completed_idx
  on public.match_analysis_snapshots(user_id,completed_at desc);
alter table public.match_analysis_snapshots enable row level security;
drop policy if exists match_analysis_read_own on public.match_analysis_snapshots;
create policy match_analysis_read_own on public.match_analysis_snapshots
for select to authenticated using ((select auth.uid())=user_id);
grant select on public.match_analysis_snapshots to authenticated;

-- -----------------------------------------------------------------------------
-- Deterministic piggy bank / Kasa. Never subtracts normal match rewards.
-- -----------------------------------------------------------------------------
create table if not exists public.piggy_banks (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  tier integer not null default 0 check (tier between 0 and 4),
  bonus_sc integer not null default 0 check (bonus_sc in (0,200,400,600,800)),
  opened_at timestamptz,
  updated_at timestamptz not null default now()
);
alter table public.piggy_banks enable row level security;
drop policy if exists piggy_read_own on public.piggy_banks;
create policy piggy_read_own on public.piggy_banks
for select to authenticated using ((select auth.uid())=user_id);
grant select on public.piggy_banks to authenticated;

create or replace function public.open_piggy_bank_v1()
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid := auth.uid();
  v_bonus integer;
  v_balance integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  insert into public.piggy_banks(user_id) values(v_uid) on conflict(user_id) do nothing;
  select bonus_sc into v_bonus from public.piggy_banks where user_id=v_uid for update;
  if coalesce(v_bonus,0) not in (200,400,600,800) then raise exception 'piggy_not_ready'; end if;

  update public.profiles set diamonds=coalesce(diamonds,0)+v_bonus,updated_at=now()
  where id=v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason)
  values(v_uid,v_bonus,'piggy_open:'||extract(epoch from now())::bigint::text);
  update public.piggy_banks set tier=0,bonus_sc=0,opened_at=now(),updated_at=now() where user_id=v_uid;
  return jsonb_build_object('success',true,'bonus_sc',v_bonus,'balance',v_balance);
end $$;
revoke all on function public.open_piggy_bank_v1() from public,anon;
grant execute on function public.open_piggy_bank_v1() to authenticated;

-- Disable the legacy random chest economy while preserving old keys/data.
create or replace function public.open_reward_chest()
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  return jsonb_build_object('success',false,'disabled',true,'reason','replaced_by_deterministic_piggy');
end $$;

-- VIP no longer bypasses watching an ad to receive an ad-funded reward.
-- Existing v7 API remains callable for client compatibility but requires proof equally for VIP/free.
create or replace function public.claim_optional_reward_v7(p_reward_type text,p_ad_response_id text default null)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid := auth.uid();
  v_day date := (timezone('utc',now()))::date;
  v_used int;
  v_limit int;
  v_proof text := nullif(trim(p_ad_response_id),'');
  v_amount int;
  v_balance int;
  v_trial text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_reward_type not in ('diamonds','trial') then raise exception 'invalid_reward_type'; end if;
  if v_proof is null then raise exception 'missing_ad_proof'; end if;
  if exists(select 1 from public.rewarded_ad_claims where user_id=v_uid and ad_response_id=v_proof) then
    raise exception 'ad_already_claimed';
  end if;

  v_limit := case p_reward_type when 'diamonds' then 3 else 1 end;
  select count(*) into v_used from public.rewarded_ad_claims
  where user_id=v_uid and reward_date=v_day and reward_type=p_reward_type;
  if v_used>=v_limit then raise exception 'daily_limit_reached'; end if;

  if p_reward_type='diamonds' then
    v_amount := 10;
    update public.profiles set diamonds=coalesce(diamonds,0)+v_amount,updated_at=now()
      where id=v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason)
      values(v_uid,v_amount,'rewarded_ad:'||v_proof);
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,diamonds_awarded)
      values(v_uid,p_reward_type,v_proof,v_day,v_amount);
    return jsonb_build_object('success',true,'reward_type','diamonds','diamonds_awarded',v_amount,'diamonds',v_balance);
  end if;

  select id into v_trial from public.shop_items
  where active=true and trial_mode is not null
  order by sort_order,id limit 1;
  if v_trial is null then raise exception 'trial_item_unavailable'; end if;
  insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,trial_item_id)
    values(v_uid,p_reward_type,v_proof,v_day,v_trial);
  return jsonb_build_object('success',true,'reward_type','trial','trial_item_id',v_trial);
end $$;

-- Explicit fair-play VIP entitlement response; historical joker columns stay disabled.
create or replace function public.get_vip_entitlements_v7()
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  return jsonb_build_object(
    'is_vip',coalesce(v_vip,false),
    'daily_jokers_claimed',true,
    'freezer_count',0,'swap_count',0,'hint_count',0,'streak_shield_count',0,
    'xp_multiplier',1,'diamond_multiplier',1,'rewarded_ad_bypass',false,
    'used_words_access',true,'direct_messages_access',true,
    'ranked_live_assist',false,'post_match_analysis',coalesce(v_vip,false),
    'saved_friend_list',coalesce(v_vip,false),'private_rooms',coalesce(v_vip,false)
  );
end $$;

-- -----------------------------------------------------------------------------
-- Privacy-safe analytics event stream. No payment tokens / message text / PII.
-- -----------------------------------------------------------------------------
create table if not exists public.store_analytics_events (
  id bigint generated by default as identity primary key,
  user_id uuid references public.profiles(id) on delete set null,
  event_name text not null,
  product_id text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  check (event_name in (
    'store_view','product_view','preview_start','checkout_start','purchase_success',
    'purchase_cancel','purchase_failure','purchase_pending','restore','equip',
    'vip_start','vip_renew','vip_cancel','vip_expire','season_upgrade',
    'rewarded_ad_complete','piggy_open'
  ))
);
create index if not exists store_analytics_event_time_idx on public.store_analytics_events(event_name,created_at desc);
alter table public.store_analytics_events enable row level security;
revoke all on public.store_analytics_events from anon,authenticated;

create or replace function public.track_store_event_v1(p_event_name text,p_product_id text default null,p_metadata jsonb default '{}'::jsonb)
returns void language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_event_name not in (
    'store_view','product_view','preview_start','checkout_start','purchase_success',
    'purchase_cancel','purchase_failure','purchase_pending','restore','equip',
    'vip_start','vip_renew','vip_cancel','vip_expire','season_upgrade',
    'rewarded_ad_complete','piggy_open'
  ) then raise exception 'invalid_event'; end if;
  -- Strip common sensitive keys defensively.
  p_metadata := coalesce(p_metadata,'{}'::jsonb) - array['purchaseToken','purchase_token','orderId','order_id','email','phone','message'];
  insert into public.store_analytics_events(user_id,event_name,product_id,metadata)
  values(v_uid,p_event_name,nullif(trim(p_product_id),''),p_metadata);
end $$;
revoke all on function public.track_store_event_v1(text,text,jsonb) from public,anon;
grant execute on function public.track_store_event_v1(text,text,jsonb) to authenticated;

-- No legacy VIP XP advantage.
update public.profiles set vip_xp_bonus=0 where vip_xp_bonus<>0;
