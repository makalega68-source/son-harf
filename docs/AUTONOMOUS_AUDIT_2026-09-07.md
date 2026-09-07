# Son Harf — Autonomous Release Audit Ledger — 2026-09-07

## Verified baseline
- Repository: `makalega68-source/son-harf`
- Protected working baseline: `3f3a79299e269e6135a53ca4f74ac0c67f32a73b`
- Baseline app version: `0.9.2` (`versionCode 18`)
- Safety branch: `audit/autonomous-release-hardening-20260907`
- Main is intentionally unchanged until quality gates pass.
- Live Supabase project was inspected read-only before any production schema write.

## Issue ledger

### SH-WS-001 — Kuşatma regression test asserted superseded scoring migration
- Module/screen: Word Siege / scoring tests
- Severity: High
- User impact: A future regression could reintroduce opponent-area subtraction while tests still passed.
- Reproduction: Inspect `WordSiegeFinalRulesTest` against v2/v3/v4 migrations.
- Expected: Word/letter score remains permanent; total is permanent word score plus currently owned cubes × 2.
- Actual: Production v3/v4 already had the correct formula, but the regression test still asserted the superseded v2 SQL text.
- Root cause: Test contract was not advanced when scoring authority migrated from v2 to v3/v4.
- Changed files: `app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt`
- Fix: Validate v3/v4 formula and add capture/loss/recapture ownership sequence.
- Added test: Current-territory formula, one cube = 2, rival neutral capture isolation, recapture restoration.
- Regression risk: Low; test-only package.
- Test result: PASS — Android CI #1438, Final Unified Validation #726, Frame Provenance Gate #297.
- Commit: `9ca38af3f3299ca27c0d5afca2d255d852e7793a`
- Status: Fixed on safety branch; not merged to main.

### SH-SEC-001 — Anonymous SECURITY DEFINER state mutation paths
- Module/screen: Supabase multiplayer timeout + private-room pause/resume RPCs
- Severity: Critical
- User impact: Anonymous callers could reach state-mutating SECURITY DEFINER RPCs whose participant checks were NULL-unsafe.
- Reproduction: Live privilege/body inspection showed `claim_turn_timeout_v2(uuid,uuid,timestamptz)`, `pause_private_room(uuid)` and `resume_private_room(uuid)` executable by `anon`; guards used comparisons that did not explicitly reject NULL `auth.uid()`.
- Expected: No anonymous EXECUTE; explicit authenticated identity; null-safe participant validation.
- Actual: `anon` EXECUTE present on those mutation paths.
- Root cause: Legacy overloads retained broad grants and NULL-unsafe participant comparison patterns.
- Changed files: `supabase/migrations/20260907020500_release_security_matchmaking_hardening.sql`, `app/src/test/java/com/sonharf/game/ReleaseSecurityMatchmakingContractTest.kt`
- Fix: Revoke PUBLIC/anon execution, grant only authenticated/service-role, explicit `not_authenticated`, use `IS DISTINCT FROM` participant checks, retain timeout optimistic expected-player/deadline idempotency.
- Added test: Release contract for auth guard/grants/null-safe participant logic on timeout and private-room state changes.
- Regression risk: Medium; legacy anonymous clients are rejected by design.
- Test result: PASS — Android CI #1443, Final Unified Validation #731, Frame Provenance Gate #302.
- Latest test commit: `d285e101145509f4d83adde17c2a6a07a7dd6e92`.
- Status: Fixed in source branch; NOT deployed to production because staging/backup gate is not available in this session.

