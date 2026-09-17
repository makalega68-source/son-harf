begin;

-- Admin bootstrap uses the real Supabase Auth identities. It never invents UUIDs.
insert into public.admin_users(user_id, role, free_test_purchases)
select u.id, 'admin', true
from auth.users u
where lower(trim(u.email)) in ('makalega58@gmail.com','makalega68@gmail.com')
on conflict (user_id) do update set role = excluded.role;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
  select exists(
    select 1 from public.admin_users a
    where a.user_id = auth.uid() and a.role = 'admin'
  );
$$;
revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

create or replace function public.admin_search_players_v2(p_query text)
returns table(
  user_id uuid,
  email text,
  display_name text,
  is_vip boolean,
  diamonds integer,
  rating integer,
  last_seen_at timestamptz,
  created_at timestamptz,
  blocked_until timestamptz,
  is_owner_account boolean
)
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_q text := lower(trim(coalesce(p_query,'')));
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if length(v_q) < 2 then raise exception 'query_too_short'; end if;
  return query
  select p.id, coalesce(u.email,'')::text, p.display_name, p.is_vip, p.diamonds, p.rating,
         p.last_seen_at, p.created_at, u.banned_until,
         exists(select 1 from public.owner_game_accounts o where o.user_id=p.id and o.active)
  from public.profiles p
  join auth.users u on u.id=p.id
  where lower(coalesce(u.email,'')) like '%'||v_q||'%'
     or lower(coalesce(p.display_name,'')) like '%'||v_q||'%'
  order by p.last_seen_at desc nulls last
  limit 50;
end;
$$;
revoke all on function public.admin_search_players_v2(text) from public, anon;
grant execute on function public.admin_search_players_v2(text) to authenticated;

create or replace function public.admin_adjust_player_diamonds_v1(p_user_id uuid, p_delta integer)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before integer; v_after integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_delta is null or p_delta = 0 or abs(p_delta) > 1000000 then raise exception 'invalid_delta'; end if;
  select diamonds into v_before from public.profiles where id=p_user_id for update;
  if v_before is null then raise exception 'user_not_found'; end if;
  v_after := greatest(0, least(10000000, v_before + p_delta));
  update public.profiles set diamonds=v_after, updated_at=now() where id=p_user_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'adjust_player_diamonds','profile',p_user_id::text,
         jsonb_build_object('diamonds',v_before),jsonb_build_object('diamonds',v_after,'delta',p_delta));
end;
$$;
revoke all on function public.admin_adjust_player_diamonds_v1(uuid,integer) from public, anon;
grant execute on function public.admin_adjust_player_diamonds_v1(uuid,integer) to authenticated;

create or replace function public.admin_set_player_blocked_v1(p_user_id uuid, p_blocked boolean)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before timestamptz; v_after timestamptz;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if exists(select 1 from public.admin_users where user_id=p_user_id and role='admin') then
    raise exception 'cannot_block_admin';
  end if;
  select banned_until into v_before from auth.users where id=p_user_id for update;
  if not found then raise exception 'user_not_found'; end if;
  v_after := case when coalesce(p_blocked,false) then now()+interval '24 hours' else null end;
  update auth.users set banned_until=v_after where id=p_user_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_player_blocked','auth_user',p_user_id::text,
         jsonb_build_object('banned_until',v_before),jsonb_build_object('banned_until',v_after,'temporary_24h',coalesce(p_blocked,false)));
end;
$$;
revoke all on function public.admin_set_player_blocked_v1(uuid,boolean) from public, anon;
grant execute on function public.admin_set_player_blocked_v1(uuid,boolean) to authenticated;

create or replace function public.admin_store_catalog_v1()
returns table(
  product_id text,
  gross_price_minor bigint,
  currency text,
  enabled boolean,
  badge_tr text,
  badge_en text,
  sort_order integer,
  updated_at timestamptz
)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select coalesce(p.product_id,c.product_id), coalesce(p.gross_price_minor,0), coalesce(p.currency,'TRY'),
         coalesce(c.enabled,true), c.badge_tr, c.badge_en, coalesce(c.sort_order,100),
         greatest(coalesce(p.updated_at,'epoch'::timestamptz),coalesce(c.updated_at,'epoch'::timestamptz))
  from public.admin_product_catalog p
  full join public.store_catalog_config c on c.product_id=p.product_id
  order by coalesce(c.sort_order,100), coalesce(p.product_id,c.product_id);
end;
$$;
revoke all on function public.admin_store_catalog_v1() from public, anon;
grant execute on function public.admin_store_catalog_v1() to authenticated;

