-- Remove legacy profile frames from the active catalogue without deleting historical ownership/equip references.
-- Only the purchased 2D Avatar Frame package IDs remain eligible for the active profile-frame catalogue.
update public.shop_items
set active = false
where kind = 'profile_frame'
  and id not in (
    'frame_asset_red',
    'frame_asset_green',
    'frame_asset_mint',
    'frame_asset_purple',
    'frame_asset_gold',
    'frame_asset_gold_crown',
    'frame_asset_christmas',
    'frame_asset_halloween'
  );
