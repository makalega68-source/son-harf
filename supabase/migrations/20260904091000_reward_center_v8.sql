-- Reward Center v8: deterministic Kasa + secure Style trials.
-- This migration removes legacy random chest/rewarded-ad paths from the client-facing API.

alter table public.piggy_banks add column if not exists baseline_match_count integer not null default 0;
alter table public.piggy_banks add column if not exists last_match_count integer not null default 0;

-- Existing random chest keys are historical data only. No new key can be earned/opened.
revoke execute on function public.open_reward_chest() from authenticated;
revoke execute on function public.claim_rewarded_ad(text,text) from authenticated;
revoke execute on function public.claim_optional_reward_v7(text,text) from authenticated;

-- Seed explicit, non-random trial modes on already-active visual products.
update public.shop_items set trial_mode='minutes',trial_value=30
where id='frame_asset_gold' and active=true;
update public.shop_items set trial_mode='match',trial_value=1
where id='theme_monster_blue' and active=true;

create or replace function public.completed_store_match_count_v1(p_uid uuid)
returns integer
language sql
security invoker
set search_path=''
stable
as $$
  select (
    (select count(*) from public.game_rooms g
      where g.status='finished' and g.stats_applied and (g.host_id=p_uid or g.guest_id=p_uid))
    +
    (select count(*) from public.word_arena_rooms a
      where a.status='finished' and a.result_applied and (a.host_id=p_uid or a.guest_id=p_uid))
  )::integer
$$;
revoke all on function public.completed_store_match_count_v1(uuid) from public,anon;
grant execute on function public.completed_store_match_count_v1(uuid) to authenticated;

create or replace function public.get_store_reward_status_v1()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_day date := (timezone('utc',now()))::date;
  v_ad_count integer := 0;
  v_trial_count integer := 0;
  v_match_count integer := 0;
  v_baseline integer := 0;
  v_delta integer := 0;
  v_tier integer := 0;
  v_bonus integer := 0;
  v_trial public.style_trials%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select count(*) filter(where reward_type='diamonds'),
         count(*) filter(where reward_type='trial')
    into v_ad_count,v_trial_count
  from public.rewarded_ad_claims
  where user_id=v_uid and reward_date=v_day;

  v_match_count := public.completed_store_match_count_v1(v_uid);
  insert into public.piggy_banks(user_id,baseline_match_count,last_match_count)
  values(v_uid,v_match_count,v_match_count)
  on conflict(user_id) do nothing;

  select baseline_match_count into v_baseline
  from public.piggy_banks where user_id=v_uid for update;
  v_delta := greatest(0,v_match_count-v_baseline);
  v_tier := case
    when v_delta>=8 then 4
    when v_delta>=6 then 3
    when v_delta>=4 then 2
    when v_delta>=2 then 1
    else 0
  end;
  v_bonus := case v_tier when 1 then 200 when 2 then 400 when 3 then 600 when 4 then 800 else 0 end;
  update public.piggy_banks
  set tier=v_tier,bonus_sc=v_bonus,last_match_count=v_match_count,updated_at=now()
  where user_id=v_uid;

  select * into v_trial
  from public.style_trials
  where user_id=v_uid and ended_at is null
    and (expires_at is null or expires_at>now())
    and (matches_remaining is null or matches_remaining>0)
  order by started_at desc limit 1;

  return jsonb_build_object(
    'coin_ads_used',coalesce(v_ad_count,0),
    'coin_ads_limit',3,
    'coin_per_ad',10,
    'trial_ads_used',coalesce(v_trial_count,0),
    'trial_ads_limit',1,
    'trial_item_id',v_trial.item_id,
    'trial_mode',v_trial.mode,
    'trial_matches_remaining',v_trial.matches_remaining,
    'trial_expires_at',v_trial.expires_at,
    'piggy_tier',v_tier,
    'piggy_bonus_sc',v_bonus,
    'piggy_match_progress',least(v_delta,8),
    'piggy_match_target',8
  );
end
$$;
revoke all on function public.get_store_reward_status_v1() from public,anon;
grant execute on function public.get_store_reward_status_v1() to authenticated;

