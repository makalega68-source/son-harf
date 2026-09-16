-- G4.4 admin UI bridge — authenticated admin-only seasonal theme operations.
-- Keeps service_role out of the Android client and reuses the existing
-- server-side seasonal theme functions from 20260917070000_g44_seasonal_admin_v1.sql.

set search_path = public, pg_temp;

create or replace function public.admin_ui_list_seasonal_themes_v1()
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
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
begin
    if not public.is_admin() then
        raise exception 'admin_required';
    end if;

    return query
    select t.id,
           t.name_tr,
           t.name_en,
           t.enabled,
           t.starts_at,
           t.ends_at,
           t.multiplier,
           (select count(*) from public.seasonal_theme_words w where w.theme_id = t.id)::bigint
      from public.seasonal_themes t
     order by t.enabled desc, t.id desc;
end;
$$;

revoke all on function public.admin_ui_list_seasonal_themes_v1()
    from public, anon;
grant execute on function public.admin_ui_list_seasonal_themes_v1()
    to authenticated;

create or replace function public.admin_ui_set_seasonal_theme_active_v1(
    p_theme_id bigint,
    p_enabled boolean
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_before jsonb;
begin
    if not public.is_admin() then
        raise exception 'admin_required';
    end if;

    select to_jsonb(t) into v_before
      from public.seasonal_themes t
     where t.id = p_theme_id;

    if v_before is null then
        raise exception 'seasonal_theme_not_found';
    end if;

    if coalesce(p_enabled, false) then
        perform public.admin_activate_seasonal_theme(p_theme_id);
    else
        perform public.admin_deactivate_seasonal_theme(p_theme_id);
    end if;

    insert into public.admin_audit_log(
        admin_id, action, target_type, target_id, before_data, after_data
    )
    select auth.uid(),
           'set_seasonal_theme_active',
           'seasonal_theme',
           p_theme_id::text,
           v_before,
           to_jsonb(t)
      from public.seasonal_themes t
     where t.id = p_theme_id;
end;
$$;

revoke all on function public.admin_ui_set_seasonal_theme_active_v1(bigint, boolean)
    from public, anon;
grant execute on function public.admin_ui_set_seasonal_theme_active_v1(bigint, boolean)
    to authenticated;

create or replace function public.admin_ui_add_seasonal_word_v1(
    p_theme_id bigint,
    p_language text,
    p_word text
)
returns int
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_language text := lower(trim(coalesce(p_language, '')));
    v_word text := lower(trim(coalesce(p_word, '')));
    v_added int;
begin
    if not public.is_admin() then
        raise exception 'admin_required';
    end if;

    if not exists(select 1 from public.seasonal_themes where id = p_theme_id) then
        raise exception 'seasonal_theme_not_found';
    end if;

    if v_language not in ('tr', 'en') then
        raise exception 'invalid_language';
    end if;

    if v_word = '' or length(v_word) > 64 then
        raise exception 'invalid_word';
    end if;

    v_added := public.admin_seed_seasonal_words(
        p_theme_id,
        v_language,
        array[v_word]
    );

    insert into public.admin_audit_log(
        admin_id, action, target_type, target_id, after_data
    ) values (
        auth.uid(),
        'add_seasonal_word',
        'seasonal_theme',
        p_theme_id::text,
        jsonb_build_object(
            'language', v_language,
            'word', v_word,
            'inserted', v_added > 0
        )
    );

    return v_added;
end;
$$;

revoke all on function public.admin_ui_add_seasonal_word_v1(bigint, text, text)
    from public, anon;
grant execute on function public.admin_ui_add_seasonal_word_v1(bigint, text, text)
    to authenticated;

select pg_notify('pgrst', 'reload schema');
