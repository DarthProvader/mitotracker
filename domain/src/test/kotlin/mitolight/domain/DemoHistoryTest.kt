package mitolight.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoHistoryTest {

    private val zone: ZoneId = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 7, 29)

    private val sessions = DemoHistory.generate(until = today, zone = zone)

    @Test
    fun `the same seed gives the same history`() {
        assertEquals(sessions, DemoHistory.generate(until = today, zone = zone))
    }

    @Test
    fun `a different seed gives a different one`() {
        assertTrue(sessions != DemoHistory.generate(until = today, zone = zone, seed = 1))
    }

    @Test
    fun `it is newest first`() {
        assertEquals(sessions.sortedByDescending { it.startedAtMillis }, sessions)
    }

    @Test
    fun `there are gaps, because nobody manages four unbroken months`() {
        val used = sessions.map { it.dateIn(zone) }.toSet()

        assertTrue(used.size < 120, "every single day was used, which nobody would believe")
        assertTrue(used.size > 60, "too sparse to show a habit")
    }

    @Test
    fun `there is a run long enough to be worth showing`() {
        assertTrue(streak(sessions, today, zone).longest >= 7)
    }

    @Test
    fun `at least one area goes over the recommendation, so the warning has something to show`() {
        val overdone = sessions
            .map { it.dateIn(zone) }
            .distinct()
            .flatMap { date -> dosesOn(date, sessions, zone).values }
            .filter { it.status.isOver }

        assertTrue(overdone.isNotEmpty(), "nothing ever exceeds the limit, so amber never appears")
    }

    @Test
    fun `some runs are only a lamp`() {
        assertTrue(sessions.any { it.isUnattributed })
    }

    @Test
    fun `some runs were cut short, so asked-for and actual differ`() {
        assertTrue(sessions.any { !it.ranToCompletion })
        assertTrue(sessions.any { it.ranToCompletion })
    }

    @Test
    fun `every exposure belongs to a session that was lit at least as long`() {
        assertTrue(sessions.all { session -> session.exposures.sumOf { it.seconds } <= session.seconds })
    }
}
