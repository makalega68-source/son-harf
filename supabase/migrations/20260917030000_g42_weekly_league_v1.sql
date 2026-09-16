-- G4.2 Rekabet: haftalık lig anlık görüntüsü + mevcut haftalık turnuva entegrasyonu.
--
-- Mevcut rating-tabanlı lig sistemi (public.profiles.rating) ve mevcut
-- weekly_tournaments / weekly_tournament_entries / weekly_tournament_match_events
-- tabloları korunur. Bu migration ikinci bir turnuva şeması oluşturmaz.
--
-- Bu migration eklenenler:
--   1) weekly_league_snapshots: her hafta bir kere alınan rating
--      anlık görüntüsü. Hafta sonu promosyon/demosyon algoritması
--      bunu okur.
--   2) weekly_tournament_badges: mevcut haftalık turnuvayı kazanan
--      oyuncuya verilecek kalıcı rozet kaydı.
--   3) get_my_weekly_league_position(): oyuncunun hafta içi rating
--      değişimini döner.
--   4) get_my_active_weekly_tournament(): mevcut turnuva şemasına
--      uyumlu, geriye dönük istemci özeti.
--
-- Cezalandırıcı sistem yok; Bronz'dan düşme yok.

set search_path = public, pg_temp;

-- ---------------------------------------------------------------------
-- 1) weekly_league_snapshots
-- ---------------------------------------------------------------------
create table if not exists public.weekly_league_snapshots (
    user_id uuid not null references auth.users(id) on delete cascade,
    week_start date not null,
    rating_start int not null,
    rating_end int,
    league_start text not null,
    league_end text,
    promoted boolean,
    demoted boolean,
    created_at timestamptz not null default now(),
    closed_at timestamptz,
    primary key (user_id, week_start)
);

alter table public.weekly_league_snapshots enable row level security;
drop policy if exists weekly_league_snapshots_owner_read on public.weekly_league_snapshots;
create policy weekly_league_snapshots_owner_read on public.weekly_league_snapshots
    for select using (auth.uid() = user_id);
revoke insert, update, delete on public.weekly_league_snapshots
    from anon, authenticated;

-- ---------------------------------------------------------------------
-- 2) Existing weekly tournament system: winner badge only.
--    weekly_tournaments.id production'da UUID'dir; yeni/çakışan bracket
--    tabloları oluşturulmaz.
-- ---------------------------------------------------------------------
create table if not exists public.weekly_tournament_badges (
    user_id uuid not null references auth.users(id) on delete cascade,
    tournament_id uuid not null references public.weekly_tournaments(id) on delete cascade,
    awarded_at timestamptz not null default now(),
    primary key (user_id, tournament_id)
);

alter table public.weekly_tournament_badges enable row level security;
drop policy if exists weekly_tournament_badges_read on public.weekly_tournament_badges;
create policy weekly_tournament_badges_read on public.weekly_tournament_badges
    for select using (true);
revoke insert, update, delete on public.weekly_tournament_badges
    from anon, authenticated;

-- ---------------------------------------------------------------------
-- 3) RPCs
-- ---------------------------------------------------------------------

-- Monday of week (aligned with G4.4).
create or replace function public.sonharf_g42_week_start(p_day date)
returns date
language sql
immutable
as $$
    select p_day - ((extract(isodow from p_day)::int - 1))
$$;

-- Take a snapshot of the caller's rating for the current week.
-- Idempotent: same week, same user is a no-op.
create or replace function public.sonharf_g42_touch_weekly_snapshot()
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_week date := public.sonharf_g42_week_start(v_today);
    v_rating int;
    v_league text;
begin
    if v_user is null then return; end if;
    select coalesce(rating, 1000) into v_rating from public.profiles where id = v_user;
    if v_rating is null then return; end if;
    v_league := case
        when v_rating >= 1800 then 'EFSANE'
        when v_rating >= 1600 then 'ELMAS'
        when v_rating >= 1400 then 'PLATİN'
        when v_rating >= 1250 then 'ALTIN'
        when v_rating >= 1100 then 'GÜMÜŞ'
        else 'BRONZ'
    end;
    insert into public.weekly_league_snapshots
        (user_id, week_start, rating_start, league_start)
    values (v_user, v_week, v_rating, v_league)
    on conflict do nothing;
