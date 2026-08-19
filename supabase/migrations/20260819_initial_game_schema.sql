-- Son Harf initial backend schema
create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (char_length(display_name) between 2 and 24),
  avatar_url text,
  is_vip boolean not null default false,
  diamonds integer not null default 0 check (diamonds >= 0),
  wins integer not null default 0,
  losses integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.game_rooms (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  host_id uuid not null references public.profiles(id) on delete cascade,
  guest_id uuid references public.profiles(id) on delete set null,
  status text not null default 'waiting' check (status in ('waiting','playing','finished','cancelled')),
  current_player_id uuid references public.profiles(id) on delete set null,
  winner_id uuid references public.profiles(id) on delete set null,
  turn_deadline timestamptz,
  created_at timestamptz not null default now(),
  finished_at timestamptz
);

create table if not exists public.game_words (
  id bigint generated always as identity primary key,
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  player_id uuid not null references public.profiles(id) on delete cascade,
  word text not null,
  normalized_word text not null,
  created_at timestamptz not null default now(),
  unique(room_id, normalized_word)
);

create table if not exists public.chat_messages (
  id bigint generated always as identity primary key,
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 300),
  created_at timestamptz not null default now()
);

create table if not exists public.purchases (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  product_id text not null,
  purchase_token text not null unique,
  order_id text,
  status text not null default 'pending' check (status in ('pending','verified','rejected','refunded')),
  purchased_at timestamptz,
  verified_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.game_rooms enable row level security;
alter table public.game_words enable row level security;
alter table public.chat_messages enable row level security;
alter table public.purchases enable row level security;

create policy "profiles readable" on public.profiles for select to authenticated using (true);
create policy "profile self update" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());
create policy "rooms participants read" on public.game_rooms for select to authenticated using (host_id = auth.uid() or guest_id = auth.uid());
create policy "rooms host create" on public.game_rooms for insert to authenticated with check (host_id = auth.uid());
create policy "words participants read" on public.game_words for select to authenticated using (
  exists(select 1 from public.game_rooms r where r.id = room_id and (r.host_id = auth.uid() or r.guest_id = auth.uid()))
);
create policy "words self insert" on public.game_words for insert to authenticated with check (player_id = auth.uid());
create policy "chat participants read" on public.chat_messages for select to authenticated using (
  exists(select 1 from public.game_rooms r where r.id = room_id and (r.host_id = auth.uid() or r.guest_id = auth.uid()))
);
create policy "chat self insert" on public.chat_messages for insert to authenticated with check (sender_id = auth.uid());
create policy "purchases self read" on public.purchases for select to authenticated using (user_id = auth.uid());

create index if not exists game_words_room_created_idx on public.game_words(room_id, created_at);
create index if not exists chat_messages_room_created_idx on public.chat_messages(room_id, created_at);
create index if not exists game_rooms_status_idx on public.game_rooms(status);

alter publication supabase_realtime add table public.game_rooms;
alter publication supabase_realtime add table public.game_words;
alter publication supabase_realtime add table public.chat_messages;
