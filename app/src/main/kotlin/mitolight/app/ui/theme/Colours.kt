package mitolight.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colour tokens from [DESIGN.md](../../../../../../../../DESIGN.md), which are Mito Light's own:
 * `primary` and the greys were read out of their website stylesheets and their app's `MitoColors`,
 * not invented here.
 *
 * The `bright` variants are not second brand colours. A saturated fill and readable small text are
 * different jobs, and `#F50537` on `#141414` measures 4.46:1, which fails AA for body text — so
 * fills use the base token and text uses the bright one.
 */
@Immutable
data class MitoColours(
    val primary: Color = Color(0xFFF50537),
    val primaryPressed: Color = Color(0xFFCF0000),
    val primaryBright: Color = Color(0xFFFF5A6E),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val onPrimaryDark: Color = Color(0xFF0A0A0A),
    /** Both channels belong to one red-light panel; NIR is the deeper, quieter red. */
    val channelRed: Color = Color(0xFFEF202C),
    val channelNir: Color = Color(0xFF981B32),
    val channelNirBright: Color = Color(0xFFD84A5F),
    val background: Color = Color(0xFF141414),
    val surface: Color = Color(0xFF282828),
    val surfaceHigh: Color = Color(0xFF3D3D3D),
    val onSurface: Color = Color(0xFFF2F2F2),
    val onSurfaceMuted: Color = Color(0xFFA8A8A8),
    val outline: Color = Color(0xFF3D3D3D),
    val scrim: Color = Color(0x54000000),
    val success: Color = Color(0xFF24C02B),
    val successBright: Color = Color(0xFF4FE057),
    /** The only colour allowed to contradict the user, and only on the manual's authority. */
    val warning: Color = Color(0xFFF5A524),
    val error: Color = Color(0xFFFF5A6E),
    val heatNone: Color = Color(0xFF232323),
    val heatLight: Color = Color(0xFF5C0A1B),
    val heatFull: Color = Color(0xFFA00A27),
    val heatOver: Color = Color(0xFFF50537),

    /**
     * Depth, from the Stitch drawings. A card is a translucent gradient held together by a hairline
     * rather than a flat tonal step, so the maroon light behind the interface shows through it.
     */
    val glassTop: Color = Color(0xB3282828),
    val glassBottom: Color = Color(0x661E1E1E),
    val hairline: Color = Color(0x0DFFFFFF),
    val hairlineBright: Color = Color(0x1AFFFFFF),
    /** Deepest surface, under a pill on a card. Not neutral: it carries the red cast too. */
    val deep: Color = Color(0xFF1A0909),
    /** The wall behind the phone, lit by the panel. */
    val vignette: Color = Color(0x385C0A1B),
    val glowRed: Color = Color(0x26EF202C),
    val glowAmber: Color = Color(0x1AF5A524),
    val glowNir: Color = Color(0x14981B32),
) {

    /**
     * A day in the exposure map. The ramp measures how much light that day, not whether a limit was
     * passed: a day's total spans several areas, and 40 minutes across four of them breaks nothing.
     * The steps are pitched at what a real day looks like — one 10 to 15 minute run is the middle of
     * the scale, not the top of it — because a ramp where every day lands on the same step draws a
     * flat wall instead of a habit.
     */
    fun heat(minutes: Int): Color = when {
        minutes <= 0 -> heatNone
        minutes < 10 -> heatLight
        minutes < 25 -> heatFull
        else -> heatOver
    }

    /** Text is only legible on the brightest step if it goes dark rather than lighter. */
    fun onHeat(minutes: Int): Color = if (minutes >= 25) onPrimaryDark else onSurface

    fun channel(channel: Int, channelCount: Int): Color =
        if (isRed(channel, channelCount)) channelRed else channelNir

    fun channelText(channel: Int, channelCount: Int): Color =
        if (isRed(channel, channelCount)) primaryBright else channelNirBright

    private fun isRed(channel: Int, channelCount: Int): Boolean =
        if (channelCount == 2) channel == 0 else channel < 3
}
