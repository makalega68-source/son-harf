-- Remove Turkish circumflex letters (â, î, û) from every validation path.
-- Stored words keep their unaccented spelling; player input is also folded.

create or replace function public.normalize_game_word(p_language text, p_word text)
returns text
language plpgsql immutable
as $$
declare
  w text;
begin
  w := trim(p_word);
  if p_language = 'tr' then
    w := translate(w, 'IİÇĞÖŞÜ', 'ıiçğöşü');
    w := lower(w);
    w := translate(w, 'âîû', 'aiu');
  else
    w := lower(w);
  end if;
  return w;
end;
$$;

create or replace function public.validate_game_word_v3(p_word text, p_language text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_language text := case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end;
    v_word text := public.normalize_game_word(
        case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end,
        coalesce(p_word,'')
    );
    v_valid boolean := false;
    v_length integer;
begin
    v_length := char_length(v_word);

    if v_length < 2 or v_length > 30 then
        return jsonb_build_object(
            'valid', false,
            'reason', 'invalid_length',
            'normalized_word', v_word,
            'first_letter', coalesce(left(v_word, 1), ''),
            'last_letter', coalesce(right(v_word, 1), ''),
            'char_length', v_length
        );
    end if;

    if (v_language = 'tr' and v_word !~ '^[a-zçğıöşü]+$')
       or (v_language = 'en' and v_word !~ '^[a-z]+$') then
        return jsonb_build_object(
            'valid', false,
            'reason', 'invalid_characters',
            'normalized_word', v_word,
            'first_letter', coalesce(left(v_word, 1), ''),
            'last_letter', coalesce(right(v_word, 1), ''),
            'char_length', v_length
        );
    end if;

    select exists (
        select 1
        from public.dictionary_words d
        where d.language = v_language
          and d.normalized_word = v_word
          and d.active = true
          and d.game_allowed = true
          and d.is_abbreviation = false
          and d.is_proper_noun = false
          and d.source_id = case
                when v_language = 'tr' then 'tdk-gts-autocomplete'
                else 'scowl-esdb-en-us'
              end
    ) into v_valid;

    return jsonb_build_object(
        'valid', v_valid,
        'reason', case when v_valid then 'ok' else 'not_in_master_dictionary' end,
        'normalized_word', v_word,
        'first_letter', coalesce(left(v_word, 1), ''),
        'last_letter', coalesce(right(v_word, 1), ''),
        'char_length', v_length
    );
end;
$$;

revoke all on function public.validate_game_word_v3(text,text) from public;
grant execute on function public.validate_game_word_v3(text,text) to anon, authenticated, service_role;

create or replace function private.validate_dictionary_word_v1(p_word text, p_language text)
returns table(valid boolean, reason text, normalized_word text)
language plpgsql
stable
set search_path = pg_catalog, public, private, pg_temp
as $$
declare
    v_language text := lower(coalesce(p_language,''));
    v_word text;
    d public.dictionary_words%rowtype;
begin
    if v_language not in ('tr','en') then
        return query select false,'invalid_language'::text,''::text;
        return;
    end if;

    v_word := public.normalize_game_word(v_language, p_word);
    if char_length(v_word) < 2 or char_length(v_word) > 30 then
        return query select false,'invalid_length'::text,v_word;
        return;
    end if;
    if (v_language='tr' and v_word !~ '^[a-zçğıöşü]+$')
       or (v_language='en' and v_word !~ '^[a-z]+$') then
        return query select false,'invalid_characters'::text,v_word;
        return;
    end if;

    select * into d
    from public.dictionary_words x
    where x.language = v_language
      and x.normalized_word = v_word
      and x.source_id = case
            when v_language = 'tr' then 'tdk-gts-autocomplete'
            else 'scowl-esdb-en-us'
          end
    limit 1;

    if d.id is null or not coalesce(d.active,false) then
        return query select false,'not_in_dictionary'::text,v_word;
        return;
    end if;
    if coalesce(d.is_abbreviation,false)
       or coalesce(d.lexical_kind,'word') in ('abbreviation','acronym','code','symbol') then
        return query select false,'abbreviation_not_allowed'::text,v_word;
        return;
    end if;
    if coalesce(d.is_proper_noun,false) or coalesce(d.lexical_kind,'word')='proper_noun' then
        return query select false,'proper_noun_not_allowed'::text,v_word;
        return;
    end if;
    if not coalesce(d.game_allowed,true) then
        return query select false,'not_game_allowed'::text,v_word;
        return;
    end if;

    return query select true,'valid'::text,v_word;
end;
$$;

create or replace function public.activate_dictionary_sync_v1(p_job_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
set statement_timeout = '120s'
as $$
declare
    v_job public.dictionary_sync_jobs%rowtype;
    v_tr_count integer;
    v_en_count integer;
begin
    perform pg_advisory_xact_lock(hashtext('master_dictionary_v5'));

    select * into v_job
    from public.dictionary_sync_jobs
    where id = p_job_id
    for update;

    if not found then raise exception 'dictionary_sync_job_not_found'; end if;
    if v_job.status <> 'staged' then raise exception 'dictionary_sync_job_not_staged:%', v_job.status; end if;

    select count(*) into v_tr_count from public.dictionary_stage_words where job_id = p_job_id and language = 'tr';
    select count(*) into v_en_count from public.dictionary_stage_words where job_id = p_job_id and language = 'en';

    if v_tr_count < 20000 then raise exception 'turkish_dictionary_too_small:%', v_tr_count; end if;
    if v_en_count < 20000 then raise exception 'english_dictionary_too_small:%', v_en_count; end if;
    if v_job.tr_source_sha256 is null or v_job.en_source_sha256 is null then raise exception 'dictionary_source_hash_missing'; end if;

    if exists (
        select 1 from public.dictionary_stage_words
        where job_id = p_job_id
          and (
            char_length(normalized_word) < 2
            or char_length(normalized_word) > 30
            or (language = 'en' and normalized_word !~ '^[a-z]+$')
            or (language = 'tr' and normalized_word !~ '^[abcçdefgğhıijklmnoöprsştuüvyz]+$')
          )
    ) then
        raise exception 'dictionary_stage_contains_invalid_word';
    end if;

    delete from public.dictionary_words where true;

    insert into public.dictionary_words (
        language, word, normalized_word, active, word_source, bot_eligible,
        source_id, source_url, source_version, source_commit,
        license_name, license_url, source_sha256, lexical_kind,
        is_abbreviation, is_proper_noun, game_allowed, reject_reason, source_text
    )
    select
        s.language, s.word, s.normalized_word, true,
        case when s.language = 'tr' then 'tdk' else 'scowl' end,
        true,
        case when s.language = 'tr' then 'tdk-gts-autocomplete' else 'scowl-esdb-en-us' end,
        case when s.language = 'tr' then v_job.tr_source_url else v_job.en_source_url end,
        case when s.language = 'tr' then v_job.tr_source_version else v_job.en_source_version end,
        case when s.language = 'tr' then null else v_job.en_source_commit end,
        case when s.language = 'tr' then 'TDK official headword source; redistribution rights not asserted' else 'SCOWL / English Speller Database source licences' end,
        case when s.language = 'tr' then 'https://sozluk.gov.tr/' else 'https://wordlist.aspell.net/' end,
        case when s.language = 'tr' then v_job.tr_source_sha256 else v_job.en_source_sha256 end,
        'word', false, false, true, null, s.source_text
    from public.dictionary_stage_words s
    where s.job_id = p_job_id;

    update public.dictionary_sync_jobs
    set status = 'active', tr_count = v_tr_count, en_count = v_en_count,
        completed_at = now(), error_message = null
    where id = p_job_id;

    delete from public.dictionary_stage_words where job_id = p_job_id;

    return jsonb_build_object(
        'job_id', p_job_id,
        'status', 'active',
        'tr_count', v_tr_count,
        'en_count', v_en_count,
        'tr_sha256', v_job.tr_source_sha256,
        'en_sha256', v_job.en_source_sha256
    );
end;
$$;
