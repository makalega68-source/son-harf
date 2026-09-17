begin;

-- Bilingual announcement/maintenance copy, backwards-compatible with the legacy message column.
alter table public.admin_announcement
  add column if not exists message_tr text not null default '',
  add column if not exists message_en text not null default '',
  add column if not exists maintenance boolean not null default false;

update public.admin_announcement
set message_tr = case when message_tr = '' then coalesce(message,'') else message_tr end,
    message_en = case when message_en = '' then coalesce(message,'') else message_en end
where singleton = true;

create or replace function public.admin_get_announcement_v2()
returns table(message_tr text, message_en text, enabled boolean, maintenance boolean, updated_at timestamptz)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select a.message_tr,a.message_en,a.enabled,a.maintenance,a.updated_at
  from public.admin_announcement a where a.singleton=true;
end;
$$;
revoke all on function public.admin_get_announcement_v2() from public, anon;
grant execute on function public.admin_get_announcement_v2() to authenticated;

create or replace function public.admin_set_announcement_v2(
  p_message_tr text,
  p_message_en text,
  p_enabled boolean,
  p_maintenance boolean
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select to_jsonb(a) into v_before from public.admin_announcement a where a.singleton=true;
  insert into public.admin_announcement(singleton,message,message_tr,message_en,enabled,maintenance,updated_at,updated_by)
  values(
    true,
    left(coalesce(p_message_tr,''),500),
    left(coalesce(p_message_tr,''),500),
    left(coalesce(p_message_en,''),500),
    coalesce(p_enabled,false),
    coalesce(p_maintenance,false),
    now(),auth.uid()
  )
  on conflict(singleton) do update set
    message=excluded.message,
    message_tr=excluded.message_tr,
    message_en=excluded.message_en,
    enabled=excluded.enabled,
    maintenance=excluded.maintenance,
    updated_at=excluded.updated_at,
    updated_by=excluded.updated_by;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'announcement_update_v2','system','announcement',v_before,
         jsonb_build_object('enabled',coalesce(p_enabled,false),'maintenance',coalesce(p_maintenance,false),
                            'tr_length',length(coalesce(p_message_tr,'')),'en_length',length(coalesce(p_message_en,''))));
end;
$$;
revoke all on function public.admin_set_announcement_v2(text,text,boolean,boolean) from public, anon;
grant execute on function public.admin_set_announcement_v2(text,text,boolean,boolean) to authenticated;

-- Safer allow-listed feature flag mutation. The Android client no longer needs arbitrary config writes.
create or replace function public.admin_set_game_control_v1(p_key text, p_enabled boolean)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_key text:=lower(trim(coalesce(p_key,''))); v_before jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_key not in (
    'word_siege_enabled','word_siege_matchmaking_enabled',
    'son_harf_enabled','son_harf_matchmaking_enabled','son_harf_bot_fallback_enabled',
    'chat_enabled','maintenance_mode'
  ) then raise exception 'unsupported_game_control'; end if;
  select value into v_before from public.app_config where key=v_key;
  insert into public.app_config(key,value,updated_at)
  values(v_key,to_jsonb(coalesce(p_enabled,false)),now())
  on conflict(key) do update set value=excluded.value,updated_at=now();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_game_control','app_config',v_key,v_before,to_jsonb(coalesce(p_enabled,false)));
end;
$$;
revoke all on function public.admin_set_game_control_v1(text,boolean) from public, anon;
grant execute on function public.admin_set_game_control_v1(text,boolean) to authenticated;

create or replace function public.admin_game_controls_v1()
returns table(config_key text, title text, detail text, enabled boolean)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query values
    ('word_siege_enabled','Kelime Kuşatması aktif','Yeni Kelime Kuşatması girişleri açık.',public.sonharf_config_enabled('word_siege_enabled',true)),
    ('word_siege_matchmaking_enabled','Kelime Kuşatması eşleşme','Rastgele Kelime Kuşatması eşleşmeleri açık.',public.sonharf_config_enabled('word_siege_matchmaking_enabled',true)),
    ('son_harf_enabled','Son Harf aktif','Yeni Son Harf maç girişleri açık.',public.sonharf_config_enabled('son_harf_enabled',true)),
    ('son_harf_matchmaking_enabled','Son Harf eşleşme','Rastgele Son Harf eşleşmeleri açık.',public.sonharf_config_enabled('son_harf_matchmaking_enabled',true)),
    ('son_harf_bot_fallback_enabled','Son Harf bot fallback','15 saniye sonra uygun bot eşleşmesi açık.',public.sonharf_config_enabled('son_harf_bot_fallback_enabled',true)),
    ('chat_enabled','Maç sohbeti','Mevcut maç sohbeti sistemi için global operasyon bayrağı.',public.sonharf_config_enabled('chat_enabled',true)),
    ('maintenance_mode','Bakım modu','Yeni oyun girişlerini geçici olarak durdurur; adminler hariç fail-closed.',public.sonharf_config_enabled('maintenance_mode',false));
