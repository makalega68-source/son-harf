-- Son Harf owner gameplay accounts + admin platform capacity (2026-08-29)
-- Keeps owner gameplay privileges separate from admin authorization.

create table if not exists public.owner_game_accounts (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  lifetime_vip boolean not null default true,
  unlimited_diamonds boolean not null default true,
  unlimited_son_coin boolean not null default true,
  active boolean not null default true,
  notes text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  updated_by uuid null references auth.users(id) on delete set null
);
alter table public.owner_game_accounts enable row level security;
revoke all on public.owner_game_accounts from anon, authenticated;

insert into public.owner_game_accounts(
  user_id,lifetime_vip,unlimited_diamonds,unlimited_son_coin,active,notes,updated_by
)
select u.id,true,true,true,true,'Kurucu rekabet hesabı',u.id
from auth.users u
where lower(u.email)=lower('makalega68@gmail.com')
on conflict(user_id) do update set
  lifetime_vip=true,
  unlimited_diamonds=true,
  unlimited_son_coin=true,
  active=true,
  notes=case when public.owner_game_accounts.notes='' then 'Kurucu rekabet hesabı' else public.owner_game_accounts.notes end,
  updated_at=now();

update public.profiles p
set is_vip=true, updated_at=now()
where exists (
  select 1 from public.owner_game_accounts o
  where o.user_id=p.id and o.active and o.lifetime_vip
);

create or replace function public.enforce_owner_lifetime_vip_v1()
returns trigger
language plpgsql
security invoker
set search_path=public,pg_temp
as $$
begin
  if exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=new.id and o.active and o.lifetime_vip
  ) then
    new.is_vip := true;
  end if;
  return new;
end
$$;

drop trigger if exists trg_enforce_owner_lifetime_vip_v1 on public.profiles;
create trigger trg_enforce_owner_lifetime_vip_v1
before insert or update of is_vip on public.profiles
for each row execute function public.enforce_owner_lifetime_vip_v1();

create or replace function public.admin_owner_accounts_v1()
returns table(
  user_id uuid,
  email text,
  display_name text,
  lifetime_vip boolean,
  unlimited_diamonds boolean,
  unlimited_son_coin boolean,
  active boolean,
  current_diamonds integer,
  rating integer,
  updated_at timestamptz
)
language plpgsql
security definer
set search_path=public,auth,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select o.user_id,u.email::text,p.display_name,o.lifetime_vip,o.unlimited_diamonds,
         o.unlimited_son_coin,o.active,p.diamonds,p.rating,o.updated_at
  from public.owner_game_accounts o
  join auth.users u on u.id=o.user_id
  join public.profiles p on p.id=o.user_id
  order by o.created_at,o.user_id;
end
$$;

create or replace function public.admin_set_owner_account_v1(
  p_email text,
  p_lifetime_vip boolean,
  p_unlimited_diamonds boolean,
  p_unlimited_son_coin boolean,
  p_active boolean
)
returns jsonb
language plpgsql
security definer
set search_path=public,auth,pg_temp
as $$
declare
  v_uid uuid;
  v_email text:=lower(trim(coalesce(p_email,'')));
  v_before jsonb;
  v_active_count integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_email='' then raise exception 'invalid_email'; end if;

  select u.id into v_uid from auth.users u where lower(u.email)=v_email limit 1;
  if v_uid is null then raise exception 'user_not_found'; end if;

  if coalesce(p_active,false) and not exists(select 1 from public.owner_game_accounts where user_id=v_uid and active) then
    select count(*) into v_active_count from public.owner_game_accounts where active;
    if v_active_count >= 5 then raise exception 'owner_account_limit_reached'; end if;
  end if;

  select to_jsonb(o) into v_before from public.owner_game_accounts o where o.user_id=v_uid;

  insert into public.owner_game_accounts(
    user_id,lifetime_vip,unlimited_diamonds,unlimited_son_coin,active,updated_at,updated_by
  )
  values(
    v_uid,coalesce(p_lifetime_vip,false),coalesce(p_unlimited_diamonds,false),
    coalesce(p_unlimited_son_coin,false),coalesce(p_active,false),now(),auth.uid()
  )
  on conflict(user_id) do update set
    lifetime_vip=excluded.lifetime_vip,
    unlimited_diamonds=excluded.unlimited_diamonds,
    unlimited_son_coin=excluded.unlimited_son_coin,
    active=excluded.active,
    updated_at=now(),
    updated_by=auth.uid();

  if coalesce(p_active,false) and coalesce(p_lifetime_vip,false) then
    update public.profiles set is_vip=true,updated_at=now() where id=v_uid;
  end if;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(
    auth.uid(),'set_owner_game_account','owner_game_account',v_uid::text,v_before,
    jsonb_build_object(
      'email',v_email,
      'lifetime_vip',coalesce(p_lifetime_vip,false),
      'unlimited_diamonds',coalesce(p_unlimited_diamonds,false),
      'unlimited_son_coin',coalesce(p_unlimited_son_coin,false),
      'active',coalesce(p_active,false)
    )
  );

  return jsonb_build_object('success',true,'user_id',v_uid,'email',v_email);
