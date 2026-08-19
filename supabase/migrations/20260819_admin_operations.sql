-- Secure admin / operations backend.
create table if not exists public.admin_users (
  user_id uuid primary key references auth.users(id) on delete cascade,
  role text not null default 'admin' check(role in ('admin','operator')),
  created_at timestamptz not null default now()
);
alter table public.admin_users enable row level security;

create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path=public as $$
  select exists(select 1 from public.admin_users where user_id=auth.uid());
$$;

grant execute on function public.is_admin() to authenticated;

create policy admin_users_self_read on public.admin_users
for select to authenticated using (user_id=auth.uid());

-- Harden previously-created operations functions.
revoke execute on function public.run_game_maintenance() from public, anon, authenticated;
revoke execute on function public.system_health_snapshot() from public, anon, authenticated;

create or replace function public.admin_system_health()
returns jsonb language plpgsql security definer set search_path=public as $$
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  return public.system_health_snapshot();
end $$;

grant execute on function public.admin_system_health() to authenticated;

create or replace function public.admin_run_maintenance()
returns jsonb language plpgsql security definer set search_path=public as $$
declare r jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  r := public.run_game_maintenance();
  insert into public.admin_audit_log(admin_id,action,after_data) values(auth.uid(),'run_maintenance',r);
  return r;
end $$;
grant execute on function public.admin_run_maintenance() to authenticated;

create or replace function public.admin_set_config(p_key text,p_value jsonb)
returns void language plpgsql security definer set search_path=public as $$
declare oldv jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select value into oldv from public.app_config where key=p_key;
  insert into public.app_config(key,value,updated_at) values(p_key,p_value,now())
  on conflict(key) do update set value=excluded.value,updated_at=now();
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data,after_data)
  values(auth.uid(),'set_config','app_config',p_key,oldv,p_value);
end $$;
grant execute on function public.admin_set_config(text,jsonb) to authenticated;

create or replace function public.admin_close_room(p_room_id uuid,p_reason text default 'admin_closed')
returns void language plpgsql security definer set search_path=public as $$
declare beforev jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  select to_jsonb(r) into beforev from public.game_rooms r where id=p_room_id;
  update public.game_rooms set status='finished',turn_deadline=null,last_event=p_reason where id=p_room_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id,before_data)
  values(auth.uid(),'close_room','game_room',p_room_id::text,beforev);
end $$;
grant execute on function public.admin_close_room(uuid,text) to authenticated;

create or replace function public.admin_clear_chat_suspension(p_user_id uuid)
returns void language plpgsql security definer set search_path=public as $$;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  update public.profiles set chat_suspended_until=null where id=p_user_id;
  insert into public.admin_audit_log(admin_id,action,target_type,target_id)
  values(auth.uid(),'clear_chat_suspension','profile',p_user_id::text);
end;
$$;
grant execute on function public.admin_clear_chat_suspension(uuid) to authenticated;

-- Admin-only read policies for operational tables.
create policy system_events_admin_read on public.system_events for select to authenticated using(public.is_admin());
create policy admin_audit_admin_read on public.admin_audit_log for select to authenticated using(public.is_admin());
