package com.sonharf.game

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto
import kotlinx.coroutines.delay

internal const val WORD_SIEGE_MATCH_INTRO_TOTAL_MS = 2_400L
private const val WORD_SIEGE_MATCH_INTRO_VS_MS = 760L
private const val WORD_SIEGE_MATCH_INTRO_MAP_MS = 920L

private val IntroMine = Color(0xFF5F9472)
private val IntroRival = Color(0xFF6E93B3)
private val IntroLavender = Color(0xFFE9E2F1)
private val IntroGold = Color(0xFFE8D7A3)
private val IntroNeutral = Color(0xFFF6F1E6)

@Composable
internal fun WordSiegeMatchIntro(
    gameId: String,
    mine: ProfileDto?,
    opponent: ProfileDto?,
    startsWithMe: Boolean,
    onComplete: () -> Unit,
) {
    var phase by remember(gameId) { mutableIntStateOf(0) }
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220),
        label = "word-siege-intro-alpha",
    )

    LaunchedEffect(gameId) {
        phase = 0
        delay(WORD_SIEGE_MATCH_INTRO_VS_MS)
        phase = 1
        delay(WORD_SIEGE_MATCH_INTRO_MAP_MS)
        phase = 2
        delay(WORD_SIEGE_MATCH_INTRO_TOTAL_MS - WORD_SIEGE_MATCH_INTRO_VS_MS - WORD_SIEGE_MATCH_INTRO_MAP_MS)
        onComplete()
    }

    WordSiegeGameTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WordSiegeGameUi.Background,
        ) {
            Box(Modifier.fillMaxSize()) {
                WordSiegeIntroEdgeSquares()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                        .alpha(contentAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        sh("KELİME KUŞATMASI", "WORD SIEGE"),
                        color = WordSiegeGameUi.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        sh("Kelime kur • Bölgeyi ele geçir • Haritayı yönet", "Build words • Capture territory • Control the map"),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(26.dp))

                    Crossfade(
                        targetState = phase,
                        animationSpec = tween(220),
                        label = "word-siege-intro-phase",
                    ) { current ->
                        when (current) {
                            0 -> WordSiegeIntroVersus(mine, opponent, startsWithMe)
                            1 -> WordSiegeIntroMapPreview()
                            else -> WordSiegeIntroLaunch(startsWithMe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordSiegeIntroVersus(
    mine: ProfileDto?,
    opponent: ProfileDto?,
    startsWithMe: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WordSiegeIntroPlayer(
                profile = mine,
                fallbackName = sh("Sen", "You"),
                accent = IntroMine,
                modifier = Modifier.weight(1f),
            )
            Surface(
                modifier = Modifier.padding(horizontal = 10.dp),
                shape = CircleShape,
                color = WordSiegeGameUi.Text,
            ) {
                Text(
                    "VS",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            WordSiegeIntroPlayer(
                profile = opponent,
                fallbackName = sh("Rakip", "Rival"),
                accent = IntroRival,
                modifier = Modifier.weight(1f),
            )
        }
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = if (startsWithMe) IntroMine.copy(alpha = .13f) else IntroRival.copy(alpha = .13f),
            border = BorderStroke(1.dp, if (startsWithMe) IntroMine.copy(alpha = .38f) else IntroRival.copy(alpha = .38f)),
        ) {
            Text(
                if (startsWithMe) sh("İLK HAMLE SENDE", "YOU MOVE FIRST") else sh("İLK HAMLE RAKİPTE", "RIVAL MOVES FIRST"),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                color = if (startsWithMe) IntroMine else IntroRival,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun WordSiegeIntroPlayer(
    profile: ProfileDto?,
    fallbackName: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = accent.copy(alpha = .10f),
            border = BorderStroke(2.dp, accent.copy(alpha = .55f)),
        ) {
            Box(Modifier.padding(5.dp)) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: fallbackName,
                    size = 72.dp,
                    accent = accent,
                    visible = profile?.avatarVisibility != "hidden",
                )
            }
        }
        Text(
            profile?.displayName ?: fallbackName,
            color = WordSiegeGameUi.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            sh("Lig ${profile?.rating ?: 1000}", "Rating ${profile?.rating ?: 1000}"),
            color = WordSiegeGameUi.Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WordSiegeIntroMapPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(
            sh("HARİTAYI OKU", "READ THE MAP"),
            color = WordSiegeGameUi.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WordSiegeGameUi.Surface,
            border = BorderStroke(1.dp, WordSiegeGameUi.Border),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { previewRow ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(7) { previewColumn ->
                            val boardRow = WordSiegeBoardSpec.Size / 2 - 3 + previewRow
                            val boardColumn = WordSiegeBoardSpec.Size / 2 - 3 + previewColumn
                            val boardIndex = WordSiegeBoardSpec.index(boardRow, boardColumn)
                            val bonus = WordSiegeBoardSpec.bonusAt(boardIndex)
                            val surface = when (bonus) {
                                WordSiegeBoardSpec.CenterBonus -> IntroLavender
                                WordSiegeBoardSpec.StarBonus -> IntroGold
                                "2H", "3H" -> IntroRival.copy(alpha = .22f)
                                "2K", "3K" -> IntroMine.copy(alpha = .22f)
                                else -> IntroNeutral
                            }
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = surface,
                                border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .75f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (bonus != null) {
                                        Text(
                                            WordSiegeBoardSpec.displayBonusLabel(bonus, !SonHarfUiState.isEnglish)
                                                .replace("\n", " ")
                                                .take(3),
                                            color = WordSiegeGameUi.Muted,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            sh("Kritik bölgeleri kapat; kelime puanı kalır, alan hakimiyeti maçı çevirir.", "Secure critical zones; word points stay, territory control can turn the match."),
            modifier = Modifier.fillMaxWidth(),
            color = WordSiegeGameUi.Muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordSiegeIntroLaunch(startsWithMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Surface(
            modifier = Modifier.size(86.dp),
            shape = RoundedCornerShape(26.dp),
            color = IntroMine.copy(alpha = .13f),
            border = BorderStroke(1.dp, IntroMine.copy(alpha = .34f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⚔", fontSize = 38.sp)
            }
        }
        Text(
            sh("KUŞATMA BAŞLIYOR", "THE SIEGE BEGINS"),
            color = WordSiegeGameUi.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            if (startsWithMe) sh("İlk rotayı sen çiz.", "Set the first route.") else sh("Rakibin ilk hamlesini oku.", "Read your rival's opening move."),
            color = WordSiegeGameUi.Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WordSiegeIntroEdgeSquares() {
    Box(Modifier.fillMaxSize()) {
        val dots = listOf(
            Triple(18.dp, 34.dp, IntroMine),
            Triple(50.dp, 70.dp, IntroRival),
            Triple(84.dp, 24.dp, IntroGold),
            Triple(22.dp, 104.dp, IntroRival.copy(alpha = .7f)),
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dots.forEachIndexed { index, (_, _, color) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat((4 - index).coerceAtLeast(1)) {
                        Box(
                            Modifier
                                .size((8 - index).dp)
                                .background(color.copy(alpha = .22f - index * .035f), RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dots.forEachIndexed { index, (_, _, color) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat((4 - index).coerceAtLeast(1)) {
                        Box(
                            Modifier
                                .size((8 - index).dp)
                                .background(color.copy(alpha = .20f - index * .03f), RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
    }
}
