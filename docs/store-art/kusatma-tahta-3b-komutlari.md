# Kelime Kuşatması: 3B tahta ve bonus ikonu komutları (GPT / Higgsfield)

Amaç: Kelime Kuşatması'na klasik kelime oyunlarından (Kelimelik, Scrabble) ayrı, kendine ait bir görünüm vermek.
Tahta bir **harita / kale arazisi**, kareler **taş plakalar**, bonuslar ise **yere gömülü 3B nesneler** olacak.
Renkli bonus kareleri, "2K / 3H" yazıları, ahşap raf ve düz bej tahta bu görünümde **yok**.

## Nasıl çalışacak (önemli)

- **Tahta görseli yalnızca zemin ve dokudur.** Harfleri, bonus işaretlerini, bölge renklerini (yeşil / kırmızı küp) ve dokunma alanlarını oyun kendisi çizer. Bu yüzden görselde karelerin **milimetrik hizalı** olması şart.
- **Higgsfield "gerçek 3B" nesne dosyası üretmez.** Ürettiği şey 3B render görünümlü görseldir. Bu görseller tahtada 3B gibi görünür ve telefonda hızlı çalışır. Döndürülebilir 3B model (GLB) gerekmez.
- **Her temadan önce ızgara şablonunu referans ver.** Aşağıdaki ölçülerle hazırladığım boş ızgara PNG'sini Higgsfield'da "image reference / structure" olarak yükle. Kareler böylece yerinden kaymaz. Şablon: `docs/store-art/kusatma-izgara-sablonu.png`.
- **Üretilen görseli bana gönder.** Kare hizasını ölçüp düzeltir, uygulamaya bağlar ve tahtada test ederim.

## Ölçüler (her tahta teması için aynı)

| Özellik | Değer |
|---|---|
| Görsel | 2048 × 2048 px, kare, PNG (şeffaflık gerekmez) |
| Izgara | **15 × 15** kare |
| Dış çerçeve | her kenarda 64 px (süsler yalnız burada) |
| Oyun alanı | 1920 × 1920 px, sol üst köşe (64, 64) |
| Kare adımı | 128 px (1920 / 15) |
| Taş plaka | 120 × 120 px, her karenin tam ortasında |
| Plakalar arası derz | 8 px (her taraf 4 px), **plakayla neredeyse aynı renk** |
| Merkez kare | 8. satır, 8. sütun: (960, 960) merkezli plaka |
| Işık | yumuşak, bulutlu gün ışığı; kısa ve silik gölge |
| Kamera | tam tepeden (orthographic top-down), **perspektif yok** |

Plaka merkezleri: x = 128 + 128·i, y = 128 + 128·j (i, j = 0…14).

## Göz yormaması için (en önemli kural)

225 açık kareyi koyu, keskin çizgilerle ayırınca çizgilerin kesiştiği yerlerde gri noktalar yanıp söner gibi görünür (Hermann ızgara yanılsaması). Göz kayar, baş döner. Kesişimlerde parlayan nokta varsa etki çok daha güçlü olur. Bunu önlemek için:

- Derzler koyu çizgi olmayacak. Plakayla **aynı renk ailesinde, en fazla %8–10 daha koyu**, ince ve yumuşak olacak.
- Kesişim noktalarında ışık, parıltı, nokta, çivi ya da süs **olmayacak**.
- Plakalar **mat** olacak. Parlak kenar ışığı ve sert beyaz bevel çizgisi olmayacak.
- Plakalar arasında **hafif doğal ton farkı** olacak, çünkü birebir aynı kareler tekrarı artırır.
- Gölgeler kısa ve silik olacak (bulutlu gün ışığı).
- Uygulama oyunda da aynı kuralı uyguluyor: boş karelerde çizgi yok, yalnız hafif gölge var.

---

## Ortak kurallar (her tahta komutunun başına aynen ekle)

