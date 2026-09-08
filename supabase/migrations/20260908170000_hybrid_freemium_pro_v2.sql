-- Son Harf hybrid Freemium / F2P + ads + Premium/PRO foundation.
-- Additive/backward-compatible: preserve existing shop, inventory, subscription,
-- entitlement, dictionary and server-authoritative gameplay structures.

-- 1) Persist the player's preferred product language without opening profile
-- server-authoritative fields to direct client writes.
alter table public.profiles
  add column if not exists preferred_language text not null default 'tr';

alter table public.profiles
  drop constraint if exists profiles_preferred_language_check;

alter table public.profiles
  add constraint profiles_preferred_language_check
  check (preferred_language in ('tr','en'));

create or replace function public.set_preferred_language_v1(p_language text)
returns jsonb
language plpgsql
security definer
set search_path = 'public','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_language text := lower(coalesce(p_language,''));
begin
  if v_uid is null then raise exception 'unauthorized'; end if;
  if v_language not in ('tr','en') then raise exception 'invalid_language'; end if;

  update public.profiles
  set preferred_language = v_language,
      updated_at = now()
  where id = v_uid;

  if not found then raise exception 'profile_not_found'; end if;

  return jsonb_build_object('success',true,'preferred_language',v_language);
end
$$;

revoke all on function public.set_preferred_language_v1(text) from public, anon;
grant execute on function public.set_preferred_language_v1(text) to authenticated, service_role;

-- 2) Extend the existing shop catalog instead of introducing a parallel catalog.
alter table public.shop_items
  add column if not exists available_from timestamptz not null default now(),
  add column if not exists available_until timestamptz,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

alter table public.shop_items
  drop constraint if exists shop_items_availability_window_check;

alter table public.shop_items
  add constraint shop_items_availability_window_check
  check (available_until is null or available_until > available_from);

create index if not exists idx_shop_items_sale_window
  on public.shop_items(active, available_from, available_until);

-- Active catalog is saleable only inside its server-controlled window.
-- The existing shop_items_owned_read policy still exposes retired/expired item
-- metadata to legitimate owners so purchased Style remains manageable.
drop policy if exists shop_items_read on public.shop_items;
create policy shop_items_read on public.shop_items
for select to authenticated
using (
  active = true
  and available_from <= now()
  and (available_until is null or available_until > now())
);

-- 3) Purchased inventory must not disappear when a catalog row is retired.
-- ON DELETE RESTRICT forces retirement through active/window state rather than
-- destructive catalog deletion.
alter table public.user_inventory
  drop constraint if exists user_inventory_item_id_fkey;

alter table public.user_inventory
  add constraint user_inventory_item_id_fkey
  foreign key (item_id) references public.shop_items(id) on delete restrict;

-- 4) Preserve the exact currently installed purchase implementation while
-- adding server-side sale-window enforcement. Fail closed if the reviewed
-- function shape has changed instead of silently overwriting a newer function.
do $migration$
declare
  original text := pg_get_functiondef('public.purchase_shop_item(text)'::regprocedure);
  updated text;
  old_clause text := 'where id=p_item_id and active=true;';
  new_clause text := 'where id=p_item_id and active=true and available_from <= now() and (available_until is null or available_until > now());';
begin
  if position(new_clause in original) > 0 then
    return;
  end if;

  if position(old_clause in original) = 0 then
    raise exception 'unreviewed_purchase_shop_item_definition';
  end if;

  updated := replace(original, old_clause, new_clause);
  execute updated;
end
$migration$;

revoke all on function public.purchase_shop_item(text) from public, anon;
grant execute on function public.purchase_shop_item(text) to authenticated, service_role;

