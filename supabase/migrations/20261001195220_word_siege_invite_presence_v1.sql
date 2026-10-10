-- Accepting an invite must not mark the sender as inside a room before they enter it.
CREATE OR REPLACE FUNCTION public.respond_word_siege_invite_v1(p_invite_id uuid, p_accept boolean)
 RETURNS word_siege_games
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
declare
  v_uid uuid := auth.uid();
  inv public.word_siege_invites;
  r public.word_siege_games;
  v_bag text;
  v_one_rack text;
  v_two_rack text;
  v_sender_active integer;
  v_receiver_active integer;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;

  select * into inv
  from public.word_siege_invites
  where id = p_invite_id
  for update;

  if inv.id is null then raise exception 'word_siege_invite_not_found'; end if;
  if inv.receiver_id <> v_uid then raise exception 'word_siege_not_invite_receiver'; end if;
  if inv.status <> 'pending' then raise exception 'word_siege_invite_not_pending'; end if;

  if inv.expires_at < now() then
    update public.word_siege_invites
    set status = 'expired', responded_at = now()
    where id = inv.id;
    return null;
  end if;

  if not coalesce(p_accept, false) then
    update public.word_siege_invites
    set status = 'declined', responded_at = now()
    where id = inv.id;
    return null;
  end if;

  if not exists (
    select 1
    from public.friendships f
    where f.user_id = least(inv.sender_id, inv.receiver_id)
      and f.friend_id = greatest(inv.sender_id, inv.receiver_id)
      and f.status = 'accepted'
  ) then raise exception 'word_siege_not_friends'; end if;

  if exists (
    select 1
    from public.user_blocks b
    where (b.blocker_id = inv.sender_id and b.blocked_id = inv.receiver_id)
       or (b.blocker_id = inv.receiver_id and b.blocked_id = inv.sender_id)
  ) then raise exception 'word_siege_blocked_relationship'; end if;

  perform pg_advisory_xact_lock(
    hashtextextended('word_siege_friend:' || least(inv.sender_id, inv.receiver_id)::text || ':' || greatest(inv.sender_id, inv.receiver_id)::text, 0)
  );

  select count(*)::integer into v_sender_active
  from public.word_siege_games g
  where g.status in ('waiting','playing')
    and inv.sender_id in (g.player_one_id, g.player_two_id);

  select count(*)::integer into v_receiver_active
  from public.word_siege_games g
  where g.status in ('waiting','playing')
    and inv.receiver_id in (g.player_one_id, g.player_two_id);

  if v_sender_active >= public.word_siege_active_limit_v1(inv.sender_id) or v_receiver_active >= public.word_siege_active_limit_v1(inv.receiver_id) then
    raise exception 'word_siege_active_limit';
  end if;

  v_bag := private.word_siege_new_bag_v1(inv.language);
  v_one_rack := substring(v_bag from 1 for 7);
  v_two_rack := substring(v_bag from 8 for 7);
  v_bag := substring(v_bag from 15);

  insert into public.word_siege_games(
    player_one_id,
    player_two_id,
    status,
    language,
    current_player_id,
    board,
    bag,
    player_one_rack,
    player_two_rack,
    last_action,
    last_action_player_id
  ) values (
    inv.sender_id,
    inv.receiver_id,
    'playing',
    inv.language,
    inv.sender_id,
    private.word_siege_new_board_v1(),
    v_bag,
    v_one_rack,
    v_two_rack,
    'friend_game_started',
    inv.sender_id
  )
  returning * into r;

  update public.word_siege_invites
  set status = 'accepted', game_id = r.id, responded_at = now()
  where id = inv.id;

  update public.profiles
  set presence_status = 'in_game', last_seen_at = now(), updated_at = now()
  where id = v_uid;

  return r;
end
$function$
;
CREATE OR REPLACE FUNCTION public.respond_word_siege_series_invite_v1(p_invite_id uuid, p_accept boolean)
 RETURNS word_siege_games
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'public', 'private', 'pg_temp'
AS $function$
declare
  v_uid uuid:=auth.uid();
  inv public.word_siege_series_invites;
  r public.word_siege_games;
  v_bag text; v_one_rack text; v_two_rack text;
  v_sender_active integer; v_receiver_active integer;
  v_now timestamptz:=clock_timestamp();
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into inv from public.word_siege_series_invites where id=p_invite_id for update;
  if inv.id is null then raise exception 'word_siege_invite_not_found'; end if;
  if inv.receiver_id<>v_uid then raise exception 'word_siege_not_invite_receiver'; end if;
  if inv.status<>'pending' then raise exception 'word_siege_invite_not_pending'; end if;
  if inv.expires_at<now() then update public.word_siege_series_invites set status='expired',responded_at=now() where id=inv.id; return null; end if;
  if not coalesce(p_accept,false) then update public.word_siege_series_invites set status='declined',responded_at=now() where id=inv.id; return null; end if;
  if not public.has_series_game_access_v1(inv.sender_id) or not public.has_series_game_access_v1(inv.receiver_id) then raise exception 'series_game_required'; end if;
  if not exists(select 1 from public.friendships f where f.user_id=least(inv.sender_id,inv.receiver_id) and f.friend_id=greatest(inv.sender_id,inv.receiver_id) and f.status='accepted') then raise exception 'word_siege_not_friends'; end if;
  if exists(select 1 from public.user_blocks b where (b.blocker_id=inv.sender_id and b.blocked_id=inv.receiver_id) or (b.blocker_id=inv.receiver_id and b.blocked_id=inv.sender_id)) then raise exception 'word_siege_blocked_relationship'; end if;

  perform pg_advisory_xact_lock(hashtextextended('word_siege_series_friend:'||least(inv.sender_id,inv.receiver_id)::text||':'||greatest(inv.sender_id,inv.receiver_id)::text,0));
  select count(*)::integer into v_sender_active from public.word_siege_games g where g.status in('waiting','playing') and inv.sender_id in(g.player_one_id,g.player_two_id);
  select count(*)::integer into v_receiver_active from public.word_siege_games g where g.status in('waiting','playing') and inv.receiver_id in(g.player_one_id,g.player_two_id);
  if v_sender_active>=public.word_siege_active_limit_v1(inv.sender_id) or v_receiver_active>=public.word_siege_active_limit_v1(inv.receiver_id) then raise exception 'word_siege_active_limit'; end if;

  v_bag:=private.word_siege_new_bag_v1(inv.language);
  v_one_rack:=substring(v_bag from 1 for 7);
  v_two_rack:=substring(v_bag from 8 for 7);
  v_bag:=substring(v_bag from 15);
  insert into public.word_siege_games(
    player_one_id,player_two_id,status,language,current_player_id,game_mode,turn_duration_minutes,turn_duration_hours,
    board,bag,player_one_rack,player_two_rack,last_action,last_action_player_id,turn_started_at,turn_deadline
  ) values(
    inv.sender_id,inv.receiver_id,'playing',inv.language,inv.sender_id,'series',inv.turn_duration_minutes,12,
    private.word_siege_new_board_v1(),v_bag,v_one_rack,v_two_rack,'series_friend_game_started',inv.sender_id,
    v_now,v_now+make_interval(mins=>inv.turn_duration_minutes)
  ) returning * into r;
  update public.word_siege_series_invites set status='accepted',game_id=r.id,responded_at=now() where id=inv.id;
  update public.profiles set presence_status='in_game',last_seen_at=now(),updated_at=now() where id=v_uid;
  return r;
end
$function$
;
