package mitolight.panel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedPanelTest {

    @Test
    fun `a countdown runs down and the light switches itself off`() = runTest {
        val panel = SimulatedPanel(scope = backgroundScope)
        panel.connect()
        panel.setBrightness(CHANNEL_RED_INDEX, 100)
        panel.setCountdown(seconds = 3)
        panel.start()

        assertTrue(panel.ready().status!!.running)

        advanceTimeBy(2_100)
        assertEquals(1, panel.ready().status!!.remainingSeconds)

        advanceTimeBy(1_000)
        assertFalse(panel.ready().status!!.running)
        assertEquals(0, panel.ready().status!!.remainingSeconds)
    }

    @Test
    fun `stopping puts the countdown back where it started`() = runTest {
        val panel = SimulatedPanel(scope = backgroundScope)
        panel.connect()
        panel.setCountdown(seconds = 600)
        panel.start()

        advanceTimeBy(5_100)
        panel.stop()

        assertFalse(panel.ready().status!!.running)
        assertEquals(600, panel.ready().status!!.remainingSeconds)
    }

    @Test
    fun `a bulb refuses to go dimmer than its floor, and a six-channel panel has six channels`() = runTest {
        val bulb = SimulatedPanel(scope = backgroundScope, capability = PanelCapability.BULB)
        bulb.connect()
        bulb.setBrightness(CHANNEL_RED_INDEX, 1)
        assertEquals(3, bulb.ready().requestedBrightness[CHANNEL_RED_INDEX])

        val six = SimulatedPanel(scope = backgroundScope, capability = PanelCapability.SIX_CHANNEL)
        six.connect()
        six.setBrightness(channel = 5, percent = 40)
        assertEquals(40, six.ready().requestedBrightness[5])
    }

    @Test
    fun `capability is read off the bluetooth name`() {
        assertEquals(PanelCapability.TWO_CHANNEL, PanelCapability.ofName("ML4 AABBCCDDEEFF"))
        assertEquals(PanelCapability.SIX_CHANNEL, PanelCapability.ofName("ML5 EXPERT"))
        assertEquals(PanelCapability.BULB, PanelCapability.ofName("ML5 BULB 1234"))
        assertEquals(PanelCapability.TWO_CHANNEL, PanelCapability.ofName(null))
    }

    private fun Panel.ready(): PanelState.Ready = state.value as PanelState.Ready

    private companion object {
        const val CHANNEL_RED_INDEX = 0
    }
}
