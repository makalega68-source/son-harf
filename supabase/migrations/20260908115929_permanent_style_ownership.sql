-- Preserve catalog rows and ownership. Retirement changes sale eligibility only.
-- Narrowly adapt the installed equip function instead of overwriting newer live slots.
-- Fail closed when its body differs from both reviewed versions.
do $migration$
declare
  original text := pg_get_functiondef('public.equip_shop_item(text)'::regprocedure);
  updated text;
begin
  if position('id=p_item_id and (active=true or rarity in (''SEASON'',''EVENT''))' in original) > 0 then
    updated := replace(original,
      'id=p_item_id and (active=true or rarity in (''SEASON'',''EVENT''))', 'id=p_item_id');
  elsif position('id=p_item_id and active=true' in original) > 0 then
    updated := replace(original, 'id=p_item_id and active=true', 'id=p_item_id');
  else
    raise exception 'unreviewed_equip_shop_item_definition';
  end if;
  execute updated;
end
$migration$;

-- Keep the existing active-catalog policy. Add access to retired metadata ONLY for its owner.
create policy shop_items_owned_read on public.shop_items
for select to authenticated
using (exists (
  select 1 from public.user_inventory i
  where i.user_id = (select auth.uid()) and i.item_id = shop_items.id
));

revoke execute on function public.equip_shop_item(text) from public, anon;
grant execute on function public.equip_shop_item(text) to authenticated;
