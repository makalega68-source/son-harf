package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Kelime Kuşatması'nın yeni ana görsel sistemi.
 *
 * Eski bej/sage "adult" tema tamamen kaldırıldı. Yeni varsayılan tema; satın alınan Casual Game UI
 * #02 paketinin güçlü mavi/yeşil/altın oyun hissini, metinsiz ve lisans açısından güvenli native
 * Compose yüzeyleriyle yeniden kurar. Oyun mekaniği ve veri akışı bu dosyadan etkilenmez.
 */
internal object SiegeRoyalePalette {
    val Background = Color(0xFFEAF3FF)
    val BackgroundDeep = Color(0xFFD8E9FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFE4EFFC)
    val SurfaceElevated = Color(0xFFF7FBFF)
    val Navigation = Color(0xFFF9FCFF)
    val Modal = Color(0xFFFFFFFF)

    val GameSurface = Color(0xFFDCEAF8)
    val GameTile = Color(0xFFFFFFFF)
    val GameTileBorder = Color(0xFFA9C4E5)

    val RoyalBlue = Color(0xFF246EDB)
    val RoyalBlueDeep = Color(0xFF174C9F)
    val RoyalBlueSoft = Color(0xFFDDEAFF)
    val Aqua = Color(0xFF17A7B8)
    val AquaDeep = Color(0xFF0E7B8B)
    val AquaSoft = Color(0xFFDDF5F7)
    val Emerald = Color(0xFF3AAF50)
    val EmeraldDeep = Color(0xFF24833A)
    val EmeraldSoft = Color(0xFFE0F4E4)
    val Violet = Color(0xFF765BE5)
    val VioletSoft = Color(0xFFECE8FF)
    val Amber = Color(0xFFF0A128)
    val AmberSoft = Color(0xFFFFF0D0)
    val RivalRed = Color(0xFFE25358)
    val RivalRedSoft = Color(0xFFFFE5E7)

    val Ink = Color(0xFF13223F)
    val Muted = Color(0xFF60718E)
    val Border = Color(0xFFBCD0EA)
    val Gold = Color(0xFFF0B33C)
    val GoldSoft = Color(0xFFFFE9AF)
}

/** Premium Black Theme remains a cosmetic-only alternative for users who own it. */
internal object BlackThemePalette {
    val Background = Color(0xFF0B1424)
    val Surface = Color(0xFF121F34)
    val SurfaceSecondary = Color(0xFF192A43)
    val SurfaceElevated = Color(0xFF203552)
    val Navigation = Color(0xFF0E1A2C)
    val Modal = Color(0xFF14243A)
    val GameSurface = Color(0xFF101D30)
    val GameTile = Color(0xFF1B2C46)
    val GameTileBorder = Color(0xFF365575)
    val Primary = Color(0xFF6CA8FF)
    val PrimarySoft = Color(0xFF173761)
    val Turquoise = Color(0xFF45D4D0)
    val Purple = Color(0xFFA58CFF)
    val Orange = Color(0xFFFFB84E)
    val Green = Color(0xFF62D878)
    val Text = Color(0xFFF6F9FF)
    val Muted = Color(0xFFA9B8CC)
    val Border = Color(0xFF304867)
    val SuccessSoft = Color(0xFF173F32)
    val Disabled = Color(0xFF223047)
    val DisabledText = Color(0xFF70829B)
}

/** Single source of truth for the complete application shell and all game modes. */
internal object SonHarfTheme {
    val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive

    val Background: Color get() = if (IsDark) BlackThemePalette.Background else SiegeRoyalePalette.Background
    val Surface: Color get() = if (IsDark) BlackThemePalette.Surface else SiegeRoyalePalette.Surface
    val SurfaceSecondary: Color get() = if (IsDark) BlackThemePalette.SurfaceSecondary else SiegeRoyalePalette.SurfaceSecondary
    val SurfaceElevated: Color get() = if (IsDark) BlackThemePalette.SurfaceElevated else SiegeRoyalePalette.SurfaceElevated
    val NavigationSurface: Color get() = if (IsDark) BlackThemePalette.Navigation else SiegeRoyalePalette.Navigation
    val ModalSurface: Color get() = if (IsDark) BlackThemePalette.Modal else SiegeRoyalePalette.Modal

