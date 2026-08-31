-- Son Harf admin/owner security hardening.
-- Extends the existing admin platform; does not create a parallel administration system.

-- Keep the two approved administrator accounts in the server-side allowlist.
insert into public.admin_users(user_id, role, free_test_purchases)
select u.id, 'admin', case when lower(u.email)='makalega58@gmail.com' then true else false end
from auth.users u
where lower(u.email) in ('makalega58@gmail.com','makalega68@gmail.com')
on conflict(user_id) do update set role='admin';

-- The special gameplay owner entitlement is bound to the authenticated user id,
-- not a client-provided email or local preference.
insert into public.owner_game_accounts(
  user_id,lifetime_vip,unlimited_diamonds,unlimited_son_coin,active,notes,updated_by
)
select u.id,true,true,true,true,'Kurucu rekabet hesabı',u.id
from auth.users u
where lower(u.email)='makalega68@gmail.com'
on conflict(user_id) do update set
  lifetime_vip=true,
  unlimited_diamonds=true,
  unlimited_son_coin=true,
  active=true,
  updated_at=now();

-- Do not authorize from a client-visible/stale JWT email claim. Resolve the
-- authenticated uid against auth.users and the private server allowlist.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
  select exists(
    select 1
    from public.admin_users a
    join auth.users u on u.id=a.user_id
    where a.user_id=auth.uid()
      and lower(coalesce(u.email,'')) in ('makalega58@gmail.com','makalega68@gmail.com')
      and a.role='admin'
  )
$$;
revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated, service_role;

-- Minimal self-capability endpoint used only to decide whether the Admin Panel
-- entry can be rendered. Critical panel data/actions remain independently guarded.
create or replace function public.admin_access_v1()
returns table(
  authorized boolean,
  admin_role text,
  lifetime_vip boolean,
  unlimited_diamonds boolean,
  unlimited_son_coin boolean
)
language sql
stable
security definer
set search_path = pg_catalog, public, auth, pg_temp
as $$
  select
    public.is_admin(),
    case when public.is_admin() then coalesce(a.role,'') else '' end,
    coalesce(o.active and o.lifetime_vip,false),
    coalesce(o.active and o.unlimited_diamonds,false),
    coalesce(o.active and o.unlimited_son_coin,false)
  from (select auth.uid() uid) me
  left join public.admin_users a on a.user_id=me.uid
  left join public.owner_game_accounts o on o.user_id=me.uid
$$;
revoke all on function public.admin_access_v1() from public, anon;
grant execute on function public.admin_access_v1() to authenticated, service_role;

-- Make audit schema explicitly capable of recording action outcome/error in new
-- maintenance/admin paths while preserving all old rows and callers.
alter table public.admin_audit_log
  add column if not exists outcome text not null default 'success',
  add column if not exists error_text text;

-- Chat feature flag is enforced server-side for both live Son Harf and Word Siege.
create or replace function public.enforce_chat_admin_control_v1()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
begin
  if not public.sonharf_config_enabled('chat_enabled',true) and not public.is_admin() then
    raise exception 'chat_disabled';
  end if;
  return new;
end
$$;
revoke all on function public.enforce_chat_admin_control_v1() from public, anon, authenticated;

drop trigger if exists enforce_chat_admin_control_v1 on public.chat_messages;
create trigger enforce_chat_admin_control_v1
before insert on public.chat_messages
for each row execute function public.enforce_chat_admin_control_v1();

drop trigger if exists enforce_word_siege_chat_admin_control_v1 on public.word_siege_messages;
create trigger enforce_word_siege_chat_admin_control_v1
before insert on public.word_siege_messages
for each row execute function public.enforce_chat_admin_control_v1();

