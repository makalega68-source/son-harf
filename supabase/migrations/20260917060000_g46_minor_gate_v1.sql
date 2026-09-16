-- G4.6 — 18 yaş altı serbest sohbet kilidi.
--
-- Kural (TXT): "18 yaş altı olduğu bilinen hesaplarda serbest sohbet
-- KAPALI, sadece hazır mesajlar."
--
-- Tasarım:
--   - profiles.birth_year opsiyonel (nullable). Bilinmiyorsa varsayılan
--     "yetişkin sayılır" (spec yalnızca BİLİNEN minörleri kilitler).
--   - is_minor_user(user_id) helper: birth_year varsa
--     (extract(year from now()) - birth_year) < 18.
--   - chat_messages BEFORE INSERT trigger: serbest yazı (message_key
--     NULL) minör tarafından atılamaz -> 'chat_minor_free_text' hatası.
--     Hazır mesajlar (message_key allowlist) her yaşa serbest.

set search_path = public, pg_temp;

alter table public.profiles
    add column if not exists birth_year int
    check (birth_year is null or (birth_year between 1900 and 2100));

create or replace function public.is_minor_user(p_user uuid)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    -- Returns TRUE only when we KNOW the user is <18 (birth_year set
    -- and age < 18). Unknown birth_year -> FALSE (spec: only "bilinen"
    -- minors are locked).
    select coalesce(
        (select (extract(year from (now() at time zone 'Europe/Istanbul'))::int
                 - p.birth_year) < 18
         from public.profiles p
         where p.id = p_user
           and p.birth_year is not null
         limit 1),
        false
    );
$$;

grant execute on function public.is_minor_user(uuid) to authenticated;

-- Free-text gate for minors. Quick messages (message_key non-null) get
-- through — those are the safe allowlist. The G4.6 body filter trigger
-- from migration 20260917050000 still runs after this one; the two
-- are independent.
create or replace function public.sonharf_g46_minor_free_text_guard()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if new.message_key is not null then
        return new;
    end if;
    if public.is_minor_user(new.sender_id) then
        raise exception 'chat_minor_free_text' using errcode = 'P0001';
    end if;
    return new;
end;
$$;

drop trigger if exists chat_messages_g46_minor_guard on public.chat_messages;
create trigger chat_messages_g46_minor_guard
    before insert on public.chat_messages
    for each row execute function public.sonharf_g46_minor_free_text_guard();

-- Client-usable read: kendi yaş durumu.
create or replace function public.is_minor_self()
returns boolean
language sql
security definer
set search_path = public, pg_temp
as $$
    select public.is_minor_user(auth.uid());
$$;

grant execute on function public.is_minor_self() to authenticated;

select pg_notify('pgrst', 'reload schema');