    val GameSurface: Color get() = if (IsDark) BlackThemePalette.GameSurface else SiegeRoyalePalette.GameSurface
    val GameTile: Color get() = if (IsDark) BlackThemePalette.GameTile else SiegeRoyalePalette.GameTile
    val GameTileBorder: Color get() = if (IsDark) BlackThemePalette.GameTileBorder else SiegeRoyalePalette.GameTileBorder

    val Primary: Color get() = if (IsDark) BlackThemePalette.Primary else SiegeRoyalePalette.RoyalBlue
    val PrimarySoft: Color get() = if (IsDark) BlackThemePalette.PrimarySoft else SiegeRoyalePalette.RoyalBlueSoft
    val SoftBlue: Color get() = if (IsDark) Color(0xFF8AB8EE) else Color(0xFF5D8CC7)
    val Turquoise: Color get() = if (IsDark) BlackThemePalette.Turquoise else SiegeRoyalePalette.Aqua
    val ActionOrange: Color get() = if (IsDark) BlackThemePalette.Orange else SiegeRoyalePalette.Amber
    val Lavender: Color get() = if (IsDark) BlackThemePalette.Purple else SiegeRoyalePalette.Violet
    val Sand: Color get() = if (IsDark) Color(0xFF3E321D) else SiegeRoyalePalette.AmberSoft
    val PlayGreen: Color get() = if (IsDark) BlackThemePalette.Green else SiegeRoyalePalette.Emerald
    val PlayGreenDeep: Color get() = if (IsDark) Color(0xFF329D4D) else SiegeRoyalePalette.EmeraldDeep

    val TextPrimary: Color get() = if (IsDark) BlackThemePalette.Text else SiegeRoyalePalette.Ink
    val TextSecondary: Color get() = if (IsDark) BlackThemePalette.Muted else SiegeRoyalePalette.Muted
    val Border: Color get() = if (IsDark) BlackThemePalette.Border else SiegeRoyalePalette.Border

    val Success: Color get() = PlayGreen
    val SuccessSoft: Color get() = if (IsDark) BlackThemePalette.SuccessSoft else SiegeRoyalePalette.EmeraldSoft
    val Error: Color get() = if (IsDark) Color(0xFFFF6D75) else SiegeRoyalePalette.RivalRed
    val Warning: Color get() = ActionOrange
    val DisabledBackground: Color get() = if (IsDark) BlackThemePalette.Disabled else Color(0xFFDCE5F1)
    val DisabledContent: Color get() = if (IsDark) BlackThemePalette.DisabledText else Color(0xFF8EA0B8)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Flagship hero: royal blue -> electric blue -> aqua. No landscape/fantasy artwork required.
    val HeroStart: Color get() = if (IsDark) Color(0xFF123A72) else Color(0xFF174EAE)
    val HeroMiddle: Color get() = if (IsDark) Color(0xFF185B9D) else Color(0xFF2677E5)
    val HeroEnd: Color get() = if (IsDark) Color(0xFF146A78) else Color(0xFF17A7B8)

    // Compatibility aliases used by existing screens. They now resolve to the new game theme.
    val Forest: Color get() = Turquoise
    val ForestDeep: Color get() = if (IsDark) Color(0xFF0A4450) else SiegeRoyalePalette.RoyalBlueDeep
    val PremiumGold: Color get() = if (IsDark) Color(0xFFFFCF67) else SiegeRoyalePalette.Gold
    val PremiumGoldLight: Color get() = if (IsDark) Color(0xFF4A3B19) else SiegeRoyalePalette.GoldSoft
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
