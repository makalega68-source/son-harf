package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

/** Main tabs, in product order. OYNA sits in the middle as the main call to action. */
internal enum class GameMainTab { HOME, SOCIAL, PLAY, LEAGUE, SHOP }

@Composable
internal fun GameBottomNavigation(
    selected: GameMainTab?,
    onSelect: (GameMainTab) -> Unit,
) {
    val items = listOf(
        Triple(GameMainTab.HOME, Icons.Rounded.Home, gameText("Ana Sayfa", "Home")),
        Triple(GameMainTab.SOCIAL, Icons.Rounded.Groups, gameText("Sosyal", "Social")),
        Triple(GameMainTab.PLAY, Icons.Rounded.PlayArrow, gameText("Oyna", "Play")),
        Triple(GameMainTab.LEAGUE, Icons.Rounded.EmojiEvents, gameText("Lig", "League")),
        Triple(GameMainTab.SHOP, Icons.Rounded.Storefront, gameText("Market", "Market")),
    )
    Surface(
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (tab, icon, label) ->
                val active = selected == tab
                val play = tab == GameMainTab.PLAY
                Surface(
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    shape = GameShapes.Medium,
                    color = Color.Transparent,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = if (play) 1.dp else 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Game-style badges: the active tab (and Play, always) wears its coloured badge.
                        val accent = when (tab) {
                            GameMainTab.HOME -> GameColors.PrimaryBlue
                            GameMainTab.SOCIAL -> GameColors.Lavender
                            GameMainTab.PLAY -> GameColors.PlayGreen
                            GameMainTab.LEAGUE -> GameColors.PrestigeGold
                            GameMainTab.SHOP -> GameColors.RewardAmber
                        }
                        if (active || play) {
                            GameBadgeIcon(icon, accent, size = if (play) 36.dp else 30.dp, contentDescription = label)
                        } else {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = GameColors.TextSecondary,
                                modifier = Modifier.padding(3.dp).size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            color = if (active) GameColors.TextPrimary else GameColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (active || play) FontWeight.Bold else FontWeight.Medium,
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
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (subtitle == null) 56.dp else 64.dp).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, gameText("Geri", "Back"), tint = GameColors.TextPrimary)
            }
            Spacer(Modifier.width(2.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
        Box(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) GameColors.TextPrimary else GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SegmentedGameTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.size <= 3) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            labels.forEachIndexed { index, label ->
                GameTab(label, index == selectedIndex, { onSelected(index) }, Modifier.weight(1f))
            }
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEachIndexed { index, label ->
                GameTab(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    modifier = Modifier.widthIn(min = 92.dp),
                )
            }
        }
    }
}
