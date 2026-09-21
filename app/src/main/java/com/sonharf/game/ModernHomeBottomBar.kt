package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomeNavBg = Color(0xFFFDFEFD)
private val HomeNavBorder = Color(0xFFDCE5E1)
private val HomeNavActive = Color(0xFF176C61)
private val HomeNavActiveSoft = Color(0xFFE1F1ED)
private val HomeNavIdle = Color(0xFF78837E)

@Composable
internal fun ModernHomeBottomNavigation(
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HomeNavBg,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, HomeNavBorder),
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeNavItem(Modifier.weight(1f), Icons.Rounded.Home, sh("Ana Sayfa", "Home"), true, onHome)
            HomeNavItem(Modifier.weight(1f), Icons.Rounded.Groups, sh("Sosyal", "Social"), false, onSocial)
            HomeNavItem(Modifier.weight(1f), Icons.Rounded.Storefront, sh("Mağaza", "Shop"), false, onShop)
            HomeNavItem(Modifier.weight(1f), Icons.Rounded.Person, sh("Profil", "Profile"), false, onProfile)
        }
    }
}

@Composable
private fun HomeNavItem(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) HomeNavActiveSoft else Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Surface(shape = CircleShape, color = HomeNavActive) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(19.dp))
                }
            } else {
                Icon(icon, null, tint = HomeNavIdle, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = if (selected) HomeNavActive else HomeNavIdle,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
