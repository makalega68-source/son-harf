-- PRO Gold profile frame v1.
-- The verified purchased Gold asset becomes a permanent inventory reward for PRO members.
-- Ownership is never revoked when PRO expires; equipped state remains server-authoritative.

insert into public.shop_items (
  id, kind, name_tr, name_en, description_tr, description_en,
  diamond_price, vip_only, active, sort_order
)
values (
  'frame_asset_gold',
  'profile_frame',
  'PRO Altın Çerçeve',
  'PRO Gold Frame',
  'PRO üyelik ile kalıcı olarak profil envanterine eklenen altın çerçeve.',
  'Gold frame permanently added to the profile inventory with PRO membership.',
  0,
  true,
  false,
  525
)
on conflict (id) do update set
  kind = excluded.kind,
  name_tr = excluded.name_tr,
  name_en = excluded.name_en,
  description_tr = excluded.description_tr,
  description_en = excluded.description_en,
  diamond_price = 0,
  vip_only = true,
  active = false,
  sort_order = excluded.sort_order;

-- Backfill every account that already has active PRO/VIP status.
insert into public.user_inventory(user_id, item_id)
select id, 'frame_asset_gold'
from public.profiles
where coalesce(is_vip, false)
on conflict do nothing;

-- Future PRO activations receive the same permanent reward exactly once.
create or replace function public.grant_pro_gold_profile_frame_v1()
returns trigger
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $$
begin
  if coalesce(new.is_vip, false) then
    insert into public.user_inventory(user_id, item_id)
    values (new.id, 'frame_asset_gold')
    on conflict do nothing;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_grant_pro_gold_profile_frame_v1 on public.profiles;
create trigger trg_grant_pro_gold_profile_frame_v1
after insert or update of is_vip on public.profiles
for each row execute function public.grant_pro_gold_profile_frame_v1();

revoke all on function public.grant_pro_gold_profile_frame_v1() from public, anon, authenticated;
grant execute on function public.grant_pro_gold_profile_frame_v1() to service_role;

-- Public presentation API: exposes only the equipped profile-frame id of an authenticated player's
-- requested profile. Inventory, entitlement, economy and other equipped cosmetics remain private.
create or replace function public.get_public_profile_frame_v1(p_user_id uuid)
returns table(user_id uuid, profile_frame_id text)
language sql
stable
security definer
set search_path = ''
as $$
  select e.user_id, e.profile_frame_id
  from public.user_equipped_cosmetics e
  join public.shop_items s on s.id = e.profile_frame_id
  where auth.uid() is not null
    and e.user_id = p_user_id
    and s.kind = 'profile_frame'
  limit 1;
$$;

revoke all on function public.get_public_profile_frame_v1(uuid) from public, anon;
grant execute on function public.get_public_profile_frame_v1(uuid) to authenticated, service_role;

select pg_notify('pgrst', 'reload schema');
