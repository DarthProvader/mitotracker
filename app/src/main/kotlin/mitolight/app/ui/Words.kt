package mitolight.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * How the app says numbers and channels out loud. Shared, because the same run appears on the control
 * screen, in the summary and in the log, and it would be a different session in each if they each
 * phrased it themselves.
 *
 * Unit and channel words default to English so [WordsTest] and non-Compose callers keep working;
 * Compose screens pass translated fragments from resources.
 */

/** A countdown. Minutes and seconds, never padded on the left, because a leading zero reads as a clock. */
fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * A span of light, in the coarsest unit that is still true. Dose is counted in whole minutes (ADR
 * 0002), so anything shorter would round to "0 min" — which looks like a bug rather than like the
 * fifty seconds it was.
 */
fun spell(seconds: Int, unitSeconds: String = "s", unitMinutes: String = "min"): String =
    if (seconds < 60) "$seconds $unitSeconds" else "${seconds / 60} $unitMinutes"

/** The manual settles on two channels for a 4.0 and six named wavelengths for a 5.0. */
fun channelName(
    channelCount: Int,
    channel: Int,
    red: String = "Red",
    nearInfrared: String = "Near-infrared",
): String =
    if (channelCount == 2) {
        if (channel == 0) red else nearInfrared
    } else {
        listOf("630 nm", "660 nm", "670 nm", "810 nm", "830 nm", "850 nm")[channel]
    }

/** Visible light gets a sun; near-infrared gets something that cannot be seen but is felt. */
fun channelIcon(channelCount: Int, channel: Int): ImageVector {
    val red = if (channelCount == 2) channel == 0 else channel < 3
    return if (red) Icons.Outlined.LightMode else Icons.Outlined.BlurOn
}

fun mix(
    brightness: Map<Int, Int>,
    channelCount: Int,
    redWord: String = "red",
    nearInfraredWord: String = "near-infrared",
): String =
    if (channelCount == 2) {
        "${brightness[0] ?: 0} % $redWord · ${brightness[1] ?: 0} % $nearInfraredWord"
    } else {
        brightness.entries.sortedBy { it.key }.joinToString(" · ") { "${it.value} %" }
    }

/** The same as [mix] where the row it goes in is narrow: abbreviated rather than wrapped. */
fun shortMix(
    brightness: Map<Int, Int>,
    channelCount: Int,
    redWord: String = "red",
    nirWord: String = "NIR",
): String =
    if (channelCount == 2) {
        "${brightness[0] ?: 0} % $redWord · ${brightness[1] ?: 0} % $nirWord"
    } else if (channelCount == 6) {
        val red = range((0..2).map { brightness[it] ?: 0 })
        val nearInfrared = range((3..5).map { brightness[it] ?: 0 })
        "$red % $redWord · $nearInfrared % $nirWord"
    } else {
        mix(brightness, channelCount, redWord = redWord, nearInfraredWord = nirWord)
    }

private fun range(values: List<Int>): String =
    if (values.distinct().size == 1) "${values.first()}" else "${values.min()}–${values.max()}"
