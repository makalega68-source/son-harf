# Son Harf Android release build

Version 0.8.0 introduces a CI path for signed release APK/AAB artifacts while keeping the existing debug QA build.

## Required GitHub Actions secrets

The signed release step runs only when all of these secrets exist:

- `SON_HARF_RELEASE_KEYSTORE_B64`: base64-encoded upload keystore (`.jks`)
- `SON_HARF_RELEASE_STORE_PASSWORD`: keystore password
- `SON_HARF_RELEASE_KEY_ALIAS`: upload key alias
- `SON_HARF_RELEASE_KEY_PASSWORD`: upload key password
- `SON_HARF_ADMOB_APP_ID`: production AdMob application ID
- `SON_HARF_ADMOB_REWARDED_ID`: production rewarded-ad unit ID

Do not commit a keystore or passwords to the repository.

## Supabase / Google Play verification secrets

The live `verify-play-purchase` Edge Function requires Google Play Developer API credentials before real purchases can be granted:

- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: complete service-account JSON with Android Publisher access to the Son Harf Play Console app
- `GOOGLE_PLAY_PACKAGE_NAME`: optional; defaults to `com.sonharf.game`

The function verifies the authenticated Supabase user, checks the purchase token directly against Google Play, validates product and purchase/subscription state, applies the entitlement through the service-role-only database RPC, and then attempts server-side acknowledgement. Purchase tokens are unique in `public.purchases`, so retries are idempotent.

Configured Play product IDs used by the app are:

- subscriptions: `vip_monthly`, `vip_yearly`
- one-time products: `coins_500`, `coins_1500`, `theme_neon`

Until `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is configured, the verifier deliberately returns `google_play_not_configured` and no entitlement is granted.

## Outputs

Every PR to `main` builds the debug APK used for QA. A push to `main` also builds the signed release artifacts when the release secrets above are configured:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

The `.aab` file is the Play Console upload artifact. The release build intentionally does not fall back to the Google test AdMob IDs in CI: if production AdMob secrets or signing material are missing, only the debug QA artifact is produced and Actions emits a warning.

## Local signed build

Pass the same values as Gradle properties or environment variables and run:

```bash
gradle assembleRelease bundleRelease \
  -PSON_HARF_RELEASE_STORE_FILE=/secure/path/son-harf-upload.jks \
  -PSON_HARF_RELEASE_STORE_PASSWORD='***' \
  -PSON_HARF_RELEASE_KEY_ALIAS='***' \
  -PSON_HARF_RELEASE_KEY_PASSWORD='***' \
  -PSON_HARF_ADMOB_APP_ID='ca-app-pub-...' \
  -PSON_HARF_ADMOB_REWARDED_ID='ca-app-pub-.../...'
```

Keep the upload keystore backed up securely. Losing it can disrupt future Play Store updates even when Play App Signing is enabled.
