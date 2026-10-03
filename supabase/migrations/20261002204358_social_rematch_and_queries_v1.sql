-- Direct rematches require a finished match with this exact opponent.
alter table public.word_siege_invites add column if not exists rematch_of uuid references public.word_siege_games(id) on delete set null;
alter table public.word_siege_series_invites add column if not exists rematch_of uuid references public.word_siege_games(id) on delete set null;
create index if not exists word_siege_invites_rematch_of_idx on public.word_siege_invites(rematch_of) where rematch_of is not null;
create index if not exists word_siege_series_invites_rematch_of_idx on public.word_siege_series_invites(rematch_of) where rematch_of is not null;

create or replace function public.request_word_siege_rematch_v1(p_game_id uuid)
returns jsonb language plpgsql security definer set search_path=pg_catalog,public,private,pg_temp as $$
declare
  me uuid:=auth.uid(); g public.word_siege_games; rival uuid; result jsonb;
begin
  if me is null then raise exception 'word_siege_unauthorized'; end if;
  select * into g from public.word_siege_games where id=p_game_id;
  if g.id is null or g.status <> 'finished' or not(me=g.player_one_id or me=coalesce(g.player_two_id,g.player_one_id)) then
    raise exception 'word_siege_rematch_unavailable';
  end if;
  rival:=case when me=g.player_one_id then g.player_two_id else g.player_one_id end;
  if rival is null or rival=me then raise exception 'word_siege_rematch_unavailable'; end if;
  if exists(select 1 from public.user_blocks b where (b.blocker_id=me and b.blocked_id=rival) or(b.blocker_id=rival and b.blocked_id=me)) then
    raise exception 'word_siege_blocked_relationship';
  end if;
  perform pg_advisory_xact_lock(hashtextextended('word_siege_rematch:'||least(me,rival)::text||':'||greatest(me,rival)::text,0));
  if g.game_mode='series' then
    if not public.has_series_game_access_v1(me) or not public.has_series_game_access_v1(rival) then raise exception 'series_game_required'; end if;
    if exists(select 1 from public.word_siege_series_invites where status='pending' and expires_at>=now() and
      ((sender_id=me and receiver_id=rival) or(sender_id=rival and receiver_id=me))) then raise exception 'word_siege_invite_pending'; end if;
    insert into public.word_siege_series_invites(sender_id,receiver_id,language,turn_duration_minutes,rematch_of)
      values(me,rival,g.language,coalesce(g.turn_duration_minutes,5),g.id) returning to_jsonb(word_siege_series_invites.*) into result;
  else
    if exists(select 1 from public.word_siege_invites where status='pending' and expires_at>=now() and
      ((sender_id=me and receiver_id=rival) or(sender_id=rival and receiver_id=me))) then raise exception 'word_siege_invite_pending'; end if;
    insert into public.word_siege_invites(sender_id,receiver_id,language,rematch_of)
      values(me,rival,g.language,g.id) returning to_jsonb(word_siege_invites.*) into result;
  end if;
  return result;
end $$;
revoke all on function public.request_word_siege_rematch_v1(uuid) from public,anon;
grant execute on function public.request_word_siege_rematch_v1(uuid) to authenticated;

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
  ) and not exists(select 1 from public.word_siege_games g where g.id=inv.rematch_of and g.status='finished'
      and ((g.player_one_id=inv.sender_id and g.player_two_id=inv.receiver_id) or(g.player_two_id=inv.sender_id and g.player_one_id=inv.receiver_id))) then raise exception 'word_siege_not_friends'; end if;

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
  if not exists(select 1 from public.friendships f where f.user_id=least(inv.sender_id,inv.receiver_id) and f.friend_id=greatest(inv.sender_id,inv.receiver_id) and f.status='accepted') and not exists(select 1 from public.word_siege_games g where g.id=inv.rematch_of and g.status='finished'
      and ((g.player_one_id=inv.sender_id and g.player_two_id=inv.receiver_id) or(g.player_two_id=inv.sender_id and g.player_one_id=inv.receiver_id))) then raise exception 'word_siege_not_friends'; end if;
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

create or replace function public.get_social_inbox_v1() returns setof public.social_activity
language sql stable security invoker set search_path=pg_catalog,public,pg_temp as $$
  select * from public.social_activity where user_id=auth.uid() order by created_at desc,id desc limit 100;
$$;
create or replace function public.get_my_last_letter_rooms_v1() returns setof public.game_rooms
language sql stable security invoker set search_path=pg_catalog,public,pg_temp as $$
  select * from public.game_rooms where auth.uid() in(host_id,guest_id)
    and status in('waiting','playing','quiz','final','sudden_death','paused') order by created_at desc limit 50;
$$;
revoke all on function public.get_social_inbox_v1(),public.get_my_last_letter_rooms_v1() from public,anon;
grant execute on function public.get_social_inbox_v1(),public.get_my_last_letter_rooms_v1() to authenticated;
-- Include quiz/final turns and distinguish direct rematches in the persistent inbox.
do $$
declare definition text;
begin
  select pg_get_functiondef('private.social_activity_trigger_v1()'::regprocedure) into definition;
  definition:=replace(definition,'new.status in (''playing'',''sudden_death'')','new.status in (''playing'',''quiz'',''final'',''sudden_death'')');
  execute definition;
end $$;
