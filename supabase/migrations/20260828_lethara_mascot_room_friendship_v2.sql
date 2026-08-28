-- Lethara mascot room and friendship v2.
-- Friendship and room customization are cosmetic/lore-only and never affect ranked-match power.

alter table public.user_mascot_progress
  add column if not exists friendship_xp integer not null default 0 check (friendship_xp >= 0),
  add column if not exists friendship_level integer not null default 1 check (friendship_level between 1 and 30),
  add column if not exists selected_room_item text not null default 'star_window';

create table if not exists public.mascot_room_catalog (
  id text primary key,
  name_tr text not null,
  name_en text not null,
  description_tr text not null default '',
  description_en text not null default '',
  unlock_friendship_level integer not null default 1 check (unlock_friendship_level between 1 and 30),
  icon text not null default '✦',
  sort_order integer not null default 0,
  active boolean not null default true
);

insert into public.mascot_room_catalog(
  id,name_tr,name_en,description_tr,description_en,unlock_friendship_level,icon,sort_order,active
) values
('star_window','Yıldız Penceresi','Star Window','Lethara gecesini ve Söz Dokusu kıvılcımlarını odaya taşır.','Brings Lethara night and Word Weave sparks into the room.',1,'✦',1,true),
('seal_rug','Mühür Halısı','Seal Rug','Altı Mühür desenleriyle işlenmiş sıcak bir oda zemini.','A warm room rug woven with the patterns of the Six Seals.',2,'◇',2,true),
('moon_lantern','Ay Feneri','Moon Lantern','Mor Ay ışığını yumuşak bir parıltıya dönüştürür.','Turns Violet Moon light into a soft glow.',4,'☾',3,true),
('memory_book','Hafıza Kitabı','Memory Book','Bulunan anı parçalarının yankılarını sessizce saklar.','Quietly preserves echoes of recovered memory fragments.',7,'📖',4,true),
('memory_crystal','Hafıza Kristali','Memory Crystal','Dostluk güçlendikçe daha parlak titreşen eski bir kristal.','An ancient crystal that glows brighter as friendship grows.',10,'🔮',5,true),
('celestial_gate','Göksel Geçit','Celestial Gate','Yüksek dostlukta açılan prestij odası dekoru; yalnızca görseldir.','A prestige room decoration unlocked by deep friendship; visual only.',15,'✧',6,true)
on conflict(id) do update set
  name_tr=excluded.name_tr,name_en=excluded.name_en,
  description_tr=excluded.description_tr,description_en=excluded.description_en,
  unlock_friendship_level=excluded.unlock_friendship_level,
  icon=excluded.icon,sort_order=excluded.sort_order,active=excluded.active;

alter table public.mascot_room_catalog enable row level security;
drop policy if exists mascot_room_catalog_read on public.mascot_room_catalog;
create policy mascot_room_catalog_read on public.mascot_room_catalog
for select to authenticated using (active=true);

create table if not exists public.user_mascot_daily_bond (
  user_id uuid not null references public.profiles(id) on delete cascade,
  mascot_id text not null,
  bond_date date not null default current_date,
  loved boolean not null default false,
  played boolean not null default false,
  groomed boolean not null default false,
  completion_rewarded boolean not null default false,
  primary key(user_id,mascot_id,bond_date)
);
alter table public.user_mascot_daily_bond enable row level security;
drop policy if exists mascot_daily_bond_self_read on public.user_mascot_daily_bond;
create policy mascot_daily_bond_self_read on public.user_mascot_daily_bond
for select to authenticated using ((select auth.uid())=user_id);

