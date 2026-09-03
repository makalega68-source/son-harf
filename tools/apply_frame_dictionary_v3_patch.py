from pathlib import Path

root = Path(__file__).resolve().parents[1]

# Shared dictionary uses one game contract on both client and server: 2..12 letters.
p = root / 'app/src/main/java/com/sonharf/game/data/SharedDictionaryService.kt'
s = p.read_text()
s = s.replace('private const val MAX_CANONICAL_LENGTH = 32', 'private const val MAX_CANONICAL_LENGTH = 12')
p.write_text(s)

# Integrate all purchased LAYERLAB variants while keeping backend shop_items authoritative.
p = root / 'app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt'
s = p.read_text()
s = s.replace('    const val GOLD_CROWN = "frame_asset_gold_crown"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN)', '    const val GOLD_CROWN = "frame_asset_gold_crown"\n    const val CHRISTMAS = "frame_asset_christmas"\n    const val HALLOWEEN = "frame_asset_halloween"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN, CHRISTMAS, HALLOWEEN)')
s = s.replace('        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n        else -> null', '        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n        CHRISTMAS -> R.drawable.style_frame_christmas\n        HALLOWEEN -> R.drawable.style_frame_halloween\n        else -> null')
s = s.replace('        GOLD_CROWN -> Color(0xFFE0A51C)\n        "frame_neon"', '        GOLD_CROWN -> Color(0xFFE0A51C)\n        CHRISTMAS -> Color(0xFF2FAE68)\n        HALLOWEEN -> Color(0xFFE07A1C)\n        "frame_neon"')
s = s.replace('    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),\n)', '    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),\n    PurchasedFrameSpec(PurchasedFrameCatalog.CHRISTMAS, "Yılbaşı", "Christmas", "Sezonluk yılbaşı etkinlik çerçevesi", "Seasonal Christmas event frame", R.drawable.style_frame_christmas, Color(0xFF2FAE68), "ETKİNLİK", "EVENT", R.drawable.style_icon_trophy),\n    PurchasedFrameSpec(PurchasedFrameCatalog.HALLOWEEN, "Halloween", "Halloween", "Sezonluk Halloween etkinlik çerçevesi", "Seasonal Halloween event frame", R.drawable.style_frame_halloween, Color(0xFFE07A1C), "ETKİNLİK", "EVENT", R.drawable.style_icon_trophy),\n)')
s = s.replace('            contentScale = ContentScale.FillBounds,', '            contentScale = ContentScale.Fit,')
s = s.replace('                                        contentScale = ContentScale.FillBounds,', '                                        contentScale = ContentScale.Fit,')
old = '''        val legacy = shopItems.values
            .filter { it.id !in PurchasedFrameCatalog.ids }
            .sortedBy { it.sortOrder }
            .map(::legacyFrameSpec)
        // Staged frame_asset_* entries are recovery renderers only. The backend active catalog
        // is authoritative for discovery/sales; preserve staged items only when already owned/equipped.
        val staged = purchasedFrameSpecs.filter { spec ->
            spec.id in inventory || equippedId == spec.id
        }
        (legacy + staged).distinctBy { it.id }'''
new = '''        val purchasedById = purchasedFrameSpecs.associateBy { it.id }
        // Active backend rows are authoritative for discovery and sale. For purchased LAYERLAB IDs,
        // backend metadata (price/availability) is combined with the verified local artwork spec.
        val activeCatalog = shopItems.values
            .sortedBy { it.sortOrder }
            .map { item -> purchasedById[item.id] ?: legacyFrameSpec(item) }
        // Inactive reward/event items are never exposed for purchase. If a user already owns or has
        // one equipped, keep a local recovery renderer so existing ownership cannot disappear.
        val ownedRecovery = purchasedFrameSpecs.filter { spec ->
            spec.id !in shopItems && (spec.id in inventory || equippedId == spec.id)
        }
        (activeCatalog + ownedRecovery).distinctBy { it.id }'''
if old not in s:
    raise SystemExit('PurchasedStyleUi displaySpecs anchor not found')
s = s.replace(old, new)
p.write_text(s)
