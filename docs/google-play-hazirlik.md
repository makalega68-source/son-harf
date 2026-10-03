# Kelime Tahtı — Google Play hazırlık listesi

Uygulama kodu Play'e yüklenecek şekilde hazır: `applicationId = com.sonharf.game`,
`targetSdk = 36`, `versionCode = 37`, `versionName = 1.0.0`. Yayın derlemesi R8 ile küçültülür ve
kullanılmayan kaynaklar atılır. Her gönderimde CI küçültülmüş sürümü de derler
(**Kelime-Tahti-Yayin-Kontrol** APK'sı); yüklemeden önce bu APK bir telefonda denenmelidir.

Aşağıdakiler hesap sahibinin yapması gereken, koddan yapılamayan adımlardır.

## 1. İmza anahtarı (zorunlu)
1. Bir yükleme anahtarı (upload keystore) oluştur ve güvenli bir yerde sakla (kaybolursa güncelleme yüklenemez).
2. GitHub deposunda Settings → Secrets and variables → Actions altına ekle:
   `SON_HARF_RELEASE_KEYSTORE_B64` (keystore dosyasının base64'ü), `SON_HARF_RELEASE_STORE_PASSWORD`,
   `SON_HARF_RELEASE_KEY_ALIAS`, `SON_HARF_RELEASE_KEY_PASSWORD`.
3. Play Console'da "Play App Signing" açık kalsın.

## 2. AdMob (zorunlu — yoksa yayın derlemesi bilerek durur)
1. AdMob'da uygulamayı oluştur; uygulama kimliğini `SON_HARF_ADMOB_APP_ID` gizli değişkenine yaz.
2. Bir **ödüllü** reklam birimi oluştur → `SON_HARF_ADMOB_REWARDED_ID`.
   Sunucu doğrulaması (SSV) adresi: `https://bzdtftzdjtjoqhtcqtxb.supabase.co/functions/v1/admob-ssv`
3. Bir **uyarlanabilir banner** birimi oluştur → `SON_HARF_ADMOB_BANNER_ID`.
4. Test kimlikleri yalnızca test APK'sında kullanılır; yayın derlemesi onlarla derlenmez.

## 3. Play Console ürünleri
- PRO abonelikleri ve Sezon Kartı (mevcut ürün kimlikleriyle).
- Klavyeler (50 TL): `keyboard_black_gold`, `keyboard_crystal`, `keyboard_midnight`, `keyboard_obsidian`.
- Maskotlar ve çerçeveler (mağazadaki ürün kimlikleriyle).
- Coin paketleri.

## 4. E-posta (kayıt kodu)
Supabase'in yerleşik e-posta servisi yalnızca proje ekibine gönderir. Yayından önce:
1. Supabase → Authentication → Emails → SMTP Settings: kendi SMTP servisini bağla (ör. Resend).
2. Authentication → Sign In / Providers → Email → "Confirm email" seçeneğini tekrar aç.

## 5. Mağaza sayfası ve politikalar
- Gizlilik politikası adresi (zorunlu): hesap, e-posta, profil fotoğrafı, oyun verisi, reklam kimliği.
- Veri güvenliği formu: e-posta, kullanıcı adı, fotoğraf (isteğe bağlı), satın alma geçmişi,
  reklam kimliği (AdMob), sesli giriş için mikrofon (yalnızca kullanıcı başlatınca).
- Hesap silme: uygulama içinde mevcut; ayrıca web'den silme isteği adresi gerekir.
- İçerik derecelendirme anketi, hedef kitle (13+ önerilir), reklam içerir işareti.
- Görseller: 512×512 simge, 1024×500 öne çıkan görsel, en az 2 telefon ekran görüntüsü
  (tablet ekran görüntüleri de önerilir).

## 6. Test kalıntıları (yayından önce kapat)
- İlk 5 hesaba verilen moderatör erişimi kalıcıdır; istenirse `moderator_accounts` tablosundan kaldırılır.
- Kayıt otomatik moderatör alımı 5 kişiden sonra kendiliğinden kapanır.
