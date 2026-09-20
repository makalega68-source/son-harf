begin;

create table if not exists public.son_coin_refund_debts (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  amount integer not null check (amount > 0),
  updated_at timestamptz not null default now()
);

alter table public.son_coin_refund_debts enable row level security;
revoke all on table public.son_coin_refund_debts from anon, authenticated;

create or replace function private.absorb_son_coin_refund_debt_v1()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_debt integer;
  v_increase integer;
  v_absorbed integer;
begin
  if new.diamonds <= old.diamonds then
    return new;
  end if;

  select d.amount
    into v_debt
    from public.son_coin_refund_debts d
   where d.user_id = old.id
   for update;

  if coalesce(v_debt, 0) <= 0 then
    return new;
  end if;

  v_increase := new.diamonds - old.diamonds;
  v_absorbed := least(v_increase, v_debt);
  new.diamonds := new.diamonds - v_absorbed;

  if v_debt = v_absorbed then
    delete from public.son_coin_refund_debts where user_id = old.id;
  else
    update public.son_coin_refund_debts
       set amount = amount - v_absorbed,
           updated_at = now()
     where user_id = old.id;
  end if;

  insert into public.diamond_ledger(user_id, delta, reason)
  values(old.id, -v_absorbed, 'google_play_refund_debt_repayment');

  return new;
end
$function$;

revoke all on function private.absorb_son_coin_refund_debt_v1() from public;

drop trigger if exists profiles_absorb_son_coin_refund_debt_v1 on public.profiles;
create trigger profiles_absorb_son_coin_refund_debt_v1
before update of diamonds on public.profiles
for each row
execute function private.absorb_son_coin_refund_debt_v1();

