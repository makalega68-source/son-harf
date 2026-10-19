-- Admin panel, round two:
--  * the player list loads without typing a search (newest activity first),
--  * an admin can gift Son Coin to a player (ledgered),
--  * an announcement saved without English is translated on the server through MyMemory's free
--    endpoint (pg_net), so it no longer depends on the admin's phone.

create or replace function public.admin_list_players_v1(p_query text default '')
returns table(user_id uuid, email text, display_name text, is_vip boolean, diamonds integer, rating integer,
              last_seen_at timestamptz, created_at timestamptz, blocked_until timestamptz, is_admin boolean)
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'auth', 'pg_temp'
as $function$
declare v_q text := lower(trim(coalesce(p_query, '')));
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select p.id, coalesce(u.email, '')::text, p.display_name, p.is_vip, p.diamonds, p.rating,
         p.last_seen_at, p.created_at, u.banned_until,
         exists(select 1 from public.admin_users a where a.user_id = p.id)
  from public.profiles p
  join auth.users u on u.id = p.id
  where v_q = ''
     or lower(coalesce(u.email, '')) like '%' || v_q || '%'
     or lower(coalesce(p.display_name, '')) like '%' || v_q || '%'
  order by p.last_seen_at desc nulls last
  limit 100;
end;
$function$;

create or replace function public.admin_gift_coins_v1(p_user_id uuid, p_amount integer)
returns integer
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'pg_temp'
as $function$
declare v_balance integer;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_amount is null or p_amount < 1 or p_amount > 100000 then raise exception 'amount_out_of_range'; end if;
  update public.profiles set diamonds = coalesce(diamonds, 0) + p_amount, updated_at = now()
  where id = p_user_id
  returning diamonds into v_balance;
  if v_balance is null then raise exception 'player_not_found'; end if;
  insert into public.diamond_ledger(user_id, delta, reason) values (p_user_id, p_amount, 'admin_gift');
  insert into public.admin_audit_log(admin_id, action, target_type, target_id, before_data, after_data)
  values (auth.uid(), 'coin_gift_v1', 'player', p_user_id::text, null, jsonb_build_object('amount', p_amount, 'balance', v_balance));
  return v_balance;
end;
$function$;

-- Percent-encoding for the translation URL (UTF-8 bytes of anything outside the unreserved set).
create or replace function private.url_encode_v1(p_text text)
returns text
language sql
immutable
set search_path to 'pg_catalog'
as $function$
  select coalesce(string_agg(
    case when s.c ~ '^[A-Za-z0-9_.~-]$' then s.c
         else (select string_agg('%' || upper(lpad(to_hex(get_byte(convert_to(s.c, 'UTF8'), i)), 2, '0')), '' order by i)
               from generate_series(0, octet_length(convert_to(s.c, 'UTF8')) - 1) i)
    end, '' order by s.n), '')
  from regexp_split_to_table(coalesce(p_text, ''), '') with ordinality as s(c, n);
$function$;

alter table public.admin_announcement add column if not exists en_translation_request bigint;

create or replace function private.collect_announcement_translation_v1()
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'net', 'pg_temp'
as $function$
declare
  v_request bigint;
  v_updated timestamptz;
  v_status integer;
  v_content text;
  v_text text;
begin
  select a.en_translation_request, a.updated_at into v_request, v_updated
  from public.admin_announcement a where a.singleton = true;
  if v_request is null then return; end if;

  select r.status_code, r.content into v_status, v_content from net._http_response r where r.id = v_request;
  if not found then
    if v_updated < now() - interval '5 minutes' then
      update public.admin_announcement set en_translation_request = null where singleton = true;
    end if;
    return;
  end if;

  if v_status = 200 then
    begin
      v_text := trim(v_content::jsonb -> 'responseData' ->> 'translatedText');
    exception when others then
      v_text := null;
    end;
  end if;
  update public.admin_announcement
  set message_en = case
        when v_text is not null and v_text <> '' and v_text not ilike 'MYMEMORY WARNING%' then left(v_text, 500)
        else message_en end,
      en_translation_request = null
  where singleton = true and en_translation_request = v_request;
end;
$function$;

revoke all on function private.collect_announcement_translation_v1() from public, anon, authenticated;
revoke all on function private.url_encode_v1(text) from public, anon, authenticated;

create or replace function public.admin_set_announcement_v2(p_message_tr text, p_message_en text, p_enabled boolean, p_maintenance boolean)
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'auth', 'pg_temp'
as $function$
declare
  v_before jsonb;
  v_tr text := left(trim(coalesce(p_message_tr, '')), 500);
  v_en text := left(trim(coalesce(p_message_en, '')), 500);
  v_request bigint;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select to_jsonb(a) into v_before from public.admin_announcement a where a.singleton = true;
  -- English left empty (or a copy of the Turkish) is filled in by the server translation.
  if v_tr <> '' and (v_en = '' or lower(v_en) = lower(v_tr)) then
    v_en := '';
    v_request := net.http_get(
      'https://api.mymemory.translated.net/get?langpair=tr%7Cen&q=' || private.url_encode_v1(v_tr));
  end if;
  insert into public.admin_announcement(singleton, message, message_tr, message_en, enabled, maintenance, updated_at, updated_by, en_translation_request)
  values (true, v_tr, v_tr, v_en, coalesce(p_enabled, false), coalesce(p_maintenance, false), now(), auth.uid(), v_request)
  on conflict (singleton) do update set message = excluded.message, message_tr = excluded.message_tr, message_en = excluded.message_en,
    enabled = excluded.enabled, maintenance = excluded.maintenance, updated_at = excluded.updated_at, updated_by = excluded.updated_by,
    en_translation_request = excluded.en_translation_request;
  insert into public.admin_audit_log(admin_id, action, target_type, target_id, before_data, after_data)
  values (auth.uid(), 'announcement_update_v2', 'system', 'announcement', v_before,
          jsonb_build_object('enabled', coalesce(p_enabled, false), 'maintenance', coalesce(p_maintenance, false),
                             'tr_length', length(v_tr), 'en_length', length(v_en), 'auto_translate', v_request is not null));
end;
$function$;

create or replace function public.admin_get_announcement_v2()
returns table(message_tr text, message_en text, enabled boolean, maintenance boolean, updated_at timestamptz)
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'pg_temp'
as $function$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  perform private.collect_announcement_translation_v1();
  return query
  select a.message_tr, a.message_en, a.enabled, a.maintenance, a.updated_at
  from public.admin_announcement a where a.singleton = true;
end;
$function$;

revoke all on function public.admin_list_players_v1(text) from public, anon;
revoke all on function public.admin_gift_coins_v1(uuid, integer) from public, anon;
grant execute on function public.admin_list_players_v1(text) to authenticated;
grant execute on function public.admin_gift_coins_v1(uuid, integer) to authenticated;

select cron.schedule('announcement_translation_collect_v1', '* * * * *',
  'select private.collect_announcement_translation_v1();');
