-- Direct Style trial hardening.
-- Trial eligibility and expiry are server-authoritative; no client clock or local counter is trusted.

alter table public.style_trials add column if not exists match_count_at_start integer;

create table if not exists public.style_trial_daily_claims (
  user_id uuid not null references public.profiles(id) on delete cascade,
  item_id text not null references public.shop_items(id) on delete cascade,
  claim_date date not null,
  created_at timestamptz not null default now(),
  primary key(user_id,item_id,claim_date)
);
alter table public.style_trial_daily_claims enable row level security;
revoke all on public.style_trial_daily_claims from anon,authenticated;

create or replace function public.start_style_trial_v2(p_item_id text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_item public.shop_items%rowtype;
  v_date date := (timezone('utc',now()))::date;
  v_match_count integer;
  v_expires timestamptz;
  v_matches integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_item from public.shop_items
  where id=nullif(trim(p_item_id),'') and active=true and trial_mode in ('match','minutes') and coalesce(trial_value,0)>0;
  if not found then raise exception 'trial_item_unavailable'; end if;
  if exists(select 1 from public.user_inventory where user_id=v_uid and item_id=v_item.id) then
    raise exception 'already_owned';
  end if;

  insert into public.style_trial_daily_claims(user_id,item_id,claim_date)
  values(v_uid,v_item.id,v_date)
  on conflict do nothing;
  if not found then raise exception 'trial_daily_limit_reached'; end if;

  v_match_count := public.completed_store_match_count_v1(v_uid);
  if v_item.trial_mode='minutes' then
    v_expires := now()+make_interval(mins=>least(v_item.trial_value,30));
    v_matches := null;
  else
    v_expires := null;
    v_matches := least(v_item.trial_value,1);
  end if;

  update public.style_trials set ended_at=now()
  where user_id=v_uid and ended_at is null;
  insert into public.style_trials(user_id,item_id,mode,matches_remaining,expires_at,match_count_at_start)
  values(v_uid,v_item.id,v_item.trial_mode,v_matches,v_expires,v_match_count);

  return jsonb_build_object(
    'success',true,'trial_item_id',v_item.id,'trial_mode',v_item.trial_mode,
    'trial_matches_remaining',v_matches,'trial_expires_at',v_expires
  );
end
$$;
revoke all on function public.start_style_trial_v2(text) from public,anon;
grant execute on function public.start_style_trial_v2(text) to authenticated;

create or replace function public.equip_style_trial_v2()
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_trial public.style_trials%rowtype;
  v_item public.shop_items%rowtype;
  v_current_matches integer;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  select * into v_trial from public.style_trials
  where user_id=v_uid and ended_at is null
  order by started_at desc limit 1 for update;
  if not found then raise exception 'trial_not_active'; end if;

  if v_trial.mode='minutes' then
    if v_trial.expires_at is null or v_trial.expires_at<=now() then
      update public.style_trials set ended_at=now() where id=v_trial.id;
      raise exception 'trial_expired';
    end if;
  else
    v_current_matches := public.completed_store_match_count_v1(v_uid);
    if v_trial.match_count_at_start is null or v_current_matches-v_trial.match_count_at_start>=coalesce(v_trial.matches_remaining,1) then
      update public.style_trials set ended_at=now() where id=v_trial.id;
      raise exception 'trial_expired';
    end if;
  end if;

  select * into v_item from public.shop_items where id=v_trial.item_id and active=true;
  if not found then raise exception 'trial_item_unavailable'; end if;

  insert into public.user_equipped_cosmetics(user_id) values(v_uid) on conflict(user_id) do nothing;
  update public.user_equipped_cosmetics set
    profile_frame_id=case when v_item.kind='profile_frame' then v_item.id else profile_frame_id end,
    name_style_id=case when v_item.kind='name_style' then v_item.id else name_style_id end,
    game_theme_id=case when v_item.kind='game_theme' then v_item.id else game_theme_id end,
    keyboard_theme_id=case when v_item.kind='keyboard_theme' then v_item.id else keyboard_theme_id end,
    victory_effect_id=case when v_item.kind='victory_effect' then v_item.id else victory_effect_id end,
    avatar_background_id=case when v_item.kind='avatar_background' then v_item.id else avatar_background_id end,
    nameplate_id=case when v_item.kind='nameplate' then v_item.id else nameplate_id end,
    badge_id=case when v_item.kind='badge' then v_item.id else badge_id end,
    title_style_id=case when v_item.kind='title' then v_item.id else title_style_id end,
    vs_intro_id=case when v_item.kind='vs_intro' then v_item.id else vs_intro_id end,
    word_effect_id=case when v_item.kind='word_effect' then v_item.id else word_effect_id end,
    emote_id=case when v_item.kind='emote' then v_item.id else emote_id end,
    updated_at=now()
  where user_id=v_uid;

  return jsonb_build_object(
    'success',true,'item_id',v_item.id,'kind',v_item.kind,
    'expires_at',v_trial.expires_at,'matches_remaining',v_trial.matches_remaining
  );
end
$$;
revoke all on function public.equip_style_trial_v1() from authenticated;
revoke all on function public.equip_style_trial_v2() from public,anon;
grant execute on function public.equip_style_trial_v2() to authenticated;