create or replace function public.reconcile_play_entitlement_v1(
  p_purchase_token text,
  p_play_state text,
  p_expires_at timestamptz default null,
  p_revoke boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path to ''
as $function$
declare
  v_purchase public.purchases%rowtype;
  v_status text;
  v_vip_active boolean := false;
  v_was_revoked boolean := false;
  v_coin_grant integer := 0;
  v_balance integer := 0;
  v_clawback integer := 0;
  v_debt_added integer := 0;
  v_debt_balance integer := 0;
begin
  select *
    into v_purchase
    from public.purchases
   where purchase_token = trim(p_purchase_token)
   for update;

  if not found then
    return jsonb_build_object('success', true, 'known', false);
  end if;

  v_was_revoked := v_purchase.status = 'revoked' or v_purchase.revoked_at is not null;

  v_status := case
    when p_revoke then 'revoked'
    when p_play_state = 'PURCHASED' then 'verified'
    when p_play_state = 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD' then 'grace'
    when p_play_state in ('SUBSCRIPTION_STATE_ON_HOLD', 'SUBSCRIPTION_STATE_PAUSED') then 'hold'
    when p_play_state = 'SUBSCRIPTION_STATE_CANCELED'
      and coalesce(p_expires_at, v_purchase.expires_at) > now() then 'canceled'
    when p_play_state in ('SUBSCRIPTION_STATE_EXPIRED', 'SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED') then 'expired'
    when p_play_state = 'SUBSCRIPTION_STATE_ACTIVE' then 'active'
    else 'pending'
  end;

  if p_revoke and not v_was_revoked then
    -- Use the immutable purchase ledger as the source of truth so current and future
    -- one-time products automatically claw back the exact Son Coin grant for this token.
    select coalesce(sum(l.delta), 0)::integer
      into v_coin_grant
      from public.diamond_ledger l
     where l.user_id = v_purchase.user_id
       and l.delta > 0
       and l.reason in (
         'google_play_purchase:' || trim(p_purchase_token),
         'google_play_pro_lifetime:' || trim(p_purchase_token)
       );

    if v_coin_grant > 0 then
      select coalesce(p.diamonds, 0)
        into v_balance
        from public.profiles p
       where p.id = v_purchase.user_id
       for update;

      v_clawback := least(greatest(v_balance, 0), v_coin_grant);
      v_debt_added := v_coin_grant - v_clawback;

      if v_clawback > 0 then
        update public.profiles
           set diamonds = diamonds - v_clawback,
               updated_at = now()
         where id = v_purchase.user_id;

        insert into public.diamond_ledger(user_id, delta, reason)
        values(
          v_purchase.user_id,
          -v_clawback,
          'google_play_refund_clawback:' || trim(p_purchase_token)
        );
      end if;

      if v_debt_added > 0 then
        insert into public.son_coin_refund_debts(user_id, amount, updated_at)
        values(v_purchase.user_id, v_debt_added, now())
        on conflict(user_id) do update
          set amount = public.son_coin_refund_debts.amount + excluded.amount,
              updated_at = now();
      end if;
    end if;
  end if;

  update public.purchases
     set play_state = p_play_state,
         status = v_status,
         expires_at = coalesce(p_expires_at, expires_at),
         revoked_at = case when p_revoke then coalesce(revoked_at, now()) else revoked_at end,
         last_checked_at = now()
   where id = v_purchase.id;

  if v_purchase.product_id in ('vip_monthly', 'vip_yearly') then
    update public.subscriptions
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id;

    update public.store_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id
       and entitlement_key = 'vip'
       and source_type = 'play'
       and source_id = trim(p_purchase_token);

    select exists(
      select 1
        from public.subscriptions
       where user_id = v_purchase.user_id
         and status in ('active', 'grace', 'canceled')
         and expires_at > now()
    ) into v_vip_active;

    update public.profiles
       set is_vip = (
         v_vip_active
         or public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')
       ),
       updated_at = now()
     where id = v_purchase.user_id;

  elsif v_purchase.product_id in ('season_pass', 'season_pass_monthly') then
    update public.season_pass_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id;

    update public.store_entitlements
       set status = v_status,
           expires_at = coalesce(p_expires_at, expires_at),
           updated_at = now()
     where user_id = v_purchase.user_id
       and entitlement_key = 'season_pass'
       and source_type = 'play'
       and source_id = trim(p_purchase_token);

  elsif v_purchase.product_id in ('series_game', 'letter_table', 'score_calculator', 'pro_lifetime') then
    if p_revoke then
      update public.store_entitlements
         set status = 'revoked',
             updated_at = now()
       where user_id = v_purchase.user_id
         and source_id = trim(p_purchase_token)
         and (
           (
             source_type = 'play'
             and entitlement_key = v_purchase.product_id
           )
           or (
             v_purchase.product_id = 'pro_lifetime'
             and source_type = 'pro_bundle'
             and entitlement_key in ('series_game', 'letter_table', 'score_calculator')
           )
         );

      if v_purchase.product_id = 'pro_lifetime' then
        select exists(
          select 1
            from public.subscriptions
           where user_id = v_purchase.user_id
             and status in ('active', 'grace', 'canceled')
             and expires_at > now()
        ) into v_vip_active;

        update public.profiles
           set is_vip = (
             v_vip_active
             or public.has_permanent_entitlement_v1(v_purchase.user_id, 'pro_lifetime')
           ),
           updated_at = now()
         where id = v_purchase.user_id;
      end if;
    end if;
  end if;

  select coalesce(d.amount, 0)
    into v_debt_balance
    from public.son_coin_refund_debts d
   where d.user_id = v_purchase.user_id;

  return jsonb_build_object(
    'success', true,
    'known', true,
    'status', v_status,
    'product_id', v_purchase.product_id,
    'son_coin_grant_found', v_coin_grant,
    'son_coin_clawed_back', v_clawback,
    'son_coin_debt_added', v_debt_added,
    'son_coin_refund_debt', coalesce(v_debt_balance, 0)
  );
end
$function$;

commit;
