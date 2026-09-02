-- Canonical dictionary snapshot API for every client word-game mode.
-- The source of truth remains public.dictionary_words; this RPC avoids per-tile/per-word network validation.

create or replace function public.get_dictionary_snapshot_v1(p_language text default 'tr')
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_lang text := case when lower(coalesce(p_language, 'tr')) = 'en' then 'en' else 'tr' end;
  v_words jsonb;
begin
  select coalesce(jsonb_agg(d.normalized_word order by d.normalized_word), '[]'::jsonb)
  into v_words
  from public.dictionary_words d
  where d.language = v_lang
    and d.active
    and char_length(d.normalized_word) between 3 and 12;

  return jsonb_build_object(
    'language', v_lang,
    'words', v_words
  );
end;
$$;

revoke all on function public.get_dictionary_snapshot_v1(text) from public;
grant execute on function public.get_dictionary_snapshot_v1(text) to authenticated;

comment on function public.get_dictionary_snapshot_v1(text) is
  'Returns one canonical active dictionary snapshot for local indexed validation. TR and EN use the same API and public.dictionary_words source.';
