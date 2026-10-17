# Obi — Google Play'e çıkış kontrol listesi

Kodda hazır olanlar:
- Uyarlanabilir uygulama simgesi (`res/mipmap-anydpi-v26/ic_obi.xml`) ve bildirim simgesi.
- Uygulama içi izin açıklaması ve gizlilik metni (giriş ekranında "Gizlilik"), aynı metin `PRIVACY.md` içinde.
- Bildirim izni Android 13+ cihazlarda ilk açılışta isteniyor.

Play Console'da yapılması gerekenler (kodla yapılamaz):
1. **Gizlilik politikası URL'si:** `PRIVACY.md` içeriğini herkese açık bir sayfada yayınlayıp adresini Play Console'a gir.
2. **Ön plan hizmeti beyanı:** Uygulama `specialUse` türünde ön plan hizmeti kullanıyor (ekranda duran maskot). Play Console → Uygulama içeriği → Ön plan hizmeti izinleri bölümünde kısa açıklama ve bir video istenir.
3. **Ekran üstü gösterme (SYSTEM_ALERT_WINDOW):** Mağaza açıklamasında Obi'nin neden diğer uygulamaların üzerinde durduğunu açıkça yaz.
4. **Kesin alarm (SCHEDULE_EXACT_ALARM):** Kullanıcının kurduğu alarmlar için kullanılıyor; beyan formunda "kullanıcının ayarladığı alarm/hatırlatma" seçeneği.
5. **Veri güvenliği formu:** Toplanan veri yok; Gemini kullanılırsa mesajlar kullanıcı isteğiyle Google'a iletilir (üçüncü taraf hizmet).
6. Uygulama kimliği şu an `com.obi.local`; mağazaya çıkmadan önce kalıcı bir kimlik seç (sonradan değiştirilemez).
