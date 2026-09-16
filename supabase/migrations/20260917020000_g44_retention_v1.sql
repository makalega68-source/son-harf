-- G4.4 Geri Dönme (Retention):
--   1) Günlük seri (🔥) — her gün en az 1 maç. Haftada 1 kez kaçırma
--      hakkı (grace) seriyi bozmaz. "Seri kurtarma" satışı YOK,
--      cezalandırıcı sistem YOK.
--   2) Kelime koleksiyonu — oyuncunun oynadığı farklı kelimeler
--      sayılır. "Bu ay 240 yeni kelime" gibi göstergeler için.
--   3) Mevsimsel tema — yönetici panelinden aç/kapa, tema kelimelerine
--      2x puan. Oyuncuya görünen ek rozet.
--
-- Tasarım: G4.1 gibi additive. Mevcut submit_word / bot RPC'leri
-- değişmez; işi trigger'lar yapar.

set search_path = public, pg_temp;

-- ---------------------------------------------------------------------
-- 1) player_daily_streaks
-- ---------------------------------------------------------------------
create table if not exists public.player_daily_streaks (
    user_id uuid primary key references auth.users(id) on delete cascade,
    current_streak int not null default 0 check (current_streak >= 0),
    longest_streak int not null default 0 check (longest_streak >= 0),
    -- Turkish calendar day of the last recorded match.
    last_played_day date,
    -- ISO week starting Monday, tracked so grace usage resets weekly.
    grace_week_started date,
    grace_used_week int not null default 0 check (grace_used_week >= 0),
    updated_at timestamptz not null default now()
);

alter table public.player_daily_streaks enable row level security;
drop policy if exists player_daily_streaks_owner_read on public.player_daily_streaks;
create policy player_daily_streaks_owner_read on public.player_daily_streaks
    for select using (auth.uid() = user_id);
revoke insert, update, delete on public.player_daily_streaks
    from anon, authenticated;

-- ---------------------------------------------------------------------
-- 2) player_word_collection
-- ---------------------------------------------------------------------
create table if not exists public.player_word_collection (
    user_id uuid not null references auth.users(id) on delete cascade,
    language text not null check (language in ('tr', 'en')),
    normalized_word text not null,
    first_played_at timestamptz not null default now(),
    primary key (user_id, language, normalized_word)
);

create index if not exists player_word_collection_month_idx
    on public.player_word_collection(user_id, first_played_at);

alter table public.player_word_collection enable row level security;
drop policy if exists player_word_collection_owner_read on public.player_word_collection;
create policy player_word_collection_owner_read on public.player_word_collection
    for select using (auth.uid() = user_id);
revoke insert, update, delete on public.player_word_collection
    from anon, authenticated;

-- ---------------------------------------------------------------------
-- 3) seasonal_themes + seasonal_theme_words
-- ---------------------------------------------------------------------
create table if not exists public.seasonal_themes (
    id bigserial primary key,
    name_tr text not null,
    name_en text not null,
    enabled boolean not null default false,
    starts_at timestamptz,
    ends_at timestamptz,
    -- Score multiplier (default 2x). Bonus applied = base * (multiplier - 1).
    multiplier int not null default 2 check (multiplier between 1 and 5),
    created_at timestamptz not null default now()
);

create table if not exists public.seasonal_theme_words (
    theme_id bigint not null references public.seasonal_themes(id) on delete cascade,
    language text not null check (language in ('tr', 'en')),
    normalized_word text not null,
    primary key (theme_id, language, normalized_word)
);

create index if not exists seasonal_theme_words_lang_word_idx
    on public.seasonal_theme_words(language, normalized_word);

-- Anyone can read active themes (for the "SEASONAL" badge in the app).
alter table public.seasonal_themes enable row level security;
drop policy if exists seasonal_themes_read on public.seasonal_themes;
create policy seasonal_themes_read on public.seasonal_themes
    for select using (true);

alter table public.seasonal_theme_words enable row level security;
drop policy if exists seasonal_theme_words_read on public.seasonal_theme_words;
create policy seasonal_theme_words_read on public.seasonal_theme_words
    for select using (true);

-- Writes go through admin RPCs or the SQL editor.
revoke insert, update, delete on public.seasonal_themes
    from anon, authenticated;
