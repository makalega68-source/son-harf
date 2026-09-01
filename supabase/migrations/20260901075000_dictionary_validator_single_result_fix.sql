create or replace function private.validate_dictionary_word_v1(p_word text, p_language text)
returns table(valid boolean, reason text, normalized_word text)
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text := lower(coalesce(p_language, ''));
  v_word text;
  d public.dictionary_words%rowtype;
begin
  if v_language not in ('tr','en') then
    return query select false, 'invalid_language'::text, ''::text;
    return;
  end if;

  v_word := public.normalize_game_word(v_language, p_word);
  if char_length(v_word) < 2 or char_length(v_word) > 40 then
    return query select false, 'invalid_length'::text, v_word;
    return;
  end if;

  if (v_language = 'tr' and v_word !~ '^[a-zçğıöşü]+$')
     or (v_language = 'en' and v_word !~ '^[a-z]+$') then
    return query select false, 'invalid_characters'::text, v_word;
    return;
  end if;

  select * into d
  from public.dictionary_words x
  where x.language = v_language and x.normalized_word = v_word
  limit 1;

  if d.id is null or not coalesce(d.active, false) then
    return query select false, 'not_in_dictionary'::text, v_word;
    return;
  end if;
  if coalesce(d.is_abbreviation, false) or d.lexical_kind in ('abbreviation','acronym','code','symbol') then
    return query select false, 'abbreviation_not_allowed'::text, v_word;
    return;
  end if;
  if coalesce(d.is_proper_noun, false) or d.lexical_kind = 'proper_noun' then
    return query select false, 'proper_noun_not_allowed'::text, v_word;
    return;
  end if;
  if not coalesce(d.game_allowed, true) then
    return query select false, 'not_game_allowed'::text, v_word;
    return;
  end if;

  return query select true, 'valid'::text, v_word;
end
$$;

select pg_notify('pgrst','reload schema');