end
$$;

create or replace function public.get_my_owner_perks_v1()
returns table(
  lifetime_vip boolean,
  unlimited_diamonds boolean,
  unlimited_son_coin boolean,
  active boolean
)
language sql
stable
security definer
set search_path=public,pg_temp
as $$
  select coalesce(o.lifetime_vip,false),coalesce(o.unlimited_diamonds,false),
         coalesce(o.unlimited_son_coin,false),coalesce(o.active,false)
  from (select auth.uid() as uid) me
  left join public.owner_game_accounts o on o.user_id=me.uid
$$;

create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_item public.shop_items%rowtype;
  v_balance integer;
  v_vip boolean;
  v_admin_free boolean:=false;
  v_owner_unlimited boolean:=false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items where id=p_item_id and active=true;
  if not found then raise exception 'item_not_found'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;

  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;
  select coalesce(a.free_test_purchases,false) into v_admin_free from public.admin_users a where a.user_id=v_uid;
  v_admin_free:=coalesce(v_admin_free,false) and public.is_admin();

  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=v_uid and o.active and o.unlimited_diamonds
  ) into v_owner_unlimited;

  if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;

  if not v_admin_free and not v_owner_unlimited then
    if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_item.diamond_price,updated_at=now() where id=v_uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  end if;

  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);

  if v_admin_free then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'test_free_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  elsif v_owner_unlimited then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'owner_unlimited_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  end if;

  return jsonb_build_object(
    'success',true,'item_id',p_item_id,
    'diamonds',case when v_admin_free or v_owner_unlimited then v_balance else v_balance-v_item.diamond_price end,
    'admin_test_free',v_admin_free,'owner_unlimited',v_owner_unlimited
  );
end
$$;

create or replace function public.buy_mascot_fruit_v1(p_fruit_id text, p_quantity integer default 1)
returns table(
  success boolean,
  fruit_id text,
  quantity integer,
  inventory_quantity integer,
  son_coin_spent integer,
  son_coin_balance integer
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_price integer;
  v_magic boolean;
  v_cost integer;
  v_balance integer;
  v_qty integer:=greatest(1,least(coalesce(p_quantity,1),20));
  v_inventory integer;
  v_owner_unlimited boolean:=false;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  select son_coin_price,is_magic into v_price,v_magic
  from public.mascot_fruit_catalog where id=p_fruit_id and active=true;
  if not found then raise exception 'fruit_not_found'; end if;
  if not v_magic or v_price<=0 then raise exception 'fruit_not_purchasable'; end if;

  v_cost:=v_price*v_qty;
  select diamonds into v_balance from public.profiles where id=v_uid for update;
  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=v_uid and o.active and o.unlimited_son_coin
  ) into v_owner_unlimited;

  if not v_owner_unlimited then
    if coalesce(v_balance,0)<v_cost then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_cost,updated_at=now()
    where id=v_uid returning diamonds into v_balance;
    if to_regclass('public.diamond_ledger') is not null then
      insert into public.diamond_ledger(user_id,delta,reason)
      values(v_uid,-v_cost,'mascot_magic_fruit');
    end if;
  else
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'owner_unlimited_son_coin_purchase','mascot_fruit',p_fruit_id,
           jsonb_build_object('quantity',v_qty,'normal_son_coin_cost',v_cost));
  end if;

  insert into public.user_mascot_fruit_inventory(user_id,fruit_id,quantity)
  values(v_uid,p_fruit_id,v_qty)
  on conflict(user_id,fruit_id) do update
  set quantity=public.user_mascot_fruit_inventory.quantity+excluded.quantity,updated_at=now()
  returning public.user_mascot_fruit_inventory.quantity into v_inventory;

  return query select true,p_fruit_id,v_qty,v_inventory,
    case when v_owner_unlimited then 0 else v_cost end,v_balance;
end
$$;

