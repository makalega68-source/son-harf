create index if not exists word_siege_games_winner_idx
  on public.word_siege_games(winner_id)
  where winner_id is not null;

create index if not exists word_siege_games_last_action_player_idx
  on public.word_siege_games(last_action_player_id)
  where last_action_player_id is not null;
