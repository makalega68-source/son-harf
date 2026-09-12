-- STAGING ONLY companion patch for 20260912_dictionary_immutable_releases_v1.sql.
-- pgcrypto is installed in the extensions schema; the admin builder intentionally uses search_path=''.

create or replace function public.admin_create_dictionary_release_v1(p_language text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid := auth.uid();
  v_lang text := lower(trim(coalesce(p_language,'')));
  v_words text[];
  v_word_count integer;
  v_checksum text;
  v_release public.dictionary_releases_v1%rowtype;
  v_manifest jsonb;
  v_sync public.dictionary_sync_state%rowtype;
  v_missing_source integer := 0;
begin
  if v_uid is null or not public.is_admin() then raise exception 'admin_required'; end if;
  if v_lang not in ('tr','en') then raise exception 'invalid_language'; end if;

  select * into v_sync
  from public.dictionary_sync_state
  where language=v_lang;
  if not found or not coalesce(v_sync.ready,false) then raise exception 'dictionary_not_ready'; end if;

  with eligible as (
    select distinct d.normalized_word
    from public.dictionary_words d
    where d.language=v_lang
      and d.active
      and coalesce(d.game_allowed,true)
      and not coalesce(d.is_abbreviation,false)
      and not coalesce(d.is_proper_noun,false)
      and coalesce(d.lexical_kind,'word') not in ('abbreviation','acronym','code','symbol','proper_noun','synthetic_pair')
      and char_length(d.normalized_word) between 2 and 15
      and not (v_lang='tr' and right(d.normalized_word,1)='ğ')
  ), ordered as (
    select normalized_word
    from eligible
    order by normalized_word collate "C"
  )
  select
    array_agg(normalized_word order by normalized_word collate "C"),
    count(*)::integer,
    encode(
      extensions.digest(
        convert_to(
          string_agg(
            octet_length(convert_to(normalized_word,'UTF8'))::text || ':' || normalized_word,
            E'\n' order by normalized_word collate "C"
          ),
          'UTF8'
        ),
        'sha256'
      ),
      'hex'
    )
  into v_words,v_word_count,v_checksum
  from ordered;

  if coalesce(v_word_count,0)=0 or v_words is null or v_checksum is null then
    raise exception 'dictionary_release_empty';
  end if;

  select count(*)::integer into v_missing_source
  from public.dictionary_words d
  where d.language=v_lang
    and d.normalized_word=any(v_words)
    and (nullif(trim(d.source_id),'') is null or nullif(trim(d.source_version),'') is null);
  if v_missing_source>0 then raise exception 'dictionary_source_provenance_missing:%',v_missing_source; end if;

  select jsonb_build_object(
    'sync_state',jsonb_build_object(
      'ready',v_sync.ready,
      'reported_word_count',v_sync.word_count,
      'source_name',v_sync.source_name,
      'source_url',v_sync.source_url,
      'license_note',v_sync.license_note,
      'synced_at',v_sync.synced_at
    ),
    'sources',coalesce((
      select jsonb_agg(
        jsonb_build_object('source_id',q.source_id,'source_version',q.source_version)
        order by q.source_id collate "C",q.source_version collate "C"
      )
      from (
        select distinct d.source_id,d.source_version
        from public.dictionary_words d
        where d.language=v_lang and d.normalized_word=any(v_words)
      ) q
    ),'[]'::jsonb),
    'eligibility',jsonb_build_object(
      'min_length',2,
      'max_length',15,
      'active_required',true,
      'game_allowed_required',true,
      'abbreviations_allowed',false,
      'proper_nouns_allowed',false,
      'synthetic_pairs_allowed',false,
      'turkish_terminal_soft_g_allowed',false
    ),
    'checksum_format','sha256(sorted_distinct_utf8_byte_length_prefixed_lines_v1)'
  ) into v_manifest;

  select * into v_release
  from public.dictionary_releases_v1
  where language=v_lang and checksum_sha256=v_checksum;

  if found then
    return jsonb_build_object(
      'created',false,
      'release_id',v_release.release_id,
      'release_key',v_release.release_key,
      'language',v_release.language,
      'checksum_sha256',v_release.checksum_sha256,
      'word_count',v_release.word_count,
      'source_manifest',v_release.source_manifest,
      'created_at',v_release.created_at
    );
  end if;

  insert into public.dictionary_releases_v1(
    release_key,language,checksum_sha256,word_count,words,source_manifest,created_by
  ) values (
    v_lang || '-' || left(v_checksum,20),v_lang,v_checksum,v_word_count,v_words,v_manifest,v_uid
  ) returning * into v_release;

  insert into public.dictionary_release_events_v1(
    language,action,release_id,actor_id,details
  ) values (
    v_lang,'create',v_release.release_id,v_uid,
    jsonb_build_object('checksum_sha256',v_checksum,'word_count',v_word_count)
  );

  return jsonb_build_object(
    'created',true,
    'release_id',v_release.release_id,
    'release_key',v_release.release_key,
    'language',v_release.language,
    'checksum_sha256',v_release.checksum_sha256,
    'word_count',v_release.word_count,
    'source_manifest',v_release.source_manifest,
    'created_at',v_release.created_at
  );
end
$$;

revoke all on function public.admin_create_dictionary_release_v1(text) from public,anon,service_role;
grant execute on function public.admin_create_dictionary_release_v1(text) to authenticated;