```
Top-down orthographic 3D render of a game board, 2048x2048 px, exactly square. Camera perfectly overhead, zero perspective, zero lens distortion, zero tilt. The playing field is a perfect 15 by 15 grid of identical square stone plates: each plate 116x116 px, plate centres on an exact 128 px pitch starting at (128,128), 12 px joints between plates, all plates the same size, perfectly aligned in straight rows and columns, none missing, none covered, none rotated. Outer frame band of 64 px on every side; decorative elements (rocks, plants, water, snow, ruins) are allowed ONLY inside this outer frame band and must never overlap any plate. Plates are 120x120 px with 8 px joints. EYE COMFORT IS CRITICAL: the joints must be very low contrast, the same colour family as the plates and only 8-10% darker, soft and thin, never dark lines, never black grout; absolutely no lights, glows, dots, studs or ornaments where joints cross. Plates are matte with a very soft rounded bevel (no bright edge highlights), gentle natural tone variation from plate to plate, realistic physically based stone, soft overcast daylight with short faint shadows. Plate tops are calm and low-contrast so letters drawn on top stay readable. NO letters, NO numbers, NO symbols, NO coloured bonus squares, NO text, NO logos, NO watermark, NO wooden rack, NO game pieces. Must not resemble Scrabble or any existing word-game board: no red, pink, light-blue or dark-blue premium squares, no star in the centre, no beige cardboard look. Style: premium mobile strategy-game terrain, like a miniature fortified landscape seen from above.
```

Olumsuz komut (Higgsfield "negative prompt" alanı):

```
perspective, tilted camera, fisheye, uneven grid, missing tiles, merged tiles, different tile sizes, dark grout lines, black joints, high contrast grid, glowing dots, lights at intersections, shiny bevels, glossy tiles, text, letters, numbers, logo, watermark, scrabble, red squares, pink squares, blue squares, star, wooden rack, cardboard, people, animals, blurry, low resolution
```

**Higgsfield ayarları:** kare oran (1:1), en yüksek çözünürlük, "structure / reference" gücü yüksek (0.7–0.85). Sabit bir seed kullan, böylece dört tema aynı dilde çıkar.

---

## Tahta temaları

### 1. Taş Kale (varsayılan, açık tema)
Dosya adı: `board_stone_keep.png`

```
[ORTAK KURALLAR]
Theme: sunlit ancient stone keep. Plates are warm limestone (#D9D0BC to #CFC5AE) with faint natural cracks; joints are the same limestone tone only slightly darker (#C4B99F), with a hint of moss, never dark. The outer frame band is a low weathered castle wall of rough grey-beige blocks with small tufts of grass and a few pebbles tucked against it; four corner towers seen from above as round stone caps. Overall palette calm, warm and light so dark ink letters read clearly.
```

### 2. Vadi Nehri (yeşil tema)
Dosya adı: `board_river_valley.png`

```
[ORTAK KURALLAR]
Theme: green river valley fortress. Plates are pale granite (#CFCBBE); joints are soft grey-green (#B9B7A8), only slightly darker than the plates, never dark. In the outer frame band only: a thin turquoise stream running around the four sides, mossy boulders in each corner, small shrubs. The stream and boulders never cross into the 15x15 plate area. Palette fresh and natural, plate tops stay light and calm.
```

### 3. Kar Kalesi (kış teması)
Dosya adı: `board_frost_citadel.png`

```
[ORTAK KURALLAR]
Theme: frozen mountain citadel. Plates are frosted blue-white stone (#E6ECF2 to #D8E0EA), plate centres clear and smooth; joints are filled with soft snow in a slightly darker blue-grey (#C9D3DF), never dark slate lines. Outer frame band: dark slate wall (#3E4652) with snow on top, small snowy pine trees and ice rocks in the corners. Cold daylight from the top-left, crisp soft shadows. Plate tops must stay evenly light so letters read clearly; no ice glare on plates.
```

### 4. Obsidyen Kale (siyah tema)
Dosya adı: `board_obsidian_bastion.png`

```
[ORTAK KURALLAR]
Theme: night obsidian bastion. Plates are matte dark basalt (#2A2D33 to #33373F); joints are only slightly darker basalt (#23262B), no gold lines on the plates, no glowing joints, no lights or dots at intersections. Outer frame band: black volcanic stone wall with thin gold inlay lines and four small brazier-like corner caps (unlit or softly glowing). Plate tops stay uniformly dark and matte so light ivory letters read clearly; no reflections on plates.
```

---

## Bonus ikonları (3B nesne, şeffaf arka plan)

Bunlar karenin ortasına oturan küçük 3B nesnelerdir. Oyun, ikonun altına kısa etiketi (ör. "Harf Gücü") kendisi yazar. İkonların üstünde yazı olmaz.

