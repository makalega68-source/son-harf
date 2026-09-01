#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise SystemExit(f"Expected block not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_monster_ui() -> None:
    path = ROOT / "app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt"
    text = path.read_text(encoding="utf-8")
    replacement = '''internal object MonsterUi {
    private val palette: SonHarfThemePalette get() = SonHarfCosmetics.currentThemePalette
    val Background: Color get() = palette.background
    val Surface: Color get() = palette.surface
    val SurfaceRaised: Color get() = palette.surfaceRaised
    val SurfaceSoft: Color get() = palette.surfaceSoft
    val Text: Color get() = palette.text
    val Muted: Color get() = palette.muted
    val Border: Color get() = palette.border
    val Accent: Color get() = palette.accent
    val AccentText: Color get() = palette.accentText
    val Live: Color get() = palette.live
    val Coral: Color get() = palette.coral
    val Orange: Color get() = palette.orange
    val Green: Color get() = palette.green
    val Gold: Color get() = palette.gold
}
'''
    updated, count = re.subn(r'internal object MonsterUi \{.*?\n\}\n(?=\nprivate data class MonsterHomeStats)', replacement, text, count=1, flags=re.S)
    if count == 0:
        if 'private val palette: SonHarfThemePalette get()' in text:
            return
        raise SystemExit("MonsterUi block not found")
    path.write_text(updated, encoding="utf-8")


def patch_main_ui() -> None:
    path = ROOT / "app/src/main/java/com/sonharf/game/MainExperienceApp.kt"
    text = path.read_text(encoding="utf-8")
    replacement = '''internal object MainUi {
    val Background: Color get() = MonsterUi.Background
    val Surface: Color get() = MonsterUi.Surface
    val SurfaceSoft: Color get() = MonsterUi.SurfaceSoft
    val Text: Color get() = MonsterUi.Text
    val Muted: Color get() = MonsterUi.Muted
    val Blue: Color get() = MonsterUi.Accent
    val BlueDeep: Color get() = MonsterUi.Accent
    val BlueSoft: Color get() = MonsterUi.SurfaceRaised
    val Border: Color get() = MonsterUi.Border
    val Green: Color get() = MonsterUi.Green
    val Gold: Color get() = MonsterUi.Gold
    val Red: Color get() = MonsterUi.Coral
    val Purple: Color get() = if (SonHarfCosmetics.sapphireIceTheme) Color(0xFF9B8CFF) else Color(0xFF7659D6)
}
'''
    updated, count = re.subn(r'internal object MainUi \{.*?\n\}\n(?=\n@Composable\nfun SonHarfMainApp)', replacement, text, count=1, flags=re.S)
    if count == 0:
        if 'val Background: Color get() = MonsterUi.Background' in text:
            return
        raise SystemExit("MainUi block not found")
    path.write_text(updated, encoding="utf-8")


def patch_profile_selector() -> None:
    path = ROOT / "app/src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt"
    text = path.read_text(encoding="utf-8")
    if 'ProfileThemeSelector(backend)' in text:
        return
    marker = '''        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(
                    onClick = onVip,'''
    insertion = '''        item {
            MainSectionTitle(sh("TEMA & GÖRÜNÜM", "THEME & APPEARANCE"))
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MainUi.Surface,
                border = BorderStroke(1.dp, MainUi.Border),
            ) {
                Box(Modifier.fillMaxWidth().padding(14.dp)) {
                    ProfileThemeSelector(backend)
                }
            }
        }

''' + marker
    replace_once(path, marker, insertion)


def patch_vip_benefits() -> None:
    path = ROOT / "app/src/main/java/com/sonharf/game/MainSettingsVipScreen.kt"
    text = path.read_text(encoding="utf-8")
    old = '''                    MainVipBenefit(Icons.Rounded.Block, sh("Reklamsız deneyim", "Ad-free experience"), sh("Maç dışında da sade ve kesintisiz", "Clean and uninterrupted outside matches"))
                    MainVipBenefit(Icons.Rounded.Verified, sh("VIP profil rozeti", "VIP profile badge"), sh("Profil ve sosyal alanlarda görünür", "Visible on profile and social surfaces"))
                    MainVipBenefit(Icons.Rounded.Checkroom, sh("Özel Style içerikleri", "Exclusive Style content"), sh("Profil çerçevesi ve kişiselleştirme", "Profile frames and personalization"))
                    MainVipBenefit(Icons.Rounded.History, sh("Kelime geçmişi", "Word history"), sh("Düelloda son kelimeleri gör", "See recent words during a duel"))
                    MainVipBenefit(Icons.Rounded.Insights, sh("Gelişmiş istatistikler", "Advanced statistics"), sh("Performansını daha ayrıntılı incele", "Review performance in more detail"))
                    MainVipBenefit(Icons.Rounded.Lock, sh("Özel oda oluşturma", "Create private rooms"), sh("Arkadaşlarınla kodlu oda aç", "Open coded rooms with friends"))
                    MainVipBenefit(Icons.Rounded.Storefront, sh("VIP Style görünümü", "VIP Style view"), sh("Üyelere özel ürünleri keşfet", "Discover member-only items"))'''
    new = '''                    MainVipBenefit(Icons.Rounded.Diamond, sh("Aylık 400 Son Coin", "400 Son Coin monthly"), sh("Her üyelik ayında hesabına Style bütçesi", "A Style budget added each membership month"))
                    MainVipBenefit(Icons.Rounded.Block, sh("Reklamsız deneyim", "Ad-free experience"), sh("Opsiyonel ödül akışlarında reklam beklemeden, aynı limitlerle devam et", "Skip ads in optional reward flows while keeping the same limits"))
                    MainVipBenefit(Icons.Rounded.Verified, sh("VIP profil rozeti", "VIP profile badge"), sh("Profil ve sosyal alanlarda görünür", "Visible on profile and social surfaces"))
                    MainVipBenefit(Icons.Rounded.Checkroom, sh("Özel Style içerikleri", "Exclusive Style content"), sh("VIP görünüm ve kişiselleştirme seçenekleri", "VIP appearance and personalization options"))
                    MainVipBenefit(Icons.Rounded.Insights, sh("Gelişmiş istatistikler", "Advanced statistics"), sh("Performansını daha ayrıntılı incele", "Review performance in more detail"))
                    MainVipBenefit(Icons.Rounded.Lock, sh("Özel oda oluşturma", "Create private rooms"), sh("Arkadaşlarınla kodlu oda aç", "Open coded rooms with friends"))
                    MainVipBenefit(Icons.Rounded.Storefront, sh("VIP Style görünümü", "VIP Style view"), sh("Üyelere özel ürünleri keşfet", "Discover member-only items"))'''
    if old in text:
        text = text.replace(old, new, 1)
    elif 'Aylık 400 Son Coin' not in text:
        raise SystemExit("VIP benefit block not found")
    path.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    patch_monster_ui()
    patch_main_ui()
    patch_profile_selector()
    patch_vip_benefits()
    print("Theme/VIP integration applied")
