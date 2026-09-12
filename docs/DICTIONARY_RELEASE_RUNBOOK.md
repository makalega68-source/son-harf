# TR/EN immutable dictionary release runbook

Issue: #342

Status: **staging-only until all release/rollback tests pass.**

The current production `dictionary_words`, `get_dictionary_snapshot_v4` and `validate_game_word_v2` remain authoritative until the release-control-plane migration is separately promoted after staging. The files under `supabase/staging-migrations/` are not production migrations.

## Release model

Each approved language release stores:

- immutable UUID `release_id` and content-derived `release_key`;
- language (`tr` or `en`);
- SHA-256 checksum;
- exact word count;
- sorted distinct immutable `text[]` snapshot;
- source manifest containing sync metadata, source IDs/versions and eligibility rules;
- creator and creation timestamp.

Checksum format is versioned as:

`sha256(sorted_distinct_utf8_byte_length_prefixed_lines_v1)`

For every sorted word, hash input contains `UTF8_BYTE_LENGTH:word`; entries are joined by `\n`. Android reproduces the same algorithm before accepting a refreshed cache.

Release rows have a trigger that rejects UPDATE and DELETE. Changes to the mutable source corpus therefore always produce a new release checksum/release row.

## Eligibility contract

A release contains only words that meet the current canonical snapshot policy:

- `language in ('tr','en')`;
- `active=true`;
- `game_allowed=true`;
- not abbreviation/acronym/code/symbol;
- not proper noun;
- not synthetic pair;
- 2–15 characters;
- Turkish words ending with `ğ` excluded.

Release creation fails closed if any eligible word lacks `source_id` or `source_version`.

## Runtime transition

The staging migration deliberately preserves the existing RPC names:

- `get_dictionary_snapshot_v4(text)`
- `validate_game_word_v2(text,text)`

Before a language has an active immutable release, both functions retain the existing `dictionary_words` behavior. After activation, both read the same immutable active release, so offline/preload snapshots and online authoritative validation cannot drift across rollback.

The Android `DictionaryReleaseCacheCoordinator` reads only small release metadata. If release ID/checksum/local content checksum are unchanged, it restores the existing persisted V4 cache. If any differ, it invokes the normal canonical V4 refresh and accepts the cache only when both word count and SHA-256 match the active release. Startup wraps this in `runCatching`, so a control-plane outage does not block the first UI frame.

## Staging procedure

1. Create/use an isolated Supabase development branch or staging project only after any required cost approval.
2. Capture current production/staging definitions and ACLs for:
   - `get_dictionary_snapshot_v4(text)`
   - `validate_game_word_v2(text,text)`
   - `dictionary_words`
   - `dictionary_sync_state`
3. Apply in order:
   - `supabase/staging-migrations/20260912_dictionary_immutable_releases_v1.sql`
   - `supabase/staging-migrations/20260912161000_dictionary_release_digest_schema_fix.sql`
4. Run `supabase/staging-tests/dictionary_immutable_releases_v1.sql`. It must finish without exception and roll back every fixture change.
5. Confirm ACLs:
   - V4/meta/validation: `public=false`, `anon=false`, `authenticated=true`, `service_role=true`.
   - admin create/activate/rollback RPCs: only authenticated can invoke; each still requires `auth.uid()+is_admin()` internally.
6. Confirm authenticated direct table reads expose only the active release; inactive release history and audit events are not client-readable.
7. Create real staging TR and EN releases from the complete staging corpus. Record release ID, checksum, count and source manifest.
8. Compare counts/checksums from a second creation attempt; identical source must reuse the same immutable release.
9. Activate each staging release and verify Android refresh, practice cache, V4 output and `validate_game_word_v2` agree.
10. Create a controlled second release, activate it, then call one `admin_rollback_dictionary_release_v1(language,release_id)` operation. Verify both V4 and online validation immediately follow the rolled-back pointer.
11. Run Android CI, Final Unified Validation and Frame Provenance Gate.
12. Run Supabase security/performance advisors and compare with the pre-migration baseline.

## Production promotion

Only after staging evidence is complete:

1. Take a fresh production backup/snapshot.
2. Capture the exact live V4/V2 definitions and execute grants for rollback.
3. Consolidate the staging SQL into a reviewed timestamped production migration under `supabase/migrations/`. Preserve the tested logic; do not rewrite the checksum algorithm during promotion.
4. Merge only after repository CI is green.
5. Apply the production migration.
6. Verify tables, RLS, policies, function definitions, ACLs and advisors before creating any active release.
7. Create TR and EN releases with `admin_create_dictionary_release_v1` and archive the returned release ID/checksum/count/source manifest in deployment records.
8. Independently compare release word counts with current V4 eligible counts. Expected pre-release baseline observed on 12 Sep 2026: EN 42,845 and TR 90,695; re-check live values at promotion time rather than assuming they are unchanged.
9. Activate one language at a time. Verify Android checksum refresh and online validation before activating the second language.
10. Keep the previous active release IDs in the deployment record for one-operation rollback.

## Rollback

Content rollback is intentionally non-destructive:

`admin_rollback_dictionary_release_v1('<tr|en>','<previous_release_uuid>')`

This updates only the active pointer and appends an immutable audit event. It does not rewrite `dictionary_words`, edit a historical release, or delete source/provenance data.

If the release-control-plane code itself must be rolled back, first point each language to the desired known-good release, then restore the captured V4/V2 definitions and grants. Do not delete release/audit tables during an incident; they are due-diligence and support evidence.

## Backup / restore evidence

For every production release operation retain:

- database backup identifier/time;
- Git merge SHA and migration filename;
- language;
- new and previous release IDs;
- SHA-256 and word count;
- source manifest;
- actor/admin identity;
- create/activate/rollback audit event IDs;
- CI run links/results;
- advisor before/after counts.
