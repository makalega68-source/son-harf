# Kelime Kuşatması: Canva için 3B tahta ve bonus komutları

Canva'nın yapay zekâ görsel aracı (Magic Media / Dream Lab) için uyarlanmış sürüm.
Tasarım dili, isimler ve renkler `kusatma-tahta-3b-komutlari.md` ile aynıdır.

## Canva'da farklı olan şeyler

- **Kısa komut:** Komutlar kısa tutuldu, çünkü Canva uzun komutları kısaltıyor. Her komut ~250 karakterin altında.
- **Olumsuz komut alanı yok:** "yazı yok, harf yok" gibi kurallar komutun içine yazıldı.
- **Seed ve piksel ölçüsü ayarı yok:** Canva 15×15 karelik hizalı bir ızgarayı tek görselde garanti edemez. Bu yüzden **tahtayı tek parça ürettirmiyoruz**:
  1. Canva'da yalnızca **tek bir taş plaka** ve **boş bir çerçeve / zemin** üret.
  2. Bana gönder.
  3. Oyun 15×15 ızgarayı bu plakayla kodda, milimetrik hizalı kurar. Böylece kareler asla kaymaz.
- **Şeffaf arka plan:** Canva görseli şeffaf üretmez. Bonus ikonlarını üret, sonra **Düzenle → Arka Plan Kaldırıcı** (BG Remover) ile arka planı sil ve **PNG, şeffaf arka plan** olarak indir. Bu özellik Canva Pro ister.

## Göz yormaması için

Açık kareleri koyu çizgilerle ayırmak, çizgilerin kesiştiği yerlerde yanıp sönen noktalar gibi görünür (Hermann ızgara yanılsaması) ve baş döndürür. Bu yüzden:

- Plakalar mat ve yumuşak kenarlı olacak.
- Oyun plakaları aralarında **plakayla aynı renkte, çok ince** boşlukla dizecek; koyu çizgi ve kesişim noktasında ışık olmayacak.
- Plakada parlak kenar ışığı istemiyoruz.

## Canva ayarları

- Oluştur: **Görsel**
- En boy oranı: **Kare (1:1)**
- Stil: **Yok** ya da **3D / Render**. Hepsinde aynı stili seç.
- Beğendiğin ilk sonucu bir sonrakinde **stil referansı** olarak kullan ("Buna benzer oluştur"), böylece hepsi aynı ailede kalır.
- İndirme: **PNG**, en yüksek boyut. Tahta parçaları için 2048 px, ikonlar için 1024 px yeterli.

---

## 1. Taş plaka (her tema için 1 adet)

Üst yüzü düz ve sakin olmalı, çünkü harf bunun üstüne yazılacak.

**Taş Kale**, dosya adı `plate_stone_keep.png`:
```
Top-down 3D render of one square limestone floor plate, warm beige-grey, matte, very soft rounded edges, faint cracks, calm flat top, soft overcast light, no shiny edges, plain beige background. No text, no letters, no symbols.
```

**Vadi Nehri**, dosya adı `plate_river_valley.png`:
```
Top-down 3D render of one square pale granite floor plate, matte, very soft rounded edges, calm flat top, soft overcast light, no shiny edges, plain pale grey background. No text, no letters, no symbols.
```

**Kar Kalesi**, dosya adı `plate_frost_citadel.png`:
```
Top-down 3D render of one square frosted blue-white stone plate, matte, very soft rounded edges, smooth clear top, soft overcast light, no shiny edges, plain pale blue background. No text, no letters.
```

**Obsidyen Kale**, dosya adı `plate_obsidian.png`:
```
Top-down 3D render of one square matte dark basalt plate, matte, very soft rounded edges, no gold lines, no reflections, soft light, plain dark grey background. No text, no letters.
```

## 2. Çerçeve ve zemin (her tema için 1 adet)

Ortası **boş** olmalı. Oyun karelerini oraya kendisi yerleştirir.

**Taş Kale**, dosya adı `frame_stone_keep.png`:
```
Top-down 3D render of a square castle courtyard, empty flat dirt centre, low weathered stone wall on all four edges, round stone towers in the corners, grass tufts. No tiles, no text.
```

**Vadi Nehri**, dosya adı `frame_river_valley.png`:
```
Top-down 3D render of a square valley plateau, empty flat centre, thin turquoise stream running around all four edges, mossy boulders and shrubs in the corners. No tiles, no text.
```

**Kar Kalesi**, dosya adı `frame_frost_citadel.png`:
```
Top-down 3D render of a square snowy mountain fort, empty flat snowy centre, dark slate wall on all four edges, snowy pine trees and ice rocks in the corners. No tiles, no text.
```

**Obsidyen Kale**, dosya adı `frame_obsidian.png`:
```
Top-down 3D render of a square night bastion, empty flat dark centre, black volcanic stone wall on all edges with thin gold inlay lines, softly glowing braziers in the corners. No tiles, no text.
```

## 3. Bonus ikonları (üret, sonra arka planı kaldır)

Oyun ikonun altına kısa etiketi kendisi yazar, bu yüzden ikonda yazı olmaz.

**Harf Gücü**, dosya adı `bonus_letter_boost.png`:
```
3D game icon: one cut teal diamond-shaped gemstone set in a small round bronze socket, soft inner glow, 3/4 top view, plain white background. No text, no numbers.
```

**Harf Gücü+**, dosya adı `bonus_letter_boost_plus.png`:
```
3D game icon: two stacked cut deep-teal gemstones in a bronze socket with three prongs, strong inner glow, tiny light sparks, 3/4 top view, plain white background. No text, no numbers.
```

**Kelime Akımı**, dosya adı `bonus_word_surge.png`:
```
3D game icon: a small amber glass sculpture of one curling wave on a round bronze base, warm light passing through, 3/4 top view, plain white background. No text, no numbers.
```

**Kelime Akımı+**, dosya adı `bonus_word_surge_plus.png`:
```
3D game icon: a double curling deep-amber glass wave sculpture on a bronze base with a thin gold ring, strong warm glow, amber sparks, 3/4 top view, plain white background. No text.
```

**Başlangıç Mührü**, dosya adı `bonus_starting_seal.png`:
```
3D game icon: a round royal seal disc of deep navy stone with a raised gold four-pointed star and a gold outer ring, glowing gold edges, 3/4 top view, plain white background. No crown, no text.
```

**Sürpriz Ödül**, dosya adı `bonus_surprise_reward.png`:
```
3D game icon: a small closed treasure pouch of dark leather with gold trim, golden light leaking from the top, a few sparkles, 3/4 top view, plain white background. No coins, no star, no text.
```

**İsteğe bağlı, boş harf taşı**, dosya adı `tile_ivory_stone.png`:
```
Top-down 3D render of one square smooth ivory stone tile, soft rounded bevel, very subtle grain, blank face, soft light from top-left, plain dark background. No letter, no text, not wooden.
```

---

## Kontrol listesi (indirmeden önce)

- [ ] Hiçbir görselde yazı, harf, sayı ya da logo yok.
- [ ] Bonuslar renkli kare değil, 3B nesne. Kırmızı, pembe ya da mavi bonus karesi yok.
- [ ] Çerçevelerin ortası boş; plakaların üstü düz ve sakin.
- [ ] İkonların arka planı kaldırıldı ve dosyalar şeffaf PNG.
- [ ] Başka bir oyunun görseli referans olarak kullanılmadı.
- [ ] Canva planının yapay zekâ görsellerini ticari kullanıma izin verdiği kontrol edildi.

Dosyaları gönderdiğinde tahtayı ve ikonları uygulamaya bağlar, hizayı test ederim.
