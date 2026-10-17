-- Store test fix: four keyboards and three name colours were on sale on the server but have no
-- palette/colour or artwork in the app, so a buyer would pay for nothing. Nobody owns them; they
-- come off sale (runtime support is kept so any future owner could still equip them once added).
update public.shop_items
set active = false
where id in ('keyboard_sakura','keyboard_ocean','keyboard_forest','keyboard_royal_purple',
             'name_emerald','name_ruby','name_sunset');
