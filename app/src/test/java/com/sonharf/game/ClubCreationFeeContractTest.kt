package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubCreationFeeContractTest {

    @Test
    fun androidShowsClubFeeAndBalanceFailure() {
        val source = projectFile("app/src/main/java/com/sonharf/game/CompetitionHubScreen.kt").readText()

        assertTrue(source.contains("KULÜP KURMA BEDELİ: 1.000 SON COIN"))
        assertTrue(source.contains("1.000 SC İLE OLUŞTUR"))
        assertTrue(source.contains("insufficient_club_creation_balance"))
    }

    @Test
    fun migrationChargesExactlyOneThousandAtomicallyAndKeepsPublicWrapperInvoker() {
        val sql = projectFile("supabase/migrations/20260829_zzzzzzzz_club_creation_fee_v1.sql").readText()

        assertTrue(sql.contains("v_cost constant integer:=1000"))
        assertTrue(sql.contains("for update"))
        assertTrue(sql.contains("set diamonds=diamonds-v_cost"))
        assertTrue(sql.contains("values(v_uid,-v_cost,'club_creation:'||v_club::text,null)"))
        assertTrue(sql.contains("language sql"))
        assertTrue(sql.contains("security invoker"))
        assertTrue(sql.contains("revoke all on function public.create_club_v1"))
        assertTrue(sql.contains("grant execute on function public.create_club_v1"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
