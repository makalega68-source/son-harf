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

### SH-SEC-001 — Anonymous SECURITY DEFINER timeout mutation
- Module/screen: Supabase multiplayer timeout RPC
- Severity: Critical
- User impact: An anonymous caller with room/state identifiers could attempt server-state mutation through the three-argument optimistic timeout RPC.
- Reproduction: Live function privilege/body inspection showed `claim_turn_timeout_v2(uuid,uuid,timestamptz)` executable by `anon`; participant guard used `auth.uid() <> ...`, which is NULL-unsafe for unauthenticated callers.
- Expected: No anonymous EXECUTE; explicit authenticated identity; null-safe participant validation.
- Actual: `anon` EXECUTE present; no explicit `auth.uid() is null` rejection in that overload.
- Root cause: Legacy overload retained broad grant and a comparison pattern that fails closed only for non-null identities.
- Changed files: `supabase/migrations/20260907020500_release_security_matchmaking_hardening.sql`, `ReleaseSecurityMatchmakingContractTest.kt`
- Fix: Revoke PUBLIC/anon execution, grant only authenticated/service-role, explicit `not_authenticated`, use `IS DISTINCT FROM` participant check, retain optimistic expected-player/deadline idempotency.
- Added test: Static release contract for auth guard/grants/null-safe participant logic.
- Regression risk: Medium; legacy anonymous clients would be rejected by design.
- Test result: CI pending for the combined backend hardening package.
- Related commit: source migration `1aa89e5d2322e31dae140c2b912481a21d9406b4`; test `aaa525002c06bdf2f807d916643b7053f67d1840`.
- Status: Fixed in source branch; NOT deployed to production because staging/backup gate is not yet available in this session.

### SH-MM-001 — Bot fallback starts at 10 seconds instead of approved 15 seconds
- Module/screen: Random matchmaking / bot fallback
- Severity: High
- User impact: Real opponents receive five seconds less search time than the approved product behavior; analytics and perceived real-player availability are distorted.
- Reproduction: Read-only live `poll_random_matchmaking_v2()` inspection shows `queued_at <= now() - interval '10 seconds'`.
- Expected: Bot fallback no earlier than 15 seconds; a queue entry can yield only one active match.
- Actual: 10-second threshold in live function.
- Root cause: Legacy fallback migration remained authoritative after product behavior changed.
- Changed files: same release-hardening migration and contract test as SH-SEC-001.
- Fix: 15-second threshold, explicit auth guard, conditional queue transition, race cleanup if another transaction already resolved/cancelled the queue.
- Added test: 15-second literal lock; absence of 10-second threshold in hardening migration; single-resolution guard.
- Regression risk: Medium; matchmaking latency distribution changes by design.
- Test result: CI pending.
- Status: Fixed in source branch; NOT deployed to production until staging/backup gate is satisfied.

### SH-BOT-001 — Practice bot is not adaptive to player performance
- Module/screen: Bot difficulty / practice matchmaking
- Severity: High
- User impact: New/weak players can receive inappropriate difficulty; experienced players can receive insufficient challenge; retention target is not met.
- Reproduction: Live `bot_take_turn_normal_v1` reads only persisted `bot_difficulty` (`easy|normal|hard`). Existing schema exposes rating, matches, wins/losses, valid words and best streak, but current bot selection does not derive difficulty from them.
- Expected: Server-selected, explainable difficulty using player skill/performance signals; deterministic seed for repeatable tests.
- Actual: Manual three-level preference and database `random()` selection.
- Root cause: Initial manual bot difficulty system was never upgraded to adaptive policy.
- Changed files: none yet.
- Test: none yet.
- Status: Open — source/server design required; do not mark complete.

### SH-IAP-001 — Refund/chargeback reconciliation not yet proven
- Module/screen: Google Play purchase verification / entitlement lifecycle
- Severity: High
- User impact: A refunded or charged-back entitlement may remain active unless an external reconciliation path exists.
- Reproduction: Source verification flow proves purchase/token validation, idempotent grant and consume/ack paths; no verified refund/chargeback reconciliation path has been located yet.
- Expected: pending/completed/failed/refunded/chargeback lifecycle with entitlement reconciliation and audit trail.
- Actual: Initial purchase verification is present; downstream refund/chargeback proof is incomplete.
- Root cause: Audit incomplete or lifecycle feature absent.
- Changed files: none yet.
- Status: Open pending repository/backend proof.

### SH-MUSIC-001 — Warm Beginnings license provenance incomplete
- Module/screen: Audio / due diligence
- Severity: High
- User impact: Store/release and future asset transfer carry licensing risk if the music right cannot be proven.
- Reproduction: Runtime is explicitly single-track `R.raw.warm_beginnings`; asset register states it is the sole background music, but current repository/library search has not produced a license/source record for that track.
- Expected: Source, license/receipt/permission, file hash and usage scope documented.
- Actual: Usage identity is clear; provenance evidence not located.
- Root cause: Missing or undiscovered license record.
- Changed files: none.
- Status: Open; no replacement or unlicensed download permitted.

## Confirmed product invariants during audit
- Runtime music implementation references only Warm Beginnings.
- Current Style shop copy exposes Son Coin (`SC`) and explicitly states it buys personalization, not match power.
- Play purchase grant RPC is service-role-only and purchase-token idempotent.
- Current Word Siege v3/v4 server formula preserves word score and values each currently owned cube at exactly 2.
- Frame package provenance exists for the purchased LAYERLAB avatar-frame package; verified integration report records 8/8 decoded-RGBA matches.

## Release blockers
1. SH-SEC-001 must be deployed to a safe environment and negative-tested before production.
2. SH-MM-001 must be deployed/tested with queue cancellation/reconnect/concurrent resolution scenarios before production.
3. SH-BOT-001 remains functionally incomplete against the approved adaptive-bot requirement.
4. SH-IAP-001 refund/chargeback lifecycle must be proven or implemented before purchase acceptance is marked complete.
5. SH-MUSIC-001 provenance must be resolved before asset/license due-diligence acceptance.
6. No production database migration is permitted until backup/staging prerequisites are satisfied.

## Continuation / RESUME_HERE
Last known verified baseline: `3f3a79299e269e6135a53ca4f74ac0c67f32a73b`.
Current safety-branch head after backend contract tests: `aaa525002c06bdf2f807d916643b7053f67d1840`.
Open draft PR: #269.
Next checks: wait for CI at current head; inspect failing job immediately if any; then continue server-authoritative bot, IAP lifecycle, RLS/advisor, ad-placement, avatar consistency, dictionary provenance/performance, admin RBAC/audit and release gates.
