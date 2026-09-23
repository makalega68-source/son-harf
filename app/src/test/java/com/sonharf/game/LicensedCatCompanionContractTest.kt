package com.sonharf.game

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensedCatCompanionContractTest {
    @Test
    fun `companion runtime is presentation only`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/companion/LicensedCatCompanion.kt").readText()
        assertTrue(source.contains("ValueAnimator.areAnimatorsEnabled()"))
        assertTrue(source.contains("accessibilityLabel"))
        assertTrue(source.contains("CompanionMoment.BIG_SIEGE"))
        assertTrue(source.contains("CompanionMoment.LEAGUE_UP"))
        assertFalse(source.contains("OnlineGameBackend"))
        assertFalse(source.contains("SupabaseProvider"))
        assertFalse(source.contains("rpc("))
        assertFalse(source.contains("while (true)"))
        assertFalse(source.contains("delay("))
    }

    @Test
    fun `licensed atlas is exact audited asset`() {
        val atlas = repoFile("app/src/main/res/drawable-nodpi/licensed_cat_companion_atlas.png")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(atlas.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertTrue(digest == "0af65dd6a1bd537be61a617516487b275fc2b3442a18c86ece998ff086b7363a")

        val proof = repoFile("app/src/main/java/com/sonharf/game/companion/LicensedCatAssetProof.kt").readText()
        assertTrue(proof.contains("magecat.unitypackage"))
        assertTrue(proof.contains("mage_cat.fbx"))
        assertTrue(proof.contains("mage_cat_textures.zip"))
        assertTrue(proof.contains(digest))
    }

    @Test
    fun `existing provenance and gameplay gates are not weakened`() {
        val workflow = repoFile(".github/workflows/frame-provenance-gate.yml").readText()
        assertTrue(workflow.contains("test ! -d app/src/main/java/com/sonharf/game/mascot"))
        assertTrue(workflow.contains("test ! -f app/src/main/res/drawable-nodpi/mage_cat_runtime.webp"))
        assertTrue(workflow.contains("! grep -q 'MageCat' app/src/main/java/com/sonharf/game/UnifiedProApp.kt"))

        val changedRuntime = repoFile("app/src/main/java/com/sonharf/game/companion/LicensedCatCompanion.kt").readText()
        listOf("PREMIER_TURN_SECONDS", "matchmaking", "submit_word", "purchase_shop_item", "diamonds")
            .forEach { forbidden -> assertFalse(changedRuntime.contains(forbidden)) }
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
