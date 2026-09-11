# Live Supabase Migration Ledger — 2026-09-12

Canlı proje: `bzdtftzdjtjoqhtcqtxb` (`son-harf`).

Bu belge, GitHub kaynak migration'ları ile canlı `supabase_migrations.schema_migrations` geçmişi arasındaki provenance eşlemesini tutar. `apply_migration` ile oluşturulan production version timestamp'leri her zaman kaynak dosya adıyla aynı değildir. Bu nedenle timestamp farkı tek başına migration'ın eksik veya uygulanmamış olduğu anlamına gelmez.

## Güvenlik kuralı

- `docs/live-supabase-history/` yalnız audit snapshot alanıdır; SQL buradan çalıştırılmaz.
- Eski production version'larını `supabase/migrations/` altına körlemesine eklemek yasaktır. Önce güncel fonksiyon tanımı ve daha yeni migration zinciri kontrol edilir.
- Canlı migration geçmişi yeniden yazılmaz ve eski migration replay edilmez.
- Yeni şema değişikliği gerekiyorsa yeni bir ileri migration hazırlanır.

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

Son doğrulama tarihi: 12 Eylül 2026. Canlı DB bu audit sırasında yalnız okunmuştur; DDL veya data mutation yapılmamıştır.
