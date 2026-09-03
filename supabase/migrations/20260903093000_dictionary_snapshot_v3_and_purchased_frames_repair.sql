-- Canonical dictionary v3: complete game-allowed 2..12 snapshot for both Turkish and English.
-- Full source datasets are stored in dictionary_words; the mobile snapshot is bounded by game rules.
create or replace function public.get_dictionary_snapshot_v3(p_language text default 'tr')
returns table(language text, words text[])
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_lang text := case when lower(coalesce(p_language, 'tr')) = 'en' then 'en' else 'tr' end;
begin
  return query
  select v_lang,
         coalesce(array_agg(d.normalized_word order by d.normalized_word), array[]::text[])
  from public.dictionary_words d
  where d.language = v_lang
    and d.active
    and coalesce(d.game_allowed, true)
    and not coalesce(d.is_abbreviation, false)
    and not coalesce(d.is_proper_noun, false)
    and char_length(d.normalized_word) between 2 and 12;
end;
$$;

grant execute on function public.get_dictionary_snapshot_v3(text) to anon, authenticated, service_role;

-- Purchased LAYERLAB package is repaired from the original archive. Permanent retail variants are
-- active again; Gold Crown remains an earned Legend reward and seasonal variants remain inactive.
insert into public.shop_items(id, kind, name_tr, name_en, description_tr, description_en, diamond_price, vip_only, active, sort_order) values
('frame_asset_red','profile_frame','Kırmızı Hat','Red Line','Satın alınan LAYERLAB paketinden kırmızı profil çerçevesi.','Red profile frame from the purchased LAYERLAB pack.',120,false,true,105),
('frame_asset_green','profile_frame','Zümrüt Hat','Emerald Line','Satın alınan LAYERLAB paketinden zümrüt profil çerçevesi.','Emerald profile frame from the purchased LAYERLAB pack.',240,false,true,110),
('frame_asset_mint','profile_frame','Buz Mint','Ice Mint','Satın alınan LAYERLAB paketinden mint profil çerçevesi.','Mint profile frame from the purchased LAYERLAB pack.',220,false,true,120),
('frame_asset_purple','profile_frame','Mor Spektrum','Violet Spectrum','Satın alınan LAYERLAB paketinden mor profil çerçevesi.','Violet profile frame from the purchased LAYERLAB pack.',280,false,true,130),
('frame_asset_gold','profile_frame','Altın Hat','Gold Line','Satın alınan LAYERLAB paketinden VIP altın profil çerçevesi.','VIP gold profile frame from the purchased LAYERLAB pack.',260,true,true,140),
('frame_asset_gold_crown','profile_frame','Altın Taç','Gold Crown','Efsane ligine ulaşınca açılan prestij çerçevesi.','Prestige frame unlocked at Legend league.',0,false,false,150),
('frame_asset_christmas','profile_frame','Yılbaşı','Christmas','Sezonluk yılbaşı etkinlik çerçevesi.','Seasonal Christmas event frame.',0,false,false,160),
('frame_asset_halloween','profile_frame','Halloween','Halloween','Sezonluk Halloween etkinlik çerçevesi.','Seasonal Halloween event frame.',0,false,false,170)
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
