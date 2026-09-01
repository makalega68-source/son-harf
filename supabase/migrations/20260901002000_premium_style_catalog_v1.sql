-- Premium Style catalog. Visual-only cosmetics; no competitive/gameplay fields.
insert into public.shop_items(id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order) values
('frame_black_gold','profile_frame','Black Gold','Black Gold','Mat siyah ve kontrollü altın profil çerçevesi.','Matte black and restrained gold profile frame.',240,false,true,110),
('frame_royal_gold','profile_frame','Royal Gold','Royal Gold','Klasik premium altın profil çerçevesi.','Classic premium gold profile frame.',260,false,true,111),
('frame_crystal','profile_frame','Crystal','Crystal','Temiz cam/kristal hissinde profil çerçevesi.','Clean glass/crystal profile frame.',250,false,true,112),
('frame_purple_prestige','profile_frame','Purple Prestige','Purple Prestige','Sade mor prestij profil çerçevesi.','Restrained purple prestige profile frame.',260,false,true,113),
('frame_ice','profile_frame','Ice','Ice','Soğuk mavi buz profil çerçevesi.','Cool blue ice profile frame.',230,false,true,114),
('frame_modern_neon','profile_frame','Modern Neon','Modern Neon','Abartısız modern neon profil çerçevesi.','Restrained modern neon profile frame.',250,false,true,115),
('keyboard_midnight','keyboard_theme','Midnight Keyboard','Midnight Keyboard','Koyu, yüksek kontrastlı oyun klavyesi.','Dark high-contrast game keyboard.',220,false,true,130),
('keyboard_black_gold','keyboard_theme','Black Gold Keyboard','Black Gold Keyboard','Siyah-altın premium klavye görünümü.','Premium black-and-gold keyboard styling.',240,false,true,131),
('keyboard_crystal','keyboard_theme','Crystal Keyboard','Crystal Keyboard','Açık kristal/cam klavye görünümü.','Light crystal/glass keyboard styling.',220,false,true,132),
('keyboard_premium_white','keyboard_theme','Premium White Keyboard','Premium White Keyboard','Temiz ve yüksek kontrastlı beyaz klavye.','Clean high-contrast white keyboard.',210,false,true,133),
('theme_midnight','game_theme','Midnight / Black Theme','Midnight / Black Theme','Ana ekranları çok koyu premium yüzeye taşır.','Applies a premium near-black surface to supported screens.',320,false,true,150)
on conflict(id) do update set
 kind=excluded.kind,name_tr=excluded.name_tr,name_en=excluded.name_en,
 description_tr=excluded.description_tr,description_en=excluded.description_en,
 diamond_price=excluded.diamond_price,vip_only=excluded.vip_only,active=excluded.active,sort_order=excluded.sort_order;
