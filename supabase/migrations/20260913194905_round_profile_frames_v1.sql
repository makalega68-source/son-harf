-- Round profile frames v1: server is the authority for ownership, equipment and VIP access.
-- Existing inventory remains intact; only obsolete equipped square IDs are replaced.

alter table public.profiles add column if not exists default_profile_frame_id text;

insert into public.shop_items (id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order)
values
  ('frame_round_starter_blue','profile_frame','Başlangıç Mavi','Starter Blue','Ücretsiz başlangıç yuvarlak çerçevesi.','Free starter round frame.',0,false,false,501),
  ('frame_round_starter_pink','profile_frame','Başlangıç Pembe','Starter Pink','Ücretsiz başlangıç yuvarlak çerçevesi.','Free starter round frame.',0,false,false,502),
  ('frame_round_starter_neutral','profile_frame','Başlangıç Nötr','Starter Neutral','Ücretsiz başlangıç yuvarlak çerçevesi.','Free starter round frame.',0,false,false,503),
  ('frame_round_ocean','profile_frame','Okyanus Halkası','Ocean Ring','Google Play ile kalıcı kozmetik çerçeve.','Permanent cosmetic frame via Google Play.',0,false,false,510),
  ('frame_round_botanic','profile_frame','Botanik Halka','Botanic Ring','Google Play ile kalıcı kozmetik çerçeve.','Permanent cosmetic frame via Google Play.',0,false,false,511),
  ('frame_round_lilac','profile_frame','Lila Halo','Lilac Halo','Google Play ile kalıcı kozmetik çerçeve.','Permanent cosmetic frame via Google Play.',0,false,false,512),
  ('frame_round_rose','profile_frame','Gül Işığı','Rose Glow','Google Play ile kalıcı kozmetik çerçeve.','Permanent cosmetic frame via Google Play.',0,false,false,513),
  ('frame_round_golden_avatar','profile_frame','Golden Avatar','Golden Avatar','Yalnızca aktif VIP / PRO oyuncular için.','Only for active VIP / PRO players.',0,true,false,520)
on conflict (id) do update set
  kind=excluded.kind,name_tr=excluded.name_tr,name_en=excluded.name_en,
  description_tr=excluded.description_tr,description_en=excluded.description_en,
  diamond_price=0,vip_only=excluded.vip_only,active=false,sort_order=excluded.sort_order;

-- Retire every old rectangular catalogue record. Historical ownership rows are intentionally kept.
update public.shop_items set active=false
where kind='profile_frame' and id in (
  'frame_asset_red','frame_asset_green','frame_asset_mint','frame_asset_purple','frame_asset_gold',
  'frame_asset_gold_crown','frame_asset_christmas','frame_asset_halloween','frame_neon','frame_gold','frame_starter'
);

create or replace function public.profile_frame_default_v1(p_gender text)
returns text language sql immutable set search_path='' as $$
  select case lower(coalesce(trim(p_gender),''))
    when 'male' then 'frame_round_starter_blue'
    when 'erkek' then 'frame_round_starter_blue'
    when 'female' then 'frame_round_starter_pink'
    when 'kadın' then 'frame_round_starter_pink'
    when 'kadin' then 'frame_round_starter_pink'
    else 'frame_round_starter_neutral'
  end
$$;

create or replace function public.ensure_profile_frame_default_v1()
returns trigger language plpgsql security definer set search_path to 'public','pg_temp' as $$
declare v_frame text;
begin
  v_frame:=coalesce(new.default_profile_frame_id, public.profile_frame_default_v1(new.gender));
  new.default_profile_frame_id:=v_frame;
  insert into public.user_inventory(user_id,item_id) values(new.id,v_frame) on conflict do nothing;
  return new;
end $$;

drop trigger if exists trg_ensure_profile_frame_default_v1 on public.profiles;
create trigger trg_ensure_profile_frame_default_v1 before insert on public.profiles
for each row execute function public.ensure_profile_frame_default_v1();

update public.profiles set default_profile_frame_id=public.profile_frame_default_v1(gender)
where default_profile_frame_id is null;
insert into public.user_inventory(user_id,item_id)
select id,default_profile_frame_id from public.profiles
where default_profile_frame_id is not null on conflict do nothing;

