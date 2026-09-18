-- PRO lifetime grants child feature entitlements with an explicit bundle source.
alter table public.store_entitlements
  drop constraint if exists store_entitlements_source_type_check;
alter table public.store_entitlements
  add constraint store_entitlements_source_type_check
  check (source_type in ('play','season','vip','event','admin','legacy','pro_bundle'));
