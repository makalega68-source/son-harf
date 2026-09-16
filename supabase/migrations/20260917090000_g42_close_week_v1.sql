-- G4.2 — Haftalık lig kapanışı.
--
-- sonharf_g42_close_week():
--   * Aktif haftada snapshot'ı eksik profilleri yakalar.
--   * Her league_start grubu içinde rating'e göre top %20 promote,
--     bottom %20 demote eder. BRONZ demote, EFSANE promote olmaz.
--   * Rating'i yalnızca gerekli komşu lig sınırına taşır; zaten o
--     sınırı aşmış oyuncunun puanını geriye çekmez.
--   * Snapshot promoted/demoted/rating_end/league_end/closed_at alanlarını
--     doldurur.
--   * Bitmiş mevcut haftalık turnuvanın kazananına badge yazar.
--   * closed_at + advisory lock sayesinde tekrar çağrıda idempotenttir.

set search_path = public, pg_temp;

create or replace function public.sonharf_g42_close_week()
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_week date := public.sonharf_g42_week_start(v_today);
    v_promoted int := 0;
    v_demoted int := 0;
    v_closed int := 0;
    v_badges int := 0;
    v_move_count int;
    v_final_rating int;
    v_final_league text;
    v_is_promoted boolean;
    v_is_demoted boolean;
    rec record;
begin
    -- Only one weekly closer may mutate ratings/snapshots at a time.
    perform pg_advisory_xact_lock(4242001);

    -- Capture anyone who did not trigger the lazy client snapshot during
    -- the week. For these late captures rating_start is necessarily the
    -- best available current rating; no historical value is invented.
    insert into public.weekly_league_snapshots (
        user_id,
        week_start,
        rating_start,
        league_start
    )
    select
        p.id,
        v_week,
        coalesce(p.rating, 1000),
        case
            when coalesce(p.rating, 1000) >= 1800 then 'EFSANE'
            when coalesce(p.rating, 1000) >= 1600 then 'ELMAS'
            when coalesce(p.rating, 1000) >= 1400 then 'PLATİN'
            when coalesce(p.rating, 1000) >= 1250 then 'ALTIN'
            when coalesce(p.rating, 1000) >= 1100 then 'GÜMÜŞ'
            else 'BRONZ'
        end
    from public.profiles p
    where not exists (
        select 1
        from public.weekly_league_snapshots s
        where s.user_id = p.id
          and s.week_start = v_week
    )
    on conflict (user_id, week_start) do nothing;

    -- Rank only still-open snapshots. floor(N * .20) means leagues with
    -- fewer than 5 members move nobody; top/bottom groups never overlap.
    for rec in
        with ranked as (
            select
                s.user_id,
                s.league_start,
                coalesce(p.rating, 1000)::int as rating_now,
                row_number() over (
                    partition by s.league_start
                    order by coalesce(p.rating, 1000) desc, s.user_id
                ) as rank_desc,
                row_number() over (
                    partition by s.league_start
                    order by coalesce(p.rating, 1000) asc, s.user_id
                ) as rank_asc,
                count(*) over (partition by s.league_start) as league_size
            from public.weekly_league_snapshots s
            join public.profiles p on p.id = s.user_id
            where s.week_start = v_week
              and s.closed_at is null
        )
        select * from ranked
        order by league_start, rank_desc
    loop
        v_move_count := floor(rec.league_size::numeric * 0.20)::int;
        v_is_promoted := rec.league_start <> 'EFSANE'
            and v_move_count > 0
            and rec.rank_desc <= v_move_count;
        v_is_demoted := rec.league_start <> 'BRONZ'
            and v_move_count > 0
            and rec.rank_asc <= v_move_count;

        v_final_rating := rec.rating_now;

        if v_is_promoted then
            v_final_rating := case rec.league_start
                when 'BRONZ' then greatest(rec.rating_now, 1100)
                when 'GÜMÜŞ' then greatest(rec.rating_now, 1250)
                when 'ALTIN' then greatest(rec.rating_now, 1400)
                when 'PLATİN' then greatest(rec.rating_now, 1600)
                when 'ELMAS' then greatest(rec.rating_now, 1800)
                else rec.rating_now
            end;
            v_promoted := v_promoted + 1;
        elsif v_is_demoted then
            v_final_rating := case rec.league_start
                when 'GÜMÜŞ' then least(rec.rating_now, 1099)
                when 'ALTIN' then least(rec.rating_now, 1249)
                when 'PLATİN' then least(rec.rating_now, 1399)
                when 'ELMAS' then least(rec.rating_now, 1599)
                when 'EFSANE' then least(rec.rating_now, 1799)
                else rec.rating_now
            end;
            v_demoted := v_demoted + 1;
        end if;

        v_final_league := case
            when v_final_rating >= 1800 then 'EFSANE'
            when v_final_rating >= 1600 then 'ELMAS'
            when v_final_rating >= 1400 then 'PLATİN'
            when v_final_rating >= 1250 then 'ALTIN'
            when v_final_rating >= 1100 then 'GÜMÜŞ'
            else 'BRONZ'
        end;

        if v_is_promoted or v_is_demoted then
            update public.profiles
            set rating = v_final_rating,
                updated_at = now()
            where id = rec.user_id;
        end if;

        update public.weekly_league_snapshots
        set rating_end = v_final_rating,
            league_end = v_final_league,
            promoted = v_is_promoted,
            demoted = v_is_demoted,
            closed_at = now()
        where user_id = rec.user_id
          and week_start = v_week
          and closed_at is null;

        if found then
            v_closed := v_closed + 1;
        end if;
    end loop;

    -- Existing weekly tournament winner: same canonical ranking used by
    -- weekly tournament history/reward logic. Only completed tournaments
    -- with at least one played match can award a badge.
    with score as (
        select
            e.tournament_id,
            e.user_id,
            coalesce(sum(m.points), 0)::bigint as pts,
            count(*) filter (where m.won)::bigint as wins,
            count(m.id)::bigint as matches
        from public.weekly_tournament_entries e
        join public.weekly_tournaments t on t.id = e.tournament_id
        left join public.weekly_tournament_match_events m
          on m.tournament_id = e.tournament_id
         and m.user_id = e.user_id
        where t.week_start = v_week
          and t.ends_at <= now()
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
    insert into public.weekly_tournament_badges (user_id, tournament_id)
    select r.user_id, r.tournament_id
    from ranked r
    where r.rnk = 1
    on conflict (user_id, tournament_id) do nothing;

    get diagnostics v_badges = row_count;

    return jsonb_build_object(
        'week_start', v_week,
        'snapshots_closed', v_closed,
        'promoted', v_promoted,
        'demoted', v_demoted,
        'badges_awarded', v_badges
    );
end;
$$;

revoke all on function public.sonharf_g42_close_week() from public, anon, authenticated;
grant execute on function public.sonharf_g42_close_week() to service_role;

comment on function public.sonharf_g42_close_week() is
'G4.2 weekly closer: snapshots active week, promotes top 20%, demotes bottom 20% (no Bronze demotion), adjusts rating, records snapshot result and awards weekly tournament winner badge.';

select pg_notify('pgrst', 'reload schema');
