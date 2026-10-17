-- Profile frames v2: frames come back with new artwork.
--  * Simple rings are Son Coin shop items (active, priced).
--  * Ornate crests are permanent Google Play products. Their shop row id equals the Play product
--    id and stays inactive so it can never be bought with coins; the purchase verifier grants the
--    item into user_inventory, after which equip_shop_item equips it like any other cosmetic.

-- 1) Runtime support for the new frame set.
create or replace function public.is_runtime_supported_shop_item_v1(p_item_id text, p_kind text)
returns boolean
language sql
immutable
set search_path to ''
as $$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena','theme_walnut_ivory')
    when 'profile_frame' then p_item_id in (
      'frame_round_starter_blue','frame_round_starter_neutral','frame_round_pearl','frame_round_ocean',
      'frame_round_rose','frame_round_lilac','frame_round_botanic',
      'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
      'profile_frame_pink_blossom','profile_frame_blue_royal'
    )
    when 'name_style' then p_item_id in (
      'name_cyan','name_sapphire','name_amethyst','name_aurelia',
      'name_emerald','name_ruby','name_sunset'
    )
    when 'keyboard_theme' then p_item_id in (
      'keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold',
      'keyboard_premium_white','keyboard_sakura','keyboard_ocean','keyboard_forest',
      'keyboard_royal_purple'
    )
    when 'victory_effect' then p_item_id='victory_crown'
    when 'emoji_pack' then p_item_id='emoji_vip'
    when 'mascot_hat' then p_item_id in ('hat_wizard','hat_beret','hat_flower','hat_top')
    else false
  end
$$;

-- 2) Coin frames may be active again; premium frame rows stay inactive (Play-only).
alter table public.shop_items drop constraint if exists shop_items_no_active_profile_frames_v3;
alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v3;
alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v4;

insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order, rarity)
values
  ('frame_round_starter_blue',    'profile_frame', 'Başlangıç Mavi', 'Starter Blue',   'Parlak mavi sade halka.',      'A bright blue simple ring.',   60, false, true, 300, 'STANDARD'),
  ('frame_round_starter_neutral', 'profile_frame', 'Başlangıç Nötr', 'Starter Neutral','Gümüş tonlu sade halka.',      'A silver-toned simple ring.',  60, false, true, 301, 'STANDARD'),
  ('frame_round_pearl',           'profile_frame', 'Beyaz İnci',     'White Pearl',    'İnci beyazı yumuşak halka.',   'A soft pearl-white ring.',     80, false, true, 302, 'STANDARD'),
  ('frame_round_ocean',           'profile_frame', 'Okyanus',        'Ocean',          'Dalgalı okyanus mavisi halka.', 'A wavy ocean-blue ring.',     150, false, true, 303, 'RARE'),
  ('frame_round_rose',            'profile_frame', 'Gül Pembesi',    'Rose Pink',      'Parlak gül pembesi halka.',    'A glossy rose-pink ring.',    150, false, true, 304, 'RARE'),
  ('frame_round_lilac',           'profile_frame', 'Lavanta',        'Lavender',       'Yumuşak lavanta halka.',       'A soft lavender ring.',       150, false, true, 305, 'RARE'),
  ('frame_round_botanic',         'profile_frame', 'Botanik Yeşil',  'Botanic Green',  'Yapraklı yeşil halka.',        'A leafy green ring.',         150, false, true, 306, 'RARE'),
  ('profile_frame_royal_gold',    'profile_frame', 'Kraliyet Altın', 'Royal Gold',     'Yakutlu taçlı altın çerçeve.',  'A ruby-crowned gold frame.',    0, false, false, 310, 'LEGENDARY'),
  ('profile_frame_gold_crest',    'profile_frame', 'Altın Arma',     'Gold Crest',     'Işıltılı altın arma çerçeve.',  'A radiant gold crest frame.',   0, false, false, 311, 'LEGENDARY'),
  ('profile_frame_emerald',       'profile_frame', 'Zümrüt Arma',    'Emerald Crest',  'Zümrüt taşlı altın çerçeve.',   'A gold frame with emeralds.',   0, false, false, 312, 'EPIC'),
  ('profile_frame_amethyst',      'profile_frame', 'Ametist Arma',   'Amethyst Crest', 'Ametist ışıltılı çerçeve.',     'A glowing amethyst frame.',     0, false, false, 313, 'EPIC'),
  ('profile_frame_pink_blossom',  'profile_frame', 'Sakura Çelengi', 'Sakura Wreath',  'Kiraz çiçekli gül altın çerçeve.', 'A rose-gold sakura frame.',   0, false, false, 314, 'EPIC'),
  ('profile_frame_blue_royal',    'profile_frame', 'Safir Arma',     'Sapphire Crest', 'Safir taşlı gümüş çerçeve.',    'A silver frame with sapphires.',0, false, false, 315, 'EPIC')
on conflict (id) do update set kind = excluded.kind, name_tr = excluded.name_tr, name_en = excluded.name_en,
  description_tr = excluded.description_tr, description_en = excluded.description_en,
  diamond_price = excluded.diamond_price, vip_only = false, active = excluded.active,
  sort_order = excluded.sort_order, rarity = excluded.rarity;

