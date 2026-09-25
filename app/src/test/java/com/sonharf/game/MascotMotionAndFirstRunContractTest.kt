package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotMotionAndFirstRunContractTest {
    @Test
    fun newMascotMovesExistAndAreUsed() {
        val view = source("WordSiegeMascotView.kt")
        val companion = source("WordSiegeMascotCompanion.kt")
        listOf("DANCE", "WIGGLE", "SPIN_HOP", "BOUNCE", "WAVE", "INTRO").forEach { move ->
            assertTrue("rig move $move", view.contains("WordSiegeMascotAction.$move ->"))
        }
        listOf("DANCE", "WIGGLE", "BOUNCE", "SPIN_HOP", "WAVE").forEach { idle ->
            assertTrue("idle $idle", companion.contains("WordSiegeMascotIdle.$idle -> "))
        }
        assertTrue(companion.contains("grandEntrance: Boolean = false"))
        assertTrue(companion.contains("perform(WordSiegeMascotAction.INTRO)"))
        assertTrue(view.contains("val waving = "))
    }

    @Test
    fun firstRunLanguageScreenIsAnimatedAndUsesTheGrandEntrance() {
        val startup = source("StableV1App.kt")
        assertTrue(startup.contains("grandEntrance = true"))
        assertTrue(startup.contains("private fun FirstRunContinueButton("))
        assertTrue(startup.contains("rememberInfiniteTransition"))
        assertTrue(startup.contains("KELİME KUŞATMASI / WORD SIEGE"))
    }

    @Test
    fun leavingSonHarfReturnsToWhereItWasOpened() {
        val app = source("ProfessionalUnifiedApp.kt")
        assertTrue(app.contains("destination = if (destination == ProfessionalDestination.LAST_LETTER) gameReturn else ProfessionalDestination.HOME"))
    }

    private fun source(name: String): String =
        listOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .first(File::exists)
            .readText()
}
