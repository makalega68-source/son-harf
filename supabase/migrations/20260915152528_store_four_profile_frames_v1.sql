-- Store four packaged profile frames v1.
-- These visual-only cosmetics use the already bundled verified artwork and the existing
-- server-authoritative Son Coin purchase/equip path. PRO Gold remains a separate membership reward.

update public.shop_items
set
  active = true,
  vip_only = false,
  available_from = least(coalesce(available_from, now()), now()),
  available_until = null,
  name_tr = case id
    when 'frame_asset_red' then 'Kırmızı Hat'
    when 'frame_asset_green' then 'Zümrüt Hat'
    when 'frame_asset_mint' then 'Buz Mint'
    when 'frame_asset_purple' then 'Mor Spektrum'
    else name_tr
  end,
  name_en = case id
    when 'frame_asset_red' then 'Red Line'
    when 'frame_asset_green' then 'Emerald Line'
    when 'frame_asset_mint' then 'Ice Mint'
    when 'frame_asset_purple' then 'Violet Spectrum'
    else name_en
  end,
  description_tr = case id
    when 'frame_asset_red' then 'Canlı kırmızı tonlu profil çerçevesi.'
    when 'frame_asset_green' then 'Zümrüt tonlu profil çerçevesi.'
    when 'frame_asset_mint' then 'Ferah mint tonlu profil çerçevesi.'
    when 'frame_asset_purple' then 'Mor spektrum tonlu profil çerçevesi.'
    else description_tr
  end,
  description_en = case id
    when 'frame_asset_red' then 'Vivid red profile frame.'
    when 'frame_asset_green' then 'Emerald profile frame.'
    when 'frame_asset_mint' then 'Fresh mint profile frame.'
    when 'frame_asset_purple' then 'Violet spectrum profile frame.'
    else description_en
  end
where id in (
  'frame_asset_red',
  'frame_asset_green',
  'frame_asset_mint',
  'frame_asset_purple'
)
  and kind = 'profile_frame';

update public.shop_items
set diamond_price = case id
  when 'frame_asset_red' then 120
  when 'frame_asset_green' then 240
  when 'frame_asset_mint' then 220
  when 'frame_asset_purple' then 280
  else diamond_price
end
where id in (
  'frame_asset_red',
  'frame_asset_green',
  'frame_asset_mint',
  'frame_asset_purple'
);

select pg_notify('pgrst', 'reload schema');