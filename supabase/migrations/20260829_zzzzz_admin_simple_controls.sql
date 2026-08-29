-- Son Harf simple admin controls and direct operations links (2026-08-29)

create or replace function public.admin_game_controls_v1()
returns table(
  config_key text,
  title text,
  detail text,
  enabled boolean
)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query values
    ('matchmaking_enabled','Eşleşme','Yeni rastgele eşleşmeler açık.',coalesce((select (value #>> '{}')::boolean from public.app_config where key='matchmaking_enabled'),true)),
    ('chat_enabled','Maç Sohbeti','Oyuncular arası maç sohbeti açık.',coalesce((select (value #>> '{}')::boolean from public.app_config where key='chat_enabled'),true)),
    ('trivia_enabled','Bil Bakalım','Bil Bakalım / quiz akışı açık.',coalesce((select (value #>> '{}')::boolean from public.app_config where key='trivia_enabled'),true)),
    ('maintenance_mode','Bakım Modu','Acil bakım göstergesi. Yalnız gerektiğinde aç.',coalesce((select (value #>> '{}')::boolean from public.app_config where key='maintenance_mode'),false));
end
$$;

revoke all on function public.admin_game_controls_v1() from public,anon;
grant execute on function public.admin_game_controls_v1() to authenticated;

create or replace function public.admin_capacity_v1()
returns table(
  metric_key text,
  title text,
  status text,
  used_value bigint,
  limit_value bigint,
  percent_used integer,
  unit text,
  detail text,
  resolve_url text
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
    (
      'supabase_database',
      'Supabase Veritabanı',
      case when v_db_pct>=90 then 'critical' when v_db_pct>=75 then 'warning' else 'ok' end,
      v_db,c.database_limit_bytes,v_db_pct,'bytes',
      'Plan: '||upper(c.supabase_plan)||' • Veritabanı kullanım oranı.',
      'https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/observability/database'
    ),
    (
      'supabase_storage',
      'Supabase Storage',
      case when v_storage_pct>=90 then 'critical' when v_storage_pct>=75 then 'warning' else 'ok' end,
      v_storage,c.storage_limit_bytes,v_storage_pct,'bytes',
      'Plan: '||upper(c.supabase_plan)||' • Dosya depolama kullanım oranı.',
      'https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'
    ),
    (
      'supabase_usage',
      'Supabase Kota ve Trafik',
      'info',
      0,c.realtime_message_limit,0,'link',
      'MAU, Realtime mesajları, bağlantılar, Edge Function ve egress kullanımını aç.',
      'https://supabase.com/dashboard/org/jyioohqncfymfsoigyzr/usage'
    ),
    (
      'supabase_auth_email',
      'E-posta Şablonu',
      'info',
      0,0,0,'link',
      'Son Harf doğrulama e-postası, konu ve içerik ayarlarının bulunduğu ekran.',
      'https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/auth/templates'
    ),
    (
      'supabase_auth_urls',
      'Doğrulama Yönlendirmesi',
      'info',
      0,0,0,'link',
      'sonharf://auth dönüş adresi ve Site URL ayarlarını aç.',
      'https://supabase.com/dashboard/project/bzdtftzdjtjoqhtcqtxb/auth/url-configuration'
    ),
    (
      'github_actions',
      'GitHub Build / Actions',
      'info',
      0,0,0,'link',
      'APK/AAB derleme ve CI hatalarını doğrudan GitHub Actions ekranından kontrol et.',
      'https://github.com/makalega68-source/son-harf/actions'
    ),
    (
      'github_rollback',
      'Son Sağlam Sürüme Dön',
      'info',
      0,0,0,'link',
      'Kod son başarılı ana sürüme döner. Oyuncu veritabanı geri sarılmaz veya silinmez. GitHub ek onay ister.',
      'https://github.com/makalega68-source/son-harf/actions/workflows/rollback-last-green.yml'
    );
end
$$;

revoke all on function public.admin_capacity_v1() from public,anon;
grant execute on function public.admin_capacity_v1() to authenticated;

select pg_notify('pgrst','reload schema');
