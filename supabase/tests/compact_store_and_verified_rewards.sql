-- Run against the migrated test database. All fixtures and effects are rolled back.
begin;
do $test$
declare
 u uuid:=gen_random_uuid();intent uuid;another uuid;r jsonb;n integer;before_balance integer;
begin
 insert into auth.users(id,email) values(u,u::text||'@store-test.invalid');
 insert into public.profiles(id,display_name,diamonds,is_vip) values(u,'Store regression fixture',1000,false)
 on conflict(id) do update set diamonds=1000,is_vip=false;
 perform set_config('request.jwt.claims',jsonb_build_object('sub',u,'role','authenticated')::text,true);
 perform set_config('request.jwt.claim.sub',u::text,true);

 r:=public.purchase_store_bundle_v1('starter_collection_v1');
 if (select diamonds from public.profiles where id=u)<>760 then raise exception 'wrong_bundle_debit';end if;
 if (select count(*) from public.user_inventory where user_id=u and item_id in ('frame_asset_red','name_cyan'))<>2 then raise exception 'bundle_delivery_missing';end if;
 perform public.purchase_store_bundle_v1('starter_collection_v1');
 if (select diamonds from public.profiles where id=u)<>760 then raise exception 'duplicate_bundle_charge';end if;

 update public.profiles set diamonds=0 where id=u;
 begin
  perform public.purchase_store_bundle_v1('quiet_light_collection_v1');
  raise exception 'expected_insufficient_balance';
 exception when others then if sqlerrm<>'insufficient_diamonds' then raise;end if;end;
 if exists(select 1 from public.store_bundle_purchases where user_id=u and bundle_id='quiet_light_collection_v1') then raise exception 'failed_purchase_granted';end if;
 update public.profiles set diamonds=760 where id=u;
 update public.store_bundles set available_until=now()-interval '1 hour',available_from=now()-interval '2 days' where id='quiet_light_collection_v1';
 begin
  perform public.purchase_store_bundle_v1('quiet_light_collection_v1');
  raise exception 'expected_expired_offer_rejection';
 exception when others then if sqlerrm<>'bundle_unavailable' then raise;end if;end;

 n:=public.claim_daily_checkin_v1();
 if n<>40 or public.claim_daily_checkin_v1()<>0 then raise exception 'daily_reward_not_idempotent';end if;
 if not (public.get_storefront_v1()->>'daily_claimed')::boolean then raise exception 'daily_status_incorrect';end if;
 begin
  perform public.prepare_store_ad_v1('diamonds');
  raise exception 'expected_disabled_ads';
 exception when others then if sqlerrm<>'rewarded_ads_unavailable' then raise;end if;end;

 update public.store_monetization_config set rewarded_enabled=true,allowed_rewarded_ad_units=array['test-unit'] where id;
 intent:=public.prepare_store_ad_v1('diamonds');
 begin
  perform public.claim_store_rewarded_ad_v1('diamonds',intent::text);
  raise exception 'unverified_ad_was_granted';
 exception when others then if sqlerrm<>'ad_verification_pending' then raise;end if;end;
 begin
  perform public.fulfil_store_ad_v1(intent,'test-transaction-1',u,'test-unit');
  raise exception 'client_fulfilled_ad';
 exception when others then if sqlerrm<>'service_only' then raise;end if;end;
 before_balance:=(select diamonds from public.profiles where id=u);
 perform set_config('request.jwt.claims',jsonb_build_object('role','service_role')::text,true);
 perform set_config('request.jwt.claim.sub','',true);
 r:=public.fulfil_store_ad_v1(intent,'test-transaction-1',u,'test-unit');
 if (r->>'diamonds_awarded')::int<>10 then raise exception 'wrong_ad_award';end if;
 perform public.fulfil_store_ad_v1(intent,'test-transaction-1',u,'test-unit');
 if (select diamonds from public.profiles where id=u)<>before_balance+10 then raise exception 'replayed_ad_granted';end if;
 begin
  perform public.fulfil_store_ad_v1(intent,'test-transaction-1',u,'foreign-unit');
  raise exception 'foreign_unit_accepted';
 exception when others then if sqlerrm<>'ad_unit_not_allowed' then raise;end if;end;
 begin
  perform public.fulfil_store_ad_v1(intent,'test-transaction-1',gen_random_uuid(),'test-unit');
  raise exception 'foreign_user_accepted';
 exception when others then if sqlerrm<>'invalid_ad_intent' then raise;end if;end;
 perform set_config('request.jwt.claims',jsonb_build_object('sub',u,'role','authenticated')::text,true);
 perform set_config('request.jwt.claim.sub',u::text,true);
 r:=public.claim_store_rewarded_ad_v1('diamonds',intent::text);
 if not (r->>'success')::boolean then raise exception 'receipt_not_readable';end if;

 if has_function_privilege('authenticated','public.grant_store_ad_internal_v1(text,text,text)','EXECUTE')
 or has_function_privilege('authenticated','public.fulfil_store_ad_v1(uuid,text,uuid,text)','EXECUTE')
 or has_function_privilege('anon','public.purchase_store_bundle_v1(text)','EXECUTE')
 or has_table_privilege('authenticated','public.store_ad_intents','INSERT')
 or has_table_privilege('authenticated','public.store_monetization_config','UPDATE')
 then raise exception 'economy_privilege_exposure';end if;
end $test$;

rollback;
