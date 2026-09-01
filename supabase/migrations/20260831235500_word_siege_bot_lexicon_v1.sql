-- Word Siege training bot: authoritative dictionary candidate feed.
-- This does not validate a move. It only narrows the main dictionary to words
-- whose letters could plausibly be built from the rack + board alphabet.
-- The client still runs the shared Word Siege move validator for every candidate.
create or replace function public.word_siege_bot_lexicon_v1(
  p_letters text,
  p_language text default 'tr',
  p_limit integer default 900
)
returns setof text
language plpgsql
security definer
stable
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
    and char_length(d.normalized_word) between 2 and 9
    and (v_language <> 'tr' or (left(d.normalized_word,1) <> 'ğ' and right(d.normalized_word,1) <> 'ğ'))
    -- Fast alphabet prefilter only. Repeated-letter multiplicity and board geometry
    -- are checked by candidate placement generation and the shared move validator.
    and translate(d.normalized_word, v_letters, '')=''
  order by char_length(d.normalized_word) desc, d.normalized_word
  limit v_limit;
end;
$$;

revoke all on function public.word_siege_bot_lexicon_v1(text,text,integer) from public, anon;
grant execute on function public.word_siege_bot_lexicon_v1(text,text,integer) to authenticated;
