package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotVisualContractTest {
    @Test
    fun freeLyraUsesDedicatedWhiteChibiAssetAndNotLegacyPet() {
        val catalog = sourceFile(
            "src/main/java/com/sonharf/game/MascotCatalog.kt",
            "app/src/main/java/com/sonharf/game/MascotCatalog.kt",
        ).readText()

        assertTrue(catalog.contains("WHITE_ASSET = \"models/lyra_white_chibi.glb\""))
        assertTrue(catalog.contains("DEFAULT_ID -> runCatching { context.assets.open(WHITE_ASSET).use { } }.isSuccess"))
        assertTrue(catalog.contains("CHIBI_WIZARD_ID -> runCatching { ChibiEmbeddedModel.ensureFile(context).isFile }"))
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
    }

    @Test
    fun lyraDoesNotDependOnGlobalRuntimeTint() {
        val stage = sourceFile(
            "src/main/java/com/sonharf/game/MascotLive3DStage.kt",
            "app/src/main/java/com/sonharf/game/MascotLive3DStage.kt",
        ).readText()

        assertTrue(stage.contains("val effectiveTint = appearanceTint"))
        assertFalse(stage.contains("Color(0xFFF8F8F6)"))
        assertFalse(stage.contains("appearanceTint ?: if (resolvedId == MascotCatalog.DEFAULT_ID)"))
    }

    private fun sourceFile(vararg candidates: String): File =
        candidates.asSequence().map(::File).firstOrNull(File::isFile)
            ?: error("Required mascot source file is missing: ${candidates.joinToString()}")
}
