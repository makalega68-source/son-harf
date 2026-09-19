package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumCoinPackArtworkContractTest {
    @Test
    fun `every Son Coin package has distinct transparent text-free artwork`() {
        val paths = listOf(
            "app/src/main/res/drawable/premium_coin_500.xml",
            "app/src/main/res/drawable/premium_coin_1500.xml",
            "app/src/main/res/drawable/premium_coin_3500.xml",
            "app/src/main/res/drawable/premium_coin_8000.xml",
        )
        val contents = paths.map { path ->
            val vector = repoFile(path).readText()
            assertTrue("Missing vector root for $path", vector.contains("<vector"))
            assertFalse("Artwork must not bake product copy: $path", vector.contains("<text"))
            assertFalse("Artwork must not bake Son Coin quantities: $path", Regex(">(?:500|1500|3500|8000)<").containsMatchIn(vector))
            vector
        }
        assertEquals("Each Son Coin package must have its own artwork", contents.size, contents.toSet().size)
    }

    @Test
    fun `Google Play rows map exact package to exact artwork`() {
        val play = repoFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()
        mapOf(
            "COINS_500" to "premium_coin_500",
            "COINS_1500" to "premium_coin_1500",
            "COINS_3500" to "premium_coin_3500",
            "COINS_8000" to "premium_coin_8000",
        ).forEach { (product, artwork) ->
            assertTrue("Missing product query for $product", play.contains("products[ProductCatalog.$product]"))
            assertTrue("Missing artwork for $product", play.contains("R.drawable.$artwork"))
        }
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
