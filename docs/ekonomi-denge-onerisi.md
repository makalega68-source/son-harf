# Son Coin ekonomi denetimi ve denge önerisi (onay bekliyor)

Kaynak: canlı Supabase fonksiyonları ve `shop_items` tablosu (2026-09-29), uygulamanın çağırdığı RPC'ler.
Bu belgede **hiçbir rakam henüz değiştirilmedi**.

## 1. Son Coin kaynakları (canlı)

| Kaynak | Sunucu fonksiyonu | Miktar | Tekrar | Teorik max/gün | Farm riski | Karar |
|---|---|---|---|---|---|---|
| Günlük giriş | `claim_daily_checkin_v1` | 15 (PRO 30) | 1/gün | 15 | yok | sunucu |
| 7 günlük döngü (uygulama kullanmıyor) | `claim_daily_reward_cycle_v1` | 30…150 (PRO ×2) | 1/gün | 150 | — | sunucu |
| Günlük meydan okuma (3 maç) | `claim_daily_challenge_v1` | 30 | 1/gün | 30 | AI ve özel oda maçları da sayılıyor; kayıt defterine yazılmıyor | sunucu |
| Birleşik görevler (günlük) | `claim_unified_mission_v1` | 4 + 6 + 10 | 1/gün | 20 | öne çıkan görev eski modları (Kelime Arenası, Bil Bakalım, Takım Arenası) istiyor | sunucu |
| Birleşik görevler (haftalık) | aynı | 8×6 + 10 + 35 | 1/hafta | ~13/gün | düşük | sunucu |
| Haftalık hedefler | `claim_goal_v1` | 45+30+40+40+25 = 180 (PRO ×2) | 1/hafta | ~26/gün | `streak_5` ömür boyu en iyi seriye bakıyor → her hafta bedava 25 | sunucu |
| Kelime Avı bulmacası | `submit_daily_cipher_guess_v1` | 35 | **dil başına** 1/gün | 70 | TR + EN ile günde iki kez | sunucu |
| Ödüllü reklam (mağaza) | `claim_store_rewarded_ad_v1` (AdMob SSV) | 10 | 3/gün | 30 | yok (SSV doğrulamalı) | sunucu |
| **Kumbara** | `open_piggy_bank_v2` | 8 maçta 80 (maç başı 10) | **sınırsız** | sınırsız | **en büyük açık**: AI maçları da sayılıyor, açınca sıfırlanıyor, günlük limit yok | sunucu |
| Haftalık turnuva | `claim_previous_weekly_tournament_reward_v1` | 10…250 | 1/hafta | ~36 | düşük | sunucu |
| Atölye haftalık | `claim_atelier_weekly_reward_v1` | ilk 10: 15…100 | 1/hafta/dil | ~14 | düşük | sunucu |
| Ustalık yolu | `claim_mastery_reward_v1` | toplam 1.080 | bir kez | — | yok | sunucu |
| Başarımlar | `sync_achievement_unlocks_v1` | toplam 810 | bir kez | — | yok | sunucu |
| Geri dönüş hediyesi | `touch_presence_v1` | 30/50 | 14 günde 1 | — | yok | sunucu |
| PRO aylık | `claim_vip_monthly_diamonds` | 400 | 1/ay | ~13 | PRO ekonomik avantajı | sunucu |
| Sezon / kulüp / rekabet sezonu | `claim_season_reward_v1`, `claim_club_weekly_mission_v1`, `claim_competitive_season_reward_v1` | 25…150 | dönemsel | düşük | düşük | sunucu |
| Gerçek para paketleri | `apply_verified_play_purchase_v2` | 500 / 1.500 / 3.500 / 8.000 | satın alma | — | Play doğrulamalı | sunucu |
| Yönetici / moderatör | `admin_*`, `grant_moderator_access_internal_v1` | — | — | — | yalnız yetkili hesaplar | sunucu |

