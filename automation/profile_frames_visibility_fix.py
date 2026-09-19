from pathlib import Path
import re

ROOT = Path("app/src/main/java/com/sonharf/game")
PHOTO = ROOT / "ProfilePhotoRuntime.kt"
FRAMES = ROOT / "ProfileFramesV2.kt"
PROFILE = ROOT / "MainPlayerProfileScreen.kt"
STORE = ROOT / "PremiumStoreScreen.kt"
SIEGE_UI = ROOT / "WordSiegeGameUi.kt"
PRACTICE = ROOT / "WordSiegePracticeScreen.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# 1) Remove gender symbols from every shared avatar renderer while preserving gender data itself.
photo = PHOTO.read_text(encoding="utf-8")
if "private data class GenderVisual" in photo:
    photo = re.sub(
        r"\nprivate data class GenderVisual\(.*?\n@Composable\nprivate fun SyntheticProfilePortrait",
        "\n@Composable\nprivate fun SyntheticProfilePortrait",
        photo,
        count=1,
        flags=re.S,
    )
photo = photo.replace("    val visual = genderVisual(gender)\n", "")
photo = photo.replace(
    "                    (visual?.color ?: Color(0xFF57C7F3)).copy(alpha = .28f),\n",
    "                    Color(0xFF57C7F3).copy(alpha = .28f),\n",
)
photo = photo.replace(
    """        Box(Modifier.align(Alignment.BottomEnd)) {
            FramelessGenderSymbol(gender, size)
        }
""",
    "",
)
photo = photo.replace(
    """        if (showGenderBadge) {
            Box(Modifier.align(Alignment.BottomEnd)) {
                FramelessGenderSymbol(gender, size)
            }
        }
""",
    "",
)
photo = photo.replace(
    """        if (showGenderBadge) {
            Box(Modifier.align(Alignment.BottomEnd)) {
                FramelessGenderSymbol(gender, diameter)
            }
        }
""",
    "",
)
PHOTO.write_text(photo, encoding="utf-8")

# 2) V2 decorative frames must never add a gender badge.
frames = FRAMES.read_text(encoding="utf-8")
if "private data class ProfileFrameGenderVisual" in frames:
    frames = re.sub(
        r"\nprivate data class ProfileFrameGenderVisual\(.*?\n@Composable\ninternal fun ProfileFrameAvatarBytesV2",
        "\n@Composable\ninternal fun ProfileFrameAvatarBytesV2",
        frames,
        count=1,
        flags=re.S,
    )
frames = frames.replace(
    """        if (showGenderBadge) {
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                ProfileFrameGenderBadge(gender = gender, outerSize = outerSize)
            }
        }
""",
    "",
)

# Make the ordinary-player frame visibly white even on very light profile backgrounds.
# The transparent artwork stays on top; a crisp white support ring is layered underneath the photo.
if "val standardFrame = equippedPaidFrameId == null && !isPro" not in frames:
    frames = replace_once(
        frames,
        """    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        val photoSize = outerSize * visual.photoRatio
        if (bitmap != null) {""",
        """    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        val photoSize = outerSize * visual.photoRatio
        val standardFrame = equippedPaidFrameId == null && !isPro
        if (standardFrame) {
            Surface(
                modifier = Modifier.size(photoSize + 12.dp),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, Color(0xFFD7DDE5)),
                shadowElevation = 2.dp,
            ) {}
        }
        if (bitmap != null) {""",
        "bytes standard white support ring",
    )
    frames = replace_once(
        frames,
        """    val photoSize = outerSize * visual.photoRatio

    // Render the photo directly beneath the decorative PNG. Do not call the legacy avatar
    // renderer here: it adds its own gradient ring/padding and creates a visible double-frame.
    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        if (bitmap != null) {""",
        """    val photoSize = outerSize * visual.photoRatio
    val standardFrame = equippedPaidFrameId == null && !isPro

    // Render the photo directly beneath the decorative PNG. Do not call the legacy avatar
    // renderer here: it adds its own gradient ring/padding and creates a visible double-frame.
    Box(modifier = Modifier.size(outerSize), contentAlignment = Alignment.Center) {
        if (standardFrame) {
            Surface(
                modifier = Modifier.size(photoSize + 12.dp),
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(2.dp, Color(0xFFD7DDE5)),
                shadowElevation = 2.dp,
            ) {}
        }
        if (bitmap != null) {""",
        "path standard white support ring",
    )
FRAMES.write_text(frames, encoding="utf-8")

# 3) Active profile screen validates equipped paid frame against server-owned inventory.
profile = PROFILE.read_text(encoding="utf-8")
if "val inventoryTask = async" not in profile:
    profile = replace_once(
        profile,
        "        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }\n",
        "        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }\n        val inventoryTask = async { runCatching { backend.getInventory() }.getOrDefault(emptySet()) }\n",
        "profile inventory task",
    )
    profile = replace_once(
        profile,
        "        SonHarfCosmetics.apply(cosmeticsTask.await())\n",
        "        SonHarfCosmetics.apply(cosmeticsTask.await(), inventoryTask.await())\n",
        "profile authoritative cosmetics",
    )
PROFILE.write_text(profile, encoding="utf-8")

# 4) Paid frames are visible from Featured as well as Styles, before Play ProductDetails load.
store = STORE.read_text(encoding="utf-8")
store = store.replace("                if (tab == 2) {\n", "                if (tab == 0 || tab == 2) {\n", 1)
STORE.write_text(store, encoding="utf-8")

