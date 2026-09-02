-- Canonical dictionary: return one PostgREST record instead of a scalar jsonb payload.
-- This keeps supabase-kt decodeSingle on the same contract as normal row queries.
create or replace function public.get_dictionary_snapshot_v2(p_language text default 'tr')
returns table(language text, words text[])
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_lang text := case when lower(coalesce(p_language, 'tr')) = 'en' then 'en' else 'tr' end;
begin
  return query
  select
    v_lang,
    coalesce(array_agg(d.normalized_word order by d.normalized_word), array[]::text[])
  from public.dictionary_words d
  where d.language = v_lang
    and d.active
    and char_length(d.normalized_word) between 3 and 12;
end;
$$;

grant execute on function public.get_dictionary_snapshot_v2(text) to anon, authenticated, service_role;

-- The pre-existing backend profile-frame catalog is authoritative for sale/discovery.
-- Staged frame_asset_* rows were introduced later and include malformed artwork.
-- Disable them from new sales without touching user_inventory or equipped ownership.
update public.shop_items
set active = false
where kind = 'profile_frame'
  and id like 'frame_asset_%';
