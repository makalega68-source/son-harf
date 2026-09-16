-- Keep master-dictionary activation atomic while allowing the ~140k-row replacement to finish.

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
            or (language = 'tr' and normalized_word !~ '^[abcçdefgğhıijklmnoöprsştuüvyzâîû]+$')
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
