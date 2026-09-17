package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.getAdminDashboard

/**
 * Keeps the current settings UI intact while restoring the server-authorized admin entry point.
 * Admin visibility is derived only from the protected backend RPC; the client never grants access
 * from an email, local flag, or other user-controlled value.
 */
@Composable
internal fun AdminAwareSettingsScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onSignedOut: () -> Unit,
) {
    var adminChecked by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(backend) {
        isAdmin = runCatching {
            backend.getAdminDashboard()
            true
        }.getOrDefault(false)
        adminChecked = true
    }

    BackHandler(enabled = showAdmin) { showAdmin = false }

    if (showAdmin) {
        AdminConsoleScreen { showAdmin = false }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            MainSettingsScreen(
                backend = backend,
                onBack = onBack,
                onAccount = onAccount,
                onSignedOut = onSignedOut,
            )
        }

        if (adminChecked && isAdmin) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showAdmin = true },
                shape = RoundedCornerShape(18.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Blue.copy(alpha = .32f)),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MainUi.BlueSoft,
                    ) {
                        Icon(
                            Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = MainUi.Blue,
                            modifier = Modifier.padding(9.dp).size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("YÖNETİM PANELİ", "ADMIN PANEL"),
                            color = MainUi.Text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh(
                                "Oyuncular, oyun kontrolleri ve operasyon",
                                "Players, game controls and operations",
                            ),
                            color = MainUi.Muted,
                            fontSize = 9.sp,
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MainUi.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
