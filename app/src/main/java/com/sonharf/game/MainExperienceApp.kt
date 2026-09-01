package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared design tokens for screens that still depend on the historical MainUi
 * API. The old light dashboard layout is intentionally retired; these values
 * now mirror the purchased Monster Livescore visual language so feature screens
 * remain visually coherent while their business logic stays untouched.
 */
internal object MainUi {
    val Background = MonsterUi.Background
    val Surface = MonsterUi.Surface
    val SurfaceSoft = MonsterUi.SurfaceSoft
    val Text = MonsterUi.Text
    val Muted = MonsterUi.Muted
    val Blue = MonsterUi.Accent
    val BlueDeep = Color(0xFFCFE900)
    val BlueSoft = MonsterUi.Accent.copy(alpha = .12f)
    val Border = MonsterUi.Border
    val Green = MonsterUi.Green
    val Gold = MonsterUi.Gold
    val Red = MonsterUi.Coral
    val Purple = Color(0xFF9A86FF)
}

/** Legacy entry point kept only for source compatibility. */
@Composable
fun SonHarfMainApp(onSignedOut: () -> Unit) {
    MonsterExperienceApp(onSignedOut = onSignedOut)
}

@Composable
internal fun MainScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String = "",
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                color = MonsterUi.SurfaceRaised,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = sh("Geri", "Back"),
                    tint = MainUi.Text,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 10.sp)
        }
        if (actionIcon != null && onAction != null) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                color = MonsterUi.SurfaceRaised,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Icon(
                    actionIcon,
                    contentDescription = actionDescription,
                    tint = MainUi.Text,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun MainMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MainUi.Muted, fontSize = 8.5.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun MainSectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MainUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                Text(action, color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
