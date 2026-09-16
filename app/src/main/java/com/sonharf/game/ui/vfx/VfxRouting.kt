package com.sonharf.game.ui.vfx

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sonharf.game.SonHarfTheme

/** Map each event to the primary sprite that carries the moment. */
internal fun pickSpriteFor(event: VfxEvent): Int = when (event) {
    is VfxEvent.LetterDrop -> VfxAssets.TwinkleSmall
    is VfxEvent.WordAccepted -> VfxAssets.LightSweep
    is VfxEvent.CellCaptured -> VfxAssets.RingThin
    is VfxEvent.BigSiege -> VfxAssets.ShockwaveSoft
    is VfxEvent.CriticalZone -> VfxAssets.RingBright
    is VfxEvent.PerfectMove -> VfxAssets.StarSharp
    is VfxEvent.Victory -> VfxAssets.LightBurstGold
    is VfxEvent.Defeat -> VfxAssets.RadialGlow
    is VfxEvent.Streak -> VfxAssets.BurstRays
    VfxEvent.LastSeconds -> VfxAssets.Flash
    is VfxEvent.OpponentError -> VfxAssets.FlareOrange
    is VfxEvent.RoundWin -> VfxAssets.LightBurstGold
    is VfxEvent.PathStep -> VfxAssets.SquareRimGlow
    is VfxEvent.SpecialNode -> VfxAssets.RingRune
    is VfxEvent.Reward -> VfxAssets.FlareCross
    is VfxEvent.PathComplete -> VfxAssets.RingBright
    is VfxEvent.LevelUp -> VfxAssets.RingGlow
    is VfxEvent.DiamondGain -> VfxAssets.CometGold
    is VfxEvent.Combo -> VfxAssets.SheetLightning
    is VfxEvent.CastleFall -> VfxAssets.BurstSpiky
    is VfxEvent.ShieldBlock -> VfxAssets.Shield
    is VfxEvent.MapWave -> VfxAssets.RingViolet
    is VfxEvent.LetterBridge -> VfxAssets.CometGold
    is VfxEvent.LongWord -> VfxAssets.Twinkle
    is VfxEvent.LastSecondSave -> VfxAssets.LightBurstGold
    is VfxEvent.UnlockLevel -> VfxAssets.ShardIce
    is VfxEvent.HintReveal -> VfxAssets.FlareCross
}

/** Map each event to its accent tint (per-mode colours). */
internal fun pickTintFor(event: VfxEvent): Color? = when (event) {
    is VfxEvent.WordAccepted -> event.tint
    is VfxEvent.CellCaptured -> event.tint
    is VfxEvent.BigSiege -> SonHarfTheme.KusatmaPurple
    is VfxEvent.CriticalZone -> SonHarfTheme.GoldBright
    is VfxEvent.PerfectMove -> SonHarfTheme.GoldBright
    is VfxEvent.Victory -> SonHarfTheme.GoldBright
    is VfxEvent.Defeat -> SonHarfTheme.Lavender
    is VfxEvent.Streak -> SonHarfTheme.SonHarfOrange
    VfxEvent.LastSeconds -> SonHarfTheme.SonHarfOrange
    is VfxEvent.OpponentError -> Color(0xFFE85555)
    is VfxEvent.RoundWin -> SonHarfTheme.GoldBright
    is VfxEvent.PathStep -> SonHarfTheme.KelimeYoluTeal
    is VfxEvent.SpecialNode -> SonHarfTheme.KelimeYoluTeal
    is VfxEvent.Reward -> SonHarfTheme.GoldBright
    is VfxEvent.PathComplete -> SonHarfTheme.KelimeYoluTeal
    is VfxEvent.LevelUp -> SonHarfTheme.GoldBright
    is VfxEvent.DiamondGain -> SonHarfTheme.DiamondBlue
    is VfxEvent.Combo -> SonHarfTheme.KusatmaPurple
    is VfxEvent.CastleFall -> SonHarfTheme.KusatmaPurple
    is VfxEvent.ShieldBlock -> SonHarfTheme.KusatmaPurple
    is VfxEvent.MapWave -> SonHarfTheme.KusatmaPurple
    is VfxEvent.LetterBridge -> SonHarfTheme.GoldBright
    is VfxEvent.LongWord -> SonHarfTheme.SonHarfOrange
    is VfxEvent.LastSecondSave -> SonHarfTheme.GoldBright
    is VfxEvent.UnlockLevel -> SonHarfTheme.KelimeYoluTeal
    is VfxEvent.HintReveal -> SonHarfTheme.KelimeYoluTeal
    is VfxEvent.LetterDrop -> null
}