### Ortak kurallar (her ikon komutunun başına aynen ekle)

```
Single 3D game icon, 512x512 px PNG with a fully TRANSPARENT background (no floor, no backdrop, no shadow plate, no frame). The object is centred and fills about 70% of the canvas, seen from a 3/4 top view as if resting on a stone floor tile. Premium stylised 3D render, physically based materials, soft key light from the top-left, subtle rim light, crisp silhouette that reads clearly at 40 px. NO text, NO letters, NO numbers, NO logos, NO watermark. Must not look like a coloured square, a card or a Scrabble premium square.
```

Olumsuz komut:

```
text, letters, numbers, logo, watermark, background, floor, square tile, card, frame, border, scrabble, blurry, low resolution
```

### Harf Gücü (2H): `bonus_letter_boost.png`
```
[ORTAK KURALLAR]
A single cut teal gemstone (rhombus / diamond shape, colour #1F8A7A to #4FC3B0) set into a small round bronze socket, glowing softly from inside with a calm teal light.
```

### Harf Gücü+ (3H): `bonus_letter_boost_plus.png`
```
[ORTAK KURALLAR]
Two stacked cut teal gemstones (rhombus shape, deeper teal #0E6B5E to #2FB3A0) set into a bronze socket with three small prongs, stronger inner glow and a few tiny floating light sparks. Clearly a stronger version of a single teal gemstone.
```

### Kelime Akımı (2K): `bonus_word_surge.png`
```
[ORTAK KURALLAR]
A small amber-glass wave sculpture (a single curling wave, colour #D9922B to #F2C064) rising from a round bronze base, warm light passing through the glass.
```

### Kelime Akımı+ (3K): `bonus_word_surge_plus.png`
```
[ORTAK KURALLAR]
A double curling amber-glass wave sculpture (two waves, deeper amber #A25A06 to #E8A33A) on a round bronze base with a thin gold ring, stronger warm glow and a few floating amber sparks. Clearly a stronger version of the single amber wave.
```

### Başlangıç Mührü (merkez, 4K): `bonus_starting_seal.png`
```
[ORTAK KURALLAR]
A round royal seal embedded in the ground: a deep navy stone disc (#1E2A44) with a raised gold four-pointed star emblem in the centre and a gold outer ring, gently glowing gold edges. Majestic, clearly the most important cell on the board. No crown, no letters.
```

### Sürpriz Ödül (+25): `bonus_surprise_reward.png`
```
[ORTAK KURALLAR]
A small closed treasure pouch / relic box made of dark leather and gold trim with a soft golden glow leaking from the lid, a few gold sparkles around it. Friendly and rewarding, not a star, not a coin.
```

---

## Ek (isteğe bağlı): harf taşı

Oyuncu taşları şu an kodla çiziliyor. İstenirse taş dokusu da üretilebilir.

Dosya adı: `tile_ivory_stone.png`

```
Single square game tile, 256x256 px PNG with TRANSPARENT background, top-down orthographic view, no perspective. A smooth ivory stone plaque (#F4EDDC) with a soft 8 px bevel, very subtle stone grain, gentle top-left light and a short soft shadow inside the canvas. Face completely blank: NO letter, NO number, NO symbol, NO text. Must not look like a wooden Scrabble tile.
```

---

## Telif ve özgünlük kontrol listesi (üretimden sonra)

- [ ] Karelerde harf, sayı, yazı ya da logo yok.
- [ ] Kırmızı, pembe, açık mavi, lacivert renkli bonus kareleri yok. Bonuslar kare değil, 3B nesne.
- [ ] Merkezde yıldız yok; merkez **Başlangıç Mührü**.
- [ ] Ahşap raf ve düz bej karton tahta görünümü yok.
- [ ] Her görselin hangi araçla, hangi hesapla ve hangi tarihte üretildiği `docs/ASSET_LICENSE_AND_USAGE.md` dosyasına yazılacak. Higgsfield ve GPT görsellerinde ticari kullanım hakkı, kullanılan planın şartlarına bağlıdır. Kullanılan planın ticari kullanıma izin verdiğini kontrol et.
- [ ] Başka bir oyunun ekran görüntüsü "referans görsel" olarak **kullanılmadı**. Referans yalnızca bizim ızgara şablonumuz.
