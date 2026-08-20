-- SON HARF optional rewarded-ad center
-- Rewards are intentionally non-pay-to-win: diamonds, reward chests and temporary cosmetics only.

create table if not exists public.reward_wallet (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  chest_keys integer not null default 0 check (chest_keys >= 0),
  trial_item_id text references public.shop_items(id) on delete set null,
  trial_expires_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.rewarded_ad_claims (
  id bigserial primary key,
  user_id uuid not null references public.profiles(id) on delete cascade,
  reward_type text not null check (reward_type in ('diamonds','chest','trial')),
  ad_response_id text not null,
  reward_date date not null default (timezone('utc', now()))::date,
  diamonds_awarded integer not null default 0,
  chest_keys_awarded integer not null default 0,
  trial_item_id text references public.shop_items(id) on delete set null,
  created_at timestamptz not null default now(),
  unique(user_id, ad_response_id)
);
create index if not exists idx_rewarded_ad_claims_daily on public.rewarded_ad_claims(user_id,reward_date,reward_type);

alter table public.reward_wallet enable row level security;
alter table public.rewarded_ad_claims enable row level security;

drop policy if exists reward_wallet_read_own on public.reward_wallet;
create policy reward_wallet_read_own on public.reward_wallet for select to authenticated using (user_id=auth.uid());
drop policy if exists rewarded_claims_read_own on public.rewarded_ad_claims;
create policy rewarded_claims_read_own on public.rewarded_ad_claims for select to authenticated using (user_id=auth.uid());

grant select on public.reward_wallet to authenticated;
grant select on public.rewarded_ad_claims to authenticated;

create or replace function public.get_reward_center_status()
returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid:=auth.uid();
  v_day date:=(timezone('utc',now()))::date;
  v_diamond_count int;
  v_chest_count int;
  v_trial_count int;
  v_wallet public.reward_wallet%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  insert into public.reward_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  select count(*) filter(where reward_type='diamonds'),
         count(*) filter(where reward_type='chest'),
         count(*) filter(where reward_type='trial')
    into v_diamond_count,v_chest_count,v_trial_count
  from public.rewarded_ad_claims where user_id=v_uid and reward_date=v_day;
  select * into v_wallet from public.reward_wallet where user_id=v_uid;
  if v_wallet.trial_expires_at is not null and v_wallet.trial_expires_at <= now() then
    update public.reward_wallet set trial_item_id=null,trial_expires_at=null,updated_at=now() where user_id=v_uid;
    v_wallet.trial_item_id:=null; v_wallet.trial_expires_at:=null;
  end if;
  return jsonb_build_object(
    'diamond_ads_used',coalesce(v_diamond_count,0),'diamond_ads_limit',3,'diamond_per_ad',10,
    'chest_ads_used',coalesce(v_chest_count,0),'chest_ads_limit',2,
    'trial_ads_used',coalesce(v_trial_count,0),'trial_ads_limit',1,
    'chest_keys',coalesce(v_wallet.chest_keys,0),
    'trial_item_id',v_wallet.trial_item_id,
    'trial_expires_at',v_wallet.trial_expires_at
  );
end $$;

create or replace function public.claim_rewarded_ad(p_reward_type text,p_ad_response_id text)
returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid:=auth.uid();
  v_day date:=(timezone('utc',now()))::date;
  v_used int;
  v_limit int;
  v_trial text;
  v_balance int;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_reward_type not in ('diamonds','chest','trial') then raise exception 'invalid_reward_type'; end if;
  if nullif(trim(p_ad_response_id),'') is null then raise exception 'missing_ad_proof'; end if;
  if exists(select 1 from public.rewarded_ad_claims where user_id=v_uid and ad_response_id=p_ad_response_id) then raise exception 'ad_already_claimed'; end if;
  v_limit:=case p_reward_type when 'diamonds' then 3 when 'chest' then 2 else 1 end;
  select count(*) into v_used from public.rewarded_ad_claims where user_id=v_uid and reward_date=v_day and reward_type=p_reward_type;
  if v_used>=v_limit then raise exception 'daily_limit_reached'; end if;
  insert into public.reward_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;

  if p_reward_type='diamonds' then
    update public.profiles set diamonds=diamonds+10 where id=v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,10,'rewarded_ad');
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,diamonds_awarded)
      values(v_uid,p_reward_type,p_ad_response_id,v_day,10);
    return jsonb_build_object('success',true,'reward_type','diamonds','diamonds_awarded',10,'diamonds',v_balance);
  elsif p_reward_type='chest' then
    update public.reward_wallet set chest_keys=chest_keys+1,updated_at=now() where user_id=v_uid;
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,chest_keys_awarded)
      values(v_uid,p_reward_type,p_ad_response_id,v_day,1);
    return jsonb_build_object('success',true,'reward_type','chest','chest_keys_awarded',1);
  else
    select id into v_trial from public.shop_items where active=true and vip_only=true order by random() limit 1;
    if v_trial is null then raise exception 'trial_item_unavailable'; end if;
    update public.reward_wallet set trial_item_id=v_trial,trial_expires_at=now()+interval '24 hours',updated_at=now() where user_id=v_uid;
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,trial_item_id)
      values(v_uid,p_reward_type,p_ad_response_id,v_day,v_trial);
    return jsonb_build_object('success',true,'reward_type','trial','trial_item_id',v_trial,'trial_expires_at',now()+interval '24 hours');
  end if;
end $$;

create or replace function public.open_reward_chest()
returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid:=auth.uid();
  v_keys int;
  v_roll float8:=random();
  v_reward int;
  v_balance int;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  insert into public.reward_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  select chest_keys into v_keys from public.reward_wallet where user_id=v_uid for update;
  if coalesce(v_keys,0)<=0 then raise exception 'no_chest_key'; end if;
  v_reward:=case when v_roll<0.60 then 15 when v_roll<0.90 then 25 else 40 end;
  update public.reward_wallet set chest_keys=chest_keys-1,updated_at=now() where user_id=v_uid;
  update public.profiles set diamonds=diamonds+v_reward where id=v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_reward,'reward_chest');
  return jsonb_build_object('success',true,'diamonds_awarded',v_reward,'diamonds',v_balance,'chest_keys',v_keys-1);
end $$;

revoke all on function public.get_reward_center_status() from public;
revoke all on function public.claim_rewarded_ad(text,text) from public;
revoke all on function public.open_reward_chest() from public;
grant execute on function public.get_reward_center_status() to authenticated;
grant execute on function public.claim_rewarded_ad(text,text) to authenticated;
grant execute on function public.open_reward_chest() to authenticated;
