package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBoardSkinContractTest {
    private fun read(path: String) = listOf(File(path), File("../$path"), File("app/$path")).first(File::exists).readText()

    @Test fun classicBoardIsTheDefaultAndSkinsAreProducts() {
        val skin = read("src/main/java/com/sonharf/game/WordSiegeBoardSkin.kt")
        assertFalse(skin.contains("val DEFAULT = STONE_KEEP"))
        assertTrue(skin.contains("setOf(RIVER_VALLEY.id, FROST_CITADEL.id, OBSIDIAN.id)"))
        assertTrue(skin.contains("get() = WordSiegeBoardSkin.fromId(selectedId)\n"))
        assertTrue(File("src/main/res/drawable-nodpi/store_art_board_classic.jpg").exists() || File("app/src/main/res/drawable-nodpi/store_art_board_classic.jpg").exists())
        listOf("stone_keep", "river_valley", "frost_citadel", "obsidian").forEach { name ->
            listOf("board_plate_$name", "store_art_board_$name").forEach { res ->
                assertTrue("$res missing", File("src/main/res/drawable-nodpi/$res.jpg").exists() || File("app/src/main/res/drawable-nodpi/$res.jpg").exists())
            }
        }
    }

    @Test fun bothBoardsDrawTheSkinWithoutChangingTheGrid() {
        listOf("WordSiegePanMatch.kt", "WordSiegePracticeBoard.kt").forEach { file ->
            val board = read("src/main/java/com/sonharf/game/$file")
            assertTrue(board.contains("WordSiegeSkinnedBoard(boardSkin, Modifier.fillMaxSize())"))
            assertTrue(board.contains("val skinPlate = LocalWordSiegePlate.current"))
            assertTrue(board.contains("wordSiegePlateTexture(skinPlate)"))
            // Grid geometry stays: same cell gap literal used by the tap maths.
            assertTrue(board.contains("val regionGap = 1.25.dp"))
        }
    }

    @Test fun serverSellsOnlyTheThreeSkinsAndTheyAreSelectableInTheCollection() {
        val sql = read("../supabase/migrations/20260929160000_board_skins_v1.sql")
        assertTrue(sql.contains("when 'board_skin' then p_item_id in ('board_river_valley','board_frost_citadel','board_obsidian')"))
        assertFalse(sql.contains("'board_stone_keep', 'board_skin'"))
        val collection = read("src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt")
        assertTrue(collection.contains("WordSiegeBoardSkins.select(context, itemId)"))
        assertTrue(collection.contains("WordSiegeBoardSkins.select(context, null)"))
        val tabs = read("src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt")
        assertTrue(tabs.contains("\"board_skin\" to sh(\"Tahta\", \"Board\")"))
    }

    @Test fun tutorialExplainsEveryBonusWithItsPicture() {
        val guidance = read("src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt")
        assertTrue(guidance.contains("if (step >= 4) WordSiegeBonusLegend("))
        val mark = read("src/main/java/com/sonharf/game/WordSiegeBonusMark.kt")
        listOf("2H", "3H", "2K", "3K").forEach { assertTrue(mark.contains("\"$it\" -> if (turkish)")) }
        assertTrue(mark.contains("WordSiegeBoardSpec.CenterBonus -> if (turkish) \"İlk kelime buradan geçer; puanı 4 kat.\""))
    }
}
