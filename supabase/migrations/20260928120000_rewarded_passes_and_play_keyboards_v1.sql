-- Rewarded video passes and Google Play keyboards.
--
-- Every reward is granted only after AdMob's server-side verification (admob-ssv ->
-- fulfil_store_ad_v1 -> grant_store_ad_internal_v1), and every quota is counted here on the server.
--
--   key              reward                                   limit
--   keyboard_day     one premium keyboard for 24 hours        2 per 7 days
--   theme_day        one game theme for 24 hours              1 per 7 days
--   quick_games      5 Quick Games (series) within 3 days     1 per day, only without Quick Game access
--   hints_son_harf   +2 Son Harf hints (valid 7 days)         2 per day, only without a mascot
--   hints_siege      +2 Siege hints (valid 7 days)            2 per day, only without a mascot
--   hints_workshop   +2 Word Workshop hints (valid 7 days)    2 per day, only without a mascot
--   daily_double     today's daily login gift once more       1 per day, after claiming it
--
-- Premium White stays a Son Coin keyboard; the other keyboards become permanent Google Play
-- products (50 TL, price set in Play Console) whose product id equals the shop item id.

create table if not exists public.reward_passes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  reward_key text not null,
  item_id text,
  amount integer not null default 0,
  used integer not null default 0,
  expires_at timestamptz,
  ad_proof text not null,
  created_at timestamptz not null default now(),
  unique (user_id, ad_proof)
);
create index if not exists reward_passes_user_key_idx on public.reward_passes(user_id, reward_key, created_at desc);
alter table public.reward_passes enable row level security;
drop policy if exists reward_passes_own_read on public.reward_passes;
create policy reward_passes_own_read on public.reward_passes for select to authenticated using (user_id = auth.uid());
revoke insert, update, delete on public.reward_passes from anon, authenticated;

-- Ad intents carry the new reward types (keyboard/theme passes name their item).
alter table public.store_ad_intents drop constraint if exists store_ad_intents_reward_type_check;
alter table public.store_ad_intents add constraint store_ad_intents_reward_type_check check (reward_type = any (array[
  'diamonds','trial','keyboard_day','theme_day','quick_games','hints_son_harf','hints_siege','hints_workshop','daily_double'
]));
alter table public.store_ad_intents drop constraint if exists store_ad_intents_check;
alter table public.store_ad_intents add constraint store_ad_intents_check check (reward_type in ('trial','keyboard_day','theme_day') or trial_item_id is null);

-- The rule table for each rewarded video.
create or replace function public.reward_ad_rule_v1(p_key text, out max_claims integer, out period_days integer, out amount integer, out valid_hours integer)
language sql
immutable
set search_path = ''
as $$
  select r.max_claims, r.period_days, r.amount, r.valid_hours
  from (values
    ('keyboard_day',   2, 7, 0, 24),
    ('theme_day',      1, 7, 0, 24),
    ('quick_games',    1, 1, 5, 72),
    ('hints_son_harf', 2, 1, 2, 168),
    ('hints_siege',    2, 1, 2, 168),
    ('hints_workshop', 2, 1, 2, 168),
    ('daily_double',   1, 1, 0, 0)
  ) r(k, max_claims, period_days, amount, valid_hours)
  where r.k = p_key;
$$;

-- Quick Game access from a rewarded pass: the pass is alive and fewer than its games were started.
create or replace function public.has_series_ad_pass_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists(
    select 1 from public.reward_passes p
    where p.user_id = p_user_id
      and p.reward_key = 'quick_games'
      and p.expires_at > now()
      and (
        select count(*) from public.word_siege_games g
        where g.game_mode = 'series'
          and p_user_id in (g.player_one_id, g.player_two_id)
          and (g.created_at >= p.created_at or (g.player_two_id = p_user_id and g.turn_started_at >= p.created_at))
          and not (g.status = 'cancelled' and g.player_two_id is null)
      ) < p.amount
  );
$$;

-- Is this reward allowed right now? Raises the reason when it is not.
create or replace function public.reward_pass_check_v1(p_uid uuid, p_key text, p_item text)
returns void
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  r record;
  v_used integer;
