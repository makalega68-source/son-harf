-- Mascot room works for the store mascots (entitlements mascot_<id>): care, feed, bond.
create or replace function public.mascot_id_is_playable_v1(p_mascot_id text)
 returns boolean language sql security definer set search_path to 'public','pg_temp' as $$
  select p_mascot_id='mascot_white'
      or p_mascot_id in ('mascot_klasik','mascot_pembe','mascot_mavi_seytancik','mascot_kirmizi_seytancik','mascot_tekir','mascot_robot','mascot_astronot')
      or exists(select 1 from public.shop_items where id=p_mascot_id and kind='mascot' and active=true);
$$;

create or replace function public.ensure_mascot_progress_v1(p_mascot_id text)
 returns void language plpgsql security definer set search_path to 'public','pg_temp' as $function$
declare v_uid uuid:=auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  if not public.mascot_id_is_playable_v1(p_mascot_id) then raise exception 'mascot_not_available'; end if;
  if p_mascot_id<>'mascot_white'
     and not exists(select 1 from public.user_inventory where user_id=v_uid and item_id=p_mascot_id)
     and not exists(select 1 from public.store_entitlements e where e.user_id=v_uid and e.status='active'
                    and e.entitlement_key=p_mascot_id and (e.expires_at is null or e.expires_at>now()))
  then raise exception 'mascot_not_owned'; end if;

  insert into public.user_mascot_progress(user_id,mascot_id,pet_name)
  values(v_uid,p_mascot_id,case p_mascot_id
      when 'mascot_white' then 'Lyra' when 'mascot_chibi_wizard' then 'Neris'
      when 'mascot_klasik' then 'Obi' when 'mascot_pembe' then 'Pinki' when 'mascot_mavi_seytancik' then 'Buzi'
      when 'mascot_kirmizi_seytancik' then 'Zıpır' when 'mascot_tekir' then 'Mırnav' when 'mascot_robot' then 'Bipbop'
      when 'mascot_astronot' then 'Nova' else 'Dostum' end)
  on conflict(user_id,mascot_id) do nothing;
end $function$;
