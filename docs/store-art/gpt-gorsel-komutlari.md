# Mağaza görselleri: GPT komutları

Bu ürünlerin gerçek bir görseli yok. Uygulama şu an bunları kodla çiziyor ya da basit bir vektör gösteriyor:

| Ürün | Mağaza kimliği | Kaydedilecek dosya adı |
|---|---|---|
| Siyah Tema | `theme_black` | `store_art_theme_black.png` |
| Ceviz & Fildişi | `theme_walnut_ivory` | `store_art_theme_walnut_ivory.png` |
| Kristal Tuşlar | `keyboard_crystal` | `store_art_keyboard_crystal.png` |
| Ressam Beresi | `hat_beret` | `store_art_hat_beret.png` |
| Çiçek Tacı | `hat_flower` | `store_art_hat_flower.png` |
| Büyücü Şapkası | `hat_wizard` | `store_art_hat_wizard.png` |
| Altın Silindir | `hat_top` | `store_art_hat_top.png` |
| PRO üyelik kartı | `premium_pro` | `premium_pro.png` |

## Her komutun başına eklenecek ortak kurallar

Bu paragrafı her komutun başına aynen yapıştır. Tüm görseller aynı aileden görünür.

```
Premium mobile word-game store icon. Square 1024x1024 PNG with a fully TRANSPARENT background (no backdrop, no floor, no shadow plate). One single object, centred, filling about 80% of the canvas, slightly tilted 3/4 view. Polished soft-3D game art style like high-end casual mobile games: smooth glossy materials, gentle rim light from the top-left, soft inner shadows, crisp clean edges that read well at 64 px. Colour palette harmonises with warm cream (#FAF3E3), rich gold (#D4A21F) and deep walnut brown (#3A2417). NO text, numbers, logos, watermarks or UI elements anywhere in the image; the only allowed letters are the ones the subject below explicitly asks for on letter tiles.
```

> Harf taşı olan temalarda tek istisna taşların üstündeki harflerdir. Komutta hangi harflerin yazılacağı açıkça belirtildi.

---

### 1. Siyah Tema: `store_art_theme_black.png`

```
[ortak kurallar]
Subject: a small, luxurious game board tile set in a MIDNIGHT BLACK theme. A floating square board piece made of matte black obsidian with a thin brushed-gold bevelled rim. On it sit three chunky square letter tiles in a row, glossy charcoal-black with gold lettering reading "K", "T", "Z" (only these three letters, bold serif, engraved gold). The middle tile glows softly with an emerald-green edge light, as if it was just placed. Tiny golden sparkles around the board. Elegant, premium, night-time feel; blacks must stay rich, not grey.
```

### 2. Ceviz & Fildişi: `store_art_theme_walnut_ivory.png`

```
[ortak kurallar]
Subject: a premium WALNUT AND IVORY game board piece. A floating square board section carved from dark polished walnut wood (visible fine wood grain, warm brown #3A2417 to #553624) with a slightly raised bevelled frame. Three chunky ivory letter tiles (#FAF3E3, like old piano keys, subtle ivory texture) sit in a row with deep brown engraved letters "C", "F", "T" (only these three letters). The middle tile has a soft emerald-green (#2E9A62) glowing underside to show it is the player's move. Warm, cosy, classic board-game luxury, like a hand-made wooden board game in a library.
```

### 3. Kristal Tuşlar: `store_art_keyboard_crystal.png`

```
[ortak kurallar]
Subject: a short curved row of five keyboard keys made of clear faceted CRYSTAL glass, floating in a gentle arc. The keys are transparent with icy blue and soft lilac light refracting inside them and bright white specular glints on the facets. Each key has a thin silver base. The middle key is slightly pressed down and glows brighter. No letters on the keys. Magical, clean, cool-toned, premium.
```

### 4. Ressam Beresi: `store_art_hat_beret.png`

```
[ortak kurallar]
Subject: a single cute FRENCH PAINTER'S BERET, shown on its own (nobody wearing it). Soft burgundy-red felt (#B23A48) with a small stalk on top, gently slouched to one side, and a tiny paint-brush pin with a dab of gold paint on the brim. Soft felt texture, rounded chunky proportions sized for a small round mascot's head.
```

### 5. Çiçek Tacı: `store_art_hat_flower.png`

```
[ortak kurallar]
Subject: a single FLOWER CROWN, shown on its own (nobody wearing it): a round wreath of small pastel flowers (peach roses, white daisies, lilac blossoms) woven with fresh green leaves and a few tiny golden berries. Seen from a slight top 3/4 angle so the circle of the wreath is visible. Fresh, soft, spring-like, chunky and cute proportions.
```

### 6. Büyücü Şapkası: `store_art_hat_wizard.png`

```
[ortak kurallar]
Subject: a single WIZARD HAT, shown on its own (nobody wearing it): tall pointed hat with a wide brim, deep royal-purple velvet (#4B2A8A) fading to indigo, the tip gently bent over. Covered with small embroidered golden stars and a crescent moon, with a gold band around the base. A few tiny magic sparkles float near the tip. Chunky, cute, premium game-item proportions.
```

### 7. Altın Silindir: `store_art_hat_top.png`

```
[ortak kurallar]
Subject: a single elegant TOP HAT, shown on its own (nobody wearing it): glossy black silk top hat with a thick shiny GOLD satin band and a small gold buckle, polished reflective highlights on the crown. Slightly tilted, classy, rich; chunky proportions sized for a small round mascot's head.
```

### 8. PRO üyelik kartı: `premium_pro.png`

```
[ortak kurallar]
Subject: a royal emblem for a PRO membership: an ornate golden shield with a ruby-red gem in the centre, topped by a small jewelled gold crown with three rubies, with two stylised gold laurel branches curling around the bottom of the shield. Radiant soft golden glow behind the emblem (glow only, background still transparent). Luxurious, majestic, clearly the "top tier" item.
```

---

## Görseller hazır olunca

1. Görseller 1024×1024 ve şeffaf arka planlı olmalı. GPT beyaz arka plan verirse aynı sohbette şunu yaz: "Make the background fully transparent and export as PNG."
2. Dosyaları yukarıdaki adlarla bana gönder. Ben `drawable-nodpi` klasörüne yerleştirip mağaza ve profil önizlemelerine bağlarım.
3. Obi'nin oyunda taktığı şapkalar kodla çizilmeye devam eder. Bu görseller sadece mağaza ve koleksiyon kartlarında kullanılır.
