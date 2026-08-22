-- Owner-only operations console for Son Harf.
-- Authorization is always server-side through public.is_admin(); UI visibility is only a convenience.

alter table public.admin_users
  add column if not exists free_test_purchases boolean not null default true;

create table if not exists public.admin_product_catalog (
  product_id text primary key,
  gross_price_minor bigint check (gross_price_minor is null or gross_price_minor >= 0),
  currency text not null default 'TRY' check (currency ~ '^[A-Z]{3}$'),
  updated_at timestamptz not null default now(),
  updated_by uuid references public.profiles(id) on delete set null
);
alter table public.admin_product_catalog enable row level security;
revoke all on public.admin_product_catalog from anon, authenticated;

insert into public.admin_product_catalog(product_id,gross_price_minor,currency)
values
  ('vip_monthly',null,'TRY'),
  ('vip_yearly',null,'TRY'),
  ('coins_500',null,'TRY'),
  ('coins_1500',null,'TRY'),
  ('theme_neon',null,'TRY')
on conflict (product_id) do nothing;

create or replace function public.admin_dashboard_v1()
returns table(
  total_users bigint,
  active_now bigint,
  active_today bigint,
  active_7d bigint,
  vip_users bigint,
  active_subscriptions bigint,
  matches_total bigint,
  matches_today bigint,
  active_rooms bigint,
  stale_rooms bigint,
  queue_waiting bigint,
  verified_purchases bigint,
  gross_revenue_minor bigint,
  revenue_currency text,
  unpriced_purchases bigint,
  son_harf_opens bigint,
  bil_bakalim_opens bigint,
  my_is_vip boolean,
  free_test_purchases boolean
)
language plpgsql security definer set search_path=public,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query select
    (select count(*) from public.profiles)::bigint,
    (select count(*) from public.profiles p where p.last_seen_at >= now()-interval '5 minutes')::bigint,
    (select count(*) from public.profiles p where p.last_seen_at >= now()-interval '1 day')::bigint,
    (select count(*) from public.profiles p where p.last_seen_at >= now()-interval '7 days')::bigint,
    (select count(*) from public.profiles p where p.is_vip and not exists(select 1 from public.admin_users a where a.user_id=p.id))::bigint,
    (select count(*) from public.subscriptions s where s.status='active' and s.expires_at>now() and not exists(select 1 from public.admin_users a where a.user_id=s.user_id))::bigint,
    (select count(*) from public.game_rooms)::bigint,
    (select count(*) from public.game_rooms r where r.created_at >= now()-interval '1 day')::bigint,
    (select count(*) from public.game_rooms r where r.status in ('waiting','playing','quiz','final','sudden_death','paused'))::bigint,
    (select count(*) from public.game_rooms r where r.status in ('playing','quiz','final','sudden_death','paused') and greatest(coalesce(r.host_last_seen_at,r.created_at),coalesce(r.guest_last_seen_at,r.created_at)) < now()-interval '5 minutes')::bigint,
    (select count(*) from public.matchmaking_queue q where q.status='waiting')::bigint,
    (select count(*) from public.purchases p where p.status='verified' and not exists(select 1 from public.admin_users a where a.user_id=p.user_id))::bigint,
    coalesce((select sum(c.gross_price_minor) from public.purchases p join public.admin_product_catalog c on c.product_id=p.product_id where p.status='verified' and c.gross_price_minor is not null and not exists(select 1 from public.admin_users a where a.user_id=p.user_id)),0)::bigint,
    coalesce((select min(c.currency) from public.admin_product_catalog c where c.gross_price_minor is not null),'TRY')::text,
    (select count(*) from public.purchases p left join public.admin_product_catalog c on c.product_id=p.product_id where p.status='verified' and c.gross_price_minor is null and not exists(select 1 from public.admin_users a where a.user_id=p.user_id))::bigint,
    (select count(*) from public.app_events e where e.event_name='son_harf_open' and not exists(select 1 from public.admin_users a where a.user_id=e.user_id))::bigint,
    (select count(*) from public.app_events e where e.event_name='bil_bakalim_open' and not exists(select 1 from public.admin_users a where a.user_id=e.user_id))::bigint,
    coalesce((select p.is_vip from public.profiles p where p.id=auth.uid()),false),
    coalesce((select a.free_test_purchases from public.admin_users a where a.user_id=auth.uid()),false);
