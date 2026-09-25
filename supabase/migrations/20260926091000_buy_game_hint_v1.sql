-- Coin sink: an extra in-game hint costs 25 Son Coin once the free hints are used.
create or replace function public.buy_game_hint_v1(p_game text default 'word')
 returns table(success boolean, son_coin_spent integer, son_coin_balance integer)
 language plpgsql security definer set search_path to 'pg_catalog','public','pg_temp' as $function$
declare v_uid uuid:=auth.uid(); v_cost integer:=25; v_balance integer; v_owner boolean:=false; v_recent integer;
begin
  if v_uid is null then raise exception 'not_authenticated'; end if;
  -- A gentle cap so a runaway client cannot drain a wallet: 30 bought hints per hour.
  select count(*) into v_recent from public.diamond_ledger where user_id=v_uid and reason='game_hint' and created_at>now()-interval '1 hour';
  if v_recent>=30 then raise exception 'hint_rate_limited'; end if;
  select p.diamonds into v_balance from public.profiles p where p.id=v_uid for update;
  select exists(select 1 from public.owner_game_accounts o where o.user_id=v_uid and o.active and o.unlimited_son_coin) into v_owner;
  if v_owner then
    return query select true,0,coalesce(v_balance,0); return;
  end if;
  if coalesce(v_balance,0)<v_cost then raise exception 'insufficient_diamonds'; end if;
  update public.profiles p set diamonds=p.diamonds-v_cost,updated_at=now() where p.id=v_uid returning p.diamonds into v_balance;
  insert into public.diamond_ledger(user_id,delta,reason) values(v_uid,-v_cost,'game_hint');
  return query select true,v_cost,v_balance;
end $function$;
revoke all on function public.buy_game_hint_v1(text) from public, anon;
grant execute on function public.buy_game_hint_v1(text) to authenticated;
