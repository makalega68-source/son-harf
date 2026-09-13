-- Enforce the v5 master dictionary on all active game validators/candidate feeds.
-- Turkish accepts only the official TDK corpus; English accepts only the pinned SCOWL/ESDB corpus.

create or replace function public.validate_game_word_v3(p_word text, p_language text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_language text := case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end;
    v_word text := public.normalize_game_word(
        case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end,
        coalesce(p_word,'')
    );
    v_valid boolean := false;
    v_length integer;
begin
    v_length := char_length(v_word);

    if v_length < 2 or v_length > 30 then
        return jsonb_build_object(
            'valid', false,
            'reason', 'invalid_length',
            'normalized_word', v_word,
            'first_letter', coalesce(left(v_word, 1), ''),
            'last_letter', coalesce(right(v_word, 1), ''),
            'char_length', v_length
        );
    end if;

    if (v_language = 'tr' and v_word !~ '^[a-zçğıöşü]+$')
       or (v_language = 'en' and v_word !~ '^[a-z]+$') then
        return jsonb_build_object(
            'valid', false,
            'reason', 'invalid_characters',
            'normalized_word', v_word,
            'first_letter', coalesce(left(v_word, 1), ''),
            'last_letter', coalesce(right(v_word, 1), ''),
            'char_length', v_length
        );
    end if;

    select exists (
        select 1
        from public.dictionary_words d
        where d.language = v_language
          and d.normalized_word = v_word
          and d.active = true
          and d.game_allowed = true
          and d.is_abbreviation = false
          and d.is_proper_noun = false
          and d.source_id = case
                when v_language = 'tr' then 'tdk-gts-autocomplete'
                else 'scowl-esdb-en-us'
              end
    ) into v_valid;

    return jsonb_build_object(
        'valid', v_valid,
        'reason', case when v_valid then 'ok' else 'not_in_master_dictionary' end,
        'normalized_word', v_word,
        'first_letter', coalesce(left(v_word, 1), ''),
        'last_letter', coalesce(right(v_word, 1), ''),
        'char_length', v_length
    );
end;
$$;

revoke all on function public.validate_game_word_v3(text,text) from public;
grant execute on function public.validate_game_word_v3(text,text) to anon, authenticated, service_role;

create or replace function private.validate_dictionary_word_v1(p_word text, p_language text)
returns table(valid boolean, reason text, normalized_word text)
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
    v_language text := lower(coalesce(p_language,''));
    v_word text;
    d public.dictionary_words%rowtype;
begin
    if v_language not in ('tr','en') then
        return query select false,'invalid_language'::text,''::text;
        return;
    end if;

    v_word := public.normalize_game_word(v_language, p_word);
    if char_length(v_word) < 2 or char_length(v_word) > 30 then
        return query select false,'invalid_length'::text,v_word;
        return;
    end if;
    if (v_language='tr' and v_word !~ '^[a-zçğıöşü]+$')
       or (v_language='en' and v_word !~ '^[a-z]+$') then
        return query select false,'invalid_characters'::text,v_word;
        return;
    end if;

    select * into d
    from public.dictionary_words x
    where x.language = v_language
      and x.normalized_word = v_word
      and x.source_id = case
            when v_language = 'tr' then 'tdk-gts-autocomplete'
            else 'scowl-esdb-en-us'
          end
    limit 1;

    if d.id is null or not coalesce(d.active,false) then
        return query select false,'not_in_dictionary'::text,v_word;
        return;
    end if;
    if coalesce(d.is_abbreviation,false)
       or coalesce(d.lexical_kind,'word') in ('abbreviation','acronym','code','symbol') then
        return query select false,'abbreviation_not_allowed'::text,v_word;
        return;
    end if;
    if coalesce(d.is_proper_noun,false) or coalesce(d.lexical_kind,'word')='proper_noun' then
        return query select false,'proper_noun_not_allowed'::text,v_word;
        return;
    end if;
    if not coalesce(d.game_allowed,true) then
        return query select false,'not_game_allowed'::text,v_word;
        return;
    end if;

    return query select true,'valid'::text,v_word;
end;
$$;

create or replace function public.get_core_word_candidates_v1(
    p_letters text,
    p_language text default 'tr',
    p_limit integer default 160
)
returns table(word text, normalized_word text)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_letters text := public.normalize_game_word(v_lang,coalesce(p_letters,''));
  v_limit int := least(greatest(coalesce(p_limit,160),10),300);
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  return query
  select d.word,d.normalized_word
  from public.dictionary_words d
  where d.language=v_lang
    and d.active
    and d.game_allowed
    and not d.is_abbreviation
    and not d.is_proper_noun
    and d.source_id = case when v_lang='tr' then 'tdk-gts-autocomplete' else 'scowl-esdb-en-us' end
    and char_length(d.normalized_word) between 3 and 10
    and public.arena_word_fits_letters_v1(d.normalized_word,v_letters)
  order by random()
  limit v_limit;
end
$$;

create or replace function public.word_siege_bot_lexicon_v1(
    p_letters text,
    p_language text default 'tr',
    p_limit integer default 900
)
returns setof text
language plpgsql
stable security definer
set search_path = pg_catalog, public, pg_temp
as $$
declare
  v_language text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_letters text := public.normalize_game_word(v_language, coalesce(p_letters,''));
  v_limit integer := greatest(50, least(coalesce(p_limit,900),1200));
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if char_length(v_letters) < 2 then return; end if;
  return query
  select d.normalized_word
  from public.dictionary_words d
  where d.language=v_language
    and d.active
    and d.game_allowed
    and not d.is_abbreviation
    and not d.is_proper_noun
    and d.bot_eligible
    and d.source_id = case when v_language='tr' then 'tdk-gts-autocomplete' else 'scowl-esdb-en-us' end
    and char_length(d.normalized_word) between 2 and 9
    and (v_language <> 'tr' or (left(d.normalized_word,1) <> 'ğ' and right(d.normalized_word,1) <> 'ğ'))
    and translate(d.normalized_word, v_letters, '')=''
  order by char_length(d.normalized_word) desc, d.normalized_word
  limit v_limit;
end;
$$;
