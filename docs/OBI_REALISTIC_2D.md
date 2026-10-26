# Obi — Higgsfield ile geliştirilen 2D görünüm

Obi aynı canlı 2D yüz ve hareket sistemiyle çizilir. Yeni gövde ve ayrı tepe tutamı Higgsfield üzerinden üretilen saydam görsellerden hazırlanmıştır. Gerçek bir 3D motoru, video veya kare animasyonu eklenmemiştir.

## Uygulama

- Yalnız klasik Obi (`ORB`) yeni dokuyu kullanır. Diğer maskotların mevcut dokuları ve yüz değerleri korunur.
- Gövde ve tepe tutamı `word_siege_mascot/obi_realistic_{orb,tuft}.webp` dosyalarıdır. İkisi de 768 × 768 saydam WebP'dir; özgün 1254 birimlik koordinat sistemine oturtulmuştur. Gözler, ağız, kaşlar, eller ve tutam bağımsız hareket eder.
- Göz yüzeyinde yumuşak kenar gölgeleri ve ışık yansıması; ağız ve dilde hacimli renk geçişleri kullanılır. Bütün materyaller ve matrisler tekrar kullanılır; çizim döngüsüne nesne üretimi eklenmez.
- Dinlenmede küçük bir tebessüm, odaklanmada daha yumuşak kaşlar, gülmede yükselen yanaklar, şaşırmada yuvarlak ağız bulunur. Diğer canlı ifade ve hareket durumları korunur.
- Yeni katmanlar bulunamazsa özgün çizime dönülür. Doku çözme sonucu süreç genelinde önbelleğe alınır; kare başına doku çözülmez.
- Oyun kuralları, veriler, mağaza hakları, sesler ve oyun dışı Obi uygulaması değiştirilmez.

## Üretim kaydı

Kaynak oyun sürümü: `a2dc5c5ffd14e7aa39d8a17e72d332d9d0d81a78`.

Higgsfield GPT Image 2.5, high kalite:

- Ana tasarım: `e9819f8e-f855-46b1-9dea-905924f3e11e`, 2048 × 2048.
- On iki ifade referansı: `3789eacc-9a87-4515-88d3-a3d10998e303`, 3312 × 2480. Sıra: sakin, mutlu, gülen, şaşkın; odaklı, meraklı, üzgün, ağlayan; kızgın, uykulu, gururlu, göz kırpan.
- Ayrılabilir gövde kaynağı: `5e42cb19-bdb5-4edc-8f43-b3bd1fcc61cc`, 2048 × 2048.

On iki tam karakter görseli sanat referansıdır; oyundaki canlı yüz sisteminin yerine geçirilmez.

## Doğrulama

Saydamlık, WebP çözme, katman ölçüleri ve gövdenin özgün koordinatlara hizası yerel olarak kontrol edilir. Android derlemesi, mevcut birim testleri ve gerçek Android Canvas yüz ekranı `.github/workflows/obi-visual-qa.yml` ile çalıştırılır. Akışın sonuçları görülmeden APK doğrulanmış sayılmamalıdır.
