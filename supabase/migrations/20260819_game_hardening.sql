-- Son Harf backend hardening and economy
create table if not exists public.subscriptions (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  product_id text not null,
  status text not null default 'inactive' check (status in ('inactive','active','grace','expired','cancelled')),
  expires_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.player_reports (
  id bigint generated always as identity primary key,
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  reported_id uuid not null references public.profiles(id) on delete cascade,
  room_id uuid references public.game_rooms(id) on delete set null,
  reason text not null check (char_length(reason) between 3 and 200),
  created_at timestamptz not null default now()
);

alter table public.subscriptions enable row level security;
alter table public.player_reports enable row level security;

create policy "subscription self read" on public.subscriptions
for select to authenticated using (user_id = auth.uid());

create policy "report self create" on public.player_reports
for insert to authenticated with check (reporter_id = auth.uid() and reported_id <> auth.uid());

create or replace function public.is_room_participant(p_room_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists(
    select 1 from public.game_rooms
    where id = p_room_id and (host_id = p_user_id or guest_id = p_user_id)
  );
$$;

create or replace function public.join_room_by_code(p_code text)
returns public.game_rooms
language plpgsql
security definer
set search_path = public
as $$
declare
  r public.game_rooms;
begin
  update public.game_rooms
    set guest_id = auth.uid(), status = 'playing', current_player_id = host_id
    where code = upper(trim(p_code)) and status = 'waiting' and guest_id is null and host_id <> auth.uid()
    returning * into r;
  if r.id is null then raise exception 'room_not_available'; end if;
  return r;
end;
$$;

grant execute on function public.join_room_by_code(text) to authenticated;

create index if not exists reports_reported_idx on public.player_reports(reported_id, created_at desc);
create index if not exists subscriptions_status_idx on public.subscriptions(status, expires_at);
