package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasedFinalSafeAreaSiegeGuidanceContractTest {
    @Test
    fun purchasedBottomNavigationRespectsAndroidNavigationInset() {
        val source = File("src/main/java/com/sonharf/game/PremiumAdultApp.kt").readText()
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains("PurchasedNavItem("))
    }

    @Test
    fun siegePracticeGuidanceUsesPurchasedShellAndCurrentBrand() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeGuidance.kt").readText()
        assertTrue(source.contains("KELİME KUŞATMASI NASIL OYNANIR?"))
        assertTrue(source.contains("HOW TO PLAY WORD SIEGE"))
        assertTrue(source.contains("PurchasedPanel("))
        assertTrue(source.contains("PurchasedButton("))
        assertTrue(source.contains("Dialog(onDismissRequest = onDismiss)"))
        assertFalse(source.contains("KELİME TAHTI NASIL OYNANIR?"))
        assertFalse(source.contains("HOW TO PLAY WORD THRONE"))
        assertFalse(source.contains("AlertDialog("))
    }
}
