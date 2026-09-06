-- VIP social relationship marks are server-authoritative.
-- Free players keep normal friendships, recent rivals and standard rematch flows.
-- VIP-only convenience marks (favorite / arch rival pin) cannot be forged by direct table writes.

revoke insert,update,delete on public.player_relationship_marks from authenticated;
grant select on public.player_relationship_marks to authenticated;

create or replace function public.set_vip_relationship_mark_v1(
  p_other_user_id uuid,
  p_favorite boolean default false,
  p_arch_rival boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
  v_are_friends boolean := false;
  v_has_match boolean := false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if p_other_user_id is null or p_other_user_id=v_uid then raise exception 'invalid_target'; end if;

  select coalesce(is_vip,false) into v_vip
  from public.profiles where id=v_uid;
  if not v_vip then raise exception 'vip_required'; end if;

  select public.are_friends(v_uid,p_other_user_id) into v_are_friends;
  if p_favorite and not coalesce(v_are_friends,false) then raise exception 'favorite_requires_friend'; end if;

  select exists(
    select 1 from public.game_rooms g
    where g.status='finished'
      and coalesce(g.is_bot,false)=false
      and ((g.host_id=v_uid and g.guest_id=p_other_user_id) or (g.host_id=p_other_user_id and g.guest_id=v_uid))
    union all
    select 1 from public.word_arena_rooms a
    where a.status='finished' and a.result_applied
      and ((a.host_id=v_uid and a.guest_id=p_other_user_id) or (a.host_id=p_other_user_id and a.guest_id=v_uid))
    union all
    select 1 from public.word_siege_games s
    where s.status='finished'
      and ((s.player_one_id=v_uid and s.player_two_id=p_other_user_id) or (s.player_one_id=p_other_user_id and s.player_two_id=v_uid))
    limit 1
  ) into v_has_match;

  if p_arch_rival and not v_has_match then raise exception 'arch_rival_requires_match'; end if;

  insert into public.player_relationship_marks(user_id,other_user_id,favorite,arch_rival,blocked,updated_at)
  values(v_uid,p_other_user_id,p_favorite,p_arch_rival,false,now())
  on conflict(user_id,other_user_id) do update set
    favorite=excluded.favorite,
    arch_rival=excluded.arch_rival,
    updated_at=now();

  return jsonb_build_object(
    'success',true,
    'other_user_id',p_other_user_id,
    'favorite',p_favorite,
    'arch_rival',p_arch_rival
  );
end
$$;

revoke all on function public.set_vip_relationship_mark_v1(uuid,boolean,boolean) from public,anon;
grant execute on function public.set_vip_relationship_mark_v1(uuid,boolean,boolean) to authenticated;

create or replace function public.get_vip_saved_social_v1()
returns table(
  other_user_id uuid,
  display_name text,
  favorite boolean,
  arch_rival boolean,
  presence_status text,
  last_seen_at timestamptz,
  is_friend boolean,
  can_invite boolean
)
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip,false) into v_vip from public.profiles where id=v_uid;
  if not v_vip then raise exception 'vip_required'; end if;

  return query
  select
    m.other_user_id,
    p.display_name,
    m.favorite,
    m.arch_rival,
    case when coalesce(pref.show_online_status,false) then coalesce(p.presence_status,'offline') else 'hidden' end,
    case when coalesce(pref.show_last_seen,false) then p.last_seen_at else null end,
    public.are_friends(v_uid,m.other_user_id),
    (
      public.are_friends(v_uid,m.other_user_id)
      and not exists(
        select 1 from public.user_blocks b
        where (b.blocker_id=v_uid and b.blocked_id=m.other_user_id)
           or (b.blocker_id=m.other_user_id and b.blocked_id=v_uid)
      )
    )
  from public.player_relationship_marks m
  join public.profiles p on p.id=m.other_user_id
  left join public.player_social_preferences pref on pref.user_id=m.other_user_id
  where m.user_id=v_uid and (m.favorite or m.arch_rival)
  order by m.favorite desc,m.arch_rival desc,p.display_name;
end
$$;

revoke all on function public.get_vip_saved_social_v1() from public,anon;
grant execute on function public.get_vip_saved_social_v1() to authenticated;

-- Relationship marks never replace the canonical block/report systems.
update public.player_relationship_marks set blocked=false where blocked=true;
