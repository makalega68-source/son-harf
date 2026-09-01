-- Shared TR/EN dictionary validation and game-specific lexical rules.
-- Scope: dictionary/validation, Word Siege language scoring context, Son Harf terminal-soft-g rule.

alter table public.dictionary_words
  add column if not exists source_id text,
  add column if not exists source_version text,
  add column if not exists lexical_kind text not null default 'word',
  add column if not exists is_abbreviation boolean not null default false,
  add column if not exists is_proper_noun boolean not null default false,
  add column if not exists game_allowed boolean not null default true;

update public.dictionary_words
set source_id = coalesce(source_id, 'legacy_unattributed'),
    source_version = coalesce(source_version, 'pre-2026-09-01')
where source_id is null or source_version is null;

-- Undo the 2026-08-22 fallback that inserted every Turkish two-letter pair.
-- Genuine entries that existed before that batch keep their older row and remain active.
update public.dictionary_words
set active = false,
    game_allowed = false,
    lexical_kind = 'synthetic_pair',
    source_id = 'legacy_synthetic_two_letter_fallback',
    source_version = '2026-08-22'
where language = 'tr'
  and created_at >= timestamptz '2026-08-22 12:32:00+00'
  and created_at <  timestamptz '2026-08-22 12:33:00+00'
  and char_length(normalized_word) = 2;

-- Explicitly classify known abbreviation/acronym regressions. These rows are retained for
-- provenance but are not playable. Future bulk imports must populate the metadata columns.
update public.dictionary_words
set is_abbreviation = true,
    game_allowed = false,
    lexical_kind = 'abbreviation'
where normalized_word in (
  'tbmm','tc','thy','trt','abd','ab','bm','tckn','ptt',
  'usa','uk','ceo','fbi','nasa','cia','nsa','eu','un','bbc','cnn','html','http','https','sms','lol'
);

-- Restore real Turkish words ending in soft-g to the COMMON dictionary. Son Harf rejects
-- these later as a game-specific rule; Word Siege may use them normally.
insert into public.dictionary_words(
  language, word, normalized_word, active, source_id, source_version,
  lexical_kind, is_abbreviation, is_proper_noun, game_allowed
) values
  ('tr','ağ','ağ',true,'kaikki-enwiktionary','2026-08-05','word',false,false,true),
  ('tr','dağ','dağ',true,'kaikki-enwiktionary','2026-08-05','word',false,false,true),
  ('tr','yağ','yağ',true,'kaikki-enwiktionary','2026-08-05','word',false,false,true),
  ('tr','sağ','sağ',true,'kaikki-enwiktionary','2026-08-05','word',false,false,true)
on conflict(language, normalized_word) do update
set word = excluded.word,
    active = true,
    source_id = excluded.source_id,
    source_version = excluded.source_version,
    lexical_kind = 'word',
    is_abbreviation = false,
    is_proper_noun = false,
    game_allowed = true;

create or replace function public.normalize_game_word(p_language text, p_word text)
returns text
language plpgsql
immutable
set search_path = pg_catalog, public, pg_temp
as $$
declare
  v_language text := lower(coalesce(p_language, ''));
  w text;
begin
  if v_language not in ('tr','en') then
    return '';
  end if;

  w := normalize(trim(coalesce(p_word, '')), NFC);
  if v_language = 'tr' then
    -- Locale-aware Turkish casing without depending on database/server locale.
    w := translate(w, 'IİÇĞÖŞÜ', 'ıiçğöşü');
    w := lower(w);
  else
    w := lower(w);
  end if;
  return normalize(w, NFC);
end
$$;

create or replace function private.validate_dictionary_word_v1(p_word text, p_language text)
returns table(valid boolean, reason text, normalized_word text)
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text := lower(coalesce(p_language, ''));
  v_word text;
  d public.dictionary_words%rowtype;
