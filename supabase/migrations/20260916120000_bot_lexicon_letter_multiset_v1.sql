-- Enforce a letter-count budget on the Word Siege bot lexicon so bots
-- cannot suggest words that repeat a letter more times than the rack allows.

create or replace function public.word_fits_letter_multiset_v1(p_word text, p_letters text)
returns boolean
language sql
immutable
parallel safe
set search_path = pg_catalog, pg_temp
as $$
    select not exists (
        select 1
        from (
            select ch, count(*) as need
            from regexp_split_to_table(coalesce(p_word, ''), '') as ch
            group by ch
        ) w
        where w.need > char_length(coalesce(p_letters, ''))
                     - char_length(replace(coalesce(p_letters, ''), w.ch, ''))
    );
$$;

revoke all on function public.word_fits_letter_multiset_v1(text,text) from public, anon;
grant execute on function public.word_fits_letter_multiset_v1(text,text) to authenticated, service_role;

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
    and char_length(d.normalized_word) between 2 and least(9, char_length(v_letters))
    and (v_language <> 'tr' or (left(d.normalized_word,1) <> 'ğ' and right(d.normalized_word,1) <> 'ğ'))
    and translate(d.normalized_word, v_letters, '') = ''
    and public.word_fits_letter_multiset_v1(d.normalized_word, v_letters)
  order by char_length(d.normalized_word) desc, d.normalized_word
  limit v_limit;
end;
$$;
