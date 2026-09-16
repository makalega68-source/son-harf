-- G4.3 Daily Quests (3 games) — server skeleton.
--
-- Each player receives exactly three quests per Turkish-calendar day:
-- one from Son Harf, one from Kelime Kuşatması, one from Kelime Yolu.
-- Progress is counted server-side (RPCs below) so no client can falsify
-- rewards. Rewards are diamond drops per quest; completing all three
-- unlocks the daily chest (handled by the reward center).

set search_path = public, pg_temp;

-- Quest pool. Each row is one candidate quest. `game` is 'son_harf',
-- 'kusatma' or 'kelime_yolu'. `metric` names the counter the client
-- pings via record_quest_progress. `target` is how many units to hit.
create table if not exists public.daily_quest_pool (
    id bigserial primary key,
    game text not null check (game in ('son_harf', 'kusatma', 'kelime_yolu')),
    metric text not null,
    target integer not null check (target > 0),
    -- Turkish and English labels shown in the UI.
    title_tr text not null,
    title_en text not null,
    reward_diamonds integer not null default 3 check (reward_diamonds > 0),
    active boolean not null default true
);

create index if not exists daily_quest_pool_active_by_game_idx
    on public.daily_quest_pool(game, active);

-- Per-player daily assignment. `day` is a Turkish calendar date
-- (Europe/Istanbul) so midnight rollover is aligned to the spec.
create table if not exists public.daily_quests (
    id bigserial primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    day date not null,
    game text not null check (game in ('son_harf', 'kusatma', 'kelime_yolu')),
    pool_id bigint not null references public.daily_quest_pool(id),
    progress integer not null default 0 check (progress >= 0),
    completed boolean not null default false,
    reward_claimed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (user_id, day, game)
);

create index if not exists daily_quests_user_day_idx
    on public.daily_quests(user_id, day);

alter table public.daily_quests enable row level security;

drop policy if exists daily_quests_owner_select on public.daily_quests;
create policy daily_quests_owner_select on public.daily_quests
    for select
    using (auth.uid() = user_id);

-- Only the RPCs below (security definer) mutate daily_quests;
-- players cannot insert / update directly.
revoke insert, update, delete on public.daily_quests from anon, authenticated;

-- Everyone can read the pool for label lookups.
alter table public.daily_quest_pool enable row level security;

drop policy if exists daily_quest_pool_read on public.daily_quest_pool;
create policy daily_quest_pool_read on public.daily_quest_pool
    for select
    using (true);

revoke insert, update, delete on public.daily_quest_pool from anon, authenticated;

-- Seed a small pool from the spec examples. Rows are additive:
-- inserting again with the same title is a no-op.
insert into public.daily_quest_pool (game, metric, target, title_tr, title_en, reward_diamonds)
values
    ('son_harf', 'wins', 3, '3 maç kazan', 'Win 3 matches', 4),
    ('son_harf', 'long_words_7', 2, '7+ harfli 2 kelime yaz', 'Play 2 words of 7+ letters', 4),
    ('son_harf', 'streak_5', 1, '5 seri yap', 'Reach a streak of 5', 4),
    ('son_harf', 'hard_letter_end', 3, 'Zor harfle biten 3 kelime bırak', 'End 3 words on a hard letter', 4),

    ('kusatma', 'cells_captured', 10, 'Bir maçta 10 hücre ele geçir', 'Capture 10 cells in one match', 5),
    ('kusatma', 'castle_taken', 1, '1 kale al', 'Take 1 castle', 5),
    ('kusatma', 'big_move_30', 1, '30+ puanlık hamle yap', 'Play a 30+ point move', 5),
    ('kusatma', 'matches_played', 2, '2 maç oyna', 'Play 2 matches', 4),

    ('kelime_yolu', 'levels_completed', 2, '2 bölüm bitir', 'Finish 2 levels', 4),
    ('kelime_yolu', 'hintless_level', 1, 'İpucusuz 1 bölüm bitir', 'Finish 1 level without hints', 6),
    ('kelime_yolu', 'consecutive_correct', 5, '5 ardışık doğru cevap', 'Get 5 correct answers in a row', 4)
on conflict do nothing;

