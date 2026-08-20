# Son Harf

Android için iki kişilik gerçek zamanlı Türkçe kelime düellosu.

## Güncel sürüm: v0.8.0
- Kotlin + Jetpack Compose
- İki kişilik oda / davet kodu ve gerçek zamanlı maç
- Son harfle kelime zinciri, normal ve uzman modlar
- Süreli turlar, sohbet, rövanş ve bot zorlukları
- Profil, kariyer, XP, seviye, lig ve başarımlar
- Günlük ödül, günlük görevler ve haftalık hedefler
- Arkadaş düellosu, hızlı tepkiler ve maç sonucu paylaşımı
- VIP abonelik ve uygulama içi ürün altyapısı
- Uygulama içinden kalıcı hesap silme
- Supabase Auth, Postgres, Realtime ve Edge Functions
- Google Play Billing ve sunucu taraflı satın alma doğrulama altyapısı

## Güvenlik
Kritik oyun kuralları, ekonomi ve Google Play satın alma hakları istemciye güvenmeden sunucu tarafında doğrulanır. Supabase RPC erişimleri RLS ve rol izinleriyle sınırlandırılır; anonim kullanıcılar `SECURITY DEFINER` oyun/ekonomi RPC'lerini çalıştıramaz.

## Yayın
CI her `main` değişikliğinde QA APK üretir. İmzalı release APK/AAB üretimi için upload-keystore ve production AdMob secret'ları gerekir. Ayrıntılar `docs/RELEASE_BUILD.md` dosyasındadır.