create table if not exists public.admin_platform_config (
  singleton boolean primary key default true check (singleton),
  supabase_plan text not null default 'free' check (supabase_plan in ('free','pro','team','enterprise')),
  database_limit_bytes bigint not null default 524288000 check (database_limit_bytes > 0),
  storage_limit_bytes bigint not null default 1073741824 check (storage_limit_bytes > 0),
  realtime_peak_connections_limit integer not null default 200 check (realtime_peak_connections_limit > 0),
  realtime_message_limit bigint not null default 2000000 check (realtime_message_limit > 0),
  edge_function_invocation_limit bigint not null default 500000 check (edge_function_invocation_limit > 0),
  mau_limit bigint not null default 50000 check (mau_limit > 0),
  updated_at timestamptz not null default now(),
  updated_by uuid null references auth.users(id) on delete set null
);
insert into public.admin_platform_config(singleton,supabase_plan,database_limit_bytes,storage_limit_bytes,realtime_peak_connections_limit,realtime_message_limit,edge_function_invocation_limit,mau_limit)
values(true,'free',524288000,1073741824,200,2000000,500000,50000)
on conflict(singleton) do nothing;
alter table public.admin_platform_config enable row level security;
revoke all on public.admin_platform_config from anon,authenticated;

create or replace function public.admin_capacity_v1()
returns table(
  metric_key text,title text,status text,used_value bigint,limit_value bigint,
  percent_used integer,unit text,detail text,resolve_url text
)
language plpgsql
security definer
set search_path=public,storage,pg_temp
as $$
declare
  c public.admin_platform_config%rowtype;
  v_db bigint;
  v_storage bigint;
  v_db_pct integer;
  v_storage_pct integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select * into c from public.admin_platform_config where singleton=true;
  v_db := pg_database_size(current_database());
  select coalesce(sum((o.metadata->>'size')::bigint),0) into v_storage
  from storage.objects o where o.metadata ? 'size';
  v_db_pct := least(999,round(v_db*100.0/c.database_limit_bytes)::int);
  v_storage_pct := least(999,round(v_storage*100.0/c.storage_limit_bytes)::int);

  return query values
    ('supabase_database','Supabase Veritabanı',
      case when v_db_pct>=90 then 'critical' when v_db_pct>=75 then 'warning' else 'ok' end,
      v_db,c.database_limit_bytes,v_db_pct,'bytes',
      'Plan: '||upper(c.supabase_plan)||' • Veritabanı kullanım oranı.',
      'https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/observability/database'),
    ('supabase_storage','Supabase Storage',
      case when v_storage_pct>=90 then 'critical' when v_storage_pct>=75 then 'warning' else 'ok' end,
      v_storage,c.storage_limit_bytes,v_storage_pct,'bytes',
      'Plan: '||upper(c.supabase_plan)||' • Dosya depolama kullanım oranı.',
      'https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('supabase_usage','Supabase Kota ve Trafik','info',
      0,c.realtime_message_limit,0,'link',
      'MAU, Realtime mesajları, bağlantılar, Edge Function ve egress kullanımı Supabase Usage ekranında izlenir.',
      'https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('github_actions','GitHub Build / Actions','info',
      0,0,0,'link',
      'APK/AAB derleme ve CI hatalarını doğrudan GitHub Actions ekranından kontrol et.',
      'https://github.com/makalega68-source/son-harf/actions');
end
$$;

create or replace function public.admin_set_platform_plan_v1(p_plan text)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_plan text:=lower(trim(coalesce(p_plan,'')));
  v_before jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_plan not in ('free','pro','team') then raise exception 'unsupported_plan'; end if;
  select to_jsonb(c) into v_before from public.admin_platform_config c where c.singleton=true;

  update public.admin_platform_config
  set supabase_plan=v_plan,
      database_limit_bytes=case when v_plan='free' then 524288000 else 8589934592 end,
      storage_limit_bytes=case when v_plan='free' then 1073741824 else 107374182400 end,
      realtime_peak_connections_limit=case when v_plan='free' then 200 else 500 end,
      realtime_message_limit=case when v_plan='free' then 2000000 else 5000000 end,
      edge_function_invocation_limit=case when v_plan='free' then 500000 else 2000000 end,
      mau_limit=case when v_plan='free' then 50000 else 100000 end,
      updated_at=now(),updated_by=auth.uid()
  where singleton=true;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_platform_plan','platform','supabase',v_before,jsonb_build_object('supabase_plan',v_plan));
  return jsonb_build_object('success',true,'plan',v_plan);
end
$$;

revoke all on function public.admin_owner_accounts_v1() from public,anon;
revoke all on function public.admin_set_owner_account_v1(text,boolean,boolean,boolean,boolean) from public,anon;
revoke all on function public.get_my_owner_perks_v1() from public,anon;
revoke all on function public.admin_capacity_v1() from public,anon;
revoke all on function public.admin_set_platform_plan_v1(text) from public,anon;
grant execute on function public.admin_owner_accounts_v1() to authenticated;
grant execute on function public.admin_set_owner_account_v1(text,boolean,boolean,boolean,boolean) to authenticated;
grant execute on function public.get_my_owner_perks_v1() to authenticated;
grant execute on function public.admin_capacity_v1() to authenticated;
grant execute on function public.admin_set_platform_plan_v1(text) to authenticated;

select pg_notify('pgrst','reload schema');
