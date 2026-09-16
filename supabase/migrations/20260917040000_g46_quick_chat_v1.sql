-- G4.6 Sosyal — MAÇ İÇİ HIZLI SOHBET + SUSTUR + rate-limit.
--
-- Mevcut altyapı (KORUNUR):
--   public.chat_messages  (room-based chat body, existing RLS)
--   public.user_blocks    (kalıcı engelleme — arkadaş listesi/davetler
--                          zaten kullanıyor)
--   public.player_reports (raporlama)
--   public.friendships    (arkadaş listesi)
--
-- Bu migration EKLER, hiçbir mevcut tabloyu yeniden yazmaz.

set search_path = public, pg_temp;

-- ---------------------------------------------------------------------
-- 1) chat_messages'e hızlı mesaj etiketi ekle.
--    - message_key: 6 hazır TXT anahtarından biri veya "emoji:👏".
--      Serbest yazı için null (Kuşatma serbest yazı sonraki tur).
--    - Body zaten var; hazır mesajlar için client body'ye TR karşılığını
--      koyar, key ise anahtar tutar (i18n istemcide).
-- ---------------------------------------------------------------------
alter table public.chat_messages
    add column if not exists message_key text;

create index if not exists chat_messages_room_key_idx
    on public.chat_messages(room_id, message_key)
    where message_key is not null;

-- ---------------------------------------------------------------------
-- 2) match_mutes: bir maçta bir oyuncuyu susturmak.
--    Rakibin gönderdiği mesajlar bu satır varken UI'da görünmez;
--    sunucu tarafında da rate-limit kontrolünden önce reddederiz.
-- ---------------------------------------------------------------------
create table if not exists public.match_mutes (
    muter_id uuid not null references auth.users(id) on delete cascade,
    muted_id uuid not null references auth.users(id) on delete cascade,
    room_id uuid not null references public.game_rooms(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (muter_id, muted_id, room_id),
    check (muter_id <> muted_id)
);

alter table public.match_mutes enable row level security;
drop policy if exists match_mutes_owner_read on public.match_mutes;
create policy match_mutes_owner_read on public.match_mutes
    for select using (auth.uid() = muter_id);
revoke insert, update, delete on public.match_mutes from anon, authenticated;

-- ---------------------------------------------------------------------
-- 3) chat_rate_limits: son 10 saniyede kaç mesaj gönderildi.
--    Ring-buffer değil; her mesaj için bir kayıt (rate check window).
--    Trigger + eski satır temizleme rate-limit fonksiyonunun içinde.
-- ---------------------------------------------------------------------
create table if not exists public.chat_rate_limit_events (
    id bigserial primary key,
    sender_id uuid not null references auth.users(id) on delete cascade,
    sent_at timestamptz not null default now()
);

create index if not exists chat_rate_limit_events_sender_time_idx
    on public.chat_rate_limit_events(sender_id, sent_at desc);

alter table public.chat_rate_limit_events enable row level security;
revoke all on public.chat_rate_limit_events from anon, authenticated;

-- Retention housekeeping: rows older than 60s are useless for the 10s
-- window and can be pruned. Server hourly job or the send RPC itself
-- deletes them opportunistically.
create or replace function public.sonharf_g46_prune_rate_events(p_user uuid)
returns void
language sql
security definer
set search_path = public, pg_temp
as $$
    delete from public.chat_rate_limit_events
    where sender_id = p_user
      and sent_at < now() - interval '60 seconds';
$$;

