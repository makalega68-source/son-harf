-- Market v2: fair-play monetization catalog and verified entitlements.
-- Revenue products are limited to subscriptions, Son Coin and Style; no match-power entitlements.

create table if not exists public.season_pass_entitlements (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  product_id text not null,
  status text not null default 'active',
  expires_at timestamptz not null,
  updated_at timestamptz not null default now()
);

alter table public.season_pass_entitlements enable row level security;
drop policy if exists season_pass_read_own on public.season_pass_entitlements;
drop policy if exists season_pass_self_select on public.season_pass_entitlements;
create policy season_pass_self_select
on public.season_pass_entitlements
for select to authenticated
using ((select auth.uid()) = user_id);
grant select on public.season_pass_entitlements to authenticated;

create table if not exists public.admin_product_catalog (
  product_id text primary key,
  gross_price_minor bigint check (gross_price_minor is null or gross_price_minor >= 0),
  currency text not null default 'TRY' check (currency ~ '^[A-Z]{3}$'),
  updated_at timestamptz not null default now(),
  updated_by uuid references public.profiles(id) on delete set null
);
alter table public.admin_product_catalog enable row level security;

insert into public.admin_product_catalog(product_id,currency)
values
 ('vip_monthly','TRY'),
 ('vip_yearly','TRY'),
 ('season_pass_monthly','TRY'),
 ('coins_500','TRY'),
 ('coins_1500','TRY'),
 ('coins_3500','TRY'),
 ('coins_8000','TRY'),
 ('starter_style_pack','TRY'),
 ('theme_neon','TRY')
on conflict(product_id) do update set updated_at=now();

insert into public.shop_items(
  id,kind,name_tr,name_en,description_tr,description_en,
  diamond_price,vip_only,active,sort_order
) values
(
  'frame_starter','profile_frame',
  'Kurucu Işık Çerçevesi','Founder Glow Frame',
  'Başlangıç Style Paketi ile açılabilir veya Son Coin ile alınabilir özel profil çerçevesi.',
  'Exclusive profile frame unlockable with the Starter Style Pack or Son Coin.',
  2200,false,true,15
)
on conflict(id) do update set
  kind=excluded.kind,
  name_tr=excluded.name_tr,
  name_en=excluded.name_en,
  description_tr=excluded.description_tr,
  description_en=excluded.description_en,
  vip_only=excluded.vip_only,
  active=true,
  sort_order=excluded.sort_order;

create or replace function public.apply_verified_play_purchase_v1(
  p_user_id uuid,
  p_product_id text,
  p_purchase_token text,
  p_order_id text default null,
  p_expires_at timestamptz default null
)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_inserted uuid;
  v_delta integer := 0;
  v_balance integer;
  v_inventory_items text[] := array[]::text[];
  v_item text;
begin
  if p_user_id is null or p_purchase_token is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if not exists(select 1 from public.profiles where id=p_user_id) then
    raise exception 'profile_not_found';
  end if;

  insert into public.purchases(user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at)
  values(p_user_id,p_product_id,p_purchase_token,nullif(trim(p_order_id),''),'verified',now(),now())
  on conflict(purchase_token) do nothing
  returning id into v_inserted;

  if v_inserted is null then
    return jsonb_build_object('success',true,'already_processed',true,'product_id',p_product_id);
  end if;

  if p_product_id in ('vip_monthly','vip_yearly') then
    if p_expires_at is null or p_expires_at <= now() then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.subscriptions(user_id,product_id,status,expires_at,updated_at)
    values(p_user_id,p_product_id,'active',p_expires_at,now())
    on conflict(user_id) do update set
      product_id=excluded.product_id,
      status='active',
      expires_at=greatest(coalesce(public.subscriptions.expires_at,'epoch'::timestamptz),excluded.expires_at),
      updated_at=now();
    update public.profiles set is_vip=true where id=p_user_id;

  elsif p_product_id='season_pass_monthly' then
    if p_expires_at is null or p_expires_at <= now() then raise exception 'invalid_subscription_expiry'; end if;
    insert into public.season_pass_entitlements(user_id,product_id,status,expires_at,updated_at)
    values(p_user_id,p_product_id,'active',p_expires_at,now())
    on conflict(user_id) do update set
      product_id=excluded.product_id,
      status='active',
      expires_at=greatest(public.season_pass_entitlements.expires_at,excluded.expires_at),
      updated_at=now();

  elsif p_product_id='coins_500' then
    v_delta := 500;
  elsif p_product_id='coins_1500' then
    v_delta := 1500;
  elsif p_product_id='coins_3500' then
    v_delta := 3500;
  elsif p_product_id='coins_8000' then
    v_delta := 8000;
  elsif p_product_id='starter_style_pack' then
    v_delta := 800;
    v_inventory_items := array['frame_starter'];
  elsif p_product_id='theme_neon' then
    v_inventory_items := array['theme_neon'];
  else
    raise exception 'unsupported_product';
  end if;

  if v_delta > 0 then
    update public.profiles
    set diamonds=coalesce(diamonds,0)+v_delta
    where id=p_user_id
    returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason)
    values(p_user_id,v_delta,'google_play_purchase:' || p_product_id);
  end if;

  foreach v_item in array v_inventory_items loop
    insert into public.user_inventory(user_id,item_id)
    values(p_user_id,v_item)
    on conflict(user_id,item_id) do nothing;
  end loop;

  return jsonb_build_object(
    'success',true,
    'already_processed',false,
    'product_id',p_product_id,
    'diamonds_granted',v_delta,
    'diamond_balance',v_balance,
    'inventory_items',to_jsonb(v_inventory_items),
    'expires_at',p_expires_at
  );
end
$$;

revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from public;
revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from anon;
revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from authenticated;
grant execute on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) to service_role;
