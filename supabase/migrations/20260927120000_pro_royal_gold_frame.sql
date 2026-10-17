-- PRO-only frame: the ruby-crowned Royal Gold art now lives on the existing PRO frame id
-- (frame_round_golden_avatar). It is granted and equipped the moment a player becomes PRO and
-- taken back (and unequipped) when PRO ends. It is never sold; the Play product planned for the
-- same art is withdrawn.

-- The PRO frame is equippable (equip_shop_item checks runtime support + ownership).
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
      'frame_round_rose','frame_round_lilac','frame_round_botanic','frame_round_golden_avatar',
      'profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
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

update public.shop_items
set name_tr = 'PRO Kraliyet Altın', name_en = 'PRO Royal Gold',
    description_tr = 'PRO üyelere özel yakutlu taçlı altın çerçeve.',
    description_en = 'Ruby-crowned gold frame, for PRO members only.',
    active = false, vip_only = true, diamond_price = 0, rarity = 'VIP'
where id = 'frame_round_golden_avatar';

-- Withdraw the Play product for the same art.
update public.store_catalog_config set enabled = false, updated_at = now() where product_id = 'profile_frame_royal_gold';
delete from public.shop_items s
where s.id = 'profile_frame_royal_gold'
  and not exists (select 1 from public.user_inventory i where i.item_id = s.id);

create or replace function public.sync_pro_golden_avatar_frame_v2()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_became_pro boolean := false;
begin
  if coalesce(new.is_vip, false) then
    insert into public.user_inventory(user_id, item_id)
    select new.id, 'frame_round_golden_avatar'
    where not exists (select 1 from public.user_inventory where user_id = new.id and item_id = 'frame_round_golden_avatar');

    if tg_op = 'INSERT' then
      v_became_pro := true;
    elsif tg_op = 'UPDATE' then
      v_became_pro := not coalesce(old.is_vip, false);
    end if;

    if v_became_pro then
      insert into public.user_equipped_cosmetics(user_id)
      values (new.id)
      on conflict (user_id) do nothing;

      update public.user_equipped_cosmetics
      set profile_frame_id = 'frame_round_golden_avatar',
          updated_at = now()
      where user_id = new.id;
    end if;
  elsif tg_op = 'UPDATE' and coalesce(old.is_vip, false) then
    -- PRO ended: the PRO-only frame goes back.
    delete from public.user_inventory where user_id = new.id and item_id = 'frame_round_golden_avatar';
    update public.user_equipped_cosmetics
    set profile_frame_id = null, updated_at = now()
    where user_id = new.id and profile_frame_id = 'frame_round_golden_avatar';
  end if;

  return new;
end;
$function$;

-- Current PRO players get the frame now; it is equipped unless they already wear another frame.
insert into public.user_inventory(user_id, item_id)
select p.id, 'frame_round_golden_avatar'
from public.profiles p
where p.is_vip
  and not exists (select 1 from public.user_inventory i where i.user_id = p.id and i.item_id = 'frame_round_golden_avatar');

insert into public.user_equipped_cosmetics(user_id)
select p.id from public.profiles p where p.is_vip
on conflict (user_id) do nothing;

update public.user_equipped_cosmetics e
set profile_frame_id = 'frame_round_golden_avatar', updated_at = now()
from public.profiles p
where p.id = e.user_id and p.is_vip
  and (e.profile_frame_id is null or e.profile_frame_id not in (
    'frame_round_starter_blue','frame_round_starter_neutral','frame_round_pearl','frame_round_ocean',
    'frame_round_rose','frame_round_lilac','frame_round_botanic',
    'profile_frame_gold_crest','profile_frame_emerald','profile_frame_amethyst',
    'profile_frame_pink_blossom','profile_frame_blue_royal'));

-- Non-PRO players never keep the PRO frame.
update public.user_equipped_cosmetics e
set profile_frame_id = null, updated_at = now()
from public.profiles p
where p.id = e.user_id and not coalesce(p.is_vip, false) and e.profile_frame_id = 'frame_round_golden_avatar';
delete from public.user_inventory i
using public.profiles p
where p.id = i.user_id and not coalesce(p.is_vip, false) and i.item_id = 'frame_round_golden_avatar';