alter table public.shop_items add constraint shop_items_runtime_sale_guard_v4 check (
  active = false
  or (kind = 'game_theme' and id in ('theme_black','theme_walnut_ivory'))
  or (kind = 'keyboard_theme' and id in ('keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold',
      'keyboard_premium_white','keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple'))
  or (kind = 'name_style' and id in ('name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset'))
  or (kind = 'victory_effect' and id = 'victory_crown')
  or (kind = 'mascot_hat' and id in ('hat_wizard','hat_beret','hat_flower','hat_top'))
  or (kind = 'profile_frame' and diamond_price > 0 and id in (
      'frame_round_starter_blue','frame_round_starter_neutral','frame_round_pearl','frame_round_ocean',
      'frame_round_rose','frame_round_lilac','frame_round_botanic'))
);

-- 3) Equip / unequip frames.
create or replace function public.equip_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_uid uuid := auth.uid();
  v_item public.shop_items%rowtype;
  v_owned boolean := false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select * into v_item from public.shop_items where id=p_item_id;
  if not found then raise exception 'item_not_found'; end if;

  if not public.is_runtime_supported_shop_item_v1(v_item.id,v_item.kind) then
    raise exception 'item_runtime_unavailable';
  end if;

  select exists(
    select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id
  ) into v_owned;
  if not v_owned then raise exception 'not_owned'; end if;

  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics
  set name_style_id=case when v_item.kind='name_style' then p_item_id else name_style_id end,
      game_theme_id=case when v_item.kind='game_theme' then p_item_id else game_theme_id end,
      keyboard_theme_id=case when v_item.kind='keyboard_theme' then p_item_id else keyboard_theme_id end,
      victory_effect_id=case when v_item.kind='victory_effect' then p_item_id else victory_effect_id end,
      emoji_pack_id=case when v_item.kind='emoji_pack' then p_item_id else emoji_pack_id end,
      mascot_hat_id=case when v_item.kind='mascot_hat' then p_item_id else mascot_hat_id end,
      profile_frame_id=case when v_item.kind='profile_frame' then p_item_id else profile_frame_id end,
      updated_at=now()
  where user_id=v_uid;

  return jsonb_build_object('success',true,'item_id',p_item_id,'kind',v_item.kind);
end
$function$;

create or replace function public.equip_default_cosmetic(p_kind text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'unauthorized';
  end if;
  if p_kind not in ('game_theme','keyboard_theme','name_style','victory_effect','emoji_pack','mascot_hat','profile_frame') then
    raise exception 'unsupported cosmetic kind: %', p_kind;
  end if;

  insert into public.user_equipped_cosmetics(user_id)
  values (v_uid)
  on conflict (user_id) do nothing;

  update public.user_equipped_cosmetics
  set game_theme_id     = case when p_kind = 'game_theme'     then null else game_theme_id end,
      keyboard_theme_id = case when p_kind = 'keyboard_theme' then null else keyboard_theme_id end,
      name_style_id     = case when p_kind = 'name_style'     then null else name_style_id end,
      victory_effect_id = case when p_kind = 'victory_effect' then null else victory_effect_id end,
      emoji_pack_id     = case when p_kind = 'emoji_pack'     then null else emoji_pack_id end,
      mascot_hat_id     = case when p_kind = 'mascot_hat'     then null else mascot_hat_id end,
      profile_frame_id  = case when p_kind = 'profile_frame'  then null else profile_frame_id end,
      updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object('success', true, 'kind', p_kind);
end;
$$;

-- 4) Google Play: premium frames become sellable one-time products.
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v3;
alter table public.store_catalog_config drop constraint if exists store_catalog_enabled_product_guard_v4;
alter table public.store_catalog_config
  add constraint store_catalog_enabled_product_guard_v4
  check (enabled = false or product_id = any (array[
    'vip_monthly','vip_yearly','season_pass_monthly','series_game','letter_table','score_calculator','pro_lifetime',
    'coins_500','coins_1500','coins_3500','coins_8000',
    'mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik',
    'mascot_tekir','mascot_robot','mascot_astronot',
    'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
    'profile_frame_pink_blossom','profile_frame_blue_royal'
  ]));

insert into public.store_catalog_config(product_id, enabled, badge_tr, badge_en, sort_order, updated_at)
values
  ('profile_frame_royal_gold', true, 'ÇERÇEVE', 'FRAME', 40, now()),
  ('profile_frame_gold_crest', true, 'ÇERÇEVE', 'FRAME', 41, now()),
  ('profile_frame_emerald', true, 'ÇERÇEVE', 'FRAME', 42, now()),
  ('profile_frame_amethyst', true, 'ÇERÇEVE', 'FRAME', 43, now()),
  ('profile_frame_pink_blossom', true, 'ÇERÇEVE', 'FRAME', 44, now()),
  ('profile_frame_blue_royal', true, 'ÇERÇEVE', 'FRAME', 45, now())
on conflict (product_id) do update
set enabled = excluded.enabled, badge_tr = excluded.badge_tr, badge_en = excluded.badge_en,
    sort_order = excluded.sort_order, updated_at = now();

-- 5) Verified premium purchase: frames grant their shop item into the inventory.
create or replace function public.apply_verified_premium_purchase_v1(
  p_user_id uuid,
  p_product_id text,
  p_purchase_token text,
  p_order_id text default null,
  p_play_state text default null,
  p_acknowledgement_state text default null
)
returns jsonb
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_purchase_id uuid;
  v_purchase_user_id uuid;
  v_purchase_product_id text;
  v_inserted boolean := false;
  v_key text;
  v_balance integer;
  v_frame boolean := p_product_id in (
    'profile_frame_royal_gold','profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
    'profile_frame_pink_blossom','profile_frame_blue_royal'
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
    -- Each mascot and each premium frame is its own permanent entitlement, keyed by its product id.
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
    -- Lifetime PRO permanently owns the premium feature bundle. Frames are sold separately.
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
$function$;
