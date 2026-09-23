# Kelime Kuşatması Mascot Lab

Bu laboratuvar yalnızca `feature/kelime-kusatmasi-mascot-lab-20260923` branchinde çalışır. Production akışına dokunmaz.

## Değişmez kurallar

- Video, MP4, GIF veya önceden render edilmiş idle loop kullanılmaz.
- Onaylı orb silueti ve yüz kimliği korunur.
- Squash/stretch, jelly deformasyonu, morphing, uzuv, ekstra gövde ve alev yoktur.
- Nefes hissi yalnızca ışık yoğunluğu ile verilir.
- Hover hareketi yaklaşık 2–3 px aralığındadır.
- Maskot normal durumda oyun alanına ve aktif harf/kelime hedefine bakar.
- Uçuş sırasında yalnızca az sayıda, küçük ve hızla kaybolan runtime parıltı üretilir.

## Laboratuvar davranışları

`MascotLabActivity` gerçek zamanlı Compose renderer kullanır. Seçili tahta hücresi aynı anda maskotun bakış hedefidir. Otomatik izleme açıkken hedef hücre değişir ve bakış/yön tepkisi canlı olarak güncellenir.

Test kontrolleri: harf takibi, AI davranış demosu, konuşma/ağız hareketi, başarılı hamle, hatalı hamle, tehdit ve uçuş/parıltı izi.

`MascotBrain` arayüzü oyun olayları ile görsel maskot davranışını ayırır. Laboratuvarda güvenli ve deterministik `RuleBasedMascotBrain` kullanılır. Daha sonra gerçek AI servisi bu sözleşmenin başka bir implementasyonu olarak eklenebilir; skor, ekonomi veya oyun kuralı üzerinde yetkisi olmaz.

## Açma

Android cihazda / emülatörde deep link:

```text
sonharf://mascot-lab
```

ADB ile:

```bash
adb shell am start -a android.intent.action.VIEW -d sonharf://mascot-lab
```

## Asset

Laboratuvar onaylı, çemberleri kaldırılmış ve parıltıları azaltılmış master 2D asset'i yükler. Higgsfield tarafındaki canonical reference element: `kelime-kusatmasi-orb-live`.

Production öncesi CDN bağımlılığı kaldırılacak; doğrulanmış master asset uygulama paketine lokal drawable olarak gömülecek ve göz/ağız katmanları ayrı runtime katmanlara dönüştürülecektir.
