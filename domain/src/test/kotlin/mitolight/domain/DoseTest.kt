package mitolight.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoseTest {

    private val zone: ZoneId = ZoneId.of("Europe/Prague")
    private val day = LocalDate.of(2026, 7, 29)

    @Test
    fun `minutes for one area add up across sessions`() {
        val sessions = listOf(
            session(day, 9, exposures = listOf(exposure(Area.KNEES, 10))),
            session(day, 20, exposures = listOf(exposure(Area.KNEES, 8))),
        )

        assertEquals(18, doseOn(Area.KNEES, day, sessions, zone).minutes)
    }

    @Test
    fun `an unattributed session contributes to nothing`() {
        val sessions = listOf(session(day, 21, exposures = emptyList()))

        assertTrue(dosesOn(day, sessions, zone).isEmpty())
        assertEquals(0, doseOn(Area.KNEES, day, sessions, zone).minutes)
    }

    @Test
    fun `an unattributed session is still minutes on the heat map`() {
        val sessions = listOf(session(day, 21, minutes = 25, exposures = emptyList()))

        assertEquals(mapOf(day to 25), minutesByDay(sessions, zone))
    }

    @Test
    fun `the manual's numbers decide when a dose is over`() {
        val limit = Limit.FromTheManual

        assertEquals(DoseStatus.WITHIN, limit.status(10))
        assertEquals(DoseStatus.NEARING, limit.status(16))
        assertEquals(DoseStatus.OVER_RECOMMENDED, limit.status(22))
        assertEquals(DoseStatus.OVER_CEILING, limit.status(31))
    }

    @Test
    fun `remaining minutes never go negative`() {
        assertEquals(0, Limit.FromTheManual.remainingMinutes(45))
        assertEquals(8, Limit.FromTheManual.remainingMinutes(12))
    }

    @Test
    fun `a bar never overflows its track`() {
        assertEquals(1f, Dose(Area.KNEES, 45).fractionOfLimit)
    }

    @Test
    fun `minutes are counted per area over a span`() {
        val sessions = listOf(
            session(day, 9, exposures = listOf(exposure(Area.KNEES, 10), exposure(Area.SHOULDERS, 5))),
            session(day.minusDays(1), 9, exposures = listOf(exposure(Area.KNEES, 10))),
            session(day.minusDays(9), 9, exposures = listOf(exposure(Area.KNEES, 99))),
        )

        val minutes = minutesByArea(sessions, from = day.minusDays(6), to = day, zone = zone)

        assertEquals(mapOf(Area.KNEES to 20, Area.SHOULDERS to 5), minutes)
    }

    private fun session(
        date: LocalDate,
        hour: Int,
        minutes: Int = 10,
        exposures: List<Exposure>,
    ) = Session(
        id = "$date-$hour",
        panelId = "panel",
        panelName = "ML4",
        startedAtMillis = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        seconds = minutes * 60,
        countdownSeconds = minutes * 60,
        exposures = exposures,
    )

    private fun exposure(area: Area, minutes: Int) =
        Exposure(area = area, seconds = minutes * 60, brightnessByChannel = mapOf(0 to 100, 1 to 100))
}
