-- Season Style hardening.
-- Remove the placeholder frame that has no packaged artwork and make earned
-- SEASON/EVENT cosmetics equippable only when the authenticated user owns them.

-- Never ship a catalog id whose artwork does not exist in the APK.
delete from public.season_store_rewards
where reward_key='frame_asset_blue_season';

delete from public.shop_items
where id='frame_asset_blue_season';

-- Keep the level-3 premium milestone useful without inventing an asset.
insert into public.season_store_rewards(
  season_id,level,track,reward_type,reward_key,amount,sort_order
)
select s.id,3,'premium','son_coin','',125,40
from public.competitive_seasons s
where s.starts_at<=now() and s.ends_at>now()
on conflict do nothing;

create or replace function public.equip_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_item public.shop_items%rowtype;
  v_owned boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select exists(
    select 1 from public.user_inventory i
    where i.user_id=v_uid and i.item_id=p_item_id
  ) into v_owned;
  if not v_owned then raise exception 'not_owned'; end if;

  select * into v_item
  from public.shop_items
  where id=p_item_id
    and (active=true or rarity in ('SEASON','EVENT'));
  if v_item.id is null then raise exception 'item_not_found'; end if;

  insert into public.user_equipped_cosmetics(user_id)
  values(v_uid)
  on conflict(user_id) do nothing;

  update public.user_equipped_cosmetics set
    profile_frame_id=case when v_item.kind='profile_frame' then p_item_id else profile_frame_id end,
    avatar_background_id=case when v_item.kind='avatar_background' then p_item_id else avatar_background_id end,
    nameplate_id=case when v_item.kind='nameplate' then p_item_id else nameplate_id end,
    badge_id=case when v_item.kind='badge' then p_item_id else badge_id end,
    title_style_id=case when v_item.kind='title' then p_item_id else title_style_id end,
    name_style_id=case when v_item.kind='name_style' then p_item_id else name_style_id end,
    game_theme_id=case when v_item.kind='game_theme' then p_item_id else game_theme_id end,
    keyboard_theme_id=case when v_item.kind='keyboard_theme' then p_item_id else keyboard_theme_id end,
    vs_intro_id=case when v_item.kind='vs_intro' then p_item_id else vs_intro_id end,
    word_effect_id=case when v_item.kind='word_effect' then p_item_id else word_effect_id end,
    victory_effect_id=case when v_item.kind='victory_effect' then p_item_id else victory_effect_id end,
    emote_id=case when v_item.kind='emote' then p_item_id else emote_id end,
    emoji_pack_id=case when v_item.kind='emoji_pack' then p_item_id else emoji_pack_id end,
    mascot_id=case when v_item.kind='mascot' then p_item_id else mascot_id end,
    updated_at=now()
  where user_id=v_uid;

  return jsonb_build_object('success',true,'item_id',p_item_id,'kind',v_item.kind);
end
$$;
revoke all on function public.equip_shop_item(text) from public,anon;
grant execute on function public.equip_shop_item(text) to authenticated;