begin
  select * into r from public.reward_ad_rule_v1(p_key);
  if r.max_claims is null then raise exception 'invalid_reward_type'; end if;
  if r.period_days = 1 then
    select count(*) into v_used from public.reward_passes
     where user_id = p_uid and reward_key = p_key
       and (timezone('utc', created_at))::date = (timezone('utc', now()))::date;
    if v_used >= r.max_claims then raise exception 'daily_limit_reached'; end if;
  else
    select count(*) into v_used from public.reward_passes
     where user_id = p_uid and reward_key = p_key
       and created_at > now() - make_interval(days => r.period_days);
    if v_used >= r.max_claims then raise exception 'weekly_limit_reached'; end if;
  end if;

  if p_key in ('keyboard_day', 'theme_day') then
    if not exists(
      select 1 from public.shop_items s
      where s.id = p_item and s.active
        and s.kind = case p_key when 'keyboard_day' then 'keyboard_theme' else 'game_theme' end
    ) then raise exception 'trial_item_unavailable'; end if;
    if exists(select 1 from public.user_inventory where user_id = p_uid and item_id = p_item) then raise exception 'already_owned'; end if;
  elsif p_key like 'hints\_%' escape '\' then
    -- Mascot owners already get three hints every match.
    if exists(
      select 1 from public.store_entitlements e
      where e.user_id = p_uid and e.status = 'active'
        and e.entitlement_key like 'mascot\_%' escape '\'
        and (e.expires_at is null or e.expires_at > now())
    ) then raise exception 'mascot_owner'; end if;
  elsif p_key = 'quick_games' then
    if public.has_permanent_entitlement_v1(p_uid, 'series_game') or public.has_pro_access_v1(p_uid) then raise exception 'already_owned'; end if;
    if public.has_series_ad_pass_v1(p_uid) then raise exception 'pass_active'; end if;
  elsif p_key = 'daily_double' then
    if not exists(select 1 from public.daily_checkins where user_id = p_uid and checkin_date = current_date) then
      raise exception 'daily_not_claimed';
    end if;
  end if;
end $$;

-- Grants a verified rewarded video (called inside grant_store_ad_internal_v1 as the player).
create or replace function public.grant_reward_pass_internal_v1(p_key text, p_proof text, p_item text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  r record;
  v_amount integer;
  v_balance integer;
  v_pass public.reward_passes%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if nullif(trim(p_proof), '') is null then raise exception 'missing_ad_proof'; end if;
  if exists(select 1 from public.reward_passes where user_id = v_uid and ad_proof = p_proof) then raise exception 'ad_already_claimed'; end if;
  perform 1 from public.profiles where id = v_uid for update;
  perform public.reward_pass_check_v1(v_uid, p_key, p_item);
  select * into r from public.reward_ad_rule_v1(p_key);
  v_amount := r.amount;

  if p_key = 'daily_double' then
    select reward_diamonds into v_amount from public.daily_checkins where user_id = v_uid and checkin_date = current_date;
    v_amount := coalesce(v_amount, 40);
    update public.profiles set diamonds = coalesce(diamonds, 0) + v_amount, updated_at = now() where id = v_uid returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id, delta, reason) values(v_uid, v_amount, 'rewarded_ad_daily_double:' || p_proof);
  end if;

  insert into public.reward_passes(user_id, reward_key, item_id, amount, expires_at, ad_proof)
  values (
    v_uid, p_key,
    case when p_key in ('keyboard_day', 'theme_day') then p_item end,
    v_amount,
    case when r.valid_hours > 0 then now() + make_interval(hours => r.valid_hours) end,
    p_proof
  )
  returning * into v_pass;

  return jsonb_build_object(
    'success', true,
    'reward_type', p_key,
    'trial_item_id', v_pass.item_id,
    'amount', v_pass.amount,
    'trial_expires_at', v_pass.expires_at,
    'diamonds_awarded', case when p_key = 'daily_double' then v_amount else 0 end,
    'diamonds', v_balance
  );
end $$;

