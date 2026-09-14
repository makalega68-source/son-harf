-- Club Challenges v1
-- A 24-hour, skill-only competition between two clubs. Points are derived from
-- server-recorded club point events; the client cannot submit challenge points.

create table if not exists public.club_challenges (
  id uuid primary key default gen_random_uuid(),
  challenger_club_id uuid not null references public.clubs(id) on delete cascade,
  challenged_club_id uuid not null references public.clubs(id) on delete cascade,
  created_by uuid not null references public.profiles(id) on delete restrict,
  accepted_by uuid references public.profiles(id) on delete set null,
  status text not null default 'pending'
    check (status in ('pending','active','declined','expired','completed')),
  created_at timestamptz not null default now(),
  starts_at timestamptz,
  ends_at timestamptz,
  responded_at timestamptz,
  check (challenger_club_id <> challenged_club_id),
  check ((status = 'active' and starts_at is not null and ends_at is not null and ends_at = starts_at + interval '24 hours') or status <> 'active')
);

create index if not exists club_challenges_open_challenger_idx
  on public.club_challenges(challenger_club_id,created_at desc)
  where status in ('pending','active');
create index if not exists club_challenges_open_challenged_idx
  on public.club_challenges(challenged_club_id,created_at desc)
  where status in ('pending','active');
create unique index if not exists club_challenges_one_open_challenger_uq
  on public.club_challenges(challenger_club_id)
  where status in ('pending','active');
create unique index if not exists club_challenges_one_open_challenged_uq
  on public.club_challenges(challenged_club_id)
  where status in ('pending','active');

alter table public.club_challenges enable row level security;
revoke all on public.club_challenges from public,anon,authenticated;

create or replace function private.expire_club_challenges_v1()
returns void
language sql
security definer
set search_path=''
as $$
  update public.club_challenges
  set status = case when status = 'pending' then 'expired' else 'completed' end,
      responded_at = coalesce(responded_at, now())
  where (status = 'pending' and created_at + interval '24 hours' <= now())
     or (status = 'active' and ends_at <= now());
$$;

create or replace function private.create_club_challenge_v1(p_target_club_id uuid)
returns uuid
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_club uuid;
  v_challenge uuid;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.expire_club_challenges_v1();

  select cm.club_id into v_club
  from public.club_members cm
  where cm.user_id = v_uid and cm.role = 'owner';
  if v_club is null then raise exception 'club_owner_required'; end if;
  if p_target_club_id is null or p_target_club_id = v_club then raise exception 'invalid_challenge_target'; end if;
  if not exists(select 1 from public.clubs c where c.id = p_target_club_id) then raise exception 'club_not_found'; end if;
  -- Serialize both clubs, including opposite-direction requests, before checking
  -- for an open challenge. This prevents two owners opening parallel challenges.
  perform pg_advisory_xact_lock(hashtextextended(least(v_club::text,p_target_club_id::text),0));
  perform pg_advisory_xact_lock(hashtextextended(greatest(v_club::text,p_target_club_id::text),0));
  if exists (
    select 1 from public.club_challenges c
    where c.status in ('pending','active')
      and (v_club in (c.challenger_club_id,c.challenged_club_id)
        or p_target_club_id in (c.challenger_club_id,c.challenged_club_id))
  ) then raise exception 'club_challenge_already_open'; end if;

  insert into public.club_challenges(challenger_club_id,challenged_club_id,created_by)
  values(v_club,p_target_club_id,v_uid)
  returning id into v_challenge;
  return v_challenge;
exception when unique_violation then
  raise exception 'club_challenge_already_open';
end;
$$;

create or replace function private.respond_club_challenge_v1(p_challenge_id uuid, p_accept boolean)
returns text
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_challenge public.club_challenges%rowtype;
  v_club uuid;
  v_start timestamptz;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.expire_club_challenges_v1();
  select * into v_challenge from public.club_challenges where id = p_challenge_id for update;
  if not found then raise exception 'club_challenge_not_found'; end if;
  if v_challenge.status <> 'pending' then raise exception 'club_challenge_not_pending'; end if;
  select cm.club_id into v_club from public.club_members cm where cm.user_id=v_uid and cm.role='owner';
  if v_club is distinct from v_challenge.challenged_club_id then raise exception 'challenged_club_owner_required'; end if;

  if p_accept then
    v_start := now();
    update public.club_challenges
    set status='active', accepted_by=v_uid, starts_at=v_start, ends_at=v_start + interval '24 hours', responded_at=v_start
    where id=p_challenge_id;
    return 'active';
  end if;
  update public.club_challenges set status='declined', accepted_by=v_uid, responded_at=now() where id=p_challenge_id;
  return 'declined';
end;
$$;

