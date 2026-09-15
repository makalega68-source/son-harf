-- Apply, in order, to an isolated staging/dev branch first:
--   supabase/staging-migrations/20260912_dictionary_immutable_releases_v1.sql
--   supabase/staging-migrations/20260912161000_dictionary_release_digest_schema_fix.sql
-- Then run this script. Every fixture mutation rolls back.

begin;

-- Use an existing staging admin identity only as the authenticated actor. No auth/admin row is changed.
do $$
declare
  v_admin uuid;
begin
  select user_id into v_admin
  from public.admin_users
  order by created_at
  limit 1;
  if v_admin is null then raise exception 'staging_admin_missing'; end if;
  perform set_config('request.jwt.claim.sub',v_admin::text,true);
end
$$;

-- Build a tiny deterministic English source corpus inside the rollback transaction.
update public.dictionary_words set active=false where language='en';
update public.dictionary_sync_state
set ready=true,
    word_count=5,
    source_name='staging-release-fixture',
    source_url='https://example.invalid/dictionary-fixture',
    license_note='staging-test-only',
    synced_at=now()
where language='en';

insert into public.dictionary_words(
  id,language,word,normalized_word,active,source_id,source_version,
  lexical_kind,is_abbreviation,is_proper_noun,game_allowed
) values
  (-342001,'en','zqxone','zqxone',true,'staging-342','v1','word',false,false,true),
  (-342002,'en','zqxtwo','zqxtwo',true,'staging-342','v1','word',false,false,true),
  (-342003,'en','zqxthree','zqxthree',true,'staging-342','v1','word',false,false,true),
  (-342004,'en','zzzzzzzzzzzzzzzz','zzzzzzzzzzzzzzzz',true,'staging-342','v1','word',false,false,true),
  (-342005,'en','zqxabbr','zqxabbr',true,'staging-342','v1','abbreviation',true,false,true)
on conflict(language,normalized_word) do update
set active=excluded.active,
    source_id=excluded.source_id,
    source_version=excluded.source_version,
    lexical_kind=excluded.lexical_kind,
    is_abbreviation=excluded.is_abbreviation,
    is_proper_noun=excluded.is_proper_noun,
    game_allowed=excluded.game_allowed;

set local role authenticated;

do $$
declare
  v_first jsonb;
  v_repeat jsonb;
  v_first_id uuid;
  v_words text[];
  v_meta_id uuid;
  v_meta_checksum text;
  v_meta_count integer;
  v_valid boolean;
  v_visible integer;
begin
  v_first := public.admin_create_dictionary_release_v1('en');
  v_first_id := (v_first->>'release_id')::uuid;
  if (v_first->>'created')::boolean is not true then raise exception 'first_release_not_created'; end if;
  if (v_first->>'checksum_sha256') <> 'd135b7f0c6f8ef4e2a58e6cacceabceacb88d7a0ceb52ab2a6fefa68724ddd46' then
    raise exception 'first_release_checksum_mismatch:%',v_first->>'checksum_sha256';
  end if;
  if (v_first->>'word_count')::integer <> 3 then raise exception 'first_release_word_count_mismatch'; end if;

  -- Same source state is idempotent: immutable release is reused, not duplicated.
  v_repeat := public.admin_create_dictionary_release_v1('en');
  if (v_repeat->>'created')::boolean is not false
     or (v_repeat->>'release_id')::uuid <> v_first_id then
    raise exception 'same_content_release_not_idempotent';
  end if;

  perform public.admin_activate_dictionary_release_v1(v_first_id);
  perform set_config('sonharf.test.release1',v_first_id::text,true);

  select release_id,checksum_sha256,word_count
    into v_meta_id,v_meta_checksum,v_meta_count
  from public.get_dictionary_release_meta_v1('en');
  if v_meta_id<>v_first_id or v_meta_checksum<>'d135b7f0c6f8ef4e2a58e6cacceabceacb88d7a0ceb52ab2a6fefa68724ddd46' or v_meta_count<>3 then
    raise exception 'first_release_meta_mismatch';
  end if;

  select words into v_words from public.get_dictionary_snapshot_v4('en');
  if v_words<>array['zqxone','zqxthree','zqxtwo']::text[] then
    raise exception 'first_release_snapshot_mismatch:%',v_words;
  end if;

  select valid into v_valid from public.validate_game_word_v2('zqxone','en');
  if v_valid is not true then raise exception 'active_release_valid_word_rejected'; end if;
  select valid into v_valid from public.validate_game_word_v2('zzzzzzzzzzzzzzzz','en');
  if v_valid is not false then raise exception 'release_length_rule_regressed'; end if;
  select valid into v_valid from public.validate_game_word_v2('zqxabbr','en');
  if v_valid is not false then raise exception 'release_abbreviation_rule_regressed'; end if;

  -- RLS exposes only the active immutable release to a normal authenticated client.
  select count(*)::integer into v_visible from public.dictionary_releases_v1 where language='en';
  if v_visible<>1 then raise exception 'active_release_rls_visibility_mismatch:%',v_visible; end if;
  if exists(select 1 from public.dictionary_release_state_v1 where language='tr') then
    raise exception 'language_isolation_regressed';
  end if;
end
$$;

reset role;

-- Release rows stay immutable even for the migration/test owner.
do $$
declare
  v_first_id uuid := current_setting('sonharf.test.release1')::uuid;