-- ---------------------------------------------------------------------
-- 4) send_quick_chat: hazır mesaj / emoji gönderme.
--    Kontroller:
--      - Katılımcı mı? (host/guest)
--      - Rakip bloklamış mı? (chat gitmemeli)
--      - Rate limit: 10sn içinde en fazla 5 mesaj
--      - Maç başına 8 mesaj sınırı (chat_messages count'a bakılır)
--    Yazı serbest DEĞİL; anahtar allowlist ile eşleşmeli.
-- ---------------------------------------------------------------------
create or replace function public.send_quick_chat(
    p_room_id uuid,
    p_message_key text
)
returns bigint
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    r public.game_rooms;
    v_opponent uuid;
    v_recent_count int;
    v_room_count int;
    v_body text;
    v_id bigint;
begin
    if v_user is null then raise exception 'not_authenticated'; end if;

    -- Allowlist for quick messages + the 3 emojis from G3.6.
    -- Body is the Turkish surface text; client resolves EN itself.
    v_body := case p_message_key
        when 'good_luck' then 'İyi şanslar!'
        when 'nice_word' then 'Güzel kelime!'
        when 'wow'       then 'Vay be!'
        when 'close'     then 'Az kaldı!'
        when 'gg'        then 'İyi oyundu!'
        when 'again'     then 'Tekrar?'
        when 'emoji:clap' then '👏'
        when 'emoji:wow'  then '😮'
        when 'emoji:fire' then '🔥'
        else null
    end;
    if v_body is null then raise exception 'invalid_message_key'; end if;

    select * into r from public.game_rooms where id = p_room_id;
    if r.id is null then raise exception 'room_not_found'; end if;
    if v_user <> r.host_id and v_user <> r.guest_id then
        raise exception 'not_participant';
    end if;
    if r.status not in ('playing', 'final', 'sudden_death', 'quiz', 'finished') then
        raise exception 'room_not_open';
    end if;

    v_opponent := case when v_user = r.host_id then r.guest_id else r.host_id end;

    -- If either side has blocked the other, silently drop; no error so
    -- the sender's UI doesn't leak that they are blocked.
    if v_opponent is not null and exists (
        select 1 from public.user_blocks b
        where (b.blocker_id = v_opponent and b.blocked_id = v_user)
           or (b.blocker_id = v_user and b.blocked_id = v_opponent)
    ) then
        return 0;
    end if;

    -- Rate limit: max 5 in the last 10 seconds.
    perform public.sonharf_g46_prune_rate_events(v_user);
    select count(*) into v_recent_count
    from public.chat_rate_limit_events
    where sender_id = v_user
      and sent_at >= now() - interval '10 seconds';
    if v_recent_count >= 5 then raise exception 'chat_rate_limit'; end if;

    -- Per-room 8-message cap (across both players' quick messages).
    select count(*) into v_room_count
    from public.chat_messages
    where room_id = p_room_id
      and sender_id = v_user
      and message_key is not null;
    if v_room_count >= 8 then raise exception 'chat_room_limit'; end if;

    insert into public.chat_messages
        (room_id, sender_id, body, message_key)
    values (p_room_id, v_user, v_body, p_message_key)
    returning id into v_id;

    insert into public.chat_rate_limit_events (sender_id) values (v_user);

    return v_id;
end;
$$;

grant execute on function public.send_quick_chat(uuid, text) to authenticated;

-- ---------------------------------------------------------------------
-- 5) mute_in_match / unmute_in_match: tek dokunuşla sustur.
--    Kalıcı engelleme değil; sadece o maç boyunca sesini keser.
-- ---------------------------------------------------------------------
create or replace function public.mute_in_match(
    p_room_id uuid,
    p_target_id uuid
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    r public.game_rooms;
begin
    if v_user is null then raise exception 'not_authenticated'; end if;
    if p_target_id is null or p_target_id = v_user then
        raise exception 'invalid_target';
    end if;

    select * into r from public.game_rooms where id = p_room_id;
    if r.id is null then raise exception 'room_not_found'; end if;
    if v_user <> r.host_id and v_user <> r.guest_id then
        raise exception 'not_participant';
    end if;

    insert into public.match_mutes (muter_id, muted_id, room_id)
    values (v_user, p_target_id, p_room_id)
    on conflict do nothing;
end;
$$;

grant execute on function public.mute_in_match(uuid, uuid) to authenticated;

create or replace function public.unmute_in_match(
    p_room_id uuid,
    p_target_id uuid
)
returns void
language sql
security definer
set search_path = public, pg_temp
as $$
    delete from public.match_mutes
    where muter_id = auth.uid()
      and muted_id = p_target_id
      and room_id = p_room_id;
$$;

grant execute on function public.unmute_in_match(uuid, uuid) to authenticated;

-- ---------------------------------------------------------------------
-- 6) is_muted_in_match: client checks whether the opponent's live
--    message should be shown as a bubble.
-- ---------------------------------------------------------------------
create or replace function public.is_muted_in_match(
    p_room_id uuid,
    p_target_id uuid
)
returns boolean
language sql
security definer
set search_path = public, pg_temp
as $$
    select exists (
        select 1 from public.match_mutes
        where muter_id = auth.uid()
          and muted_id = p_target_id
          and room_id = p_room_id
    );
$$;

grant execute on function public.is_muted_in_match(uuid, uuid) to authenticated;

select pg_notify('pgrst', 'reload schema');
