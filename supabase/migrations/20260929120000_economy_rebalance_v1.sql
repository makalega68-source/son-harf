-- Son Coin economy rebalance (see docs/ekonomi-denge-onerisi.md).
-- Goal: a normal active free player earns ~80-100 SC a day; small cosmetics in days, valuable ones in
-- weeks/months, legendary/prestige items also need real achievements that bought coins cannot skip.
--  * Nobody's balance or inventory is touched. Only future earning rates and prices change.
--  * Every grant stays server-side, idempotent per day/week/claim, and is written to diamond_ledger.
--  * PRO keeps comfort/looks; its 2x coin multipliers are removed and the monthly grant is 150.

-- 1. Rarity ladder for collection value. STANDARD = common; PRESTIGE is new.
alter table public.shop_items drop constraint if exists shop_items_rarity_check;
alter table public.shop_items add constraint shop_items_rarity_check
  check (rarity = any (array['STANDARD','RARE','EPIC','LEGENDARY','PRESTIGE','SEASON','EVENT','VIP']));

-- 2. Prices. Target days assume ~85 SC/day for a normal active player.
update public.shop_items s set diamond_price = v.price, rarity = v.rarity,
  metadata = coalesce(s.metadata, '{}'::jsonb) || v.meta
from (values
  ('frame_round_starter_blue',    150, 'STANDARD',  '{"economy_tier":"starter"}'::jsonb),
  ('frame_round_starter_neutral', 150, 'STANDARD',  '{"economy_tier":"starter"}'::jsonb),
  ('frame_round_pearl',           250, 'STANDARD',  '{"economy_tier":"starter"}'::jsonb),
  ('frame_round_botanic',         550, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('frame_round_ocean',           550, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('frame_round_rose',            550, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('frame_round_lilac',           550, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('keyboard_premium_white',      500, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('keyboard_midnight',           600, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('keyboard_black_gold',        1500, 'RARE',      '{"economy_tier":"rare"}'::jsonb),
  ('keyboard_crystal',           1700, 'RARE',      '{"economy_tier":"rare"}'::jsonb),
  ('keyboard_obsidian',          3600, 'EPIC',      '{"economy_tier":"epic"}'::jsonb),
  ('name_cyan',                   500, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('name_sapphire',               650, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('name_amethyst',              1500, 'RARE',      '{"economy_tier":"rare"}'::jsonb),
  ('name_aurelia',               1900, 'RARE',      '{"economy_tier":"rare"}'::jsonb),
  ('hat_beret',                   650, 'STANDARD',  '{"economy_tier":"common"}'::jsonb),
  ('hat_flower',                 1600, 'RARE',      '{"economy_tier":"rare"}'::jsonb),
  ('hat_wizard',                 3800, 'EPIC',      '{"economy_tier":"epic"}'::jsonb),
  ('hat_top',                    4200, 'EPIC',      '{"economy_tier":"epic"}'::jsonb),
  ('theme_walnut_ivory',         4000, 'EPIC',      '{"economy_tier":"epic"}'::jsonb),
  ('theme_black',                4000, 'EPIC',      '{"economy_tier":"epic"}'::jsonb),
  ('victory_crown',              8000, 'LEGENDARY', '{"economy_tier":"legendary","requirements":{"min_wins":50}}'::jsonb)
) as v(id, price, rarity, meta)
where s.id = v.id;

-- 3. Achievement requirements (coins alone, bought or earned, never unlock them).
--    metadata.requirements: {"min_wins":int, "min_rating":int, "min_matches":int, "season_honor":text}
create or replace function public.shop_item_requirement_gap_v1(p_uid uuid, p_requirements jsonb)
returns text
language plpgsql
stable
security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_wins int; v_rating int; v_matches int;
begin
  if p_requirements is null or jsonb_typeof(p_requirements) <> 'object' then return null; end if;
  select coalesce(wins,0), coalesce(rating,1000), coalesce(total_matches, coalesce(wins,0)+coalesce(losses,0), 0)
  into v_wins, v_rating, v_matches from public.profiles where id = p_uid;
  if (p_requirements ? 'min_wins') and v_wins < (p_requirements->>'min_wins')::int then return 'wins'; end if;
  if (p_requirements ? 'min_rating') and v_rating < (p_requirements->>'min_rating')::int then return 'rating'; end if;
  if (p_requirements ? 'min_matches') and v_matches < (p_requirements->>'min_matches')::int then return 'matches'; end if;
  if (p_requirements ? 'season_honor') and not exists (
    select 1 from public.season_honors h where h.user_id = p_uid and h.honor_code = p_requirements->>'season_honor'
  ) then return 'season_honor'; end if;
  return null;
end $$;
revoke all on function public.shop_item_requirement_gap_v1(uuid, jsonb) from public, anon, authenticated;

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid(); v_item public.shop_items%rowtype; v_balance integer; v_vip boolean;
  v_admin_free boolean := false; v_owner_unlimited boolean := false; v_gap text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items where id=p_item_id and active=true and available_from<=now() and (available_until is null or available_until>now());
  if not found then raise exception 'item_not_found'; end if;
  if not public.is_runtime_supported_shop_item_v1(v_item.id,v_item.kind) then raise exception 'item_runtime_unavailable'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;
  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;
  select coalesce(a.free_test_purchases,false) into v_admin_free from public.admin_users a where a.user_id=v_uid;
  v_admin_free := coalesce(v_admin_free,false) and public.is_admin();
  select exists(select 1 from public.owner_game_accounts o where o.user_id=v_uid and o.active and o.unlimited_diamonds) into v_owner_unlimited;
  if coalesce(v_item.metadata->>'play_product_id','')<>'' and not v_admin_free and not v_owner_unlimited then raise exception 'play_only'; end if;
  if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
  -- Achievement requirements apply to every player; only admin test accounts may skip them.
  if not v_admin_free and not v_owner_unlimited then
    v_gap := public.shop_item_requirement_gap_v1(v_uid, v_item.metadata->'requirements');
    if v_gap is not null then raise exception 'requirement_not_met:%', v_gap; end if;
  end if;
  if not v_admin_free and not v_owner_unlimited then
    if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_item.diamond_price,updated_at=now() where id=v_uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id) values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  end if;
  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);
  if v_admin_free then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data) values(v_uid,'test_free_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  elsif v_owner_unlimited then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data) values(v_uid,'owner_unlimited_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  end if;
  return jsonb_build_object('success',true,'item_id',p_item_id,
    'diamonds',case when v_admin_free or v_owner_unlimited then v_balance else v_balance-v_item.diamond_price end,
    'admin_test_free',v_admin_free,'owner_unlimited',v_owner_unlimited);
end $$;

-- 4. Daily check-in: a 7-day streak 5,5,10,10,15,15,20 (avg ~11/day). No PRO multiplier.
create or replace function public.siege_daily_cycle_rewards_v1()
returns integer[] language sql immutable set search_path to '' as $$ select array[5, 5, 10, 10, 15, 15, 20] $$;

create or replace function public.daily_checkin_streak_before_today_v1(p_uid uuid)
returns integer language plpgsql stable security definer set search_path to 'public', 'pg_temp' as $$
declare v_streak int := 0; v_day date := current_date - 1;
begin
  while exists(select 1 from public.daily_checkins c where c.user_id = p_uid and c.checkin_date = v_day) loop
    v_streak := v_streak + 1; v_day := v_day - 1;
    exit when v_streak >= 365;
  end loop;
  return v_streak;
end $$;
revoke all on function public.daily_checkin_streak_before_today_v1(uuid) from public, anon, authenticated;

create or replace function public.claim_daily_checkin_v1()
returns integer language plpgsql security definer set search_path to 'public', 'pg_temp' as $$
declare
  v_uid uuid := auth.uid(); v_reward integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  v_reward := (public.siege_daily_cycle_rewards_v1())[(public.daily_checkin_streak_before_today_v1(v_uid) % 7) + 1];
  insert into public.daily_checkins(user_id,checkin_date,reward_diamonds) values(v_uid,current_date,v_reward)
  on conflict (user_id,checkin_date) do nothing;
  if not found then return 0; end if;
  update public.profiles set diamonds=coalesce(diamonds,0)+v_reward,updated_at=now() where id=v_uid;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_reward,'daily_checkin');
  return v_reward;
end $$;

create or replace function public.get_daily_reward_cycle_v1()
returns table(cycle_day integer, streak integer, claimed_today boolean, today_reward integer, rewards integer[], vip boolean)
language plpgsql stable security definer set search_path to '' as $$
declare v_uid uuid := auth.uid(); v_vip boolean := false; v_claimed boolean := false; v_streak integer := 0;
  v_rewards integer[] := public.siege_daily_cycle_rewards_v1(); v_cycle integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select coalesce(p.is_vip, false) into v_vip from public.profiles p where p.id = v_uid;
  select exists(select 1 from public.daily_checkins c where c.user_id = v_uid and c.checkin_date = current_date) into v_claimed;
  v_streak := public.daily_checkin_streak_before_today_v1(v_uid);
  v_cycle := (v_streak % 7) + 1;
  return query select v_cycle, v_streak + case when v_claimed then 1 else 0 end, v_claimed, v_rewards[v_cycle], v_rewards, v_vip;
end $$;

-- 5. Daily challenge (3 finished matches): 15 SC, private rooms do not count, written to the ledger.
create or replace function public.claim_daily_challenge_v1()
returns integer language plpgsql security definer set search_path to 'public' as $$
declare v_uid uuid := auth.uid(); v_reward integer := 15; v_matches integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select count(*) into v_matches from public.game_rooms
  where status='finished' and coalesce(finished_at,created_at)::date=current_date and (host_id=v_uid or guest_id=v_uid)
    and coalesce(room_mode,'') <> 'private' and coalesce(room_type,'') <> 'private';
  select v_matches + count(*) into v_matches from public.word_siege_games
  where status='finished' and coalesce(finished_at,updated_at)::date=current_date and v_uid in (player_one_id, player_two_id);
  if v_matches < 3 then raise exception 'daily_challenge_incomplete'; end if;
  insert into public.daily_challenge_claims(user_id,challenge_date,reward_diamonds) values(v_uid,current_date,v_reward)
  on conflict (user_id,challenge_date) do nothing;
  if not found then return 0; end if;
  update public.profiles set diamonds = coalesce(diamonds,0) + v_reward, updated_at=now() where id=v_uid;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_reward,'daily_challenge');
  return v_reward;
end $$;

-- 6. Daily/weekly route missions: only modes that exist in the app today
--    (Son Harf duel, Kelime Kuşatması, Kelime Atölyesi daily race, Kelime Avı puzzle).
create or replace function public.get_unified_missions_v1()
returns table(mission_id text, scope text, period_start date, title_tr text, title_en text, mode_key text, target integer, progress integer, reward_coins integer, completed boolean, claimed boolean, route_order integer)
language plpgsql security definer set search_path to '' as $$
declare
  v_uid uuid := auth.uid();
  v_now_local timestamp := timezone('Europe/Istanbul', clock_timestamp());
  v_today date := v_now_local::date;
  v_week date := date_trunc('week', v_now_local)::date;
  v_today_ts timestamptz := (date_trunc('day', v_now_local) at time zone 'Europe/Istanbul');
  v_week_ts timestamptz := (date_trunc('week', v_now_local) at time zone 'Europe/Istanbul');
  v_day integer := extract(isodow from v_now_local)::integer;
  d_duel int := 0; d_siege int := 0; d_atelier int := 0; d_cipher int := 0; d_feature int := 0;
  w_duel int := 0; w_siege int := 0; w_atelier int := 0; w_cipher int := 0; w_done int := 0;
  v_feature_key text; v_feature_tr text; v_feature_en text;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select count(*) filter (where coalesce(g.finished_at,g.created_at) >= v_today_ts)::int, count(*)::int into d_duel, w_duel
  from public.game_rooms g
  where g.status='finished' and coalesce(g.finished_at,g.created_at) >= v_week_ts and (g.host_id=v_uid or g.guest_id=v_uid)
    and (coalesce(g.valid_word_count,0)>0 or g.winner_id is not null);
  select count(*) filter (where coalesce(s.finished_at,s.updated_at) >= v_today_ts)::int, count(*)::int into d_siege, w_siege
  from public.word_siege_games s
  where s.status='finished' and coalesce(s.finished_at,s.updated_at) >= v_week_ts and v_uid in (s.player_one_id, s.player_two_id);
  select count(*) filter (where a.day = v_today)::int, count(*)::int into d_atelier, w_atelier
  from public.atelier_daily_runs a where a.user_id=v_uid and a.finished_at is not null and a.day >= v_week;
  select count(*) filter (where c.challenge_date = v_today)::int, count(*)::int into d_cipher, w_cipher
  from public.daily_cipher_sessions c where c.user_id=v_uid and c.finished and c.challenge_date >= v_week;
  w_done := (case when w_duel>=3 then 1 else 0 end) + (case when w_siege>=2 then 1 else 0 end)
          + (case when w_atelier>=1 then 1 else 0 end) + (case when w_cipher>=1 then 1 else 0 end);
  case (v_day % 3)
    when 1 then v_feature_key := 'word_siege'; v_feature_tr := 'Kelime Kuşatması: 1 maç bitir'; v_feature_en := 'Word Siege: finish 1 match'; d_feature := d_siege;
    when 2 then v_feature_key := 'atelier'; v_feature_tr := 'Kelime Atölyesi: günün yarışını tamamla'; v_feature_en := 'Word Atelier: finish today''s race'; d_feature := d_atelier;
    else v_feature_key := 'daily_cipher'; v_feature_tr := 'Kelime Avı: bugünün bulmacasını tamamla'; v_feature_en := 'Word Hunt: finish today''s puzzle'; d_feature := d_cipher;
  end case;
  return query
  with mission_rows as (
    select 'daily_duel'::text id, 'daily'::text sc, v_today ps, 'Bugünün Düellosu: 1 maç tamamla'::text tr, 'Today''s Duel: finish 1 match'::text en, 'duel'::text mk, 1::int tgt, least(d_duel,1)::int prog, 3::int reward, 10::int ord
    union all select 'daily_featured','daily',v_today,v_feature_tr,v_feature_en,v_feature_key,1,least(d_feature,1),5,20
    union all select 'daily_route','daily',v_today,'Günlük Rota: iki görevi de bitir','Daily Route: finish both tasks','route',2,(least(d_duel,1)+least(d_feature,1))::int,8,30
    union all select 'weekly_duel','weekly',v_week,'Düello Ustası: 3 maç tamamla','Duel Master: finish 3 matches','duel',3,least(w_duel,3),5,100
    union all select 'weekly_siege','weekly',v_week,'Kuşatma Komutanı: 2 maç bitir','Siege Commander: finish 2 matches','word_siege',2,least(w_siege,2),5,110
    union all select 'weekly_atelier','weekly',v_week,'Atölye: 1 günlük yarış','Atelier: 1 daily race','atelier',1,least(w_atelier,1),5,120
    union all select 'weekly_cipher','weekly',v_week,'Kelime Avı: 1 günlük bulmaca','Word Hunt: 1 daily puzzle','daily_cipher',1,least(w_cipher,1),5,130
    union all select 'weekly_route','weekly',v_week,'Haftalık Büyük Rota: dört görevi de bitir','Weekly Grand Route: finish all four','route',4,w_done,15,190
  )
  select m.id,m.sc,m.ps,m.tr,m.en,m.mk,m.tgt,m.prog,m.reward,(m.prog>=m.tgt),
    exists(select 1 from public.unified_mission_claims c where c.user_id=v_uid and c.mission_id=m.id and c.period_start=m.ps),
    m.ord
  from mission_rows m order by m.ord;
end $$;

-- 7. Kelime Avı: 10 SC once a day, whichever language is solved first.
do $$
declare v_def text;
begin
  select pg_get_functiondef('public.submit_daily_cipher_guess_v1(text,text)'::regprocedure) into v_def;
  if position('if v_won and v_reward=0 then v_new_reward := 35;' in regexp_replace(v_def, '\s+', ' ', 'g')) = 0 then
    raise exception 'cipher reward block not found; refusing to patch blindly';
  end if;
  v_def := regexp_replace(v_def,
    'if v_won and v_reward\s*=\s*0 then\s+v_new_reward := 35;',
    'if v_won and v_reward=0 and not exists (select 1 from public.daily_cipher_sessions o where o.user_id=v_uid and o.challenge_date=current_date and o.language<>v_lang and o.reward_coins>0) then v_new_reward := 10;');
  execute v_def;
end $$;

-- 8. Rewarded ads: 3 a day, 5 SC each (~15% of a normal day).
do $$
declare v_def text;
begin
  select pg_get_functiondef('public.grant_store_ad_internal_v1(text,text,text)'::regprocedure) into v_def;
  v_def := replace(v_def, 'diamonds=coalesce(diamonds,0)+10', 'diamonds=coalesce(diamonds,0)+5');
  v_def := replace(v_def, 'values(v_uid,10,''rewarded_ad:''||v_proof)', 'values(v_uid,5,''rewarded_ad:''||v_proof)');
  v_def := replace(v_def, 'values(v_uid,''diamonds'',v_proof,v_day,10)', 'values(v_uid,''diamonds'',v_proof,v_day,5)');
  v_def := replace(v_def, '''diamonds_awarded'',10', '''diamonds_awarded'',5');
  if v_def ~ '\+10|,10\)|awarded'',10' then raise exception 'store ad reward patch incomplete'; end if;
  execute v_def;

  select pg_get_functiondef('public.claim_rewarded_ad(text,text)'::regprocedure) into v_def;
  v_def := replace(v_def, 'diamonds=diamonds+10', 'diamonds=diamonds+5');
  v_def := replace(v_def, 'values(v_uid,10,''rewarded_ad'')', 'values(v_uid,5,''rewarded_ad'')');
  v_def := replace(v_def, 'values(v_uid,p_reward_type,p_ad_response_id,v_day,10)', 'values(v_uid,p_reward_type,p_ad_response_id,v_day,5)');
  v_def := replace(v_def, '''diamonds_awarded'',10', '''diamonds_awarded'',5');
  execute v_def;
end $$;

-- 9. Piggy bank: counts Son Harf and Kelime Kuşatması matches, opens once a day,
--    2/4/6/8 matches -> 15/25/35/40 SC (diminishing per match). Also fixes the old check constraint
--    (it allowed 200..800 while the function wrote 20..80, so the status call failed after 2 matches).
alter table public.piggy_banks drop constraint if exists piggy_banks_bonus_sc_check;
alter table public.piggy_banks add constraint piggy_banks_bonus_sc_check check (bonus_sc = any (array[0,15,25,35,40]));

create or replace function public.completed_store_match_count_v1(p_uid uuid)
returns integer language sql stable set search_path to '' as $$
  select ((select count(*) from public.game_rooms g where g.status='finished' and g.stats_applied and (g.host_id=p_uid or g.guest_id=p_uid))
        + (select count(*) from public.word_arena_rooms a where a.status='finished' and a.result_applied and (a.host_id=p_uid or a.guest_id=p_uid))
        + (select count(*) from public.word_siege_games s where s.status='finished' and p_uid in (s.player_one_id, s.player_two_id)))::integer
$$;

-- Re-baseline so past Kuşatma games do not pour into anyone's piggy at once.
update public.piggy_banks p set tier=0, bonus_sc=0,
  baseline_match_count = public.completed_store_match_count_v1(p.user_id),
  last_match_count = public.completed_store_match_count_v1(p.user_id), updated_at=now();

create or replace function public.piggy_opened_today_v1(p_opened_at timestamptz)
returns boolean language sql stable set search_path to '' as $$
  select p_opened_at is not null and timezone('Europe/Istanbul', p_opened_at)::date = timezone('Europe/Istanbul', now())::date
$$;

create or replace function public.get_store_reward_status_v1()
returns jsonb language plpgsql security definer set search_path to '' as $$
declare
  v_uid uuid := auth.uid(); v_day date := (timezone('utc',now()))::date;
  v_ad_count integer := 0; v_trial_count integer := 0; v_match_count integer := 0; v_baseline integer := 0;
  v_delta integer := 0; v_tier integer := 0; v_bonus integer := 0; v_opened_at timestamptz; v_opened_today boolean := false;
  v_trial public.style_trials%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select count(*) filter(where reward_type='diamonds'), count(*) filter(where reward_type='trial') into v_ad_count, v_trial_count
  from public.rewarded_ad_claims where user_id=v_uid and reward_date=v_day;
  v_match_count := public.completed_store_match_count_v1(v_uid);
  insert into public.piggy_banks(user_id,baseline_match_count,last_match_count) values(v_uid,v_match_count,v_match_count) on conflict(user_id) do nothing;
  select baseline_match_count, opened_at into v_baseline, v_opened_at from public.piggy_banks where user_id=v_uid for update;
  v_opened_today := public.piggy_opened_today_v1(v_opened_at);
  v_delta := greatest(0, v_match_count - v_baseline);
  v_tier := case when v_delta>=8 then 4 when v_delta>=6 then 3 when v_delta>=4 then 2 when v_delta>=2 then 1 else 0 end;
  v_bonus := case v_tier when 1 then 15 when 2 then 25 when 3 then 35 when 4 then 40 else 0 end;
  update public.piggy_banks set tier=v_tier, bonus_sc=v_bonus, last_match_count=v_match_count, updated_at=now() where user_id=v_uid;
  select * into v_trial from public.style_trials where user_id=v_uid and ended_at is null
    and (expires_at is null or expires_at>now()) and (matches_remaining is null or matches_remaining>0)
  order by started_at desc limit 1;
  return jsonb_build_object('coin_ads_used',coalesce(v_ad_count,0),'coin_ads_limit',3,'coin_per_ad',5,
    'trial_ads_used',coalesce(v_trial_count,0),'trial_ads_limit',1,'trial_item_id',v_trial.item_id,'trial_mode',v_trial.mode,
    'trial_matches_remaining',v_trial.matches_remaining,'trial_expires_at',v_trial.expires_at,
    'piggy_tier',v_tier,'piggy_bonus_sc',v_bonus,'piggy_match_progress',least(v_delta,8),'piggy_match_target',8,
    'piggy_opened_today',v_opened_today);
end $$;

create or replace function public.open_piggy_bank_v2()
returns jsonb language plpgsql security definer set search_path to '' as $$
declare v_uid uuid := auth.uid(); v_status jsonb; v_bonus integer; v_match_count integer; v_balance integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  v_status := public.get_store_reward_status_v1();
  if coalesce((v_status->>'piggy_opened_today')::boolean, false) then raise exception 'piggy_daily_limit'; end if;
  v_bonus := coalesce((v_status->>'piggy_bonus_sc')::integer, 0);
  if v_bonus not in (15,25,35,40) then raise exception 'piggy_not_ready'; end if;
  v_match_count := public.completed_store_match_count_v1(v_uid);
  update public.profiles set diamonds=coalesce(diamonds,0)+v_bonus, updated_at=now() where id=v_uid returning diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_bonus,'piggy_open:'||gen_random_uuid()::text);
  update public.piggy_banks set tier=0, bonus_sc=0, baseline_match_count=v_match_count, last_match_count=v_match_count,
    opened_at=now(), updated_at=now() where user_id=v_uid;
  return jsonb_build_object('success',true,'bonus_sc',v_bonus,'balance',v_balance);
end $$;

-- 10. Weekly goals: 60 SC a week in total, no PRO multiplier, lifetime-metric goals pay once.
update public.goal_definitions set reward_diamonds = v.reward from (values
  ('weekly_wins_5',15),('weekly_words_20',10),('weekly_friend_3',15),('expert_finish',15),('streak_5',5)
) v(id, reward) where goal_definitions.id = v.id;

create or replace function public.claim_goal_v1(p_goal_id text)
returns integer language plpgsql security definer set search_path to 'public', 'pg_temp' as $$
declare g public.goal_definitions; prog int; ws date := date_trunc('week',now())::date; new_balance int; v_reward int;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into g from public.goal_definitions where id=p_goal_id and active;
  if g.id is null then raise exception 'goal_not_found'; end if;
  prog := public.goal_progress_value(auth.uid(), g.metric);
  if prog < g.target then raise exception 'goal_not_complete'; end if;
  -- A goal measured on a lifetime record (best streak) can only be claimed once, not every week.
  if g.metric = 'best_streak' and exists(select 1 from public.goal_claims c where c.user_id=auth.uid() and c.goal_id=g.id) then
    raise exception 'goal_already_claimed';
  end if;
  insert into public.goal_claims(user_id,goal_id,week_start) values(auth.uid(),g.id,ws) on conflict do nothing;
  if not found then raise exception 'goal_already_claimed'; end if;
  v_reward := g.reward_diamonds;
  update public.profiles set diamonds=diamonds+v_reward,updated_at=now() where id=auth.uid() returning diamonds into new_balance;
  insert into public.diamond_ledger(user_id,delta,reason,item_id) values(auth.uid(),v_reward,'goal_reward:'||g.id,null);
  return new_balance;
end $$;

-- 11. PRO monthly coins: 400 -> 150. PRO value stays in comfort, looks and prestige.
create or replace function public.claim_vip_monthly_diamonds()
returns jsonb language plpgsql security definer set search_path to 'public', 'pg_temp' as $$
declare v_uid uuid := auth.uid(); v_period date := date_trunc('month',now())::date; v_vip boolean; v_balance integer; v_grant integer := 150;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select is_vip, diamonds into v_vip, v_balance from public.profiles where id=v_uid for update;
  if not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
  begin
    insert into public.vip_monthly_claims(user_id,period_start,diamonds) values(v_uid,v_period,v_grant);
  exception when unique_violation then raise exception 'already_claimed'; end;
  update public.profiles set diamonds=diamonds+v_grant where id=v_uid;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,v_grant,'vip_monthly');
  return jsonb_build_object('success',true,'diamonds',v_balance+v_grant,'granted',v_grant);
end $$;

-- 12. Home dashboard shows today's real check-in amount (it had a fixed 40).
do $$
declare v_def text;
begin
  select pg_get_functiondef('public.get_growth_dashboard_v1()'::regprocedure) into v_def;
  if position('40::int, exists(select 1 from public.daily_checkins' in regexp_replace(v_def, '\s+', ' ', 'g')) = 0 then
    raise exception 'dashboard daily reward literal not found';
  end if;
  v_def := regexp_replace(v_def, '40::int,(\s*)exists\(select 1 from public\.daily_checkins',
    '(public.siege_daily_cycle_rewards_v1())[(public.daily_checkin_streak_before_today_v1((select auth.uid())) % 7) + 1],\1exists(select 1 from public.daily_checkins');
  execute v_def;
end $$;
