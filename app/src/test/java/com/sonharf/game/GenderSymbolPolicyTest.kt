package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenderSymbolPolicyTest {
    @Test fun suppliedPortraitsReplaceEmptyPhotosAndGenderSymbolsAreGone() {
        assertTrue(File("src/main/res/drawable-nodpi/default_profile_male.webp").isFile)
        assertTrue(File("src/main/res/drawable-nodpi/default_profile_female.webp").isFile)
        val runtime = File("src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()
        assertTrue(runtime.contains("\"kadın\", \"kadin\", \"female\", \"woman\" -> R.drawable.default_profile_female"))
        assertTrue(runtime.contains("else -> R.drawable.default_profile_male"))
        assertFalse(runtime.contains("FramelessGenderSymbol"))
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { source ->
            assertFalse(source.path, source.readText().contains("R.drawable.gender_"))
        }
        assertFalse(runtime.contains("\"♂\""))
        assertFalse(File("src/main/java/com/sonharf/game/ProfileExperienceV2.kt").readText().contains("\"♀\""))
    }
}
