# Obi — Gizlilik

Obi, telefonda yaşayan kişisel bir maskot ve yardımcıdır. Obi'nin kendi sunucusu yoktur.

- **Telefonda kalanlar:** Notlar, alarmlar ve hatırlatmalar, alışveriş listesi, harcamalar, doğum günleri, geri sayımlar, ruh hâli kayıtları, sohbet geçmişi, Obi ile bağ puanı ve kıyafet seçimi. Bunlar yalnızca bu cihazda saklanır.
- **Gemini (isteğe bağlı):** Kullanıcı kendi Google AI Studio API anahtarını bağlarsa, Gemini'ye yazdığı mesajlar ve "Özetle / Çevir / Kibar cevap" ile gönderdiği pano metni yanıt üretmek için Google'a gönderilir. Anahtar Android Keystore ile şifrelenerek telefonda saklanır.
- **Hava durumu:** Yalnızca kullanıcının yazdığı şehir adı Open-Meteo'ya gönderilir. Konum izni istenmez.
- **Çeviri:** Google ML Kit ile cihaz üzerinde yapılır. Dil modeli ilk kullanımda indirilir.
- **Pil ve ekran süresi:** Yalnızca Obi'nin tepkileri (şarj uyarısı, mola önerisi) için cihazda okunur, hiçbir yere gönderilmez.
- Obi ekranı okumaz, başka uygulamaları kontrol etmez, mikrofon, kamera ya da konum kullanmaz.
- Sohbet, paneldeki "Sohbeti temizle" ile; tüm veriler uygulama kaldırılarak silinir.