end $$;

create or replace function public.admin_top_products_v1()
returns table(product_id text, product_name text, purchase_count bigint, revenue_minor bigint, currency text, price_configured boolean)
language plpgsql security definer set search_path=public,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select p.product_id,
    case p.product_id when 'vip_monthly' then 'VIP Aylık' when 'vip_yearly' then 'VIP Yıllık' when 'coins_500' then '500 Elmas' when 'coins_1500' then '1500 Elmas' when 'theme_neon' then 'Neon Tema' else p.product_id end,
    count(*)::bigint,coalesce(sum(c.gross_price_minor),0)::bigint,coalesce(max(c.currency),'TRY')::text,bool_and(c.gross_price_minor is not null)
  from public.purchases p left join public.admin_product_catalog c on c.product_id=p.product_id
  where p.status='verified' and not exists(select 1 from public.admin_users a where a.user_id=p.user_id)
  group by p.product_id order by count(*) desc,p.product_id limit 10;
end $$;

create or replace function public.admin_top_store_items_v1()
returns table(item_id text, item_name text, acquisition_count bigint)
language plpgsql security definer set search_path=public,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select ui.item_id,coalesce(si.name_tr,ui.item_id),count(*)::bigint
  from public.user_inventory ui left join public.shop_items si on si.id=ui.item_id
  where not exists(select 1 from public.admin_users a where a.user_id=ui.user_id)
  group by ui.item_id,si.name_tr order by count(*) desc,ui.item_id limit 10;
end $$;

create or replace function public.admin_health_v1()
returns table(metric_key text,title text,status text,metric_value bigint,detail text)
language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_stale_rooms bigint; v_stale_queue bigint; v_stuck_quiz bigint; v_tr bigint; v_en bigint; v_recent_errors bigint;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select count(*) into v_stale_rooms from public.game_rooms r where r.status in ('playing','quiz','final','sudden_death','paused') and greatest(coalesce(r.host_last_seen_at,r.created_at),coalesce(r.guest_last_seen_at,r.created_at)) < now()-interval '5 minutes';
  select count(*) into v_stale_queue from public.matchmaking_queue q where q.status='waiting' and q.heartbeat_at < now()-interval '2 minutes';
  select count(*) into v_stuck_quiz from public.game_rooms r where r.status='quiz' and exists(select 1 from public.trivia_rounds t where t.room_id=r.id and ((t.resolved_at is not null and t.result_until < now()-interval '1 minute') or (t.resolved_at is null and t.answer_deadline < now()-interval '5 minutes')));
  select coalesce(max(word_count) filter(where language='tr' and ready),0) into v_tr from public.dictionary_sync_state;
  select coalesce(max(word_count) filter(where language='en' and ready),0) into v_en from public.dictionary_sync_state;
  select count(*) into v_recent_errors from public.system_events e where e.created_at>=now()-interval '24 hours' and e.severity in ('error','critical');
  return query values
    ('stale_rooms','Takılı / Eski Maçlar',case when v_stale_rooms=0 then 'ok' else 'warning' end,v_stale_rooms,case when v_stale_rooms=0 then 'Aktif maç akışı normal.' else '5 dakikadan uzun süredir güncellenmeyen aktif maç var.' end),
    ('stale_queue','Eski Eşleşme Kuyruğu',case when v_stale_queue=0 then 'ok' else 'warning' end,v_stale_queue,case when v_stale_queue=0 then 'Eşleşme kuyruğu temiz.' else 'Süresi geçmiş bekleyen eşleşme kaydı var.' end),
    ('stuck_quiz','Takılı Bil Bakalım / Quiz',case when v_stuck_quiz=0 then 'ok' else 'warning' end,v_stuck_quiz,case when v_stuck_quiz=0 then 'Bonus turları normal.' else 'Sonuçlanması veya devam etmesi gereken bonus turu var.' end),
    ('dictionary_tr','Türkçe Sözlük',case when v_tr>1000 then 'ok' else 'warning' end,v_tr,'Hazır Türkçe kelime sayısı.'),
    ('dictionary_en','İngilizce Sözlük',case when v_en>1000 then 'ok' else 'warning' end,v_en,'Hazır İngilizce kelime sayısı.'),
    ('recent_errors','Son 24 Saat Sistem Hatası',case when v_recent_errors=0 then 'ok' else 'warning' end,v_recent_errors,'system_events tablosundaki error/critical kayıtları.');
