-- Free basic social access; paid modes and all game/scoring rules stay unchanged.
create or replace function public.can_use_pro_friend_list_v1(p_user_id uuid)
returns boolean language sql stable security invoker set search_path = '' as $$
 select p_user_id is not null and p_user_id = (select auth.uid());
$$;
drop policy if exists "friendships pro and series scoped read v2" on public.friendships;
drop policy if exists "friendships pro accepted read v1" on public.friendships;
create policy "friendships participant read v3" on public.friendships for select to authenticated
using (user_id=(select auth.uid()) or friend_id=(select auth.uid()));
-- Keep legacy RPC names for old installed clients; the helper now permits every signed-in owner.
-- Updating the existing response changes only the free friend-list entitlement.
do $$ declare d text; begin
 select pg_get_functiondef('public.get_vip_entitlements_v7()'::regprocedure) into d;
 d := replace(d,'''saved_friend_list'', pro','''saved_friend_list'', true');
 execute d;
end $$;

create table public.player_invite_codes (
 user_id uuid primary key references public.profiles(id) on delete cascade,
 code text not null unique default upper(substr(replace(gen_random_uuid()::text,'-',''),1,12)),
 created_at timestamptz not null default now(), check (code ~ '^[A-F0-9]{12}$')
);
alter table public.player_invite_codes enable row level security;
revoke all on public.player_invite_codes from anon,authenticated;
grant select on public.player_invite_codes to authenticated;
create policy "own invite code" on public.player_invite_codes for select to authenticated using (user_id=(select auth.uid()));
create or replace function public.get_my_invite_code_v1() returns text
language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid(); v text; begin
 if u is null then raise exception 'not_authenticated'; end if;
 insert into public.player_invite_codes(user_id) values(u) on conflict(user_id) do nothing;
 select code into v from public.player_invite_codes where user_id=u; return v;
end $$;
create or replace function public.use_player_invite_code_v1(p_code text) returns uuid
language plpgsql security definer set search_path='' as $$
declare u uuid:=auth.uid(); target uuid; begin
 if u is null then raise exception 'not_authenticated'; end if;
 if upper(trim(p_code)) !~ '^[A-F0-9]{12}$' then raise exception 'invalid_invite_code'; end if;
 select user_id into target from public.player_invite_codes where code=upper(trim(p_code));
 if target is null then raise exception 'invalid_invite_code'; end if;
 perform public.send_friend_request(target); return target;
end $$;
revoke all on function public.get_my_invite_code_v1(),public.use_player_invite_code_v1(text) from public,anon;
grant execute on function public.get_my_invite_code_v1(),public.use_player_invite_code_v1(text) to authenticated;

create table public.social_activity (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null references public.profiles(id) on delete cascade,
 actor_id uuid references public.profiles(id) on delete set null,
 kind text not null check(kind in ('friend_request','friend_accepted','challenge','your_turn','match_finished','rematch','friend_online')),
 target_kind text not null check(target_kind in ('social','activity','son_harf','siege','series')),
 target_id uuid, event_key text not null, created_at timestamptz not null default now(), read_at timestamptz,
 unique(user_id,event_key)
);
create index social_activity_inbox_idx on public.social_activity(user_id,created_at desc);
create index social_activity_unread_idx on public.social_activity(user_id,created_at desc) where read_at is null;
alter table public.social_activity enable row level security;
revoke all on public.social_activity from anon,authenticated;
grant select on public.social_activity to authenticated;
grant update(read_at) on public.social_activity to authenticated;
create policy "activity owner read" on public.social_activity for select to authenticated using(user_id=(select auth.uid()));
create policy "activity owner mark" on public.social_activity for update to authenticated using(user_id=(select auth.uid())) with check(user_id=(select auth.uid()));

create or replace function private.add_social_activity_v1(u uuid,a uuid,k text,t text,target uuid,key text) returns void
language plpgsql security definer set search_path='' as $$ begin
 if u is null or u=a then return; end if;
 if a is not null and exists(select 1 from public.user_blocks where (blocker_id=u and blocked_id=a) or (blocker_id=a and blocked_id=u)) then return; end if;
 insert into public.social_activity(user_id,actor_id,kind,target_kind,target_id,event_key)
 values(u,a,k,t,target,key) on conflict(user_id,event_key) do nothing;
end $$;
revoke all on function private.add_social_activity_v1(uuid,uuid,text,text,uuid,text) from public,anon,authenticated;

create or replace function private.social_activity_trigger_v1() returns trigger
language plpgsql security definer set search_path='' as $$
declare n jsonb:=to_jsonb(new); o jsonb; a uuid; b uuid; mode text; event text; seq text; begin
 if tg_op='UPDATE' then o:=to_jsonb(old); else o:='{}'::jsonb; end if;
 if tg_table_name='friendships' then
  a:=new.requested_by; b:=case when new.user_id=a then new.friend_id else new.user_id end;
  if new.status='pending' and (tg_op='INSERT' or old.status is distinct from new.status or old.requested_by is distinct from new.requested_by) then
   perform private.add_social_activity_v1(b,a,'friend_request','social',null,'friend:'||a||':'||new.updated_at);
  elsif new.status='accepted' and o->>'status' is distinct from 'accepted' then
   perform private.add_social_activity_v1(a,b,'friend_accepted','social',null,'accepted:'||b||':'||new.updated_at);
  end if;
 elsif tg_table_name in ('game_invites','word_siege_invites','word_siege_series_invites') then
  if new.status='pending' and tg_op='INSERT' then
   perform private.add_social_activity_v1(new.receiver_id,new.sender_id,'challenge','activity',new.id,'invite:'||new.id);
  end if;
 else
  mode:=case when tg_table_name='game_rooms' then 'son_harf' when n->>'game_mode'='series' then 'series' else 'siege' end;
  a:=coalesce((n->>'host_id')::uuid,(n->>'player_one_id')::uuid);
  b:=coalesce((n->>'guest_id')::uuid,(n->>'player_two_id')::uuid);
  if b is null or coalesce((n->>'is_bot')::boolean,false) then return new; end if;
  seq:=coalesce(n->>'action_seq',n->>'move_count','0');
  if n->>'status' in ('playing','sudden_death') and (tg_op='INSERT' or n->>'current_player_id' is distinct from o->>'current_player_id' or n->>'status' is distinct from o->>'status') then
   perform private.add_social_activity_v1((n->>'current_player_id')::uuid,case when (n->>'current_player_id')::uuid=a then b else a end,'your_turn',mode,new.id,'turn:'||new.id||':'||seq||':'||coalesce(n->>'current_player_id',''));
  end if;
  if n->>'status'='finished' and o->>'status' is distinct from 'finished' then
   perform private.add_social_activity_v1(a,b,'match_finished',mode,new.id,'finish:'||new.id);
   perform private.add_social_activity_v1(b,a,'match_finished',mode,new.id,'finish:'||new.id);
  end if;
  if mode='son_harf' then
   if coalesce((n->>'host_rematch')::boolean,false) and not coalesce((o->>'host_rematch')::boolean,false) then perform private.add_social_activity_v1(b,a,'rematch',mode,new.id,'rematch:'||new.id||':'||a); end if;
   if coalesce((n->>'guest_rematch')::boolean,false) and not coalesce((o->>'guest_rematch')::boolean,false) then perform private.add_social_activity_v1(a,b,'rematch',mode,new.id,'rematch:'||new.id||':'||b); end if;
  end if;
 end if;
 return new;
end $$;
revoke all on function private.social_activity_trigger_v1() from public,anon,authenticated;
create trigger social_friend_event_v1 after insert or update on public.friendships for each row execute function private.social_activity_trigger_v1();
create trigger social_duel_event_v1 after insert or update on public.game_rooms for each row execute function private.social_activity_trigger_v1();
create trigger social_siege_event_v1 after insert or update on public.word_siege_games for each row execute function private.social_activity_trigger_v1();
create trigger social_duel_invite_v1 after insert on public.game_invites for each row execute function private.social_activity_trigger_v1();
create trigger social_siege_invite_v1 after insert on public.word_siege_invites for each row execute function private.social_activity_trigger_v1();
create trigger social_series_invite_v1 after insert on public.word_siege_series_invites for each row execute function private.social_activity_trigger_v1();

create or replace function private.social_online_trigger_v1() returns trigger language plpgsql security definer set search_path='' as $$
declare f record; begin
 if new.presence_status='online' and old.presence_status is distinct from 'online' then
  for f in select case when user_id=new.id then friend_id else user_id end as id from public.friendships where status='accepted' and (user_id=new.id or friend_id=new.id) loop
   perform private.add_social_activity_v1(f.id,new.id,'friend_online','social',null,'online:'||new.id||':'||(now() at time zone 'Europe/Istanbul')::date);
  end loop;
 end if; return new;
end $$;
revoke all on function private.social_online_trigger_v1() from public,anon,authenticated;
create trigger social_online_event_v1 after update of presence_status on public.profiles for each row execute function private.social_online_trigger_v1();
