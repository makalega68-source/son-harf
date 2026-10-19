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

## Sonraki geliştirmeler

1. Gerçek cihazlarda çok raundlu Son Harf, bağlantı kopması/geri gelmesi ve iki hesapla
   davet-kabul akışını doğrula; bildirilen donma ve sayaç sorunları için sonuçları kaydet.
2. Son Harf'te oyuncu geri dönüşünü ölç; bağımsız testçilerle ilk maç tamamlama, 7. gün
   ve 30. gün geri dönüşü izle. Henüz başarı oranı veya gelir tahmini yapılmış değildir.
3. Üç maçlık arkadaş serisi ve maç sonu doğrulanmış kelime açıklaması için ayrı küçük
   denemeler hazırla. Mevcut rövanş daveti üç maçlık seri olarak sunulmamalıdır.
4. Obi'nin gerçek maç verisine dayanan kısa gözlemlerini geliştir. Sözlük reddi nedenlerini
   anlaşılır tut. Yardım haklarının adilliğini mevcut satın almaları bozmadan değerlendir.
5. Atölye açılışında günlük ortak harf seti/eşit koşullu mücadeleyi dene. Önce Atölye,
   ardından Kuşatma açılışı değerlendirilir; tarih sözü verilmez.

## Yayın öncesi kontrol

- TR/EN ana sayfa: Yeni Oyun Son Harf'i açar; diğer iki kart Yakında'dır.
- Oyun merkezi ve eski Kuşatma/seri bağlantıları kapalı oyunu başlatmaz.
- Arkadaş/rövanş daveti Son Harf'e gider; kabulde iki oyuncu da doğru odaya girer.
- Açık Son Harf maçlarının ve davetlerinin rozeti çalışır; kapalı modlar rozet üretmez.
- Atölye turnuva daveti, kapalı mod görevi veya kullanılamayan yeni ürün önerilmez.
- Satın almaların geri yüklenmesi, envanter ve bakiyeler iki hesapla doğrulanır.
- Play görselleri ve açıklaması Son Harf'i anlatır; gelecek oyunlar oynanabilir gösterilmez.
- Yeni AAB hazırlanırken Play'e daha önce yüklenmemiş bir versionCode kullanılır.

Kod doğrulaması cihazda oynanış doğrulaması yerine geçmez. Bu değişiklik Play Console'a
otomatik yükleme veya üretime yayınlama yapmaz.