-- Everything the app needs to show and apply the player's rewarded passes.
create or replace function public.get_reward_passes_v1()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_keyboard jsonb;
  v_theme jsonb;
  v_usage jsonb;
  v_games_left integer := 0;
  v_games_expire timestamptz;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select jsonb_build_object('item_id', item_id, 'expires_at', expires_at) into v_keyboard
    from public.reward_passes
   where user_id = v_uid and reward_key = 'keyboard_day' and expires_at > now()
   order by created_at desc limit 1;
  select jsonb_build_object('item_id', item_id, 'expires_at', expires_at) into v_theme
    from public.reward_passes
   where user_id = v_uid and reward_key = 'theme_day' and expires_at > now()
   order by created_at desc limit 1;

  select greatest(0, p.amount - (
           select count(*) from public.word_siege_games g
           where g.game_mode = 'series'
             and v_uid in (g.player_one_id, g.player_two_id)
             and (g.created_at >= p.created_at or (g.player_two_id = v_uid and g.turn_started_at >= p.created_at))
             and not (g.status = 'cancelled' and g.player_two_id is null)
         )::integer),
         p.expires_at
    into v_games_left, v_games_expire
    from public.reward_passes p
   where p.user_id = v_uid and p.reward_key = 'quick_games' and p.expires_at > now()
   order by p.created_at desc limit 1;

  select coalesce(jsonb_agg(jsonb_build_object(
           'key', k.key,
           'max', rule.max_claims,
           'period_days', rule.period_days,
           'amount', rule.amount,
           'used', (
             select count(*) from public.reward_passes p
             where p.user_id = v_uid and p.reward_key = k.key
               and case when rule.period_days = 1
                        then (timezone('utc', p.created_at))::date = (timezone('utc', now()))::date
                        else p.created_at > now() - make_interval(days => rule.period_days) end
           )
         )), '[]'::jsonb)
    into v_usage
    from unnest(array['keyboard_day','theme_day','quick_games','hints_son_harf','hints_siege','hints_workshop','daily_double']) k(key)
    cross join lateral public.reward_ad_rule_v1(k.key) rule;

  return jsonb_build_object(
    'hints_son_harf', (select coalesce(sum(amount - used), 0) from public.reward_passes where user_id = v_uid and reward_key = 'hints_son_harf' and expires_at > now()),
    'hints_siege', (select coalesce(sum(amount - used), 0) from public.reward_passes where user_id = v_uid and reward_key = 'hints_siege' and expires_at > now()),
    'hints_workshop', (select coalesce(sum(amount - used), 0) from public.reward_passes where user_id = v_uid and reward_key = 'hints_workshop' and expires_at > now()),
    'keyboard', v_keyboard,
    'theme', v_theme,
    'quick_games_left', coalesce(v_games_left, 0),
    'quick_games_expires_at', v_games_expire,
    'usage', v_usage
  );
end $$;

-- Spends one rewarded hint for a game ('son_harf', 'siege', 'kelime_atolyesi'); returns what is left.
create or replace function public.use_reward_hint_v1(p_game text)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_key text := case p_game
    when 'son_harf' then 'hints_son_harf'
    when 'siege' then 'hints_siege'
    when 'kelime_atolyesi' then 'hints_workshop'
    when 'workshop' then 'hints_workshop'
  end;
  v_id uuid;
  v_left integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if v_key is null then raise exception 'invalid_game'; end if;
  select id into v_id from public.reward_passes
   where user_id = v_uid and reward_key = v_key and expires_at > now() and used < amount
   order by expires_at, created_at limit 1 for update;
  if v_id is null then raise exception 'no_reward_hints'; end if;
  update public.reward_passes set used = used + 1 where id = v_id;
  select coalesce(sum(amount - used), 0) into v_left from public.reward_passes
   where user_id = v_uid and reward_key = v_key and expires_at > now();
  return v_left;
end $$;

