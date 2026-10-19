# Kelime Tahtı — ilk yayın kapsamı

19 Ekim 2026 kararı: İlk yayında yalnız Son Harf oynanabilir. Kelime Kuşatması ve
Kelime Atölyesi ana sayfada küçük, etkileşimsiz **Yakında / Coming soon** kartlarıdır.
Uygulamanın adı, paketi, mevcut oyun kuralları ve oyuncu hesapları korunur.

## İlk sürümde

- Son Harf; arkadaş davetleri ve rövanş davetleri; Oyunlarım; Taht; profil; mağaza; Obi.
- Kapalı modlar oyun merkezi, davet, eski maç bağlantısı veya etkinlik üzerinden açılamaz.
- Atölye sayacı ve turnuva bildirimleri gösterilmez. Taht Saati, Son Harf'i öne çıkarır.
- Kapalı modların yeni tahta ürünleri, paketleri ve video ödülleri sunulmaz. Mevcut
  satın almalar, envanter, bakiyeler ve geçmiş sunucu verileri silinmez veya geri alınmaz.
- Ek günlük bulmaca oyunları ilk yayında gösterilmez.

Oyunların açıklığı `GameAvailability.kt` üzerinden yönetilir. Bu bir uygulama sürümü
kararıdır; eski test APK'larını sunucudan kapatan bir değişiklik değildir. Bir modu yeniden
açarken kart, davet, yardım, etkinlik, bildirim ve mağaza akışları birlikte doğrulanmalıdır.

## Altı önerinin uygulaması

1. **Son Harf odağı:** İlk yayında diğer oyunların bütün girişleri kapalıdır; küçük
   Yakında kartları gelecek oyunları gösterir.
2. **Günlük ortak mücadele:** Son Harf lobisindeki Günün Zinciri, her dilde herkese aynı
   günlük başlangıç kelimesini verir. Sunucu saatine göre 60 saniye, hesap/dil/gün başına
   tek deneme ve ipucusuz oyun vardır. Ekrandan çıkmak süreyi sıfırlamaz. Kelimeleri ana
   oyun sözlüğü doğrular. RP, XP veya para verilmez; biten sonuç cihazın paylaş menüsünden
   paylaşılabilir. Atölye ilk yayında kapalı olduğundan öneri Son Harf'e uyarlanmıştır.
3. **Üç maçlık arkadaş serisi:** Mevcut sunucu rövanş bağlantıları üç ayrı maç halinde
   sayılır. Sonuç ekranı tamamlanan maçları ve iki oyuncunun galibiyetlerini gösterir.
   Her yeni maç için iki oyuncunun da rövanşı kabul etmesi gerekir; üç maç sonunda yeni
   seri başlar. Eksik geçmişten sonuç uydurulmaz, mevcut maç puanları değiştirilmez.
4. **Kelimeden öğrenme:** Sonuç ekranında o maçta gerçekten kullanılan bir kelimenin
   kısa açıklaması gösterilir. TR/EN özgün kısa açıklama havuzu, Türkçede gerektiğinde
   mevcut resmi TDK sorgusu kullanılır. Açıklama bulunamazsa anlam uydurulmaz.
   Kelime reddi, ana oyunun mevcut gerekçeleriyle; günlük alanda da anlaşılır metinle verilir.
5. **Obi'nin gerçek gözlemleri:** Kendi kelimelerine göre yeni uzunluk rekoru ve beşli
   geçerli kelime serisi yorumları, maç sonunda kendi en uzun kelimen ve kelime sayın.
   Ses veya ücretli yapay zekâ servisi eklenmez; uzun kelimeye olmayan puan bonusu vaat edilmez.
6. **Eşit rekabet ve PRO:** Gerçek rakibe karşı mevcut üç ipucu kuralı hesap/maç başına
   atomik sunucu kotasıyla korunur. Yeniden giriş kotayı sıfırlamaz. Video/Son Coin ek
   ipuçları yalnız AI maçlarında kalır. PRO açıklaması ek dereceli puan veya ipucu
   vermediğini açıklar. Mevcut fiyatlar, haklar ve satın almalar değiştirilmez.

Sunucu değişikliği: `supabase/migrations/20261019112802_last_letter_niche_features_v1.sql`.
Yeni tablolar için yalnız kendi verisini okuma politikası vardır; istemci doğrudan yazamaz.
Günlük deneme ve kota işlemleri kimliği denetleyen RPC'ler üzerinden yapılır.
`supabase/tests/last_letter_niche_features.sql` gerçek veritabanında tek işlem içinde
çalıştırılıp geri alınmıştır: hesap izolasyonu, tekrar başlatma, süre bitimi, kelime
doğrulama, üç ipucu sınırı, sıra kontrolü ve yetkisiz erişim testleri geçti.

## Devam eden ürün doğrulaması

- Gerçek cihazlarda çok raundlu oyun, bağlantı kopması/geri gelmesi ve iki hesapla
  davet-kabul/üç maçlık seri akışı doğrulanmalıdır. Kod testleri fiziksel cihaz testi değildir.
- Bağımsız testçilerle ilk maç tamamlama, 7. gün ve 30. gün geri dönüşü ölçülmelidir.
  Henüz başarı oranı veya gelir tahmini yapılmış değildir.
- Atölye ve Kuşatma ancak Son Harf'in kullanımı ve teknik kararlılığı görüldükten sonra
  açılış için değerlendirilir; tarih sözü verilmez.

## Yayın öncesi kontrol

- TR/EN ana sayfa: Yeni Oyun Son Harf'i açar; diğer iki kart Yakında'dır.
- Oyun merkezi ve eski Kuşatma/seri bağlantıları kapalı oyunu başlatmaz.
- Arkadaş/rövanş daveti Son Harf'e gider; kabulde iki oyuncu da doğru odaya girer.
- Günün Zinciri aynı gün yeniden başlamaz; arka plandan dönüşte süre devam eder.
- Üç maçlık seri rakibin gecikmeli kabulünde de devam eder; dördüncü maç yeni seridir.
- Maç sonu kelimesi gerçekten oynanmıştır; Obi notu rakibin kelimesini sana mal etmez.
- Gerçek rakipte dördüncü ipucu, başka cihaz veya yeniden girişle de alınamaz.
- Açık Son Harf maçlarının ve davetlerinin rozeti çalışır; kapalı modlar rozet üretmez.
- Atölye turnuva daveti, kapalı mod görevi veya kullanılamayan yeni ürün önerilmez.
- Satın almaların geri yüklenmesi, envanter ve bakiyeler iki hesapla doğrulanır.
- Play görselleri ve açıklaması Son Harf'i anlatır; gelecek oyunlar oynanabilir gösterilmez.
- Yeni AAB hazırlanırken Play'e daha önce yüklenmemiş bir versionCode kullanılır.

Kod doğrulaması cihazda oynanış doğrulaması yerine geçmez. Bu değişiklik Play Console'a
otomatik yükleme veya üretime yayınlama yapmaz.
