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
  elsif v_id='coins_500' then
    v_delta:=500;
  elsif v_id='coins_1500' then
    v_delta:=1500;
  elsif v_id='theme_neon' then
    insert into public.user_inventory(user_id,item_id) values(auth.uid(),'theme_neon') on conflict(user_id,item_id) do nothing;
  elsif exists(select 1 from public.shop_items s where s.id=v_id and s.active) then
    insert into public.user_inventory(user_id,item_id) values(auth.uid(),v_id) on conflict(user_id,item_id) do nothing;
  else
    raise exception 'unsupported_test_product';
  end if;

  if v_delta>0 then
    update public.profiles set diamonds=diamonds+v_delta where id=auth.uid() returning diamonds into v_balance;
    insert into public.diamond_ledger(user_id,delta,reason,item_id) values(auth.uid(),v_delta,'admin_test_grant',v_id);
  else
    select diamonds into v_balance from public.profiles where id=auth.uid();
  end if;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'grant_test_product','product',v_id,jsonb_build_object('diamonds_granted',v_delta,'balance',v_balance));

  return jsonb_build_object('success',true,'product_id',v_id,'diamonds_granted',v_delta,'diamond_balance',v_balance,'test_only',true);
end $$;

revoke all on function public.admin_grant_test_product_v1(text) from public,anon;
grant execute on function public.admin_grant_test_product_v1(text) to authenticated;
