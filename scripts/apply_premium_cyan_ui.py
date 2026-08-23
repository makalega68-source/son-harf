from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

replacements = {
    "app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt": {
        "private val ClassicBg = Color(0xFFF7FBFF)": "private val ClassicBg = Color(0xFFF4FBFF)",
        "private val ClassicBgDeep = Color(0xFFE8F6FF)": "private val ClassicBgDeep = Color(0xFFEAF8FF)",
        "private val ClassicPanel2 = Color(0xFFEAF7FF)": "private val ClassicPanel2 = Color(0xFFEAF8FF)",
        "private val ClassicBorder = Color(0xFFB9E5F8)": "private val ClassicBorder = Color(0xFFB9E8F8)",
        "private val ClassicGold = Color(0xFF56BDE8)": "private val ClassicGold = Color(0xFF24AEE4)",
        "private val ClassicGoldSoft = Color(0xFF299FD3)": "private val ClassicGoldSoft = Color(0xFF1799D0)",
        "private val ClassicCream = Color(0xFF16324A)": "private val ClassicCream = Color(0xFF173B57)",
        "private val ClassicText = Color(0xFF16324A)": "private val ClassicText = Color(0xFF173B57)",
        "private val ClassicMuted = Color(0xFF698296)": "private val ClassicMuted = Color(0xFF6D879A)",
        "private val ClassicGreen = Color(0xFF77A878)": "private val ClassicGreen = Color(0xFF32C985)",
        "private val ClassicBlue = Color(0xFF43B6E8)": "private val ClassicBlue = Color(0xFF38C7F4)",
        "private val ClassicRed = Color(0xFFB66A68)": "private val ClassicRed = Color(0xFFFF7891)",
        "contentColor = Color(0xFF2A1E0D)": "contentColor = Color.White",
        "ClassicScreen.HOME -> ClassicHome(": "ClassicScreen.HOME -> PremiumMasterHome(",
    },
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt": {
        "private val TGbg = Color(0xFFF7FBFF)": "private val TGbg = Color(0xFFF4FBFF)",
        "private val TGpanel2 = Color(0xFFEAF7FF)": "private val TGpanel2 = Color(0xFFEAF8FF)",
        "private val TGcyan = Color(0xFF46BFEF)": "private val TGcyan = Color(0xFF38C7F4)",
        "private val TGpurple = Color(0xFF6CC8ED)": "private val TGpurple = Color(0xFF6ED6F7)",
        "private val TGpink = Color(0xFFEA7484)": "private val TGpink = Color(0xFFFF7891)",
        "private val TGgold = Color(0xFF52BCE8)": "private val TGgold = Color(0xFF24AEE4)",
        "private val TGblue = Color(0xFF2FA8DC)": "private val TGblue = Color(0xFF1799D0)",
        "private val TGtext = Color(0xFF16324A)": "private val TGtext = Color(0xFF173B57)",
        "private val TGmuted = Color(0xFF6B8294)": "private val TGmuted = Color(0xFF6D879A)",
        "NEON KELİME DÜELLOSU": "CANLI KELİME DÜELLOSU",
    },
    "app/src/main/java/com/sonharf/game/GamePortalApp.kt": {
        "internal val PortalBg = Color(0xFFF5FBFF)": "internal val PortalBg = Color(0xFFF4FBFF)",
        "internal val PortalText = Color(0xFF16324A)": "internal val PortalText = Color(0xFF173B57)",
        "internal val PortalMuted = Color(0xFF698296)": "internal val PortalMuted = Color(0xFF6D879A)",
        "internal val PortalBlue = Color(0xFF43B6E8)": "internal val PortalBlue = Color(0xFF24AEE4)",
        "internal val PortalGold = Color(0xFFD8AC5C)": "internal val PortalGold = Color(0xFFFFC857)",
        "internal val PortalGreen = Color(0xFF39B978)": "internal val PortalGreen = Color(0xFF32C985)",
        "internal val PortalRed = Color(0xFFCE6470)": "internal val PortalRed = Color(0xFFFF7891)",
    },
    "app/src/main/java/com/sonharf/game/SocialExperience.kt": {
        "color = Color(0xFF071027).copy(alpha = .96f)": "color = SonHarfSurface.copy(alpha = .98f)",
        "containerColor = Color(0xFF071027)": "containerColor = SonHarfSurface",
        "HorizontalDivider(color = Color.White.copy(alpha = .08f))": "HorizontalDivider(color = SonHarfMuted.copy(alpha = .14f))",
        "// Keep the historical quick-access feature, but make it visually part of the neon HUD.": "// Keep quick access visually consistent with the premium cyan UI.",
    },
    "app/src/main/java/com/sonharf/game/EconomyShopScreen.kt": {
        "Brush.verticalGradient(listOf(Color(0xFF040717), SonHarfBg, Color(0xFF070C1D)))": "Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))",
        "color = Color(0xFF122840)": "color = Color(0xFFEAF8FF)",
    },
    "app/src/main/java/com/sonharf/game/RewardCenterScreen.kt": {
        "contentColor = Color(0xFF171000)": "contentColor = Color.White",
    },
    "app/src/main/java/com/sonharf/game/LeaderboardExperience.kt": {
        "Brush.linearGradient(listOf(Color(0xFFB784FF), SonHarfPurple, Color(0xFF392071)))": "Brush.linearGradient(listOf(Color(0xFF8DE1FA), SonHarfBlue, Color(0xFF157FB0)))",
        "Brush.linearGradient(listOf(Color(0xFFFF52E8), SonHarfPurple, SonHarfCyan))": "Brush.linearGradient(listOf(Color(0xFF7FE2FA), SonHarfBlue, SonHarfCyan))",
    },
    "app/src/main/java/com/sonharf/game/BilBakalimExcitementScreen.kt": {
        "Color(0xFFF4FAFF)": "Color(0xFFF4FBFF)",
        "Color(0xFFE9F5FF)": "Color(0xFFEAF8FF)",
        "color = Color(0xFFD09A32)": "color = SonHarfBlue",
        "Color(0xFFFFF8E8)": "Color(0xFFEAF8FF)",
        "Color(0xFFE6C46D)": "Color(0xFFB9E8F8)",
    },
}

changed = []
for rel, mapping in replacements.items():
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in mapping.items():
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)

classic_path = ROOT / "app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt"
classic = classic_path.read_text(encoding="utf-8")
classic_original = classic
if "import com.sonharf.game.data.getLeaderboardV2" not in classic:
    classic = classic.replace("import com.sonharf.game.data.getAdminDashboard\n", "import com.sonharf.game.data.getAdminDashboard\nimport com.sonharf.game.data.getLeaderboardV2\n")
if "item { ClassicWeeklyTopThree(backend) }" not in classic:
    classic = classic.replace("        item { ClassicStats(growth) }", "        item { ClassicWeeklyTopThree(backend) }\n        item { ClassicStats(growth) }")
if classic != classic_original:
    classic_path.write_text(classic, encoding="utf-8")
    if "app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt" not in changed:
        changed.append("app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt")

print("premium cyan ui updated:", ", ".join(changed) if changed else "no changes")
