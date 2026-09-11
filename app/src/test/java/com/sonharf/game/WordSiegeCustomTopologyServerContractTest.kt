package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeCustomTopologyServerContractTest {
    @Test
    fun authoritativeNewGameBoardMatchesClientSiegeTopology() {
        val migration = projectFile(
            "supabase/migrations/20260911170000_word_siege_custom_topology_v8.sql",
        ).readText()

        assertTrue(migration.contains("from generate_series(0, 224) i"))
        assertTrue(migration.contains("when i = 112 then '4K'"))
        assertTrue(migration.contains("when i = v_star_index then '3Y'"))
        assertTrue(migration.contains("when i in (22,106,118,202) then '3K'"))
        assertTrue(migration.contains("when i in (34,40,62,72,152,162,184,190) then '3H'"))
        assertTrue(migration.contains("when i in (51,53,93,101,123,131,171,173) then '2K'"))
        assertTrue(migration.contains("when i in (18,26,46,58,80,84,140,144,166,178,198,206) then '2H'"))
        assertFalse(migration.contains("when i in (0,7,14,105,119,210,217,224) then '3K'"))

        val staticBonuses = (0 until WordSiegeBoardSpec.CellCount)
            .filter { WordSiegeBoardSpec.bonusAt(it) != null }
        assertTrue(staticBonuses.size == 33)
        assertTrue(WordSiegeBoardSpec.bonusAt(112) == "4K")
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
