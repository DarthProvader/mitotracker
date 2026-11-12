package mitolight.domain

import kotlin.math.roundToInt

/**
 * A named routine: which areas, how bright, and for how long. Starting a session from a preset is
 * how the app learns what is being lit, which is what makes dose possible at all.
 *
 * A preset is not a panel light mode. Six-channel panels have their own built-in modes and those
 * live in the hardware.
 *
 * [areas] may be empty, which is how a run that is only ambient light gets started without being
 * filed against anybody's shoulders (ADR 0008).
 */
data class Preset(
    val id: String,
    val name: String,
    val areas: List<Area>,
    val brightnessByChannel: Map<Int, Int>,
    val countdownMinutes: Int,
) {
    val isUnattributed: Boolean get() = areas.isEmpty()

    /**
     * What the connected panel is told. Two-channel recipes expand over ML5's wavelength groups;
     * exact ML5 recipes collapse to group averages if they are ever loaded on ML4.
     */
    fun brightnessFor(channelCount: Int): Map<Int, Int> = when {
        channelCount == brightnessByChannel.size -> brightnessByChannel
        brightnessByChannel.size == 2 -> {
            val red = brightnessByChannel[0] ?: 0
            val nearInfrared = brightnessByChannel[1] ?: 0
            (0 until channelCount).associateWith { channel ->
                if (isRedChannel(channel, channelCount)) red else nearInfrared
            }
        }

        brightnessByChannel.size == 6 && channelCount == 2 -> mapOf(
            0 to (0..2).map { brightnessByChannel[it] ?: 0 }.average().roundToInt(),
            1 to (3..5).map { brightnessByChannel[it] ?: 0 }.average().roundToInt(),
        )

        else -> (0 until channelCount).associateWith { brightnessByChannel[it] ?: 0 }
    }

    companion object {
        /** The three peaks below 700 nm are the visible red ones on a six-channel panel. */
        fun isRedChannel(channel: Int, channelCount: Int): Boolean =
            if (channelCount == 2) channel == 0 else channel < 3

        /**
         * The ten built-in routines. These are working defaults, not clinical protocols, and
         * their exact output has not been reviewed against a clinical source. Stable IDs matter because names and translations may change.
         *
         * Until ML5 presets preserve all six wavelengths, [brightnessFor] deliberately expands each
         * Red/NIR pair over the panel's three visible and three near-infrared channels.
         */
        val defaults = listOf(
            Preset(
                id = "sleep",
                name = "Sleep",
                areas = listOf(Area.SCALP, Area.FACE_AND_NECK),
                brightnessByChannel = mapOf(0 to 25, 1 to 75),
                countdownMinutes = 10,
            ),
            Preset(
                id = "joint-support",
                name = "Joint support",
                areas = listOf(Area.KNEES, Area.HIPS),
                brightnessByChannel = mapOf(0 to 50, 1 to 100),
                countdownMinutes = 10,
            ),
            Preset(
                id = "brain-cognition",
                name = "Brain & cognition",
                areas = listOf(Area.SCALP),
                brightnessByChannel = mapOf(0 to 25, 1 to 75),
                countdownMinutes = 10,
            ),
            Preset(
                id = "beauty-skin",
                name = "Beauty & skin",
                areas = listOf(Area.FACE_AND_NECK),
                brightnessByChannel = mapOf(0 to 100, 1 to 100),
                countdownMinutes = 10,
            ),
            Preset(
                id = "recovery",
                name = "Recovery",
                areas = listOf(Area.SHOULDERS, Area.THIGHS),
                brightnessByChannel = mapOf(0 to 50, 1 to 100),
                countdownMinutes = 10,
            ),
            Preset(
                id = "vision",
                name = "Vision",
                areas = listOf(Area.FACE_AND_NECK),
                brightnessByChannel = mapOf(0 to 25, 1 to 0),
                countdownMinutes = 5,
            ),
            Preset(
                id = "musculoskeletal-support",
                name = "Musculoskeletal support",
                areas = listOf(Area.SHOULDERS, Area.UPPER_BACK, Area.LOWER_BACK),
                brightnessByChannel = mapOf(0 to 50, 1 to 100),
                countdownMinutes = 10,
            ),
            Preset(
                id = "feeling-under-the-weather",
                name = "Feeling under the weather",
                areas = listOf(Area.CHEST, Area.ABDOMEN),
                brightnessByChannel = mapOf(0 to 50, 1 to 50),
                countdownMinutes = 10,
            ),
            Preset(
                id = "inflammation-support",
                name = "Inflammation support",
                areas = listOf(Area.ABDOMEN),
                brightnessByChannel = mapOf(0 to 50, 1 to 100),
                countdownMinutes = 10,
            ),
            Preset(
                id = "mood-wellbeing",
                name = "Mood & mental wellbeing",
                areas = listOf(Area.SCALP, Area.FACE_AND_NECK),
                brightnessByChannel = mapOf(0 to 25, 1 to 75),
                countdownMinutes = 10,
            ),
        )

        /** A dark, never-configured panel still needs Start to produce light on the first tap. */
        val firstRun: Preset = defaults.single { it.id == "beauty-skin" }
    }
}
