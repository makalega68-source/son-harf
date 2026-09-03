# Son Harf Safe Cleanup Audit

Date: 2026-09-03
Base main commit: 47b9da551e779ef41a2046955d02e229e438e074
Cleanup branch: safe-cleanup

## Rules
- Preserve the latest working system.
- Delete only proven redundant/obsolete files.
- Do not delete purchased source asset packages merely because they are currently unused.
- Keep suspicious items until dependency/use is proven.
- Validate each cleanup package with build/test/regression before merge.

## Initial classification

### AKTIF
- `.github/workflows/android.yml` — primary Android CI; runs debug unit tests + debug APK, can build signed release APK/AAB, publishes `final-test` release.
- `.github/workflows/current-apk-build.yml` — current APK publication pipeline; verifies committed LightDuel palette, runs debug unit tests + assembleDebug, uploads `SonHarf-Current`, publishes build result into `dist/`.
- `.github/workflows/apk-release.yml` — explicit/manual stable APK release path using fixed `mobile-latest` release; kept because it serves a different distribution purpose from CI artifacts.

### GEREKLI AMA PASIF / OZEL AMACLI
- Frame provenance/style validation workflows — retain until purchased-frame provenance and current Style integration are fully consolidated.
- Regression/stabilization and production validation workflows — retain pending a dedicated dependency and trigger audit.
- Asset-generation/integration workflows — retain until generated/runtime assets and license provenance are confirmed independent of them.

### SILINEBILIR — KANITLANDI, SILME PAKETI 1 ADAYI
- `.github/workflows/final-apk.yml`
  - duplicates debug APK assembly already covered by `android.yml` and `current-apk-build.yml`;
  - does not run unit tests;
  - only triggers when its own workflow file changes or on pull requests;
  - does not provide a unique release/signing/publication function.
  - Removal is therefore low-risk, but must still pass CI before merge.

### SUPHELI — SILME
- Any old render/UI source path not yet proven unreachable from the active navigation graph.
- Any temporary-looking script referenced by a workflow or build trigger.
- Purchased source asset archives/packages and licensing/provenance documents.
- Avatar standardization script/debug support until the canonical avatar patch is committed directly into production source and the user validates the APK on-device.

## Next packages
1. Package 1: retire redundant `final-apk.yml`; run CI/regression.
2. Audit remaining overlapping APK workflows and old validation workflows by unique function and trigger.
3. Map active Compose navigation/render entry points before touching old UI files.
4. Audit temporary scripts only after checking all workflow/build references.
