-- Ensure an invite cannot be accepted after either participant blocks the other.
-- This closes the classic Son Harf path where friend invites with room_id IS NULL
-- previously re-checked friendship state but not the current block relationship.

create or replace function public.respond_game_invite(p_invite_id uuid, p_accept boolean)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public', 'extensions', 'pg_temp'
as $function$
declare
  inv public.game_invites;
  r public.game_rooms;
  generated_code text;
  attempts int := 0;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;

  select * into inv from public.game_invites where id=p_invite_id for update;
  if inv.id is null then raise exception 'invite_not_found'; end if;
  if inv.receiver_id <> auth.uid() then raise exception 'not_invite_receiver'; end if;
  if inv.status <> 'pending' then raise exception 'invite_not_pending'; end if;
  if inv.expires_at < now() then
    update public.game_invites set status='expired',responded_at=now() where id=inv.id;
    raise exception 'invite_expired';
  end if;

  if not p_accept then
    update public.game_invites set status='declined',responded_at=now() where id=inv.id;
    return null;
  end if;

  if exists(
    select 1 from public.user_blocks b
    where (b.blocker_id=inv.sender_id and b.blocked_id=inv.receiver_id)
       or (b.blocker_id=inv.receiver_id and b.blocked_id=inv.sender_id)
  ) then raise exception 'blocked_relationship'; end if;

  if exists(
    select 1 from public.game_rooms
    where status in ('playing','quiz','final','sudden_death')
      and (host_id=auth.uid() or guest_id=auth.uid())
  ) then raise exception 'player_already_in_game'; end if;

  if inv.room_id is not null then
    select * into r from public.game_rooms where id=inv.room_id for update;
    if r.id is null or r.room_type<>'private' or r.host_id<>inv.sender_id
       or r.status<>'waiting' or r.guest_id is not null then
      raise exception 'room_not_available';
    end if;
    if exists(
      select 1 from public.user_blocks b
      where (b.blocker_id=r.host_id and b.blocked_id=auth.uid())
         or (b.blocker_id=auth.uid() and b.blocked_id=r.host_id)
    ) then raise exception 'blocked_relationship'; end if;

    update public.game_rooms
    set guest_id=auth.uid(), status='playing', current_player_id=host_id,
        turn_deadline=now()+interval '15 seconds', guest_last_seen_at=now(),
        last_event='private_room_joined', last_event_player_id=auth.uid()
    where id=r.id
    returning * into r;
  else
    if exists(
      select 1 from public.game_rooms
      where status in ('playing','quiz','final','sudden_death')
        and (host_id in (inv.sender_id,inv.receiver_id) or guest_id in (inv.sender_id,inv.receiver_id))
    ) then raise exception 'player_already_in_game'; end if;

    loop
      attempts:=attempts+1;
      generated_code:=upper(substr(encode(gen_random_bytes(8),'hex'),1,6));
      begin
        insert into public.game_rooms(code,host_id,guest_id,status,current_player_id,turn_deadline,language,room_type)
        values(generated_code,inv.sender_id,inv.receiver_id,'playing',inv.sender_id,now()+interval '15 seconds',inv.language,'friend')
        returning * into r;
        exit;
      exception when unique_violation then
        if attempts>=8 then raise; end if;
      end;
    end loop;
  end if;

  update public.game_invites
  set status='accepted',room_id=r.id,responded_at=now()
  where id=inv.id;

  update public.game_invites
  set status='expired',responded_at=now()
  where id<>inv.id and room_id=r.id and status='pending';

  update public.profiles
  set presence_status='in_game',last_seen_at=now()
  where id in (r.host_id,r.guest_id);

  return r;
end;
$function$;
