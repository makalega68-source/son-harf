# Devir notu: Kelime Tahtı (Son Harf) — kalan işler ve Google Play test yayını

Bu dosya, işe başka bir yapay zekâ asistanıyla (GPT/Codex) devam edilmesi için yazıldı.
Önce "Asistana yapıştırılacak komut" bölümünü asistana ver; asistan bu dosyanın tamamını okuyarak çalışsın.

---

## Asistana yapıştırılacak komut

```
Sen "Kelime Tahtı" (paket: com.sonharf.game) Android oyununun geliştirme asistanısın.
Benimle her zaman Türkçe konuş. Repo: github.com/makalega68-source/son-harf
Çalışma dalı: claude/mascot-animations-behavior-31rof9 (PR #459). Önce repodaki
docs/DEVIR_GPT_GOOGLE_PLAY.md dosyasını baştan sona oku ve oradaki kurallara uy.

Görevin iki parça:
1) Kodla ilgili kalan işleri sen yap: değişiklik yap, testleri çalıştır, aynı dala
   commit + push et, GitHub Actions sonucunu kontrol et, kırmızıysa düzelt.
2) Google Play'de "gerçek oyuncularla test yayını" (kapalı test) açmam için beni
   adım adım yönlendir. Play Console / AdMob / Google hesabı gerektiren adımları ben
   yapacağım; sen her seferinde tek bir adım söyle, ekranda neye basacağımı yaz,
   ekran görüntümü bekle, sonra sıradakine geç.

Kesin kurallar:
- İmza anahtarını (keystore), şifreleri, AdMob kimliklerini asla repoya koyma, asla
  sohbete yazdırma. Onlar yalnızca GitHub Secrets'ta durur.
- Yeni dal açma, main'e birleştirme yapma (ben istemedikçe).
- Oyun kurallarını, satın almaları, oyuncu verilerini ve bakiyeleri bozma/azaltma.
- Atölye'de "level/seviye" sistemi yok; ekleme.
- Maskot konuşma balonları her yerde var; sesli konuşma (seslendirme) asla yok.
- Ücretli bulut yapay zekâ servisi kullanma (benden onay almadan).
- Bir şeyi cihazda göremiyorsan bunu açıkça söyle; test etmeden "çalışıyor" deme.
```

---

## 0. Oyun yapısı (7 Ekim 2026 kararı)

- Uygulamanın adı **Kelime Tahtı** olarak kalır.
- **Ana oyun Son Harf'tir.** Ana sayfadaki büyük "Yeni Oyun" düğmesi Son Harf'i açar; oyun merkezinde
  ve yardımda Son Harf ilk sıradadır.
- **Kelime Kuşatması ikinci oyundur**: ana sayfada "Diğer Oyunlar" altında kart olarak durur
  (Kelime Atölyesi ile birlikte). Kullanıcılar Kuşatma'yı "Kelimelik'e benziyor" diye eleştirdi.
- Mağaza açıklaması ve ekran görüntüleri Son Harf'i öne çıkarmalıdır.

## 1. Proje bilgileri

