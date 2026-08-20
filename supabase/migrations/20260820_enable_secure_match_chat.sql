drop policy if exists "chat participant insert" on public.chat_messages;
drop policy if exists "chat participants read" on public.chat_messages;
drop policy if exists chat_messages_select_participants on public.chat_messages;
drop policy if exists chat_messages_insert_sender on public.chat_messages;

create policy chat_messages_select_participants
on public.chat_messages
for select
to authenticated
using (
  exists (
    select 1
    from public.game_rooms r
    where r.id = chat_messages.room_id
      and auth.uid() in (r.host_id, r.guest_id)
  )
);

create policy chat_messages_insert_sender
on public.chat_messages
for insert
to authenticated
with check (
  sender_id = auth.uid()
  and char_length(btrim(body)) between 1 and 300
  and exists (
    select 1
    from public.game_rooms r
    where r.id = chat_messages.room_id
      and auth.uid() in (r.host_id, r.guest_id)
      and coalesce(r.is_bot, false) = false
      and not exists (
        select 1 from public.user_blocks b
        where (b.blocker_id = r.host_id and b.blocked_id = r.guest_id)
           or (b.blocker_id = r.guest_id and b.blocked_id = r.host_id)
      )
  )
  and exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and coalesce(p.allow_match_chat, true) = true
      and (p.chat_suspended_until is null or p.chat_suspended_until <= now())
  )
);

grant select, insert on public.chat_messages to authenticated;

do $$
declare seq regclass;
begin
  seq := pg_get_serial_sequence('public.chat_messages','id')::regclass;
  if seq is not null then
    execute format('grant usage, select on sequence %s to authenticated', seq);
  end if;
end $$;