begin
  if v_language not in ('tr','en') then
    return query select false, 'invalid_language'::text, ''::text;
    return;
  end if;

  v_word := public.normalize_game_word(v_language, p_word);
  if char_length(v_word) < 2 or char_length(v_word) > 40 then
    return query select false, 'invalid_length'::text, v_word;
    return;
  end if;

  if (v_language = 'tr' and v_word !~ '^[a-zçğıöşü]+$')
     or (v_language = 'en' and v_word !~ '^[a-z]+$') then
    return query select false, 'invalid_characters'::text, v_word;
    return;
  end if;

  select * into d
  from public.dictionary_words x
  where x.language = v_language
    and x.normalized_word = v_word
  limit 1;

  if d.id is null or not coalesce(d.active, false) then
    return query select false, 'not_in_dictionary'::text, v_word;
  elsif coalesce(d.is_abbreviation, false) or d.lexical_kind in ('abbreviation','acronym','code','symbol') then
    return query select false, 'abbreviation_not_allowed'::text, v_word;
  elsif coalesce(d.is_proper_noun, false) or d.lexical_kind = 'proper_noun' then
    return query select false, 'proper_noun_not_allowed'::text, v_word;
  elsif not coalesce(d.game_allowed, true) then
    return query select false, 'not_game_allowed'::text, v_word;
  end if;

  return query select true, 'valid'::text, v_word;
end
$$;

revoke all on function private.validate_dictionary_word_v1(text,text) from public, anon, authenticated;

create or replace function private.word_siege_word_allowed_v1(p_word text, p_language text)
returns boolean
language sql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
  select coalesce((select v.valid from private.validate_dictionary_word_v1(p_word, p_language) v limit 1), false)
$$;

-- English Word Siege uses English tile values while Turkish keeps the existing Turkish table.
-- Existing score_word_v1 is intentionally left structurally unchanged and reads this helper.
create or replace function private.word_siege_letter_value_v1(p_letter text)
returns integer
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text := coalesce(current_setting('sonharf.word_siege_language', true), 'tr');
  l text := upper(coalesce(p_letter, ''));
begin
  if v_language = 'en' then
    return case l
      when 'A' then 1 when 'B' then 3 when 'C' then 3 when 'D' then 2 when 'E' then 1
      when 'F' then 4 when 'G' then 2 when 'H' then 4 when 'I' then 1 when 'J' then 8
      when 'K' then 5 when 'L' then 1 when 'M' then 3 when 'N' then 1 when 'O' then 1
      when 'P' then 3 when 'Q' then 10 when 'R' then 1 when 'S' then 1 when 'T' then 1
      when 'U' then 1 when 'V' then 4 when 'W' then 4 when 'X' then 8 when 'Y' then 4
      when 'Z' then 10 else 1 end;
  end if;

  return case l
    when 'A' then 1 when 'B' then 3 when 'C' then 4 when 'Ç' then 4
    when 'D' then 3 when 'E' then 1 when 'F' then 7 when 'G' then 5
    when 'Ğ' then 8 when 'H' then 5 when 'I' then 2 when 'İ' then 1
    when 'J' then 10 when 'K' then 1 when 'L' then 1 when 'M' then 2
    when 'N' then 1 when 'O' then 2 when 'Ö' then 7 when 'P' then 5
    when 'R' then 1 when 'S' then 2 when 'Ş' then 4 when 'T' then 1
    when 'U' then 2 when 'Ü' then 3 when 'V' then 7 when 'Y' then 3
    when 'Z' then 4 else 1 end;
end
$$;

-- Preserve the validated/deadline/idempotency pipeline; only bind score language to match state.
create or replace function public.submit_word_siege_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean default true
)
returns public.word_siege_games
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.word_siege_games;
  v_uid uuid := auth.uid();
  v_fingerprint text := md5(p_placements::text || ':' || coalesce(p_horizontal, true)::text);
  v_existing_move_number integer;
