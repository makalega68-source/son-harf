-- Offline-capable friend invitations and notification outbox.
-- Online friends receive an in-app invite; offline friends keep a pending invite and
-- can later be delivered by a push worker without changing game logic.

create table if not exists public.push_tokens (
  user_id uuid not null references public.profiles(id) on delete cascade,
  token text not null,
  platform text not null default 'android',
  updated_at timestamptz not null default now(),
  primary key(user_id,token)
);
alter table public.push_tokens enable row level security;
create policy push_tokens_self_all on public.push_tokens for all to authenticated
using(user_id=auth.uid()) with check(user_id=auth.uid());

create table if not exists public.notification_outbox (
  id bigint generated always as identity primary key,
  user_id uuid not null references public.profiles(id) on delete cascade,
  kind text not null,
  payload jsonb not null default '{}'::jsonb,
  delivered_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists notification_outbox_pending_idx on public.notification_outbox(user_id,created_at) where delivered_at is null;
alter table public.notification_outbox enable row level security;
create policy notification_outbox_self_read on public.notification_outbox for select to authenticated using(user_id=auth.uid());

create or replace function public.invite_friend_to_game(p_friend_id uuid, p_language text)
returns public.game_invites
language plpgsql security definer set search_path=public as $$
declare inv public.game_invites; f public.friendships; p public.profiles;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  if p_language not in ('tr','en') then raise exception 'invalid_language'; end if;
  select * into f from public.friendships
   where user_id=least(auth.uid(),p_friend_id) and friend_id=greatest(auth.uid(),p_friend_id) and status='accepted';
  if f.user_id is null then raise exception 'not_friends'; end if;
  if exists(select 1 from public.user_blocks where (blocker_id=auth.uid() and blocked_id=p_friend_id) or (blocker_id=p_friend_id and blocked_id=auth.uid())) then
    raise exception 'blocked_relationship';
  end if;
  select * into p from public.profiles where id=p_friend_id;
  if p.id is null then raise exception 'friend_not_found'; end if;
  if p.presence_status='in_game' then raise exception 'friend_in_game'; end if;

  update public.game_invites set status='expired',responded_at=now()
   where receiver_id=p_friend_id and status='pending' and expires_at<now();

  insert into public.game_invites(sender_id,receiver_id,language,expires_at)
  values(auth.uid(),p_friend_id,p_language,case when p.presence_status='online' then now()+interval '2 minutes' else now()+interval '24 hours' end)
  returning * into inv;

  insert into public.notification_outbox(user_id,kind,payload)
  values(p_friend_id,'game_invite',jsonb_build_object('invite_id',inv.id,'sender_id',auth.uid(),'language',p_language));
  return inv;
end $$;
grant execute on function public.invite_friend_to_game(uuid,text) to authenticated;
