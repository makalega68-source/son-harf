-- Batch authoritative validation for Word Siege bot planning.
create or replace function public.word_siege_validate_words_v1(
  p_words text[],
  p_language text default 'tr'
)
returns setof text
language plpgsql
security definer
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text := case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if coalesce(array_length(p_words,1),0) > 1600 then raise exception 'too_many_words'; end if;

  return query
  select distinct w.raw_word
  from unnest(coalesce(p_words,array[]::text[])) as w(raw_word)
  where private.word_siege_word_allowed_v1(w.raw_word, v_language);
end;
$$;

revoke all on function public.word_siege_validate_words_v1(text[],text) from public, anon;
grant execute on function public.word_siege_validate_words_v1(text[],text) to authenticated;
