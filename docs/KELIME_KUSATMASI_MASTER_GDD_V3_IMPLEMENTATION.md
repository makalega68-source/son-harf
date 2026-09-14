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

### 5. Skor ve alan kuralları
Mevcut doğrulanmış sözleşme korunur:
- Kelime puanı kalıcı birikimdir.
- Sahip olunan her küp 2 bölge puanıdır.
- Rakip bir küpü geri aldığında yalnız o küpün 2 bölge puanı kaybedilir; geçmiş kelime puanı geri alınmaz.
- Maç yüzeyinde kelime ve bölge puanı ayrı kaynaklar olarak korunur.

### 6. Sosyal/kulüp
Mevcut kulüp, arkadaş/rakip, çevrimiçi durum, rövanş ve rekabet altyapısı yeniden yazılmaz. GDD navigasyonu bu mevcut yüzeylere bağlanır. Güvenlik/RLS ve moderasyon sınırları backend tarafında korunur.

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

Bu uygulama paketi mevcut oyun motorunu, sözlük v5 sözleşmesini, online eşleşmeyi, bot/maç akışını, güvenli backend doğrulamasını veya Supabase şemasını topluca yeniden yazmaz. GDD ile zaten uyumlu çalışan sistemler korunur; değişiklikler marka, ürün hiyerarşisi, navigasyon, görsel tokenlar, dokümantasyon ve bunları kilitleyen testlerle sınırlı tutulur.