-- 5) Re-enable only the first reviewed PRO gameplay helpers that had been
-- disabled solely by the retired absolute no-pay-to-win policy.
-- Initial grant: +1 Hint and +1 Letter Swap per UTC day.
-- Timer freeze and streak/rating shield remain ungranted in this rollout.
create or replace function public.claim_vip_daily_jokers_v7()
returns jsonb
language plpgsql
security definer
set search_path = 'public','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
  v_new_claim boolean := false;
  v_freezer integer := 0;
  v_swap integer := 0;
  v_hint integer := 0;
  v_shield integer := 0;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select coalesce(is_vip,false)
  into v_vip
  from public.profiles
  where id = v_uid;

  if not coalesce(v_vip,false) then raise exception 'not_vip'; end if;

  insert into public.vip_daily_joker_claims(user_id, claim_date)
  values(v_uid, current_date)
  on conflict (user_id, claim_date) do nothing
  returning true into v_new_claim;

  if coalesce(v_new_claim,false) then
    insert into public.vip_joker_wallet(user_id, freezer_count, swap_count, hint_count, streak_shield_count, updated_at)
    values(v_uid, 0, 1, 1, 0, now())
    on conflict (user_id) do update
    set swap_count = public.vip_joker_wallet.swap_count + 1,
        hint_count = public.vip_joker_wallet.hint_count + 1,
        updated_at = now();
  end if;

  select coalesce(freezer_count,0), coalesce(swap_count,0), coalesce(hint_count,0), coalesce(streak_shield_count,0)
  into v_freezer, v_swap, v_hint, v_shield
  from public.vip_joker_wallet
  where user_id = v_uid;

  return jsonb_build_object(
    'success',true,
    'already_claimed',not coalesce(v_new_claim,false),
    'freezer_count',coalesce(v_freezer,0),
    'swap_count',coalesce(v_swap,0),
    'hint_count',coalesce(v_hint,0),
    'streak_shield_count',coalesce(v_shield,0)
  );
end
$$;

revoke all on function public.claim_vip_daily_jokers_v7() from public, anon;
grant execute on function public.claim_vip_daily_jokers_v7() to authenticated, service_role;

-- 6) Entitlement reader now reports the real daily claim/wallet state while
-- preserving the newer live entitlement keys. ranked_live_assist remains false;
-- this rollout does not alter ranked score/timer/rating semantics.
create or replace function public.get_vip_entitlements_v7()
returns jsonb
language plpgsql
security definer
set search_path = 'public','pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_vip boolean := false;
  v_claimed boolean := false;
  v_freezer integer := 0;
  v_swap integer := 0;
  v_hint integer := 0;
  v_shield integer := 0;
begin
  if v_uid is null then raise exception 'unauthorized'; end if;

  select coalesce(is_vip,false)
  into v_vip
  from public.profiles
  where id = v_uid;

  select exists(
    select 1
    from public.vip_daily_joker_claims c
    where c.user_id = v_uid and c.claim_date = current_date
  ) into v_claimed;

  select coalesce(freezer_count,0), coalesce(swap_count,0), coalesce(hint_count,0), coalesce(streak_shield_count,0)
  into v_freezer, v_swap, v_hint, v_shield
  from public.vip_joker_wallet
  where user_id = v_uid;

  return jsonb_build_object(
    'is_vip',coalesce(v_vip,false),
    'daily_jokers_claimed',coalesce(v_claimed,false),
    'freezer_count',coalesce(v_freezer,0),
    'swap_count',coalesce(v_swap,0),
    'hint_count',coalesce(v_hint,0),
    'streak_shield_count',coalesce(v_shield,0),
    'xp_multiplier',1,
    'diamond_multiplier',1,
    'rewarded_ad_bypass',false,
    'used_words_access',true,
    'direct_messages_access',true,
    'ranked_live_assist',false,
    'post_match_analysis',coalesce(v_vip,false),
    'saved_friend_list',coalesce(v_vip,false),
    'private_rooms',coalesce(v_vip,false)
  );
end
$$;

revoke all on function public.get_vip_entitlements_v7() from public, anon;
grant execute on function public.get_vip_entitlements_v7() to authenticated, service_role;

comment on function public.claim_vip_daily_jokers_v7() is
  'PRO daily helper grant: one Hint and one Letter Swap per UTC day; freeze/shield remain dormant.';
comment on function public.get_vip_entitlements_v7() is
  'Hybrid Freemium/PRO entitlement snapshot; gameplay effects remain mode-scoped and server-authoritative.';

select pg_notify('pgrst','reload schema');
