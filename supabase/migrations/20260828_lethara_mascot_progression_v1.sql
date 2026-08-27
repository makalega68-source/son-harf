-- Lethara mascot progression, memory and fruit economy v1.
-- Competitive rule: mascot progression is cosmetic/lore only and never changes ranked match power.

create table if not exists public.mascot_fruit_catalog (
  id text primary key,
  name_tr text not null,
  name_en text not null,
  description_tr text not null default '',
  description_en text not null default '',
  xp_reward integer not null check (xp_reward in (3,10,20,30)),
  son_coin_price integer not null default 0 check (son_coin_price >= 0),
  is_magic boolean not null default false,
  sort_order integer not null default 0,
  active boolean not null default true
);

insert into public.mascot_fruit_catalog(
  id,name_tr,name_en,description_tr,description_en,xp_reward,son_coin_price,is_magic,sort_order,active
) values
('lethara_apple','Lethara Elması','Lethara Apple','Günlük bakım meyvesi. Maskota +3 XP verir; günde en fazla 3 kez ücretsiz kullanılabilir.','Daily care fruit. Grants +3 XP; usable free up to 3 times per day.',3,0,false,1,true),
('moon_fruit','Ay Meyvesi','Moon Fruit','Ay ışığında olgunlaşan büyülü meyve. +10 maskot XP.','A magical fruit ripened in moonlight. +10 mascot XP.',10,20,true,10,true),
('star_fruit','Yıldız Meyvesi','Star Fruit','Söz Dokusu kıvılcımları taşıyan nadir meyve. +20 maskot XP.','A rare fruit carrying sparks of the Word Weave. +20 mascot XP.',20,45,true,20,true),
('seal_fruit','Mühür Meyvesi','Seal Fruit','Eski mühür bahçelerinden kalan yoğun büyülü meyve. +30 maskot XP.','A potent magical fruit from the ancient seal gardens. +30 mascot XP.',30,70,true,30,true)
on conflict (id) do update set
 name_tr=excluded.name_tr,name_en=excluded.name_en,
 description_tr=excluded.description_tr,description_en=excluded.description_en,
 xp_reward=excluded.xp_reward,son_coin_price=excluded.son_coin_price,
 is_magic=excluded.is_magic,sort_order=excluded.sort_order,active=excluded.active;

alter table public.mascot_fruit_catalog enable row level security;
drop policy if exists mascot_fruit_catalog_read on public.mascot_fruit_catalog;
create policy mascot_fruit_catalog_read on public.mascot_fruit_catalog for select to authenticated using (active=true);

create table if not exists public.user_mascot_progress (
  user_id uuid not null references public.profiles(id) on delete cascade,
  mascot_id text not null,
  pet_name text not null default 'Dostum',
  total_xp integer not null default 0 check (total_xp >= 0),
  level integer not null default 1 check (level >= 1),
  happiness integer not null default 82 check (happiness between 0 and 100),
  fullness integer not null default 70 check (fullness between 0 and 100),
  energy integer not null default 90 check (energy between 0 and 100),
  memory_fragments integer not null default 0 check (memory_fragments between 0 and 120),
  game_xp_synced integer not null default 0 check (game_xp_synced >= 0),
  updated_at timestamptz not null default now(),
  primary key(user_id,mascot_id)
);
alter table public.user_mascot_progress enable row level security;
drop policy if exists mascot_progress_self_read on public.user_mascot_progress;
create policy mascot_progress_self_read on public.user_mascot_progress for select to authenticated using ((select auth.uid())=user_id);

create table if not exists public.user_mascot_fruit_inventory (
  user_id uuid not null references public.profiles(id) on delete cascade,
  fruit_id text not null references public.mascot_fruit_catalog(id),
  quantity integer not null default 0 check (quantity >= 0),
  updated_at timestamptz not null default now(),
  primary key(user_id,fruit_id)
);
alter table public.user_mascot_fruit_inventory enable row level security;
drop policy if exists mascot_fruit_inventory_self_read on public.user_mascot_fruit_inventory;
create policy mascot_fruit_inventory_self_read on public.user_mascot_fruit_inventory for select to authenticated using ((select auth.uid())=user_id);

