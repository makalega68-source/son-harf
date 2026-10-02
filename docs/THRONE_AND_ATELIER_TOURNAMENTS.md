# Taht ve Kelime Atölyesi turnuvaları

Takvim Europe/Istanbul saat dilimindedir; telefon saati sonuç ve XP belirlemez.

## Turnuvalar

- Her çift saatte (00, 02, … 22) bir turnuva açılır. Normal turnuva XP çarpanı ×1,5'tir.
- 19:00 ek turnuvadır. 19:00 ve 22:00 turnuvaları ×3 XP verir; 22:00 için ikinci bir turnuva oluşturulmaz.
- Bir turnuva 30 dakika sürer: ilk 10 dakika hazırlık, ikinci 10 dakika yarı final, son 10 dakika final.
- Oyun süreleri aşamalar için sırasıyla 60, 120 ve 180 saniyedir. Görev sayıları 6, 15 ve 24'tür.
- Katılım ücretsizdir ve PRO şartı yoktur. Her aşamada tek deneme vardır; önceki aşamayı tamamlayan herkes sonraki aşamaya geçebilir. Elenme yoktur.
- Bir oyuncu turnuva boyunca aynı dilde devam eder. Aynı dil/aşamadaki oyuncular aynı başlangıç harfleri ve görevleri ile başlar.
- Aşamaya, oyunun bitmesine en az oyun süresi + 10 saniye kalıyorsa girilebilir. Bir önceki aşama bitmeden sonraki aşamaya girilemez.
- Sonuç aşama bitiminden en fazla 15 saniye sonra kaydedilebilir. Ağ hatasında bu pencere içinde aynı sonucu tekrar göndermek mümkündür ve XP ikinci kez verilmez.
- Başlanan ama tamamlanmayan aşama yeniden başlatılmaz. Bağlantı koptuğunda devam eden yerel oyun kalır; uygulamadan çıkıp yeniden girmek aşamayı geri yüklemez.
- Sıralama önce tamamlanan aşama sayısı, ardından toplam oyun puanı, son bitirme zamanı, ardından kullanıcı kimliği ile belirlenir.
- Son biten ve en az bir tamamlanan sonucu bulunan turnuvanın ilk üç oyuncusu ana ekranda kayan yazıyla gösterilir. Sıfır katılımlı turnuva sahte kazanan üretmez.
- Ana sayaç her çift saatte 02:00:00 değerine döner. 19:00 ek turnuvasının sayacı ayrıca gösterilir.
- Takvim ve aşama kapanışı zaman karşılaştırmalarıyla otomatik işler; açık uygulama veya cron görevi gerektirmez. Sunucu sorgusu ilgili anda güncel aşamayı hesaplar.

## XP ve haftalık Taht

- Resmî Kuşatma ve Son Harf maçı: 35 katılım XP; galibiyet +85 XP (toplam 120).
- İptal edilmiş maçlar XP vermez. Son Harf'te en az 2 geçerli kelime; Kuşatma'da en az 2 hamle ve pozitif kelime puanı gerekir. İnsanlara XP verilir; botlar sıralamaya girmez.
- Atölye günlük yarışının temel XP'si `min(120, 35 + floor(skor / (20 × dakika)))` olur.
- Turnuva aşamasının aynı temel XP'sine turnuvanın çarpanı uygulanır; kesirli sonuç aşağı yuvarlanır. Üç aşama tamamlanınca normal turnuva en çok 540, özel turnuva en çok 1.080 XP verir.
- Çarpan sadece turnuva aşamalarına uygulanır. Haftalık görev ödülleri çarpılmaz.
- Antrenman puanı yerel rekora yazılır; resmî haftalık XP değildir. Eski rating/lig, coin ve sezon biletinin mevcut ödülleri değiştirilmez.
- Her maç/aşama benzersiz bir kaynak kaydıyla yazılır. İstemci haftalık toplamı, çarpanı veya görev ödülünü belirleyemez.
- Türkiye saatine göre pazartesi 00:00 yeni hafta başlar. Geçmiş kayıtlar korunur; yeni haftanın puanları sıfırdan toplanır.
- Haftalık sıralama toplam XP, ilk XP kazanma zamanı ve kullanıcı kimliğiyle tek bir lider seçer. Bu haftanın lideri Taht Sahibi olarak gösterilir; geçen haftanın sahibi ayrıca yayınlanır.
- Profil seviyesi/level eklenmez.

## Haftalık görevler

Her oyun için ayrı üç görev, toplam dokuz görev vardır:

| Görev | Kuşatma / Son Harf | Atölye | Ödül |
|---|---|---|---|
| Katılım | 5 resmî maç | 5 günlük yarış veya turnuva aşaması | 100 XP |
| Ustalık | 3 galibiyet | 12 görev tamamlama | 150 XP |
| Deneyim | 1.000 oyun XP | 1.000 oyun XP | 200 XP |

Görev ödülleri otomatik ve haftada bir defa eklenir. Deneyim görevinin ilerlemesi görev bonuslarını içermez; döngüsel bonus oluşmaz. Her oyunun haftalık XP, maç, galibiyet ve görev toplamı Taht ekranında görülebilir.

## Doğrulama ve sınırlar

XP kayıtlarına doğrudan istemci yazamaz; RLS ve RPC izinleri uygulanır. Sunucu süreyi, aşama katılımını, skor/kelime/görev sınırlarını, benzersiz kelime listesini ve sözlük üyeliğini denetler. Atölye motoru cihazda çalışır; tüm harf havuzu geçişlerini sunucuda yeniden oynatan tam bir hile denetimi bu sürümde yoktur. Eski günlük atölye sonuçları da mevcut makullük denetimleriyle kabul edilir.

Test: `supabase/tests/throne_tournament_regression.sql` bütün denemeleri transaction içinde yapıp geri alır. Android birim testleri İstanbul sayaç sınırlarını ve mevcut oyun regresyonlarını doğrular.
