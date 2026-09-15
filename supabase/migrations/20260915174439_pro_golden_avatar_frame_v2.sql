-- Canonical PRO Golden Avatar frame v2.
-- Uses the existing standardized 512x512 round Golden Avatar artwork.
-- Previous PRO Gold ownership is transferred to the canonical frame without loss.

insert into public.shop_items (
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
)
values (
  'frame_round_golden_avatar',
  'profile_frame',
  'PRO Golden',
  'PRO Golden',
  'PRO üyeliğiyle kalıcı olarak kazanılan standart Golden profil çerçevesi.',
  'Standard Golden profile frame permanently earned with PRO membership.',
  0,
  true,
  false,
  520
)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = 0,
  vip_only = true,
  active = false,
  sort_order = excluded.sort_order;

-- Preserve the earlier PRO Gold reward by transferring its ownership to the canonical 512x512 frame.
insert into public.user_inventory(user_id, item_id)
select user_id, 'frame_round_golden_avatar'
from public.user_inventory
where item_id = 'frame_asset_gold'
on conflict do nothing;

-- Grant the canonical Golden frame to every currently active PRO member.
insert into public.user_inventory(user_id, item_id)
select id, 'frame_round_golden_avatar'
from public.profiles
where coalesce(is_vip, false)
on conflict do nothing;

-- Migrate any legacy equipped Gold frame first.
update public.user_equipped_cosmetics
set profile_frame_id = 'frame_round_golden_avatar',
    updated_at = now()
where profile_frame_id = 'frame_asset_gold';

-- Apply Golden immediately to currently active PRO members.
insert into public.user_equipped_cosmetics(user_id)
select id
from public.profiles
where coalesce(is_vip, false)
on conflict (user_id) do nothing;

update public.user_equipped_cosmetics e
set profile_frame_id = 'frame_round_golden_avatar',
    updated_at = now()
from public.profiles p
where e.user_id = p.id
  and coalesce(p.is_vip, false);

-- The legacy item is replaced by the canonical frame; ownership has already been transferred above.
delete from public.user_inventory
where item_id = 'frame_asset_gold';

-- Replace the old grant-only trigger with a canonical grant + first-equip trigger.
drop trigger if exists trg_grant_pro_gold_profile_frame_v1 on public.profiles;
drop trigger if exists trg_sync_pro_golden_avatar_frame_v2 on public.profiles;
drop function if exists public.grant_pro_gold_profile_frame_v1();

create or replace function public.sync_pro_golden_avatar_frame_v2()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_became_pro boolean := false;
begin
  if coalesce(new.is_vip, false) then
    insert into public.user_inventory(user_id, item_id)
    values (new.id, 'frame_round_golden_avatar')
    on conflict do nothing;

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
  end if;

  return new;
end;
$$;

create trigger trg_sync_pro_golden_avatar_frame_v2
after insert or update of is_vip on public.profiles
for each row execute function public.sync_pro_golden_avatar_frame_v2();

revoke all on function public.sync_pro_golden_avatar_frame_v2() from public, anon, authenticated;
grant execute on function public.sync_pro_golden_avatar_frame_v2() to service_role;

-- Keep the existing equip RPC behavior for all cosmetics, but allow a permanently-owned
-- Golden frame to remain usable after PRO ends. Active PRO can self-heal a missing grant.
create or replace function public.equip_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_item public.shop_items%rowtype;
  v_vip boolean;
  v_owned boolean;
begin
  if v_uid is null then
    raise exception 'unauthorized';
  end if;

  select * into v_item
  from public.shop_items
  where id = p_item_id;

  if not found then
    raise exception 'item_not_found';
  end if;

  select is_vip into v_vip
  from public.profiles
  where id = v_uid;

  select exists(
    select 1
    from public.user_inventory
    where user_id = v_uid and item_id = p_item_id
  ) into v_owned;

  if v_item.kind = 'profile_frame' and p_item_id = 'frame_round_golden_avatar' then
    if not v_owned then
      if not coalesce(v_vip, false) then
        raise exception 'vip_required';
      end if;

      insert into public.user_inventory(user_id, item_id)
      values (v_uid, p_item_id)
      on conflict do nothing;
    end if;
  elsif not v_owned then
    raise exception 'not_owned';
  end if;

  insert into public.user_equipped_cosmetics(user_id)
  values (v_uid)
  on conflict (user_id) do nothing;

  update public.user_equipped_cosmetics
  set profile_frame_id = case when v_item.kind = 'profile_frame' then p_item_id else profile_frame_id end,
      name_style_id = case when v_item.kind = 'name_style' then p_item_id else name_style_id end,
      game_theme_id = case when v_item.kind = 'game_theme' then p_item_id else game_theme_id end,
      keyboard_theme_id = case when v_item.kind = 'keyboard_theme' then p_item_id else keyboard_theme_id end,
      victory_effect_id = case when v_item.kind = 'victory_effect' then p_item_id else victory_effect_id end,
      emoji_pack_id = case when v_item.kind = 'emoji_pack' then p_item_id else emoji_pack_id end,
      mascot_id = case when v_item.kind = 'mascot' then p_item_id else mascot_id end,
      updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object('success', true, 'item_id', p_item_id, 'kind', v_item.kind);
end;
$$;

select pg_notify('pgrst', 'reload schema');