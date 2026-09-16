-- G4.4 admin — mevsimsel tema aç/kapa + kelime tohumla.
--
-- Migration 20260917020000_g44_retention_v1.sql seasonal_themes ve
-- seasonal_theme_words tablolarını + get_active_seasonal_theme okuma
-- RPC'sini ekliyor. Bu migration admin tarafında bir tema
-- oluşturup açan/kapatan/kelime ekleyen fonksiyonları getiriyor.
--
-- Sadece service_role çağırabilir (yönetici paneli / SQL editör).

set search_path = public, pg_temp;

create or replace function public.admin_create_seasonal_theme(
    p_name_tr text,
    p_name_en text,
    p_multiplier int default 2,
    p_starts_at timestamptz default null,
    p_ends_at timestamptz default null
)
returns bigint
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_id bigint;
begin
    insert into public.seasonal_themes
        (name_tr, name_en, enabled, starts_at, ends_at, multiplier)
    values
        (coalesce(p_name_tr, '(isimsiz)'),
         coalesce(p_name_en, '(untitled)'),
         false,
         p_starts_at,
         p_ends_at,
         greatest(1, least(5, coalesce(p_multiplier, 2))))
    returning id into v_id;
    return v_id;
end;
$$;

revoke all on function public.admin_create_seasonal_theme(text, text, int, timestamptz, timestamptz)
    from public, anon, authenticated;
grant execute on function public.admin_create_seasonal_theme(text, text, int, timestamptz, timestamptz)
    to service_role;

create or replace function public.admin_seed_seasonal_words(
    p_theme_id bigint,
    p_language text,
    p_words text[]
)
returns int
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_added int := 0;
    v_word text;
    v_norm text;
begin
    if p_language not in ('tr', 'en') then
        raise exception 'invalid_language';
    end if;
    if p_words is null then return 0; end if;

    foreach v_word in array p_words loop
        v_norm := lower(trim(v_word));
        continue when v_norm = '' or v_norm is null;
        insert into public.seasonal_theme_words
            (theme_id, language, normalized_word)
        values (p_theme_id, p_language, v_norm)
        on conflict do nothing;
        v_added := v_added + 1;
    end loop;
    return v_added;
end;
$$;

revoke all on function public.admin_seed_seasonal_words(bigint, text, text[])
    from public, anon, authenticated;
grant execute on function public.admin_seed_seasonal_words(bigint, text, text[])
    to service_role;

create or replace function public.admin_activate_seasonal_theme(p_theme_id bigint)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    -- Convention: only one active theme at a time. Deactivate any
    -- currently active theme first.
    update public.seasonal_themes
       set enabled = false
     where enabled = true
       and id <> p_theme_id;

    update public.seasonal_themes
       set enabled = true,
           starts_at = coalesce(starts_at, now())
     where id = p_theme_id;
end;
$$;

revoke all on function public.admin_activate_seasonal_theme(bigint)
    from public, anon, authenticated;
grant execute on function public.admin_activate_seasonal_theme(bigint)
    to service_role;

create or replace function public.admin_deactivate_seasonal_theme(p_theme_id bigint)
returns void
language sql
security definer
set search_path = public, pg_temp
as $$
    update public.seasonal_themes
       set enabled = false,
           ends_at = coalesce(ends_at, now())
     where id = p_theme_id;
$$;

revoke all on function public.admin_deactivate_seasonal_theme(bigint)
    from public, anon, authenticated;
grant execute on function public.admin_deactivate_seasonal_theme(bigint)
    to service_role;

-- Convenience: full listing for the admin panel.
create or replace function public.admin_list_seasonal_themes()
returns table (
    id bigint,
    name_tr text,
    name_en text,
    enabled boolean,
    starts_at timestamptz,
    ends_at timestamptz,
    multiplier int,
    word_count bigint
)
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select t.id, t.name_tr, t.name_en, t.enabled, t.starts_at, t.ends_at, t.multiplier,
           (select count(*) from public.seasonal_theme_words w where w.theme_id = t.id)::bigint
    from public.seasonal_themes t
    order by t.enabled desc, t.id desc;
$$;

revoke all on function public.admin_list_seasonal_themes()
    from public, anon, authenticated;
grant execute on function public.admin_list_seasonal_themes()
    to service_role;

select pg_notify('pgrst', 'reload schema');