create or replace function public.admin_set_store_enabled_v1(p_product_id text, p_enabled boolean)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_id text := trim(coalesce(p_product_id,'')); v_before boolean;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_id='' or not exists(
      select 1 from public.admin_product_catalog where product_id=v_id
      union all select 1 from public.store_catalog_config where product_id=v_id
  ) then raise exception 'unknown_product'; end if;
  select enabled into v_before from public.store_catalog_config where product_id=v_id;
  insert into public.store_catalog_config(product_id,enabled,updated_at)
  values(v_id,coalesce(p_enabled,false),now())
  on conflict(product_id) do update set enabled=excluded.enabled,updated_at=now();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_store_enabled','product',v_id,
         jsonb_build_object('enabled',v_before),jsonb_build_object('enabled',coalesce(p_enabled,false)));
end;
$$;
revoke all on function public.admin_set_store_enabled_v1(text,boolean) from public, anon;
grant execute on function public.admin_set_store_enabled_v1(text,boolean) to authenticated;

create or replace function public.admin_audit_v2()
returns table(
  id bigint,
  created_at timestamptz,
  admin_email text,
  action text,
  target_type text,
  target_id text,
  before_data text,
  after_data text,
  outcome text,
  error_text text
)
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select a.id,a.created_at,coalesce(u.email,'')::text,a.action,a.target_type,a.target_id,
         a.before_data::text,a.after_data::text,coalesce(a.outcome,'success'),a.error_text
  from public.admin_audit_log a
  left join auth.users u on u.id=a.admin_id
  order by a.created_at desc
  limit 100;
end;
$$;
revoke all on function public.admin_audit_v2() from public, anon;
grant execute on function public.admin_audit_v2() to authenticated;

create or replace function public.admin_recent_errors_v1()
returns table(id bigint,severity text,source text,event_type text,details text,created_at timestamptz)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select e.id,e.severity,e.source,e.event_type,e.details::text,e.created_at
  from public.system_events e
  where e.severity in ('error','critical')
  order by e.created_at desc
  limit 100;
end;
$$;
revoke all on function public.admin_recent_errors_v1() from public, anon;
grant execute on function public.admin_recent_errors_v1() to authenticated;

-- User-requested capacity bands: Normal 0-69, Warning 70-84, High 85-94, Critical 95+.
create or replace function public.admin_capacity_v1()
returns table(metric_key text,title text,status text,used_value bigint,limit_value bigint,percent_used integer,unit text,detail text,resolve_url text)
language plpgsql
security definer
set search_path = pg_catalog, public, storage, pg_temp
as $$
declare c public.admin_platform_config%rowtype; v_db bigint; v_storage bigint; v_db_pct integer; v_storage_pct integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select * into c from public.admin_platform_config where singleton=true;
  v_db := pg_database_size(current_database());
  select coalesce(sum((o.metadata->>'size')::bigint),0) into v_storage from storage.objects o where o.metadata ? 'size';
  if c.database_limit_bytes > 0 then v_db_pct := least(999,round(v_db*100.0/c.database_limit_bytes)::int); else v_db_pct := 0; end if;
  if c.storage_limit_bytes > 0 then v_storage_pct := least(999,round(v_storage*100.0/c.storage_limit_bytes)::int); else v_storage_pct := 0; end if;
  return query values
    ('supabase_database','Supabase Veritabanı',case when v_db_pct>=95 then 'critical' when v_db_pct>=85 then 'high' when v_db_pct>=70 then 'warning' else 'ok' end,
     v_db,c.database_limit_bytes,v_db_pct,'bytes','Gerçek PostgreSQL kullanılan alan; toplam limit admin platform plan yapılandırmasından gelir.','https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/observability/database'),
    ('supabase_storage','Supabase Storage',case when v_storage_pct>=95 then 'critical' when v_storage_pct>=85 then 'high' when v_storage_pct>=70 then 'warning' else 'ok' end,
     v_storage,c.storage_limit_bytes,v_storage_pct,'bytes','Gerçek storage nesne boyutu; toplam limit admin platform plan yapılandırmasından gelir.','https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('supabase_usage','Supabase Auth / Edge / Trafik','info',0,0,0,'link','MAU, Edge Function, egress ve bağlantı kotaları SQL API üzerinden güvenilir biçimde alınamıyor.','https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'),
    ('github_actions','GitHub / Actions','info',0,0,0,'link','Private repository ayrıntıları için APK içine token gömülmez; güvenli backend yetkisi olmadan kota tahmini gösterilmez.','https://github.com/makalega68-source/son-harf/actions');
end;
$$;
revoke all on function public.admin_capacity_v1() from public, anon;
grant execute on function public.admin_capacity_v1() to authenticated;

commit;
