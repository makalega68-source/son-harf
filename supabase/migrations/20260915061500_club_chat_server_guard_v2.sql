-- Kelime Kuşatması Master GDD v3.0
-- Server-authoritative club chat anti-spam and abuse guard.
-- Existing RLS remains authoritative for visibility and membership; this trigger closes the gap
-- where a modified client could bypass the local cooldown and submit bursts directly to PostgREST.
-- It also enforces the existing report_player -> profiles.chat_suspended_until moderation system.

create index if not exists club_messages_club_sender_created_idx
  on public.club_messages (club_id, sender_id, created_at desc);

create or replace function private.guard_club_message_insert_v2()
returns trigger
language plpgsql
security definer
set search_path = 'pg_catalog', 'public', 'private', 'pg_temp'
as $$
declare
  v_uid uuid := auth.uid();
  v_body text := btrim(coalesce(new.body, ''));
  v_last_created_at timestamptz;
  v_recent_count integer;
begin
  if v_uid is null then
    raise exception 'club_chat_unauthorized';
  end if;

  if new.sender_id is distinct from v_uid then
    raise exception 'club_chat_sender_mismatch';
  end if;

  if char_length(v_body) not between 1 and 300 then
    raise exception 'club_chat_invalid_length';
  end if;

  if not exists (
    select 1
    from public.club_members cm
    where cm.club_id = new.club_id
      and cm.user_id = v_uid
  ) then
    raise exception 'club_membership_required';
  end if;

  -- Reuse the existing global chat moderation penalty applied by public.report_player().
  if exists (
    select 1
    from public.profiles p
    where p.id = v_uid
      and p.chat_suspended_until is not null
      and p.chat_suspended_until > clock_timestamp()
  ) then
    raise exception 'chat_suspended';
  end if;

  -- Serialize one member's writes inside one club so parallel requests cannot race the cooldown.
  perform pg_advisory_xact_lock(hashtext(new.club_id::text), hashtext(v_uid::text));

  select max(m.created_at)
  into v_last_created_at
  from public.club_messages m
  where m.club_id = new.club_id
    and m.sender_id = v_uid;

  if v_last_created_at is not null
     and clock_timestamp() - v_last_created_at < interval '1500 milliseconds' then
    raise exception 'club_chat_rate_limited';
  end if;

  select count(*)
  into v_recent_count
  from public.club_messages m
  where m.club_id = new.club_id
    and m.sender_id = v_uid
    and m.created_at >= clock_timestamp() - interval '30 seconds';

  if v_recent_count >= 8 then
    raise exception 'club_chat_rate_limited';
  end if;

  if exists (
    select 1
    from public.club_messages m
    where m.club_id = new.club_id
      and m.sender_id = v_uid
      and m.created_at >= clock_timestamp() - interval '30 seconds'
      and lower(btrim(m.body)) = lower(v_body)
  ) then
    raise exception 'club_chat_duplicate_message';
  end if;

  -- Normalize server-side so whitespace/timestamp manipulation cannot bypass moderation windows.
  new.body := v_body;
  new.created_at := clock_timestamp();
  return new;
end
$$;

revoke all on function private.guard_club_message_insert_v2()
  from public, anon, authenticated;

drop trigger if exists club_messages_server_guard_v2 on public.club_messages;
create trigger club_messages_server_guard_v2
before insert on public.club_messages
for each row
execute function private.guard_club_message_insert_v2();
