# Live Supabase Migration Ledger — 2026-09-12

Canlı proje: `bzdtftzdjtjoqhtcqtxb` (`son-harf`).

Bu belge, GitHub kaynak migration'ları ile canlı `supabase_migrations.schema_migrations` geçmişi arasındaki provenance eşlemesini tutar. `apply_migration` ile oluşturulan production version timestamp'leri her zaman kaynak dosya adıyla aynı değildir. Bu nedenle timestamp farkı tek başına migration'ın eksik veya uygulanmamış olduğu anlamına gelmez.

## Güvenlik kuralı

- `docs/live-supabase-history/` yalnız audit snapshot alanıdır; SQL buradan çalıştırılmaz.
- Eski production version'larını `supabase/migrations/` altına körlemesine eklemek yasaktır. Önce güncel fonksiyon tanımı ve daha yeni migration zinciri kontrol edilir.
- Canlı migration geçmişi yeniden yazılmaz ve eski migration replay edilmez.
- Yeni şema değişikliği gerekiyorsa yeni bir ileri migration hazırlanır.
- Audit snapshot'ın tarihsel davranış göstermesi, o davranışın bugün geçerli olduğu anlamına gelmez. Güncel executable migration zinciri ve canlı fonksiyon tanımları runtime source-of-truth'tur.

## 4 Eylül 2026 Store/VIP production provenance — #344

PR #243'teki authored migration timestamp'leri production'da kullanılan version timestamp'leri değildir. Aşağıdaki sekiz kayıt doğrudan canlı `supabase_migrations.schema_migrations.statements` gövdelerinden alınmıştır. Snapshot gövde MD5'leri production kaydıyla kontrat testinde birebir doğrulanır.

| Canlı version | Canlı ad | Eski #243 authored dosyası | Live statement MD5 | Audit snapshot / durum |
|---|---|---|---|---|
| `20260904083102` | `store_vip_production_hardening` | `20260904090000_store_vip_production_hardening.sql` | `90a87a9481c9313b9cc3760b71fba46f` | `docs/live-supabase-history/20260904083102_store_vip_production_hardening.sql` — audit-only. |
| `20260904083238` | `reward_center_v8` | `20260904091000_reward_center_v8.sql` | `435312e09b3ef3c193f11453aec886e4` | `docs/live-supabase-history/20260904083238_reward_center_v8.sql` — audit-only. Güncel Android Reward Center #346 ile canlı RPC yüzeyine hizalandı. |
| `20260904083258` | `style_trial_direct` | `20260904091500_style_trial_direct.sql` | `85a400fd59d26323ffacc7f31c212395` | `docs/live-supabase-history/20260904083258_style_trial_direct.sql` — audit-only. |
| `20260904083323` | `word_siege_area_score_authority` | `20260904092000_word_siege_area_score_authority.sql` | `840ddd901b45c51eda90302f28d70f76` | `docs/live-supabase-history/20260904083323_word_siege_area_score_authority.sql` — tarihsel 9×9/81-cell preview ve alan skoru dönemi; güncel Kelime Tahtı v8/v9 veya #334 scoring source-of-truth değildir. |
| `20260904083343` | `vip_social_authority` | `20260904092500_vip_social_authority.sql` | `ce8ad8375a3fd0c955d831a7059b509f` | `docs/live-supabase-history/20260904083343_vip_social_authority.sql` — audit-only. |
| `20260904083438` | `vip_match_analysis` | `20260904093000_vip_match_analysis.sql` | `3d3c70b73a8b9feb3bd4fdc0cdefac12` | `docs/live-supabase-history/20260904083438_vip_match_analysis.sql` — audit-only. Güncel ürün kararı: Premium analiz yalnız maç sonrası; eski live/in-match assist geri getirilmez. |
| `20260904083620` | `season_store_tracks` | `20260904093500_season_store_tracks.sql` | `7b32bf574b9f835001bfcbf8a9c812fa` | `docs/live-supabase-history/20260904083620_season_store_tracks.sql` — audit-only; dinamik Season Center successor işi #347. |
| `20260904083637` | `season_style_equip_hardening` | `20260904094000_season_style_equip_hardening.sql` | `ecc32b097486e24a34b46ce927fd872c` | `docs/live-supabase-history/20260904083637_season_style_equip_hardening.sql` — audit-only; daha yeni permanent Style ownership zinciri önceliklidir. |

