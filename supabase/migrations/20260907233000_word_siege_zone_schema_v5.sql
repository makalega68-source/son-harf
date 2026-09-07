-- Word Siege v5 / 1: zone-conquest state.

alter table public.word_siege_games
  add column if not exists player_one_conquest_meter integer not null default 0,
  add column if not exists player_two_conquest_meter integer not null default 0,
  add column if not exists player_one_onslaught_active boolean not null default false,
  add column if not exists player_two_onslaught_active boolean not null default false,
  add column if not exists turn_started_at timestamptz,
  add column if not exists turn_deadline timestamptz;

alter table public.word_siege_games drop constraint if exists word_siege_games_player_one_conquest_meter_v5_check;
alter table public.word_siege_games drop constraint if exists word_siege_games_player_two_conquest_meter_v5_check;
alter table public.word_siege_games
  add constraint word_siege_games_player_one_conquest_meter_v5_check check (player_one_conquest_meter between 0 and 2),
  add constraint word_siege_games_player_two_conquest_meter_v5_check check (player_two_conquest_meter between 0 and 2);

alter table public.word_siege_moves
  add column if not exists raw_word_score integer not null default 0,
  add column if not exists zones_flipped integer[] not null default '{}'::integer[],
  add column if not exists onslaught_triggered boolean not null default false,
  add column if not exists onslaught_consumed boolean not null default false;
