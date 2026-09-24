-- The owner's two admin accounts test everything: lifetime PRO and every product, for free.
-- Idempotent data grant; every row is tagged so it can be found and revoked later.
--   makalega68@gmail.com, makalega58@gmail.com

begin;

create temporary table owner_test_accounts on commit drop as
select u.id
from auth.users u
join public.profiles p on p.id = u.id
where lower(u.email) in ('makalega68@gmail.com', 'makalega58@gmail.com');

-- Diamond-shop purchases are free for these accounts (purchase_shop_item honours this flag),
-- including items added to the shop later.
update public.admin_users a
set free_test_purchases = true
where a.user_id in (select id from owner_test_accounts);

-- Lifetime PRO: the permanent entitlement keeps is_vip true through every reconciliation.
update public.profiles
set is_vip = true, updated_at = now()
where id in (select id from owner_test_accounts);

-- Every Google Play product as a permanent entitlement.
insert into public.store_entitlements(user_id, entitlement_key, source_type, source_id, status, expires_at, updated_at)
select o.id, k, 'admin', 'owner_full_access_20260924', 'active', null, now()
from owner_test_accounts o
cross join unnest(array[
  'pro_lifetime', 'series_game', 'letter_table', 'score_calculator',
  'mascot_klasik', 'mascot_pembe', 'mascot_mavi_seytancik', 'mascot_kirmizi_seytancik',
  'mascot_tekir', 'mascot_robot', 'mascot_astronot'
]) k
on conflict (user_id, entitlement_key, source_type, source_id)
do update set status = 'active', expires_at = null, updated_at = now();

-- Season pass without an end date in practice.
insert into public.season_pass_entitlements(user_id, product_id, status, expires_at, updated_at)
select o.id, 'season_pass_monthly', 'active', timestamptz '2099-12-31 23:59:59+00', now()
from owner_test_accounts o
on conflict (user_id) do update
set product_id = excluded.product_id,
    status = 'active',
    expires_at = excluded.expires_at,
    updated_at = now();

-- Every shop item (styles, frames, themes) into the inventory.
insert into public.user_inventory(user_id, item_id)
select o.id, s.id
from owner_test_accounts o
cross join public.shop_items s
on conflict (user_id, item_id) do nothing;

-- A generous diamond balance for anything priced in diamonds.
with before as (
  select p.id, coalesce(p.diamonds, 0) as balance
  from public.profiles p
  where p.id in (select id from owner_test_accounts)
    and coalesce(p.diamonds, 0) < 999999
),
topped as (
  update public.profiles p
  set diamonds = 999999, updated_at = now()
  from before b
  where p.id = b.id
  returning p.id, b.balance
)
insert into public.diamond_ledger(user_id, delta, reason)
select t.id, 999999 - t.balance, 'owner_full_access_20260924'
from topped t;

commit;