| Konu | Değer |
|---|---|
| Uygulama | Kelime Tahtı — paket `com.sonharf.game` |
| Repo / dal | `makalega68-source/son-harf`, dal `claude/mascot-animations-behavior-31rof9`, PR #459 |
| Sürüm | `versionCode 48`, `versionName 1.0.11` (`app/build.gradle.kts`) |
| Sunucu | Supabase projesi `bzdtftzdjtjoqhtcqtxb`; migration dosyaları `supabase/migrations/` |
| CI | GitHub Actions: Android CI, Branch APK CI, Final Unified Validation, Frame Provenance Gate |
| APK | Push sonrası çalışan CI'da `Kelime-Tahti-Guncel` artifact'ı |
| AAB (Play'e yüklenecek) | CI'da `son-harf-release-NNNN` artifact'ı (içinde `app-release.aab`) |
| İmza | Play App Signing açık. Yükleme anahtarı + şifreler GitHub Secrets'ta: `SON_HARF_RELEASE_KEYSTORE_B64`, `SON_HARF_RELEASE_STORE_PASSWORD`, `SON_HARF_RELEASE_KEY_ALIAS`, `SON_HARF_RELEASE_KEY_PASSWORD`. Yedeği kullanıcıda. |
| AdMob | GitHub Secrets: `SON_HARF_ADMOB_APP_ID`, `SON_HARF_ADMOB_REWARDED_ID`, `SON_HARF_ADMOB_BANNER_ID` (gerçek reklam kimlikleri) |
| Mevcut kontrol listesi | `docs/PLAY_STORE_CHECKLIST.md` |

Önemli: Ana kodun (Compose) derleme hataları yalnızca CI'da görünür. Her push'tan sonra
Actions sonucunu kontrol et. Aynı dala arka arkaya push edilirse eski çalışmalar iptal olur
("cancelled"), bu normaldir; son commit'in sonucu esastır.

---

## 2. Son yapılanlar (cihazda doğrulanmalı)

Son iki commit (6 Ekim 2026):
- `05466bb1` — Canlı maç donmaları ve ipuçları:
  - Son Harf: oda/kelime/sohbet izleme hata alınca artık yeniden başlıyor (önceden durup
    oyunu donduruyordu ve oyuncu maçtan düşüyordu). AI hamlesi gerçekten gelene kadar tekrar
    isteniyor. Sunucu okumaları ana iş parçacığı dışında.
  - Son Harf: ipucu her maçta net cevap kelimesi veriyor (maç başına 3). Ekrandaki uyarılar
    5 sn sonra kayboluyor (ipucu düğmesini gizlemesin diye).
  - Son Harf: raund arası sayaç en fazla 10 sn gösteriyor; oyun alanı ve ara ekran petrol renk.
  - Kuşatma (online): ipucu en iyi hamleyi tahtaya yerleştiriyor, maskot oraya uçuyor;
    zoom sırasında tüm tahta yeniden çizilmiyor; sunucu okumaları arka planda.
  - Ana sayfa: "Arkadaşlar" kartı eklendi.
- `5bb51030` — Tek tip premium bildirim rozeti (`NotificationBadge.kt`): Oyunlarım, sohbet
  düğmeleri, Birlikte Oyna kartı. Oyunlarım'a basınca rozet kaybolur; yalnızca yeni sıra veya
  yeni davet gelince geri gelir (`SeenBadges`).

Devir anında `5bb51030` için CI hâlâ çalışıyordu. İlk iş: bu commit'in Actions sonucunu
kontrol et; kırmızıysa log'u oku (`Android CI` işi), düzelt, tekrar push et.

### Cihazda test listesi (kullanıcıyla birlikte)
1. Kuşatma, gerçek oyuncuya karşı: İPUCU taşları tahtaya koyuyor mu? Zoom akıcı mı?
2. Son Harf (AI ve gerçek oyuncu): ipucu çıkıyor mu? 2–3 raund boyunca donma/maçtan atılma var mı?
3. Son Harf raund arası: önce 4 sn "Raund bitti", sonra sayaç en fazla 10'dan geri sayıyor mu?
   (Önceden 23 sn görülmüştü, kök neden kesin bulunamadı. Hâlâ uzun görünürse sunucudaki
   `turn_deadline` hesabını incele: `submit_word_normal_v3`, `bot_take_turn_normal_v1`,
   `private.sonharf_consume_missed_slot_v1` fonksiyonları ara için `+10 seconds` ekliyor.)
4. Oyunlarım rozeti: basınca kayboluyor mu, yeni sıra gelince geri geliyor mu?
5. Ana sayfadaki Arkadaşlar kartı doğru sayfayı açıyor mu?
6. Ödüllü video reklam ve banner gerçek reklam gösteriyor mu? (Geliştirici kendi gerçek
   reklamına tıklamamalı; AdMob'da test cihazı tanımlanmalı.)

---

## 3. Kalan kod işleri (öncelik sırasıyla)

1. CI yeşil + yukarıdaki cihaz testlerinden çıkan hatalar.
2. Genel arayüz cilası: mağaza, profil, sosyal, ayarlar sayfalarında yazı simetrisi ve petrol tema tutarlılığı.
3. Oyun parası (altın) için yeni ürün fikirleri — önce kullanıcıya öner, onay almadan ekleme.
4. 3B bonus ikonları (isteğe bağlı, düşük öncelik).
5. Yayın öncesi: her sürümde `versionCode` bir artırılmalı (Play aynı numarayı ikinci kez kabul etmez).

---

## 4. Google Play: gerçek oyuncularla test yayını yol haritası

Mevcut durum: Geliştirici hesabı doğrulandı. Uygulama Play Console'da oluşturuldu.
Dahili test (internal testing) sürümü yüklendi (37 numaralı derleme), test kullanıcı listesi "Ekip" oluşturuldu.

Hedef: **Kapalı test (Closed testing)** ile gerçek oyunculara açmak, sonra üretim başvurusu.
Kişisel geliştirici hesaplarında üretime çıkmak için: **en az 12 test kullanıcısı, 14 gün
kesintisiz** kapalı teste katılmış olmalı. Bu süre ancak kapalı test başladığında işler.

### Adım adım (kullanıcı Play Console'da yapar, asistan yönlendirir)
1. **En yeni AAB'yi al:** Son yeşil CI çalışmasından `son-harf-release-NNNN` artifact'ını indir,
   zip'ten `app-release.aab` çıkar. (versionCode 48 olmalı.)
2. **Uygulama içeriği (App content) formlarını tamamla** (Panel → Uygulama içeriği):
   - Gizlilik politikası URL'si (herkese açık bir sayfa; hesap, profil fotoğrafı, sohbet,
     satın alma, reklam (AdMob) verilerini anlatmalı). Yoksa asistan metni hazırlasın,
     kullanıcı GitHub Pages / Google Sites gibi bir yerde yayımlasın.
   - Uygulama erişimi: giriş gerektiriyorsa inceleme ekibi için bir test hesabı (e-posta/şifre) ver.
   - Reklamlar: "Evet, reklam içeriyor".
   - İçerik derecelendirmesi anketi (oyun; kullanıcılar arası sohbet var → bunu belirt).
   - Hedef kitle ve içerik: 13+ veya 18+ önerilir (sohbet ve satın alma olduğu için çocuklara yönelik seçilmemeli).
   - Veri güvenliği (Data safety): toplanan veriler — e-posta/hesap, ad, profil fotoğrafı,
     kullanıcı içeriği (sohbet mesajları), satın alma geçmişi, uygulama etkileşimleri,
     reklam kimliği (AdMob). Aktarımda şifreli: evet. Hesap silme: uygulama içinde var.
   - Hesap silme URL'si/talebi: uygulamada hesap silme akışı var; Play ayrıca bir web
     bağlantısı isteyebilir.
   - Haber uygulaması: hayır. Devlet uygulaması: hayır. Finansal özellik: hayır.
3. **Mağaza girişi (Ana mağaza girişi):** uygulama adı, kısa açıklama (80 karakter), uzun
   açıklama, 512×512 ikon, 1024×500 özellik grafiği, en az 2 telefon ekran görüntüsü.
   Asistan Türkçe/İngilizce metinleri yazabilir.
4. **Kapalı test kanalı oluştur:** Test et ve yayınla → Test → Kapalı test → yeni kanal
   (veya "Alfa") → Test kullanıcıları: e-posta listesi (en az 12 kişi, Gmail adresleri) veya
   Google Grubu → Ülkeler: Türkiye (+ istenenler) → Yeni sürüm oluştur → AAB'yi yükle →
   sürüm notu yaz → İncele → "Kapalı teste yayınla".
5. **Google incelemesi** (birkaç saat – birkaç gün). Onaydan sonra test bağlantısını
   ("Web'de katıl" linki) test kullanıcılarına gönder. Her kişi linke tıklayıp "Test kullanıcısı
   ol" demeli, sonra Play Store'dan uygulamayı indirmeli.
6. **14 gün boyunca** en az 12 kişi testte kalmalı ve uygulamayı kullanmalı. Bu sürede çıkan
   hatalar düzeltilip yeni AAB'ler (versionCode artırılarak) aynı kapalı teste yüklenebilir.
7. **Uygulama içi satın alımlar:** Para kazanma → Ürünler bölümünde ürünlerin (abonelik ve
   altın paketleri) kimlikleri koddakilerle birebir aynı olmalı. Lisans test kullanıcıları
   (Ayarlar → Lisans testi) eklenirse testçiler gerçek para ödemeden satın alma deneyebilir.
8. **AdMob:** uygulama Play'de yayınlanınca AdMob'da uygulamayı mağaza girişine bağla;
   geliştiricinin web sitesi varsa `app-ads.txt` ekle. Ödeme bilgileri AdMob'da tamamlanmalı.
9. **14 gün dolunca:** Panel'de "Üretime erişim için başvur" → anket (test süreci, geri
   bildirimler) → onay sonrası üretim sürümü.

### iOS notu
iOS ileride eklenecek. Bu Android projesi doğrudan iOS'a derlenmez; ayrı bir iOS uygulaması
(veya çoklu platform geçişi) ve Apple Developer hesabı (yıllık ücretli) gerekir. Supabase
sunucusu ortak kullanılabilir. AdMob'da iOS için ayrı uygulama ve reklam birimleri açılır.

---

## 5. Sık karşılaşılan sorunlar

- **"Öğe bulunamadı" (test linkinde):** Sürüm yeni yayınlandıysa yayılması saatler sürebilir;
  test kullanıcısı listesinde o Gmail hesabı var mı ve telefondaki Play Store aynı hesapla mı
  açık, kontrol et. Beklerken APK ile test edilebilir.
- **"Bu sürüm kodu zaten kullanılmış":** `versionCode` artırılıp yeniden derlenmeli.
- **CI'da imzalı AAB atlandı uyarısı:** İmza ya da AdMob secret'larından biri eksik/hatalı.
- **Supabase değişikliği:** Sunucu fonksiyonu değişirse migration hem uygulanmalı hem de
  `supabase/migrations/` altına dosya olarak eklenmeli.
