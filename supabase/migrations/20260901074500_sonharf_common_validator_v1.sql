-- Route Son Harf human and bot lexical checks through the same fail-closed dictionary service.

create or replace function public.sonharf_word_allowed(p_language text, p_word text)
returns boolean
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_check record;
begin
  select * into v_check
  from private.validate_dictionary_word_v1(p_word, p_language)
  limit 1;

  if not coalesce(v_check.valid, false) then return false; end if;
  -- Son Harf-specific rule. This is intentionally NOT part of common dictionary validity.
  if lower(coalesce(p_language, '')) = 'tr'
     and right(v_check.normalized_word, 1) = 'ğ' then
    return false;
  end if;
  return true;
end
$$;

-- Keep bot difficulty/choice heuristics untouched. Candidate admissibility is now the
-- same strict lexical rule used by the human path.
create or replace function public.sonharf_bot_word_allowed(p_language text, p_word text)
returns boolean
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if not public.sonharf_word_allowed(p_language, p_word) then return false; end if;
  if char_length(p_word) < 3 or char_length(p_word) > 12 then return false; end if;
  if exists(
    select 1 from public.bot_word_exclusions e
    where e.language = p_language and e.normalized_word = p_word
  ) then return false; end if;
  if p_language = 'tr' and char_length(p_word) > 10
     and (right(p_word,3) = 'mak' or right(p_word,3) = 'mek') then return false; end if;
  if p_word ~ '(.)\1\1' then return false; end if;
  return true;
end
$$;

revoke all on function public.sonharf_word_allowed(text,text) from public, anon;
grant execute on function public.sonharf_word_allowed(text,text) to authenticated;
revoke all on function public.sonharf_bot_word_allowed(text,text) from public, anon;
grant execute on function public.sonharf_bot_word_allowed(text,text) to authenticated;

select pg_notify('pgrst','reload schema');
