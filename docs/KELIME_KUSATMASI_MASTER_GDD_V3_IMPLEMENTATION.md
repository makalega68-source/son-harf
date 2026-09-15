# Kelime Kuşatması Master GDD v3.0 — Uygulama İzleme Belgesi

Bu belge, 2026-09-15 tarihli Master GDD v3.0 talimatının mevcut çalışan Android/Supabase yapısına nasıl uygulandığını izler. Ana ilke: çalışan oyun sözleşmelerini, backend kimliklerini ve regresyon kilitlerini bozmadan kullanıcı görünür ürün hiyerarşisini Kelime Kuşatması etrafında birleştirmek.

## Uygulanan kararlar

### 1. Ana ürün ve marka
- Kullanıcı görünür uygulama adı: **Kelime Kuşatması**.
- Ana ürün kimliği: **kelime oyunu + taktik alan savaşı + sosyal rekabet**.
- Eski `Kelime Tahtı` metinleri, dokunulmamış uyumluluk yüzeylerinde ortak yerelleştirme katmanında Kelime Kuşatması olarak normalize edilir.
- `applicationId`, Kotlin package adları, `WordSiege` sınıf adları, `word_siege` Supabase nesneleri ve `sonharf://auth` deep-link şeması değiştirilmez.

### 2. Ana ekran hiyerarşisi
- En baskın kart Kelime Kuşatması ve doğrudan savaş CTA'sıdır.
- Son Harf ve Harf Yolu ana kartın altında ikincil oyunlar olarak gösterilir.
- Alt navigasyon: **Ana Sayfa · Kulüp · Arkadaşlar · Mağaza · Profil**.
- Profil, Son Coin ve PRO durumu ana ekran üst alanında görünür kalır.
- Bildirim zili mevcut sosyal aktivite/davet merkezine bağlanır; kaynakta ayrı bir notification backend'i bulunmadığı için sahte veya paralel veri katmanı oluşturulmaz.
- Günlük hedef/rekabet yüzeyi ana ekranda retention giriş noktası olarak korunur.

### 3. Görsel sistem
Master GDD paleti semantik token olarak `SonHarfTheme.kt` içine alınmıştır:
- Sage Green `#8A9A86`
- Off-White `#F9F8F6`
- Soft Blue `#7A9AEE`
- Turquoise `#40E0D0`
- Light Beige `#F2EFE9`
- Lavender `#B5A2FF`
- Slate Blue `#5C6F84`
- Pale Mint `#A3E4D7`
- Controlled Warm Accent `#E07A5F`

Ambient yüzeyler GDD tokenlarını kullanır. Ana aksiyon rengi erişilebilir kontrastı korumak için daha koyu semantik bir tonda tutulur.

### 4. Oyun modu mimarisi
- Kelime Kuşatması ana moddur.
- Son Harf hızlı ikinci moddur.
- Harf Yolu doğrudan ana ekrandan erişilen üçüncü moddur.
- Her mod mevcut TR/EN seçim sözleşmesini korur.

### 5. Skor, alan ve zafer kuralları
- Kelime puanı kalıcı birikimdir.
- Sahip olunan her küp 2 bölge puanıdır.
- Rakip bir küpü geri aldığında yalnız o küpün 2 bölge puanı kaybedilir; geçmiş kelime puanı geri alınmaz.
- Maç yüzeyinde kelime ve bölge puanı ayrı kaynaklar olarak korunur; toplam skor açıklayıcı performans metriği olarak gösterilebilir.
- Master GDD v3 gereği normal maç kazananı **yalnız maç bittiği andaki bölge hâkimiyetiyle** belirlenir. Kelime puanı veya toplam skor gizli tie-break olarak kullanılmaz.
- Bölge sayısı eşitse normal maç berabere biter.
- Pes/forfeit özel durumunda rakip doğrudan kazanır; bu güvenli ve mevcut davranış korunur.
- Otoritatif online kural yeni `20260915060000_word_siege_territory_victory_v10.sql` migration'ında, aynı kural bot/alıştırma tarafında `WordSiegePracticeEngine` içinde uygulanır.
- GDD “grid saturation or time expiry” der; kaynak herhangi bir global maç süresi belirtmez. Bu nedenle keyfi bir dakika değeri uydurulmamıştır. Mevcut güvenli bitiş tetikleri (ör. torba/rack tükenmesi ve ardışık pas) korunur; gelecekte zamanlı Kelime Kuşatması varyantı `finish_word_siege_game_v1` üzerinden bittiğinde aynı bölge-hâkimiyeti zafer kuralını otomatik kullanır.

### 6. Sosyal/kulüp
- Mevcut kulüp, arkadaş/rakip, çevrimiçi durum, rövanş ve rekabet altyapısı yeniden yazılmaz.
- Alt menüdeki **Kulüp** girişi `KelimeKusatmasiClubScreen` üzerinden ayrı tam ekran sohbet deneyimine açılır.
- Tam ekran sohbet mevcut `getClubMessages` / `sendClubMessage` backend sözleşmesini kullanır; mesaj girişi 300 karakterle sınırlandırılır, gönderimler arasında 1,5 saniyelik istemci anti-spam bekleme uygulanır ve canlı mesajlar mevcut polling sözleşmesiyle yenilenir.
- Mevcut gönderim sözleşmesi kulüp üyeliğini doğrular; istemci üyelik kontrolünü atlatan paralel bir mesaj yolu oluşturmaz.
- Kulüp merkezi, üyeler, görevler, sıralama ve kulüpler arası meydan okuma yüzeyleri mevcut `CompetitionHubScreen` üzerinden erişilebilir kalır.
- Bu entegrasyon kapsamında bağımsız, otoritatif sunucu-side rate-limit/moderasyon katmanı doğrulanmadığı için varmış gibi belgelenmez. Böyle bir güvenlik katmanı ayrıca devreye alınacaksa Supabase/RLS ve abuse politikalarıyla birlikte ayrı backend hardening işi olarak ele alınmalıdır.

### 7. Mağaza, koleksiyon ve çerçeveler
Mevcut profil çerçevesi sözleşmesi GDD katalog yapısıyla uyumludur:
- Starter Blue
- Starter Pink
- Starter Neutral
- Ocean Ring
- Botanic Ring
- Lilac Halo
- Rose Glow
- PRO Golden Avatar erişimi

Backend `vip_*` adlarının kullanıcı görünür karşılığı **PRO** olarak kalır; veritabanı kimlikleri bu marka güncellemesinde değiştirilmez.

### 8. Monetizasyon güvenliği
Pay-to-win yasaktır. Satın alma/abonelik:
- kelime üretimi,
- skor veya rating artışı,
- eşleşme ağırlığı,
- hamle gücü
sağlayamaz. Monetizasyon kozmetik, prestij, reklamsız deneyim, analiz/konfor, Son Coin ve sezon içeriğiyle sınırlıdır.

### 9. Dil kapsamı
Yalnız Türkçe ve İngilizce. Yeni dil eklenmez.

## Regresyon sınırı

Bu uygulama paketi mevcut oyun motorunu, sözlük v5 sözleşmesini, online eşleşmeyi, bot/maç akışını veya güvenli backend doğrulamasını topluca yeniden yazmaz. Supabase tarafında yalnız Master GDD ile çeliştiği doğrulanan otoritatif kazanan hesabı yeni, ileri yönlü migration ile değiştirilir. Diğer çalışan sistemler korunur; değişiklikler marka, ürün hiyerarşisi, navigasyon, kulüp sohbet yüzeyi, görsel tokenlar, zafer kuralı, dokümantasyon ve bunları kilitleyen testlerle sınırlı tutulur.