Aynı eski PR zincirindeki Google Play reconciliation ayrı ele alınmıştır: production `20260904083134_play_entitlement_reconciliation` ↔ güncel executable kaynak `supabase/migrations/20260904083134_play_entitlement_reconciliation.sql` (#339). Eski `20260904090500_play_entitlement_reconciliation.sql` geri getirilmez.

Bu authored dosyaların hiçbiri (`090000`, `090500`, `091000`, `091500`, `092000`, `092500`, `093000`, `093500`, `094000`) executable migration olarak geri eklenmemelidir. Tarihsel içerik yalnız audit alanında gerçek production version adıyla tutulur.

## 6–11 Eylül 2026 production eşlemesi

| Canlı version | Canlı ad | Güncel repo kaynağı / durum |
|---|---|---|
| `20260906110600` | `store_ready_style_catalog` | `supabase/migrations/20260906113000_store_ready_style_catalog.sql` |
| `20260906114237` | `restore_single_verified_theme_catalog` | Audit-only production hotfix snapshot. Güncel katalogda `theme_aurora` kaynak ürünü yok. |
| `20260906132054` | `exact_duel_scoring_action_feedback` | `supabase/migrations/20260906_exact_duel_scoring_action_feedback.sql` |
| `20260906160322` | `core_duel_bot_server_authority_v5` | Audit-only historical bot wrapper snapshot; daha yeni zincir tarafından superseded. |
| `20260906160407` | `core_duel_existing_rpc_bot_autocontinue` | Audit-only historical snapshot. Eski PR #267 bunun farklı timestamp'li kopyasıydı; güncel davranış değildir. |
| `20260906173209` | `store_ready_style_catalog` | Aynı katalog kaynağının production tekrar uygulaması; kaynak `20260906113000_store_ready_style_catalog.sql`. |
| `20260907081246` | `harden_anonymous_rpc_access_20260907` | `supabase/migrations/20260907090000_harden_anonymous_rpc_access.sql` |
| `20260907095809` | `remove_bilbakalim_and_release_bot_submit` | `supabase/migrations/20260907103000_remove_bilbakalim_and_release_bot_submit.sql` |
| `20260907104346` | `remove_bilbakalim_and_release_bot_submit` | Aynı production SQL ikinci kez uygulanmış; güncel kaynak yine `20260907103000_remove_bilbakalim_and_release_bot_submit.sql`. |
| `20260907200345` | `fix_midmatch_quiz_longword_steal` | Audit-only production hotfix snapshot; parçaları sonraki duel zinciri tarafından superseded olsa da geçmiş korunur. |
| `20260908085328` | `dictionary_board_parity_and_default_theme` | `supabase/migrations/20260908114500_dictionary_board_parity_and_default_theme.sql` |
| `20260908102724` | `premier_duel_dictionary_v4` | İlk SECURITY DEFINER production sürümü; hemen sonraki invoker hardening ile superseded. Güncel kaynak `supabase/migrations/20260908_premier_duel_dictionary_v4.sql`. |
| `20260908102820` | `premier_duel_dictionary_v4_invoker_hardening` | Güncel `20260908_premier_duel_dictionary_v4.sql` içinde final SECURITY INVOKER davranışı birleşik halde. |
| `20260908120313` | `permanent_style_ownership` | `supabase/migrations/20260908120313_permanent_style_ownership.sql` — production version ile birebir kayıtlı. |
| `20260908155955` | `unified_pro_boosters_and_turn20` | `supabase/migrations/20260908155955_unified_pro_boosters_and_turn20.sql` — birebir. |
| `20260908163019` | `unified_pro_booster_index_hardening` | `supabase/migrations/20260908163019_unified_pro_booster_index_hardening.sql` — birebir. |
| `20260909062231` | `word_siege_current_territory_score_v5` | `supabase/migrations/20260909090000_word_siege_current_territory_score_v5.sql` |
| `20260909063437` | `revoke_legacy_anon_security_definer` | `supabase/migrations/20260909094500_revoke_legacy_anon_security_definer.sql` |
| `20260909070110` | `word_siege_15x15_server_parity_v6` | `supabase/migrations/20260909101500_word_siege_15x15_server_parity_v6.sql` |
| `20260909123826` | `word_siege_star_4k_board_v7` | `supabase/migrations/20260909145500_word_siege_star_4k_board_v7.sql` |
| `20260909124004` | `restore_premier_fair_play_v1` | `supabase/migrations/20260909113000_restore_premier_fair_play_v1.sql` |
| `20260910071515` | `fix_premier_bot_timeout` | `supabase/migrations/20260910060000_fix_premier_bot_timeout.sql` |
| `20260910074415` | `speed_up_bot_turn_and_resume` | `supabase/migrations/20260910080000_speed_up_bot_turn_and_resume.sql` |
| `20260910102251` | `premier_server_clock` | `supabase/migrations/20260910103000_premier_server_clock.sql` |
| `20260910113149` | `premier_turn_15_seconds` | `supabase/migrations/20260910142500_premier_turn_15_seconds.sql` |
| `20260911150233` | `word_siege_custom_topology_v8` | `supabase/migrations/20260911170000_word_siege_custom_topology_v8.sql` |
| `20260911150315` | `word_siege_custom_topology_v8_private_acl_fix` | ACL hotfix güncel v8 kaynak dosyasına entegre: `revoke all on function private.word_siege_new_board_v1() ...`. |
| `20260911150455` | `word_siege_friend_invites_v9` | `supabase/migrations/20260911173000_word_siege_friend_invites_v9.sql` |
| `20260911151543` | `word_siege_friend_invite_expiry_fix_v9_1` | Expiry fix güncel v9 kaynak dosyasına entegre (`expires_at < now()` → `expired` → `return null`). |
| `20260911183652` | `word_siege_matchmaking_expiry_v1` | `supabase/migrations/20260911190000_word_siege_matchmaking_expiry_v1.sql` |

## 12 Eylül 2026 ileri migration

| Canlı version | Canlı ad | Güncel repo kaynağı / durum |
|---|---|---|
| `20260912090721` | `shop_sale_window_purchase_enforcement` | `supabase/migrations/20260912103000_shop_sale_window_purchase_enforcement.sql` — #350 ile merge edildi ve production'da uygulandı. `purchase_shop_item(text)` katalog satış penceresini server-side zorlar; owner/admin/VIP/bakiye semantiği korunur. |

## #267 kararı

PR #267 (`20260906190500_core_duel_server_authoritative_bot_rpc.sql`) doğrudan merge edilmemelidir.

Neden:

1. Canlı migration geçmişinde `20260906190500` yoktur. Aynı tarihsel işlevin gerçek production sürümleri `20260906160322` ve `20260906160407`'dir.
2. `20260906160407`, `submit_word_v3` içinde bot sırasını senkron auto-continue ediyordu.
3. `20260907095809` ve `20260907104346` bunu bilinçli olarak kaldırdı; güncel `submit_word_v3` yalnız `submit_word_v3_core_v1` çağırır. Böylece kelime doğrulama cevabı bot düşünmesi başlamadan istemciye döner.
4. `claim_turn_timeout` daha sonra `20260910071515_fix_premier_bot_timeout` ile `playing`, `final`, `sudden_death` semantiği, final cezası, bot geçişi ve `final_moves_remaining` davranışı açısından yeniden tanımlandı.
5. PR #267 replay edilirse bu daha yeni production davranışlarının bir kısmını eski sürüme geri götürme riski oluşur.

## Bugünkü doğrulanmış kontratlar

- `submit_word_v3`: server-authoritative core doğrulamasını çağırır; eski senkron bot auto-continue wrapper'ı geri getirilmez.
- `claim_turn_timeout`: `playing`, `final`, `sudden_death` durumlarını destekler; final hamlesini azaltır ve bot eşleşmesi geçişini korur.
- Premier tur süresi 15 saniyedir; trivia/quiz penceresi bundan ayrı tutulur.
- Dictionary V4 fonksiyonlarının bugünkü canlı hali SECURITY INVOKER/default-invoker semantiğindedir ve güncel kaynakla uyumludur.
- Kelime Kuşatması v8 private board constructor doğrudan authenticated client'a açık değildir.
- Kelime Kuşatması v9 süresi dolmuş arkadaş davetini kabul etmez; daveti `expired` olarak kapatır.
- Store satın alma RPC'si yalnız aktif ve mevcut satış penceresindeki ürünleri kabul eder; anon execute kapalıdır.

Son provenance doğrulama tarihi: 12 Eylül 2026. #344 Store/VIP provenance auditi production'a DDL veya data mutation yapmaz. Aynı gün ayrı #343 işi kapsamında uygulanan `20260912090721_shop_sale_window_purchase_enforcement` ileri migration'ı yukarıda ayrıca kayıtlıdır.