create or replace function public.get_mascot_room_state_v2(p_mascot_id text)
returns table(
  mascot_id text,
  friendship_xp integer,
  friendship_level integer,
  selected_room_item text,
  loved_today boolean,
  played_today boolean,
  groomed_today boolean,
  daily_bond_completed boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $room$
declare v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform public.ensure_mascot_progress_v1(p_mascot_id);

  insert into public.user_mascot_daily_bond(user_id,mascot_id,bond_date)
  values(v_uid,p_mascot_id,current_date)
  on conflict(user_id,mascot_id,bond_date) do nothing;

  return query
  select p.mascot_id,p.friendship_xp,p.friendship_level,p.selected_room_item,
         b.loved,b.played,b.groomed,(b.loved and b.played and b.groomed)
  from public.user_mascot_progress p
  join public.user_mascot_daily_bond b
    on b.user_id=p.user_id and b.mascot_id=p.mascot_id and b.bond_date=current_date
  where p.user_id=v_uid and p.mascot_id=p_mascot_id;
end $room$;
revoke all on function public.get_mascot_room_state_v2(text) from public,anon;
grant execute on function public.get_mascot_room_state_v2(text) to authenticated;

create or replace function public.care_mascot_v2(p_mascot_id text,p_action text)
returns table(
  success boolean,
  mascot_id text,
  action text,
  happiness integer,
  fullness integer,
  energy integer,
  friendship_xp integer,
  friendship_level integer,
  friendship_gained integer,
  daily_bond_completed boolean,
  daily_bonus_awarded boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $care$
declare
  v_uid uuid:=auth.uid();
  v_action text:=lower(trim(coalesce(p_action,'')));
  v_happiness integer;
  v_fullness integer;
  v_energy integer;
  v_friendship_xp integer;
  v_friendship_level integer;
  v_base_gain integer:=0;
  v_bonus integer:=0;
  v_completed boolean:=false;
  v_rewarded boolean:=false;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if v_action not in ('love','play','groom') then raise exception 'invalid_care_action'; end if;
  perform public.ensure_mascot_progress_v1(p_mascot_id);

  v_base_gain:=case v_action when 'love' then 3 when 'play' then 4 else 2 end;

  insert into public.user_mascot_daily_bond(user_id,mascot_id,bond_date,loved,played,groomed)
  values(
    v_uid,p_mascot_id,current_date,
    v_action='love',v_action='play',v_action='groom'
  )
  on conflict(user_id,mascot_id,bond_date) do update set
    loved=public.user_mascot_daily_bond.loved or excluded.loved,
    played=public.user_mascot_daily_bond.played or excluded.played,
    groomed=public.user_mascot_daily_bond.groomed or excluded.groomed;

  select loved and played and groomed,completion_rewarded
  into v_completed,v_rewarded
  from public.user_mascot_daily_bond
  where user_id=v_uid and mascot_id=p_mascot_id and bond_date=current_date
  for update;

  if v_completed and not v_rewarded then
    v_bonus:=10;
    update public.user_mascot_daily_bond
    set completion_rewarded=true
    where user_id=v_uid and mascot_id=p_mascot_id and bond_date=current_date;
  end if;

  update public.user_mascot_progress
  set happiness=least(100,happiness+case v_action when 'love' then 8 when 'play' then 6 else 4 end),
      fullness=least(100,fullness+case when v_action='groom' then 2 else 0 end),
      energy=greatest(0,least(100,energy+case v_action when 'love' then 2 when 'play' then -4 else 5 end)),
      friendship_xp=friendship_xp+v_base_gain+v_bonus,
      friendship_level=least(30,greatest(1,((friendship_xp+v_base_gain+v_bonus)/40)+1)),
      memory_fragments=least(120,memory_fragments+case when v_bonus>0 then 1 else 0 end),
      updated_at=now()
  where user_id=v_uid and user_mascot_progress.mascot_id=p_mascot_id
  returning user_mascot_progress.happiness,user_mascot_progress.fullness,user_mascot_progress.energy,
            user_mascot_progress.friendship_xp,user_mascot_progress.friendship_level
  into v_happiness,v_fullness,v_energy,v_friendship_xp,v_friendship_level;

  return query select true,p_mascot_id,v_action,v_happiness,v_fullness,v_energy,
                      v_friendship_xp,v_friendship_level,v_base_gain+v_bonus,v_completed,(v_bonus>0);
end $care$;
revoke all on function public.care_mascot_v2(text,text) from public,anon;
grant execute on function public.care_mascot_v2(text,text) to authenticated;

create or replace function public.set_mascot_room_item_v2(p_mascot_id text,p_room_item text)
returns table(
  mascot_id text,
  friendship_xp integer,
  friendship_level integer,
  selected_room_item text,
  loved_today boolean,
  played_today boolean,
  groomed_today boolean,
  daily_bond_completed boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $select$
declare
  v_uid uuid:=auth.uid();
  v_required integer;
  v_level integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform public.ensure_mascot_progress_v1(p_mascot_id);

  select unlock_friendship_level into v_required
  from public.mascot_room_catalog
  where id=p_room_item and active=true;
  if not found then raise exception 'room_item_not_found'; end if;

  select friendship_level into v_level
  from public.user_mascot_progress
  where user_id=v_uid and mascot_id=p_mascot_id
  for update;

  if v_level<v_required then raise exception 'friendship_level_required'; end if;

  update public.user_mascot_progress
  set selected_room_item=p_room_item,updated_at=now()
  where user_id=v_uid and mascot_id=p_mascot_id;

  return query select * from public.get_mascot_room_state_v2(p_mascot_id);
end $select$;
revoke all on function public.set_mascot_room_item_v2(text,text) from public,anon;
grant execute on function public.set_mascot_room_item_v2(text,text) to authenticated;