end;
$$;

grant execute on function public.sonharf_g42_touch_weekly_snapshot() to authenticated;

-- Position summary the client uses to render the weekly rank change chip.
create or replace function public.get_my_weekly_league_position()
returns table (
    week_start date,
    rating_start int,
    rating_now int,
    league_start text,
    league_now text,
    delta int,
    is_bronze boolean
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_week date := public.sonharf_g42_week_start(v_today);
    v_snap public.weekly_league_snapshots;
    v_current_rating int;
    v_current_league text;
begin
    if v_user is null then raise exception 'not_authenticated'; end if;

    perform public.sonharf_g42_touch_weekly_snapshot();

    select * into v_snap
    from public.weekly_league_snapshots
    where user_id = v_user and week_start = v_week;
    if not found then return; end if;

    select coalesce(rating, 1000) into v_current_rating from public.profiles where id = v_user;
    v_current_league := case
        when v_current_rating >= 1800 then 'EFSANE'
        when v_current_rating >= 1600 then 'ELMAS'
        when v_current_rating >= 1400 then 'PLATİN'
        when v_current_rating >= 1250 then 'ALTIN'
        when v_current_rating >= 1100 then 'GÜMÜŞ'
        else 'BRONZ'
    end;

    return query select
        v_snap.week_start,
        v_snap.rating_start,
        v_current_rating,
        v_snap.league_start,
        v_current_league,
        v_current_rating - v_snap.rating_start,
        v_snap.league_start = 'BRONZ';
end;
$$;

grant execute on function public.get_my_weekly_league_position() to authenticated;

-- Existing weekly tournament schema compatibility layer.
-- Legacy tournament rows have UUID id, starts_at/ends_at and membership
-- in weekly_tournament_entries. There is no separate participant/round
-- table, so unsupported seed/next-room fields stay NULL instead of
-- inventing a second bracket model.
create or replace function public.get_my_active_weekly_tournament()
returns table (
    tournament_id uuid,
    week_start date,
    language text,
    status text,
    my_seed int,
    winner_id uuid,
    my_next_room_id uuid
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_week date := public.sonharf_g42_week_start(v_today);
begin
    if v_user is null then raise exception 'not_authenticated'; end if;

    return query
    with selected_tournament as (
        select t.id, t.week_start, t.starts_at, t.ends_at
        from public.weekly_tournaments t
        join public.weekly_tournament_entries e
          on e.tournament_id = t.id
         and e.user_id = v_user
        where t.week_start = v_week
        order by t.starts_at desc, t.id
        limit 1
    ), score as (
        select
            e.tournament_id,
            e.user_id,
            coalesce(sum(m.points), 0)::bigint as pts,
            count(*) filter (where m.won)::bigint as wins,
            count(m.id)::bigint as matches
        from public.weekly_tournament_entries e
        left join public.weekly_tournament_match_events m
          on m.tournament_id = e.tournament_id
         and m.user_id = e.user_id
        where e.tournament_id = (select st.id from selected_tournament st)
        group by e.tournament_id, e.user_id
    ), ranked as (
        select
            s.*,
            row_number() over (
                partition by s.tournament_id
                order by s.pts desc, s.wins desc, s.matches desc, s.user_id
            ) as rnk
        from score s
        where s.matches > 0
    )
    select
        st.id,
        st.week_start,
        'tr'::text,
        case
            when now() < st.starts_at then 'scheduled'::text
            when now() < st.ends_at then 'in_progress'::text
            else 'finished'::text
        end,
        null::int,
        case
            when now() >= st.ends_at then (
                select r.user_id from ranked r where r.rnk = 1 limit 1
            )
            else null::uuid
        end,
        null::uuid
    from selected_tournament st;
end;
$$;

grant execute on function public.get_my_active_weekly_tournament() to authenticated;

select pg_notify('pgrst', 'reload schema');