end $$;

create or replace function public.admin_set_my_vip_v1(p_enabled boolean)
returns boolean language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_before boolean; begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select is_vip into v_before from public.profiles where id=auth.uid() for update;
  update public.profiles set is_vip=coalesce(p_enabled,false),updated_at=now() where id=auth.uid();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_my_vip','profile',auth.uid()::text,jsonb_build_object('is_vip',v_before),jsonb_build_object('is_vip',coalesce(p_enabled,false)));
  return coalesce(p_enabled,false);
end $$;

create or replace function public.admin_set_free_purchases_v1(p_enabled boolean)
returns boolean language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_before boolean; begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select free_test_purchases into v_before from public.admin_users where user_id=auth.uid() for update;
  update public.admin_users set free_test_purchases=coalesce(p_enabled,false) where user_id=auth.uid();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_free_test_purchases','admin_user',auth.uid()::text,jsonb_build_object('enabled',v_before),jsonb_build_object('enabled',coalesce(p_enabled,false)));
  return coalesce(p_enabled,false);
end $$;

create or replace function public.admin_set_product_price_v1(p_product_id text,p_gross_price_minor bigint,p_currency text default 'TRY')
returns void language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_currency text:=upper(trim(coalesce(p_currency,'TRY'))); begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_product_id is null or trim(p_product_id)='' or p_gross_price_minor<0 or v_currency !~ '^[A-Z]{3}$' then raise exception 'invalid_price'; end if;
  insert into public.admin_product_catalog(product_id,gross_price_minor,currency,updated_at,updated_by)
  values(trim(p_product_id),p_gross_price_minor,v_currency,now(),auth.uid())
  on conflict(product_id) do update set gross_price_minor=excluded.gross_price_minor,currency=excluded.currency,updated_at=now(),updated_by=auth.uid();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'set_product_price','product',trim(p_product_id),jsonb_build_object('gross_price_minor',p_gross_price_minor,'currency',v_currency));
end $$;

create or replace function public.admin_repair_v1(p_action text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_action text:=lower(trim(coalesce(p_action,''))); v_count bigint:=0; v_total bigint:=0; v_result jsonb; begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_action not in ('maintenance','stale_rooms','stale_queue','stuck_quizzes','presence','all') then raise exception 'invalid_repair_action'; end if;
  if v_action in ('maintenance','all') then v_result:=public.admin_run_maintenance(); end if;
  if v_action in ('stale_rooms','all') then
    update public.game_rooms r set status='cancelled',turn_deadline=null,bot_turn=false,last_event='admin_stale_room_repair',finished_at=coalesce(finished_at,now())
    where r.status in ('playing','quiz','final','sudden_death','paused') and greatest(coalesce(r.host_last_seen_at,r.created_at),coalesce(r.guest_last_seen_at,r.created_at)) < now()-interval '5 minutes';
    get diagnostics v_count=row_count; v_total:=v_total+v_count;
  end if;
  if v_action in ('stale_queue','all') then
    update public.matchmaking_queue q set status='cancelled',room_id=null,heartbeat_at=now() where q.status='waiting' and q.heartbeat_at < now()-interval '2 minutes';
    get diagnostics v_count=row_count; v_total:=v_total+v_count;
  end if;
  if v_action in ('stuck_quizzes','all') then
    with latest as (
      select distinct on (t.room_id) t.room_id,t.resolved_at,t.result_until,t.answer_deadline,t.resume_status,t.resume_current_player_id,t.resume_bot_turn
      from public.trivia_rounds t order by t.room_id,t.created_at desc
    )
    update public.game_rooms r set status=coalesce(l.resume_status,'playing'),current_player_id=case when coalesce(l.resume_bot_turn,false) then null else l.resume_current_player_id end,bot_turn=coalesce(l.resume_bot_turn,false),turn_deadline=case when coalesce(l.resume_bot_turn,false) then null else public.sonharf_turn_deadline(r.game_mode) end,last_event='admin_quiz_recovered'
    from latest l where r.id=l.room_id and r.status='quiz' and l.resolved_at is not null and l.result_until < now()-interval '1 minute';
    get diagnostics v_count=row_count; v_total:=v_total+v_count;
    update public.game_rooms r set status='cancelled',turn_deadline=null,bot_turn=false,last_event='admin_stuck_quiz_cancelled',finished_at=coalesce(finished_at,now())
    where r.status='quiz' and exists(select 1 from public.trivia_rounds t where t.room_id=r.id and t.resolved_at is null and t.answer_deadline < now()-interval '5 minutes');
    get diagnostics v_count=row_count; v_total:=v_total+v_count;
  end if;
  if v_action in ('presence','all') then
    update public.profiles p set presence_status=case when p.last_seen_at>=now()-interval '10 minutes' then 'online' else 'offline' end
    where p.presence_status='in_game' and not exists(select 1 from public.game_rooms r where r.status in ('waiting','playing','quiz','final','sudden_death','paused') and (r.host_id=p.id or r.guest_id=p.id));
    get diagnostics v_count=row_count; v_total:=v_total+v_count;
  end if;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'repair_'||v_action,'system',v_action,jsonb_build_object('affected',v_total,'maintenance',v_result));
  return jsonb_build_object('success',true,'action',v_action,'affected',v_total,'maintenance',v_result);
