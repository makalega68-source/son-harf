-- Permanent premium products for Kelime Kuşatması / Son Harf.
-- Purchase authority stays server-side. No client flag grants an entitlement.

create or replace function public.has_permanent_entitlement_v1(p_user_id uuid, p_key text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists(
    select 1
    from public.store_entitlements e
    where e.user_id = p_user_id
      and e.entitlement_key = p_key
      and e.status = 'active'
      and (e.expires_at is null or e.expires_at > now())
  )
$$;

revoke all on function public.has_permanent_entitlement_v1(uuid,text) from public,anon,authenticated;
grant execute on function public.has_permanent_entitlement_v1(uuid,text) to service_role;

create or replace function public.apply_verified_premium_purchase_v1(
  p_user_id uuid,
  p_product_id text,
  p_purchase_token text,
  p_order_id text default null,
  p_play_state text default null,
  p_acknowledgement_state text default null
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_purchase_id uuid;
  v_purchase_user_id uuid;
  v_purchase_product_id text;
  v_inserted boolean := false;
  v_key text;
  v_balance integer;
begin
  if p_user_id is null or nullif(trim(p_purchase_token),'') is null or length(trim(p_purchase_token)) < 8 then
    raise exception 'invalid_purchase';
  end if;
  if p_product_id not in ('series_game','letter_table','score_calculator','pro_lifetime') then
    raise exception 'unsupported_product';
  end if;
  if not exists(select 1 from public.profiles where id = p_user_id) then
    raise exception 'profile_not_found';
  end if;

  v_key := case p_product_id
    when 'series_game' then 'series_game'
    when 'letter_table' then 'letter_table'
    when 'score_calculator' then 'score_calculator'
    when 'pro_lifetime' then 'pro_lifetime'
  end;

  insert into public.purchases(
    user_id,product_id,purchase_token,order_id,status,purchased_at,verified_at,
    purchase_type,play_state,acknowledgement_state,last_checked_at,expires_at
  ) values (
    p_user_id,p_product_id,trim(p_purchase_token),nullif(trim(p_order_id),''),'verified',now(),now(),
    'one_time',p_play_state,p_acknowledgement_state,now(),null
  )
  on conflict(purchase_token) do nothing
  returning id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id;
  v_inserted := found;

  if not v_inserted then
    select id,user_id,product_id into v_purchase_id,v_purchase_user_id,v_purchase_product_id
    from public.purchases
    where purchase_token = trim(p_purchase_token)
    for update;
    if v_purchase_id is null then raise exception 'purchase_reconciliation_race'; end if;
    if v_purchase_user_id <> p_user_id then raise exception 'purchase_token_user_mismatch'; end if;
    if v_purchase_product_id <> p_product_id then raise exception 'purchase_token_product_mismatch'; end if;
  end if;

  update public.purchases
  set order_id = coalesce(nullif(trim(p_order_id),''),order_id),
      status = 'verified',
      play_state = p_play_state,
      acknowledgement_state = coalesce(p_acknowledgement_state,acknowledgement_state),
      last_checked_at = now()
  where id = v_purchase_id;

  insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
  values(p_user_id,v_key,'play',trim(p_purchase_token),'active',null,now())
  on conflict(user_id,entitlement_key,source_type,source_id)
  do update set status='active',expires_at=null,updated_at=now();

  if p_product_id = 'pro_lifetime' then
    -- PRO owns every premium feature permanently. Child entitlements make feature checks explicit.
    insert into public.store_entitlements(user_id,entitlement_key,source_type,source_id,status,expires_at,updated_at)
    select p_user_id,k,'pro_bundle',trim(p_purchase_token),'active',null,now()
    from unnest(array['series_game','letter_table','score_calculator']) k
    on conflict(user_id,entitlement_key,source_type,source_id)
    do update set status='active',expires_at=null,updated_at=now();

    -- Existing app surfaces already use profiles.is_vip for ad-free, friends, history and PRO cosmetics.
    update public.profiles set is_vip=true,updated_at=now() where id=p_user_id;

    insert into public.user_inventory(user_id,item_id)
    values(p_user_id,'frame_round_golden_avatar')
    on conflict(user_id,item_id) do nothing;

    insert into public.user_equipped_cosmetics(user_id)
    values(p_user_id)
    on conflict(user_id) do nothing;

    update public.user_equipped_cosmetics
    set profile_frame_id='frame_round_golden_avatar',updated_at=now()
    where user_id=p_user_id;

    if v_inserted then
      update public.profiles
      set diamonds=coalesce(diamonds,0)+100,updated_at=now()
      where id=p_user_id
      returning diamonds into v_balance;
      insert into public.diamond_ledger(user_id,delta,reason)
      values(p_user_id,100,'google_play_pro_lifetime:'||trim(p_purchase_token));
    end if;
  end if;

  if v_balance is null then select diamonds into v_balance from public.profiles where id=p_user_id; end if;
  return jsonb_build_object(
    'success',true,
    'already_processed',not v_inserted,
    'purchase_id',v_purchase_id,
    'product_id',p_product_id,
    'entitlement_key',v_key,
    'son_coin_granted',case when p_product_id='pro_lifetime' and v_inserted then 100 else 0 end,
    'son_coin_balance',v_balance
  );
end
$$;

revoke all on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) from public,anon,authenticated;
grant execute on function public.apply_verified_premium_purchase_v1(uuid,text,text,text,text,text) to service_role;

-- Reconcile subscription VIP without ever removing a lifetime PRO purchase.
do $reconcile_lifetime$
declare
  f text;
begin
  f := pg_get_functiondef('public.reconcile_play_entitlement_v1(text,text,timestamptz,boolean)'::regprocedure);
  if position('has_permanent_entitlement_v1' in f) = 0 then
    if position('update public.profiles set is_vip=v_vip_active,updated_at=now() where id=v_purchase.user_id;' in f) = 0 then
      raise exception 'reconcile_vip_update_hook_missing';
    end if;
    f := replace(
      f,
      'update public.profiles set is_vip=v_vip_active,updated_at=now() where id=v_purchase.user_id;',
      'update public.profiles set is_vip=(v_vip_active or public.has_permanent_entitlement_v1(v_purchase.user_id,''pro_lifetime'')),updated_at=now() where id=v_purchase.user_id;'
    );
    execute f;
  end if;
end
$reconcile_lifetime$;

create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  u uuid:=auth.uid();
  profile_vip boolean:=false;
  pro boolean:=false;
  claimed boolean:=false;
  w public.vip_joker_wallet;
  score_access boolean:=false;
  letter_access boolean:=false;
  series_access boolean:=false;
begin
  if u is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into profile_vip from public.profiles where id=u;
  pro := profile_vip or public.has_permanent_entitlement_v1(u,'pro_lifetime');
  score_access := pro or public.has_permanent_entitlement_v1(u,'score_calculator');
  letter_access := pro or public.has_permanent_entitlement_v1(u,'letter_table');
  series_access := pro or public.has_permanent_entitlement_v1(u,'series_game');

  insert into public.vip_joker_wallet(user_id) values(u) on conflict(user_id) do nothing;
  select * into w from public.vip_joker_wallet where user_id=u;
  select exists(select 1 from public.vip_daily_joker_claims where user_id=u and claim_date=current_date) into claimed;

  return jsonb_build_object(
    'is_vip',profile_vip,
    'is_pro',pro,
    'daily_jokers_claimed',claimed,
    'freezer_count',coalesce(w.freezer_count,0),
    'swap_count',coalesce(w.swap_count,0),
    'hint_count',coalesce(w.hint_count,0),
    'streak_shield_count',coalesce(w.streak_shield_count,0),
    'multiplier_count',coalesce(w.multiplier_count,0),
    'xp_multiplier',1,
    'diamond_multiplier',1,
    'rewarded_ad_bypass',pro,
    'used_words_access',pro,
    'direct_messages_access',true,
    'ranked_live_assist',true,
    'post_match_analysis',pro,
    'saved_friend_list',pro,
    'private_rooms',pro,
    'score_calculator_access',score_access,
    'letter_table_access',letter_access,
    'series_game_access',series_access,
    'active_game_limit',case when pro then 50 else 10 end
  );
end
$$;
revoke all on function public.get_vip_entitlements_v7() from public,anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated,service_role;

-- Authoritative score preview. The private preview engine is the same engine family used by submit.
create or replace function public.preview_word_siege_move_pro_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog,public,private,pg_temp
as $$
declare
  u uuid:=auth.uid();
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (public.has_permanent_entitlement_v1(u,'score_calculator')
          or public.has_permanent_entitlement_v1(u,'pro_lifetime')
          or exists(select 1 from public.profiles p where p.id=u and coalesce(p.is_vip,false))) then
    raise exception 'premium_score_calculator_required';
  end if;
  if not exists(select 1 from public.word_siege_games g where g.id=p_game_id and u in (g.player_one_id,g.player_two_id)) then
    raise exception 'word_siege_not_participant';
  end if;
  return to_jsonb(private.word_siege_preview_move_v1(p_game_id,p_placements,coalesce(p_horizontal,true)));
end
$$;
revoke all on function public.preview_word_siege_move_pro_v1(uuid,jsonb,boolean) from public,anon;
grant execute on function public.preview_word_siege_move_pro_v1(uuid,jsonb,boolean) to authenticated,service_role;

-- Harf Tablosu shows unseen counts (bag + rival rack) without exposing rival rack or draw order.
create or replace function public.get_word_siege_letter_table_v1(p_game_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog,public,private,pg_temp
as $$
declare
  u uuid:=auth.uid();
  g public.word_siege_games;
  own_rack text;
  canonical text;
  visible text;
  result jsonb;
begin
  if u is null then raise exception 'word_siege_unauthorized'; end if;
  if not (public.has_permanent_entitlement_v1(u,'letter_table')
          or public.has_permanent_entitlement_v1(u,'pro_lifetime')
          or exists(select 1 from public.profiles p where p.id=u and coalesce(p.is_vip,false))) then
    raise exception 'premium_letter_table_required';
  end if;
  select * into g from public.word_siege_games where id=p_game_id;
  if g.id is null or u not in (g.player_one_id,g.player_two_id) then raise exception 'word_siege_not_participant'; end if;
  own_rack := case when u=g.player_one_id then g.player_one_rack else coalesce(g.player_two_rack,'') end;

  if lower(coalesce(g.language,'tr'))='en' then
    canonical := repeat('E',12)||repeat('A',9)||repeat('I',9)||repeat('O',8)||repeat('N',6)||repeat('R',6)||repeat('T',6)||repeat('L',4)||repeat('S',4)||repeat('U',4)||repeat('D',4)||repeat('G',3)||repeat('B',2)||repeat('C',2)||repeat('M',2)||repeat('P',2)||repeat('F',2)||repeat('H',2)||repeat('V',2)||repeat('W',2)||repeat('Y',2)||'KJXQZ';
  else
    canonical := repeat('A',12)||repeat('B',2)||repeat('C',2)||repeat('Ç',2)||repeat('D',2)||repeat('E',8)||'F'||'G'||'Ğ'||'H'||repeat('I',4)||repeat('İ',7)||'J'||repeat('K',7)||repeat('L',7)||repeat('M',4)||repeat('N',5)||repeat('O',3)||'Ö'||'P'||repeat('R',6)||repeat('S',3)||repeat('Ş',2)||repeat('T',5)||repeat('U',3)||repeat('Ü',2)||'V'||repeat('Y',2)||repeat('Z',2);
  end if;

  select coalesce(string_agg(coalesce(cell->>'letter',''),'') ,'') into visible
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
  select jsonb_agg(jsonb_build_object('letter',a.letter,'remaining',greatest(0,a.total-coalesce(u2.used,0))) order by a.letter)
  into result
  from alphabet a left join used u2 using(letter);

  return coalesce(result,'[]'::jsonb);
end
$$;
revoke all on function public.get_word_siege_letter_table_v1(uuid) from public,anon;
grant execute on function public.get_word_siege_letter_table_v1(uuid) to authenticated,service_role;

create or replace function public.word_siege_active_limit_v1(p_user_id uuid)
returns integer
language sql
stable
security definer
set search_path=''
as $$
  select case when exists(select 1 from public.profiles p where p.id=p_user_id and coalesce(p.is_vip,false))
                    or public.has_permanent_entitlement_v1(p_user_id,'pro_lifetime')
              then 50 else 10 end
$$;
revoke all on function public.word_siege_active_limit_v1(uuid) from public,anon,authenticated;
grant execute on function public.word_siege_active_limit_v1(uuid) to service_role;

-- Raise the existing Kuşatma active-game cap only for PRO, keeping all matchmaking logic unchanged.
do $word_siege_limit$
declare
  f text;
begin
  f:=pg_get_functiondef('private.find_or_create_word_siege_game_v2(text,integer)'::regprocedure);
  if position('word_siege_active_limit_v1' in f)=0 then
    if position('if v_active_count>=10 then' in f)=0 then raise exception 'word_siege_limit_hook_missing'; end if;
    f:=replace(f,'if v_active_count>=10 then','if v_active_count>=public.word_siege_active_limit_v1(v_uid) then');
    execute f;
  end if;

  f:=pg_get_functiondef('public.respond_word_siege_invite_v1(uuid,boolean)'::regprocedure);
  if position('word_siege_active_limit_v1' in f)=0 then
    if position('if v_sender_active >= 10 or v_receiver_active >= 10 then' in f)=0 then raise exception 'word_siege_friend_limit_hook_missing'; end if;
    f:=replace(
      f,
      'if v_sender_active >= 10 or v_receiver_active >= 10 then',
      'if v_sender_active >= public.word_siege_active_limit_v1(inv.sender_id) or v_receiver_active >= public.word_siege_active_limit_v1(inv.receiver_id) then'
    );
    execute f;
  end if;
end
$word_siege_limit$;

select pg_notify('pgrst','reload schema');
