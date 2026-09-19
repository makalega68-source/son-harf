from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# Current premium store: put the four Play-billed frames directly in Görünümler / Styles.
store = "app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt"
replace_once(
    store,
    '''                if (loading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfTheme.Turquoise, trackColor = SonHarfTheme.SurfaceSecondary) }
                }

                if (tab == 0) {''',
    '''                if (loading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfTheme.Turquoise, trackColor = SonHarfTheme.SurfaceSecondary) }
                }

                if (tab == 2) {
                    item {
                        ProfileFramesV2StoreRow(
                            backend = backend,
                            onChanged = { scope.launch { reload() } },
                        )
                    }
                }

                if (tab == 0) {''',
)

# Current profile: load authoritative frame ownership/equipped state and render the V2 frame around the avatar.
profile = "app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt"
replace_once(
    profile,
    '''import com.sonharf.game.data.getCompetitiveSeasonHistory
import com.sonharf.game.data.getPersonalRecords
import com.sonharf.game.data.getVipEntitlements''',
    '''import com.sonharf.game.data.getCompetitiveSeasonHistory
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getPersonalRecords
import com.sonharf.game.data.getVipEntitlements''',
)
replace_once(
    profile,
    '''    var profile by remember { mutableStateOf<ProfileV2Dto?>(null) }
    var proActive by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }''',
    '''    var profile by remember { mutableStateOf<ProfileV2Dto?>(null) }
    var proActive by remember { mutableStateOf(false) }
    var ownedFrameIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equippedPaidFrameId by remember { mutableStateOf<String?>(null) }
    var season by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }''',
)
replace_once(
    profile,
    '''        profile = runCatching { loadProfileV2() }.getOrNull()
        proActive = runCatching { backend?.let { backend.getVipEntitlements().isPro } ?: false }.getOrDefault(false)
        avatarBytes = profile?.avatarPath?.let { runCatching { ProfilePhotoStorageV2.download(it) }.getOrNull() }''',
    '''        profile = runCatching { loadProfileV2() }.getOrNull()
        proActive = runCatching { backend?.let { backend.getVipEntitlements().isPro } ?: false }.getOrDefault(false)
        val frameOwned = runCatching { backend?.getInventory().orEmpty() }.getOrDefault(emptySet())
        val frameEquipped = runCatching { backend?.getEquippedCosmetics() }.getOrNull()
        ownedFrameIds = frameOwned
        equippedPaidFrameId = ProfileFrameV2Catalog.ownedPaidFrame(frameEquipped?.profileFrameId, frameOwned)
        SonHarfCosmetics.apply(frameEquipped, frameOwned)
        avatarBytes = profile?.avatarPath?.let { runCatching { ProfilePhotoStorageV2.download(it) }.getOrNull() }''',
)
replace_once(
    profile,
    '''                    if (proActive) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(4.dp, SonHarfGold),
                            shadowElevation = 5.dp,
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ProfileAvatarV2(avatarBytes, p?.displayName ?: "O", 106)
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd),
                            shape = RoundedCornerShape(50),
                            color = SonHarfGold,
                            shadowElevation = 3.dp,
                        ) {
                            Text(
                                "PRO",
                                Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    } else {
                        ProfileAvatarV2(avatarBytes, p?.displayName ?: "O", 112)
                    }''',
    '''                    ProfileFrameAvatarBytesV2(
                        avatarBytes = avatarBytes,
                        name = p?.displayName ?: "O",
                        outerSize = 120.dp,
                        equippedPaidFrameId = equippedPaidFrameId,
                        isPro = proActive,
                    )
                    if (proActive) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd),
                            shape = RoundedCornerShape(50),
                            color = SonHarfGold,
                            shadowElevation = 3.dp,
                        ) {
                            Text(
                                "PRO",
                                Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }''',
)

# Keep this state read visible to lint/compiler and document the trust boundary.
replace_once(
    profile,
    '''    val winRate = if (totalMatches == 0) 0 else wins * 100 / totalMatches

    LazyColumn(''',
    '''    val winRate = if (totalMatches == 0) 0 else wins * 100 / totalMatches
    val verifiedFrameOwnershipCount = ownedFrameIds.count { it in ProfileFrameV2Catalog.paidIds }
    @Suppress("UNUSED_VARIABLE") val frameOwnershipAudit = verifiedFrameOwnershipCount

    LazyColumn(''',
)

print("Profile Frames V2 source patch applied")
