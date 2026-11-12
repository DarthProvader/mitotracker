package mitolight.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class WordsTest {

    @Test
    fun `a grouped ML5 mix stays readable on a preset card`() {
        assertEquals(
            "25 % red · 75 % NIR",
            shortMix(
                mapOf(0 to 25, 1 to 25, 2 to 25, 3 to 75, 4 to 75, 5 to 75),
                channelCount = 6,
            ),
        )
    }

    @Test
    fun `an individual ML5 mix becomes a compact range`() {
        assertEquals(
            "10–30 % red · 40–60 % NIR",
            shortMix(
                mapOf(0 to 10, 1 to 20, 2 to 30, 3 to 40, 4 to 50, 5 to 60),
                channelCount = 6,
            ),
        )
    }
}
