-- Fix PL/pgSQL output-column ambiguity in the existing mascot fruit purchase RPC.
-- Keeps the owner unlimited Son Coin entitlement server-side and non-depleting.

create or replace function public.buy_mascot_fruit_v1(p_fruit_id text, p_quantity integer default 1)
returns table(
  success boolean,
  fruit_id text,
  quantity integer,
  inventory_quantity integer,
  son_coin_spent integer,
  son_coin_balance integer
)
language plpgsql
security definer
set search_path = pg_catalog, public, pg_temp
as $$
declare
  v_uid uuid:=auth.uid();
  v_price integer;
  v_magic boolean;
  v_cost integer;
  v_balance integer;
  v_qty integer:=greatest(1,least(coalesce(p_quantity,1),20));
  v_inventory integer;
  v_owner_unlimited boolean:=false;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;

  select c.son_coin_price,c.is_magic into v_price,v_magic
  from public.mascot_fruit_catalog c
  where c.id=p_fruit_id and c.active=true;
  if not found then raise exception 'fruit_not_found'; end if;
  if not v_magic or v_price<=0 then raise exception 'fruit_not_purchasable'; end if;

  v_cost:=v_price*v_qty;
  select p.diamonds into v_balance from public.profiles p where p.id=v_uid for update;

  select exists(
    select 1 from public.owner_game_accounts o
    where o.user_id=v_uid and o.active and o.unlimited_son_coin
  ) into v_owner_unlimited;

  if not v_owner_unlimited then
    if coalesce(v_balance,0)<v_cost then raise exception 'insufficient_diamonds'; end if;
    update public.profiles p set diamonds=p.diamonds-v_cost,updated_at=now()
    where p.id=v_uid returning p.diamonds into v_balance;

    insert into public.diamond_ledger(user_id,delta,reason)
    values(v_uid,-v_cost,'mascot_magic_fruit');
  else
    insert into public.admin_audit_log(admin_id,action,target_type,target_id,after_data,outcome)
    values(
      v_uid,'owner_unlimited_son_coin_purchase','mascot_fruit',p_fruit_id,
      jsonb_build_object('quantity',v_qty,'normal_son_coin_cost',v_cost),'success'
    );
  end if;

  insert into public.user_mascot_fruit_inventory(user_id,fruit_id,quantity)
  values(v_uid,p_fruit_id,v_qty)
  on conflict on constraint user_mascot_fruit_inventory_pkey do update
  set quantity=public.user_mascot_fruit_inventory.quantity+excluded.quantity,updated_at=now()
  returning public.user_mascot_fruit_inventory.quantity into v_inventory;

  return query
  select true,p_fruit_id,v_qty,v_inventory,
         case when v_owner_unlimited then 0 else v_cost end,
         v_balance;
end
$$;

revoke all on function public.buy_mascot_fruit_v1(text,integer) from public, anon;
grant execute on function public.buy_mascot_fruit_v1(text,integer) to authenticated, service_role;