end;
$$;
revoke all on function public.admin_game_controls_v1() from public, anon;
grant execute on function public.admin_game_controls_v1() to authenticated;

-- Son Harf: game-active/matchmaking gates plus 15s bot fallback flag.
create or replace function public.join_random_matchmaking(p_language text)
returns public.game_rooms
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then raise exception 'maintenance_mode'; end if;
  if not public.sonharf_config_enabled('son_harf_enabled',true) and not public.is_admin() then raise exception 'son_harf_disabled'; end if;
  if (not public.sonharf_config_enabled('son_harf_matchmaking_enabled',true)
      or not public.sonharf_config_enabled('matchmaking_enabled',true)) and not public.is_admin() then raise exception 'matchmaking_disabled'; end if;
  return public.join_random_matchmaking_v2(p_language,public.get_game_mode_v1());
end;
$$;

create or replace function public.poll_random_matchmaking_v2()
returns public.game_rooms
language plpgsql
security definer
set search_path = pg_catalog, public, extensions, pg_temp
as $$
declare q public.matchmaking_queue; r public.game_rooms; generated_code text;
begin
  select * into q from public.matchmaking_queue where user_id=auth.uid() for update;
  if q.user_id is null then return null; end if;
  if q.status='matched' and q.room_id is not null then select * into r from public.game_rooms where id=q.room_id; return r; end if;
  if q.status='waiting' then
    update public.matchmaking_queue set heartbeat_at=now() where user_id=auth.uid();
    if q.queued_at<=now()-interval '15 seconds' then
      if not public.sonharf_config_enabled('son_harf_bot_fallback_enabled',true) then return null; end if;
      generated_code:=upper(substr(md5(random()::text||clock_timestamp()::text),1,6));
      insert into public.game_rooms(code,host_id,status,current_player_id,turn_deadline,language,is_bot,bot_name,bot_turn,room_type,game_mode,host_last_seen_at)
      values(generated_code,auth.uid(),'playing',auth.uid(),public.sonharf_turn_deadline(q.game_mode),q.language,true,
             case when q.language='tr' then 'KelimeBot' else 'WordBot' end,false,'bot',q.game_mode,now()) returning * into r;
      update public.matchmaking_queue set status='matched',room_id=r.id,heartbeat_at=now() where user_id=auth.uid();
      update public.profiles set presence_status='in_game',last_seen_at=now() where id=auth.uid();
      return r;
    end if;
  end if;
  return null;
end;
$$;

-- Friend invites also respect the game-active switch, without conflating it with random matchmaking.
create or replace function public.invite_friend_to_game(p_friend_id uuid, p_language text)
returns public.game_invites
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
declare inv public.game_invites; f public.friendships; p public.profiles;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then raise exception 'maintenance_mode'; end if;
  if not public.sonharf_config_enabled('son_harf_enabled',true) and not public.is_admin() then raise exception 'son_harf_disabled'; end if;
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;
  select * into f from public.friendships where user_id=least(auth.uid(),p_friend_id) and friend_id=greatest(auth.uid(),p_friend_id) and status='accepted';
  if f.user_id is null then raise exception 'not_friends'; end if;
  if exists(select 1 from public.user_blocks where (blocker_id=auth.uid() and blocked_id=p_friend_id) or (blocker_id=p_friend_id and blocked_id=auth.uid())) then raise exception 'blocked_relationship'; end if;
  select * into p from public.profiles where id=p_friend_id;
  if p.id is null then raise exception 'friend_not_found'; end if;
  if p.presence_status='in_game' then raise exception 'friend_in_game'; end if;
  update public.game_invites set status='expired',responded_at=now() where receiver_id=p_friend_id and status='pending' and expires_at<now();
  insert into public.game_invites(sender_id,receiver_id,language,expires_at)
  values(auth.uid(),p_friend_id,p_language,case when p.presence_status='online' then now()+interval '2 minutes' else now()+interval '24 hours' end)
  returning * into inv;
  insert into public.notification_outbox(user_id,kind,payload)
  values(p_friend_id,'game_invite',jsonb_build_object('invite_id',inv.id,'sender_id',auth.uid(),'language',p_language));
  return inv;
end;
$$;

-- Kelime Kuşatması v1 now goes through the public guarded v2 wrapper instead of bypassing it.
create or replace function public.find_or_create_word_siege_game_v1(p_language text default 'tr')
returns public.word_siege_games
language sql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
  select public.find_or_create_word_siege_game_v2(p_language,12)
