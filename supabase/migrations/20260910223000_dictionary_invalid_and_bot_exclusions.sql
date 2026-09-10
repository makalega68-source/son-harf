-- Correct a confirmed Turkish dictionary-source leak while preserving valid human-play words.
-- Bot-only exclusions are intentionally separate: humans may still submit valid dictionary words.

update public.dictionary_words
set game_allowed = false
where language = 'tr'
  and normalized_word = 'amlat';

insert into public.bot_word_exclusions (language, normalized_word, reason)
select 'tr', blocked_word, 'bot-sensitive vocabulary'
from unnest(array[
  'am',
  'penis',
  'sik',
  'sikmek',
  'sikiş',
  'sikişmek',
  'yarak',
  'yarrak',
  'göt',
  'taşak',
  'taşşak',
  'vajina',
  'vulva',
  'klitoris',
  'dildo',
  'porno',
  'pornografi',
  'orospu',
  'pezevenk'
]) as blocked_word
where not exists (
  select 1
  from public.bot_word_exclusions existing
  where existing.language = 'tr'
    and existing.normalized_word = blocked_word
);
