create table if not exists public.store_bundles (
 id text primary key, name_tr text not null, name_en text not null,
 diamond_price integer not null check (diamond_price>0),
 section text not null check (section in ('starter','limited')),
 item_ids text[] not null check (cardinality(item_ids) between 2 and 4),
 active boolean not null default true, available_from timestamptz not null default now(), available_until timestamptz,
 check (available_until is null or available_until>available_from)
);
create table if not exists public.store_bundle_purchases (
 user_id uuid not null references public.profiles(id) on delete cascade,
 bundle_id text not null references public.store_bundles(id),
 purchased_at timestamptz not null default now(),price_paid integer not null,primary key(user_id,bundle_id)
);
create table if not exists public.store_monetization_config (
 id boolean primary key default true check(id),rewarded_enabled boolean not null default false,
 allowed_rewarded_ad_units text[] not null default '{}'
);
insert into public.store_monetization_config(id) values(true) on conflict do nothing;
create table if not exists public.store_ad_intents (
 id uuid primary key default gen_random_uuid(),user_id uuid not null references public.profiles(id) on delete cascade,
 reward_type text not null check(reward_type in ('diamonds','trial')),trial_item_id text references public.shop_items(id),
 created_at timestamptz not null default now(),expires_at timestamptz not null default now()+interval '30 minutes',
 transaction_id text unique,result jsonb,check(reward_type='trial' or trial_item_id is null)
);
create index if not exists store_ad_intents_user_created on public.store_ad_intents(user_id,created_at);
alter table public.store_bundles enable row level security;
alter table public.store_bundle_purchases enable row level security;
alter table public.store_monetization_config enable row level security;
alter table public.store_ad_intents enable row level security;
revoke all on public.store_bundles,public.store_bundle_purchases,public.store_monetization_config,public.store_ad_intents from public,anon,authenticated;
grant all on public.store_bundles,public.store_bundle_purchases,public.store_monetization_config,public.store_ad_intents to service_role;
grant select on public.store_bundle_purchases to authenticated;
create policy "Own bundle purchases" on public.store_bundle_purchases for select to authenticated using ((select auth.uid())=user_id);

insert into public.store_bundles(id,name_tr,name_en,diamond_price,section,item_ids,available_until) values
 ('starter_collection_v1','Başlangıç Paketi','Starter Bundle',240,'starter',array['frame_asset_red','name_cyan'],null),
 ('quiet_light_collection_v1','Sakin Işıltı Koleksiyonu','Quiet Light Collection',400,'limited',array['frame_asset_mint','frame_asset_purple'],now()+interval '14 days')
on conflict(id) do nothing;

create or replace function public.get_storefront_v1() returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_uid uuid:=auth.uid();v_vip boolean;v_bundles jsonb;v_ads boolean;
begin
 if v_uid is null then raise exception 'unauthorized';end if;
 select is_vip into v_vip from public.profiles where id=v_uid;
 select rewarded_enabled and cardinality(allowed_rewarded_ad_units)>0 into v_ads from public.store_monetization_config where id;
 select coalesce(jsonb_agg(jsonb_build_object(
 'id',b.id,'name_tr',b.name_tr,'name_en',b.name_en,'diamond_price',b.diamond_price,'section',b.section,'available_until',b.available_until,
 'owned',exists(select 1 from public.store_bundle_purchases p where p.user_id=v_uid and p.bundle_id=b.id),
 'items',(select jsonb_agg(to_jsonb(s) order by array_position(b.item_ids,s.id)) from public.shop_items s where s.id=any(b.item_ids))
 ) order by b.available_from,b.id),'[]'::jsonb) into v_bundles
 from public.store_bundles b where b.active and b.available_from<=now() and (b.available_until is null or b.available_until>now())
 and cardinality(b.item_ids)=(select count(*) from public.shop_items s where s.id=any(b.item_ids) and s.active and s.available_from<=now() and (s.available_until is null or s.available_until>now()) and s.kind in ('profile_frame','name_style','keyboard_theme','game_theme'));
 return jsonb_build_object('daily_claimed',exists(select 1 from public.daily_checkins where user_id=v_uid and checkin_date=current_date),
 'daily_reward',case when coalesce(v_vip,false) then 80 else 40 end,'rewarded_enabled',coalesce(v_ads,false),'bundles',v_bundles);
