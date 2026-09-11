# Kelime Tahtı — Ana Ürün Yönü

## Ürün hiyerarşisi

Kelime Tahtı uygulamanın ana ürünü ve ana oyunudur. Son Harf kısa oturumlu ikinci oyun/mod olarak korunur. Harf Yolu ve diğer yan modlar ana ürünün sosyal, rekabetçi ve retention ekosistemini destekler.

Ana ürün kimliği:

**Kelime Tahtı = kelime oyunu + taktik alan savaşı + sosyal rekabet**

## Ana oyun döngüsü

1. Harflerden geçerli kelime oluştur.
2. Kelime puanı kazan.
3. Yerleştirme hattı boyunca bölge/küp ele geçir veya mevcut alanı savun.
4. Harita kontrolünü büyüt.
5. Rakibin stratejik alanlarına baskı kur.
6. Lig, rövanş, sosyal rekabet, görev ve ilerleme sistemleriyle tekrar oyna.

Kelime puanı ve bölge puanı birbirinden ayrı izlenir. Toplam skor bu iki bileşenin toplamıdır. Mevcut temel kural korunur: sahip olunan her küp 2 bölge puanıdır; bir küp kaybedildiğinde yalnız o küpün bölge puanı düşer, daha önce kazanılmış kelime puanı geri alınmaz.

## Görsel kimlik

Oyuncu ekrana baktığında önce harita/bölge hâkimiyetini, sonra harfleri algılamalıdır. Klasik Scrabble/Kelimelik tahta ifadesi hedef değildir.

- Oyuncu alanı: adaçayı yeşili.
- Rakip alanı: yumuşak mavi.
- Tarafsız alan: kırık beyaz / açık kum.
- Kritik/taktik alan: hafif lavanta ve kontrollü sıcak vurgu.
- Harf taşları: modern, açık kum/kırık beyaz; büyük okunaklı harf ve erişilebilir puan değeri.
- Sahip olunan komşu hücreler mümkün olduğunca tek bölge hissi vermelidir.
- Harita kontrol yüzdesi ana rekabet göstergelerinden biridir.

## Fikri mülkiyet ayrışması

Kelimelik ve diğer kelime oyunlarının başarılı ürün prensiplerinden öğrenilebilir; ancak ekran, grafik, metin, kod, bonus geometrisi, marka, ikonografi veya özgün ifade biçimi kopyalanmamalıdır.

Yeni maçlarda klasik köşe/çapraz bonus geometrisi yerine Kelime Tahtı'na özgü taktik alan topolojisi kullanılır. Mevcut 15x15 / 225 hücre veri sözleşmesi geriye dönük uyumluluk için şimdilik korunur. Tahta boyutunun ileride değiştirilmesi ancak backend, kayıtlı maçlar, testler ve migrasyon planı birlikte ele alınarak yapılmalıdır.

## UX ilkeleri

- Ana CTA Kelime Tahtı'nı açar.
- Son Harf ikinci oyun olarak açıkça ayrıştırılır.
- Maç ekranında Kelime Puanı, Bölge Puanı ve Toplam ayrı okunabilir olmalıdır.
- Harita kontrolü görünür olmalıdır.
- Ana hamle eylemi “Hamleyi Onayla”dır; Pas/Karıştır/Değiştir ikincil eylemlerdir.
- Alan ele geçirme geri bildirimi hızlı ve güçlü olmalı; mevcut çalışan VFX korunmalı ve ileride 1–1,5 saniyelik imza ele geçirme animasyonlarına evrilmelidir.
- Bot fallback, online senkronizasyon, güvenli backend doğrulaması ve mevcut çalışan maç sözleşmeleri görsel dönüşüm uğruna bozulmamalıdır.

## Teknik uyumluluk kararı

Ürün adı kullanıcıya Kelime Tahtı olarak gösterilir. Ancak `applicationId`, Kotlin package adları, `WordSiege` sınıf adları, `word_siege` Supabase sözleşmeleri, release secret adları ve `sonharf://auth` deep-link şeması bu aşamada değiştirilmez. Bunlar kullanıcı görünür marka değil, mevcut kurulum/kimlik doğrulama/dağıtım uyumluluğunu koruyan teknik kimliklerdir. Değiştirilmeleri ayrı bir migrasyon projesi gerektirir.

## Son Harf'in rolü

Son Harf hızlı, kısa oturumlu ikinci oyun/moddur. Ana retention ve sosyal ürün kararlarını yönlendirmez; ancak aynı profil, sosyal çevre, lig, ödül, mağaza ve monetizasyon ekosistemine bağlı kalır.
