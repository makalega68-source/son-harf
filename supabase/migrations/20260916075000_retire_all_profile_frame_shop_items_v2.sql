-- Retire every profile frame from the live shop while preserving ownership and transaction history.
update public.shop_items
set active=false
where kind='profile_frame' and active=true;

update public.user_equipped_cosmetics
set profile_frame_id=null
where profile_frame_id is not null;

update public.profiles
set default_profile_frame_id=null
where default_profile_frame_id is not null;
