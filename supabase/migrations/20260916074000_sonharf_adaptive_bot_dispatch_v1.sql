-- Son Harf bot dispatch: keep new players on a beatable normal bot and graduate only proven players.
-- The existing bot turn functions remain server-authoritative and retain their row/advisory locking.

create or replace function public.sonharf_adaptive_bot_tier_v1(
    p_rating integer,
    p_total_matches integer,
    p_wins integer,
    p_requested text default 'normal'
)
returns text
language sql
immutable
set search_path = pg_catalog, public, pg_temp
as $$
    select case
        when greatest(coalesce(p_total_matches,0),0) < 6 then 'normal'
        when lower(coalesce(p_requested,'normal')) = 'expert'
             and coalesce(p_rating,1000) >= 1050 then 'expert'
        when greatest(coalesce(p_total_matches,0),0) >= 20
             and coalesce(p_rating,1000) >= 1200
             and (coalesce(p_wins,0)::numeric / greatest(coalesce(p_total_matches,0),1)) >= 0.58 then 'expert'
        else 'normal'
    end;
$$;

revoke all on function public.sonharf_adaptive_bot_tier_v1(integer,integer,integer,text) from public;
grant execute on function public.sonharf_adaptive_bot_tier_v1(integer,integer,integer,text) to authenticated, service_role;

create or replace function public.bot_take_turn(p_room_id uuid)
returns public.game_rooms
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    r public.game_rooms;
    v_rating integer := 1000;
    v_total integer := 0;
    v_wins integer := 0;
    v_requested text := 'normal';
    v_tier text := 'normal';
begin
    select * into r from public.game_rooms where id=p_room_id;
    if r.id is null or not r.is_bot then raise exception 'not_bot_room'; end if;
    if auth.uid()<>r.host_id then raise exception 'not_participant'; end if;

    if r.game_mode='expert' then
        return public.bot_take_turn_expert_v1(p_room_id);
    end if;

    select coalesce(rating,1000), coalesce(total_matches,0), coalesce(wins,0), coalesce(bot_difficulty,'normal')
      into v_rating, v_total, v_wins, v_requested
      from public.profiles where id=r.host_id;

    v_tier := public.sonharf_adaptive_bot_tier_v1(v_rating,v_total,v_wins,v_requested);
    if v_tier='expert' then
        return public.bot_take_turn_expert_v1(p_room_id);
    end if;
    return public.bot_take_turn_normal_v1(p_room_id);
end
$$;

revoke all on function public.bot_take_turn(uuid) from public;
grant execute on function public.bot_take_turn(uuid) to authenticated, service_role;
