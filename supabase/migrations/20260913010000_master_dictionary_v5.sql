-- Master dictionary v5
-- One canonical TR/EN dictionary source for every game mode, human validation and bots.
-- Data is staged first and atomically replaces dictionary_words only after source/count checks pass.

create table if not exists public.dictionary_sync_jobs (
    id uuid primary key default gen_random_uuid(),
    token_hash text not null,
    token_used_at timestamptz,
    status text not null default 'pending' check (status in ('pending','fetching','staged','active','error')),
    tr_source_url text not null default 'https://sozluk.gov.tr/autocomplete.json',
    tr_source_version text,
    tr_source_sha256 text,
    tr_count integer,
    en_source_url text not null default 'https://raw.githubusercontent.com/en-wl/wordlist-diff/71d7dd2a78741837acf11114c4ba4f0b19513dac/en_US.txt',
    en_source_version text not null default 'SCOWL/ESDB 2026.02.25',
    en_source_commit text not null default '71d7dd2a78741837acf11114c4ba4f0b19513dac',
    en_source_sha256 text,
    en_count integer,
    error_message text,
    created_at timestamptz not null default now(),
    started_at timestamptz,
    completed_at timestamptz
);

create table if not exists public.dictionary_stage_words (
    job_id uuid not null references public.dictionary_sync_jobs(id) on delete cascade,
    language text not null check (language in ('tr','en')),
    word text not null,
    normalized_word text not null,
    source_text text,
    primary key (job_id, language, normalized_word)
);

create index if not exists dictionary_stage_words_job_language_idx
    on public.dictionary_stage_words(job_id, language);

alter table public.dictionary_sync_jobs enable row level security;
alter table public.dictionary_stage_words enable row level security;

revoke all on table public.dictionary_sync_jobs from anon, authenticated;
revoke all on table public.dictionary_stage_words from anon, authenticated;
grant all on table public.dictionary_sync_jobs to service_role;
grant all on table public.dictionary_stage_words to service_role;

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

    -- Atomic replacement: concurrent readers see the old committed corpus or the new committed corpus,
    -- never a partially populated dictionary.
    delete from public.dictionary_words;

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

create or replace function public.get_dictionary_snapshot_v5(p_language text)
returns jsonb
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select jsonb_build_object(
        'language', case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end,
        'words', coalesce(jsonb_agg(d.normalized_word order by d.normalized_word), '[]'::jsonb)
    )
    from public.dictionary_words d
    where d.language = case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end
      and d.active = true
      and d.game_allowed = true
      and d.is_abbreviation = false
      and d.is_proper_noun = false
      and char_length(d.normalized_word) between 2 and 30;
$$;

revoke all on function public.get_dictionary_snapshot_v5(text) from public;
grant execute on function public.get_dictionary_snapshot_v5(text) to anon, authenticated, service_role;

create or replace function public.validate_game_word_v3(p_word text, p_language text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_language text := case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end;
    v_word text := public.normalize_game_word(coalesce(p_word,''), case when lower(coalesce(p_language,'')) = 'en' then 'en' else 'tr' end);
    v_valid boolean := false;
    v_length integer;
begin
    v_length := char_length(v_word);

    if v_length < 2 or v_length > 30 then
        return jsonb_build_object(
            'valid', false,
            'reason', 'length',
            'normalized_word', v_word,
            'first_letter', coalesce(substr(v_word, 1, 1), ''),
            'last_letter', coalesce(substr(v_word, greatest(v_length, 1), 1), ''),
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
    ) into v_valid;

    return jsonb_build_object(
        'valid', v_valid,
        'reason', case when v_valid then 'ok' else 'not_in_master_dictionary' end,
        'normalized_word', v_word,
        'first_letter', coalesce(substr(v_word, 1, 1), ''),
        'last_letter', coalesce(substr(v_word, greatest(v_length, 1), 1), ''),
        'char_length', v_length
    );
end;
$$;

revoke all on function public.validate_game_word_v3(text,text) from public;
grant execute on function public.validate_game_word_v3(text,text) to anon, authenticated, service_role;

comment on function public.get_dictionary_snapshot_v5(text) is
'Canonical master dictionary snapshot. Every word mode and bot must consume this same TR/EN corpus.';
comment on function public.validate_game_word_v3(text,text) is
'Canonical membership validation against the master dictionary v5 corpus.';
