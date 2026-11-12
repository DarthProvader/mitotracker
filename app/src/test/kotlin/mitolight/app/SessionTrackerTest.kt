package mitolight.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mitolight.domain.Area
import mitolight.domain.Session
import mitolight.panel.PanelConnection
import mitolight.panel.SimulatedPanel
import mitolight.panel.TransientPanelMemory

/**
 * The tracker against a simulated panel on a virtual clock, which is the only way to assert about
 * minutes without waiting for them. Sessions are found by panel name because the log is seeded with
 * generated history, whose timestamps are real and therefore later than anything at virtual zero.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionTrackerTest {

    @Test
    fun `a run started from the app is not unclaimed`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(2_000)

        assertFalse(fixture.tracker.unclaimed.value)
    }

    @Test
    fun `a run found already lit is unclaimed`() = runTest {
        val fixture = fixture(this)

        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(2_000)

        assertTrue(fixture.tracker.unclaimed.value)
    }

    @Test
    fun `turning around splits the run where it happened, not down the middle`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.CHEST))
        fixture.panel.setCountdown(600)
        fixture.panel.start()

        delay(420_000)
        fixture.tracker.turnedTo(listOf(Area.UPPER_BACK))
        delay(180_000)
        fixture.panel.stop()
        delay(2_000)

        val session = fixture.recorded()
        assertEquals(2, session.exposures.size)
        assertEquals(7, session.exposures.single { it.area == Area.CHEST }.minutes)
        assertEquals(3, session.exposures.single { it.area == Area.UPPER_BACK }.minutes)
    }

    @Test
    fun `two areas at once share the time they were lit for`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.KNEES, Area.FEET))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(RUN_AND_A_MOMENT_MILLIS)

        val session = fixture.recorded()
        assertEquals(5, session.exposures.single { it.area == Area.KNEES }.minutes)
        assertEquals(5, session.exposures.single { it.area == Area.FEET }.minutes)
    }

    @Test
    fun `naming the aim mid-run credits the whole run, including the minutes already spent`() =
        runTest {
            val fixture = fixture(this)

            fixture.panel.setCountdown(600)
            fixture.panel.start()
            delay(300_000)
            fixture.tracker.aimAt(listOf(Area.SHOULDERS))
            delay(RUN_AND_A_MOMENT_MILLIS - 300_000)

            val session = fixture.recorded()
            assertEquals(10, session.exposures.single().minutes)
        }

    @Test
    fun `a run nobody claims is recorded with no exposures at all`() = runTest {
        val fixture = fixture(this)

        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(RUN_AND_A_MOMENT_MILLIS)

        assertTrue(fixture.recorded().isUnattributed)
    }

    @Test
    fun `pausing turns the panel off without finishing the session`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.CHEST))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(120_000)

        fixture.tracker.pause(remainingSeconds = 480)
        fixture.panel.stop()
        delay(2_000)

        assertTrue(fixture.tracker.paused.value)
        assertEquals(480, fixture.tracker.pausedRemainingSeconds.value)
        assertFalse(fixture.panel.state.value.let { (it as mitolight.panel.PanelState.Ready).status!!.running })
        assertEquals(0, fixture.logSessions())
    }

    @Test
    fun `resume continues the same session and excludes paused wall clock time`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.CHEST))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(180_000)

        fixture.tracker.pause(remainingSeconds = 420)
        fixture.panel.stop()
        delay(300_000)

        val plan = fixture.tracker.resumePlan()!!
        assertEquals(420, plan.remainingSeconds)
        fixture.panel.setCountdown(plan.remainingSeconds)
        fixture.panel.start()
        delay(420_000)
        delay(2_000)

        val session = fixture.recorded()
        assertEquals(10, session.minutes)
        assertEquals(10, session.exposures.single { it.area == Area.CHEST }.minutes)
        assertFalse(fixture.tracker.paused.value)
    }

    @Test
    fun `ending while paused records one session of lit time only`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.FACE_AND_NECK))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(240_000)

        fixture.tracker.pause(remainingSeconds = 360)
        fixture.panel.stop()
        delay(120_000)
        fixture.tracker.endWhilePaused()
        delay(1_000)

        val session = fixture.recorded()
        assertEquals(4, session.minutes)
        assertEquals(4, session.exposures.single().minutes)
        assertFalse(fixture.tracker.paused.value)
        assertEquals(session, fixture.tracker.finished.value)
    }

    @Test
    fun `turning around before a pause still splits only the lit intervals`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.CHEST))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(180_000)
        fixture.tracker.turnedTo(listOf(Area.UPPER_BACK))
        delay(120_000)

        fixture.tracker.pause(remainingSeconds = 300)
        fixture.panel.stop()
        delay(60_000)
        fixture.tracker.endWhilePaused()
        delay(1_000)

        val session = fixture.recorded()
        assertEquals(5, session.minutes)
        assertEquals(3, session.exposures.single { it.area == Area.CHEST }.minutes)
        assertEquals(2, session.exposures.single { it.area == Area.UPPER_BACK }.minutes)
    }

    @Test
    fun `an unexpected stop while lit still ends the session`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.SHOULDERS))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(300_000)
        fixture.panel.stop()
        delay(2_000)

        assertEquals(5, fixture.recorded().minutes)
        assertFalse(fixture.tracker.paused.value)
    }

    @Test
    fun `a second pause after resume still excludes dark time`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.ABDOMEN))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(120_000)

        fixture.tracker.pause(remainingSeconds = 480)
        fixture.panel.stop()
        delay(60_000)
        fixture.panel.setCountdown(fixture.tracker.resumePlan()!!.remainingSeconds)
        fixture.panel.start()
        delay(120_000)

        fixture.tracker.pause(remainingSeconds = 360)
        fixture.panel.stop()
        delay(90_000)
        fixture.tracker.endWhilePaused()
        delay(1_000)

        assertEquals(4, fixture.recorded().minutes)
    }

    @Test
    fun `killing the tracker while paused abandons the interrupted run`() = runTest {
        val fixture = fixture(this)

        fixture.tracker.expectStart()
        fixture.tracker.aimAt(listOf(Area.SCALP))
        fixture.panel.setCountdown(600)
        fixture.panel.start()
        delay(180_000)
        fixture.tracker.pause(remainingSeconds = 420)
        fixture.panel.stop()
        delay(2_000)

        // Process death drops in-memory pause state. A replacement tracker must not invent a session
        // from a panel that is simply idle.
        val replacement = SessionTracker(
            scope = backgroundScope,
            connection = fixture.connection,
            log = fixture.log,
            now = { testScheduler.currentTime },
        )
        delay(2_000)

        assertFalse(replacement.paused.value)
        assertEquals(0, fixture.logSessions())
    }

    private class Fixture(
        val panel: SimulatedPanel,
        val tracker: SessionTracker,
        val connection: PanelConnection,
        val log: SessionLog,
    ) {
        /** The one session these tests wrote, as opposed to the generated history around it. */
        fun recorded(): Session = log.sessions.value.single { it.panelName == panel.name }

        fun logSessions(): Int = log.sessions.value.count { it.panelName == panel.name }
    }

    private suspend fun fixture(scope: TestScope): Fixture {
        val now = { scope.testScheduler.currentTime }
        val connection = PanelConnection(
            scope = scope.backgroundScope,
            memory = TransientPanelMemory(),
            reopen = { error("no panel is reopened in these tests") },
            now = now,
        )
        val log = SessionLog()
        val tracker = SessionTracker(scope.backgroundScope, connection, log, now = now)
        val panel = SimulatedPanel(scope = scope.backgroundScope)

        connection.hold(panel, remember = false)
        // Let the simulated link come up and the tracker's collector attach to it.
        delay(1_000)

        return Fixture(panel, tracker, connection, log)
    }

    private companion object {
        /**
         * Ten minutes and a little more: the panel switches itself off on the tick that reaches zero,
         * and the session is written by a collector that runs after it. Anything the panel overran by
         * is capped back to the countdown, so the slack costs the assertions nothing.
         */
        const val RUN_AND_A_MOMENT_MILLIS = 605_000L
    }
}