revoke insert, update, delete on public.seasonal_theme_words
    from anon, authenticated;

-- ---------------------------------------------------------------------
-- Helpers
-- ---------------------------------------------------------------------

-- Monday of the given date (ISO week start).
create or replace function public.sonharf_g44_week_start(p_day date)
returns date
language sql
immutable
as $$
    select p_day - ((extract(isodow from p_day)::int - 1))
$$;

-- Currently active seasonal theme (first row when many).
create or replace function public.sonharf_g44_active_theme()
returns public.seasonal_themes
language sql
stable
as $$
    select *
    from public.seasonal_themes t
    where t.enabled
      and (t.starts_at is null or t.starts_at <= now())
      and (t.ends_at is null or t.ends_at > now())
    order by t.id
    limit 1;
$$;

grant execute on function public.sonharf_g44_active_theme() to authenticated;

-- ---------------------------------------------------------------------
-- Trigger: AFTER INSERT game_words. Populates collection + applies
-- seasonal bonus on top of the base score. Skips bot inserts (bot words
-- do not belong to a human's collection; still gets seasonal bonus so
-- the "bot da aynı kurallarla" rule from G4.1 keeps holding).
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g44_after_word_insert()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    r public.game_rooms;
    theme public.seasonal_themes;
    v_lang text;
    v_bonus int := 0;
    v_theme_hit boolean := false;
begin
    select * into r from public.game_rooms where id = new.room_id;
    if r.id is null then return new; end if;
    v_lang := r.language;

    -- 1) Word collection (humans only).
    if not new.is_bot and new.player_id is not null then
        insert into public.player_word_collection
            (user_id, language, normalized_word)
        values (new.player_id, v_lang, new.normalized_word)
        on conflict do nothing;
    end if;

    -- 2) Seasonal bonus. Base score is applied by the caller function;
    --    the bonus here brings the total up toward (base * multiplier).
    theme := public.sonharf_g44_active_theme();
    if theme.id is not null then
        select true into v_theme_hit
        from public.seasonal_theme_words w
        where w.theme_id = theme.id
          and w.language = v_lang
          and w.normalized_word = new.normalized_word;

        if v_theme_hit then
            -- Base valid word = 3 (per multilang_scoring_social submit_word).
            -- Bonus adds (multiplier - 1) * 3 so total becomes 3 * multiplier.
            v_bonus := (coalesce(theme.multiplier, 2) - 1) * 3;
            if v_bonus > 0 then
                if new.is_bot then
                    update public.game_rooms
                       set guest_score = guest_score + v_bonus
                     where id = r.id;
                elsif new.player_id = r.host_id then
                    update public.game_rooms
                       set host_score = host_score + v_bonus
                     where id = r.id;
                else
                    update public.game_rooms
                       set guest_score = guest_score + v_bonus
                     where id = r.id;
                end if;
            end if;
        end if;
    end if;

    return new;
end;
$$;

drop trigger if exists game_words_g44_after_insert on public.game_words;
create trigger game_words_g44_after_insert
    after insert on public.game_words
    for each row execute function public.sonharf_g44_after_word_insert();

-- ---------------------------------------------------------------------
-- Streak update. Runs when a room transitions to a finished state for
-- either participant. Uses Europe/Istanbul so streak day aligns with the
-- rest of the retention systems (daily quests, etc.).
-- ---------------------------------------------------------------------
create or replace function public.sonharf_g44_touch_streak_for(p_user uuid)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_row public.player_daily_streaks;
    v_gap int;
    v_new_streak int;
    v_week_start date := public.sonharf_g44_week_start(v_today);
    v_grace_used int;
