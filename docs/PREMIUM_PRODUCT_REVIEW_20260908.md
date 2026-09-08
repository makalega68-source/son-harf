# Son Harf — Premium ürün incelemesi ve uygulama sırası

Tarih: 8 Eylül 2026. Dayanak: ana dal `23f1b28dfcaba7ab521e92056c4772f53819e3d4`.
Bu belgede **tespit**, **ürün kararı**, **uygulanan değişiklik** ve **açık iş** ayrı tutulur.
Kategori liderliği, eksiksiz sözlük veya retention artışı henüz kanıtlanmış değildir.

## Doğrulanmış başlangıç

- Android CI 34208197602, Final Unified Validation 34208197634 ve Frame Provenance Gate 34208197649 başarılı.
- Açık PR #282 (`bcb5b1f`) Premier 1v1 ekranını ve V4 sözlük istemcisini değiştiriyor. Bu paketin ortak profil/mağaza değişiklikleri o dosyaları değiştirmiyor. PR #282 bu paket tarafından birleştirilmedi.
- Gerçek uygulama Kotlin/Jetpack Compose. Paylaşılan Flutter metni, çalışan Android uygulamasının yerine geçirilecek doğrulanmış kaynak değildir.
- Mevcut CI maskot runtime/varlıklarını yasaklıyor. Stabilite koşulu sağlanmadan iki maskotu geri eklemek bu kapıyı kırar. Satın alınan varlıklar silinmez; entegrasyon ayrı paket olur.
- Ana müzik Warm Beginnings ve “Kelimeyi Sürdür, Rakibini Geç” kimliği korunur.

## Pazar araştırması: doğrulanan mekanikler ve Son Harf kararı

Kaynaklar resmî mağaza açıklamaları, yayıncı sayfaları ve yardım belgeleridir. Oyunların cihaz üzerinde oynanmış UX denetimi yapılmadı. Aşağıdaki zayıflıklar, aksi belirtilmedikçe Son Harf hedeflerine göre tasarım değerlendirmesidir; ölçülmüş rakip retention verisi değildir. Eski kullanıcı yorumları güncel sorunun yaygınlığını kanıtlamaz.

