-- Team Arena same-team Rematch v1
-- Host-only rematch from a finished 4-player Team Arena match.
-- Creates one new lobby, preserves prior team assignment, and re-invites the other 3 players.
-- Social-only Team Arena rules remain unchanged: no rating/league/Son Coin/stat rewards.

alter table public.team_arena_rooms
  add column if not exists rematch_of uuid
  references public.team_arena_rooms(id) on delete set null;

create unique index if not exists team_arena_rooms_rematch_of_active_uidx
  on public.team_arena_rooms(rematch_of)
  where rematch_of is not null and status<>'cancelled';

create or replace function private.create_team_arena_rematch_v1(
  p_room_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  old_room public.team_arena_rooms;
  existing_room public.team_arena_rooms;
  created jsonb;
  new_room_id uuid;
  m record;
  v_invited int:=0;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  perform pg_advisory_xact_lock(88220032);
  perform private.cleanup_team_arena_v1();

  select tr.* into old_room
  from public.team_arena_rooms tr
  where tr.id=p_room_id
  for update;

  if old_room.id is null then raise exception 'team_arena_room_not_found'; end if;
  if old_room.host_id<>v_uid then raise exception 'team_arena_host_required'; end if;
  if old_room.status<>'finished' then raise exception 'team_arena_rematch_requires_finished'; end if;

  if (
    select count(*)
    from public.team_arena_members tm
    where tm.room_id=old_room.id
  )<>4 then
    raise exception 'team_arena_rematch_requires_four_players';
  end if;

  select tr.* into existing_room
  from public.team_arena_rooms tr
  where tr.rematch_of=old_room.id
    and tr.status<>'cancelled'
  order by tr.created_at desc
  limit 1;

  if existing_room.id is not null then
    if existing_room.status='lobby'
       and existing_room.expires_at>clock_timestamp()
    then
      return jsonb_build_object(
        'status','lobby',
        'room_id',existing_room.id,
        'invited_count',(
          select count(*)::int
          from public.team_arena_invites i
          where i.room_id=existing_room.id
            and i.status in ('pending','accepted')
        ),
        'reused',true
      );
    end if;

    raise exception 'team_arena_rematch_already_created';
  end if;

  created:=private.create_team_arena_v1(old_room.language);
  new_room_id:=(created->>'room_id')::uuid;

  update public.team_arena_rooms
  set rematch_of=old_room.id
  where id=new_room_id;

  for m in
    select tm.user_id,tm.team,tm.seat
    from public.team_arena_members tm
    where tm.room_id=old_room.id
      and tm.user_id<>v_uid
    order by tm.team,tm.seat
  loop
    perform private.invite_friend_to_team_arena_v1(
      new_room_id,
      m.user_id,
      m.team
    );
    v_invited:=v_invited+1;
  end loop;

  if v_invited<>3 then
    raise exception 'team_arena_rematch_invite_count';
  end if;

  return jsonb_build_object(
    'status','lobby',
    'room_id',new_room_id,
    'invited_count',v_invited,
    'reused',false
  );
end
$$;

create or replace function public.create_team_arena_rematch_v1(
  p_room_id uuid
)
returns jsonb
language sql
security invoker
set search_path=''
as $$
  select private.create_team_arena_rematch_v1(p_room_id);
$$;

revoke all on function private.create_team_arena_rematch_v1(uuid)
  from public,anon,authenticated;
grant execute on function private.create_team_arena_rematch_v1(uuid)
  to authenticated;

revoke all on function public.create_team_arena_rematch_v1(uuid)
  from public,anon;
grant execute on function public.create_team_arena_rematch_v1(uuid)
  to authenticated;
