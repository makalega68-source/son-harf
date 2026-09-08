create or replace function public.validate_game_word_v2(
  p_word text,
  p_language text default 'tr'
)
returns table(
  valid boolean,
  reason text,
  normalized_word text,
  first_letter text,
  last_letter text,
  char_length integer
)
language plpgsql
stable
security definer
set search_path = 'pg_catalog','public','private','pg_temp'
as $$
declare
  v_lang text := lower(coalesce(p_language,''));
  v_check record;
  v_norm text;
begin
  if auth.uid() is null then
    raise exception 'not_authenticated';
  end if;
  if v_lang not in ('tr','en') then
    return query select false,'invalid_language'::text,''::text,''::text,''::text,0;
    return;
  end if;

  select * into v_check
  from private.validate_dictionary_word_v1(p_word,v_lang)
  limit 1;

  v_norm := coalesce(v_check.normalized_word,'');

  if not coalesce(v_check.valid,false) then
    return query select false,coalesce(v_check.reason,'invalid_word')::text,v_norm,
      left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;

  if char_length(v_norm) > 30 then
    return query select false,'invalid_length'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;

  if v_lang='tr' and right(v_norm,1)='ğ' then
    return query select false,'ends_with_soft_g'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;

  return query select true,'valid'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
end
$$;

revoke all on function public.validate_game_word_v2(text,text) from public;
revoke all on function public.validate_game_word_v2(text,text) from anon;
grant execute on function public.validate_game_word_v2(text,text) to authenticated;
grant execute on function public.validate_game_word_v2(text,text) to service_role;

create or replace function public.get_dictionary_snapshot_v4(p_language text default 'tr')
returns table(language text, words text[])
language plpgsql
stable
security definer
set search_path = 'pg_catalog','public','pg_temp'
as $$
declare
  v_lang text := lower(coalesce(p_language,''));
begin
  if auth.uid() is null then
    raise exception 'not_authenticated';
  end if;
  if v_lang not in ('tr','en') then
    raise exception 'invalid_language';
  end if;

  return query
  select v_lang,
         coalesce(array_agg(d.normalized_word order by d.normalized_word),array[]::text[])
  from public.dictionary_words d
  where d.language=v_lang
    and d.active
    and coalesce(d.game_allowed,true)
    and not coalesce(d.is_abbreviation,false)
    and not coalesce(d.is_proper_noun,false)
    and coalesce(d.lexical_kind,'word') not in ('abbreviation','acronym','code','symbol','proper_noun','synthetic_pair')
    and char_length(d.normalized_word) between 2 and 15
    and not (v_lang='tr' and right(d.normalized_word,1)='ğ');
end
$$;

revoke all on function public.get_dictionary_snapshot_v4(text) from public;
revoke all on function public.get_dictionary_snapshot_v4(text) from anon;
grant execute on function public.get_dictionary_snapshot_v4(text) to authenticated;
grant execute on function public.get_dictionary_snapshot_v4(text) to service_role;

create or replace function public.validate_core_word_v1(p_word text,p_language text default 'tr')
returns boolean
language plpgsql
stable
security definer
set search_path = 'pg_catalog','public','private','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := lower(coalesce(p_language,''));
  v_check record;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if v_lang not in ('tr','en') then return false; end if;

  select * into v_check
  from private.validate_dictionary_word_v1(p_word,v_lang)
  limit 1;

  if not coalesce(v_check.valid,false) then return false; end if;
  if char_length(coalesce(v_check.normalized_word,'')) > 30 then return false; end if;
  if v_lang='tr' and right(v_check.normalized_word,1)='ğ' then return false; end if;
  return true;
end
$$;

revoke all on function public.validate_core_word_v1(text,text) from public;
revoke all on function public.validate_core_word_v1(text,text) from anon;
grant execute on function public.validate_core_word_v1(text,text) to authenticated;
grant execute on function public.validate_core_word_v1(text,text) to service_role;

-- These are implementation helpers behind submit_word_v3 and must not be client-callable.
revoke all on function public.submit_word_normal_v3(uuid,text) from public;
revoke all on function public.submit_word_normal_v3(uuid,text) from anon;
revoke all on function public.submit_word_normal_v3(uuid,text) from authenticated;
revoke all on function public.submit_word_v3_legacy(uuid,text) from public;
revoke all on function public.submit_word_v3_legacy(uuid,text) from anon;
revoke all on function public.submit_word_v3_legacy(uuid,text) from authenticated;
revoke all on function public.submit_word_expert_v1(uuid,text) from public;
revoke all on function public.submit_word_expert_v1(uuid,text) from anon;
revoke all on function public.submit_word_expert_v1(uuid,text) from authenticated;

grant execute on function public.submit_word_normal_v3(uuid,text) to service_role;
grant execute on function public.submit_word_v3_legacy(uuid,text) to service_role;
grant execute on function public.submit_word_expert_v1(uuid,text) to service_role;

comment on function public.validate_game_word_v2(text,text) is 'Canonical authenticated Son Harf game-word validator for Turkish and English.';
comment on function public.get_dictionary_snapshot_v4(text) is 'Authenticated, game-safe offline snapshot (2-15 chars) backed by public.dictionary_words.';

select pg_notify('pgrst','reload schema');
