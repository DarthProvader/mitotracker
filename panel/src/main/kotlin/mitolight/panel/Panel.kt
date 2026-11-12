package mitolight.panel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mitolight.protocol.PanelStatus

/**
 * Everything a panel can be asked to do. No layer above this one touches Bluetooth, which is
 * what makes the simulator a faithful stand-in rather than a demo mode (ADR 0006).
 */
interface Panel {
    val id: String
    val name: String
    val capability: PanelCapability
    val state: StateFlow<PanelState>

    suspend fun connect()

    suspend fun disconnect()

    /** Asks the panel what it is doing. The answer arrives through [state], not as a return value. */
    suspend fun refresh()

    suspend fun setBrightness(channel: Int, percent: Int)

    suspend fun setCountdown(seconds: Int)

    suspend fun start()

    suspend fun stop()
}

sealed interface PanelState {

    data object Disconnected : PanelState

    data object Connecting : PanelState

    /**
     * Connected and authenticated. [status] stays null until the panel answers its first status
     * query. [requestedBrightness] is what we asked for rather than what the panel confirms,
     * because it does not report brightness back in any form we have decoded.
     */
    data class Ready(
        val status: PanelStatus?,
        val requestedBrightness: Map<Int, Int>,
    ) : PanelState

    data class Failed(val reason: String) : PanelState
}

/** Emits each panel once, as it is first seen. Scanning runs only while the flow is collected. */
interface PanelFinder {
    val panels: Flow<Panel>
}
