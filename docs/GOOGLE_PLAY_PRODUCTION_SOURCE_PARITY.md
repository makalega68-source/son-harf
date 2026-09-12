# Google Play Production Source Parity — 2026-09-12

Canlı Supabase proje: `bzdtftzdjtjoqhtcqtxb`.

Bu belge, Google Play satın alma otoritesinin production'da çalışan sürümleri ile GitHub kaynaklarını eşler. Bu parity paketi canlı sistemi değiştirmez; yalnız production'da zaten çalışan kaynakları repoya geri getirir.

## Doğrulanmış production durumu

- `verify-play-purchase`: ACTIVE, version 5, `verify_jwt=true`.
- `google-play-rtdn`: ACTIVE, version 1, `verify_jwt=false`; fonksiyon kendi `GOOGLE_PLAY_RTDN_SECRET` / `X-Son-Harf-RTDN-Secret` kontrolünü uygular.
- Migration `20260904083134_play_entitlement_reconciliation` canlı migration geçmişinde uygulanmıştır.
- `apply_verified_play_purchase_v2(...)` canlıda vardır ve yalnız `service_role` tarafından çalıştırılabilir.
- `reconcile_play_entitlement_v1(...)` canlıda vardır ve yalnız `service_role` tarafından çalıştırılabilir.

## Kaynak eşleme

- `supabase/migrations/20260904083134_play_entitlement_reconciliation.sql`: canlıda uygulanmış migration'ın kaynak kopyası.
- `supabase/functions/verify-play-purchase/index.ts`: production v5 kaynak davranışı; Play API doğrulaması yapar, ürünün katalogda etkinliğini kontrol eder ve `apply_verified_play_purchase_v2` çağırır.
- `supabase/functions/google-play-rtdn/index.ts`: production v1 kaynak davranışı; custom secret ile push isteğini doğrular, subscription ve one-time bildirimlerini `reconcile_play_entitlement_v1` ile işler.

## Güvenlik sınırı

Bu parity kaydı, mevcut production RTDN v1 tasarımının ideal son durum olduğu anlamına gelmez. Audit sırasında şu iyileştirme ihtiyacı ayrıca doğrulandı:

1. RTDN v1 Google Pub/Sub push kimliğini OIDC token/audience ile doğrulamıyor; custom shared-secret kullanıyor.
2. One-time ürün iptal/refund bildirimi `reconcile_play_entitlement_v1` üzerinden purchase durumunu güncellese de daha önce verilmiş Son Coin/style grant'lerini geri almıyor.
3. Eski PR #269 içinde OIDC push identity, event dedupe ve idempotent one-time clawback için bir taslak bulunuyor; bu SQL production'da uygulanmış değildir ve staging/backup doğrulaması olmadan merge/deploy edilmemelidir.

Bu nedenle sonraki güvenlik paketi ayrı ele alınmalıdır: `play_purchase_grants` provenance tablosu + `reconcile_play_entitlement_v2` + deduplicated RTDN event ledger + OIDC push authentication. Bu değişiklik production'a doğrudan uygulanmamalı; önce izole Supabase development branch/staging üzerinde migration, refund idempotency, duplicate Pub/Sub delivery ve rollback senaryoları doğrulanmalıdır.

Son doğrulama: 12 Eylül 2026. Bu audit/parity çalışmasında canlı Supabase'e DDL veya data mutation yapılmadı.
