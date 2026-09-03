from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
profile_runtime = ROOT / "app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt"
profile_screen = ROOT / "app/src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt"

src = profile_runtime.read_text(encoding="utf-8")
if "import androidx.compose.ui.semantics.contentDescription" not in src:
    src = src.replace(
        "import androidx.compose.ui.layout.ContentScale\n",
        "import androidx.compose.ui.layout.ContentScale\nimport androidx.compose.ui.semantics.contentDescription\nimport androidx.compose.ui.semantics.semantics\n",
    )

marker = "internal object AvatarVisualValidationRuntime"
if marker not in src:
    insert_at = src.index("private data class GenderVisual")
    src = src[:insert_at] + '''internal object AvatarVisualValidationRuntime {\n    var avatarBytes by mutableStateOf<ByteArray?>(null)\n    var forcedFrameId by mutableStateOf<String?>(null)\n}\n\n''' + src[insert_at:]

replacement = r'''@Composable
private fun CanonicalProfileAvatar(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color,
    visible: Boolean,
) {
    var bytes by remember(avatarPath) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(avatarPath, visible, AvatarVisualValidationRuntime.avatarBytes) {
        bytes = AvatarVisualValidationRuntime.avatarBytes
            ?: if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null
    }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
    val frameId = (AvatarVisualValidationRuntime.forcedFrameId ?: SonHarfCosmetics.profileFrameId)
        ?.takeIf { it in PurchasedFrameCatalog.ids }
    val frameSize = (size.value * 126f / 106f).dp
    val outerSize = if (frameId != null) frameSize else size + 5.dp

    Box(
        Modifier
            .size(outerSize)
            .semantics { contentDescription = "SON_HARF_AVATAR" },
        contentAlignment = Alignment.Center,
    ) {
        if (frameId != null) {
            Box(
                Modifier.size(size).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap.asImageBitmap(),
                        null,
                        Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    SyntheticProfilePortrait(name, gender, Modifier.fillMaxSize().clip(CircleShape), accent)
                }
            }
            PurchasedProfileFrameOverlay(frameId = frameId, modifier = Modifier.size(frameSize))
        } else {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color.White, accent, Color(0xFF57C7F3), Color.White)))
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap.asImageBitmap(),
                        null,
                        Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    SyntheticProfilePortrait(name, gender, Modifier.fillMaxSize().clip(CircleShape), accent)
                }
            }
            Box(Modifier.align(Alignment.BottomEnd)) {
                FramelessGenderSymbol(gender, size)
            }
        }
    }
}

@Composable
internal fun ProfilePhotoAvatar(
    avatarPath: String?,
    name: String,
    size: Dp,
    visible: Boolean = true,
    accent: Color = SonHarfCyan,
) {
    var gender by remember(avatarPath) { mutableStateOf<String?>(null) }
    LaunchedEffect(avatarPath) { gender = ProfilePhotoRuntime.genderForAvatar(avatarPath) }
    CanonicalProfileAvatar(avatarPath, gender, name, size, accent, visible)
}

@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
) {
    CanonicalProfileAvatar(avatarPath, gender, name, size, accent, visible)
}

'''
pattern = re.compile(
    r'@Composable\ninternal fun ProfilePhotoAvatar\(.*?(?=@Composable\ninternal fun ProfilePhotoAvatarRectWithGender\()',
    re.S,
)
new_src, count = pattern.subn(replacement, src, count=1)
if count != 1:
    raise SystemExit("Could not replace circular avatar renderers")
profile_runtime.write_text(new_src, encoding="utf-8")

ps = profile_screen.read_text(encoding="utf-8")
ps2, n = re.subn(
    r'\n\s*PurchasedProfileFrameOverlay\(\n\s*frameId = SonHarfCosmetics\.profileFrameId,\n\s*modifier = Modifier\.size\(126\.dp\),\n\s*\)',
    '',
    ps,
    count=1,
)
if n != 1 and "PurchasedProfileFrameOverlay" in ps:
    raise SystemExit("Could not remove profile external purchased-frame overlay")
ps = ps2
old = "modifier = Modifier.size(37.dp).clickable(onClick = onEdit),"
new = "modifier = Modifier.size(32.dp).offset(x = 18.dp, y = 18.dp).clickable(onClick = onEdit),"
if old in ps:
    ps = ps.replace(old, new, 1)
elif new not in ps:
    raise SystemExit("Could not relocate profile edit badge")
profile_screen.write_text(ps, encoding="utf-8")

# Debug-only installed-app validation entrypoint. It composes the real production screens,
# with deterministic data/photo/frame so CI can compare geometry without depending on auth/network.
debug_java = ROOT / "app/src/debug/java/com/sonharf/game/AvatarVisualValidationActivity.kt"
debug_java.parent.mkdir(parents=True, exist_ok=True)
debug_java.write_text(r'''package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend

class AvatarVisualValidationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.DEBUG)
        AvatarVisualValidationRuntime.avatarBytes = resources.openRawResource(R.drawable.son_harf_app_icon).readBytes()
        AvatarVisualValidationRuntime.forcedFrameId = PurchasedFrameCatalog.GOLD_CROWN
        val mode = intent.getStringExtra("mode") ?: "profile"
        setContent {
            MaterialTheme {
                when (mode) {
                    "profile" -> MainPlayerProfileScreen(
                        backend = remember { OnlineGameBackend() },
                        onEdit = {}, onVip = {}, onSettings = {}, onSocial = {},
                    )
                    "normal_match" -> LightDuelArena(
                        room = GameRoomDto(
                            id = "avatar-validation-room",
                            code = "AVATAR",
                            hostId = "avatar-validation-user",
                            guestId = "avatar-validation-rival",
                            status = "playing",
                            language = "tr",
                            hostScore = 24,
                            guestScore = 18,
                            currentPlayerId = "avatar-validation-user",
                            roundNo = 2,
                            hostRounds = 1,
                            guestRounds = 0,
                            isBot = true,
                            botName = "KelimeBot",
                        ),
                        me = "avatar-validation-user",
                        playerName = "Avatar QA",
                        playerAvatarPath = null,
                        playerGender = "erkek",
                        playerRating = 1260,
                        opponentName = "KelimeBot BOT",
                        opponentAvatarPath = null,
                        opponentGender = "erkek",
                        opponentRating = 1000,
                        words = emptyList(),
                        isVip = true,
                        feedbackWord = null,
                        feedbackCorrect = null,
                        wordInput = "",
                        onWordInput = {},
                        notice = "",
                        busy = false,
                        triviaRound = null,
                        triviaQuestion = null,
                        triviaSelection = null,
                        voiceSupported = false,
                        voiceUses = 0,
                        onSubmit = {}, onTimeout = {}, onBonus = {}, onVoice = {}, onTrivia = {},
                        onTriviaTimeout = {}, onChat = {}, onForfeit = {}, onExit = {}, onRematch = {},
                    )
                    "siege" -> WordSiegePracticeScreen(onExit = {})
                    else -> error("unknown validation mode: $mode")
                }
            }
        }
    }
}
''', encoding="utf-8")

debug_manifest = ROOT / "app/src/debug/AndroidManifest.xml"
debug_manifest.parent.mkdir(parents=True, exist_ok=True)
debug_manifest.write_text(r'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name=".AvatarVisualValidationActivity"
            android:exported="true" />
    </application>
</manifest>
''', encoding="utf-8")

print("Avatar visual standardization patch applied")
