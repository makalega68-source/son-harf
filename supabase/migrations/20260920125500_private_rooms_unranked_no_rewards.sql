-- PRO private rooms are a social convenience feature. They must never create a ranked,
-- progression, or currency advantage over free players.

create or replace function public.create_room_normal_v1(p_language text default 'tr'::text)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public', 'extensions', 'pg_temp'
as $function$
declare
  r public.game_rooms;
  generated_code text;
  i int;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  p_language := lower(trim(p_language));
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;
  if not exists (select 1 from public.profiles where id=auth.uid() and is_vip=true) then raise exception 'vip_required'; end if;
  if exists (
    select 1 from public.game_rooms
    where status in ('playing','quiz','final','sudden_death')
      and (host_id=auth.uid() or guest_id=auth.uid())
  ) then raise exception 'player_already_in_game'; end if;

  for i in 1..8 loop
    generated_code := upper(substr(encode(gen_random_bytes(8),'hex'),1,6));
    begin
      insert into public.game_rooms(
        code,host_id,status,current_player_id,turn_deadline,language,
        room_type,room_mode,host_last_seen_at
      ) values(
        generated_code,auth.uid(),'waiting',auth.uid(),null,p_language,
        'private','private',now()
      )
      returning * into r;
      return r;
    exception when unique_violation then
      null;
    end;
  end loop;
  raise exception 'room_code_generation_failed';
end;
$function$;

create or replace function public.sonharf_apply_finish(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  r public.game_rooms;
  v_meaningful boolean;
begin
  select * into r
  from public.game_rooms
  where id=p_room_id
  for update;

  if r.id is null then raise exception 'room_not_found'; end if;
  if r.status<>'finished' or r.stats_applied then return r; end if;

  -- Private rooms are social-only: no ranked W/L, rating or match-count progression.
  if r.room_mode='private' or r.room_type='private' then
    update public.profiles
    set presence_status='online',last_seen_at=now()
    where id in (r.host_id,r.guest_id);

    update public.game_rooms
    set stats_applied=true,streak_shielded_user_id=null
    where id=r.id
    returning * into r;

    delete from public.profile_photo_access
    where owner_id in (r.host_id,r.guest_id) or viewer_id in (r.host_id,r.guest_id);
    return r;
  end if;

  v_meaningful := (r.winner_id is not null or coalesce(r.valid_word_count,0)>0);

  if coalesce(r.is_bot,false) then
    update public.profiles
    set presence_status='online',last_seen_at=now()
    where id=r.host_id;
  else
    if r.guest_id is null then raise exception 'competitive_match_requires_two_humans'; end if;
    update public.profiles
    set total_matches=total_matches+case when v_meaningful then 1 else 0 end,
        wins=wins+case when r.winner_id is not null and id=r.winner_id then 1 else 0 end,
        losses=losses+case when r.winner_id is not null and id<>r.winner_id then 1 else 0 end,
        rating=greatest(100,rating+case when r.winner_id is null then 0 when id=r.winner_id then 20 else -15 end),
        presence_status='online',
        last_seen_at=now()
    where id in (r.host_id,r.guest_id);
  end if;

  update public.game_rooms set stats_applied=true,streak_shielded_user_id=null where id=r.id returning * into r;

  delete from public.profile_photo_access
  where owner_id in (r.host_id,r.guest_id) or viewer_id in (r.host_id,r.guest_id);

  return r;
end
$function$;

create or replace function public.claim_match_result_v10(p_room_id uuid)
returns table(
  won boolean,
  xp_gain integer,
  diamonds_awarded integer,
  league_points integer,
  current_rating integer,
  current_streak integer
)
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  r public.game_rooms;
  uid uuid:=auth.uid();
  v_won boolean;
  v_words int;
  v_rounds int;
  v_diamonds int;
  v_rating int;
  v_streak int:=0;
  v_inserted boolean:=false;
begin
  if uid is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id=p_room_id and status='finished' and (host_id=uid or guest_id=uid);
  if r.id is null then raise exception 'finished_room_not_found'; end if;

  v_won:=r.winner_id=uid;
  select rating into v_rating from public.profiles where id=uid;
  select coalesce(g.current_win_streak,0) into v_streak from public.get_growth_dashboard_v1() g limit 1;

  -- A PRO-only room must not be a source of XP, Son Coin or league/rating advantage.
  if r.room_mode='private' or r.room_type='private' then
    return query select v_won,0,0,0,v_rating::int,coalesce(v_streak,0)::int;
    return;
  end if;

  select count(*)::int into v_words from public.game_words where room_id=r.id and player_id=uid;
  v_rounds:=case when uid=r.host_id then r.host_rounds else r.guest_rounds end;
  v_diamonds:=case when v_won then 8 else 3 end;
  insert into public.match_reward_claims(room_id,user_id,diamonds) values(r.id,uid,v_diamonds) on conflict do nothing;
  if found then
    v_inserted:=true;
    update public.profiles set diamonds=diamonds+v_diamonds,updated_at=now() where id=uid;
    insert into public.diamond_ledger(user_id,delta,reason,item_id) values(uid,v_diamonds,'match_result:'||r.id::text,null);
  end if;
  return query select v_won,
    ((case when v_won then 120 else 35 end)+v_words*3+v_rounds*5)::int,
    (case when v_inserted then v_diamonds else 0 end)::int,
    (case when coalesce(r.is_bot,false) then 0 when v_won then 20 else -15 end)::int,
    v_rating::int,
    coalesce(v_streak,0)::int;
end
$function$;

-- Normalize existing private rooms so pause/resume and all future result processing agree.
update public.game_rooms
set room_mode='private'
where room_type='private' and room_mode is distinct from 'private';
