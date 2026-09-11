package com.sonharf.game

import androidx.compose.ui.graphics.Color

/**
 * Son Harf application-wide visual system.
 *
 * The default/light appearance is the premium botanical identity used by the
 * home reference: warm ivory, sage, soft teal/blue and restrained gold. Every
 * non-game and compatible game surface resolves through these semantic tokens
 * so individual screens do not drift back to an unrelated palette.
 *
 * Cosmetic themes may still switch the optional dark arena treatment, but they
 * never change gameplay state, scoring or economy.
 */
internal object SonHarfTheme {
    private val dark: Boolean get() = SonHarfCosmetics.darkArenaTheme

    val IsDark: Boolean get() = dark

    // Foundation layers.
    val Background: Color get() = if (dark) Color(0xFF101914) else Color(0xFFF8FAF4)
    val Surface: Color get() = if (dark) Color(0xFF18241E) else Color(0xFFFFFEF8)
    val SurfaceSecondary: Color get() = if (dark) Color(0xFF213129) else Color(0xFFEEF5EF)
    val SurfaceElevated: Color get() = if (dark) Color(0xFF293A32) else Color(0xFFF5F7F2)
    val NavigationSurface: Color get() = if (dark) Color(0xFF1A2821) else Color(0xFFF1F5EF)
    val ModalSurface: Color get() = if (dark) Color(0xFF202E27) else Color(0xFFFFFBF2)

    // Gameplay layers: light mint board and warm sand letter surfaces.
    val GameSurface: Color get() = if (dark) Color(0xFF1A2B27) else Color(0xFFEDF5F1)
    val GameTile: Color get() = if (dark) Color(0xFF4A4336) else Color(0xFFF3E8D3)
    val GameTileBorder: Color get() = if (dark) Color(0xFF817150) else Color(0xFFC8AE78)

    // Brand/accent family.
    val Primary: Color get() = if (dark) Color(0xFF557B67) else Color(0xFF4F7964)
    val PrimarySoft: Color get() = if (dark) Color(0xFF293D32) else Color(0xFFDDE9E1)
    val SoftBlue: Color get() = if (dark) Color(0xFF55798A) else Color(0xFF557A87)
    val Turquoise: Color get() = if (dark) Color(0xFF4C817C) else Color(0xFF4F8B82)
    val Lavender: Color get() = if (dark) Color(0xFF7C7196) else Color(0xFF7A7396)
    val Sand: Color get() = if (dark) Color(0xFFD3BE91) else Color(0xFFDCC8A0)

    // Text and dividers.
    val TextPrimary: Color get() = if (dark) Color(0xFFF3F6F2) else Color(0xFF213C31)
    val TextSecondary: Color get() = if (dark) Color(0xFFB8C8BF) else Color(0xFF687C72)
    val Border: Color get() = if (dark) Color(0xFF3C5448) else Color(0xFFD1DDD5)

    // Semantic states; intentionally calm enough to coexist with the botanical UI.
    val Success: Color get() = if (dark) Color(0xFF5C876C) else Color(0xFF4E7B61)
    val Error: Color get() = if (dark) Color(0xFFB95C66) else Color(0xFFA9535E)
    val Warning: Color get() = if (dark) Color(0xFFC19853) else Color(0xFF9B733B)
    val DisabledBackground: Color get() = if (dark) Color(0xFF2B3932) else Color(0xFFE4EAE5)
    val DisabledContent: Color get() = if (dark) Color(0xFF83948B) else Color(0xFF829189)

    val OnPrimary: Color get() = Color.White
    val OnSecondary: Color get() = Color.White
    val OnTertiary: Color get() = Color.White

    // Home/profile hero gradient: sage -> blue teal -> muted lavender.
    val HeroStart: Color get() = if (dark) Color(0xFF20362C) else Color(0xFF4D7F6C)
    val HeroMiddle: Color get() = if (dark) Color(0xFF274044) else Color(0xFF4D7881)
    val HeroEnd: Color get() = if (dark) Color(0xFF342F48) else Color(0xFF6D6B8A)

    // Premium leaderboard / CTA accents.
    val Forest: Color get() = if (dark) Color(0xFF173329) else Color(0xFF245A49)
    val ForestDeep: Color get() = if (dark) Color(0xFF10261F) else Color(0xFF16483B)
    val PremiumGold: Color get() = Color(0xFFD7B35C)
    val PremiumGoldLight: Color get() = Color(0xFFF2D98A)

    // Compatibility aliases. Legacy screens keep compiling while resolving to
    // the same application-wide botanical palette.
    val PrimaryBlue: Color get() = Primary
    val PrimaryBlueSoft: Color get() = PrimarySoft
    val SecondaryAccent: Color get() = Turquoise
    val Purple: Color get() = Lavender
}
