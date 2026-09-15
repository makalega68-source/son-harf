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
- Tam ekran sohbet mevcut `getClubMessages` / `sendClubMessage` backend sözleşmesini kullanır; mesaj girişi 300 karakterle sınırlandırılır ve canlı mesajlar mevcut polling sözleşmesiyle yenilenir.
- İstemci katmanında gönderimler arasında 1,5 saniyelik bekleme bulunur; ancak bu tek güvenlik katmanı değildir.
- Yeni `20260915061500_club_chat_server_guard_v2.sql` migration'ı `club_messages` INSERT işlemlerine otoritatif sunucu guard'ı ekler. Guard; `auth.uid()` ile gönderici eşleşmesini, aktif kulüp üyeliğini ve 1–300 karakter sınırını tekrar doğrular; aynı kulüp/üye yazımlarını transaction advisory lock ile seri hale getirir.
- Sunucu anti-spam politikası: mesajlar arasında en az 1,5 saniye, 30 saniyede en fazla 8 mesaj ve aynı normalize mesajın 30 saniye içinde tekrar gönderilmemesi. Mesaj gövdesi ve zaman damgası sunucuda normalize edilir; değiştirilmiş istemci bu pencereleri kendi `created_at` değeriyle atlayamaz.
- Mevcut RLS açık kalır; SELECT yalnız kulüp üyelerine, INSERT ise üyelik/gönderici/uzunluk şartlarına bağlıdır. Trigger RLS'nin yerine geçmez, ek savunma katmanıdır.
- Kulüp merkezi, üyeler, görevler, sıralama ve kulüpler arası meydan okuma yüzeyleri mevcut `CompetitionHubScreen` üzerinden erişilebilir kalır.
- Kaynak GDD belirli bir yasaklı kelime listesi veya insan moderasyon akışı tanımlamadığı için keyfi içerik sansürü eklenmez. Bu sürümde “spam/abuse moderation” otomatik hız, burst ve tekrar kötüye kullanımını sunucuda engelleyen guard ile uygulanır; ileride rapor/engel veya insan moderasyonu tasarlanırsa ayrı ürün ve güvenlik sözleşmesiyle eklenmelidir.

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

Bu uygulama paketi mevcut oyun motorunu, sözlük v5 sözleşmesini, online eşleşmeyi, bot/maç akışını veya güvenli backend doğrulamasını topluca yeniden yazmaz. Supabase tarafında yalnız Master GDD ile çeliştiği doğrulanan otoritatif kazanan hesabı ve kulüp sohbetinin eksik sunucu anti-spam katmanı ileri yönlü migration'larla tamamlanır. Diğer çalışan sistemler korunur; değişiklikler marka, ürün hiyerarşisi, navigasyon, kulüp sohbet yüzeyi, görsel tokenlar, zafer kuralı, dokümantasyon ve bunları kilitleyen testlerle sınırlı tutulur.
