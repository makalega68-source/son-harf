-- Master dictionary v5 production/runtime repairs.
-- Keeps a single canonical TR/EN corpus, records source provenance, and makes activation compatible
-- with projects that enforce DELETE-WITH-WHERE safety.

alter table public.dictionary_words
    add column if not exists word_source text,
    add column if not exists bot_eligible boolean not null default true,
    add column if not exists source_url text,
    add column if not exists source_commit text,
    add column if not exists license_name text,
    add column if not exists license_url text,
    add column if not exists source_sha256 text,
    add column if not exists reject_reason text,
    add column if not exists source_text text;

create index if not exists dictionary_words_language_active_game_idx
    on public.dictionary_words(language, active, game_allowed);

create index if not exists dictionary_words_source_id_idx
    on public.dictionary_words(source_id);

create or replace function public.activate_dictionary_sync_v1(p_job_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
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

    if not found then
        raise exception 'dictionary_sync_job_not_found';
    end if;
    if v_job.status <> 'staged' then
        raise exception 'dictionary_sync_job_not_staged:%', v_job.status;
    end if;

    select count(*) into v_tr_count
    from public.dictionary_stage_words
    where job_id = p_job_id and language = 'tr';

    select count(*) into v_en_count
    from public.dictionary_stage_words
    where job_id = p_job_id and language = 'en';

    if v_tr_count < 20000 then
        raise exception 'turkish_dictionary_too_small:%', v_tr_count;
    end if;
    if v_en_count < 20000 then
        raise exception 'english_dictionary_too_small:%', v_en_count;
    end if;
    if v_job.tr_source_sha256 is null or v_job.en_source_sha256 is null then
        raise exception 'dictionary_source_hash_missing';
    end if;

    if exists (
        select 1
        from public.dictionary_stage_words
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

    -- Atomic replacement. The WHERE predicate is intentional: production enables safe-update guards.
    delete from public.dictionary_words where true;

    insert into public.dictionary_words (
        language,
        word,
        normalized_word,
        active,
        word_source,
        bot_eligible,
        source_id,
        source_url,
        source_version,
        source_commit,
        license_name,
        license_url,
        source_sha256,
        lexical_kind,
        is_abbreviation,
        is_proper_noun,
        game_allowed,
        reject_reason,
        source_text
    )
    select
        s.language,
        s.word,
        s.normalized_word,
        true,
        case when s.language = 'tr' then 'tdk' else 'scowl' end,
        true,
        case when s.language = 'tr' then 'tdk-gts-autocomplete' else 'scowl-esdb-en-us' end,
        case when s.language = 'tr' then v_job.tr_source_url else v_job.en_source_url end,
        case when s.language = 'tr' then v_job.tr_source_version else v_job.en_source_version end,
        case when s.language = 'tr' then null else v_job.en_source_commit end,
        case when s.language = 'tr'
            then 'TDK official headword source; redistribution rights not asserted'
            else 'SCOWL / English Speller Database source licences'
        end,
        case when s.language = 'tr'
            then 'https://sozluk.gov.tr/'
            else 'https://wordlist.aspell.net/'
        end,
        case when s.language = 'tr' then v_job.tr_source_sha256 else v_job.en_source_sha256 end,
        'word',
        false,
        false,
        true,
        null,
        s.source_text
    from public.dictionary_stage_words s
    where s.job_id = p_job_id;

    update public.dictionary_sync_jobs
    set status = 'active',
        tr_count = v_tr_count,
        en_count = v_en_count,
        completed_at = now(),
        error_message = null
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

revoke all on function public.activate_dictionary_sync_v1(uuid) from public, anon, authenticated;
grant execute on function public.activate_dictionary_sync_v1(uuid) to service_role;

-- Generates the one-time sync token inside Postgres so it never needs to be exposed to clients.
create or replace function public.start_dictionary_sync_v1()
returns jsonb
language plpgsql
security definer
set search_path = public, extensions, pg_temp
as $$
declare
    v_token text := encode(gen_random_bytes(48), 'hex');
    v_token_hash text := encode(digest(v_token, 'sha256'), 'hex');
    v_job_id uuid;
    v_request_id bigint;
begin
    insert into public.dictionary_sync_jobs (token_hash)
    values (v_token_hash)
    returning id into v_job_id;

    select net.http_post(
        url := 'https://bzdtftzdjtjoqhtcqtxb.supabase.co/functions/v1/sync-master-dictionary',
        body := jsonb_build_object('job_id', v_job_id::text, 'token', v_token),
        headers := jsonb_build_object('Content-Type', 'application/json'),
        timeout_milliseconds := 120000
    ) into v_request_id;

    return jsonb_build_object('job_id', v_job_id, 'request_id', v_request_id);
end;
$$;

revoke all on function public.start_dictionary_sync_v1() from public, anon, authenticated;
grant execute on function public.start_dictionary_sync_v1() to service_role;
