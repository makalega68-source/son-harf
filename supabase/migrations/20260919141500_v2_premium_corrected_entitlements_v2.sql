-- Canonical V2 corrected premium entitlements.
-- Additive/CREATE OR REPLACE only: no DROP/RENAME and no legacy entitlement conversion.

create or replace function public.get_premium_entitlements_v2()
returns jsonb
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
  profile_vip boolean := false;
  pro boolean := false;
  series_direct boolean := false;
  letter_direct boolean := false;
  score_direct boolean := false;
  claimed boolean := false;
  w public.vip_joker_wallet;
begin
  if u is null then raise exception 'unauthorized'; end if;

  -- Legacy VIP remains readable for backwards-compatible legacy helper UI only.
  -- It MUST NOT grant any new Premium/PRO product.
  select coalesce(is_vip,false) into profile_vip from public.profiles where id=u;

  pro := public.has_permanent_entitlement_v1(u,'pro_lifetime');
  series_direct := public.has_permanent_entitlement_v1(u,'series_game');
  letter_direct := public.has_permanent_entitlement_v1(u,'letter_table');
  score_direct := public.has_permanent_entitlement_v1(u,'score_calculator');

  insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
  select * into w from public.vip_joker_wallet where user_id=u;
  select exists(
    select 1 from public.vip_daily_joker_claims
    where user_id=u and claim_date=current_date
  ) into claimed;

  return jsonb_build_object(
    'is_vip', profile_vip,
    'is_pro', pro,
    'series_game_direct_owned', series_direct,
    'letter_table_direct_owned', letter_direct,
    'score_calculator_direct_owned', score_direct,
    'series_game_access', series_direct or pro,
    'letter_table_access', letter_direct or pro,
    'score_calculator_access', score_direct or pro,
    'daily_jokers_claimed', claimed,
    'freezer_count', coalesce(w.freezer_count,0),
    'swap_count', coalesce(w.swap_count,0),
    'hint_count', coalesce(w.hint_count,0),
    'streak_shield_count', coalesce(w.streak_shield_count,0),
    'multiplier_count', coalesce(w.multiplier_count,0),
    'xp_multiplier', 1,
    'diamond_multiplier', 1,
    'rewarded_ad_bypass', pro,
    'used_words_access', pro,
    'direct_messages_access', true,
    'ranked_live_assist', false,
    'post_match_analysis', pro,
    'saved_friend_list', pro,
    'private_rooms', pro,
    'active_game_limit', case when pro then 50 else 10 end
  );
end
$$;

grant execute on function public.get_premium_entitlements_v2() to authenticated;

create or replace function public.word_siege_active_limit_v1(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path = ''
as $$
  select case when public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime') then 50 else 10 end
$$;

create or replace function public.has_series_game_access_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select public.has_permanent_entitlement_v1(p_user_id,'series_game')
      or public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$$;

create or replace function public.can_use_pro_friend_list_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select p_user_id is not null
     and public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$$;

create or replace function public.can_view_sonharf_word_history_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select p_user_id is not null
     and public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$$;

create or replace function public.preview_word_siege_move_pro_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns jsonb
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'private', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (
    public.has_permanent_entitlement_v1(u,'score_calculator')
    or public.has_permanent_entitlement_v1(u,'pro_lifetime')
  ) then
    raise exception 'premium_score_calculator_required';
  end if;
  if not exists(
    select 1 from public.word_siege_games g
    where g.id=p_game_id and u in (g.player_one_id,g.player_two_id)
  ) then
    raise exception 'word_siege_not_participant';
  end if;
  return to_jsonb(private.word_siege_preview_move_v1(
    p_game_id,p_placements,coalesce(p_horizontal,true)
  ));
end
$$;

grant execute on function public.preview_word_siege_move_pro_v1(uuid,jsonb,boolean) to authenticated;

create or replace function public.get_word_siege_letter_table_v1(p_game_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'private', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
  g public.word_siege_games;
  own_rack text;
  canonical text;
  visible text;
  result jsonb;
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (
    public.has_permanent_entitlement_v1(u,'letter_table')
    or public.has_permanent_entitlement_v1(u,'pro_lifetime')
  ) then
    raise exception 'premium_letter_table_required';
  end if;

  select * into g from public.word_siege_games where id=p_game_id;
  if g.id is null or u not in (g.player_one_id,g.player_two_id) then
    raise exception 'word_siege_not_participant';
  end if;

  own_rack := case when u=g.player_one_id then g.player_one_rack else coalesce(g.player_two_rack,'') end;

  -- One authoritative distribution source. Order is randomized by the helper,
  -- but this RPC exposes counts only; it never exposes bag order or rival rack.
  canonical := private.word_siege_new_bag_v1(case when lower(coalesce(g.language,'tr'))='en' then 'en' else 'tr' end);

  select coalesce(string_agg(coalesce(cell->>'letter',''),''),'') into visible
  from jsonb_array_elements(g.board) cell;
  visible := visible || own_rack;

  with alphabet as (
    select ch as letter, count(*)::integer as total
    from regexp_split_to_table(canonical,'') ch
    where ch<>''
    group by ch
  ), used as (
    select ch as letter, count(*)::integer as used
    from regexp_split_to_table(visible,'') ch
    where ch<>''
    group by ch
  )
  select jsonb_agg(
    jsonb_build_object('letter',a.letter,'remaining',greatest(0,a.total-coalesce(u2.used,0)))
    order by a.letter
  ) into result
  from alphabet a left join used u2 using(letter);

  return coalesce(result,'[]'::jsonb);
end
$$;

grant execute on function public.get_word_siege_letter_table_v1(uuid) to authenticated;

-- Entitlement-scoped Series picker: Series owners may select accepted friends
-- without gaining access to the general PRO friend-list surface.
create or replace function public.get_series_friendships_v2()
returns setof public.friendships
language plpgsql
stable
security definer
set search_path = 'pg_catalog', 'public', 'pg_temp'
as $$
declare
  u uuid := auth.uid();
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not public.has_series_game_access_v1(u) then raise exception 'series_game_required'; end if;

  return query
  select f.*
  from public.friendships f
  where f.status='accepted'
    and u in (f.user_id,f.friend_id)
    and not exists(
      select 1 from public.user_blocks b
      where (b.blocker_id=u and b.blocked_id=case when f.user_id=u then f.friend_id else f.user_id end)
         or (b.blocked_id=u and b.blocker_id=case when f.user_id=u then f.friend_id else f.user_id end)
    )
  order by f.created_at desc;
end
$$;

grant execute on function public.get_series_friendships_v2() to authenticated;
