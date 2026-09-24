package com.sonharf.game

import com.sonharf.game.data.ShopItemDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreMarketContractTest {
    private val theme = ShopItemDto(id = "keyboard_crystal", kind = "keyboard_theme", nameTr = "Kristal", nameEn = "Crystal", diamondPrice = 210)
    private val proOnly = theme.copy(id = "emoji_vip", kind = "emoji_pack", vipOnly = true)

    @Test
    fun `detail sheet action follows ownership, equip state and PRO`() {
        assertEquals(StoreProductAction.BUY, storeProductAction(theme, owned = false, equipped = false, proActive = false))
        assertEquals(StoreProductAction.EQUIP, storeProductAction(theme, owned = true, equipped = false, proActive = false))
        assertEquals(StoreProductAction.EQUIPPED, storeProductAction(theme, owned = true, equipped = true, proActive = false))
        assertEquals(StoreProductAction.NEEDS_PRO, storeProductAction(proOnly, owned = false, equipped = false, proActive = false))
        assertEquals(StoreProductAction.BUY, storeProductAction(proOnly, owned = false, equipped = false, proActive = true))
    }

    @Test
    fun `market has featured, cosmetics, collections and PRO with server-side buy and equip`() {
        val shop = source("EconomyShopScreen.kt")
        val sheet = source("StoreProductDetailSheet.kt")
        assertTrue(shop.contains("gameText(\"Kozmetik\", \"Cosmetics\")"))
        assertTrue(shop.contains("gameText(\"Maskotlar\", \"Mascots\")"))
        assertTrue(shop.contains("b.purchaseShopItem(product.id)"))
        assertTrue(shop.contains("b.equipShopItem(product.id)"))
        assertTrue(shop.contains("StoreProductDetailSheet("))
        // Collections are grouped from catalog bundle sections, not hard-coded screens.
        assertTrue(shop.contains("val groups = bundles.groupBy { it.section }"))
        // The inventory is separate from the catalog.
        assertTrue(shop.contains("gameText(\"Envanterim\", \"My inventory\")"))
        // Every product states that it gives no gameplay advantage.
        assertTrue(sheet.contains("gameText(\"Oyun avantajı\", \"Gameplay advantage\"), gameText(\"Yok\", \"None\")"))
        assertTrue(sheet.contains("ModalBottomSheet("))
        assertFalse(sheet.contains("AlertDialog("))
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