-- Return today's assignment; issue it if missing.
-- Uses Europe/Istanbul so midnight rollover matches G4.3.
create or replace function public.get_or_issue_daily_quests()
returns table (
    quest_id bigint,
    game text,
    metric text,
    target integer,
    progress integer,
    completed boolean,
    reward_claimed boolean,
    title_tr text,
    title_en text,
    reward_diamonds integer
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_game text;
    v_pool record;
    v_last_ids bigint[];
begin
    if v_user is null then
        raise exception 'not_authenticated';
    end if;

    -- For each game, if the player has no assignment for today, pick one
    -- from the pool. We avoid the player's most recent quest for the same
    -- game so the same quest does not appear back-to-back.
    foreach v_game in array array['son_harf', 'kusatma', 'kelime_yolu'] loop
        if not exists (
            select 1 from public.daily_quests dq
            where dq.user_id = v_user
              and dq.day = v_today
              and dq.game = v_game
        ) then
            select array_agg(dq.pool_id) into v_last_ids
            from public.daily_quests dq
            where dq.user_id = v_user
              and dq.game = v_game
              and dq.day >= v_today - interval '1 day'
              and dq.day < v_today;

            select * into v_pool
            from public.daily_quest_pool
            where active = true
              and game = v_game
              and (v_last_ids is null or not (id = any(v_last_ids)))
            order by random()
            limit 1;

            if v_pool.id is not null then
                insert into public.daily_quests (user_id, day, game, pool_id)
                values (v_user, v_today, v_game, v_pool.id);
            end if;
        end if;
    end loop;

    return query
        select dq.id, dq.game, p.metric, p.target,
               dq.progress, dq.completed, dq.reward_claimed,
               p.title_tr, p.title_en, p.reward_diamonds
        from public.daily_quests dq
        join public.daily_quest_pool p on p.id = dq.pool_id
        where dq.user_id = v_user
          and dq.day = v_today
        order by case dq.game
                     when 'son_harf' then 1
                     when 'kusatma' then 2
                     when 'kelime_yolu' then 3
                 end;
end;
$$;

grant execute on function public.get_or_issue_daily_quests() to authenticated;

-- Server-side progress counter. Idempotent per hamle: same key won't
-- double-count. The client passes an idempotency key that is unique
-- per event (e.g. "match:<uuid>:word:<idx>").
create table if not exists public.daily_quest_progress_events (
    id bigserial primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    quest_id bigint not null references public.daily_quests(id) on delete cascade,
    idempotency_key text not null,
    delta integer not null check (delta > 0),
    recorded_at timestamptz not null default now(),
    unique (user_id, quest_id, idempotency_key)
);

alter table public.daily_quest_progress_events enable row level security;
revoke all on public.daily_quest_progress_events from anon, authenticated;

create or replace function public.record_quest_progress(
    p_game text,
    p_metric text,
    p_delta integer,
    p_key text
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_today date := (now() at time zone 'Europe/Istanbul')::date;
    v_quest record;
    v_new_progress integer;
begin
    if v_user is null then
        raise exception 'not_authenticated';
    end if;
    if p_delta is null or p_delta <= 0 then
        return;
    end if;

    -- Only apply if today's quest for this game+metric matches. If the
    -- player's issued quest uses a different metric today, ignore.
    select dq.*, p.metric as pool_metric, p.target as pool_target
    into v_quest
    from public.daily_quests dq
    join public.daily_quest_pool p on p.id = dq.pool_id
    where dq.user_id = v_user
      and dq.day = v_today
      and dq.game = p_game
      and p.metric = p_metric
    for update;

    if not found then return; end if;
    if v_quest.completed then return; end if;

    -- Insert the event; a duplicate key is a no-op and does not raise.
    begin
        insert into public.daily_quest_progress_events
            (user_id, quest_id, idempotency_key, delta)
        values (v_user, v_quest.id, p_key, p_delta);
    exception when unique_violation then
        return;
    end;

    v_new_progress := least(v_quest.progress + p_delta, v_quest.pool_target);
    update public.daily_quests
    set progress = v_new_progress,
        completed = v_new_progress >= v_quest.pool_target,
        updated_at = now()
    where id = v_quest.id;
end;
$$;

grant execute on function public.record_quest_progress(text, text, integer, text) to authenticated;

select pg_notify('pgrst', 'reload schema');
