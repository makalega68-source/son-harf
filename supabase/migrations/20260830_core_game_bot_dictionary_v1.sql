-- Shared dictionary helpers for Kelime Düellosu bot mode and Kelime Fethi.

create or replace function public.validate_core_word_v1(
  p_word text,
  p_language text default 'tr'
)
returns boolean
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_norm text;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  v_norm:=public.normalize_game_word(v_lang,trim(coalesce(p_word,'')));
  if char_length(v_norm)<3 or char_length(v_norm)>12 then return false; end if;
  return exists(
    select 1 from public.dictionary_words d
    where d.language=v_lang
      and d.active
      and d.normalized_word=v_norm
  );
end
$$;

create or replace function public.get_core_word_candidates_v1(
  p_letters text,
  p_language text default 'tr',
  p_limit int default 160
)
returns table(word text, normalized_word text)
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_lang text:=case when lower(coalesce(p_language,'tr'))='en' then 'en' else 'tr' end;
  v_letters text:=public.normalize_game_word(v_lang,coalesce(p_letters,''));
  v_limit int:=least(greatest(coalesce(p_limit,160),10),300);
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  return query
  select d.word,d.normalized_word
  from public.dictionary_words d
  where d.language=v_lang
    and d.active
    and char_length(d.normalized_word) between 3 and 10
    and public.arena_word_fits_letters_v1(d.normalized_word,v_letters)
  order by char_length(d.normalized_word) desc, random()
  limit v_limit;
end
$$;

revoke all on function public.validate_core_word_v1(text,text) from public,anon,authenticated;
revoke all on function public.get_core_word_candidates_v1(text,text,int) from public,anon,authenticated;
grant execute on function public.validate_core_word_v1(text,text) to authenticated;
grant execute on function public.get_core_word_candidates_v1(text,text,int) to authenticated;
