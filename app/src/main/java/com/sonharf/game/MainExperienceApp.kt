package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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

/** Shared compatibility tokens for feature screens on the new Monster shell. */
internal object MainUi {
    val Background: Color get() = MonsterUi.Background
    val Surface: Color get() = MonsterUi.Surface
    val SurfaceSoft: Color get() = MonsterUi.SurfaceSoft
    val Text: Color get() = MonsterUi.Text
    val Muted: Color get() = MonsterUi.Muted
    val Blue: Color get() = MonsterUi.Accent
    val BlueDeep: Color get() = MonsterUi.Accent
    val BlueSoft: Color get() = MonsterUi.SurfaceRaised
    val Border: Color get() = MonsterUi.Border
    val Green: Color get() = MonsterUi.Green
    val Gold: Color get() = MonsterUi.Gold
    val Red: Color get() = MonsterUi.Coral
    val Purple: Color get() = if (SonHarfCosmetics.sapphireIceTheme) Color(0xFF9B8CFF) else Color(0xFF7659D6)
}

@Composable
fun SonHarfMainApp(onSignedOut: () -> Unit) {
    MonsterExperienceApp(onSignedOut = onSignedOut)
}

@Composable
internal fun MainSectionTitle(title: String) {
    Text(
        text = title,
        color = MainUi.Text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = .35.sp,
    )
}

@Composable
internal fun MainSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = MainUi.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .35.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = action,
            color = MainUi.Blue,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.clickable(onClick = onAction).padding(vertical = 4.dp),
        )
    }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Surface(onClick = onBack, shape = RoundedCornerShape(12.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MainUi.Border)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = MainUi.Text, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = MainUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MainUi.Muted, fontSize = 10.sp)
        }
        if (actionIcon != null && onAction != null) {
            Surface(onClick = onAction, shape = RoundedCornerShape(12.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MainUi.Border)) {
                Icon(actionIcon, contentDescription = actionDescription, tint = MainUi.Text, modifier = Modifier.padding(10.dp).size(20.dp))
            }
        }
    }
}

@Composable
internal fun MainMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(label, color = MainUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}