begin
    if p_user is null then return; end if;

    select * into v_row from public.player_daily_streaks where user_id = p_user for update;

    if not found then
        insert into public.player_daily_streaks
            (user_id, current_streak, longest_streak, last_played_day,
             grace_week_started, grace_used_week, updated_at)
        values (p_user, 1, 1, v_today, v_week_start, 0, now());
        return;
    end if;

    if v_row.last_played_day = v_today then
        -- Already counted today; just refresh updated_at.
        update public.player_daily_streaks
           set updated_at = now()
         where user_id = p_user;
        return;
    end if;

    v_gap := v_today - v_row.last_played_day;
    v_grace_used := case
        when v_row.grace_week_started is null or v_row.grace_week_started <> v_week_start
            then 0
        else v_row.grace_used_week
    end;

    if v_gap = 1 then
        v_new_streak := v_row.current_streak + 1;
    elsif v_gap = 2 and v_grace_used < 1 then
        -- One missed day is forgiven per ISO week (grace).
        v_new_streak := v_row.current_streak + 1;
        v_grace_used := 1;
    else
        -- Break: start a fresh streak of 1. No penalty.
        v_new_streak := 1;
        v_grace_used := 0;
    end if;

    update public.player_daily_streaks
       set current_streak = v_new_streak,
           longest_streak = greatest(longest_streak, v_new_streak),
           last_played_day = v_today,
           grace_week_started = v_week_start,
           grace_used_week = v_grace_used,
           updated_at = now()
     where user_id = p_user;
end;
$$;

grant execute on function public.sonharf_g44_touch_streak_for(uuid) to service_role;

create or replace function public.sonharf_g44_room_finish_streak()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if new.status = 'finished' and (old.status is null or old.status <> 'finished') then
        if new.host_id is not null then
            perform public.sonharf_g44_touch_streak_for(new.host_id);
        end if;
        -- Guest may be null for bot matches.
        if new.guest_id is not null then
            perform public.sonharf_g44_touch_streak_for(new.guest_id);
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists game_rooms_g44_finish_streak on public.game_rooms;
create trigger game_rooms_g44_finish_streak
    after update on public.game_rooms
    for each row execute function public.sonharf_g44_room_finish_streak();

-- ---------------------------------------------------------------------
-- Read RPCs the client calls to render the retention card.
-- ---------------------------------------------------------------------
create or replace function public.get_player_daily_streak()
returns table (
    current_streak int,
    longest_streak int,
    last_played_day date,
    grace_used_week int,
    played_today boolean,
    grace_available boolean
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_row public.player_daily_streaks;
    v_week_start date := public.sonharf_g44_week_start(v_today);
begin
    if v_user is null then raise exception 'not_authenticated'; end if;

    select * into v_row from public.player_daily_streaks where user_id = v_user;
    if not found then
        return query select 0, 0, null::date, 0,
            false, true;
        return;
    end if;

    return query select
        v_row.current_streak,
        v_row.longest_streak,
        v_row.last_played_day,
        case when v_row.grace_week_started = v_week_start then v_row.grace_used_week else 0 end,
        v_row.last_played_day = v_today,
        case when v_row.grace_week_started = v_week_start then v_row.grace_used_week < 1 else true end;
end;
$$;

grant execute on function public.get_player_daily_streak() to authenticated;

create or replace function public.get_player_word_collection()
returns table (
    total_words bigint,
    this_month bigint,
    tr_words bigint,
    en_words bigint
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_month_start timestamptz;
begin
    if v_user is null then raise exception 'not_authenticated'; end if;
    v_month_start := date_trunc('month', now() at time zone 'Europe/Istanbul')
                     at time zone 'Europe/Istanbul';
    return query
        select
            count(*)::bigint,
            count(*) filter (where first_played_at >= v_month_start)::bigint,
            count(*) filter (where language = 'tr')::bigint,
            count(*) filter (where language = 'en')::bigint
        from public.player_word_collection
        where user_id = v_user;
end;
$$;

grant execute on function public.get_player_word_collection() to authenticated;

-- Active seasonal theme banner for the app.
create or replace function public.get_active_seasonal_theme()
returns table (
    id bigint,
    name_tr text,
    name_en text,
    multiplier int,
    word_count bigint
)
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    theme public.seasonal_themes;
begin
    theme := public.sonharf_g44_active_theme();
    if theme.id is null then
        return;
    end if;
    return query
        select theme.id, theme.name_tr, theme.name_en, theme.multiplier,
               (select count(*) from public.seasonal_theme_words w where w.theme_id = theme.id)::bigint;
end;
$$;

grant execute on function public.get_active_seasonal_theme() to authenticated;

select pg_notify('pgrst', 'reload schema');