- **Maçların kendisi coin vermiyor.** `claim_match_result_v10` sunucuda var ama uygulama çağırmıyor.
- Ana oyun Kelime Kuşatması kumbaraya da sayılmıyor, yani hiç coin kazandırmıyor.
- Client tarafında coin ekleyen bir karar yok. Satın alma fiyatı sunucudaki tablodan okunuyor.

## 2. Mevcut günlük gelir (simülasyon)

| Tip | Günlük | 7 gün | 30 gün | 90 gün | 180 gün |
|---|---|---|---|---|---|
| Casual (15–20 dk) | 126 | 885 | 3.793 | 11.379 | 22.757 |
| Normal (30–45 dk) | 173 | 1.211 | 5.188 | 15.565 | 31.130 |
| Active (60–90 dk) | 245 | 1.715 | 7.351 | 22.053 | 44.106 |
| Hardcore (2+ saat) | 443 | 3.104 | 13.304 | 39.911 | 79.822 |

Mağazada coinle alınan **23 ürünün toplamı 6.300 SC**:
- Normal oyuncu bütün mağazayı **~36 günde**, hardcore oyuncu **~14 günde** bitiriyor.
- En pahalı ürün 600 SC; hardcore oyuncu bunu 2 günde alıyor.

## 3. Önerilen kaynak ayarları