end $$;

create or replace function public.purchase_store_bundle_v1(p_bundle_id text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_uid uuid:=auth.uid();v_bundle public.store_bundles%rowtype;v_balance integer;v_vip boolean;
begin
 if v_uid is null then raise exception 'unauthorized';end if;
 select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;
 if not found then raise exception 'unauthorized';end if;
 select * into v_bundle from public.store_bundles where id=p_bundle_id and active and available_from<=now() and (available_until is null or available_until>now()) for share;
 if not found then raise exception 'bundle_unavailable';end if;
 if exists(select 1 from public.store_bundle_purchases where user_id=v_uid and bundle_id=p_bundle_id) then return jsonb_build_object('success',true,'already_owned',true);end if;
 perform 1 from public.shop_items where id=any(v_bundle.item_ids) for share;
 if cardinality(v_bundle.item_ids)<>(select count(*) from public.shop_items where id=any(v_bundle.item_ids) and active and available_from<=now() and (available_until is null or available_until>now()) and kind in ('profile_frame','name_style','keyboard_theme','game_theme')) then raise exception 'bundle_unavailable';end if;
 if exists(select 1 from public.shop_items where id=any(v_bundle.item_ids) and vip_only) and not coalesce(v_vip,false) then raise exception 'vip_required';end if;
 if not exists(select 1 from unnest(v_bundle.item_ids) i where not exists(select 1 from public.user_inventory where user_id=v_uid and item_id=i)) then raise exception 'bundle_already_owned';end if;
 if coalesce(v_balance,0)<v_bundle.diamond_price then raise exception 'insufficient_diamonds';end if;
 insert into public.store_bundle_purchases(user_id,bundle_id,price_paid) values(v_uid,p_bundle_id,v_bundle.diamond_price);
 update public.profiles set diamonds=diamonds-v_bundle.diamond_price,updated_at=now() where id=v_uid;
 insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,-v_bundle.diamond_price,'store_bundle:'||p_bundle_id);
 insert into public.user_inventory(user_id,item_id) select v_uid,i from unnest(v_bundle.item_ids) i on conflict(user_id,item_id) do nothing;
 return jsonb_build_object('success',true,'diamonds',v_balance-v_bundle.diamond_price);
end $$;

-- Only the signed callback service can reach the existing grant implementation.
alter function public.claim_store_rewarded_ad_v1(text,text,text) rename to grant_store_ad_internal_v1;
revoke all on function public.grant_store_ad_internal_v1(text,text,text) from public,anon,authenticated;
grant execute on function public.grant_store_ad_internal_v1(text,text,text) to service_role;
revoke all on function public.claim_rewarded_ad(text,text) from public,anon,authenticated;

create or replace function public.prepare_store_ad_v1(p_reward_type text,p_trial_item_id text default null) returns uuid
language plpgsql security definer set search_path='' as $$
declare v_uid uuid:=auth.uid();v_id uuid;v_limit integer;
begin
 if v_uid is null then raise exception 'unauthorized';end if;
 if not exists(select 1 from public.store_monetization_config where id and rewarded_enabled and cardinality(allowed_rewarded_ad_units)>0) then raise exception 'rewarded_ads_unavailable';end if;
 if p_reward_type not in ('diamonds','trial') then raise exception 'invalid_reward_type';end if;
 if p_reward_type='trial' and not exists(select 1 from public.shop_items where id=p_trial_item_id and active and trial_mode in ('match','minutes') and trial_value>0) then raise exception 'trial_item_unavailable';end if;
 perform 1 from public.profiles where id=v_uid for update;
 v_limit:=case when p_reward_type='diamonds' then 3 else 1 end;
 if (select count(*) from public.rewarded_ad_claims where user_id=v_uid and reward_date=(timezone('utc',now()))::date and reward_type=p_reward_type)>=v_limit then raise exception 'daily_limit_reached';end if;
 if (select count(*) from public.store_ad_intents where user_id=v_uid and created_at>now()-interval '24 hours')>=20 then raise exception 'daily_limit_reached';end if;
 insert into public.store_ad_intents(user_id,reward_type,trial_item_id) values(v_uid,p_reward_type,case when p_reward_type='trial' then p_trial_item_id else null end) returning id into v_id;
 return v_id;