### SH-MM-001 — Bot fallback starts at 10 seconds instead of approved 15 seconds
- Module/screen: Random matchmaking / bot fallback
- Severity: High
- User impact: Real opponents receive five seconds less search time than the approved behavior.
- Reproduction: Read-only live `poll_random_matchmaking_v2()` inspection shows `queued_at <= now() - interval '10 seconds'`.
- Expected: Bot fallback no earlier than 15 seconds; one queue record can produce only one active match.
- Actual: 10-second threshold in live function.
- Root cause: Legacy fallback migration remained authoritative after the approved product behavior changed.
- Changed files: same release-hardening migration and contract test as SH-SEC-001.
- Fix: 15-second threshold, explicit auth guard, conditional queue transition, cleanup if another transaction already resolved/cancelled the queue.
- Added test: 15-second contract, absence of legacy 10-second threshold, single-resolution guard.
- Regression risk: Medium; matchmaking latency distribution changes by design.
- Test result: PASS in source CI package (#1443/#731/#302).
- Status: Fixed in source branch; NOT deployed to production until staging/backup gate is satisfied.

### SH-BOT-001 — Practice bot is not adaptive to player performance
- Module/screen: Bot difficulty / practice matchmaking
- Severity: High
- User impact: New/weak players can receive inappropriate difficulty; experienced players can receive insufficient challenge.
- Reproduction: Live `bot_take_turn_normal_v1` reads persisted `bot_difficulty` (`easy|normal|hard`) and uses database randomness; it does not derive difficulty from player performance signals.
- Expected: Server-selected, explainable difficulty using player rating/level/history/word success/move-time/streak signals and deterministic seeds for tests.
- Actual: Manual three-level preference.
- Root cause: Initial manual bot difficulty system was never upgraded to adaptive policy.
- Changed files: none yet.
- Status: Open — acceptance criterion not met.

### SH-IAP-001 — Refund/chargeback reconciliation not yet proven
- Module/screen: Google Play purchase verification / entitlement lifecycle
- Severity: High
- User impact: A refunded/charged-back entitlement may remain active unless a reconciliation path exists.
- Reproduction: Source verification flow proves Play purchase-token validation, authenticated user verification, service-role-only idempotent grant and consume/ack handling; no verified refund/chargeback reconciliation path has been located yet.
- Expected: pending/completed/failed/refunded/chargeback lifecycle with entitlement reconciliation and audit trail.
- Actual: Initial purchase verification is present; downstream refund/chargeback proof remains incomplete.
- Status: Open.

### SH-MUSIC-001 — Warm Beginnings license provenance incomplete
- Module/screen: Audio / due diligence
- Severity: High
- User impact: Store/release and future transfer carry licensing risk if usage rights cannot be proven.
- Reproduction: Runtime explicitly uses `R.raw.warm_beginnings`; current repository/library search has not produced a license/receipt/source record for that track.
- Expected: Source, license/receipt/permission, file hash and usage scope documented.
- Actual: Usage identity is clear; provenance evidence not located.
- Status: Open; no replacement or unlicensed download permitted.

### SH-UI-001 — Shared framed avatar still renders circular profile photos
- Module/screen: Shared avatar/profile-frame component
- Severity: High
- User impact: Avatar presentation is inconsistent with the approved rectangular profile-photo standard across surfaces.
- Reproduction: `FramedProfilePhotoAvatar` calls `ProfilePhotoAvatarWithGender`, whose image/fallback uses `CircleShape`; a rectangular `ProfilePhotoAvatarRectWithGender` already exists separately.
- Expected: One reusable rectangular aspect/crop/frame language across all avatar surfaces.
- Actual: Shared framed component is circular.
- Root cause: Frame integration reused the legacy circular avatar primitive.
- Changed files: none yet.
- Status: Open — frame aspect/overlay geometry must be verified before a safe global replacement.

### SH-ADMIN-001 — Current admin console is below required fallback scope
- Module/screen: Admin console / operational control plane
- Severity: High
- User impact: Required operations such as system health, match review, dictionary version publishing, store/inventory, sales dashboard, bot/matchmaking controls, feature flags, tournament/season management and broad audit workflows are not all exposed in the current console.
- Reproduction: Current `AdminConsoleScreen` contains catalogue, VIP/ads, moderation, profile/name and password sections; no authoritative newer approved admin-plan document was found in Library search.
- Expected: Latest approved plan, or the command's secure fallback scope with RBAC/audit.
- Actual: Partial console only.
- Status: Open; do not claim admin acceptance.

## Dictionary snapshot findings
- Live `dictionary_words`: Turkish 369,334 rows; 368,621 active; 368,618 active game-allowed; 369,334 unique normalized entries.
- English: 51,333 rows; 51,333 active; 51,312 game-allowed.
- Schema contains `source_id`, `source_version`, `lexical_kind`, `is_abbreviation`, `is_proper_noun`, `game_allowed`, review queue and sync-state support.
- `dictionary_sync_state` reports Turkish canonical source as Zemberek NLP master dictionary, Apache 2.0 repository, synced 2026-08-22; English reports wordfreq top-25k source/licensing.
- A release-grade per-version hash/rollback/provenance reconciliation for the full expanded 369k Turkish set has NOT yet been proven; dictionary acceptance remains open.

## Confirmed product invariants during audit
- Runtime music implementation references only Warm Beginnings.
- Current Style shop copy exposes Son Coin (`SC`) and explicitly states it buys personalization, not match power.
- Play purchase grant RPC is service-role-only and purchase-token idempotent.
- Current Word Siege v3/v4 server formula preserves word score and values each currently owned cube at exactly 2.
- Public schema currently has no table with RLS disabled.
- Frame package provenance exists for the purchased LAYERLAB avatar-frame package; prior verification records 8/8 decoded-RGBA matches.

## Performance/security observations not yet closed
- Supabase performance advisor reports multiple missing foreign-key indexes, per-row auth evaluation patterns in RLS, and duplicate/unused index candidates. No index was dropped solely because an advisor called it unused.
- SECURITY DEFINER exposure requires function-by-function review; admin setters inspected so far use explicit admin checks, but broad advisor findings are not blanket-cleared.

## Release blockers
1. SH-SEC-001 must be deployed to a safe environment and negative-tested before production.
2. SH-MM-001 must be deployed/tested with queue cancellation/reconnect/concurrent resolution scenarios before production.
3. SH-BOT-001 adaptive bot is incomplete.
4. SH-IAP-001 refund/chargeback lifecycle must be proven or implemented.
5. SH-MUSIC-001 license provenance must be resolved.
6. SH-UI-001 rectangular shared-avatar migration is incomplete.
7. SH-ADMIN-001 admin fallback scope is incomplete.
8. Full dictionary version/hash/rollback provenance is not yet proven.
9. No production database migration is permitted until backup/staging prerequisites are satisfied.
10. Final three consecutive clean full-suite release validation rounds have not yet been completed at the final branch head.

## Continuation / RESUME_HERE
Verified baseline: `3f3a79299e269e6135a53ca4f74ac0c67f32a73b` (`0.9.2`, versionCode 18).
Safety branch: `audit/autonomous-release-hardening-20260907`.
Latest fully CI-verified code/test head before this documentation update: `d285e101145509f4d83adde17c2a6a07a7dd6e92`.
Open draft PR: #269.
Main and production database remain unchanged.
Next required gates: adaptive bot; refund/chargeback reconciliation; avatar rectangle/frame geometry; admin fallback scope; dictionary full version/hash/rollback; ad placement matrix; slogan audit; performance/RLS hardening; staging backup/migration/E2E; final three clean full validation rounds; signed release APK/AAB + checksum only after blockers are closed.