begin
  if v_uid is null then raise exception 'word_siege_unauthorized'; end if;
  select * into r from public.word_siege_games where id = p_game_id for update;
  if r.id is null then raise exception 'word_siege_not_found'; end if;
  if v_uid not in (r.player_one_id, r.player_two_id) then raise exception 'word_siege_not_participant'; end if;

  perform set_config('sonharf.word_siege_language', r.language, true);

  select m.move_number into v_existing_move_number
  from public.word_siege_moves m
  where m.game_id = p_game_id and m.player_id = v_uid and m.request_fingerprint = v_fingerprint
  order by m.id desc limit 1;
  if v_existing_move_number is not null
     and r.move_count = v_existing_move_number
     and r.last_action_player_id = v_uid then
    return r;
  end if;

  r := private.word_siege_prepare_turn_v2(p_game_id);
  if r.status <> 'playing' then return r; end if;
  perform private.word_siege_prevalidate_move_v2(p_game_id, p_placements, p_horizontal);
  r := private.submit_word_siege_move_v1(p_game_id, p_placements, p_horizontal);
  if r.status = 'playing' then r := private.word_siege_arm_next_turn_v2(r.id); end if;
  return r;
end
$$;

revoke all on function public.submit_word_siege_move_v1(uuid,jsonb,boolean) from public, anon, authenticated;
grant execute on function public.submit_word_siege_move_v1(uuid,jsonb,boolean) to authenticated;

create or replace function public.preview_word_siege_move_v1(
  p_game_id uuid,
  p_placements jsonb,
  p_horizontal boolean
)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  v_language text;
begin
  select g.language into v_language
  from public.word_siege_games g
  where g.id = p_game_id
    and auth.uid() in (g.player_one_id, g.player_two_id);
  if v_language is null then raise exception 'word_siege_not_participant'; end if;
  perform set_config('sonharf.word_siege_language', v_language, true);
  return private.word_siege_preview_move_v1(p_game_id, p_placements, p_horizontal);
end
$$;

revoke all on function public.preview_word_siege_move_v1(uuid,jsonb,boolean) from public, anon, authenticated;
grant execute on function public.preview_word_siege_move_v1(uuid,jsonb,boolean) to authenticated;

-- Keep the previous Son Harf implementation intact behind a strict front door.
do $$
begin
  if to_regprocedure('public.submit_word_v3_legacy(uuid,text)') is null
     and to_regprocedure('public.submit_word_v3(uuid,text)') is not null then
    alter function public.submit_word_v3(uuid,text) rename to submit_word_v3_legacy;
  end if;
end
$$;

revoke all on function public.submit_word_v3_legacy(uuid,text) from public, anon, authenticated;

create or replace function public.submit_word_v3(p_room_id uuid, p_word text)
returns public.game_rooms
language plpgsql
security definer
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
  r public.game_rooms;
  v_check record;
begin
  if auth.uid() is null then raise exception 'not_authenticated'; end if;
  select * into r from public.game_rooms where id = p_room_id;
  if r.id is null then raise exception 'room_not_found'; end if;
  if auth.uid() not in (r.host_id, r.guest_id) then raise exception 'not_participant'; end if;

  select * into v_check
  from private.validate_dictionary_word_v1(p_word, r.language)
  limit 1;

  if not coalesce(v_check.valid, false) then
    return public.switch_turn_after_failure(
      p_room_id,
      case v_check.reason
        when 'abbreviation_not_allowed' then 'abbreviation_not_allowed'
        when 'proper_noun_not_allowed' then 'proper_noun_not_allowed'
        else 'not_in_dictionary'
      end
    );
  end if;

  if r.language = 'tr' and right(v_check.normalized_word, 1) = 'ğ' then
    return public.switch_turn_after_failure(p_room_id, 'ends_with_soft_g');
  end if;

  return public.submit_word_v3_legacy(p_room_id, p_word);
end
$$;

revoke all on function public.submit_word_v3(uuid,text) from public, anon, authenticated;
grant execute on function public.submit_word_v3(uuid,text) to authenticated;

create index if not exists dictionary_words_game_validation_idx
  on public.dictionary_words(language, normalized_word)
  where active and game_allowed and not is_abbreviation and not is_proper_noun;

select pg_notify('pgrst','reload schema');
