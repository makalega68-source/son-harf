insert into public.shop_items(id,kind,name_tr,name_en,description_tr,description_en,diamond_price,vip_only,active,sort_order)
values ('theme_neon','game_theme','Neon Tema','Neon Theme','Google Play veya elmas ile açılabilen neon oyun teması.','Neon game theme unlockable through Google Play or diamonds.',1200,false,true,45)
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
  v_inventory_item text;
begin
  if p_user_id is null or p_purchase_token is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if not exists(select 1 from public.profiles where id=p_user_id) then
    raise exception 'profile_not_found';
  end if;

  insert into public.purchases(user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at)
  values(p_user_id,p_product_id,p_purchase_token,nullif(trim(p_order_id),''),'verified',now(),now())
  on conflict (purchase_token) do nothing
  returning id into v_inserted;

  if v_inserted is null then
    return jsonb_build_object('success',true,'already_processed',true,'product_id',p_product_id);
  end if;

  if p_product_id in ('vip_monthly','vip_yearly') then
    if p_expires_at is null or p_expires_at <= now() then
      raise exception 'invalid_subscription_expiry';
    end if;
    insert into public.subscriptions(user_id,product_id,status,expires_at,updated_at)
    values(p_user_id,p_product_id,'active',p_expires_at,now())
    on conflict (user_id) do update set
      product_id=excluded.product_id,
      status='active',
      expires_at=greatest(coalesce(public.subscriptions.expires_at,'epoch'::timestamptz),excluded.expires_at),
      updated_at=now();
    update public.profiles set is_vip=true where id=p_user_id;
  elsif p_product_id='coins_500' then
    v_delta := 500;
  elsif p_product_id='coins_1500' then
    v_delta := 1500;
  elsif p_product_id='theme_neon' then
    v_inventory_item := 'theme_neon';
  else
    raise exception 'unsupported_product';
  end if;

  if v_delta > 0 then
    update public.profiles set diamonds=diamonds+v_delta where id=p_user_id returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason) values(p_user_id,v_delta,'google_play_purchase');
  elsif v_inventory_item is not null then
    insert into public.user_inventory(user_id,item_id) values(p_user_id,v_inventory_item)
    on conflict (user_id,item_id) do nothing;
  end if;

  return jsonb_build_object(
    'success',true,
    'already_processed',false,
    'product_id',p_product_id,
    'diamonds_granted',v_delta,
    'diamond_balance',v_balance,
    'inventory_item',v_inventory_item,
    'expires_at',p_expires_at
  );
end $$;

revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from public;
revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from anon;
revoke all on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) from authenticated;
grant execute on function public.apply_verified_play_purchase_v1(uuid,text,text,text,timestamptz) to service_role;