create table if not exists public.user_mascot_daily_care (
  user_id uuid not null references public.profiles(id) on delete cascade,
  care_date date not null default current_date,
  normal_fruit_used integer not null default 0 check (normal_fruit_used between 0 and 3),
  primary key(user_id,care_date)
);
alter table public.user_mascot_daily_care enable row level security;
drop policy if exists mascot_daily_care_self_read on public.user_mascot_daily_care;
create policy mascot_daily_care_self_read on public.user_mascot_daily_care for select to authenticated using ((select auth.uid())=user_id);

create or replace function public.mascot_id_is_playable_v1(p_mascot_id text)
returns boolean
language sql
security definer
set search_path=public,pg_temp
as $$
  select p_mascot_id='mascot_white'
      or exists(select 1 from public.shop_items where id=p_mascot_id and kind='mascot' and active=true);
$$;
revoke all on function public.mascot_id_is_playable_v1(text) from public,anon;
grant execute on function public.mascot_id_is_playable_v1(text) to authenticated;

create or replace function public.ensure_mascot_progress_v1(p_mascot_id text)
returns void
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if not public.mascot_id_is_playable_v1(p_mascot_id) then raise exception 'mascot_not_available'; end if;
  if p_mascot_id<>'mascot_white' and not exists(
    select 1 from public.user_inventory where user_id=v_uid and item_id=p_mascot_id
  ) then raise exception 'mascot_not_owned'; end if;

  insert into public.user_mascot_progress(user_id,mascot_id,pet_name)
  values(v_uid,p_mascot_id,case when p_mascot_id='mascot_white' then 'Lyra' when p_mascot_id='mascot_chibi_wizard' then 'Neris' else 'Dostum' end)
  on conflict(user_id,mascot_id) do nothing;
end $$;
revoke all on function public.ensure_mascot_progress_v1(text) from public,anon;
grant execute on function public.ensure_mascot_progress_v1(text) to authenticated;

create or replace function public.sync_mascot_game_xp_v1(p_mascot_id text)
returns integer
language plpgsql
security definer
set search_path=public,pg_temp
as $fn$
declare
  v_uid uuid:=auth.uid();
  v_game_xp integer:=0;
  v_bond_xp integer:=0;
  v_synced integer:=0;
  v_delta integer:=0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform public.ensure_mascot_progress_v1(p_mascot_id);

  select (
    coalesce(wins,0)*120
    + coalesce(losses,0)*35
    + coalesce(valid_words,0)*3
    + coalesce(total_rounds,0)*5
  )::integer
  into v_game_xp
  from public.profiles
  where id=v_uid;

  -- Ten Hatırlatıcı XP become one companion bond XP. This keeps mascot progression meaningful
  -- without turning ranked play into a pay-to-win or runaway leveling system.
  v_bond_xp:=greatest(0,coalesce(v_game_xp,0)/10);

  select game_xp_synced into v_synced
  from public.user_mascot_progress
  where user_id=v_uid and mascot_id=p_mascot_id
  for update;

  v_delta:=greatest(0,v_bond_xp-coalesce(v_synced,0));
  if v_delta>0 then
    update public.user_mascot_progress
    set total_xp=total_xp+v_delta,
        level=greatest(1,((total_xp+v_delta)/100)+1),
        memory_fragments=least(120,(total_xp+v_delta)/10),
        game_xp_synced=v_bond_xp,
        updated_at=now()
    where user_id=v_uid and mascot_id=p_mascot_id;
  elsif v_bond_xp>coalesce(v_synced,0) then
    update public.user_mascot_progress
    set game_xp_synced=v_bond_xp,updated_at=now()
    where user_id=v_uid and mascot_id=p_mascot_id;
  end if;

  return v_delta;
end $fn$;
revoke all on function public.sync_mascot_game_xp_v1(text) from public,anon;
grant execute on function public.sync_mascot_game_xp_v1(text) to authenticated;

