-- Son Harf mascot catalog v1
-- Product rules:
-- * mascot_white is the standard free/default mascot.
-- * only assets with verified commercial-game rights may be sold.
-- * mascot_chibi_wizard is licensed (Fab Standard License) but stays inactive until its converted
--   GLB is bundled and runtime-verified. Do not sell an asset that cannot render.
-- * Eve is intentionally parked and is not a shop item.

alter table public.shop_items
  drop constraint if exists shop_items_kind_check;

alter table public.shop_items
  add constraint shop_items_kind_check
  check (kind in (
    'profile_frame',
    'name_style',
    'game_theme',
    'keyboard_theme',
    'victory_effect',
    'emoji_pack',
    'mascot'
  ));

alter table public.user_equipped_cosmetics
  add column if not exists mascot_id text references public.shop_items(id);

insert into public.shop_items(
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
) values
(
  'mascot_white',
  'mascot',
  'Beyaz Dost',
  'White Buddy',
  'Standart, herkese açık Son Harf maskotu.',
  'Standard Son Harf mascot, unlocked for everyone.',
  0,
  false,
  true,
  5
),
(
  'mascot_chibi_wizard',
  'mascot',
  'Chibi Büyücü',
  'Chibi Wizard',
  'Lisanslı büyücü maskot. Oyun gücü vermez; yalnızca görünüm ve kişiselleştirme içindir.',
  'Licensed wizard mascot. Gives no match power; cosmetic personalization only.',
  700,
  false,
  false,
  6
)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = excluded.diamond_price,
  vip_only = excluded.vip_only,
  active = excluded.active,
  sort_order = excluded.sort_order;

-- Existing players receive the standard mascot without a purchase flow.
insert into public.user_inventory(user_id, item_id)
select id, 'mascot_white'
from public.profiles
on conflict (user_id, item_id) do nothing;

create or replace function public.grant_default_mascot_inventory()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  insert into public.user_inventory(user_id, item_id)
  values (new.id, 'mascot_white')
  on conflict (user_id, item_id) do nothing;
  return new;
end;
$$;

drop trigger if exists profiles_grant_default_mascot on public.profiles;
create trigger profiles_grant_default_mascot
after insert on public.profiles
for each row
execute function public.grant_default_mascot_inventory();

create or replace function public.equip_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_item public.shop_items%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select * into v_item
  from public.shop_items
  where id = p_item_id and active = true;

  if not found then raise exception 'item_not_found'; end if;

  if not exists(
    select 1 from public.user_inventory
    where user_id = v_uid and item_id = p_item_id
  ) then
    raise exception 'not_owned';
  end if;

  insert into public.user_equipped_cosmetics(user_id)
  values(v_uid)
  on conflict(user_id) do nothing;

  update public.user_equipped_cosmetics set
    profile_frame_id = case when v_item.kind = 'profile_frame' then p_item_id else profile_frame_id end,
    name_style_id = case when v_item.kind = 'name_style' then p_item_id else name_style_id end,
    game_theme_id = case when v_item.kind = 'game_theme' then p_item_id else game_theme_id end,
    keyboard_theme_id = case when v_item.kind = 'keyboard_theme' then p_item_id else keyboard_theme_id end,
    victory_effect_id = case when v_item.kind = 'victory_effect' then p_item_id else victory_effect_id end,
    emoji_pack_id = case when v_item.kind = 'emoji_pack' then p_item_id else emoji_pack_id end,
    mascot_id = case when v_item.kind = 'mascot' then p_item_id else mascot_id end,
    updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object(
    'success', true,
    'item_id', p_item_id,
    'kind', v_item.kind
  );
end;
$$;

revoke all on function public.equip_shop_item(text) from public;
grant execute on function public.equip_shop_item(text) to authenticated;