-- Maintenance/matchmaking controls must also cover the asynchronous Word Siege
-- entry point. Existing active Word Siege RPCs are intentionally not blocked.
create or replace function public.find_or_create_word_siege_game_v2(
  p_language text default 'tr',
  p_turn_duration_hours integer default 12
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
begin
  if public.sonharf_config_enabled('maintenance_mode',false) and not public.is_admin() then
    raise exception 'maintenance_mode';
  end if;
  if not public.sonharf_config_enabled('matchmaking_enabled',true) and not public.is_admin() then
    raise exception 'matchmaking_disabled';
  end if;
  return private.find_or_create_word_siege_game_v2(p_language,p_turn_duration_hours);
end
$$;
revoke all on function public.find_or_create_word_siege_game_v2(text,integer) from public, anon;
grant execute on function public.find_or_create_word_siege_game_v2(text,integer) to authenticated, service_role;

-- Safe maintenance: never auto-cancel a merely inactive competitive match.
-- stale_rooms becomes a conservative diagnostic/no-op; targeted queue, resolved
-- quiz and false-presence repairs remain idempotent and auditable.
create or replace function public.admin_repair_v1(p_action text)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
declare
  v_action text:=lower(trim(coalesce(p_action,'')));
  v_count bigint:=0;
  v_total bigint:=0;
  v_detected bigint:=0;
  v_result jsonb;
begin
  if not public.is_admin() then raise exception 'admin_required'; end if;
  if v_action not in ('maintenance','stale_rooms','stale_queue','stuck_quizzes','presence','all') then
    raise exception 'invalid_repair_action';
  end if;

  if v_action in ('maintenance','all') then
    v_result:=public.admin_run_maintenance();
  end if;

  if v_action in ('stale_rooms','all') then
    select count(*) into v_detected
    from public.game_rooms r
    where r.status in ('playing','quiz','final','sudden_death','paused')
      and greatest(coalesce(r.host_last_seen_at,r.created_at),coalesce(r.guest_last_seen_at,r.created_at)) < now()-interval '5 minutes';
    -- Deliberately do not mutate/finish these rooms: inactivity alone is not
    -- sufficient proof that a competitive result is invalid.
  end if;

  if v_action in ('stale_queue','all') then
    update public.matchmaking_queue q
    set status='cancelled',room_id=null,heartbeat_at=now()
    where q.status='waiting' and q.heartbeat_at < now()-interval '2 minutes';
    get diagnostics v_count=row_count;
    v_total:=v_total+v_count;
  end if;

  if v_action in ('stuck_quizzes','all') then
    with latest as (
      select distinct on (t.room_id)
        t.room_id,t.resolved_at,t.result_until,t.answer_deadline,
        t.resume_status,t.resume_current_player_id,t.resume_bot_turn
      from public.trivia_rounds t
      order by t.room_id,t.created_at desc
    )
    update public.game_rooms r set
      status=coalesce(l.resume_status,'playing'),
      current_player_id=case when coalesce(l.resume_bot_turn,false) then null else l.resume_current_player_id end,
      bot_turn=coalesce(l.resume_bot_turn,false),
      turn_deadline=case when coalesce(l.resume_bot_turn,false) then null else public.sonharf_turn_deadline(r.game_mode) end,
      last_event='admin_quiz_recovered'
    from latest l
    where r.id=l.room_id and r.status='quiz'
      and l.resolved_at is not null and l.result_until < now()-interval '1 minute';
    get diagnostics v_count=row_count;
    v_total:=v_total+v_count;
    -- Unresolved expired quiz rounds are diagnosed by admin_health_v1. They are
    -- not cancelled here because that could manufacture a competitive result.
  end if;

  if v_action in ('presence','all') then
    update public.profiles p
    set presence_status=case when p.last_seen_at>=now()-interval '10 minutes' then 'online' else 'offline' end
    where p.presence_status='in_game'
      and not exists(
        select 1 from public.game_rooms r
        where r.status in ('waiting','playing','quiz','final','sudden_death','paused')
          and (r.host_id=p.id or r.guest_id=p.id)
      );
    get diagnostics v_count=row_count;
    v_total:=v_total+v_count;
  end if;

  insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data,outcome)
  values(
    auth.uid(),'repair_'||v_action,'system',v_action,
    jsonb_build_object('affected',v_total,'stale_rooms_detected',v_detected,'maintenance',v_result),
    'success'
  );

  return jsonb_build_object(
    'success',true,'action',v_action,'affected',v_total,
    'stale_rooms_detected',v_detected,'maintenance',v_result
  );
end
$$;
revoke all on function public.admin_repair_v1(text) from public, anon;
grant execute on function public.admin_repair_v1(text) to authenticated, service_role;
