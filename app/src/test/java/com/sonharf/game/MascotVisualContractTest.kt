package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotVisualContractTest {
    @Test
    fun catalogContainsOnlyTheDarkChibiMascot() {
        val catalog = sourceFile(
            "src/main/java/com/sonharf/game/MascotCatalog.kt",
            "app/src/main/java/com/sonharf/game/MascotCatalog.kt",
        ).readText()

        assertTrue(catalog.contains("CHIBI_WIZARD_ID"))
        assertTrue(catalog.contains("listOf("))
        assertFalse(catalog.contains("WHITE_ASSET"))
        assertFalse(catalog.contains("mascot_white"))
        assertFalse(catalog.contains("lyra_white_chibi.glb"))
    }

    @Test
    fun nerisFurIsLightenedWithoutChangingOtherMaterials() {
        assertTrue(NerisAppearancePolicy.FUR_COLOR_LIFT > NerisAppearancePolicy.GENERAL_BRIGHTNESS_MAX)
        assertTrue(NerisAppearancePolicy.FUR_COLOR_LIFT <= 1.60f)
        assertTrue(
            NerisAppearancePolicy.brightnessFor(
                NerisAppearancePolicy.FUR_MATERIAL_NAME,
                1.16f,
            ) == NerisAppearancePolicy.FUR_COLOR_LIFT,
        )
        assertTrue(
            NerisAppearancePolicy.brightnessFor("Mage_Cat_Clothes", 1.16f) == 1.16f,
        )
    }

    @Test
    fun homeAndArenaRequestLargeMascotPresentation() {
        val home = sourceFile(
            "src/main/java/com/sonharf/game/LightWordThemeApp.kt",
            "app/src/main/java/com/sonharf/game/LightWordThemeApp.kt",
        ).readText()
        val arena = sourceFile(
            "src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
            "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
        ).readText()

        assertTrue(home.contains("height(280.dp)"))
        assertTrue(home.contains("displayScale = 1.72f"))
        assertTrue(arena.contains("displayScale = 1.58f"))
        assertTrue(arena.contains("Seni özledim!"))
        assertTrue(arena.contains("ChibiVictoryFlight"))
        assertTrue(arena.contains("MascotMotion.VICTORY"))
        assertTrue(home.contains("brightnessBoost = 1.16f"))
    }

    @Test
    fun arenaUsesAndroidImeFocusRequester() {
        val arena = sourceFile(
            "src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
            "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
        ).readText()

        assertTrue(arena.contains("FocusRequester()"))
        assertTrue(arena.contains("keyboard?.show()"))
        assertTrue(arena.contains("focusRequester(wordFocusRequester)"))
        assertTrue(arena.contains("imeAction = ImeAction.Send"))
        assertTrue(arena.contains("verticalScroll(rememberScrollState())"))
        assertTrue(arena.contains("imeVisible"))
    }

    private fun sourceFile(vararg candidates: String): File =
        candidates.asSequence().map(::File).firstOrNull(File::isFile)
            ?: error("Required mascot source file is missing: ${candidates.joinToString()}")
}
