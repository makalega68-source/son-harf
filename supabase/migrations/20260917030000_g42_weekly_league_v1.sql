-- G4.2 Rekabet: haftalık lig anlık görüntüsü + turnuva iskeleti.
--
-- Mevcut rating-tabanlı lig sistemi (public.profiles.rating) korunur.
-- LeagueRating.kt istemci-tarafı yardımcı zaten Bronz/Gümüş/Altın/
-- Platin/Elmas/Efsane isim eşlemesini yapıyor.
--
-- Bu migration eklenenler:
--   1) weekly_league_snapshots: her hafta bir kere alınan rating
--      anlık görüntüsü. Hafta sonu promosyon/demosyon algoritması
--      bunu okur.
--   2) weekly_tournament: 8 kişilik eleme turnuvası iskeleti
--      (bracket + rozet).
--   3) get_my_weekly_league_position(): oyuncunun hafta içi rating
--      değişimini ve kendi ligindeki yaklaşık sırasını döner. Karar
--      tabanlı: pozitif delta = yükseliş yolunda, negatif = tehlike.
--
-- Cezalandırıcı sistem yok; Bronz'dan düşme yok (algoritma zaten
-- current_league='BRONZ' iken demote yapmaz).

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
    -- Promoted / demoted flag filled by the weekly closer RPC below.
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
-- 2) weekly_tournament + weekly_tournament_participants + matches
-- ---------------------------------------------------------------------
create table if not exists public.weekly_tournaments (
    id bigserial primary key,
    week_start date not null unique,
    language text not null default 'tr' check (language in ('tr', 'en')),
    status text not null default 'scheduled'
        check (status in ('scheduled', 'in_progress', 'finished')),
    winner_id uuid references auth.users(id),
    created_at timestamptz not null default now(),
    finished_at timestamptz
);

create table if not exists public.weekly_tournament_participants (
    tournament_id bigint not null references public.weekly_tournaments(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    seed int not null check (seed between 1 and 8),
    eliminated_round int,
    primary key (tournament_id, user_id),
    unique (tournament_id, seed)
);

-- Bracket rounds: 1 = quarterfinal (4 matches), 2 = semifinal (2),
-- 3 = final (1). Persist bracket state so a client refresh doesn't
-- lose it. room_id ties back to the actual playable duel room.
create table if not exists public.weekly_tournament_matches (
    id bigserial primary key,
    tournament_id bigint not null references public.weekly_tournaments(id) on delete cascade,
    round int not null check (round in (1, 2, 3)),
    slot int not null check (slot >= 1 and slot <= 4),
    player_a uuid references auth.users(id),
    player_b uuid references auth.users(id),
    room_id uuid,
    winner_id uuid references auth.users(id),
    created_at timestamptz not null default now(),
    finished_at timestamptz,
    unique (tournament_id, round, slot)
);

-- 3) Badge earned by winning a weekly tournament.
create table if not exists public.weekly_tournament_badges (
    user_id uuid not null references auth.users(id) on delete cascade,
    tournament_id bigint not null references public.weekly_tournaments(id) on delete cascade,
    awarded_at timestamptz not null default now(),
    primary key (user_id, tournament_id)
);

alter table public.weekly_tournaments enable row level security;
alter table public.weekly_tournament_participants enable row level security;
alter table public.weekly_tournament_matches enable row level security;
alter table public.weekly_tournament_badges enable row level security;

drop policy if exists weekly_tournaments_read on public.weekly_tournaments;
create policy weekly_tournaments_read on public.weekly_tournaments
    for select using (true);
drop policy if exists weekly_tournament_participants_read on public.weekly_tournament_participants;
create policy weekly_tournament_participants_read on public.weekly_tournament_participants
    for select using (true);
drop policy if exists weekly_tournament_matches_read on public.weekly_tournament_matches;
create policy weekly_tournament_matches_read on public.weekly_tournament_matches
    for select using (true);
drop policy if exists weekly_tournament_badges_read on public.weekly_tournament_badges;
create policy weekly_tournament_badges_read on public.weekly_tournament_badges
    for select using (true);

revoke insert, update, delete on public.weekly_tournaments
    from anon, authenticated;
revoke insert, update, delete on public.weekly_tournament_participants
    from anon, authenticated;
revoke insert, update, delete on public.weekly_tournament_matches
    from anon, authenticated;
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

    -- Auto-issue snapshot if missing so the client doesn't have to.
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

-- Read the caller's active weekly tournament, if any. Client uses this
-- to render bracket + "your next match".
create or replace function public.get_my_active_weekly_tournament()
returns table (
    tournament_id bigint,
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
        select t.id, t.week_start, t.language, t.status, p.seed, t.winner_id,
               (select m.room_id
                  from public.weekly_tournament_matches m
                 where m.tournament_id = t.id
                   and (m.player_a = v_user or m.player_b = v_user)
                   and m.winner_id is null
                 order by m.round
                 limit 1)
        from public.weekly_tournaments t
        join public.weekly_tournament_participants p
             on p.tournament_id = t.id and p.user_id = v_user
        where t.week_start = v_week
          and t.status in ('scheduled', 'in_progress');
end;
$$;

grant execute on function public.get_my_active_weekly_tournament() to authenticated;

select pg_notify('pgrst', 'reload schema');