end $$;

create or replace function public.fulfil_store_ad_v1(p_intent_id uuid,p_transaction_id text,p_user_id uuid,p_ad_unit text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_intent public.store_ad_intents%rowtype;v_result jsonb;v_claims text:=current_setting('request.jwt.claims',true);v_sub text:=current_setting('request.jwt.claim.sub',true);
begin
 if coalesce(auth.jwt()->>'role','')<>'service_role' then raise exception 'service_only';end if;
 if not exists(select 1 from public.store_monetization_config where id and rewarded_enabled and p_ad_unit=any(allowed_rewarded_ad_units)) then raise exception 'ad_unit_not_allowed';end if;
 if p_transaction_id is null or length(p_transaction_id) not between 8 and 256 then raise exception 'invalid_transaction';end if;
 select * into v_intent from public.store_ad_intents where id=p_intent_id and user_id=p_user_id for update;
 if not found then raise exception 'invalid_ad_intent';end if;
 if v_intent.result is not null then
  if v_intent.transaction_id<>p_transaction_id then raise exception 'intent_already_used';end if;
  return v_intent.result;
 end if;
 if v_intent.expires_at<now() then raise exception 'ad_intent_expired';end if;
 perform 1 from public.profiles where id=p_user_id for update;
 update public.store_ad_intents set transaction_id=p_transaction_id where id=p_intent_id;
 perform set_config('request.jwt.claims',jsonb_build_object('sub',p_user_id,'role','authenticated')::text,true);
 perform set_config('request.jwt.claim.sub',p_user_id::text,true);
 v_result:=public.grant_store_ad_internal_v1(v_intent.reward_type,'ssv:'||p_transaction_id,v_intent.trial_item_id);
 perform set_config('request.jwt.claims',coalesce(v_claims,''),true);
 perform set_config('request.jwt.claim.sub',coalesce(v_sub,''),true);
 update public.store_ad_intents set result=v_result where id=p_intent_id;
 return v_result;
end $$;

create or replace function public.claim_store_rewarded_ad_v1(p_reward_type text,p_ad_response_id text,p_trial_item_id text default null) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_uid uuid:=auth.uid();v_result jsonb;
begin
 if v_uid is null then raise exception 'unauthorized';end if;
 select result into v_result from public.store_ad_intents where id::text=p_ad_response_id and user_id=v_uid and reward_type=p_reward_type and trial_item_id is not distinct from p_trial_item_id;
 if v_result is null then raise exception 'ad_verification_pending';end if;
 return v_result;
end $$;

revoke all on function public.get_storefront_v1(),public.purchase_store_bundle_v1(text),public.prepare_store_ad_v1(text,text),public.claim_store_rewarded_ad_v1(text,text,text),public.fulfil_store_ad_v1(uuid,text,uuid,text) from public,anon,authenticated;
grant execute on function public.get_storefront_v1(),public.purchase_store_bundle_v1(text),public.prepare_store_ad_v1(text,text),public.claim_store_rewarded_ad_v1(text,text,text) to authenticated;
grant execute on function public.fulfil_store_ad_v1(uuid,text,uuid,text) to service_role;