create or replace function public.claim_store_rewarded_ad_v1(
  p_reward_type text,
  p_ad_response_id text,
  p_trial_item_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_day date := (timezone('utc',now()))::date;
  v_proof text := nullif(trim(p_ad_response_id),'');
  v_used integer := 0;
  v_balance integer := 0;
  v_item public.shop_items%rowtype;
  v_expires timestamptz;
  v_matches integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_reward_type not in ('diamonds','trial') then raise exception 'invalid_reward_type'; end if;
  if v_proof is null then raise exception 'missing_ad_proof'; end if;
  if exists(select 1 from public.rewarded_ad_claims where user_id=v_uid and ad_response_id=v_proof) then
    raise exception 'ad_already_claimed';
  end if;

  select count(*) into v_used from public.rewarded_ad_claims
  where user_id=v_uid and reward_date=v_day and reward_type=p_reward_type;
  if v_used >= case when p_reward_type='diamonds' then 3 else 1 end then
    raise exception 'daily_limit_reached';
  end if;

  if p_reward_type='diamonds' then
    update public.profiles set diamonds=coalesce(diamonds,0)+10,updated_at=now()
    where id=v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason)
    values(v_uid,10,'rewarded_ad:'||v_proof);
    insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,diamonds_awarded)
    values(v_uid,'diamonds',v_proof,v_day,10);
    return jsonb_build_object('success',true,'reward_type','diamonds','diamonds_awarded',10,'diamonds',v_balance);
  end if;

  select * into v_item from public.shop_items
  where id=nullif(trim(p_trial_item_id),'') and active=true and trial_mode in ('match','minutes')
    and coalesce(trial_value,0)>0;
  if not found then raise exception 'trial_item_unavailable'; end if;

  if v_item.trial_mode='minutes' then
    v_expires := now() + make_interval(mins=>least(v_item.trial_value,30));
    v_matches := null;
  else
    v_expires := null;
    v_matches := least(v_item.trial_value,1);
  end if;

  update public.style_trials set ended_at=now()
  where user_id=v_uid and ended_at is null;
  insert into public.style_trials(user_id,item_id,mode,matches_remaining,expires_at)
  values(v_uid,v_item.id,v_item.trial_mode,v_matches,v_expires);
  insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,trial_item_id)
  values(v_uid,'trial',v_proof,v_day,v_item.id);

  return jsonb_build_object(
    'success',true,'reward_type','trial','trial_item_id',v_item.id,
    'trial_mode',v_item.trial_mode,'trial_matches_remaining',v_matches,'trial_expires_at',v_expires
  );
end
$$;
revoke all on function public.claim_store_rewarded_ad_v1(text,text,text) from public,anon;
grant execute on function public.claim_store_rewarded_ad_v1(text,text,text) to authenticated;

create or replace function public.equip_style_trial_v1()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_trial public.style_trials%rowtype;
  v_item public.shop_items%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_trial from public.style_trials
  where user_id=v_uid and ended_at is null
    and (expires_at is null or expires_at>now())
    and (matches_remaining is null or matches_remaining>0)
  order by started_at desc limit 1 for update;
  if not found then raise exception 'trial_not_active'; end if;
  select * into v_item from public.shop_items where id=v_trial.item_id and active=true;
  if not found then raise exception 'trial_item_unavailable'; end if;

  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics set
    profile_frame_id=case when v_item.kind='profile_frame' then v_item.id else profile_frame_id end,
    name_style_id=case when v_item.kind='name_style' then v_item.id else name_style_id end,
    game_theme_id=case when v_item.kind='game_theme' then v_item.id else game_theme_id end,
    keyboard_theme_id=case when v_item.kind='keyboard_theme' then v_item.id else keyboard_theme_id end,
    victory_effect_id=case when v_item.kind='victory_effect' then v_item.id else victory_effect_id end,
    avatar_background_id=case when v_item.kind='avatar_background' then v_item.id else avatar_background_id end,
    nameplate_id=case when v_item.kind='nameplate' then v_item.id else nameplate_id end,
    badge_id=case when v_item.kind='badge' then v_item.id else badge_id end,
    title_style_id=case when v_item.kind='title' then v_item.id else title_style_id end,
    vs_intro_id=case when v_item.kind='vs_intro' then v_item.id else vs_intro_id end,
    word_effect_id=case when v_item.kind='word_effect' then v_item.id else word_effect_id end,
    updated_at=now()
  where user_id=v_uid;
  return jsonb_build_object('success',true,'item_id',v_item.id,'kind',v_item.kind,'expires_at',v_trial.expires_at,'matches_remaining',v_trial.matches_remaining);
end
$$;
revoke all on function public.equip_style_trial_v1() from public,anon;
grant execute on function public.equip_style_trial_v1() to authenticated;

create or replace function public.open_piggy_bank_v2()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_status jsonb;
  v_bonus integer;
  v_match_count integer;
  v_balance integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  v_status := public.get_store_reward_status_v1();
  v_bonus := coalesce((v_status->>'piggy_bonus_sc')::integer,0);
  if v_bonus not in (200,400,600,800) then raise exception 'piggy_not_ready'; end if;
  v_match_count := public.completed_store_match_count_v1(v_uid);

  update public.profiles set diamonds=coalesce(diamonds,0)+v_bonus,updated_at=now()
  where id=v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason)
  values(v_uid,v_bonus,'piggy_open:'||gen_random_uuid()::text);
  update public.piggy_banks
  set tier=0,bonus_sc=0,baseline_match_count=v_match_count,last_match_count=v_match_count,opened_at=now(),updated_at=now()
  where user_id=v_uid;
  return jsonb_build_object('success',true,'bonus_sc',v_bonus,'balance',v_balance);
end
$$;
revoke all on function public.open_piggy_bank_v1() from authenticated;
revoke all on function public.open_piggy_bank_v2() from public,anon;
grant execute on function public.open_piggy_bank_v2() to authenticated;

-- Harden table grants: RLS remains the row boundary; grants remain the operation boundary.
revoke all on public.piggy_banks from anon;
revoke insert,update,delete on public.piggy_banks from authenticated;
grant select on public.piggy_banks to authenticated;
revoke all on public.style_trials from anon;
revoke insert,update,delete on public.style_trials from authenticated;
grant select on public.style_trials to authenticated;
