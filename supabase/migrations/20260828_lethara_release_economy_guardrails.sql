-- Final Lethara economy guardrails.
-- Mascot fruit may accelerate companion/lore progression only; it never affects ranked power.
alter table public.mascot_fruit_catalog
  drop constraint if exists mascot_fruit_catalog_magic_xp_contract;

alter table public.mascot_fruit_catalog
  add constraint mascot_fruit_catalog_magic_xp_contract
  check (
    (is_magic = false and xp_reward = 3 and son_coin_price = 0)
    or
    (is_magic = true and xp_reward in (10, 20, 30) and son_coin_price > 0)
  );