end $$;

-- Admin test purchases acquire the item without consuming diamonds. Everybody else follows the production path unchanged.
create or replace function public.purchase_shop_item(p_item_id text)
returns jsonb language plpgsql security definer set search_path=public,pg_temp
as $$
declare v_uid uuid:=auth.uid(); v_item public.shop_items%rowtype; v_balance integer; v_vip boolean; v_admin_free boolean:=false; begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items where id=p_item_id and active=true;
  if not found then raise exception 'item_not_found'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_item_id) then raise exception 'already_owned'; end if;
  select diamonds,is_vip into v_balance,v_vip from public.profiles where id=v_uid for update;
  select coalesce(a.free_test_purchases,false) into v_admin_free from public.admin_users a where a.user_id=v_uid;
  v_admin_free:=coalesce(v_admin_free,false) and public.is_admin();
  if not v_admin_free then
    if v_item.vip_only and not coalesce(v_vip,false) then raise exception 'vip_required'; end if;
    if coalesce(v_balance,0) < v_item.diamond_price then raise exception 'insufficient_diamonds'; end if;
    update public.profiles set diamonds=diamonds-v_item.diamond_price where id=v_uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id) values(v_uid,-v_item.diamond_price,'shop_purchase',p_item_id);
  end if;
  insert into public.user_inventory(user_id,item_id) values(v_uid,p_item_id);
  if v_admin_free then
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
    values(v_uid,'test_free_purchase','shop_item',p_item_id,jsonb_build_object('normal_diamond_price',v_item.diamond_price));
  end if;
  return jsonb_build_object('success',true,'item_id',p_item_id,'diamonds',case when v_admin_free then v_balance else v_balance-v_item.diamond_price end,'admin_test_free',v_admin_free);
end $$;

revoke all on function public.admin_dashboard_v1() from public,anon;
revoke all on function public.admin_top_products_v1() from public,anon;
revoke all on function public.admin_top_store_items_v1() from public,anon;
revoke all on function public.admin_health_v1() from public,anon;
revoke all on function public.admin_set_my_vip_v1(boolean) from public,anon;
revoke all on function public.admin_set_free_purchases_v1(boolean) from public,anon;
revoke all on function public.admin_set_product_price_v1(text,bigint,text) from public,anon;
revoke all on function public.admin_repair_v1(text) from public,anon;
grant execute on function public.admin_dashboard_v1() to authenticated;
grant execute on function public.admin_top_products_v1() to authenticated;
grant execute on function public.admin_top_store_items_v1() to authenticated;
grant execute on function public.admin_health_v1() to authenticated;
grant execute on function public.admin_set_my_vip_v1(boolean) to authenticated;
grant execute on function public.admin_set_free_purchases_v1(boolean) to authenticated;
grant execute on function public.admin_set_product_price_v1(text,bigint,text) to authenticated;
grant execute on function public.admin_repair_v1(text) to authenticated;
