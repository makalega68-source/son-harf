package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenderSymbolPolicyTest {
    @Test fun genderSymbolsAreThePaintedIconsMaleToMaleFemaleToFemale() {
        assertTrue(File("src/main/res/drawable-nodpi/gender_male.png").isFile)
        assertTrue(File("src/main/res/drawable-nodpi/gender_female.png").isFile)
        val runtime = File("src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        assertTrue(runtime.contains("\"kadın\", \"kadin\", \"female\", \"woman\" -> R.drawable.gender_female"))
        assertTrue(runtime.contains("\"erkek\", \"male\", \"man\" -> R.drawable.gender_male"))
        assertFalse(runtime.contains("\"♂\""))
        assertFalse(File("src/main/java/com/sonharf/game/ProfileExperienceV2.kt").readText().contains("\"♀\""))
    }
}