-- Rewarded-video intent: the old coin/trial types keep their rules, the new ones use the table above.
create or replace function public.prepare_store_ad_v1(p_reward_type text, p_trial_item_id text default null)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare v_uid uuid := auth.uid(); v_id uuid; v_limit integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if not exists(select 1 from public.store_monetization_config where id and rewarded_enabled and cardinality(allowed_rewarded_ad_units) > 0) then
    raise exception 'rewarded_ads_unavailable';
  end if;
  perform 1 from public.profiles where id = v_uid for update;
  if p_reward_type in ('diamonds', 'trial') then
    if p_reward_type = 'trial' and not exists(select 1 from public.shop_items where id = p_trial_item_id and active and trial_mode in ('match','minutes') and trial_value > 0) then
      raise exception 'trial_item_unavailable';
    end if;
    v_limit := case when p_reward_type = 'diamonds' then 3 else 1 end;
    if (select count(*) from public.rewarded_ad_claims where user_id = v_uid and reward_date = (timezone('utc', now()))::date and reward_type = p_reward_type) >= v_limit then
      raise exception 'daily_limit_reached';
    end if;
  else
    perform public.reward_pass_check_v1(v_uid, p_reward_type, p_trial_item_id);
  end if;
  if (select count(*) from public.store_ad_intents where user_id = v_uid and created_at > now() - interval '24 hours') >= 30 then
    raise exception 'daily_limit_reached';
  end if;
  insert into public.store_ad_intents(user_id, reward_type, trial_item_id)
  values (v_uid, p_reward_type, case when p_reward_type in ('trial', 'keyboard_day', 'theme_day') then p_trial_item_id end)
  returning id into v_id;
  return v_id;
end $$;

create or replace function public.grant_store_ad_internal_v1(p_reward_type text, p_ad_response_id text, p_trial_item_id text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
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
 if v_proof is null then raise exception 'missing_ad_proof'; end if;
 if p_reward_type not in ('diamonds','trial') then
  return public.grant_reward_pass_internal_v1(p_reward_type, v_proof, p_trial_item_id);
 end if;
 if exists(select 1 from public.rewarded_ad_claims where user_id=v_uid and ad_response_id=v_proof) then raise exception 'ad_already_claimed'; end if;
 select count(*) into v_used from public.rewarded_ad_claims where user_id=v_uid and reward_date=v_day and reward_type=p_reward_type;
 if v_used >= (case when p_reward_type='diamonds' then 3 else 1 end) then raise exception 'daily_limit_reached'; end if;
 if p_reward_type='diamonds' then
  update public.profiles set diamonds=coalesce(diamonds,0)+10,updated_at=now() where id=v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,10,'rewarded_ad:'||v_proof);
  insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,diamonds_awarded) values(v_uid,'diamonds',v_proof,v_day,10);
  return jsonb_build_object('success',true,'reward_type','diamonds','diamonds_awarded',10,'diamonds',v_balance);
 end if;
 select * into v_item from public.shop_items where id=nullif(trim(p_trial_item_id),'') and active=true and trial_mode in ('match','minutes') and coalesce(trial_value,0)>0;
 if not found then raise exception 'trial_item_unavailable'; end if;
 if v_item.trial_mode='minutes' then
  v_expires := now() + make_interval(mins=>least(v_item.trial_value,30));
  v_matches := null;
 else
  v_expires := null;
  v_matches := least(v_item.trial_value,1);
 end if;
 update public.style_trials set ended_at=now() where user_id=v_uid and ended_at is null;
 insert into public.style_trials(user_id,item_id,mode,matches_remaining,expires_at) values(v_uid,v_item.id,v_item.trial_mode,v_matches,v_expires);
 insert into public.rewarded_ad_claims(user_id,reward_type,ad_response_id,reward_date,trial_item_id) values(v_uid,'trial',v_proof,v_day,v_item.id);
 return jsonb_build_object('success',true,'reward_type','trial','trial_item_id',v_item.id,'trial_mode',v_item.trial_mode,'trial_matches_remaining',v_matches,'trial_expires_at',v_expires);
end
$$;

-- Quick Game access now also comes from a rewarded pass.
create or replace function public.has_series_game_access_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select public.has_permanent_entitlement_v1(p_user_id,'series_game')
      or public.has_pro_access_v1(p_user_id)
      or public.has_series_ad_pass_v1(p_user_id)
$$;

create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
  profile_vip boolean := false;
  pro boolean := false;
  claimed boolean := false;
  w public.vip_joker_wallet;
  score_access boolean := false;
  letter_access boolean := false;
  series_access boolean := false;
