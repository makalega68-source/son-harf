-- Profile frames are retired from the live game.
-- Ownership/receipt history is intentionally preserved for auditability, but frames are no longer
-- sold, equipped, or rendered by the client.

update public.store_catalog_config
set enabled=false, updated_at=now()
where product_id in ('profile_frame_ocean','profile_frame_botanic','profile_frame_lilac','profile_frame_rose');

update public.user_equipped_cosmetics
set profile_frame_id=null
where profile_frame_id is not null;

update public.profiles
set default_profile_frame_id=null
where default_profile_frame_id is not null;
