-- Lobby ticker: one public read for the thin scrolling band at the top of every screen.
-- Returns the active admin announcement (if enabled) and the latest real store purchases:
-- coin purchases from the diamond ledger and verified Google Play purchases. Admin test
-- grants, default items and rewards are not purchases and never appear. Only the buyer's
-- public display name and the product name are exposed.

create or replace function public.get_ticker_feed_v1(p_limit integer default 12)
returns table(kind text, player_name text, item_name_tr text, item_name_en text, message_tr text, message_en text, happened_at timestamptz)
language sql
stable
security definer
set search_path to 'pg_catalog', 'public', 'pg_temp'
as $$
  (
    select 'announcement'::text, null::text, null::text, null::text,
           nullif(btrim(a.message_tr), ''), nullif(btrim(coalesce(a.message_en, a.message_tr)), ''), a.updated_at
    from public.admin_announcement a
    where a.singleton and a.enabled and nullif(btrim(a.message_tr), '') is not null
  )
  union all
  (
    select 'purchase'::text, p.display_name,
           coalesce(s.name_tr, case when x.item_id ~* '(pro|vip|premium)' then 'PRO Üyelik' else 'Jeton Paketi' end),
           coalesce(s.name_en, case when x.item_id ~* '(pro|vip|premium)' then 'PRO Membership' else 'Coin Pack' end),
           null::text, null::text, x.happened_at
    from (
      select l.user_id, l.item_id, l.created_at as happened_at
      from public.diamond_ledger l
      where l.reason = 'shop_purchase' and l.item_id is not null and l.created_at > now() - interval '3 days'
      union all
      select pu.user_id, coalesce(si.id, pu.product_id), coalesce(pu.verified_at, pu.created_at)
      from public.purchases pu
      left join public.shop_items si on si.metadata->>'play_product_id' = pu.product_id
      where pu.verified_at is not null and pu.revoked_at is null
        and coalesce(pu.verified_at, pu.created_at) > now() - interval '3 days'
    ) x
    join public.profiles p on p.id = x.user_id
    left join public.shop_items s on s.id = x.item_id
    where nullif(btrim(p.display_name), '') is not null
    order by x.happened_at desc
    limit greatest(1, least(coalesce(p_limit, 12), 30))
  );
$$;

revoke all on function public.get_ticker_feed_v1(integer) from public;
grant execute on function public.get_ticker_feed_v1(integer) to authenticated;