create or replace function private.get_my_club_challenge_v1()
returns table(
  challenge_id uuid, challenger_club_id uuid, challenger_name text, challenger_tag text,
  challenged_club_id uuid, challenged_name text, challenged_tag text, status text,
  starts_at timestamptz, ends_at timestamptz, challenger_points bigint, challenged_points bigint,
  can_respond boolean, can_create boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_club uuid;
  v_owner boolean := false;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.expire_club_challenges_v1();
  select cm.club_id, cm.role='owner' into v_club,v_owner from public.club_members cm where cm.user_id=v_uid;
  if v_club is null then return; end if;
  return query
  select ch.id,ch.challenger_club_id,ca.name,ca.tag,ch.challenged_club_id,cb.name,cb.tag,ch.status,
         ch.starts_at,ch.ends_at,
         coalesce((select sum(e.points) from public.club_point_events e where e.club_id=ch.challenger_club_id and ch.status='active' and e.created_at>=ch.starts_at and e.created_at<ch.ends_at),0)::bigint,
         coalesce((select sum(e.points) from public.club_point_events e where e.club_id=ch.challenged_club_id and ch.status='active' and e.created_at>=ch.starts_at and e.created_at<ch.ends_at),0)::bigint,
         (v_owner and ch.challenged_club_id=v_club and ch.status='pending'),
         (v_owner and ch.status not in ('pending','active'))
  from public.club_challenges ch
  join public.clubs ca on ca.id=ch.challenger_club_id
  join public.clubs cb on cb.id=ch.challenged_club_id
  where v_club in (ch.challenger_club_id,ch.challenged_club_id)
  order by case when ch.status in ('pending','active') then 0 else 1 end, ch.created_at desc
  limit 1;
end;
$$;

create or replace function private.get_club_challenge_contributions_v1(p_challenge_id uuid)
returns table(user_id uuid, display_name text, club_id uuid, club_tag text, points bigint)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_challenge public.club_challenges%rowtype;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  perform private.expire_club_challenges_v1();
  select * into v_challenge from public.club_challenges where id=p_challenge_id;
  if not found then raise exception 'club_challenge_not_found'; end if;
  if not exists(select 1 from public.club_members cm where cm.user_id=v_uid and cm.club_id in (v_challenge.challenger_club_id,v_challenge.challenged_club_id)) then
    raise exception 'club_challenge_member_required';
  end if;
  return query
  select cm.user_id,p.display_name,cm.club_id,c.tag,
         coalesce(sum(e.points) filter(where v_challenge.status='active' and e.created_at>=v_challenge.starts_at and e.created_at<v_challenge.ends_at),0)::bigint
  from public.club_members cm
  join public.profiles p on p.id=cm.user_id
  join public.clubs c on c.id=cm.club_id
  left join public.club_point_events e on e.user_id=cm.user_id and e.club_id=cm.club_id
  where cm.club_id in (v_challenge.challenger_club_id,v_challenge.challenged_club_id)
  group by cm.user_id,p.display_name,cm.club_id,c.tag
  order by 5 desc,lower(p.display_name);
end;
$$;

create or replace function public.create_club_challenge_v1(p_target_club_id uuid)
returns uuid language sql security invoker set search_path='' as $$
  select private.create_club_challenge_v1(p_target_club_id);
$$;
create or replace function public.respond_club_challenge_v1(p_challenge_id uuid,p_accept boolean)
returns text language sql security invoker set search_path='' as $$
  select private.respond_club_challenge_v1(p_challenge_id,p_accept);
$$;
create or replace function public.get_my_club_challenge_v1()
returns table(challenge_id uuid,challenger_club_id uuid,challenger_name text,challenger_tag text,challenged_club_id uuid,challenged_name text,challenged_tag text,status text,starts_at timestamptz,ends_at timestamptz,challenger_points bigint,challenged_points bigint,can_respond boolean,can_create boolean)
language sql security invoker set search_path='' as $$ select * from private.get_my_club_challenge_v1(); $$;
create or replace function public.get_club_challenge_contributions_v1(p_challenge_id uuid)
returns table(user_id uuid,display_name text,club_id uuid,club_tag text,points bigint)
language sql security invoker set search_path='' as $$ select * from private.get_club_challenge_contributions_v1(p_challenge_id); $$;

revoke all on function private.expire_club_challenges_v1() from public,anon;
revoke all on function private.create_club_challenge_v1(uuid) from public,anon;
revoke all on function private.respond_club_challenge_v1(uuid,boolean) from public,anon;
revoke all on function private.get_my_club_challenge_v1() from public,anon;
revoke all on function private.get_club_challenge_contributions_v1(uuid) from public,anon;
grant execute on function private.create_club_challenge_v1(uuid) to authenticated;
grant execute on function private.respond_club_challenge_v1(uuid,boolean) to authenticated;
grant execute on function private.get_my_club_challenge_v1() to authenticated;
grant execute on function private.get_club_challenge_contributions_v1(uuid) to authenticated;

revoke all on function public.create_club_challenge_v1(uuid) from public,anon;
revoke all on function public.respond_club_challenge_v1(uuid,boolean) from public,anon;
revoke all on function public.get_my_club_challenge_v1() from public,anon;
revoke all on function public.get_club_challenge_contributions_v1(uuid) from public,anon;
grant execute on function public.create_club_challenge_v1(uuid) to authenticated;
grant execute on function public.respond_club_challenge_v1(uuid,boolean) to authenticated;
grant execute on function public.get_my_club_challenge_v1() to authenticated;
grant execute on function public.get_club_challenge_contributions_v1(uuid) to authenticated;
