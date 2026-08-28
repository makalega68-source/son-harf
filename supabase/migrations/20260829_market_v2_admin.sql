-- Market v2 admin tooling: test grants and revenue labels for all live product IDs.

create or replace function public.admin_grant_test_product_v1(p_product_id text)
returns jsonb
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_id text:=trim(coalesce(p_product_id,''));
  v_delta integer:=0;
  v_balance integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_id='' then raise exception 'invalid_product'; end if;

  if v_id in ('vip_monthly','vip_yearly') then
    update public.profiles set is_vip=true,updated_at=now() where id=auth.uid();
  elsif v_id='season_pass_monthly' then
    insert into public.season_pass_entitlements(user_id,product_id,status,expires_at,updated_at)
    values(auth.uid(),v_id,'active',now()+interval '30 days',now())
    on conflict(user_id) do update set
      product_id=excluded.product_id,status='active',
      expires_at=greatest(public.season_pass_entitlements.expires_at,excluded.expires_at),
      updated_at=now();
  elsif v_id='coins_500' then v_delta:=500;
  elsif v_id='coins_1500' then v_delta:=1500;
  elsif v_id='coins_3500' then v_delta:=3500;
  elsif v_id='coins_8000' then v_delta:=8000;
  elsif v_id='starter_style_pack' then
    v_delta:=800;
    insert into public.user_inventory(user_id,item_id)
    values(auth.uid(),'frame_starter')
    on conflict(user_id,item_id) do nothing;
  elsif v_id='theme_neon' then
    insert into public.user_inventory(user_id,item_id)
    values(auth.uid(),'theme_neon')
    on conflict(user_id,item_id) do nothing;
  elsif exists(select 1 from public.shop_items s where s.id=v_id and s.active) then
    insert into public.user_inventory(user_id,item_id)
    values(auth.uid(),v_id)
    on conflict(user_id,item_id) do nothing;
  else
    raise exception 'unsupported_test_product';
  end if;

  if v_delta>0 then
    update public.profiles set diamonds=coalesce(diamonds,0)+v_delta
    where id=auth.uid() returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason,item_id)
    values(auth.uid(),v_delta,'admin_test_grant',v_id);
  else
    select diamonds into v_balance from public.profiles where id=auth.uid();
  end if;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'grant_test_product','product',v_id,
    jsonb_build_object('diamonds_granted',v_delta,'balance',v_balance));

  return jsonb_build_object(
    'success',true,'product_id',v_id,
    'diamonds_granted',v_delta,'diamond_balance',v_balance,'test_only',true
  );
end $$;

create or replace function public.admin_top_products_v1()
returns table(
  product_id text,
  product_name text,
  purchase_count bigint,
  revenue_minor bigint,
  currency text,
  price_configured boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select p.product_id,
    case p.product_id
      when 'vip_monthly' then 'VIP Aylık'
      when 'vip_yearly' then 'VIP Yıllık'
      when 'season_pass_monthly' then 'Sezon Bileti'
      when 'coins_500' then '500 Son Coin'
      when 'coins_1500' then '1500 Son Coin'
      when 'coins_3500' then '3500 Son Coin'
      when 'coins_8000' then '8000 Son Coin'
      when 'starter_style_pack' then 'Başlangıç Style Paketi'
      when 'theme_neon' then 'Neon Tema'
      else p.product_id
    end,
    count(*)::bigint,
    coalesce(sum(c.gross_price_minor),0)::bigint,
    coalesce(max(c.currency),'TRY')::text,
    bool_and(c.gross_price_minor is not null)
  from public.purchases p
  left join public.admin_product_catalog c on c.product_id=p.product_id
  where p.status='verified'
    and not exists(select 1 from public.admin_users a where a.user_id=p.user_id)
  group by p.product_id
  order by count(*) desc,p.product_id
  limit 10;
end $$;
