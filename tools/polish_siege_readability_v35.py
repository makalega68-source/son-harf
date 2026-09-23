from pathlib import Path

ROOT = Path('.')


def replace(path: str, old: str, new: str, count: int = -1) -> None:
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Missing expected source in {path}: {old[:120]!r}')
    p.write_text(text.replace(old, new, count), encoding='utf-8')


# Version bump.
replace('app/build.gradle.kts', 'versionCode = 34', 'versionCode = 35')
replace('app/build.gradle.kts', 'versionName = "0.9.18"', 'versionName = "0.9.19"')

# Soften strategic bonus cells so owned letters remain the strongest visual layer.
color_map = {
    '0xFFC8E7F4': '0xFFEAF5F8',
    '0xFF9CCFEA': '0xFFDDEDF4',
    '0xFFDEC8EF': '0xFFF3EEF7',
    '0xFFC5A3E2': '0xFFECE4F2',
    '0xFFF1B95E': '0xFFF6EEDC',
    '0xFFF4C95F': '0xFFF8F0D6',
    '0xFF33445A': '0xFF66717A',
}
for path in [
    'app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt',
    'app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt',
]:
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    for old, new in color_map.items():
        if old in text:
            text = text.replace(old, new)
    p.write_text(text, encoding='utf-8')

# Premium remaining-letter dialog: five roomy columns, centered letter and separate count.
bag_path = 'app/src/main/java/com/sonharf/game/WordSiegeCompactBag.kt'
old_bag = '''                else -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    rows.chunked(6).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            group.forEach { (letter, count) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = WordSiegeGameUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, WordSiegeGameUi.Border),
                                ) {
                                    Text(
                                        "$letter $count",
                                        Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                        color = WordSiegeGameUi.Text,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            repeat(6 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }'''
new_bag = '''                else -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    val total = rows.sumOf { it.second }
                    Text(
                        sh("Torbada $total harf", "$total tiles in bag"),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    rows.chunked(5).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            group.forEach { (letter, count) ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FBFB),
                                    border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .72f)),
                                    shadowElevation = 1.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            letter,
                                            color = WordSiegeGameUi.Navy,
                                            fontSize = 18.sp,
                                            lineHeight = 19.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "×$count",
                                            color = WordSiegeGameUi.Muted,
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            repeat(5 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }'''
replace(bag_path, old_bag, new_bag)

# Practice chat: retain history and make only the dialog history vertically scrollable.
# Do not use LazyColumn here: the gameplay screen has a regression contract forbidding
# lazy scrolling in the main match surface, and a bounded verticalScroll is sufficient.
practice_path = 'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt'
replace(
    practice_path,
    'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.shape.RoundedCornerShape',
    'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.shape.RoundedCornerShape',
)
old_chat = '''                    if (chatMessages.isEmpty()) {
                        Text(
                            sh("Botla kısa mesajlaşabilirsin.", "You can exchange short messages with the bot."),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 12.sp,
                        )
                    } else {
                        chatMessages.takeLast(5).forEach { (mine, message) ->
                            Surface(
                                modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start),
                                shape = RoundedCornerShape(10.dp),
                                color = if (mine) PracticePlayerAccent.copy(alpha = .13f) else PracticeRivalAccent.copy(alpha = .10f),
                            ) {
                                Text(
                                    (if (mine) sh("Sen: ", "You: ") else "${botProfile.name}: ") + message,
                                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    color = WordSiegeGameUi.Text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }'''
new_chat = '''                    val chatScrollState = rememberScrollState()
                    LaunchedEffect(chatMessages.size, chatScrollState.maxValue) {
                        if (chatMessages.isNotEmpty() && chatScrollState.maxValue > 0) {
                            chatScrollState.animateScrollTo(chatScrollState.maxValue)
                        }
                    }
                    if (chatMessages.isEmpty()) {
                        Text(
                            sh("Botla kısa mesajlaşabilirsin.", "You can exchange short messages with the bot."),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 12.sp,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 320.dp)
                                .verticalScroll(chatScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chatMessages.forEach { (mine, message) ->
                                Box(Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier.align(if (mine) Alignment.CenterEnd else Alignment.CenterStart),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (mine) PracticePlayerAccent.copy(alpha = .13f) else PracticeRivalAccent.copy(alpha = .10f),
                                    ) {
                                        Text(
                                            (if (mine) sh("Sen: ", "You: ") else "${botProfile.name}: ") + message,
                                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            color = WordSiegeGameUi.Text,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }'''
replace(practice_path, old_chat, new_chat)
replace(
    practice_path,
    '''                                        chatMessages = (chatMessages + (true to message) +
                                            (false to sh("İyi oyunlar!", "Good game!"))).takeLast(8)''',
    '''                                        chatMessages = chatMessages + (true to message) +
                                            (false to sh("İyi oyunlar!", "Good game!"))''',
)

# Keep source-contract tests aligned with the intentionally quieter bonus palette.
for p in (ROOT / 'app/src/test/java/com/sonharf/game').glob('*.kt'):
    text = p.read_text(encoding='utf-8')
    original = text
    for old, new in color_map.items():
        text = text.replace(old, new)
    if text != original:
        p.write_text(text, encoding='utf-8')

# Dedicated regression contract for this pass.
contract = ROOT / 'app/src/test/java/com/sonharf/game/WordSiegeReadabilityV35ContractTest.kt'
contract.write_text(r'''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeReadabilityV35ContractTest {
    private fun read(path: String) = File(path).readText()

    @Test fun remainingLettersUseRoomyPremiumCells() {
        val bag = read("src/main/java/com/sonharf/game/WordSiegeCompactBag.kt")
        assertTrue(bag.contains("rows.chunked(5)"))
        assertTrue(bag.contains("Modifier.weight(1f).height(58.dp)"))
        assertTrue(bag.contains("fontSize = 18.sp"))
        assertTrue(bag.contains("Torbada $total harf"))
        assertTrue(bag.contains("×$count"))
    }

    @Test fun practiceChatKeepsAndScrollsFullHistory() {
        val screen = read("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt")
        assertTrue(screen.contains("rememberScrollState()"))
        assertTrue(screen.contains(".verticalScroll(chatScrollState)"))
        assertTrue(screen.contains("chatMessages.forEach"))
        assertFalse(screen.contains("chatMessages.takeLast(5)"))
        assertFalse(screen.contains("takeLast(8)"))
        assertFalse(screen.contains("LazyColumn"))
    }

    @Test fun strategicBonusesStayQuieterThanOwnedLetters() {
        val online = read("src/main/java/com/sonharf/game/WordSiegePanMatch.kt")
        val practice = read("src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        listOf("0xFFEAF5F8", "0xFFDDEDF4", "0xFFF3EEF7", "0xFFECE4F2", "0xFFF6EEDC", "0xFFF8F0D6").forEach {
            assertTrue(online.contains(it))
            assertTrue(practice.contains(it))
        }
        assertTrue(online.contains("PanSiegeMine = Color(0xFF8BD8AA)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFEDA09B)"))
    }
}
''', encoding='utf-8')

print('Siege readability v35 polish applied.')
