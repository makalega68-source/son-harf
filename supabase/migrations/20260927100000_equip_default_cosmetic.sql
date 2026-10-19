-- Lets a player return any equipped cosmetic slot to the built-in default look from the profile
-- (keyboard, name colour, theme, ...). Ownership is untouched; only the equipped slot clears.
create or replace function public.equip_default_cosmetic(p_kind text)
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
  if p_kind not in ('game_theme','keyboard_theme','name_style','victory_effect','emoji_pack') then
    raise exception 'unsupported cosmetic kind: %', p_kind;
  end if;

  insert into public.user_equipped_cosmetics(user_id)
  values (v_uid)
  on conflict (user_id) do nothing;

  update public.user_equipped_cosmetics
  set game_theme_id     = case when p_kind = 'game_theme'     then null else game_theme_id end,
      keyboard_theme_id = case when p_kind = 'keyboard_theme' then null else keyboard_theme_id end,
      name_style_id     = case when p_kind = 'name_style'     then null else name_style_id end,
      victory_effect_id = case when p_kind = 'victory_effect' then null else victory_effect_id end,
      emoji_pack_id     = case when p_kind = 'emoji_pack'     then null else emoji_pack_id end,
      updated_at = now()
  where user_id = v_uid;

  return jsonb_build_object('success', true, 'kind', p_kind);
end;
$$;

revoke all on function public.equip_default_cosmetic(text) from public, anon;
grant execute on function public.equip_default_cosmetic(text) to authenticated, service_role;
