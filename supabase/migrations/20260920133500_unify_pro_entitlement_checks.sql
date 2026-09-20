-- Keep every server-side PRO gate aligned with get_vip_entitlements_v7.
-- Time-based PRO is represented by profiles.is_vip after verified Play reconciliation;
-- lifetime PRO remains a permanent entitlement. Individual permanent feature purchases
-- continue to unlock only their own feature.

create or replace function public.has_pro_access_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path to ''
as $function$
  select coalesce((
    select coalesce(p.is_vip,false)
    from public.profiles p
    where p.id=p_user_id
  ),false)
  or public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
$function$;

create or replace function public.has_series_game_access_v1(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path to ''
as $function$
  select public.has_permanent_entitlement_v1(p_user_id,'series_game')
      or public.has_pro_access_v1(p_user_id)
$function$;

create or replace function public.word_siege_active_limit_v1(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path to ''
as $function$
  select case when public.has_pro_access_v1(p_user_id) then 50 else 10 end
$function$;

create or replace function public.preview_word_siege_move_pro_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
) returns jsonb
language plpgsql
security definer
set search_path to 'pg_catalog','public','private','pg_temp'
as $function$
declare u uuid:=auth.uid();
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (
    public.has_permanent_entitlement_v1(u,'score_calculator')
    or public.has_pro_access_v1(u)
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
$function$;

create or replace function public.get_word_siege_letter_table_v1(p_game_id uuid)
returns jsonb
language plpgsql
security definer
set search_path to 'pg_catalog','public','private','pg_temp'
as $function$
declare
  u uuid:=auth.uid();
  g public.word_siege_games;
  own_rack text;
  canonical text;
  visible text;
  result jsonb;
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (
    public.has_permanent_entitlement_v1(u,'letter_table')
    or public.has_pro_access_v1(u)
  ) then
    raise exception 'premium_letter_table_required';
  end if;
  select * into g from public.word_siege_games where id=p_game_id;
  if g.id is null or u not in (g.player_one_id,g.player_two_id) then
    raise exception 'word_siege_not_participant';
  end if;
  own_rack:=case when u=g.player_one_id then g.player_one_rack else coalesce(g.player_two_rack,'') end;
  canonical:=private.word_siege_new_bag_v1(
    case when lower(coalesce(g.language,'tr'))='en' then 'en' else 'tr' end
  );
  select coalesce(string_agg(coalesce(cell->>'letter',''),''),'')
    into visible
    from jsonb_array_elements(g.board) cell;
  visible:=visible||own_rack;
  with alphabet as (
    select ch as letter,count(*)::integer as total
    from regexp_split_to_table(canonical,'') ch
    where ch<>''
    group by ch
  ), used as (
    select ch as letter,count(*)::integer as used
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
$function$;
