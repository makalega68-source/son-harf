-- SON HARF economy + VIP store
create table if not exists public.shop_items (
  id text primary key,
  kind text not null check (kind in ('profile_frame','name_style','game_theme','keyboard_theme','victory_effect','emoji_pack')),
  name_tr text not null,
  name_en text not null,
  description_tr text not null default '',
  description_en text not null default '',
  diamond_price integer not null check (diamond_price >= 0),
  vip_only boolean not null default false,
  active boolean not null default true,
  sort_order integer not null default 0
);

create table if not exists public.user_inventory (
  user_id uuid not null references public.profiles(id) on delete cascade,
  item_id text not null references public.shop_items(id) on delete cascade,
  acquired_at timestamptz not null default now(),
  primary key (user_id, item_id)
);

create table if not exists public.user_equipped_cosmetics (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  profile_frame_id text references public.shop_items(id),
  name_style_id text references public.shop_items(id),
  game_theme_id text references public.shop_items(id),
  keyboard_theme_id text references public.shop_items(id),
  victory_effect_id text references public.shop_items(id),
  emoji_pack_id text references public.shop_items(id),
  updated_at timestamptz not null default now()
);

create table if not exists public.diamond_ledger (
  id bigserial primary key,
  user_id uuid not null references public.profiles(id) on delete cascade,
  delta integer not null,
  reason text not null,
  item_id text references public.shop_items(id),
  created_at timestamptz not null default now()
);

create table if not exists public.vip_monthly_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  period_start date not null,
  diamonds integer not null default 400,
  claimed_at timestamptz not null default now(),
  primary key (user_id, period_start)
);

alter table public.shop_items enable row level security;
alter table public.user_inventory enable row level security;
alter table public.user_equipped_cosmetics enable row level security;
alter table public.diamond_ledger enable row level security;
alter table public.vip_monthly_claims enable row level security;

drop policy if exists shop_items_read on public.shop_items;
create policy shop_items_read on public.shop_items for select to authenticated using (active = true);
drop policy if exists inventory_read_own on public.user_inventory;
create policy inventory_read_own on public.user_inventory for select to authenticated using (user_id = auth.uid());
drop policy if exists equipped_read_own on public.user_equipped_cosmetics;
create policy equipped_read_own on public.user_equipped_cosmetics for select to authenticated using (user_id = auth.uid());
drop policy if exists ledger_read_own on public.diamond_ledger;
create policy ledger_read_own on public.diamond_ledger for select to authenticated using (user_id = auth.uid());
drop policy if exists vip_claims_read_own on public.vip_monthly_claims;
create policy vip_claims_read_own on public.vip_monthly_claims for select to authenticated using (user_id = auth.uid());

insert into public.shop_items(id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,sort_order) values
('frame_neon','profile_frame','Neon Çerçeve','Neon Frame','Profil fotoğrafına neon çerçeve.','Neon profile frame.',250,false,10),
('frame_gold','profile_frame','Altın Çerçeve','Gold Frame','VIP altın profil çerçevesi.','VIP gold profile frame.',450,true,20),
('name_cyan','name_style','Siyan İsim','Cyan Name','Oyuncu adını siyan vurgular.','Cyan player-name accent.',180,false,30),
('theme_aurora','game_theme','Aurora Tema','Aurora Theme','Oyun alanı için Aurora görünümü.','Aurora game-board appearance.',500,false,40),
('keyboard_neon','keyboard_theme','Neon Klavye','Neon Keyboard','Kelime klavyesi için neon görünüm.','Neon word-keyboard appearance.',350,false,50),
('victory_crown','victory_effect','Taç Zaferi','Crown Victory','Maç sonunda taç zafer efekti.','Crown victory effect after a win.',600,true,60),
('emoji_vip','emoji_pack','VIP Emoji','VIP Emoji Pack','VIP sohbet emoji paketi.','VIP chat emoji pack.',300,true,70)
on conflict (id) do update set
kind=excluded.kind,name_tr=excluded.name_tr,name_en=excluded.name_en,description_tr=excluded.description_tr,description_en=excluded.description_en,
diamond_price=excluded.diamond_price,vip_only=excluded.vip_only,active=true,sort_order=excluded.sort_order;

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_item public.shop_items%rowtype; v_balance integer; v_vip boolean;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items where id=p_item_id and active=true;
  if not found then raise exception 'item_not_found'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;
  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;
  if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
  if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
  update public.profiles set diamonds=diamonds-v_item.diamond_price where id=v_uid;
  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);
  insert into public.diamond_ledger(user_id,delta,reason,item_id) values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  return jsonb_build_object('success',true,'item_id',p_item_id,'diamonds',v_balance-v_item.diamond_price);
end $$;

create or replace function public.equip_shop_item(p_item_id text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_item public.shop_items%rowtype;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items where id=p_item_id and active=true;
  if not found then raise exception 'item_not_found'; end if;
  if not exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'not_owned'; end if;
  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics set
    profile_frame_id=case when v_item.kind='profile_frame' then p_item_id else profile_frame_id end,
    name_style_id=case when v_item.kind='name_style' then p_item_id else name_style_id end,
    game_theme_id=case when v_item.kind='game_theme' then p_item_id else game_theme_id end,
    keyboard_theme_id=case when v_item.kind='keyboard_theme' then p_item_id else keyboard_theme_id end,
    victory_effect_id=case when v_item.kind='victory_effect' then p_item_id else victory_effect_id end,
    emoji_pack_id=case when v_item.kind='emoji_pack' then p_item_id else emoji_pack_id end,
    updated_at=now()
  where user_id=v_uid;
  return jsonb_build_object('success',true,'item_id',p_item_id,'kind',v_item.kind);
end $$;

create or replace function public.claim_vip_monthly_diamonds()
returns jsonb language plpgsql security definer set search_path=public,pg_temp as $$
declare v_uid uuid:=auth.uid(); v_period date:=date_trunc('month',now())::date; v_vip boolean; v_balance integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select is_vip,diamonds into v_vip,v_balance from public.profiles where id=v_uid for update;
  if not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
  begin
    insert into public.vip_monthly_claims(user_id,period_start,diamonds) values(v_uid,v_period,400);
  exception when unique_violation then raise exception 'already_claimed'; end;
  update public.profiles set diamonds=diamonds+400 where id=v_uid;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,400,'vip_monthly');
  return jsonb_build_object('success',true,'diamonds',v_balance+400,'granted',400);
end $$;

revoke all on function public.purchase_shop_item(text) from public;
revoke all on function public.equip_shop_item(text) from public;
revoke all on function public.claim_vip_monthly_diamonds() from public;
grant execute on function public.purchase_shop_item(text) to authenticated;
grant execute on function public.equip_shop_item(text) to authenticated;
grant execute on function public.claim_vip_monthly_diamonds() to authenticated;

grant select on public.shop_items to authenticated;
grant select on public.user_inventory to authenticated;
grant select on public.user_equipped_cosmetics to authenticated;
grant select on public.diamond_ledger to authenticated;
grant select on public.vip_monthly_claims to authenticated;
