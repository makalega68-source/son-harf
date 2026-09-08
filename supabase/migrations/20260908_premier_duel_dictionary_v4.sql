-- Son Harf Premier dictionary V4
-- One authoritative Turkish/English corpus, strict game filtering and minimum-privilege RPCs.

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
security invoker
set search_path = 'pg_catalog','public','pg_temp'
as $$
declare
  v_lang text := lower(coalesce(p_language,''));
  v_norm text;
  d public.dictionary_words%rowtype;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if v_lang not in ('tr','en') then
    return query select false,'invalid_language'::text,''::text,''::text,''::text,0;
    return;
  end if;

  v_norm := public.normalize_game_word(v_lang,coalesce(p_word,''));
  if char_length(v_norm)<2 or char_length(v_norm)>30 then
    return query select false,'invalid_length'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;
  if (v_lang='tr' and v_norm !~ '^[a-zçğıöşü]+$') or (v_lang='en' and v_norm !~ '^[a-z]+$') then
    return query select false,'invalid_characters'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;

  select * into d
  from public.dictionary_words x
  where x.language=v_lang and x.normalized_word=v_norm and x.active
  limit 1;

  if d.id is null then
    return query select false,'not_in_dictionary'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;
  if coalesce(d.is_abbreviation,false) or coalesce(d.lexical_kind,'word') in ('abbreviation','acronym','code','symbol') then
    return query select false,'abbreviation_not_allowed'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;
  if coalesce(d.is_proper_noun,false) or coalesce(d.lexical_kind,'word')='proper_noun' then
    return query select false,'proper_noun_not_allowed'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
    return;
  end if;
  if not coalesce(d.game_allowed,true) or coalesce(d.lexical_kind,'word')='synthetic_pair' then
    return query select false,'not_game_allowed'::text,v_norm,left(v_norm,1),right(v_norm,1),char_length(v_norm);
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
security invoker
set search_path = 'pg_catalog','public','pg_temp'
as $$
declare
  v_lang text := lower(coalesce(p_language,''));
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if v_lang not in ('tr','en') then raise exception 'invalid_language'; end if;

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
security invoker
set search_path = 'pg_catalog','public','pg_temp'
as $$
declare
  v_valid boolean := false;
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  select x.valid into v_valid from public.validate_game_word_v2(p_word,p_language) x limit 1;
  return coalesce(v_valid,false);
end
$$;

revoke all on function public.validate_core_word_v1(text,text) from public;
revoke all on function public.validate_core_word_v1(text,text) from anon;
grant execute on function public.validate_core_word_v1(text,text) to authenticated;
grant execute on function public.validate_core_word_v1(text,text) to service_role;

-- Internal helpers stay callable by SECURITY DEFINER server functions, but not directly by clients.
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
comment on function public.get_dictionary_snapshot_v4(text) is 'Authenticated game-safe offline snapshot (2-15 chars) backed by public.dictionary_words.';

select pg_notify('pgrst','reload schema');