| Kaynak | Eski | Yeni |
|---|---|---|
| Günlük giriş | 15 düz | 7 günlük seri: 5, 5, 10, 10, 15, 15, 20 + 7. gün 24 saatlik stil denemesi |
| Günlük meydan okuma | 30 | 15. Yalnız gerçek oyuncu ve eşleşme maçları sayılır, kayıt defterine yazılır |
| Birleşik günlük görevler | 4 / 6 / 10 | 3 / 5 / 8. Öne çıkan görev yalnız mevcut modlar: Kuşatma, Son Harf, Atölye, Kelime Avı |
| Kelime Avı | 35 × dil | 10, günde bir kez (dil fark etmez) |
| Ödüllü reklam | 3 × 10 | 3 × 5 (günlük ekonominin ~%15'i) |
| Kumbara | maç başı 10, sınırsız | günde 1 açılış; 2 maç 15, 4 maç 25, 6 maç 35, 8 maç 40. AI alıştırma sayılmaz; **Kuşatma çevrim içi maçları sayılır** |
| Haftalık hedefler | 180/hafta | 60/hafta; `streak_5` bir kez |
| Haftalık turnuva, Atölye, başarım, ustalık | aynı | aynı (rekabet ödülü) |
| PRO çarpanları | giriş ×2, hedefler ×2, aylık 400 | ×2 kaldırılır; aylık 400 → 150 (karar sizin) |

Coin azalınca da ilerleme devam eder: rating, lig, başarımlar, sezon, görev ilerlemesi ve istatistikler maç sayısıyla sınırlı değildir.

### Yeni simülasyon

| Tip | Günlük | 7 gün | 30 gün | 90 gün | 180 gün |
|---|---|---|---|---|---|
| Casual | ~62 | ~435 | ~1.860 | ~5.600 | ~11.200 |
| Normal | ~85 | ~600 | ~2.560 | ~7.700 | ~15.400 |
| Active | ~112 | ~785 | ~3.360 | ~10.100 | ~20.200 |
| Hardcore | ~126 | ~880 | ~3.780 | ~11.300 | ~22.700 |

Hardcore oyuncunun geliri artık lineer büyümüyor: kumbara ve görevlerin günlük tavanı var.

## 4. Fiyat tablosu (normal oyuncu ≈ 85 SC/gün)

| Ürün | Kategori | Eski nadirlik | Yeni nadirlik | Eski | Yeni | Hedef gün | Ek şart |
|---|---|---|---|---|---|---|---|
| frame_round_starter_blue | Çerçeve | STANDARD | Başlangıç | 60 | 150 | 2 | — |
| frame_round_starter_neutral | Çerçeve | STANDARD | Başlangıç | 60 | 150 | 2 | — |
| frame_round_pearl | Çerçeve | STANDARD | Başlangıç | 80 | 250 | 3 | — |
| frame_round_botanic / ocean / rose / lilac | Çerçeve | RARE | Sıradan | 150 | 550 | 6–7 | — |
| keyboard_premium_white | Klavye | STANDARD | Sıradan | 210 | 500 | 6 | — |
| keyboard_midnight | Klavye | STANDARD | Sıradan | 220 | 600 | 7 | — |
| keyboard_black_gold | Klavye | STANDARD | Ender | 240 | 1.500 | 18 | — |
| keyboard_crystal | Klavye | STANDARD | Ender | 360 | 1.700 | 20 | — |
| keyboard_obsidian | Klavye | STANDARD | Destansı | 420 | 3.600 | 42 | — |
| name_cyan | İsim stili | STANDARD | Sıradan | 220 | 500 | 6 | — |
| name_sapphire | İsim stili | STANDARD | Sıradan | 260 | 650 | 8 | — |
| name_amethyst | İsim stili | STANDARD | Ender | 280 | 1.500 | 18 | — |
| name_aurelia | İsim stili | STANDARD | Ender | 340 | 1.900 | 22 | — |
| hat_beret | Maskot şapkası | RARE | Sıradan | 250 | 650 | 8 | — |
| hat_flower | Maskot şapkası | RARE | Ender | 300 | 1.600 | 19 | — |
| hat_wizard | Maskot şapkası | EPIC | Destansı | 350 | 3.800 | 45 | — |
| hat_top | Maskot şapkası | EPIC | Destansı | 400 | 4.200 | 49 | — |
| theme_walnut_ivory | Oyun teması | STANDARD | Destansı | 600 | 4.000 | 47 | — |
| theme_black | Oyun teması | STANDARD | Destansı | 600 | 4.000 | 47 | — |
| victory_crown | Zafer efekti | EPIC | Efsanevi | 450 | 8.000 | 94 | 50 galibiyet |

- Mağaza toplamı 6.300 SC'den **41.450 SC**'ye çıkar. Normal oyuncu için ~16 ay, hardcore için ~11 ay.
- Nadirlik dağılımı: Başlangıç 3, Sıradan 9, Ender 5, Destansı 5, Efsanevi 1.

### Prestij

Şu an prestij ürünü yok. Önerilen şart modeli coin, galibiyet (`profiles.wins`), rating (`profiles.rating`) ve sezon onuru (`season_honors`) birleşimidir. Örnek: 12.000 SC + 100 galibiyet + 1500 rating veya sezon şampiyonluğu.

- Satın alınan coin bu şartları aşamaz.
- Yeni prestij ürünleri için görsel gerekir; mevcut ürünlerden birine atanabilir.

## 5. Güvenlik bulguları

1. Kumbara sınırsız ve AI maçlarını sayıyor. En büyük farm açığı.
2. Kelime Avı dil başına ödül veriyor, yani günde iki kez.
3. `streak_5` haftalık hedefi ömür boyu metriğe bakıyor; her hafta tekrar alınabiliyor.
4. `claim_daily_challenge_v1` kayıt defterine yazmıyor, AI ve özel oda maçlarını sayıyor.
5. İyi durumda olanlar:
   - Satın alma sunucu fiyatıyla yapılıyor.
   - Ödüller benzersiz anahtarla tek sefer (`on conflict`).
   - Reklam AdMob SSV ile doğrulanıyor.
   - Envanter ve bakiye yazımı yalnız `security definer` fonksiyonlarda.

## 6. Mevcut oyuncular

- Oyuncuların medyan bakiyesi 250 SC.
- Kimsenin bakiyesi silinmez, sahip olunan ürünler kalır.
- Yüksek bakiyeli 3 hesap sahip ve moderatör hesabı.
