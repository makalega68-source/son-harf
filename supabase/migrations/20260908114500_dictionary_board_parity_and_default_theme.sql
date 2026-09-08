-- Keep the canonical mobile snapshot aligned with the largest local word surface.
-- Word Siege is a 15x15 board, so valid 13..15-letter dictionary entries must not be dropped.
create or replace function public.get_dictionary_snapshot_v3(p_language text default 'tr')
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
  select v_lang,
         coalesce(array_agg(d.normalized_word order by d.normalized_word), array[]::text[])
  from public.dictionary_words d
  where d.language = v_lang
    and d.active
    and coalesce(d.game_allowed, true)
    and not coalesce(d.is_abbreviation, false)
    and not coalesce(d.is_proper_noun, false)
    and char_length(d.normalized_word) between 2 and 15;
end;
$$;

grant execute on function public.get_dictionary_snapshot_v3(text) to anon, authenticated, service_role;

-- The blue/white Son Harf visual system is built in, not a paid inventory item. Clearing the
-- equipped game theme is therefore the authoritative way to return to it from a purchased theme.
create or replace function public.equip_default_game_theme()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'unauthorized';
  end if;

  insert into public.user_equipped_cosmetics(user_id)
  values (v_uid)
  on conflict (user_id) do nothing;

  update public.user_equipped_cosmetics
  set game_theme_id = null,
      updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object('success', true, 'game_theme_id', null);
end;
$$;

grant execute on function public.equip_default_game_theme() to authenticated, service_role;
