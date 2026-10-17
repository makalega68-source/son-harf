-- Son Harf expert bot: fast and everyday words.
-- The expert turn counted possible replies for 48 candidates by scanning the whole dictionary for
-- each one, which ran past the request timeout, so the bot never moved and the match froze. It now
-- picks from the everyday word list first (full dictionary only as a fallback), uses the indexed
-- first-letter filter, and keeps its edge by preferring words that end on letters few words start
-- with (precomputed in sonharf_letter_openings) and longer words.

do $patch$
declare
  d text;
  a integer;
  b integer;
begin
  d := pg_get_functiondef('public.bot_take_turn_expert_v1(uuid)'::regprocedure);
  a := position('  with candidates as (' in d);
  b := position('  if chosen.id is null then' in d);
  if a = 0 or b = 0 or b < a then raise exception 'expert bot patch anchors not found'; end if;
  d := substr(d, 1, a - 1) || $block$
  with common_pool as (
    select dw.id, dw.normalized_word
    from public.sonharf_bot_common_words c
    join public.dictionary_words dw on dw.language = c.language and dw.normalized_word = c.normalized_word
    where c.language = r.language
      and dw.active and dw.game_allowed and not dw.is_abbreviation and not dw.is_proper_noun
      and (expected is null or left(c.normalized_word, char_length(expected)) = expected)
      and public.sonharf_bot_word_allowed(dw.language, dw.normalized_word)
      and not exists (select 1 from public.game_words w where w.room_id = r.id and w.normalized_word = dw.normalized_word)
    order by random()
    limit 60
  ),
  full_pool as (
    select dw.id, dw.normalized_word
    from public.dictionary_words dw
    where not exists (select 1 from common_pool)
      and dw.language = r.language
      and dw.active and dw.game_allowed and not dw.is_abbreviation and not dw.is_proper_noun
      and (expected is null or (left(dw.normalized_word, 1) = left(expected, 1)
        and left(dw.normalized_word, char_length(expected)) = expected))
      and public.sonharf_bot_word_allowed(dw.language, dw.normalized_word)
      and not exists (select 1 from public.game_words w where w.room_id = r.id and w.normalized_word = dw.normalized_word)
    order by random()
    limit 60
  ),
  pool as (
    select * from common_pool
    union all
    select * from full_pool
  )
  select dw.* into chosen
  from pool p
  join public.dictionary_words dw on dw.id = p.id
  left join public.sonharf_letter_openings o on o.language = r.language and o.letter = right(p.normalized_word, 1)
  order by coalesce(o.word_count, 0) + abs(char_length(p.normalized_word) - 7) * 120 + random() * 400
  limit 1;

$block$ || substr(d, b);
  execute d;
end
$patch$;

revoke all on function public.bot_take_turn_expert_v1(uuid) from public, anon, authenticated;
grant execute on function public.bot_take_turn_expert_v1(uuid) to service_role;
