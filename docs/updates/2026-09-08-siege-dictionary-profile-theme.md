# 2026-09-08 — Kuşatma, ana sözlük ve profil ana tema paketi

## Kapsam

- Kelime Kuşatması alıştırma ekranında telefonlarda üst/alt chrome sıkıştırıldı; 15×15 tahta daha fazla dikey alan alıyor.
- Ana sözlük istemci normalizasyonu sunucuyla Unicode NFC bakımından eşitlendi.
- Kalıcı sözlük snapshot'ı çevrimiçi olduğunda artık her açılışta otoritatif snapshot ile yenileniyor; eski cache kalıcı otorite değil.
- `get_dictionary_snapshot_v3` 15×15 Kuşatma tahtasıyla parite için 2..15 harf aralığına çıkarıldı.
- Profil ekranına `TEMALARIM` eklendi. Yerleşik mavi-beyaz Son Harf teması ücretsiz ve daima seçilebilir; satın alınmış Gece Arenası da aynı bölümden değiştirilebilir.
- Ana temaya dönüş ücretli/sahte mağaza ürünü oluşturmadan `equip_default_game_theme()` ile `game_theme_id = null` yapılarak uygulanıyor.

## Sözlük inceleme notu

Kullanıcı ekran görüntüsündeki `İŞLEK` kelimesi canlı Türkçe ana sözlükte mevcut ve oyun-uygun. Görüntüde son kelime `İFTİRA` olduğu için beklenen başlangıç harfi `A`; `İŞLEK` bu durumda sözlükten değil `wrong_start_letter` kuralından reddedilir.

Canlı veri incelemesinde `(language, normalized_word)` düzeyinde yinelenen kayıt bulunmadı. Migration sonrası oyun snapshot'ı Türkçede 315.288, İngilizcede 51.028 aktif/oyun-uygun kelime kapsıyor.

## Korunan alanlar

Skor, alan ele geçirme, bot hamle mantığı, klasik düello sunucu otoritesi, mağaza fiyatları, satın alma ve pay-to-win kuralları değiştirilmedi.
