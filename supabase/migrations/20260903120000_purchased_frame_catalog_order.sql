-- Keep the five permanent purchased LAYERLAB frames contiguous in the Style shop.
-- This only changes catalogue ordering; prices, active flags, ownership and equipped state are untouched.
update public.shop_items
set sort_order = case id
  when 'frame_asset_red' then 105
  when 'frame_asset_green' then 106
  when 'frame_asset_mint' then 107
  when 'frame_asset_purple' then 108
  when 'frame_asset_gold' then 109
  else sort_order
end
where id in (
  'frame_asset_red',
  'frame_asset_green',
  'frame_asset_mint',
  'frame_asset_purple',
  'frame_asset_gold'
);
