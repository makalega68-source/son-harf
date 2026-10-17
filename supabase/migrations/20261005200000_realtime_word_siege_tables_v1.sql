-- Online games wake on Realtime changes instead of polling every second: Kuşatma's game and chat
-- rows join the publication (Son Harf's rooms, words and chat already are). RLS still limits each
-- player to their own games, so nothing new becomes visible.
do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'word_siege_games') then
    alter publication supabase_realtime add table public.word_siege_games;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'word_siege_messages') then
    alter publication supabase_realtime add table public.word_siege_messages;
  end if;
end $$;
