-- Restore the established Son Harf fair-play invariant after the 2026-09-08 booster regression.
-- Paid membership must not grant match power, required-letter overrides, score multipliers,
-- rating protection, or competitive jokers in Premier duels.
-- Keep the existing schema/functions for backward compatibility, but make competitive effects inert
-- and enforce the rule server-side so older or modified clients cannot bypass the UI.

alter table public.vip_joker_wallet alter column freezer_count set default 0;
alter table public.vip_joker_wallet alter column swap_count set default 0;
alter table public.vip_joker_wallet alter column hint_count set default 0;
alter table public.vip_joker_wallet alter column streak_shield_count set default 0;
alter table public.vip_joker_wallet alter column multiplier_count set default 0;

update public.vip_joker_wallet
set freezer_count = 0,
    swap_count = 0,
    hint_count = 0,
    streak_shield_count = 0,
    multiplier_count = 0,
    updated_at = now();

-- Remove any already-armed live advantage without dropping the compatibility table.
delete from public.premier_booster_state;

-- Existing submit functions may still call these helpers. They are deliberately neutralized so the
-- canonical last-letter rule and exact server scoring stay authoritative for every client version.
create or replace function public.premier_effective_required_v1(
  p_room_id uuid,
  p_user_id uuid,
  p_default text
)
returns text
language sql
stable
security definer
set search_path='public','pg_temp'
as $$
  select p_default
$$;
revoke all on function public.premier_effective_required_v1(uuid,uuid,text) from public, anon, authenticated;
grant execute on function public.premier_effective_required_v1(uuid,uuid,text) to service_role;

create or replace function public.premier_multiplier_armed_v1(
  p_room_id uuid,
  p_user_id uuid
)
returns boolean
language sql
stable
security definer
set search_path='public','pg_temp'
as $$
  select false
$$;
revoke all on function public.premier_multiplier_armed_v1(uuid,uuid) from public, anon, authenticated;
grant execute on function public.premier_multiplier_armed_v1(uuid,uuid) to service_role;

-- Old app versions can still query status, but they only see zero competitive inventory.
create or replace function public.get_premier_booster_status_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_room public.game_rooms;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_room from public.game_rooms where id = p_room_id;
  if v_room.id is null or v_uid not in (v_room.host_id, v_room.guest_id) then
    raise exception 'not_participant';
  end if;

  return jsonb_build_object(
    'hint_count', 0,
    'swap_count', 0,
    'multiplier_count', 0,
    'required_override', null,
    'multiplier_armed', false,
    'my_turn', (v_room.current_player_id = v_uid and not v_room.bot_turn and v_room.status in ('playing','final','sudden_death')),
    'turn_deadline', v_room.turn_deadline,
    'competitive_boosters_enabled', false,
    'reason', 'fair_play'
  );
end $$;
revoke all on function public.get_premier_booster_status_v1(uuid) from public, anon;
grant execute on function public.get_premier_booster_status_v1(uuid) to authenticated, service_role;

create or replace function public.use_premier_hint_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  raise exception 'competitive_booster_disabled';
end $$;
revoke all on function public.use_premier_hint_v1(uuid) from public, anon;
grant execute on function public.use_premier_hint_v1(uuid) to authenticated, service_role;

create or replace function public.use_premier_swap_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  raise exception 'competitive_booster_disabled';
end $$;
revoke all on function public.use_premier_swap_v1(uuid) from public, anon;
grant execute on function public.use_premier_swap_v1(uuid) to authenticated, service_role;

create or replace function public.use_premier_multiplier_v1(p_room_id uuid)
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  raise exception 'competitive_booster_disabled';
end $$;
revoke all on function public.use_premier_multiplier_v1(uuid) from public, anon;
grant execute on function public.use_premier_multiplier_v1(uuid) to authenticated, service_role;

-- Daily PRO claims remain API-compatible but never mint competitive inventory.
create or replace function public.claim_vip_daily_jokers_v7()
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
begin
  if auth.uid() is null then raise exception 'unauthorized'; end if;
  return jsonb_build_object(
    'success', true,
    'already_claimed', true,
    'disabled', true,
    'reason', 'fair_play',
    'freezer_count', 0,
    'swap_count', 0,
    'hint_count', 0,
    'multiplier_count', 0,
    'streak_shield_count', 0
  );
end $$;
revoke all on function public.claim_vip_daily_jokers_v7() from public, anon;
grant execute on function public.claim_vip_daily_jokers_v7() to authenticated, service_role;

-- Preserve legitimate PRO value while explicitly denying ranked live assistance.
create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path='public','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select coalesce(is_vip, false) into v_vip from public.profiles where id = v_uid;

  return jsonb_build_object(
    'is_vip', coalesce(v_vip, false),
    'daily_jokers_claimed', true,
    'freezer_count', 0,
    'swap_count', 0,
    'hint_count', 0,
    'multiplier_count', 0,
    'streak_shield_count', 0,
    'xp_multiplier', 1,
    'diamond_multiplier', 1,
    'rewarded_ad_bypass', coalesce(v_vip, false),
    'used_words_access', true,
    'direct_messages_access', true,
    'ranked_live_assist', false,
    'post_match_analysis', coalesce(v_vip, false),
    'saved_friend_list', coalesce(v_vip, false),
    'private_rooms', coalesce(v_vip, false)
  );
end $$;
revoke all on function public.get_vip_entitlements_v7() from public, anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated, service_role;

comment on function public.use_premier_multiplier_v1(uuid) is
  'Compatibility RPC. Competitive Premier boosters are disabled by fair-play policy.';
