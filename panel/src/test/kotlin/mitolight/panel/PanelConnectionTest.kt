package mitolight.panel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

/**
 * The connection does its work in coroutines of its own, so these tests hand control back with
 * [delay] rather than `advanceUntilIdle`, which does not run what a background scope has queued.
 * Virtual time makes the waits free.
 */
class PanelConnectionTest {

    @Test
    fun `taking a panel connects it and remembers it for next time`() = runTest {
        val memory = TransientPanelMemory()
        val connection = connection(backgroundScope, memory)

        connection.hold(SimulatedPanel(scope = backgroundScope))
        delay(2_000)

        assertTrue(connection.state.value is PanelState.Ready)
        assertEquals(listOf("simulated-ml4"), memory.remembered().map(RememberedPanel::id))
    }

    @Test
    fun `a link the panel drops on its own is brought back`() = runTest {
        val connection = connection(backgroundScope)
        val panel = SimulatedPanel(scope = backgroundScope)

        connection.hold(panel)
        delay(2_000)

        // What the hardware does when the phone sleeps, or when it simply feels like it.
        panel.disconnect()
        delay(100)
        assertEquals(PanelState.Disconnected, connection.state.value)

        delay(5_000)
        assertTrue(connection.state.value is PanelState.Ready)
    }

    @Test
    fun `a panel let go of stays let go of`() = runTest {
        val connection = connection(backgroundScope)

        connection.hold(SimulatedPanel(scope = backgroundScope))
        delay(2_000)
        connection.release()
        delay(30_000)

        assertNull(connection.panel.value)
        assertEquals(PanelState.Disconnected, connection.state.value)
    }

    @Test
    fun `a remembered panel is reopened by address, without scanning`() = runTest {
        val memory = TransientPanelMemory()
        val opened = mutableListOf<String>()
        val connection = PanelConnection(
            scope = backgroundScope,
            memory = memory,
            reopen = { remembered ->
                opened += remembered.id
                SimulatedPanel(scope = backgroundScope, id = remembered.id, name = remembered.name)
            },
            now = { 1_000L },
        )

        connection.hold(SimulatedPanel(scope = backgroundScope))
        delay(2_000)
        connection.release()
        delay(1_000)

        connection.reconnect(memory.remembered().single())
        delay(2_000)

        assertEquals(listOf("simulated-ml4"), opened)
        assertTrue(connection.state.value is PanelState.Ready)
    }

    @Test
    fun `a panel that never answers is given up on rather than chased all day`() = runTest {
        val connection = connection(backgroundScope)

        connection.hold(UnreachablePanel())
        delay(5.minutes)

        assertTrue(connection.state.value is PanelState.Failed)
        assertFalse(connection.recovering.value)
    }

    @Test
    fun `forgetting a panel takes it off the list`() = runTest {
        val memory = TransientPanelMemory()
        val connection = connection(backgroundScope, memory)

        connection.hold(SimulatedPanel(scope = backgroundScope))
        delay(2_000)
        assertEquals(1, connection.remembered.value.size)

        connection.forget("simulated-ml4")
        delay(100)

        assertTrue(connection.remembered.value.isEmpty())
        assertTrue(memory.remembered().isEmpty())
    }

    private val Int.minutes: Long get() = this * 60_000L

    private fun connection(
        scope: CoroutineScope,
        memory: PanelMemory = TransientPanelMemory(),
    ) = PanelConnection(
        scope = scope,
        memory = memory,
        reopen = { SimulatedPanel(scope = scope, id = it.id, name = it.name) },
        now = { 1_000L },
    )
}

/** A panel that is switched off at the wall: it can be asked, and it never answers. */
private class UnreachablePanel : Panel {
    override val id = "unreachable"
    override val name = "ML4 UNREACHABLE"
    override val capability = PanelCapability.TWO_CHANNEL

    override val state = MutableStateFlow<PanelState>(PanelState.Disconnected)

    override suspend fun connect() {
        state.value = PanelState.Failed("no answer")
    }

    override suspend fun disconnect() {
        state.value = PanelState.Disconnected
    }

    override suspend fun refresh() = Unit

    override suspend fun setBrightness(channel: Int, percent: Int) = Unit

    override suspend fun setCountdown(seconds: Int) = Unit

    override suspend fun start() = Unit

    override suspend fun stop() = Unit
}
