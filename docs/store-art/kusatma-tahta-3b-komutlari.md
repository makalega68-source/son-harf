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
| Taş plaka | 116 × 116 px, her karenin tam ortasında |
| Plakalar arası derz | 12 px (her taraf 6 px) |
| Merkez kare | 8. satır, 8. sütun: (960, 960) merkezli plaka |
| Işık | sol üstten, 45°, yumuşak; bütün plakalarda aynı gölge yönü |
| Kamera | tam tepeden (orthographic top-down), **perspektif yok** |

Plaka merkezleri: x = 128 + 128·i, y = 128 + 128·j (i, j = 0…14).

---

## Ortak kurallar (her tahta komutunun başına aynen ekle)

```
Top-down orthographic 3D render of a game board, 2048x2048 px, exactly square. Camera perfectly overhead, zero perspective, zero lens distortion, zero tilt. The playing field is a perfect 15 by 15 grid of identical square stone plates: each plate 116x116 px, plate centres on an exact 128 px pitch starting at (128,128), 12 px joints between plates, all plates the same size, perfectly aligned in straight rows and columns, none missing, none covered, none rotated. Outer frame band of 64 px on every side; decorative elements (rocks, plants, water, snow, ruins) are allowed ONLY inside this outer frame band and must never overlap any plate. Plates are flat-topped with a subtle 3D bevel (about 6 px), physically based materials, soft ambient occlusion in the joints, one soft key light from the top-left at 45 degrees so every plate casts the same short shadow. Plate tops are mostly calm and low-contrast so letters drawn on top stay readable: keep texture detail subtle in the centre of each plate. NO letters, NO numbers, NO symbols, NO coloured bonus squares, NO text, NO logos, NO watermark, NO wooden rack, NO game pieces. Must not resemble Scrabble or any existing word-game board: no red, pink, light-blue or dark-blue premium squares, no star in the centre, no beige cardboard look. Style: premium mobile strategy-game terrain, like a miniature fortified landscape seen from above.
```

Olumsuz komut (Higgsfield "negative prompt" alanı):

```
perspective, tilted camera, fisheye, uneven grid, missing tiles, merged tiles, different tile sizes, text, letters, numbers, logo, watermark, scrabble, red squares, pink squares, blue squares, star, wooden rack, cardboard, people, animals, blurry, low resolution
```

**Higgsfield ayarları:** kare oran (1:1), en yüksek çözünürlük, "structure / reference" gücü yüksek (0.7–0.85). Sabit bir seed kullan, böylece dört tema aynı dilde çıkar.

---

## Tahta temaları

### 1. Taş Kale (varsayılan, açık tema)
Dosya adı: `board_stone_keep.png`

```
[ORTAK KURALLAR]
Theme: sunlit ancient stone keep. Plates are warm limestone (#D9D0BC to #CFC5AE) with faint natural cracks and very soft moss in the joints. The outer frame band is a low weathered castle wall of rough grey-beige blocks with small tufts of grass and a few pebbles tucked against it; four corner towers seen from above as round stone caps. Overall palette calm, warm and light so dark ink letters read clearly.
```

### 2. Vadi Nehri (yeşil tema)
Dosya adı: `board_river_valley.png`

```
[ORTAK KURALLAR]
Theme: green river valley fortress. Plates are pale granite (#CFCBBE) with a faint green patina in the joints. In the outer frame band only: a thin turquoise stream running around the four sides, mossy boulders in each corner, small shrubs. The stream and boulders never cross into the 15x15 plate area. Palette fresh and natural, plate tops stay light and calm.
```

### 3. Kar Kalesi (kış teması)
Dosya adı: `board_frost_citadel.png`

```
[ORTAK KURALLAR]
Theme: frozen mountain citadel. Plates are frosted blue-white stone (#E6ECF2 to #D8E0EA) with a thin layer of powder snow at the edges of each plate only, plate centres clear and smooth. Outer frame band: dark slate wall (#3E4652) with snow on top, small snowy pine trees and ice rocks in the corners. Cold daylight from the top-left, crisp soft shadows. Plate tops must stay evenly light so letters read clearly; no ice glare on plates.
```

### 4. Obsidyen Kale (siyah tema)
Dosya adı: `board_obsidian_bastion.png`

```
[ORTAK KURALLAR]
Theme: night obsidian bastion. Plates are matte dark basalt (#2A2D33 to #33373F) with a very subtle warm gold hairline bevel on each plate edge. Joints glow faintly with a dim amber light (very low intensity). Outer frame band: black volcanic stone wall with thin gold inlay lines and four small brazier-like corner caps (unlit or softly glowing). Plate tops stay uniformly dark and matte so light ivory letters read clearly; no reflections on plates.
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
