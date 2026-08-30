-- Production rule: Turkish gameplay must never hand the next player an impossible "Ğ" start.
-- Remove terminal-soft-g entries from the playable dictionary and reject any direct insert as a backend guard.

delete from public.dictionary_words
where language = 'tr'
  and right(normalized_word, 1) = 'ğ';

update public.dictionary_review_queue
set status = 'rejected',
    updated_at = now()
where language = 'tr'
  and right(normalized_word, 1) = 'ğ'
  and status <> 'rejected';

create or replace function public.reject_terminal_soft_g_game_word()
returns trigger
language plpgsql
security definer
set search_path=public,pg_temp
as $$
declare
  v_language text;
begin
  select language into v_language
  from public.game_rooms
  where id = new.room_id;

  if v_language = 'tr'
     and right(coalesce(new.normalized_word, ''), 1) = 'ğ' then
    raise exception 'ends_with_soft_g';
  end if;

  return new;
end
$$;

drop trigger if exists game_words_reject_terminal_soft_g on public.game_words;
create trigger game_words_reject_terminal_soft_g
before insert or update of normalized_word on public.game_words
for each row execute function public.reject_terminal_soft_g_game_word();

create index if not exists dictionary_words_tr_last_char_idx
on public.dictionary_words(language, right(normalized_word, 1));

select pg_notify('pgrst','reload schema');
