-- Chat content expires at 15 days for every client, including older app versions.
-- Restrictive policies preserve existing participant/friend/block checks.
create index if not exists chat_messages_retention_idx on public.chat_messages(created_at);
create index if not exists direct_messages_retention_idx on public.direct_messages(created_at);
create index if not exists club_messages_retention_idx on public.club_messages(created_at);
create index if not exists word_siege_messages_retention_idx on public.word_siege_messages(created_at);

create or replace function public.stamp_chat_created_at_v1()
returns trigger language plpgsql set search_path = public, pg_temp as $$
begin
  new.created_at := now();
  return new;
end;
$$;
revoke all on function public.stamp_chat_created_at_v1() from public, anon, authenticated;

do $$
declare t text;
begin
  foreach t in array array['chat_messages','direct_messages','club_messages','word_siege_messages'] loop
    execute format('drop policy if exists chat_retention_15_days on public.%I',t);
    execute format('create policy chat_retention_15_days on public.%I as restrictive for select to authenticated using (created_at > now() - interval ''15 days'')',t);
    execute format('drop trigger if exists stamp_chat_created_at on public.%I',t);
    execute format('create trigger stamp_chat_created_at before insert on public.%I for each row execute function public.stamp_chat_created_at_v1()',t);
  end loop;
end;
$$;

create or replace function public.expire_chat_and_presence_v1()
returns void language plpgsql security definer set search_path = public, pg_temp as $$
begin
  delete from public.chat_messages where created_at <= now() - interval '15 days';
  delete from public.direct_messages where created_at <= now() - interval '15 days';
  delete from public.club_messages where created_at <= now() - interval '15 days';
  delete from public.word_siege_messages where created_at <= now() - interval '15 days';
  update public.profiles set presence_status = 'offline'
    where presence_status in ('online','in_game')
      and (last_seen_at is null or last_seen_at < now() - interval '90 seconds');
end;
$$;
revoke all on function public.expire_chat_and_presence_v1() from public, anon, authenticated;
select cron.schedule('chat_retention_presence_v1','* * * * *','select public.expire_chat_and_presence_v1()');
select public.expire_chat_and_presence_v1();
