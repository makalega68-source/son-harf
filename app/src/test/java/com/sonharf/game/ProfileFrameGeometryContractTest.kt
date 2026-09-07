package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFrameGeometryContractTest {
    @Test
    fun framedProfilesUseRectangularRendererAndPreserveVisibility() {
        val framed = projectFile("app/src/main/java/com/sonharf/game/FramedProfileAvatar.kt").readText()
        val runtime = projectFile("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt").readText()

        assertTrue(framed.contains("ProfilePhotoAvatarRectWithGender("))
        assertFalse(framed.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(framed.contains("visible = visible"))
        assertTrue(runtime.contains("internal fun ProfilePhotoAvatarRectWithGender("))
        assertTrue(runtime.contains("visible: Boolean = true"))
        assertTrue(runtime.contains("LaunchedEffect(avatarPath, visible)"))
        assertTrue(runtime.contains("if (visible && !avatarPath.isNullOrBlank())"))
        assertTrue(runtime.contains("RoundedCornerShape(14.dp)"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
