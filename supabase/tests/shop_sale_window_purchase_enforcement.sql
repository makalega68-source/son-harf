-- Read-only contract test. Run after migrations are applied to the target test database.
do $test$
declare
  v_def text;
  v_compact text;
begin
  v_def := pg_get_functiondef('public.purchase_shop_item(text)'::regprocedure);
  v_compact := regexp_replace(v_def, '\s+', '', 'g');

  if position('active=true' in v_compact)=0 then
    raise exception 'purchase_rpc_missing_active_guard';
  end if;
  if position('available_from<=now()' in v_compact)=0 then
    raise exception 'purchase_rpc_missing_sale_start_guard';
  end if;
  if position('(available_untilisnulloravailable_until>now())' in v_compact)=0 then
    raise exception 'purchase_rpc_missing_sale_end_guard';
  end if;

  if position('o.activeando.unlimited_diamonds' in v_compact)=0
     or position('owner_unlimited_purchase' in v_compact)=0 then
    raise exception 'owner_unlimited_purchase_regressed';
  end if;
  if position('free_test_purchases' in v_compact)=0
     or position('public.is_admin()' in v_compact)=0 then
    raise exception 'admin_test_purchase_regressed';
  end if;
  if position('updated_at=now()whereid=v_uid' in v_compact)=0 then
    raise exception 'profile_update_timestamp_regressed';
  end if;

  if has_function_privilege('anon','public.purchase_shop_item(text)','EXECUTE') then
    raise exception 'anonymous_purchase_rpc_exposed';
  end if;
  if not has_function_privilege('authenticated','public.purchase_shop_item(text)','EXECUTE') then
    raise exception 'authenticated_purchase_rpc_missing';
  end if;
  if not has_function_privilege('service_role','public.purchase_shop_item(text)','EXECUTE') then
    raise exception 'service_role_purchase_rpc_missing';
  end if;
end
$test$;