create or replace function public.get_mascot_progress_v1(p_mascot_id text)
returns table(
  mascot_id text,pet_name text,total_xp integer,level integer,happiness integer,fullness integer,energy integer,
  memory_fragments integer,normal_fruit_used_today integer,normal_fruit_daily_limit integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare v_uid uuid:=auth.uid();
begin
  perform public.ensure_mascot_progress_v1(p_mascot_id);
  perform public.sync_mascot_game_xp_v1(p_mascot_id);
  return query
  select p.mascot_id,p.pet_name,p.total_xp,p.level,p.happiness,p.fullness,p.energy,p.memory_fragments,
         coalesce(c.normal_fruit_used,0),3
  from public.user_mascot_progress p
  left join public.user_mascot_daily_care c on c.user_id=p.user_id and c.care_date=current_date
  where p.user_id=v_uid and p.mascot_id=p_mascot_id;
end $$;
revoke all on function public.get_mascot_progress_v1(text) from public,anon;
grant execute on function public.get_mascot_progress_v1(text) to authenticated;

create or replace function public.rename_mascot_v1(p_mascot_id text,p_pet_name text)
returns table(
  mascot_id text,pet_name text,total_xp integer,level integer,happiness integer,fullness integer,energy integer,
  memory_fragments integer,normal_fruit_used_today integer,normal_fruit_daily_limit integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare v_uid uuid:=auth.uid(); v_name text:=trim(coalesce(p_pet_name,''));
begin
  perform public.ensure_mascot_progress_v1(p_mascot_id);
  if char_length(v_name)<2 or char_length(v_name)>18 then raise exception 'invalid_mascot_name'; end if;
  update public.user_mascot_progress set pet_name=v_name,updated_at=now()
  where user_id=v_uid and user_mascot_progress.mascot_id=p_mascot_id;
  return query select * from public.get_mascot_progress_v1(p_mascot_id);
end $$;
revoke all on function public.rename_mascot_v1(text,text) from public,anon;
grant execute on function public.rename_mascot_v1(text,text) to authenticated;

create or replace function public.buy_mascot_fruit_v1(p_fruit_id text,p_quantity integer default 1)
returns table(
  success boolean,fruit_id text,quantity integer,inventory_quantity integer,son_coin_spent integer,son_coin_balance integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid(); v_price integer; v_magic boolean; v_cost integer; v_balance integer; v_qty integer:=greatest(1,least(coalesce(p_quantity,1),20)); v_inventory integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select son_coin_price,is_magic into v_price,v_magic from public.mascot_fruit_catalog where id=p_fruit_id and active=true;
  if not found then raise exception 'fruit_not_found'; end if;
  if not v_magic or v_price<=0 then raise exception 'fruit_not_purchasable'; end if;
  v_cost:=v_price*v_qty;
  select diamonds into v_balance from public.profiles where id=v_uid for update;
  if coalesce(v_balance,0)<v_cost then raise exception 'insufficient_diamonds'; end if;
  update public.profiles set diamonds=diamonds-v_cost,updated_at=now() where id=v_uid returning diamonds into v_balance;
  insert into public.user_mascot_fruit_inventory(user_id,fruit_id,quantity)
  values(v_uid,p_fruit_id,v_qty)
  on conflict(user_id,fruit_id) do update set quantity=public.user_mascot_fruit_inventory.quantity+excluded.quantity,updated_at=now()
  returning public.user_mascot_fruit_inventory.quantity into v_inventory;
  if to_regclass('public.diamond_ledger') is not null then
    insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,-v_cost,'mascot_magic_fruit');
  end if;
  return query select true,p_fruit_id,v_qty,v_inventory,v_cost,v_balance;
end $$;
revoke all on function public.buy_mascot_fruit_v1(text,integer) from public,anon;
grant execute on function public.buy_mascot_fruit_v1(text,integer) to authenticated;

create or replace function public.feed_mascot_v1(p_mascot_id text,p_fruit_id text)
returns table(
  success boolean,mascot_id text,fruit_id text,xp_gained integer,total_xp integer,level integer,memory_fragments integer,
  fullness integer,happiness integer,energy integer,inventory_left integer,normal_fruit_used_today integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid(); v_xp integer; v_magic boolean; v_new_xp integer; v_level integer; v_memory integer;
  v_inventory integer:=0; v_normal_used integer:=0; v_fullness integer; v_happiness integer; v_energy integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform public.ensure_mascot_progress_v1(p_mascot_id);
  select xp_reward,is_magic into v_xp,v_magic from public.mascot_fruit_catalog where id=p_fruit_id and active=true;
  if not found then raise exception 'fruit_not_found'; end if;

  if v_magic then
    select quantity into v_inventory from public.user_mascot_fruit_inventory where user_id=v_uid and fruit_id=p_fruit_id for update;
    if coalesce(v_inventory,0)<=0 then raise exception 'fruit_not_owned'; end if;
    update public.user_mascot_fruit_inventory set quantity=quantity-1,updated_at=now()
      where user_id=v_uid and fruit_id=p_fruit_id returning quantity into v_inventory;
  else
    insert into public.user_mascot_daily_care(user_id,care_date,normal_fruit_used)
      values(v_uid,current_date,0) on conflict(user_id,care_date) do nothing;
    select normal_fruit_used into v_normal_used from public.user_mascot_daily_care where user_id=v_uid and care_date=current_date for update;
    if v_normal_used>=3 then raise exception 'normal_fruit_daily_limit'; end if;
    update public.user_mascot_daily_care set normal_fruit_used=normal_fruit_used+1
      where user_id=v_uid and care_date=current_date returning normal_fruit_used into v_normal_used;
  end if;

  update public.user_mascot_progress
  set total_xp=total_xp+v_xp,
      level=greatest(1,((total_xp+v_xp)/100)+1),
      memory_fragments=least(120,(total_xp+v_xp)/10),
      fullness=least(100,fullness+case when v_magic then 14 else 8 end),
      happiness=least(100,happiness+case when v_magic then 6 else 3 end),
      energy=least(100,energy+case when v_magic then 4 else 2 end),
      updated_at=now()
  where user_id=v_uid and user_mascot_progress.mascot_id=p_mascot_id
  returning user_mascot_progress.total_xp,user_mascot_progress.level,user_mascot_progress.memory_fragments,
            user_mascot_progress.fullness,user_mascot_progress.happiness,user_mascot_progress.energy
  into v_new_xp,v_level,v_memory,v_fullness,v_happiness,v_energy;

  return query select true,p_mascot_id,p_fruit_id,v_xp,v_new_xp,v_level,v_memory,v_fullness,v_happiness,v_energy,v_inventory,v_normal_used;
end $$;
revoke all on function public.feed_mascot_v1(text,text) from public,anon;
grant execute on function public.feed_mascot_v1(text,text) to authenticated;

-- Canonical mapping of the two runtime-verified models.
update public.shop_items set
  name_tr='Lyra — Beyaz Mühür',
  name_en='Lyra — White Seal',
  description_tr='Altı Mühür’den Lyra’nın herkese açık başlangıç formu. Hikâye ve yoldaşlık içindir; maç gücü vermez.',
  description_en='Lyra of the Six Seals in her free starter form. Story and companionship only; no match power.'
where id='mascot_white' and kind='mascot';

update public.shop_items set
  name_tr='Neris — Gölge Bilgesi',
  name_en='Neris — Shadow Sage',
  description_tr='Altı Mühür’den Neris. Lisanslı 3D maskot; görünüm, hikâye ve yoldaşlık içindir, maç gücü vermez.',
  description_en='Neris of the Six Seals. Licensed 3D mascot for story and companionship; grants no match power.'
where id='mascot_chibi_wizard' and kind='mascot';

-- Future canonical Seals are staged with planned Son Coin pricing but remain inactive until
-- a distinct licensed 3D asset passes the same production runtime gate as Lyra/Neris.
insert into public.shop_items(
  id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order
) values
('mascot_kael','mascot','Kael — Koruyucu','Kael — Guardian','Sadakat ve mühür kalkanlarının koruyucusu. 3D form runtime onayı bekliyor.','Guardian of loyalty and seal shields. Awaiting 3D runtime approval.',850,false,false,405),
('mascot_ryvan','mascot','Ryvan — Fırtına Ustası','Ryvan — Storm Master','Fırtına ve yıldırım ritminin ustası. 3D form runtime onayı bekliyor.','Master of storm and lightning rhythm. Awaiting 3D runtime approval.',900,false,false,410),
('mascot_mivo','mascot','Mivo — Neşe Büyücüsü','Mivo — Joy Mage','Mizah ve yaratıcı büyünün yaramaz ustası. 3D form runtime onayı bekliyor.','Mischievous master of humor and creative magic. Awaiting 3D runtime approval.',800,false,false,415),
('mascot_selen','mascot','Selen — Sessiz Kâhin','Selen — Silent Seer','Söylenmemiş kelimelerin sessiz kâhini. 3D form runtime onayı bekliyor.','Silent seer of unspoken words. Awaiting 3D runtime approval.',950,false,false,420)
on conflict(id) do update set
  name_tr=excluded.name_tr,name_en=excluded.name_en,
  description_tr=excluded.description_tr,description_en=excluded.description_en,
  diamond_price=excluded.diamond_price,vip_only=excluded.vip_only,
  active=false,sort_order=excluded.sort_order;
