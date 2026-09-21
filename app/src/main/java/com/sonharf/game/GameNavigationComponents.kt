package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GameBottomNavigation(
    selectedIndex: Int,
    onHome: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
    onProfile: () -> Unit,
) {
    val items = listOf(
        Triple(Icons.Rounded.Home, gameText("Ana Sayfa", "Home"), onHome),
        Triple(Icons.Rounded.Groups, gameText("Sosyal", "Social"), onSocial),
        Triple(Icons.Rounded.Storefront, gameText("Mağaza", "Shop"), onShop),
        Triple(Icons.Rounded.Person, gameText("Profil", "Profile"), onProfile),
    )
    Surface(
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(62.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = selectedIndex == index
                Surface(
                    onClick = item.third,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    shape = GameShapes.Medium,
                    color = if (active) GameColors.PrimaryBlue.copy(alpha = .15f) else Color.Transparent,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            item.first,
                            contentDescription = item.second,
                            tint = if (active) GameColors.PrimaryBlue else GameColors.TextSecondary,
                            modifier = Modifier.size(23.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.second,
                            color = if (active) GameColors.TextPrimary else GameColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun GameTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, gameText("Geri", "Back"), tint = GameColors.TextPrimary)
            }
            Spacer(Modifier.width(2.dp))
        }
        Text(
            title,
            Modifier.weight(1f),
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke(this)
    }
}

@Composable
internal fun GameTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = GameShapes.Pill,
        color = if (selected) GameColors.PrimaryBlue.copy(alpha = .18f) else GameColors.SecondarySurface,
        border = BorderStroke(1.dp, if (selected) GameColors.PrimaryBlue else GameColors.Border),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) GameColors.TextPrimary else GameColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SegmentedGameTabs(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            GameTab(label, index == selectedIndex, { onSelected(index) }, Modifier.weight(1f))
        }
    }
}
