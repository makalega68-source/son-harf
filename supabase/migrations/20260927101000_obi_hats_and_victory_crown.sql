-- New coin sinks with real in-game runtime:
--  * Obi hats (mascot_hat): Obi wears the equipped hat everywhere it appears.
--  * Taç Zaferi (victory_crown): the win celebration + crowned Obi, now sold for coins to everyone.

alter table public.user_equipped_cosmetics add column if not exists mascot_hat_id text;

alter table public.shop_items drop constraint if exists shop_items_kind_check;
alter table public.shop_items add constraint shop_items_kind_check check (kind = any (array[
  'profile_frame','name_style','game_theme','keyboard_theme','victory_effect','emoji_pack','mascot',
  'avatar_background','nameplate','badge','title','vs_intro','word_effect','emote','mascot_hat'
]));

create or replace function public.is_runtime_supported_shop_item_v1(p_item_id text, p_kind text)
returns boolean
language sql
immutable
set search_path to ''
as $$
  select case p_kind
    when 'game_theme' then p_item_id in ('theme_black','theme_dark_arena','theme_walnut_ivory')
    when 'profile_frame' then false
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

alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v2;
alter table public.shop_items drop constraint if exists shop_items_runtime_sale_guard_v3;

insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order, rarity)
values
  ('hat_beret',  'mascot_hat', 'Ressam Beresi',     'Painter''s Beret', 'Obi her yerde kırmızı beresiyle dolaşır.',        'Obi wears a red beret wherever it goes.', 250, false, true, 80, 'RARE'),
  ('hat_flower', 'mascot_hat', 'Çiçek Tacı',        'Flower Crown',     'Papatya ve güllerden taç; Obi''ye bahar gelir.',  'Daisies and roses: spring for Obi.',      300, false, true, 81, 'RARE'),
  ('hat_wizard', 'mascot_hat', 'Büyücü Şapkası',    'Wizard Hat',       'Yıldızlı mor şapka; kelimeler sihirle gelir.',     'A starry purple hat for word magic.',     350, false, true, 82, 'EPIC'),
  ('hat_top',    'mascot_hat', 'Altın Silindir',    'Golden Top Hat',   'Altın bantlı silindir şapka; tam bir centilmen.', 'A top hat with a gold band.',             400, false, true, 83, 'EPIC')
on conflict (id) do update set kind = excluded.kind, name_tr = excluded.name_tr, name_en = excluded.name_en,
  description_tr = excluded.description_tr, description_en = excluded.description_en,
  diamond_price = excluded.diamond_price, vip_only = false, active = true, sort_order = excluded.sort_order, rarity = excluded.rarity;

update public.shop_items
set active = true, vip_only = false, diamond_price = 450, rarity = 'EPIC', sort_order = 90,
    description_tr = 'Kazandığında altın taç iner ve Obi tacını takar.',
    description_en = 'Win and a golden crown drops; Obi wears it too.'
where id = 'victory_crown';

alter table public.shop_items add constraint shop_items_runtime_sale_guard_v3 check (
  active = false
  or (kind = 'game_theme' and id in ('theme_black','theme_walnut_ivory'))
  or (kind = 'keyboard_theme' and id in ('keyboard_crystal','keyboard_obsidian','keyboard_midnight','keyboard_black_gold',
      'keyboard_premium_white','keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple'))
  or (kind = 'name_style' and id in ('name_cyan','name_sapphire','name_amethyst','name_aurelia','name_emerald','name_ruby','name_sunset'))
  or (kind = 'victory_effect' and id = 'victory_crown')
  or (kind = 'mascot_hat' and id in ('hat_wizard','hat_beret','hat_flower','hat_top'))
);

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
  if p_kind not in ('game_theme','keyboard_theme','name_style','victory_effect','emoji_pack','mascot_hat') then
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
      updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object('success', true, 'kind', p_kind);
end;
$$;
