-- Chibi Wizard production activation after isolated Vulkan runtime PASS.
-- Evidence: Final Mascot Runtime run 33112639157.
-- Product rule: cosmetic only; no match power or pay-to-win benefit.

update public.shop_items
set active = true
where id = 'mascot_chibi_wizard'
  and kind = 'mascot';

do $$
begin
  if not exists (
    select 1
    from public.shop_items
    where id = 'mascot_chibi_wizard'
      and kind = 'mascot'
      and active = true
      and diamond_price = 700
      and vip_only = false
  ) then
    raise exception 'chibi_activation_contract_failed';
  end if;
end
$$;
