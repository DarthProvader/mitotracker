package mitolight.panel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mitolight.protocol.PanelStatus

/**
 * A panel that exists only in memory: it takes the same calls, counts its countdown down in real
 * time and switches itself off at zero. Enough to build and demonstrate the app while the
 * hardware is out of reach or busy with another client (ADR 0006).
 */
class SimulatedPanel(
    private val scope: CoroutineScope,
    override val id: String = "simulated-ml4",
    override val name: String = "ML4 SIMULATOR",
    override val capability: PanelCapability = PanelCapability.TWO_CHANNEL,
) : Panel {

    private val _state = MutableStateFlow<PanelState>(PanelState.Disconnected)
    override val state: StateFlow<PanelState> = _state.asStateFlow()

    private val brightness = mutableMapOf<Int, Int>()
    private var running = false
    private var countdownSeconds = 0
    private var remainingSeconds = 0
    private var ticker: Job? = null

    override suspend fun connect() {
        _state.value = PanelState.Connecting
        delay(CONNECT_MILLIS)
        publish()
    }

    override suspend fun disconnect() {
        ticker?.cancel()
        ticker = null
        running = false
        _state.value = PanelState.Disconnected
    }

    override suspend fun refresh() = publish()

    override suspend fun setBrightness(channel: Int, percent: Int) {
        require(channel in 0 until capability.channelCount) {
            "channel $channel does not exist on a ${capability.channelCount}-channel panel"
        }

        brightness[channel] = if (percent == 0) 0 else maxOf(percent, capability.minimumBrightness)
        publish()
    }

    override suspend fun setCountdown(seconds: Int) {
        countdownSeconds = seconds
        if (!running) remainingSeconds = seconds
        publish()
    }

    override suspend fun start() {
        running = true
        remainingSeconds = countdownSeconds
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive && running) {
                delay(TICK_MILLIS)
                if (countdownSeconds > 0) {
                    remainingSeconds--
                    if (remainingSeconds <= 0) {
                        remainingSeconds = 0
                        running = false
                    }
                }
                publish()
            }
        }
        publish()
    }

    override suspend fun stop() {
        running = false
        remainingSeconds = countdownSeconds
        ticker?.cancel()
        ticker = null
        publish()
    }

    private fun publish() {
        _state.value = PanelState.Ready(
            status = PanelStatus(running, countdownSeconds, remainingSeconds),
            requestedBrightness = brightness.toMap(),
        )
    }

    private companion object {
        const val CONNECT_MILLIS = 400L
        const val TICK_MILLIS = 1_000L
    }
}

/** Offers one panel of each shape, so the six-channel screens can be worked on too. */
class SimulatedPanelFinder(private val scope: CoroutineScope) : PanelFinder {
    override val panels: Flow<Panel> = flow {
        delay(300)
        emit(SimulatedPanel(scope = scope))
        delay(600)
        emit(
            SimulatedPanel(
                scope = scope,
                id = "simulated-ml5",
                name = "ML5 SIMULATOR",
                capability = PanelCapability.SIX_CHANNEL,
            ),
        )
    }
}