/** Map each event to a screen anchor + display size (dp). */
internal fun pickAnchorAndSize(event: VfxEvent): Pair<Offset, Float> = when (event) {
    is VfxEvent.LetterDrop -> event.anchor to 40f
    is VfxEvent.WordAccepted -> event.anchor to 120f
    is VfxEvent.CellCaptured -> (event.anchors.firstOrNull() ?: Offset.Zero) to 80f
    is VfxEvent.BigSiege -> (event.anchors.firstOrNull() ?: Offset.Zero) to 160f
    is VfxEvent.CriticalZone -> event.anchor to 96f
    is VfxEvent.PerfectMove -> event.anchor to 96f
    is VfxEvent.Victory -> event.anchor to 220f
    is VfxEvent.Defeat -> event.anchor to 220f
    is VfxEvent.Streak -> event.anchor to (if (event.n >= 10) 200f else 120f)
    VfxEvent.LastSeconds -> Offset.Zero to 96f
    is VfxEvent.OpponentError -> event.anchor to 96f
    is VfxEvent.RoundWin -> event.anchor to 200f
    is VfxEvent.PathStep -> event.anchor to 72f
    is VfxEvent.SpecialNode -> event.anchor to 96f
    is VfxEvent.Reward -> event.anchor to 128f
    is VfxEvent.PathComplete -> event.anchor to 200f
    is VfxEvent.LevelUp -> event.anchor to 140f
    is VfxEvent.DiamondGain -> event.from to 48f
    is VfxEvent.Combo -> (event.anchors.firstOrNull() ?: Offset.Zero) to 96f
    is VfxEvent.CastleFall -> event.anchor to 200f
    is VfxEvent.ShieldBlock -> event.anchor to 96f
    is VfxEvent.MapWave -> event.anchor to 200f
    is VfxEvent.LetterBridge -> event.from to 64f
    is VfxEvent.LongWord -> (event.letters.firstOrNull() ?: Offset.Zero) to 64f
    is VfxEvent.LastSecondSave -> event.anchor to 140f
    is VfxEvent.UnlockLevel -> event.anchor to 96f
    is VfxEvent.HintReveal -> event.anchor to 64f
}

/** Fire the right haptic strength for the event class. */
internal fun playHaptics(context: Context, event: VfxEvent) {
    val strength = when (event) {
        is VfxEvent.LetterDrop -> HapticStrength.Light
        is VfxEvent.WordAccepted -> HapticStrength.Light
        is VfxEvent.CellCaptured -> HapticStrength.Light
        is VfxEvent.BigSiege -> HapticStrength.Strong
        is VfxEvent.CriticalZone -> HapticStrength.Medium
        is VfxEvent.PerfectMove -> HapticStrength.Strong
        is VfxEvent.Victory -> HapticStrength.Strong
        is VfxEvent.Defeat -> HapticStrength.Light
        is VfxEvent.Streak -> HapticStrength.Medium
        VfxEvent.LastSeconds -> HapticStrength.Light
        is VfxEvent.CastleFall -> HapticStrength.Strong
        is VfxEvent.RoundWin -> HapticStrength.Medium
        is VfxEvent.LevelUp -> HapticStrength.Medium
        else -> HapticStrength.Light
    }
    Haptics.buzz(context, strength)
}
