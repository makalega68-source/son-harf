from pathlib import Path


def replace_exact(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new), encoding="utf-8")


replace_exact(
    "app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt",
    '''    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP
}''',
    '''    SOCIAL, SETTINGS, ACCOUNT, PROFILE_DETAILS, SHOP, PRO, PRIVATE_ROOM
}''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt",
    '''            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS, PremiumDestination.COLLECTION -> PremiumDestination.PROFILE
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS''',
    '''            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS, PremiumDestination.COLLECTION, PremiumDestination.PRO -> PremiumDestination.PROFILE
            PremiumDestination.PRIVATE_ROOM -> PremiumDestination.PRO
            PremiumDestination.SOCIAL, PremiumDestination.SHOP -> PremiumDestination.HOME
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt",
    '''                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.SHOP },
                        { destination = PremiumDestination.COLLECTION },''',
    '''                        { destination = PremiumDestination.PROFILE_DETAILS },
                        { destination = PremiumDestination.PRO },
                        { destination = PremiumDestination.COLLECTION },''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt",
    '''                    PremiumDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumDestination.COLLECTION },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()''',
    '''                    PremiumDestination.SHOP -> EconomyShopScreen(
                        onBack = { destination = PremiumDestination.HOME },
                        onMembershipChanged = { isPro = it },
                        onCollection = { destination = PremiumDestination.COLLECTION },
                        onPro = { destination = PremiumDestination.PRO },
                    )
                    PremiumDestination.PRO -> UnifiedProVipScreen(
                        backend = backend,
                        onBack = { destination = PremiumDestination.PROFILE },
                        onPrivateRoom = { destination = PremiumDestination.PRIVATE_ROOM },
                    )
                    PremiumDestination.PRIVATE_ROOM -> PrivateRoomCenterScreen(
                        onBack = { destination = PremiumDestination.PRO },
                        onRoomReady = { language -> openGame(PremiumDestination.LAST_LETTER, language) },
                    )
                    PremiumDestination.LAST_LETTER -> OnlineGameScreenV6()''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt",
    '''    onMembershipChanged: (Boolean) -> Unit = {},
    onCollection: () -> Unit = {},
) {''',
    '''    onMembershipChanged: (Boolean) -> Unit = {},
    onCollection: () -> Unit = {},
    onPro: () -> Unit = {},
) {''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt",
    '''            else EconomyCatalogScreen(tab, { tab = it }, { rewards = true }, onMembershipChanged, onCollection)''',
    '''            else EconomyCatalogScreen(tab, { tab = it }, { rewards = true }, onMembershipChanged, onCollection, onPro)''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt",
    '''    onMembershipChanged: (Boolean) -> Unit,
    onCollection: () -> Unit,
) {''',
    '''    onMembershipChanged: (Boolean) -> Unit,
    onCollection: () -> Unit,
    onPro: () -> Unit,
) {''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt",
    '''        if (section == 3) {
            item { ProShopCard(profile?.isVip == true) { showVip = true } }
            item { StoreProBenefits() }
        }''',
    '''        if (section == 3) {
            val proActive = profile?.isVip == true
            item { ProShopCard(proActive) { if (proActive) onPro() else showVip = true } }
            item { StoreProBenefits() }
            if (proActive) item {
                TextButton(onClick = onPro, modifier = Modifier.fillMaxWidth()) {
                    Text(sh("PRO ARAÇLARINI AÇ", "OPEN PRO TOOLS"), fontWeight = FontWeight.Black)
                }
            }
        }''',
)

replace_exact(
    "app/src/main/java/com/sonharf/game/MainSocialScreen.kt",
    '''notice = sh("${friend.displayName} Kelime Tahtı'na davet edildi.", "${friend.displayName} was invited to Kelime Tahtı.")''',
    '''notice = sh("${friend.displayName} Kelime Kuşatması'na davet edildi.", "${friend.displayName} was invited to Word Siege.")''',
)
replace_exact(
    "app/src/main/java/com/sonharf/game/PremiumAnalysisCenter.kt",
    '''    "siege" -> sh("Kelime Tahtı", "Kelime Tahtı")''',
    '''    "siege" -> sh("Kelime Kuşatması", "Word Siege")''',
)

print("PRO runtime routing patch applied")
