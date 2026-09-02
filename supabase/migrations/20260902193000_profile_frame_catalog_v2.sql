-- Expanded purchased LAYERLAB profile-frame catalog.
-- Cosmetic only; no item below grants rating, timer, score, letters, jokers or match power.
insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order) values
('frame_asset_red','profile_frame','Kırmızı Hat','Red Line','Sıradan başlangıç çerçevesi.','Standard starter frame.',120,false,true,105),
('frame_asset_green','profile_frame','Zümrüt Hat','Emerald Line','Dengeli zümrüt profil çerçevesi.','Balanced emerald profile frame.',240,false,true,110),
('frame_asset_mint','profile_frame','Buz Mint','Ice Mint','Temiz ve modern mint çerçeve.','Clean modern mint frame.',220,false,true,120),
('frame_asset_purple','profile_frame','Mor Spektrum','Violet Spectrum','Premium mor profil vurgusu.','Premium violet profile accent.',280,false,true,130),
('frame_asset_gold','profile_frame','Altın Hat','Gold Line','VIP ve prestij koleksiyonu çerçevesi.','VIP and prestige collection frame.',260,true,true,140),
('frame_asset_gold_crown','profile_frame','Altın Taç','Gold Crown','Efsane ligine ulaşınca açılan prestij çerçevesi.','Prestige frame unlocked at Legend league.',0,false,false,150)
on conflict (id) do update set
kind=excluded.kind,
name_tr=excluded.name_tr,
name_en=excluded.name_en,
description_tr=excluded.description_tr,
description_en=excluded.description_en,
diamond_price=excluded.diamond_price,
vip_only=excluded.vip_only,
active=excluded.active,
sort_order=excluded.sort_order;

-- Son Harf's canonical league model starts EFSANE at 1800 rating.
-- Gold Crown is therefore an earned league reward, not a permanent shop purchase.
create or replace function public.grant_legend_profile_frame_v1()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.rating >= 1800 and coalesce(old.rating, 0) < 1800 then
    insert into public.user_inventory(user_id, item_id)
    values (new.id, 'frame_asset_gold_crown')
    on conflict (user_id, item_id) do nothing;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_grant_legend_profile_frame_v1 on public.profiles;
create trigger trg_grant_legend_profile_frame_v1
after update of rating on public.profiles
for each row execute function public.grant_legend_profile_frame_v1();

-- Backfill existing EFSANE players once, idempotently.
insert into public.user_inventory(user_id, item_id)
select id, 'frame_asset_gold_crown'
from public.profiles
where rating >= 1800
on conflict (user_id, item_id) do nothing;

-- Christmas/Halloween remain reserved for time-bounded seasonal/event releases.
