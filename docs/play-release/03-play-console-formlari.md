# Play Console formları — hazır cevaplar

## Veri güvenliği (Data safety)
- Veri topluyor mu? **Evet**. Aktarımda şifreli mi? **Evet (HTTPS)**. Kullanıcı silme isteyebilir mi? **Evet (uygulama içi)**.
- Toplanan veri türleri:
  - Kişisel bilgiler: **E-posta adresi** (hesap yönetimi, zorunlu), **Ad** (görünen ad; uygulama işlevselliği)
  - Fotoğraflar: **Profil fotoğrafı** (isteğe bağlı; uygulama işlevselliği)
  - Mesajlar: **Uygulama içi mesajlar** (sohbet; uygulama işlevselliği)
  - Finansal bilgi: **Satın alma geçmişi** (uygulama işlevselliği)
  - Uygulama etkinliği: **Uygulama etkileşimleri / oyun ilerlemesi** (işlevsellik, analiz)
  - Cihaz kimlikleri: **Reklam kimliği** (reklamlar; AdMob) — "Paylaşılıyor: Evet" (AdMob)
  - Ses: **Ses kaydı** — işaretlemeyin/ya da "işlenir, saklanmaz, isteğe bağlı" (maskot sesle yazma)
- Satılmaz; üçüncü taraflarla yalnızca hizmet sağlayıcı olarak.

## İçerik derecelendirmesi (IARC)
- Kategori: Oyun. Şiddet/korku/cinsellik/kumar: **Hayır**.
- Kullanıcılar etkileşim kurabilir mi (sohbet)? **Evet**. Dijital satın alma: **Evet**.
- Beklenen sonuç: PEGI 3 / Herkes (sohbet nedeniyle "Kullanıcılar etkileşimde bulunur" notu).

## Hedef kitle
- Hedef yaş: **13+** (13-15, 16-17, 18+). Çocuklara yönelik değil.

## Reklamlar
- Uygulama reklam içeriyor mu? **Evet** (üstte ince banner, isteğe bağlı ödüllü reklam; PRO'da yok).

## Uygulama erişimi (inceleme için)
- Giriş gerekiyor: incelemeciye bir **test hesabı** (e-posta + şifre) verin. Kendi PRO/sahip
  hesabınızı DEĞİL, sıradan bir test hesabı açıp verin.

## Uygulama içi ürünler (Play Console > Monetize > Products)
Tek seferlik (Managed):
| Ürün kimliği | Ad | Önerilen fiyat |
|---|---|---|
| pro_lifetime | Kelime Tahtı PRO (kalıcı) | 479 TL |
| mascot_klasik | Maskot: Obi | 680 TL |
| mascot_pembe | Maskot: Pinki | 680 TL |
| mascot_mavi_seytancik | Maskot: Buzi | 680 TL |
| mascot_kirmizi_seytancik | Maskot: Zıpır | 680 TL |
| mascot_tekir | Maskot: Mırnav | 680 TL |
| mascot_robot | Maskot: Bipbop | 680 TL |
| mascot_astronot | Maskot: Nova | 680 TL |
| coins_500 / coins_1500 / coins_3500 / coins_8000 | Son Coin paketleri | mağaza fiyatlarına göre |
| series_game / letter_table / score_calculator | Kelime Tahtı araçları | 129 / 65 / 129 TL |
Abonelik (varsa): vip_monthly, vip_yearly, season_pass_monthly — kullanmıyorsanız oluşturmayın.

## Yayın sırası
1. GitHub Secrets'ı gir (anahtar + AdMob) → PR #459'u main'e birleştir → Actions
   "son-harf-release-N" içinden **app-release.aab** indir.
2. Play Console > Test > **Dahili test** kanalına AAB yükle, kendin ve 1-2 kişi dene.
3. Sorun yoksa **Üretim**e terfi ettir, incelemeye gönder (ilk inceleme birkaç gün sürebilir).
