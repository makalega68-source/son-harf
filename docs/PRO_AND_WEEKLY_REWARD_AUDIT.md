# PRO ve haftalık Taht ödülü — 2 Ekim 2026

## Düzeltilen erişim sorunları

- Aktif Kuşatma ekranı Hamle Önizleme'yi yalnız `profiles.is_vip` ile açıyordu. Tekil `score_calculator` satın alanlar puanı göremiyordu. Artık `score_calculator_access` hakkı ve `preview_word_siege_move_pro_v1` sunucu puanı kullanılıyor. Ücretsiz yerel geçerlilik işareti korunuyor; puan başarısızsa yenileme var.
- Mağaza başlangıç sekmesi 0–3'e sıkıştırıldığı için çerçeve/klavye/maskot kategorileri doğrudan açılamıyordu. Aralık 0–8 oldu.
- Kalıcı ürün kartları açılışta satın alımları geri yüklemiyordu. Otomatik geri yükleme ve açık geri yükleme düğmesi eklendi.
- Satın alınan çerçeve mağazada tekrar takılamıyordu. Sahip olunan çerçeve artık aynı karttan sunucuya doğrulatılarak takılıyor.
- PRO merkezinden arkadaşlar ve Hızlı Düello doğrudan açılıyor. Özel oda ve analiz mevcut gerçek ekranlara bağlı kalıyor.
- Arkadaş ekranı 30 saniyede yenileniyor; işlem sürerken arka planda yenileme yapılmıyor. Arama sonuçlarının çerçeveleri de oyuncu kimliğiyle çözülüyor.

## Haftalık ödül

- `profile_frame_gold_crest` yeni satışa kapatıldı; uygulama mağazasından çıkarıldı. Eski satın alınmış envanter silinmedi. PRO Kraliyet Altın ayrı üyelik çerçevesidir.
- `frame_throne_champion`: altın sanat + yakut/altın plaka üzerinde **1**. Satılmaz, kalıcı envantere eklenmez.
- Taht sahibi tamamlanan haftanın XP birincisidir. Haftası Türkiye saatine göre pazartesi 00:00'da başlar ve bir sonraki pazartesi 00:00'da sona erer. Eşit XP'de önce kazanma zamanı, ardından kullanıcı kimliği sıralaması kullanılır.
- Sunucu çerçeveyi okuma sırasında çözer; devir cron gecikmesine bağlı değildir. Ödül süresi dolunca oyuncunun seçtiği normal çerçeve görünür. Boş haftada eski ödül uzatılmaz.
- `get_public_profile_frame_v2` sunucu saati ve son kullanım zamanını döndürür. Önbellek en fazla 30 saniye; ödülün sona ermesi ayrıca sunucu kalan zamanına göre takip edilir. Çevrimdışı kalındığında süreli ödül korunmaz.
- Taht ekranı mevcut yarış lideri yerine son tamamlanan haftanın ödül sahibini gösterir. Devam eden yarış ayrı kürsü/sıralamada kalır.

## Referanslar ve kapsam

Kelimelik: https://play.google.com/store/apps/details?hl=tr&id=com.he2apps.kelimelik
Wordfeud: https://play.google.com/store/apps/details?id=com.hbwares.wordfeud.free
Words With Friends: https://play.google.com/store/apps/details?id=com.zynga.words3

Bu oyunların hesaplayıcı, harf tablosu, hızlı oyun, arkadaş listesi ve kişiselleştirme akışları karşılaştırıldı. Öncelik mevcut satın alımların gerçek kullanım yolunu onarmak, sosyal durumları güncel tutmak ve kalıcı ürünlerle süreli prestij ödülünü ayırmak oldu. Yeni oyun mekaniği veya AI bağlantısı eklenmedi. Kuşatma öğretisi dekoratif bonus sembolleri yerine mevcut puan kurallarını metinle anlatıyor. Tekrarlı mağaza/sosyal açıklamaları kısaltıldı.

## Doğrulama

Canlı `son-harf` Supabase projesinde ödül, hafta sınırı, eşitlik, boş hafta, mağazadan kaldırma, PRO hakları ve ücretli RPC katılımcı kontrolü testleri transaction/rollback ile geçti. Mevcut turnuva ve Kuşatma davet/presence regresyonları da geçti. Yeni çerçeve RPC'si anon'a kapalı, özel kazanan hesaplayıcısı authenticated rolüne kapalı; yeni genel v2 fonksiyonu SECURITY INVOKER.

Android derleme/test sonucu commit CI'sından ayrıca doğrulanacaktır. Emülatör/görsel kontrol ve gerçek cihaz Google Play satın alma işlemi bu denetimde yapılmadı. Ürünlerin Play Console hesap/bölge tekliflerinin gerçek cihazda kullanılabilirliği, sunucu erişim testiyle kanıtlanmış sayılmaz. APK teslim edilmedi.
