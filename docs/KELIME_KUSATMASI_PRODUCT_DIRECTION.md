# Kelime Kuşatması — Ana Ürün Yönü

## Ürün hiyerarşisi

Kelime Kuşatması uygulamanın ana ürünü, ana markası ve ana oyunudur. Son Harf kısa oturumlu ikinci oyun/mod olarak korunur. Harf Yolu ve diğer yan modlar ana ürünün sosyal, rekabetçi ve retention ekosistemini destekler.

Ana ürün kimliği:

**Kelime Kuşatması = kelime oyunu + taktik alan savaşı + sosyal rekabet**

## Ana oyun döngüsü

1. Harflerden geçerli kelime oluştur.
2. Kelime puanı kazan.
3. Yerleştirme hattı boyunca bölge/küp ele geçir veya mevcut alanı savun.
4. Harita kontrolünü büyüt.
5. Rakibin stratejik alanlarına baskı kur.
6. Lig, rövanş, sosyal rekabet, görev ve ilerleme sistemleriyle tekrar oyna.

Kelime puanı ve bölge puanı birbirinden ayrı izlenir. Toplam skor bu iki bileşenin toplamıdır. Temel kural değişmez: sahip olunan her küp 2 bölge puanıdır; bir küp kaybedildiğinde yalnız o küpün bölge puanı düşer, daha önce kazanılmış kelime puanı geri alınmaz.

## Görsel kimlik

Oyuncu ekrana baktığında önce harita/bölge hâkimiyetini, sonra harfleri algılamalıdır. Klasik Scrabble/Kelimelik tahta ifadesi hedef değildir.

Master GDD v3 paleti:

- Adaçayı yeşili: `#8A9A86`
- Kırık beyaz: `#F9F8F6`
- Yumuşak mavi: `#7A9AEE`
- Turkuaz: `#40E0D0`
- Açık bej: `#F2EFE9`
- Lavanta: `#B5A2FF`
- Gri-mavi: `#5C6F84`
- Soluk mint: `#A3E4D7`
- Kontrollü sıcak vurgu: `#E07A5F`

Harf taşları modern, büyük ve okunaklı olmalıdır. Sahip olunan komşu hücreler mümkün olduğunca tek bölge hissi vermeli; kritik çatışmalar kontrollü sıcak vurgu ile ayrışmalıdır.

## Fikri mülkiyet ayrışması

Kelimelik ve diğer kelime oyunlarının başarılı ürün prensiplerinden öğrenilebilir; ancak ekran, grafik, metin, kod, bonus geometrisi, marka, ikonografi veya özgün ifade biçimi kopyalanmamalıdır.

Yeni maçlarda klasik köşe/çapraz bonus geometrisi yerine Kelime Kuşatması'na özgü taktik alan topolojisi kullanılır. Mevcut 15x15 / 225 hücre veri sözleşmesi geriye dönük uyumluluk için şimdilik korunur. Tahta boyutunun ileride değiştirilmesi ancak backend, kayıtlı maçlar, testler ve migrasyon planı birlikte ele alınarak yapılmalıdır.

## Ana ekran ve navigasyon

- Ana ekranda en baskın kart Kelime Kuşatması'dır ve doğrudan ana savaşı başlatır.
- Son Harf ve Harf Yolu ana kartın altında ikincil modlar olarak doğrudan erişilebilir kalır.
- Alt navigasyon sırası: **Ana Sayfa · Kulüp · Arkadaşlar · Mağaza · Profil**.
- Oyuncu profili, Son Coin bakiyesi ve PRO durumu ana deneyimde görünür kalır.
- Günlük hedef/rekabet yüzeyi ana ekranın retention katmanıdır; ana navigasyonu kalabalıklaştırmaz.

## Sosyal, kulüp ve retention

Kulüp, arkadaş/rakip, rövanş, çevrimiçi durum, geçmiş karşılaşmalar, günlük hedef, lig, görev ve sezon sistemleri ana ürünün geri dönüş döngüsünü destekler. Kulüp sohbeti ayrı tam ekran yüzey olarak kalmalı; spam ve abuse kontrolleri backend sınırlarında uygulanmalıdır.

## Mağaza, koleksiyon ve PRO

Monetizasyon sıfır pay-to-win ilkesine bağlıdır. Satın almalar oyun gücü, kelime üretimi, skor, rating veya eşleşme ağırlığı sağlamaz. Son Coin, kozmetik, sezon içeriği, ödüllü reklam ve PRO aboneliği aynı ekonomik ekosistemde çalışır.

PRO; reklamsız deneyim, prestij/kozmetik, gelişmiş analiz ve konfor özellikleri sağlar. Backend'deki mevcut `isVip`/`vip_*` alanları geriye dönük teknik uyumluluk için korunabilir; kullanıcı görünür ad **PRO**'dur.

Profil çerçeveleri ve satın alınan diğer kozmetikler `Profil > Koleksiyon` üzerinden yönetilir ve donatıldığında maç, liderlik ve sosyal yüzeylerde aynı kullanıcı durumunu göstermelidir.

## UX ilkeleri

- Ana CTA Kelime Kuşatması'nı açar.
- Son Harf ikinci oyun olarak açıkça ayrıştırılır.
- Maç ekranında Kelime Puanı, Bölge Puanı ve Toplam ayrı okunabilir olmalıdır.
- Ana hamle eylemi “Hamleyi Onayla”dır; Pas/Karıştır/Değiştir ikincil eylemlerdir.
- Alan ele geçirme geri bildirimi hızlı ve güçlü olmalı; mevcut çalışan VFX korunmalı ve ileride 1–1,5 saniyelik imza ele geçirme animasyonlarına evrilmelidir.
- Bot fallback, online senkronizasyon, güvenli backend doğrulaması ve mevcut çalışan maç sözleşmeleri görsel dönüşüm uğruna bozulmamalıdır.

## Dil kapsamı

İlk yayın ve mevcut ürün kapsamı yalnız Türkçe ve İngilizcedir. Yeni diller ürün verisi ve kullanıcı ihtiyacı oluşmadan istemci/backend karmaşıklığına eklenmez.

## Teknik uyumluluk kararı

Ürün adı kullanıcıya **Kelime Kuşatması** olarak gösterilir. Buna karşılık `applicationId`, Kotlin package adları, `WordSiege` sınıf adları, `word_siege` Supabase sözleşmeleri, release secret adları ve `sonharf://auth` deep-link şeması bu marka geçişinde değiştirilmez. Bunlar kullanıcı görünür marka değil, mevcut kurulum/kimlik doğrulama/dağıtım uyumluluğunu koruyan teknik kimliklerdir. Değiştirilmeleri ayrı bir migrasyon projesi gerektirir.

## Son Harf'in rolü

Son Harf hızlı, kısa oturumlu ikinci oyun/moddur. Ana retention ve sosyal ürün kararlarını yönlendirmez; ancak aynı profil, sosyal çevre, lig, ödül, mağaza ve monetizasyon ekosistemine bağlı kalır.
