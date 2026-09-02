-- Expanded purchased LAYERLAB frame catalog. Cosmetic only; never grants gameplay power.
insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order) values
('frame_asset_red','profile_frame','Kırmızı Hat','Red Line','Sıradan başlangıç çerçevesi.','Standard starter frame.',120,false,true,105),
('frame_asset_gold_crown','profile_frame','Altın Taç','Gold Crown','VIP/prestij koleksiyon çerçevesi.','VIP/prestige collection frame.',450,true,true,150)
on conflict (id) do update set name_tr=excluded.name_tr,name_en=excluded.name_en,description_tr=excluded.description_tr,description_en=excluded.description_en,diamond_price=excluded.diamond_price,vip_only=excluded.vip_only,active=excluded.active,sort_order=excluded.sort_order;
-- Christmas/Halloween remain event-controlled in the source pack and are deliberately not permanently bundled.