begin
  begin
    update public.dictionary_releases_v1
    set word_count=word_count
    where release_id=v_first_id;
    raise exception 'immutable_release_update_allowed';
  exception when others then
    if sqlerrm='immutable_release_update_allowed' then raise; end if;
    if position('dictionary_release_immutable' in sqlerrm)=0 then
      raise exception 'unexpected_immutability_error:%',sqlerrm;
    end if;
  end;
end
$$;

-- Change the mutable source corpus and create a second immutable release.
insert into public.dictionary_words(
  id,language,word,normalized_word,active,source_id,source_version,
  lexical_kind,is_abbreviation,is_proper_noun,game_allowed
) values (
  -342006,'en','zqxfour','zqxfour',true,'staging-342','v2','word',false,false,true
)
on conflict(language,normalized_word) do update
set active=true,source_id='staging-342',source_version='v2',lexical_kind='word',
    is_abbreviation=false,is_proper_noun=false,game_allowed=true;

update public.dictionary_sync_state
set word_count=6,source_name='staging-release-fixture-v2',synced_at=now()
where language='en';

set local role authenticated;

do $$
declare
  v_first_id uuid := current_setting('sonharf.test.release1')::uuid;
  v_second jsonb;
  v_second_id uuid;
  v_words text[];
  v_valid boolean;
  v_count integer;
  v_action_count integer;
begin
  v_second := public.admin_create_dictionary_release_v1('en');
  v_second_id := (v_second->>'release_id')::uuid;
  if (v_second->>'created')::boolean is not true or v_second_id=v_first_id then
    raise exception 'changed_content_did_not_create_new_release';
  end if;
  if (v_second->>'checksum_sha256') <> '62d62ea692106f228285d87fa19a03f0363706883fa0ee72a2fb044a377d8292' then
    raise exception 'second_release_checksum_mismatch:%',v_second->>'checksum_sha256';
  end if;
  if (v_second->>'word_count')::integer<>4 then raise exception 'second_release_word_count_mismatch'; end if;

  perform public.admin_activate_dictionary_release_v1(v_second_id);

  select words into v_words from public.get_dictionary_snapshot_v4('en');
  if v_words<>array['zqxfour','zqxone','zqxthree','zqxtwo']::text[] then
    raise exception 'second_release_snapshot_mismatch:%',v_words;
  end if;
  select valid into v_valid from public.validate_game_word_v2('zqxfour','en');
  if v_valid is not true then raise exception 'second_release_validation_not_active'; end if;

  -- One admin operation rolls both snapshot and authoritative validation back to release 1.
  perform public.admin_rollback_dictionary_release_v1('en',v_first_id);

  select words into v_words from public.get_dictionary_snapshot_v4('en');
  if v_words<>array['zqxone','zqxthree','zqxtwo']::text[] then
    raise exception 'rollback_snapshot_mismatch:%',v_words;
  end if;
  select valid into v_valid from public.validate_game_word_v2('zqxfour','en');
  if v_valid is not false then raise exception 'rollback_validation_did_not_follow_pointer'; end if;
  select valid into v_valid from public.validate_game_word_v2('zqxthree','en');
  if v_valid is not true then raise exception 'rollback_valid_word_rejected'; end if;

  select count(*)::integer into v_count
  from public.dictionary_releases_v1
  where language='en';
  if v_count<>1 then raise exception 'rls_did_not_hide_inactive_release:%',v_count; end if;

  -- Two create events, two activations and exactly one rollback. Repeated identical create added no event.
  select count(*)::integer into v_action_count
  from public.dictionary_release_events_v1
  where language='en' and action='rollback';
  -- Audit table intentionally has no authenticated direct SELECT grant, so this query must fail if
  -- executed as the client role. Audit verification is done after RESET ROLE below.
exception when insufficient_privilege then
  null;
end
$$;

reset role;

do $$
declare
  v_create integer;
  v_activate integer;
  v_rollback integer;
  v_source_manifest jsonb;
  v_first_id uuid := current_setting('sonharf.test.release1')::uuid;
begin
  select count(*) filter(where action='create')::integer,
         count(*) filter(where action='activate')::integer,
         count(*) filter(where action='rollback')::integer
    into v_create,v_activate,v_rollback
  from public.dictionary_release_events_v1
  where language='en';
  if v_create<>2 or v_activate<>2 or v_rollback<>1 then
    raise exception 'release_audit_counts_mismatch:%/%/%',v_create,v_activate,v_rollback;
  end if;

  select source_manifest into v_source_manifest
  from public.dictionary_releases_v1
  where release_id=v_first_id;
  if jsonb_array_length(v_source_manifest->'sources')<>1
     or v_source_manifest#>>'{sources,0,source_id}'<>'staging-342'
     or v_source_manifest#>>'{eligibility,min_length}'<>'2'
     or v_source_manifest#>>'{eligibility,max_length}'<>'15' then
    raise exception 'source_manifest_mismatch:%',v_source_manifest;
  end if;

  if has_function_privilege('anon','public.get_dictionary_release_meta_v1(text)','execute')
     or has_function_privilege('anon','public.get_dictionary_snapshot_v4(text)','execute')
     or has_function_privilege('anon','public.validate_game_word_v2(text,text)','execute') then
    raise exception 'dictionary_anon_execute_regression';
  end if;
  if not has_function_privilege('authenticated','public.get_dictionary_release_meta_v1(text)','execute')
     or not has_function_privilege('authenticated','public.get_dictionary_snapshot_v4(text)','execute')
     or not has_function_privilege('authenticated','public.validate_game_word_v2(text,text)','execute') then
    raise exception 'dictionary_authenticated_execute_missing';
  end if;
end
$$;

rollback;