begin
  if u is null then
    raise exception 'unauthorized';
  end if;

  select coalesce(is_vip, false)
    into profile_vip
    from public.profiles
   where id = u;

  pro := profile_vip or public.has_permanent_entitlement_v1(u, 'pro_lifetime');
  score_access := pro or public.has_permanent_entitlement_v1(u, 'score_calculator');
  letter_access := pro or public.has_permanent_entitlement_v1(u, 'letter_table');
  series_access := pro or public.has_permanent_entitlement_v1(u, 'series_game') or public.has_series_ad_pass_v1(u);

  insert into public.vip_joker_wallet(user_id)
  values (u)
  on conflict (user_id) do nothing;

  select *
    into w
    from public.vip_joker_wallet
   where user_id = u;

  select exists(
    select 1
      from public.vip_daily_joker_claims
     where user_id = u
       and claim_date = current_date
  ) into claimed;

  return jsonb_build_object(
    'is_vip', profile_vip,
    'is_pro', pro,
    'daily_jokers_claimed', claimed,
    'freezer_count', coalesce(w.freezer_count, 0),
    'swap_count', coalesce(w.swap_count, 0),
    'hint_count', coalesce(w.hint_count, 0),
    'streak_shield_count', coalesce(w.streak_shield_count, 0),
    'multiplier_count', coalesce(w.multiplier_count, 0),
    'xp_multiplier', 1,
    'diamond_multiplier', 1,
    'rewarded_ad_bypass', pro,
    'used_words_access', pro,
    'direct_messages_access', true,
    'ranked_live_assist', false,
    'post_match_analysis', pro,
    'saved_friend_list', pro,
    'private_rooms', pro,
    'score_calculator_access', score_access,
    'letter_table_access', letter_access,
    'series_game_access', series_access,
    'active_game_limit', case when pro then 50 else 10 end
  );
end
$$;

-- Keyboards: Premium White stays on Son Coin; the others are Google Play products (50 TL).
update public.shop_items
   set metadata = coalesce(metadata, '{}'::jsonb) || jsonb_build_object('play_product_id', id, 'list_price_try', '50 TL')
 where id in ('keyboard_black_gold', 'keyboard_crystal', 'keyboard_midnight', 'keyboard_obsidian');

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  v_uid uuid:=auth.uid();
  v_item public.shop_items%rowtype;
  v_balance integer;
  v_vip boolean;
  v_admin_free boolean:=false;
  v_owner_unlimited boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select * into v_item
  from public.shop_items
  where id=p_item_id
    and active=true
    and available_from<=now()
    and (available_until is null or available_until>now());

  if not found then raise exception 'item_not_found'; end if;

  if not public.is_runtime_supported_shop_item_v1(v_item.id,v_item.kind) then
    raise exception 'item_runtime_unavailable';
  end if;

  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;

  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;

  select coalesce(a.free_test_purchases,false) into v_admin_free
  from public.admin_users a where a.user_id=v_uid;
  v_admin_free:=coalesce(v_admin_free,false) and public.is_admin();

  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=v_uid and o.active and o.unlimited_diamonds
  ) into v_owner_unlimited;

  -- Google Play products are never sold for Son Coin (test accounts excepted).
  if coalesce(v_item.metadata->>'play_product_id','')<>'' and not v_admin_free and not v_owner_unlimited then
    raise exception 'play_only';
  end if;

  if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;

  if not v_admin_free and not v_owner_unlimited then
    if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_item.diamond_price,updated_at=now() where id=v_uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  end if;

  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);

  if v_admin_free then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'test_free_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  elsif v_owner_unlimited then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'owner_unlimited_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  end if;

  return jsonb_build_object(
    'success',true,
    'item_id',p_item_id,
    'diamonds',case when v_admin_free or v_owner_unlimited then v_balance else v_balance-v_item.diamond_price end,
    'admin_test_free',v_admin_free,
    'owner_unlimited',v_owner_unlimited
  );
end
$$;

