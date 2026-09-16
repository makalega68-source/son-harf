-- G4.3 — Daily bonus chest claim.
--
-- Bugünün üç görevi de tamamlandığında oyuncu günlük sandığı bir kez
-- alabilir. Ödül: +15 elmas. Claim kaydı ve ekonomi ledger girdisi aynı
-- transaction içinde yazılır; aynı anda iki istek gelse bile tek ödül verilir.

set search_path = public, pg_temp;

create table if not exists public.chest_claimed_days (
    user_id uuid not null references public.profiles(id) on delete cascade,
    day date not null,
    diamonds_awarded integer not null default 15 check (diamonds_awarded > 0),
    claimed_at timestamptz not null default now(),
    primary key (user_id, day)
);

alter table public.chest_claimed_days enable row level security;

drop policy if exists chest_claimed_days_owner_read on public.chest_claimed_days;
create policy chest_claimed_days_owner_read on public.chest_claimed_days
    for select to authenticated
    using (auth.uid() = user_id);

revoke insert, update, delete on public.chest_claimed_days from anon, authenticated;
grant select on public.chest_claimed_days to authenticated;

create or replace function public.claim_daily_bonus_chest()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_total integer := 0;
    v_completed integer := 0;
    v_claimed_day date;
    v_balance integer;
begin
    if v_user is null then
        raise exception 'not_authenticated';
    end if;

    -- Exactly the three daily assignments (Son Harf, Kuşatma, Kelime Yolu)
    -- must exist and all three must be completed server-side.
    select
        count(*)::int,
        count(*) filter (where dq.completed)::int
    into v_total, v_completed
    from public.daily_quests dq
    where dq.user_id = v_user
      and dq.day = v_today;

    if v_total <> 3 or v_completed <> 3 then
        return jsonb_build_object(
            'success', false,
            'reason', 'quests_incomplete',
            'day', v_today,
            'completed', v_completed,
            'required', 3
        );
    end if;

    -- Primary key (user_id, day) is the concurrency/idempotency guard.
    insert into public.chest_claimed_days (user_id, day, diamonds_awarded)
    values (v_user, v_today, 15)
    on conflict (user_id, day) do nothing
    returning day into v_claimed_day;

    if v_claimed_day is null then
        select coalesce(p.diamonds, 0)
        into v_balance
        from public.profiles p
        where p.id = v_user;

        return jsonb_build_object(
            'success', false,
            'reason', 'already_claimed',
            'day', v_today,
            'granted', 0,
            'diamonds', coalesce(v_balance, 0)
        );
    end if;

    update public.profiles
    set diamonds = coalesce(diamonds, 0) + 15,
        updated_at = now()
    where id = v_user
    returning diamonds into v_balance;

    if not found then
        raise exception 'profile_not_found';
    end if;

    insert into public.diamond_ledger (user_id, delta, reason)
    values (v_user, 15, 'daily_bonus_chest');

    return jsonb_build_object(
        'success', true,
        'day', v_today,
        'granted', 15,
        'diamonds', v_balance
    );
end;
$$;

revoke all on function public.claim_daily_bonus_chest() from public, anon;
grant execute on function public.claim_daily_bonus_chest() to authenticated;

comment on function public.claim_daily_bonus_chest() is
'G4.3: awards +15 diamonds once per Europe/Istanbul day after all 3 daily quests are completed; claim is transaction-safe and recorded in chest_claimed_days + diamond_ledger.';

select pg_notify('pgrst', 'reload schema');
