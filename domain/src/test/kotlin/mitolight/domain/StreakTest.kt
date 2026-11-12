package mitolight.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {

    private val zone: ZoneId = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun `consecutive days count`() {
        val sessions = (0..4).map { on(today.minusDays(it.toLong())) }

        assertEquals(5, streak(sessions, today, zone).current)
    }

    @Test
    fun `a morning with nothing yet does not break yesterday's run`() {
        val sessions = (1..3).map { on(today.minusDays(it.toLong())) }

        assertEquals(3, streak(sessions, today, zone).current)
    }

    @Test
    fun `a missed yesterday does break it`() {
        val sessions = (2..6).map { on(today.minusDays(it.toLong())) }

        assertEquals(0, streak(sessions, today, zone).current)
    }

    @Test
    fun `the longest run survives later gaps`() {
        val sessions = (10..19).map { on(today.minusDays(it.toLong())) } +
            listOf(on(today), on(today.minusDays(1)))

        val result = streak(sessions, today, zone)

        assertEquals(2, result.current)
        assertEquals(10, result.longest)
    }

    @Test
    fun `lighting a room does not keep a streak alive`() {
        val sessions = listOf(
            on(today.minusDays(2)),
            on(today.minusDays(1), attributed = false),
            on(today, attributed = false),
        )

        assertEquals(0, streak(sessions, today, zone).current)
        assertEquals(1, streak(sessions, today, zone).longest)
    }

    @Test
    fun `no history is no streak`() {
        assertEquals(Streak(0, 0), streak(emptyList(), today, zone))
    }

    @Test
    fun `two sessions on one day are still one day`() {
        val sessions = listOf(on(today), on(today, hour = 20), on(today.minusDays(1)))

        assertEquals(2, streak(sessions, today, zone).current)
    }

    private fun on(date: LocalDate, hour: Int = 8, attributed: Boolean = true) = Session(
        id = "$date-$hour",
        panelId = "panel",
        panelName = "ML4",
        startedAtMillis = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        seconds = 600,
        countdownSeconds = 600,
        exposures = if (attributed) {
            listOf(Exposure(Area.KNEES, 600, mapOf(0 to 100, 1 to 100)))
        } else {
            emptyList()
        },
    )
}