update public.user_equipped_cosmetics e
set profile_frame_id=p.default_profile_frame_id
from public.profiles p
where p.id=e.user_id and e.profile_frame_id in (
  'frame_asset_red','frame_asset_green','frame_asset_mint','frame_asset_purple','frame_asset_gold',
  'frame_asset_gold_crown','frame_asset_christmas','frame_asset_halloween','frame_neon','frame_gold','frame_starter'
);

-- Google Play product grants. These are inert until identical INAPP SKUs are configured in Play Console.
insert into public.store_product_grants(product_id,grant_type,grant_key,amount) values
 ('profile_frame_ocean','style','frame_round_ocean',0),
 ('profile_frame_botanic','style','frame_round_botanic',0),
 ('profile_frame_lilac','style','frame_round_lilac',0),
 ('profile_frame_rose','style','frame_round_rose',0)
on conflict(product_id,grant_type,grant_key) do update set amount=excluded.amount;
insert into public.store_catalog_config(product_id,enabled) values
 ('profile_frame_ocean',true),('profile_frame_botanic',true),('profile_frame_lilac',true),('profile_frame_rose',true)
on conflict(product_id) do update set enabled=true,updated_at=now();

-- VIP frame needs neither a purchasable listing nor an inventory grant.
create or replace function public.equip_shop_item(p_item_id text)
returns jsonb language plpgsql security definer set search_path to 'public','pg_temp' as $$
declare v_uid uuid:=auth.uid(); v_item public.shop_items%rowtype; v_vip boolean;
begin
 if v_uid is null then raise exception 'unauthorized'; end if;
 select * into v_item from public.shop_items where id=p_item_id;
 if not found then raise exception 'item_not_found'; end if;
 select is_vip into v_vip from public.profiles where id=v_uid;
 if v_item.kind='profile_frame' and p_item_id='frame_round_golden_avatar' then
   if not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
 elsif not exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then
   raise exception 'not_owned';
 end if;
 insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
 update public.user_equipped_cosmetics set
   profile_frame_id=case when v_item.kind='profile_frame' then p_item_id else profile_frame_id end,
   name_style_id=case when v_item.kind='name_style' then p_item_id else name_style_id end,
   game_theme_id=case when v_item.kind='game_theme' then p_item_id else game_theme_id end,
   keyboard_theme_id=case when v_item.kind='keyboard_theme' then p_item_id else keyboard_theme_id end,
   victory_effect_id=case when v_item.kind='victory_effect' then p_item_id else victory_effect_id end,
   emoji_pack_id=case when v_item.kind='emoji_pack' then p_item_id else emoji_pack_id end,
   mascot_id=case when v_item.kind='mascot' then p_item_id else mascot_id end,
   updated_at=now() where user_id=v_uid;
 return jsonb_build_object('success',true,'item_id',p_item_id,'kind',v_item.kind);
end $$;

revoke all on function public.equip_shop_item(text) from public,anon;
grant execute on function public.equip_shop_item(text) to authenticated,service_role;
revoke all on function public.profile_frame_default_v1(text) from public,anon,authenticated;
grant execute on function public.profile_frame_default_v1(text) to service_role;

-- Security-invoker view exposes the requested model without duplicating ownership state.
create or replace view public.profile_frame_state_v1 with (security_invoker=true) as
select p.id as user_id,
  coalesce(array_agg(i.item_id) filter (where s.kind='profile_frame'),array[]::text[]) as owned_profile_frames,
  e.profile_frame_id as equipped_profile_frame,
  p.default_profile_frame_id as default_profile_frame,
  coalesce(p.is_vip,false) as vip_pro_frame_access
from public.profiles p
left join public.user_inventory i on i.user_id=p.id
left join public.shop_items s on s.id=i.item_id
left join public.user_equipped_cosmetics e on e.user_id=p.id
group by p.id,e.profile_frame_id,p.default_profile_frame_id,p.is_vip;

select pg_notify('pgrst','reload schema');
