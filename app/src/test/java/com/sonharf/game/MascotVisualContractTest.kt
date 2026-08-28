package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotVisualContractTest {
    @Test
    fun freeLyraUsesChibiAssetAndNotLegacyFourLeggedAsset() {
        val catalog = sourceFile(
            "src/main/java/com/sonharf/game/MascotCatalog.kt",
            "app/src/main/java/com/sonharf/game/MascotCatalog.kt",
        ).readText()

        assertTrue(catalog.contains("DEFAULT_ID,\n        CHIBI_WIZARD_ID -> runCatching { ChibiEmbeddedModel.ensureFile(context).isFile }"))
        assertTrue(catalog.contains("WHITE_ASSET = \"embedded:chibi-wizard-v1\""))
        assertFalse(catalog.contains("son_harf_white_pet_rigged.glb"))
    }

    @Test
    fun homeAndShopRequestLargeMascotPresentation() {
        val home = sourceFile(
            "src/main/java/com/sonharf/game/MascotHomeCompanion.kt",
            "app/src/main/java/com/sonharf/game/MascotHomeCompanion.kt",
        ).readText()
        val shop = sourceFile(
            "src/main/java/com/sonharf/game/EconomyShopScreen.kt",
            "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt",
        ).readText()

        assertTrue(home.contains("displayScale = 1.75f"))
        assertTrue(shop.contains("displayScale = 1.82f"))
        assertTrue(shop.contains("val previewHeight = if (item.kind == \"mascot\") 176.dp else 108.dp"))
        assertTrue(shop.contains("DİĞER MÜHÜRLER"))
        assertTrue(shop.contains("appearanceTint = seal.color"))
        assertTrue(shop.contains("displayScale = 1.72f"))
    }

    @Test
    fun lyraGetsWhiteRuntimeTint() {
        val stage = sourceFile(
            "src/main/java/com/sonharf/game/MascotLive3DStage.kt",
            "app/src/main/java/com/sonharf/game/MascotLive3DStage.kt",
        ).readText()

        assertTrue(stage.contains("Color(0xFFF8F8F6)"))
        assertTrue(stage.contains("\"baseColorFactor\""))
        assertTrue(stage.contains("MASCOT_TINT_APPLIED"))
    }

    private fun sourceFile(vararg candidates: String): File =
        candidates.asSequence().map(::File).firstOrNull(File::isFile)
            ?: error("Required mascot source file is missing: ${candidates.joinToString()}")
}
