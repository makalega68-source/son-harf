-- Owner and moderator test accounts that hold the other mascots also get Cikcik, same source.
insert into public.store_entitlements(user_id, entitlement_key, source_type, source_id, status, expires_at, updated_at)
select distinct e.user_id, 'mascot_cikcik', e.source_type, e.source_id, 'active', null::timestamptz, now()
from public.store_entitlements e
where e.entitlement_key = 'mascot_astronot' and e.status = 'active' and e.source_type = 'admin'
on conflict (user_id, entitlement_key, source_type, source_id)
do update set status = 'active', expires_at = null, updated_at = now();
