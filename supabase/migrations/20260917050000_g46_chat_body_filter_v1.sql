-- G4.6 sohbet güvenlik filtresi:
--   Serbest yazılı sohbet mesajlarında (message_key IS NULL) link ve
--   telefon numarası paylaşımı engellenir. Hazır mesajlar
--   (send_quick_chat üzerinden gelenler) allowlist gövdesine sahip
--   olduğu için filtreden geçmez.
--
-- Bu bir BEFORE INSERT trigger'ı; şüpheli metni tespit ettiğinde
-- 'chat_body_forbidden' hatası atar. UI hatayı yakalar ve
-- "Bağlantı/telefon paylaşımı engellendi" gibi bir metin gösterir.

set search_path = public, pg_temp;

create or replace function public.sonharf_g46_chat_body_check()
returns trigger
language plpgsql
as $$
declare
    v_body text;
begin
    -- Quick messages carry a server-controlled body; skip.
    if new.message_key is not null then
        return new;
    end if;

    v_body := lower(coalesce(new.body, ''));

    -- URL patterns: http/https/www + explicit dot-com etc.
    -- Not exhaustive (nothing regex-based is); catches the common cases.
    if v_body ~ 'https?://'
        or v_body ~ 'www\.[a-z0-9]'
        or v_body ~ '\y[a-z0-9-]+\.(com|net|org|io|tr|co|xyz|app|dev|me|link|gg|tv|shop|store)\y'
    then
        raise exception 'chat_body_forbidden' using errcode = 'P0001';
    end if;

    -- Phone patterns:
    --   * 10+ consecutive digits (with optional +/space/dash),
    --   * Turkish mobile format (0 5xx xxx xx xx or +90 5xx ...),
    --   * digits stripped of separators >= 10.
    if regexp_replace(v_body, '[^0-9]', '', 'g') ~ '[0-9]{10,}' then
        raise exception 'chat_body_forbidden' using errcode = 'P0001';
    end if;

    return new;
end;
$$;

drop trigger if exists chat_messages_g46_body_check on public.chat_messages;
create trigger chat_messages_g46_body_check
    before insert on public.chat_messages
    for each row execute function public.sonharf_g46_chat_body_check();

select pg_notify('pgrst', 'reload schema');
