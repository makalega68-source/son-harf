-- Immutable dictionary release snapshots with deterministic SHA-256 and admin rollback.
-- Existing get_dictionary_snapshot_v3 remains untouched until this migration is staged/deployed.

create table if not exists public.dictionary_release_snapshots (
  id uuid primary key default gen_random_uuid(),
  language text not null check (language in ('tr','en')),
  version text not null,
  snapshot_sha256 text not null check (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
  word_count integer not null check (word_count > 0),
  source_manifest jsonb not null,
  words text[] not null,
  created_at timestamptz not null default now(),
  created_by uuid references auth.users(id),
  unique(language, version),
  unique(language, snapshot_sha256)
);
alter table public.dictionary_release_snapshots enable row level security;
revoke all on public.dictionary_release_snapshots from public,anon,authenticated;
grant select,insert on public.dictionary_release_snapshots to service_role;

create table if not exists public.dictionary_release_state (
  language text primary key check (language in ('tr','en')),
  active_release_id uuid references public.dictionary_release_snapshots(id),
  previous_release_id uuid references public.dictionary_release_snapshots(id),
  updated_at timestamptz not null default now(),
  updated_by uuid references auth.users(id)
);
alter table public.dictionary_release_state enable row level security;
revoke all on public.dictionary_release_state from public,anon,authenticated;
grant select,insert,update on public.dictionary_release_state to service_role;

create or replace function public.admin_publish_dictionary_release_v1(p_language text,p_version text)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_language text:=lower(trim(p_language));
  v_version text:=trim(p_version);
  v_words text[];
  v_count integer;
  v_hash text;
  v_manifest jsonb;
  v_release_id uuid;
  v_old_active uuid;
begin
  if v_uid is null or not public.is_admin(v_uid) then raise exception 'forbidden'; end if;
  if v_language not in ('tr','en') then raise exception 'invalid_language'; end if;
  if v_version='' or length(v_version)>120 then raise exception 'invalid_version'; end if;

  select array_agg(w.normalized_word order by w.normalized_word), count(*)::integer
    into v_words,v_count
  from public.dictionary_words w
  where w.language=v_language
    and w.active=true
    and coalesce(w.game_allowed,true)=true
    and coalesce(w.is_abbreviation,false)=false
    and coalesce(w.is_proper_noun,false)=false
    and char_length(w.normalized_word) between 2 and 12;
  if coalesce(v_count,0)=0 then raise exception 'empty_dictionary_snapshot'; end if;

  select encode(extensions.digest(convert_to(array_to_string(v_words,E'\n'),'UTF8'),'sha256'),'hex') into v_hash;

  select coalesce(jsonb_agg(jsonb_build_object(
    'source_id',s.source_id,'source_version',s.source_version,'rows',s.rows
  ) order by s.rows desc,s.source_id,s.source_version),'[]'::jsonb)
  into v_manifest
  from (
    select coalesce(w.source_id,'legacy_unattributed') as source_id,
           coalesce(w.source_version,'unknown') as source_version,
           count(*)::integer as rows
    from public.dictionary_words w
    where w.language=v_language
      and w.active=true
      and coalesce(w.game_allowed,true)=true
      and coalesce(w.is_abbreviation,false)=false
      and coalesce(w.is_proper_noun,false)=false
      and char_length(w.normalized_word) between 2 and 12
    group by coalesce(w.source_id,'legacy_unattributed'),coalesce(w.source_version,'unknown')
  ) s;

  insert into public.dictionary_release_snapshots(language,version,snapshot_sha256,word_count,source_manifest,words,created_by)
  values(v_language,v_version,v_hash,v_count,v_manifest,v_words,v_uid)
  returning id into v_release_id;

  select active_release_id into v_old_active from public.dictionary_release_state where language=v_language for update;
  insert into public.dictionary_release_state(language,active_release_id,previous_release_id,updated_by)
  values(v_language,v_release_id,v_old_active,v_uid)
  on conflict(language) do update set
    previous_release_id=public.dictionary_release_state.active_release_id,
    active_release_id=excluded.active_release_id,
    updated_at=now(),updated_by=excluded.updated_by;

  return jsonb_build_object('success',true,'release_id',v_release_id,'language',v_language,'version',v_version,
    'sha256',v_hash,'word_count',v_count,'source_manifest',v_manifest);
end $$;
revoke all on function public.admin_publish_dictionary_release_v1(text,text) from public,anon;
grant execute on function public.admin_publish_dictionary_release_v1(text,text) to authenticated,service_role;

create or replace function public.admin_rollback_dictionary_release_v1(p_language text,p_release_id uuid default null)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  v_uid uuid:=auth.uid();
  v_language text:=lower(trim(p_language));
  v_current uuid; v_previous uuid; v_target uuid; v_version text; v_hash text;
begin
  if v_uid is null or not public.is_admin(v_uid) then raise exception 'forbidden'; end if;
  select active_release_id,previous_release_id into v_current,v_previous
  from public.dictionary_release_state where language=v_language for update;
  if v_current is null then raise exception 'release_state_missing'; end if;
  v_target:=coalesce(p_release_id,v_previous);
  if v_target is null or v_target=v_current then raise exception 'rollback_target_missing'; end if;
  select version,snapshot_sha256 into v_version,v_hash
  from public.dictionary_release_snapshots where id=v_target and language=v_language;
  if not found then raise exception 'invalid_rollback_target'; end if;
  update public.dictionary_release_state set active_release_id=v_target,previous_release_id=v_current,updated_at=now(),updated_by=v_uid
  where language=v_language;
  return jsonb_build_object('success',true,'language',v_language,'release_id',v_target,'version',v_version,'sha256',v_hash);
end $$;
revoke all on function public.admin_rollback_dictionary_release_v1(text,uuid) from public,anon;
grant execute on function public.admin_rollback_dictionary_release_v1(text,uuid) to authenticated,service_role;

create or replace function public.get_dictionary_snapshot_v4(p_language text default 'tr')
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare v_language text:=case when lower(trim(coalesce(p_language,'tr')))='en' then 'en' else 'tr' end; v_release public.dictionary_release_snapshots%rowtype;
begin
  select r.* into v_release
  from public.dictionary_release_state s join public.dictionary_release_snapshots r on r.id=s.active_release_id
  where s.language=v_language;
  if not found then return public.get_dictionary_snapshot_v3(v_language); end if;
  return jsonb_build_object('language',v_language,'version',v_release.version,'sha256',v_release.snapshot_sha256,
    'word_count',v_release.word_count,'words',to_jsonb(v_release.words));
end $$;
revoke all on function public.get_dictionary_snapshot_v4(text) from public;
grant execute on function public.get_dictionary_snapshot_v4(text) to anon,authenticated,service_role;
