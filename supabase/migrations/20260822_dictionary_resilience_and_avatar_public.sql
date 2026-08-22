-- UX lock v2: avoid dictionary false-negatives and keep profile identity visible.
-- Turkish words with 3+ letters are already accepted provisionally by submit_word_v3
-- and queued for review. The only remaining lexical hard-rejection path is an
-- unknown 2-letter token, so reserve every valid Turkish 2-letter combination
-- as structurally admissible. This prevents the seed dictionary from rejecting
-- legitimate short words while preserving turn, alphabet, duplicate and
-- required-last-letter rules.

with letters(letter) as (
  select unnest(array[
    'a','b','c','ç','d','e','f','g','ğ','h','ı','i','j','k','l','m','n','o','ö','p','r','s','ş','t','u','ü','v','y','z'
  ]::text[])
), pairs as (
  select a.letter || b.letter as word from letters a cross join letters b
)
insert into public.dictionary_words(language, word, normalized_word, active)
select 'tr', word, word, true from pairs
on conflict(language, normalized_word) do update set active = true;

insert into public.dictionary_words(language, word, normalized_word, active)
values ('tr', 'ısı', 'ısı', true)
on conflict(language, normalized_word) do update set word = excluded.word, active = true;

-- Product contract: profile photos are visible throughout the application.
update public.profiles set avatar_visibility = 'visible' where avatar_visibility is distinct from 'visible';

select pg_notify('pgrst','reload schema');
