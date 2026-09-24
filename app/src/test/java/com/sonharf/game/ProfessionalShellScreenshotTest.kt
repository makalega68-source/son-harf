package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the professional shell at the three reference phone sizes. The PNGs are written to
 * app/build/outputs/roborazzi and uploaded by CI for visual review; the test fails if a
 * screen cannot be composed or drawn at a size.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProfessionalShellScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun shell_360x800() = captureShell("shell_360x800")

    @Test
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    fun shell_390x844() = captureShell("shell_390x844")

    @Test
    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    fun shell_412x915() = captureShell("shell_412x915")

    private fun captureShell(name: String) {
        compose.setContent { ShellFrame { GameEmptyState(icon = Icons.Rounded.Home, title = "Kelime Kuşatması", body = "Ekran görüntüsü testi") } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun ShellFrame(content: @Composable () -> Unit) {
        GameTheme {
            Scaffold(
                containerColor = GameColors.AppBackground,
                bottomBar = {
                    GameBottomNavigation(selectedIndex = 0, onHome = {}, onSocial = {}, onShop = {}, onProfile = {})
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { content() }
            }
        }
    }
}
