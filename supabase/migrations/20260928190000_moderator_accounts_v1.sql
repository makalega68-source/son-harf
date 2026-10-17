-- Moderator (tester) accounts named by the owner: permanent PRO and every product for free.
-- Only the e-mail addresses in moderator_accounts get anything. Adding an address grants the
-- account at once if it exists, otherwise the moment its profile is created (first sign-in).
-- No admin-panel rights are given.

create table if not exists public.moderator_accounts (
  email text primary key check (email = lower(email)),
  note text,
  added_at timestamptz not null default now()
);
alter table public.moderator_accounts enable row level security;
revoke all on public.moderator_accounts from anon, authenticated;

create or replace function public.grant_moderator_access_internal_v1(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_balance int;
begin
  if p_user_id is null then return; end if;
  if not exists (
    select 1 from auth.users u join public.moderator_accounts m on m.email = lower(u.email)
    where u.id = p_user_id
  ) then
    return;
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id) then return; end if;

  update public.profiles set is_vip = true, updated_at = now() where id = p_user_id and not coalesce(is_vip, false);

  insert into public.store_entitlements(user_id, entitlement_key, source_type, source_id, status, expires_at, updated_at)
  select p_user_id, k, 'admin', 'moderator_access', 'active', null, now()
  from unnest(array[
    'pro_lifetime', 'series_game', 'letter_table', 'score_calculator',
    'mascot_klasik', 'mascot_pembe', 'mascot_mavi_seytancik', 'mascot_kirmizi_seytancik',
    'mascot_tekir', 'mascot_robot', 'mascot_astronot'
  ]) k
  on conflict (user_id, entitlement_key, source_type, source_id)
  do update set status = 'active', expires_at = null, updated_at = now();

  insert into public.season_pass_entitlements(user_id, product_id, status, expires_at, updated_at)
  values (p_user_id, 'season_pass_monthly', 'active', timestamptz '2099-12-31 23:59:59+00', now())
  on conflict (user_id) do update
  set product_id = excluded.product_id, status = 'active', expires_at = excluded.expires_at, updated_at = now();

  insert into public.user_inventory(user_id, item_id)
  select p_user_id, s.id from public.shop_items s
  on conflict (user_id, item_id) do nothing;

  select coalesce(diamonds, 0) into v_balance from public.profiles where id = p_user_id;
  if v_balance < 50000 then
    update public.profiles set diamonds = 50000, updated_at = now() where id = p_user_id;
    insert into public.diamond_ledger(user_id, delta, reason) values (p_user_id, 50000 - v_balance, 'moderator_access');
  end if;
end;
$$;
revoke all on function public.grant_moderator_access_internal_v1(uuid) from public, anon, authenticated;

-- A moderator's profile created later (first sign-in) is granted at once.
create or replace function public.moderator_profile_grant_v1()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public.grant_moderator_access_internal_v1(new.id);
  return new;
end;
$$;
drop trigger if exists moderator_profile_grant on public.profiles;
create trigger moderator_profile_grant
after insert on public.profiles
for each row execute function public.moderator_profile_grant_v1();

-- Adding an address grants the existing account at once.
create or replace function public.moderator_list_grant_v1()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public.grant_moderator_access_internal_v1(u.id)
  from auth.users u where lower(u.email) = new.email;
  return new;
end;
$$;
drop trigger if exists moderator_list_grant on public.moderator_accounts;
create trigger moderator_list_grant
after insert on public.moderator_accounts
for each row execute function public.moderator_list_grant_v1();

-- Items added to the shop later reach every moderator too.
create or replace function public.moderator_new_item_grant_v1()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.user_inventory(user_id, item_id)
  select u.id, new.id
  from auth.users u
  join public.moderator_accounts m on m.email = lower(u.email)
  join public.profiles p on p.id = u.id
  on conflict (user_id, item_id) do nothing;
  return new;
end;
$$;
drop trigger if exists moderator_new_item_grant on public.shop_items;
create trigger moderator_new_item_grant
after insert on public.shop_items
for each row execute function public.moderator_new_item_grant_v1();