| Oyun / kaynak | Doğrulanan güçlü mekanik | Son Harf açısından risk / fırsat | Ürün kararı |
|---|---|---|---|
| [Kelimelik — Google Play](https://play.google.com/store/apps/details?hl=tr&id=com.he2apps.kelimelik), [App Store](https://apps.apple.com/tr/app/kelimelik/id563803371), [yayıncı](https://he2apps.com/kelimelik.html) | Arkadaş/rastgele rakip, sohbet, farklı hızlarda maçlar, haftalık Süper Lig; reklam ve PRO/araç satın alımları | Ücretli harf bilgisi rekabet eşitliğiyle; arkadaş listesini ödeme arkasına koymak sosyal büyüme hedefiyle gerilim yaratır | Ücretsiz çekirdek rekabet ve temel arkadaş akışı; PRO için reklamsız deneyim, Style, gelişmiş maç sonrası istatistik |
| [Words With Friends — yayıncı](https://www.zynga.com/games/words-with-friends-2/), [App Store](https://apps.apple.com/us/app/words-with-friends-word-game/id1196764367), [Google Play](https://play.google.com/store/apps/details?hl=en&id=com.zynga.words3) | Arkadaş rekabeti, solo challenge, günlük oyun ve düellolar | Çok sayıda etkinlik ana eylemi görünmez yapabilir | Ana sayfada baskın OYNA; günlük hedef ve sosyal kısayol ikinci sırada |
| [WWF kulüpler](https://zyngasupport.helpshift.com/hc/en/63-words-with-friends-2/section/1428-clubs/), [rozetler](https://zyngasupport.helpshift.com/hc/en/63-words-with-friends-2/faq/12154-how-do-i-earn-badges/?l=en&s=vip) | Kulüp koordinasyonu, kulüp mağazası, haftalık/etkinlik rozetleri | Ayrı kulüp para birimi ekonomi karmaşıklığı yaratabilir | Tek Son Coin; kalıcı unvan/rozet; oyuncu yoğunluğu yeterli olunca takım yarışı |
| [Wordscapes — Google Play](https://play.google.com/store/apps/details?hl=en&id=com.peoplefun.wordcross), [takımlar](https://peoplefun.helpshift.com/hc/en/6-wordscapes/faq/304-what-are-wordscapes-teams/), [turnuva eşleşmesi](https://peoplefun.helpshift.com/hc/en/6-wordscapes/faq/616-how-do-team-tournaments-group-players/?p=web) | Kolay başlayan kelime bulmacaları; arkadaş takımı, yardımlaşma ve hafta sonu takım turnuvası | Başlangıç zamanına göre turnuva gruplaması aynı beceri seviyesini garanti etmez | Önce düşük bekleme süresi, sonra ölçülmüş beceriye yakın küçük lig grupları; rekabette ücretli yardım yok |
| [Brawl Stars — Şubat 2026](https://supercell.com/en/games/brawlstars/blog/release-notes/release-notes-february-2026), [Nisan 2026](https://supercell.com/en/games/brawlstars/blog/release-notes/release-notes-april-2026), [Ağustos 2026](https://supercell.com/en/games/brawlstars/blog/release-notes/release-notes-august-2026) | Kalıcı prestij ve görsel ödüller; birlikte galibiyetlere/çevrimiçi durumuna göre arkadaş sıralama; favori arkadaş bildirim kontrolü; ilerleme sunumunun yenilenmesi | Mağaza ve etkinlik yoğunluğunu kopyalamak kelime oyununu ağırlaştırır | Kalıcı kelime ustalığı, ezeli rakip skoru, favori arkadaş kontrolü; açık sonraki hedef |
| [Stumble Guys — Google Play](https://play.google.com/store/apps/details?hl=en_GB&id=com.kitkagames.fallbuddies), [ranked rehberi](https://stumbleguys.helpshift.com/hc/en/4-stumble-guys/faq/162-how-does-ranked-mode-work/), [eşleşme iyileştirmesi](https://www.stumbleguys.com/news/ranked-season-11) | Lig, sezon ödülleri, kişiselleştirme; düşük yoğunlukta daha hızlı eşleşmek için maç boyutunun küçültülmesi | Yeteneğe güç ekleyen sistemler Son Harf'e uygun değil | Bekleme ve maç tamamlama oranını ölç; sezon ödülleri yalnızca prestij/Style olsun |

## Kelimelik gelir modelinin açık eşleştirmesi

Resmî açıklamalarda ücretsiz reklam destekli oyun; PRO ile reklamsız kullanım, artan eşzamanlı maç kapasitesi, görünüm, puan hesaplayıcı, seri oyun, harf tablosu ve arkadaş kapasitesi bulunuyor. Yayıncı ve mağaza metinlerinin paket ayrıntıları farklılaşabiliyor; güncel gerçek ödeme ekranı/SKU fiyatları görülmeden fiyat ya da abonelik süresi varsayılmayacak.

| Alan | Son Harf uygulama kararı |
|---|---|
| Reklam | Maç sırasında zorunlu reklam yok; ödüllü reklam gönüllü; zaman aşımı maç açılmasını engellemez |
| PRO / Premium | Reklamsız deneyim + görünüm + rekabet dışı ayrıntılı istatistik/konfor |
| Puan hesaplayıcı / harf sayacı | Maç kararını kolaylaştıran bilgi yalnızca ücretliye verilmez; gerekiyorsa herkese eşit sunulur |
| Arkadaş ve rövanş | Temel ekleme, meydan okuma, engelleme, raporlama ve rövanş ücretsiz çekirdektir |
| Kapasite | Eşzamanlı asenkron maç kapasitesi değerlendirilebilir; günlük lig puanı üretimini ödeme avantajına çeviremez |
| Son Coin / Style | Tek para birimi; katalog ile kalıcı sahiplik ayrı; puan, süre, rating, kelime doğrulama hakkı satılmaz |
| Sezon bileti | Görsel ödüller; ücretli oyuncuya lig/maç avantajı vermez |

Bu nedenle “birebir kopyalama” ile kullanıcının kesin pay-to-win yasağı çeliştiğinde adil rekabet korunur. Mevcut ücretli haklar silinmez; hak değişikliği ayrıca değerlendirilir.

## Hedef ekran ve akış sistemi

- **İlk açılış:** Türkçe/English seçimi; 3 kısa etkileşimle öğretim (son harfi gör, kelime yaz, gönder). 30 saniye hedefi kullanıcı testiyle ölçülür; zorunlu uzun açıklama duvarı olmaz.
- **Ana sayfa:** profil/lig özeti; en baskın OYNA; mod ve maç dili açıkça görünür; tek yakın günlük hedef; çevrimiçi arkadaş ve rövanş kısayolu. Mağaza çağrısı OYNA ile yarışmaz.
- **Eşleşme:** mod+dil+beceri; 15 saniyede gerçek oyuncu yoksa açıkça bot olarak belirtilen uygun alıştırma. Arka plan gerçek oyuncu bulunduğunda mevcut maç sessizce değiştirilmez; oyuncuya geçiş önerilir. Bot galibiyetleri gerçek rekabet rating'ini şişirmez.
- **Maç:** rakip avatarı, skor, süre ve hedef harf tek hiyerarşide. 12sp taş puanı, 14sp bonus başlangıç hedefi; dar cihazlarda yakınlaştırma ve yatay kaydırma. Ses/haptik/kritik an geri bildirimi kapatılabilir; renk tek durum göstergesi olmaz.
- **Maç sonu:** sonuç + rating değişimi + sonraki lige uzaklık + bir yakın hedef; tek dokunuş rövanş; aynı ödül iki kere yazılmaz. Kaybeden oyuncuya da ulaşılabilir ustalık/görev hedefi.
- **Sosyal:** arkadaş ve ezeli rakip kartında karşılıklı skor; çevrimiçi durum, davet, engel/rapor/sessize alma. Hazır mesajlar TR/EN; açık sohbetin erişim kuralları ayrıca incelenir.
- **Profil/koleksiyon:** kalıcı satın alımlar ve kazanılan unvanlar; aktif görünüm durumu; katalogdan bağımsız kullanım. Satıştan kaldırılan ürün silinmez; desteklenmeyen varlık açıklamayla görünür.
- **Tema:** ortak semantik renkler, 4.5:1 normal metin kontrast hedefi; 320/360/412/600dp ve font 1.0/1.3/2.0 denetimi. Sabit yükseklikle büyük yazı kırpılmamalı.

## Sözlük denetimi: sayı ile kaliteyi ayır

Canlı salt okunur sayım (8 Eylül 2026):

| Dil | Toplam benzersiz normalized_word | Aktif, oyun izinli, özel ad/kısaltma olmayan |
|---|---:|---:|
| Türkçe | 369334 | 368618 |
| İngilizce | 51333 | 51312 |

Bunlar modların uzunluk/alfabe filtrelerinden önceki sayılardır; her kelimenin her modda geçerli olduğu veya sözlüğün eksiksiz olduğu anlamına gelmez. Kaynak/version alanları boş değil; buna rağmen **TR 5633 ve EN 11782 oynanabilir kayıt `legacy_unattributed`** etiketi taşıyor. Bu etiket gerçek kaynak/lisans kanıtı değildir. 686 eski sentetik iki harf kaydı bulunuyor; yapılan sorguda hiçbiri oynanabilir değil. Bunları tekrar etkinleştirme.

Ana kaynak wooorm dictionary commit `8cfea406b505e4d7df52d5a19bce525df98c54ab`; TR toplam 363010 ve EN 39532 kayıt bu kaynağa bağlı. Kelime eksikliğini azaltmak için:

1. Her modun oyun dili ve sözcük uzunluğu sınırını çıkar; oda dili sunucu otoritesidir, arayüz diliyle değişmez.
2. Türkçe I/ı ve İ/i ile NFC eşitliği; İngilizce normalizasyon; klavye, bot ve istemci/sunucu kabul kümesi aynı kurallardan türesin.
3. Kaynaklar pinned sürüm, dosya hash'i, kaynak bildirimi ve dağıtım şartlarıyla içe alınsın. [ESDB/SCOWL](https://github.com/en-wl/wordlist) İngilizce aday kaynağıdır; kısaltma, boşluklu/çizgili bileşikler oyun filtresinden geçirilmelidir. [Zemberek](https://github.com/ahmetaa/zemberek-nlp) Türkçe morfolojik denetim için adaydır; yavaş bakım durumundadır ve ürettiği her çekim otomatik oyun kelimesi sayılmaz.
4. TDK referans olması toplu veriyi dağıtma izni varsayımı oluşturmaz. Kaynağı belirsiz eski kayıtlar topluca silinmez; lisans ve dilbilimsel inceleme kuyruğuna alınır.
5. Ret nedeni (dil, uzunluk, sözlük, tekrar) ayrı tutulur. “Eksik kelime bildir” doğrulanmış kuyruğa gider; kullanıcı bildirimi otomatik geçerli kelime yapmaz.
6. Yeni sözlük sürümü önce karşılaştırma/gölge doğrulama ve örneklemle ölçülür. Maç başladığı sözlük sürümünü sabit tutar; güncelleme devam eden maçın kabul kümesini değiştirmez.
7. Soru bankası varsa TR/EN içerikler ayrı kimlikli, çeviri denetimli ve tekrar aralığı kontrollü olmalıdır; yalnızca arayüz çevirisi yeterli değildir.

Sözlük genişletmesi bu pakette yapılmadı; mevcut çalışan sözlük ve açık V4 PR korunuyor.

## Kontrollü paketler ve çıkış ölçütleri

| Sıra | Paket | Tamamlanma ölçütü | Bu çalışma |
|---|---|---|---|
| 1 | Kalıcı Style koleksiyonu / ağ hatası / okunabilirlik | Eski ürün sahibince okunur/kullanılır, başkası kullanamaz; offline tema korunur; Android test/derleme | PR #283; sahiplik migration'ı canlıda uygulandı ve tekrar test edildi; Android sonuçları PR'da izlenir |
| 2 | Bütün modlarda TR/EN ve sözlük | Dil × mod × bot × online/offline kabul matrisi; eksik çeviri yok; kaynak kayıtları tam | Açık; V4 PR ile birlikte değerlendirilecek |
| 3 | Ana sayfa / onboarding / maç sonu | İlk maç başlama ve öğretim tamamlama ölçümü; tüm eski akışlar erişilebilir | Açık |
| 4 | Lig/rating/seri/rövanş/rakip/turnuva | Sunucu tek ödül/tek sonuç garantisi; dil/mod rating ayrımı; tekrar davet yarış testi | Açık |
| 5 | Sosyal | Arkadaş daveti, engelleme, raporlama ve yarış koşulları; spam sınırı | Açık |
| 6 | Adil gelir | Ürün ve ödeme ekranı eşleşir; satın alma/restore/refund/idempotency; offline hak doğrulama | Açık |
| 7 | Yayın ve devir | Cihaz testi, iOS uygulama/derleme hattı, imzalı mağaza artefaktı, lisans dosyaları | Açık; Kotlin Android deposu iOS desteği kanıtı değil |

## Analytics ve işletim

Önerilen olaylar: onboarding_started/completed, play_tapped, matchmaking_started/completed, match_started/completed/abandoned, rematch_offered/accepted, goal_shown/completed, friend_invite_sent/accepted, collection_opened, style_equipped, purchase_started/verified/restored/refunded, dictionary_rejected/reported, connection_recovered. Mevcut olay altyapısı incelenmeden ikinci bir sistem eklenmez.

D1/D7/D30 geri dönüş; ilk maç başlama; maç tamamlama; rövanş dönüşümü; gerçek rakip bekleme p50/p95; dil/mod başına sözlük ret oranı; satın alma restore başarı oranı; çökmesiz oturum ve gecikme ölçülür. Başlangıç verisi olmadan artış yüzdesi vaat edilmez. Kullanıcı kimlikleri takma kimlikli; sohbet metni, token ve ödeme ayrıntısı analytics'e yazılmaz.

Güvenlik: sunucu süre/skor/sıra/sonuç otoritesi; idempotent hamle/ödül/satın alma; reconnect snapshot+sequence; RLS sahiplik kontrolleri; hatada tekrar gönderim çift ödül doğurmaz. Şema ile depo arasında fark bulunduğundan körlemesine db push yapılmaz. Eski APK ve dal temizliği ancak aktif dağıtım/geri dönüş bağlantıları ve saklama ihtiyacı doğrulandıktan sonra yapılır.

## İlk paketin veritabanı doğrulaması

Canlı migration kimliği `20260908120313_permanent_style_ownership` ile depo dosyası eşleştirildi. İşlem yalnızca katalog okuma politikası ve sahip olunan ürünün kullanım koşulunu değiştirir; satın alma, bakiye ve envanter satırlarını değiştirmez. Mevcut canlı kullanım fonksiyonunun yeni alanları korunur. Tanım değişmişse migration sessizce ezmek yerine hata verir.

`supabase/tests/permanent_style_ownership.sql` hem uygulama öncesi geri alınan işlemde hem uygulama sonrası çalıştırıldı: arşivlenmiş ürünü sahibi okur/kullanır; sahip olmayan okuyamaz/kullanamaz ve diğer kişinin envanterini göremez. Anon rolü kullanım RPC'sini çağıramaz. Test çerçevesi ve seçim değişiklikleri ROLLBACK ile geri alındı.

Güvenlik danışmanı uyarı sayıları öncesi/sonrası değişmedi. Bu, tüm sistemin güvenli olduğu iddiası değildir. Önceden mevcut iki [anon SECURITY DEFINER uyarısı](https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable) `equip_default_game_theme` ve `get_dictionary_snapshot_v3` içindir; ilki auth.uid() kontrolü içerir. Tüm RPC çağrı grafiği incelenmeden izinler topluca değiştirilmedi. Kaynak atfı belirsiz sözlük kayıtları ve [sızdırılmış parola koruması uyarısı](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection) yayın öncesi açık işlerdir.

Android/Compose cihaz üzerinde görsel kontrol, büyük yazı ölçeği ve iki cihaz multiplayer testi yapılmadı. PR derlemesinin başarılı olması bunların yerine geçmez. Ana dal ve mevcut APK bu paketle değiştirilmedi.