$$;

create or replace function public.find_or_create_word_siege_game_v2(p_language text default 'tr', p_turn_duration_hours integer default 12)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then raise exception 'maintenance_mode'; end if;
  if not public.sonharf_config_enabled('word_siege_enabled',true) and not public.is_admin() then raise exception 'word_siege_disabled'; end if;
  if (not public.sonharf_config_enabled('word_siege_matchmaking_enabled',true)
      or not public.sonharf_config_enabled('matchmaking_enabled',true)) and not public.is_admin() then raise exception 'matchmaking_disabled'; end if;
  return private.find_or_create_word_siege_game_v2(p_language,p_turn_duration_hours);
end;
$$;

-- Admin-visible cosmetic store management. Only existing cosmetic catalog rows are mutable.
create or replace function public.admin_shop_items_v1()
returns table(item_id text, kind text, name_tr text, name_en text, diamond_price integer, vip_only boolean, active boolean, rarity text, updated_hint text)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return query
  select s.id,s.kind,s.name_tr,s.name_en,s.diamond_price,s.vip_only,s.active,s.rarity,
         case when s.available_until is null then 'Süresiz' else s.available_until::text end
  from public.shop_items s
  order by s.kind,s.sort_order,s.id;
end;
$$;
revoke all on function public.admin_shop_items_v1() from public, anon;
grant execute on function public.admin_shop_items_v1() to authenticated;

create or replace function public.admin_set_shop_item_v1(p_item_id text, p_active boolean, p_diamond_price integer)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_before jsonb; v_kind text;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if p_diamond_price is null or p_diamond_price < 0 or p_diamond_price > 1000000 then raise exception 'invalid_price'; end if;
  select to_jsonb(s),s.kind into v_before,v_kind from public.shop_items s where s.id=trim(coalesce(p_item_id,'')) for update;
  if v_before is null then raise exception 'unknown_item'; end if;
  if v_kind not in ('badge','emoji_pack','game_theme','keyboard_theme','mascot','name_style','nameplate','profile_frame','title','victory_effect','vs_intro','word_effect') then raise exception 'non_cosmetic_item_blocked'; end if;
  update public.shop_items set active=coalesce(p_active,false),diamond_price=p_diamond_price where id=trim(p_item_id);
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_shop_item','shop_item',trim(p_item_id),v_before,
         jsonb_build_object('active',coalesce(p_active,false),'diamond_price',p_diamond_price));
end;
$$;
revoke all on function public.admin_set_shop_item_v1(text,boolean,integer) from public, anon;
grant execute on function public.admin_set_shop_item_v1(text,boolean,integer) to authenticated;

-- Test account tools operate only on explicitly configured active owner/test accounts.
create or replace function public.admin_test_inventory_v1(p_user_id uuid)
returns table(item_id text, kind text, name_tr text, quantity integer, is_equipped boolean, acquired_at timestamptz)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if not exists(select 1 from public.owner_game_accounts o where o.user_id=p_user_id and o.active) then raise exception 'test_account_required'; end if;
  return query
  select i.item_id,s.kind,s.name_tr,i.quantity,i.is_equipped,i.acquired_at
  from public.user_inventory i join public.shop_items s on s.id=i.item_id
  where i.user_id=p_user_id
  order by i.acquired_at desc,i.item_id;
end;
$$;
revoke all on function public.admin_test_inventory_v1(uuid) from public, anon;
grant execute on function public.admin_test_inventory_v1(uuid) to authenticated;

create or replace function public.admin_grant_test_item_v1(p_user_id uuid, p_item_id text)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
declare v_kind text;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if not exists(select 1 from public.owner_game_accounts o where o.user_id=p_user_id and o.active) then raise exception 'test_account_required'; end if;
  select kind into v_kind from public.shop_items where id=trim(coalesce(p_item_id,'')) and active;
  if v_kind is null then raise exception 'active_item_required'; end if;
  if v_kind not in ('badge','emoji_pack','game_theme','keyboard_theme','mascot','name_style','nameplate','profile_frame','title','victory_effect','vs_intro','word_effect') then raise exception 'non_cosmetic_item_blocked'; end if;
  insert into public.user_inventory(user_id,item_id,quantity,is_equipped)
  values(p_user_id,trim(p_item_id),1,false)
  on conflict(user_id,item_id) do update set quantity=greatest(public.user_inventory.quantity,1);
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data)
  values(auth.uid(),'grant_test_item','test_account',p_user_id::text,jsonb_build_object('item_id',trim(p_item_id),'kind',v_kind));
end;
$$;
revoke all on function public.admin_grant_test_item_v1(uuid,text) from public, anon;
grant execute on function public.admin_grant_test_item_v1(uuid,text) to authenticated;

commit;
