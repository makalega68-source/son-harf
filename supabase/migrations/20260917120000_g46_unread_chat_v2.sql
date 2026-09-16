-- G4.6 unread chat state — server-authoritative read cursor.
-- Tracks one monotonic chat_messages.id cursor per authenticated user.
-- No client can write another user's cursor directly.

set search_path = public, pg_temp;

create table if not exists public.chat_read_state (
    user_id uuid primary key references auth.users(id) on delete cascade,
    last_read_message_id bigint not null default 0 check (last_read_message_id >= 0),
    updated_at timestamptz not null default now()
);

alter table public.chat_read_state enable row level security;

drop policy if exists chat_read_state_owner_read on public.chat_read_state;
create policy chat_read_state_owner_read on public.chat_read_state
    for select using (auth.uid() = user_id);

revoke insert, update, delete on public.chat_read_state from anon, authenticated;
grant select on public.chat_read_state to authenticated;

create or replace function public.get_my_unread_chat_count_v1()
returns int
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_last_read bigint := 0;
    v_count int := 0;
begin
    if v_user is null then
        raise exception 'not_authenticated';
    end if;

    select coalesce(s.last_read_message_id, 0)
      into v_last_read
      from public.chat_read_state s
     where s.user_id = v_user;

    select count(*)::int
      into v_count
      from public.chat_messages m
      join public.game_rooms r on r.id = m.room_id
     where (r.host_id = v_user or r.guest_id = v_user)
       and m.sender_id <> v_user
       and m.id > coalesce(v_last_read, 0)
       and not exists (
            select 1
              from public.match_mutes mm
             where mm.muter_id = v_user
               and mm.muted_id = m.sender_id
               and mm.room_id = m.room_id
       )
       and not exists (
            select 1
              from public.user_blocks b
             where (b.blocker_id = v_user and b.blocked_id = m.sender_id)
                or (b.blocker_id = m.sender_id and b.blocked_id = v_user)
       );

    return least(v_count, 999);
end;
$$;

revoke all on function public.get_my_unread_chat_count_v1() from public, anon;
grant execute on function public.get_my_unread_chat_count_v1() to authenticated;

create or replace function public.mark_my_chat_read_v1()
returns bigint
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user uuid := auth.uid();
    v_max_id bigint := 0;
begin
    if v_user is null then
        raise exception 'not_authenticated';
    end if;

    select coalesce(max(m.id), 0)
      into v_max_id
      from public.chat_messages m
      join public.game_rooms r on r.id = m.room_id
     where r.host_id = v_user or r.guest_id = v_user;

    insert into public.chat_read_state(user_id, last_read_message_id, updated_at)
    values (v_user, v_max_id, now())
    on conflict (user_id) do update
       set last_read_message_id = greatest(public.chat_read_state.last_read_message_id, excluded.last_read_message_id),
           updated_at = now();

    return v_max_id;
end;
$$;

revoke all on function public.mark_my_chat_read_v1() from public, anon;
grant execute on function public.mark_my_chat_read_v1() to authenticated;

select pg_notify('pgrst', 'reload schema');
