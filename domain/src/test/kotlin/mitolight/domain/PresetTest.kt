package mitolight.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetTest {

    @Test
    fun `the default catalog is the ten built-in routines`() {
        assertEquals(
            listOf(
                "Sleep",
                "Joint support",
                "Brain & cognition",
                "Beauty & skin",
                "Recovery",
                "Vision",
                "Musculoskeletal support",
                "Feeling under the weather",
                "Inflammation support",
                "Mood & mental wellbeing",
            ),
            Preset.defaults.map(Preset::name),
        )
    }

    @Test
    fun `every default is safe to load on a two channel panel`() {
        assertEquals(10, Preset.defaults.size)
        assertEquals(10, Preset.defaults.map(Preset::id).toSet().size)

        Preset.defaults.forEach { preset ->
            assertTrue(preset.id.isNotBlank())
            assertTrue(preset.areas.isNotEmpty(), "${preset.name} needs default areas")
            assertTrue(preset.countdownMinutes in 1..20, "${preset.name} has an invalid countdown")
            assertEquals(setOf(0, 1), preset.brightnessByChannel.keys)
            assertTrue(preset.brightnessByChannel.values.all { it in 0..100 })
            assertTrue(preset.brightnessByChannel.values.any { it > 0 })
        }
    }

    @Test
    fun `every default expands to the six ML5 wavelengths`() {
        Preset.defaults.forEach { preset ->
            val mix = preset.brightnessFor(channelCount = 6)

            assertEquals((0 until 6).toSet(), mix.keys)
            assertEquals(List(3) { preset.brightnessByChannel.getValue(0) }, (0..2).map(mix::getValue))
            assertEquals(List(3) { preset.brightnessByChannel.getValue(1) }, (3..5).map(mix::getValue))
        }
    }

    @Test
    fun `an ML5 preset collapses to red and NIR averages on ML4`() {
        val preset = Preset(
            id = "six",
            name = "Six wavelengths",
            areas = listOf(Area.SCALP),
            brightnessByChannel = mapOf(
                0 to 10,
                1 to 20,
                2 to 30,
                3 to 40,
                4 to 50,
                5 to 60,
            ),
            countdownMinutes = 10,
        )

        assertEquals(mapOf(0 to 20, 1 to 50), preset.brightnessFor(channelCount = 2))
    }
}
