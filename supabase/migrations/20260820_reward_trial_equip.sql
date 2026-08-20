-- Make a 24-hour rewarded premium trial actually equippable and remove it after expiry unless owned.

create or replace function public.get_reward_center_status()
returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid:=auth.uid();
  v_day date:=(timezone('utc',now()))::date;
  v_diamond_count int;
  v_chest_count int;
  v_trial_count int;
  v_wallet public.reward_wallet%rowtype;
  v_expired_item text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  insert into public.reward_wallet(user_id) values(v_uid) on conflict(user_id) do nothing;
  select count(*) filter(where reward_type='diamonds'),
         count(*) filter(where reward_type='chest'),
         count(*) filter(where reward_type='trial')
    into v_diamond_count,v_chest_count,v_trial_count
  from public.rewarded_ad_claims where user_id=v_uid and reward_date=v_day;

  select * into v_wallet from public.reward_wallet where user_id=v_uid for update;
  if v_wallet.trial_expires_at is not null and v_wallet.trial_expires_at <= now() then
    v_expired_item:=v_wallet.trial_item_id;
    if v_expired_item is not null and not exists(
      select 1 from public.user_inventory where user_id=v_uid and item_id=v_expired_item
    ) then
      update public.user_equipped_cosmetics set
        profile_frame_id=case when profile_frame_id=v_expired_item then null else profile_frame_id end,
        name_style_id=case when name_style_id=v_expired_item then null else name_style_id end,
        game_theme_id=case when game_theme_id=v_expired_item then null else game_theme_id end,
        keyboard_theme_id=case when keyboard_theme_id=v_expired_item then null else keyboard_theme_id end,
        victory_effect_id=case when victory_effect_id=v_expired_item then null else victory_effect_id end,
        emoji_pack_id=case when emoji_pack_id=v_expired_item then null else emoji_pack_id end,
        updated_at=now()
      where user_id=v_uid;
    end if;
    update public.reward_wallet set trial_item_id=null,trial_expires_at=null,updated_at=now() where user_id=v_uid;
    v_wallet.trial_item_id:=null;
    v_wallet.trial_expires_at:=null;
  end if;

  return jsonb_build_object(
    'diamond_ads_used',coalesce(v_diamond_count,0),'diamond_ads_limit',3,'diamond_per_ad',10,
    'chest_ads_used',coalesce(v_chest_count,0),'chest_ads_limit',2,
    'trial_ads_used',coalesce(v_trial_count,0),'trial_ads_limit',1,
    'chest_keys',coalesce(v_wallet.chest_keys,0),
    'trial_item_id',v_wallet.trial_item_id,
    'trial_expires_at',v_wallet.trial_expires_at
  );
end $$;

create or replace function public.equip_reward_trial()
returns jsonb
language plpgsql security definer set search_path=public,pg_temp as $$
declare
  v_uid uuid:=auth.uid();
  v_wallet public.reward_wallet%rowtype;
  v_item public.shop_items%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_wallet from public.reward_wallet where user_id=v_uid for update;
  if not found or v_wallet.trial_item_id is null or v_wallet.trial_expires_at is null or v_wallet.trial_expires_at <= now() then
    raise exception 'trial_not_active';
  end if;
  select * into v_item from public.shop_items where id=v_wallet.trial_item_id and active=true;
  if not found then raise exception 'trial_item_unavailable'; end if;

  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics set
    profile_frame_id=case when v_item.kind='profile_frame' then v_item.id else profile_frame_id end,
    name_style_id=case when v_item.kind='name_style' then v_item.id else name_style_id end,
    game_theme_id=case when v_item.kind='game_theme' then v_item.id else game_theme_id end,
    keyboard_theme_id=case when v_item.kind='keyboard_theme' then v_item.id else keyboard_theme_id end,
    victory_effect_id=case when v_item.kind='victory_effect' then v_item.id else victory_effect_id end,
    emoji_pack_id=case when v_item.kind='emoji_pack' then v_item.id else emoji_pack_id end,
    updated_at=now()
  where user_id=v_uid;

  return jsonb_build_object('success',true,'item_id',v_item.id,'kind',v_item.kind,'expires_at',v_wallet.trial_expires_at);
end $$;

revoke all on function public.equip_reward_trial() from public;
grant execute on function public.equip_reward_trial() to authenticated;