create or replace function public.apply_verified_premium_purchase_v1(p_user_id uuid, p_product_id text, p_purchase_token text, p_order_id text default null, p_play_state text default null, p_acknowledgement_state text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_purchase_id uuid;
  v_purchase_user_id uuid;
  v_purchase_product_id text;
  v_inserted boolean := false;
  v_key text;
  v_balance integer;
  -- Frames and keyboards are cosmetics: the grant puts the item in the inventory.
  v_frame boolean := p_product_id in (
    'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
    'profile_frame_pink_blossom','profile_frame_blue_royal',
    'keyboard_black_gold','keyboard_crystal','keyboard_midnight','keyboard_obsidian'
  );
begin
  if p_user_id is null or nullif(trim(p_purchase_token),'') is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if p_product_id not in (
    'series_game','letter_table','score_calculator','pro_lifetime',
    'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik',
    'mascot_tekir','mascot_robot','mascot_astronot'
  ) and not v_frame then
    raise exception 'unsupported_product';
  end if;
  if not exists(select 1 from public.profiles where id = p_user_id) then
    raise exception 'profile_not_found';
  end if;

  v_key := case p_product_id
    when 'series_game' then 'series_game'
    when 'letter_table' then 'letter_table'
    when 'score_calculator' then 'score_calculator'
    when 'pro_lifetime' then 'pro_lifetime'
    else case when p_product_id like 'mascot\_%' escape '\' or v_frame then p_product_id end
  end;
  if v_key is null then
    raise exception 'unsupported_product';
  end if;

  insert into public.purchases(
    user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at,
    purchase_type,play_state,acknowledgement_state,last_checked_at,expires_at
  ) values (
    p_user_id,p_product_id,trim(p_purchase_token),nullif(trim(p_order_id),''),'verified',now(),now(),
    'one_time',p_play_state,p_acknowledgement_state,now(),null
  )
  on conflict(purchase_token) do nothing
  returning id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id;
  v_inserted := found;

  if not v_inserted then
    select id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id
    from public.purchases
    where purchase_token = trim(p_purchase_token)
    for update;
    if v_purchase_id is null then raise exception 'purchase_reconciliation_race'; end if;
    if v_purchase_user_id <> p_user_id then raise exception 'purchase_token_user_mismatch'; end if;
    if v_purchase_product_id <> p_product_id then raise exception 'purchase_token_product_mismatch'; end if;
  end if;

  update public.purchases
  set order_id = coalesce(nullif(trim(p_order_id),''),order_id),
      status = 'verified',
      play_state = p_play_state,
      acknowledgement_state = coalesce(p_acknowledgement_state,acknowledgement_state),
      last_checked_at = now()
  where id = v_purchase_id;

  insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
  values(p_user_id,v_key,'play',trim(p_purchase_token),'active',null,now())
  on conflict(user_id,entitlement_key,source_type,source_id)
  do update set status='active',expires_at=null,updated_at=now();

  if v_frame then
    insert into public.user_inventory(user_id,item_id)
    select p_user_id,p_product_id
    where not exists(select 1 from public.user_inventory where user_id=p_user_id and item_id=p_product_id);
  end if;

  if p_product_id = 'pro_lifetime' then
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
    select p_user_id,k,'pro_bundle',trim(p_purchase_token),'active',null,now()
    from unnest(array['series_game','letter_table','score_calculator']) k
    on conflict(user_id,entitlement_key,source_type,source_id)
    do update set status='active',expires_at=null,updated_at=now();

    update public.profiles
    set is_vip=true,updated_at=now()
    where id=p_user_id;

    if v_inserted then
      update public.profiles
      set diamonds=coalesce(diamonds,0)+100,updated_at=now()
      where id=p_user_id
      returning diamonds into v_balance;
      insert into public.diamond_ledger(user_id,delta,reason)
      values(p_user_id,100,'google_play_pro_lifetime:'||trim(p_purchase_token));
    end if;
  end if;

  if v_balance is null then select diamonds into v_balance from public.profiles where id=p_user_id; end if;
  return jsonb_build_object(
    'success',true,
    'already_processed',not v_inserted,
    'purchase_id',v_purchase_id,
    'product_id',p_product_id,
    'entitlement_key',v_key,
    'son_coin_granted',case when p_product_id='pro_lifetime' and v_inserted then 100 else 0 end,
    'son_coin_balance',v_balance
  );
end
$$;

revoke all on function public.reward_ad_rule_v1(text) from public, anon;
revoke all on function public.reward_pass_check_v1(uuid, text, text) from public, anon, authenticated;
revoke all on function public.grant_reward_pass_internal_v1(text, text, text) from public, anon, authenticated;
revoke all on function public.has_series_ad_pass_v1(uuid) from public, anon, authenticated;
revoke all on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) from public, anon, authenticated;
grant execute on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) to service_role;
revoke all on function public.get_reward_passes_v1() from public, anon;
grant execute on function public.get_reward_passes_v1() to authenticated;
revoke all on function public.use_reward_hint_v1(text) from public, anon;
grant execute on function public.use_reward_hint_v1(text) to authenticated;
