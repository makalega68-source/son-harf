package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Compatibility palette for older screens that have not yet been moved to the Game* component
 * layer. The production visual authority is GameDesignSystem; these names remain only so legacy
 * call sites can compile without reintroducing the retired neon/yellow Monster theme.
 */
internal object KelimeKusatmasiPalette {
    val MonsterBlack: Color get() = GameColors.AppBackground
    val MonsterSurface: Color get() = GameColors.PrimarySurface
    val MonsterSurface2: Color get() = GameColors.SecondarySurface
    val MonsterSurface3: Color get() = GameColors.ElevatedBackground
    val MonsterLime: Color get() = GameColors.PrimaryBlue
    val MonsterRed: Color get() = GameColors.Danger
    val MonsterPink: Color get() = GameColors.Lavender
    val MonsterOrange: Color get() = GameColors.RewardAmber
    val MonsterText: Color get() = GameColors.TextPrimary
    val MonsterMuted: Color get() = GameColors.TextSecondary
    val MonsterBorder: Color get() = GameColors.Border

    val RoyalBlue: Color get() = GameColors.PrimaryBlue
    val DeepBlue: Color get() = GameColors.DeepBlue
    val Turquoise: Color get() = GameColors.TacticalTurquoise
    val Orange: Color get() = GameColors.RewardAmber
    val Sky: Color get() = GameColors.SecondarySurface
    val OffWhite: Color get() = GameColors.AppBackground
    val Slate: Color get() = GameColors.TextSecondary
    val PaleMint: Color get() = GameColors.SecondarySurface
    val SoftIndigo: Color get() = GameColors.Lavender
    val SageGreen: Color get() = GameColors.PlayGreen
    val SoftBlue: Color get() = GameColors.PrimaryBlue
    val LightBeige: Color get() = GameColors.PrimarySurface
    val Lavender: Color get() = GameColors.Lavender
    val SlateBlue: Color get() = GameColors.TextTertiary
    val WarmAccent: Color get() = GameColors.Danger
}

/**
 * Backward-compatible theme facade.
 *
 * New production screens must use GameTheme/GameColors directly. Any older screen still using
 * SonHarfTheme receives the same professional navy/blue/turquoise/green/lavender system so a
 * legacy call site can no longer leak the retired neon/yellow visual language into the APK.
 */
internal object SonHarfTheme {
    private val alternateDark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = true

    val Background: Color get() = if (alternateDark) Color(0xFF0C121C) else GameColors.AppBackground
    val Surface: Color get() = if (alternateDark) Color(0xFF162232) else GameColors.PrimarySurface
    val SurfaceSecondary: Color get() = if (alternateDark) Color(0xFF1E3044) else GameColors.SecondarySurface
    val SurfaceElevated: Color get() = if (alternateDark) Color(0xFF182536) else GameColors.ElevatedBackground
    val NavigationSurface: Color get() = if (alternateDark) Color(0xFF0E1621) else GameColors.ElevatedBackground
    val ModalSurface: Color get() = if (alternateDark) Color(0xFF172435) else GameColors.PrimarySurface

    val GameSurface: Color get() = if (alternateDark) Color(0xFF101B28) else GameColors.ElevatedBackground
    val GameTile: Color get() = GameColors.LightSurface
    val GameTileBorder: Color get() = if (alternateDark) Color(0xFF43566D) else GameColors.Border

    val Primary: Color get() = GameColors.PrimaryBlue
    val PrimarySoft: Color get() = GameColors.PrimaryBlue.copy(alpha = .14f)
    val SoftBlue: Color get() = GameColors.PrimaryBlue
    val Turquoise: Color get() = GameColors.TacticalTurquoise
    val ActionOrange: Color get() = GameColors.RewardAmber
    val Lavender: Color get() = GameColors.Lavender
    val Sand: Color get() = GameColors.RewardAmber

    val TextPrimary: Color get() = GameColors.TextPrimary
    val TextSecondary: Color get() = GameColors.TextSecondary
    val Border: Color get() = if (alternateDark) Color(0xFF3A4D64) else GameColors.Border

    val Success: Color get() = GameColors.PlayGreen
    val SuccessSoft: Color get() = GameColors.PlayGreen.copy(alpha = .12f)
    val Error: Color get() = GameColors.Danger
    val Warning: Color get() = GameColors.RewardAmber
    val DisabledBackground: Color get() = GameColors.Disabled
    val DisabledContent: Color get() = GameColors.DisabledContent

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    val HeroStart: Color get() = GameColors.HeroStart
    val HeroMiddle: Color get() = GameColors.HeroMiddle
    val HeroEnd: Color get() = GameColors.HeroEnd

    val Forest: Color get() = GameColors.PlayGreen
    val ForestDeep: Color get() = GameColors.PlayGreenDeep

    val PremiumGold: Color get() = GameColors.PrestigeGold
    val PremiumGoldLight: Color get() = GameColors.RewardAmber

    val PrimaryBlue: Color get() = GameColors.PrimaryBlue
    val PrimaryBlueSoft: Color get() = GameColors.PrimaryBlue.copy(alpha = .14f)
    val SecondaryAccent: Color get() = GameColors.TacticalTurquoise
    val Purple: Color get() = GameColors.Lavender
}