frames = FRAMES.read_text(encoding="utf-8")
frames = frames.replace(
    '            sh("Tek ödeme • kalıcı • yalnızca kozmetik", "One-time purchase • permanent • cosmetic only"),',
    '            sh("Her biri 150 TL • tek ödeme • kalıcı • yalnızca kozmetik", "150 TL each • one-time purchase • permanent • cosmetic only"),',
    1,
)
FRAMES.write_text(frames, encoding="utf-8")

# 5) Shared Kelime Kuşatması score cards use the frame renderer. This covers practice and every
# other game surface that uses WordSiegeScoreCard.
siege = SIEGE_UI.read_text(encoding="utf-8")
if "frameId: String? = null" not in siege:
    siege = replace_once(
        siege,
        """    avatarVisible: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier,
)""",
        """    avatarVisible: Boolean,
    isBot: Boolean,
    frameId: String? = null,
    isPro: Boolean = false,
    modifier: Modifier = Modifier,
)""",
        "WordSiegeScoreCard frame parameters",
    )
    siege = replace_once(
        siege,
        """                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 34.dp,
                    accent = accent,
                    visible = avatarVisible,
                )""",
        """                FramedProfilePhotoAvatar(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 42.dp,
                    accent = accent,
                    frameId = frameId,
                    visible = avatarVisible,
                    showGenderBadge = false,
                    isPro = isPro,
                )""",
        "WordSiegeScoreCard framed avatar",
    )
SIEGE_UI.write_text(siege, encoding="utf-8")

# 6) Practice resolves the local player's verified frame + PRO entitlement. Bots use the ordinary
# white frame. No score card renders a gender symbol.
practice = PRACTICE.read_text(encoding="utf-8")
for import_line in [
    "import com.sonharf.game.data.getEquippedCosmetics\n",
    "import com.sonharf.game.data.getInventory\n",
    "import com.sonharf.game.data.getVipEntitlements\n",
]:
    if import_line not in practice:
        practice = practice.replace("import com.sonharf.game.data.SharedDictionaryService\n", "import com.sonharf.game.data.SharedDictionaryService\n" + import_line, 1)

if "var playerIsPro by remember" not in practice:
    practice = replace_once(
        practice,
        "    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }\n",
        "    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }\n    var playerIsPro by remember { mutableStateOf(false) }\n    var playerFrameId by remember { mutableStateOf<String?>(null) }\n",
        "practice frame state",
    )
    practice = replace_once(
        practice,
        """    LaunchedEffect(me, backend) {
        val b = backend ?: return@LaunchedEffect
        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }
""",
        """    LaunchedEffect(me, backend) {
        val b = backend ?: return@LaunchedEffect
        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()
        val owned = runCatching { b.getInventory() }.getOrDefault(emptySet())
        val equipped = runCatching { b.getEquippedCosmetics() }.getOrNull()
        SonHarfCosmetics.apply(equipped, owned)
        playerFrameId = ProfileFrameV2Catalog.ownedPaidFrame(equipped?.profileFrameId, owned)
        playerIsPro = runCatching { b.getVipEntitlements().isPro }.getOrDefault(false)
    }
""",
        "practice authoritative frame load",
    )
    practice = replace_once(
        practice,
        """                        avatarVisible = playerProfile?.avatarVisibility != "hidden",
                        isBot = false,
                        modifier = Modifier.weight(1f),""",
        """                        avatarVisible = playerProfile?.avatarVisibility != "hidden",
                        isBot = false,
                        frameId = playerFrameId,
                        isPro = playerIsPro,
                        modifier = Modifier.weight(1f),""",
        "practice player frame arguments",
    )
    practice = replace_once(
        practice,
        """    avatarVisible: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier,
) {
    WordSiegeScoreCard(""",
        """    avatarVisible: Boolean,
    isBot: Boolean,
    frameId: String? = null,
    isPro: Boolean = false,
    modifier: Modifier = Modifier,
) {
    WordSiegeScoreCard(""",
        "practice score wrapper frame params",
    )
    practice = replace_once(
        practice,
        """        gender = gender, avatarVisible = avatarVisible, isBot = isBot,
        modifier = modifier,""",
        """        gender = gender, avatarVisible = avatarVisible, isBot = isBot,
        frameId = frameId, isPro = isPro, modifier = modifier,""",
        "practice score wrapper forward frame",
    )
PRACTICE.write_text(practice, encoding="utf-8")

# 7) Release guards: no displayed gender symbol remains, and active surfaces are wired to V2.
all_runtime = "\n".join(p.read_text(encoding="utf-8") for p in ROOT.rglob("*.kt"))
for symbol in ("♀", "♂"):
    if symbol in all_runtime:
        raise SystemExit(f"Visible gender symbol still present in Kotlin runtime: {symbol}")

checks = {
    PHOTO: ["internal fun ProfilePhotoAvatarWithGender", "internal fun ProfilePhotoAvatarRectWithGender"],
    FRAMES: ["standardFrame", "Her biri 150 TL"],
    PROFILE: ["inventoryTask", "SonHarfCosmetics.apply(cosmeticsTask.await(), inventoryTask.await())"],
    STORE: ["if (tab == 0 || tab == 2)", "ProfileFramesV2StoreRow("],
    SIEGE_UI: ["FramedProfilePhotoAvatar(", "frameId: String? = null", "isPro: Boolean = false"],
    PRACTICE: ["playerFrameId", "playerIsPro", "ProfileFrameV2Catalog.ownedPaidFrame"],
}
for path, fragments in checks.items():
    text = path.read_text(encoding="utf-8")
    for fragment in fragments:
        if fragment not in text:
            raise SystemExit(f"{path.name} missing required fix: {fragment}")

print("Profile frame visibility + no-gender-symbol fix applied and verified")
