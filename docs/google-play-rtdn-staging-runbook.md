# Google Play RTDN OIDC + refund hardening runbook

Status: **staging-only until Issue #340 acceptance tests pass.**

Do not apply `supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql` to production and do not deploy the branch RTDN Edge Function to production before the isolated staging gate is complete.

## Required staging configuration

Configure these Edge Function secrets in the isolated staging project:

- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `GOOGLE_PLAY_PACKAGE_NAME=com.sonharf.game`
- `GOOGLE_PLAY_RTDN_AUDIENCE` — exact audience configured on the Pub/Sub push subscription
- `GOOGLE_PLAY_RTDN_PUSH_SERVICE_ACCOUNT` — exact email of the Pub/Sub push authentication service account

`GOOGLE_PLAY_RTDN_SECRET` and `X-Son-Harf-RTDN-Secret` are not part of the hardened authentication contract. Google-signed OIDC identity is the perimeter check.

The Pub/Sub push subscription must be configured for authenticated push using the expected service account and the exact audience above. The Edge Function validates the token signature, audience, `email_verified`, and service-account email before parsing or processing the RTDN payload.

## Staging order

1. Create/identify an isolated Supabase development branch or staging project. Do not create a paid Supabase branch without explicit cost approval.
2. Capture database backups/snapshots and the current definitions/grants for:
   - `apply_verified_play_purchase_v2`
   - `reconcile_play_entitlement_v1`
   - `purchases`, `subscriptions`, `store_entitlements`, `season_pass_entitlements`
3. Apply `supabase/staging-migrations/20260912_play_rtdn_refund_hardening.sql` to staging only.
4. Confirm `play_purchase_grants` and `play_rtdn_events` are RLS-enabled and executable mutation RPCs are service-role only.
5. Run `supabase/staging-tests/play_rtdn_refund_hardening.sql`. It must complete without exception and roll back its fixtures.
6. Deploy the branch version of `supabase/functions/google-play-rtdn/index.ts` to staging.
7. Point a non-production/test Pub/Sub push subscription at the staging endpoint with authenticated OIDC push.
8. Verify:
   - wrong/missing bearer token is rejected;
   - wrong audience/email is rejected;
   - duplicate `messageId` never double-processes;
   - failed/unfinished event is retried rather than silently acknowledged;
   - `VoidedPurchaseNotification` reverses only recorded grant provenance;
   - pending refund review never claws back;
   - one-time PURCHASED/CANCELED lifecycle events never grant or claw back by themselves;
   - subscription active/grace/canceled/expired/revoked paths stay constraint-safe;
   - Son Coin balance never becomes negative and any unrecovered refund is recorded in `reversal_shortfall`.
9. Run repository CI and Supabase security/performance advisors against staging.
10. Observe staging RTDN logs for retries, dedupe and reconciliation errors before production promotion.

## Production promotion

Only after every staging gate is green:

1. Take a fresh production backup and capture the current function definitions/grants again.
2. Copy the reviewed staging SQL into a new timestamped executable file under `supabase/migrations/`; do not rename/move it before staging evidence exists.
3. Merge only the exact staging-tested SQL and Edge Function source.
4. Apply the migration to production first.
5. Verify tables, constraints, RLS, grants, function definitions and advisors.
6. Configure production OIDC environment values and Pub/Sub authenticated push.
7. Deploy `google-play-rtdn` only after the database RPCs exist.
8. Send a Google test notification and verify the event ledger records exactly one successful processing row.
9. Monitor refund/reversal telemetry and `reversal_shortfall` for abnormal activity.

## Rollback

If production RTDN hardening misbehaves:

1. Stop or redirect the Pub/Sub push subscription before changing database reconciliation code.
2. Roll the Edge Function back to the previously captured version.
3. Restore the captured `apply_verified_play_purchase_v2` / `reconcile_play_entitlement_v1` definitions and their service-role-only grants if required.
4. Do **not** drop `play_purchase_grants` or `play_rtdn_events` during an incident; they are audit/provenance evidence and their presence is non-destructive.
5. Do not blindly compensate balances. Reconcile each affected token against the Play purchase record and provenance ledger.
6. Re-enable Pub/Sub only after a test delivery succeeds and duplicate delivery remains idempotent.

## Refund policy encoded by the staging candidate

- A completed Play void/refund is processed from `VoidedPurchaseNotification`.
- `pendingRefundReviewNotification` is recorded but is not a clawback signal.
- One-time PURCHASED/CANCELED lifecycle notifications do not create grants and do not independently claw back a previously granted purchase.
- Coin refund lower bound is **0 Son Coin**. If the user already spent part of the refunded grant, only the available balance is recovered and the remainder is persisted as `reversal_shortfall` for support/fraud review.
- A Style is removed only when the reversed Play provenance owns that inventory chain and no other active Play provenance owns the same Style.
