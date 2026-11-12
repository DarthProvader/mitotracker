package mitolight.panel

import com.juul.kable.Advertisement
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CancellationException
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import mitolight.protocol.Command
import mitolight.protocol.Control
import mitolight.protocol.NOTIFY_UUID
import mitolight.protocol.PanelStatus
import mitolight.protocol.SERVICE_UUID
import mitolight.protocol.WRITE_UUID
import mitolight.protocol.brightnessPayload
import mitolight.protocol.controlPayload
import mitolight.protocol.countdownPayload
import mitolight.protocol.decode
import mitolight.protocol.encode

@OptIn(ExperimentalUuidApi::class)
private val WRITE = characteristicOf(
    service = Uuid.parse(SERVICE_UUID),
    characteristic = Uuid.parse(WRITE_UUID),
)

@OptIn(ExperimentalUuidApi::class)
private val NOTIFY = characteristicOf(
    service = Uuid.parse(SERVICE_UUID),
    characteristic = Uuid.parse(NOTIFY_UUID),
)

/**
 * Panels are recognised by the `ML` prefix Mito Light gives every unit, or by advertising the
 * vendor service. Everything else is dropped: a household is full of Bluetooth, and a list of two
 * dozen bare addresses is worse than useless when only one of them can be connected to.
 *
 * The filtering is done here rather than handed to the Bluetooth controller because a panel can
 * show up nameless on some platforms, so it is not certain every panel puts its name in the advertisement, and
 * a filter we apply ourselves can be relaxed by [onlyPanels] when that turns out to matter.
 */
class BluetoothPanelFinder(
    private val scope: CoroutineScope,
    private val onlyPanels: Boolean = true,
) : PanelFinder {

    override val panels: Flow<Panel> = flow {
        val seen = mutableSetOf<String>()
        Scanner().advertisements.collect { advertisement ->
            if ((!onlyPanels || advertisement.looksLikeAPanel()) && seen.add(advertisement.identifier)) {
                emit(BluetoothPanel.found(advertisement, scope))
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun Advertisement.looksLikeAPanel(): Boolean =
    name?.startsWith("ML", ignoreCase = true) == true || uuids.contains(Uuid.parse(SERVICE_UUID))

class BluetoothPanel private constructor(
    private val open: () -> Peripheral,
    override val id: String,
    override val name: String,
    override val capability: PanelCapability,
    private val scope: CoroutineScope,
) : Panel {

    private val _state = MutableStateFlow<PanelState>(PanelState.Disconnected)
    override val state: StateFlow<PanelState> = _state.asStateFlow()

    private val requested = mutableMapOf<Int, Int>()
    private var status: PanelStatus? = null
    private var buffer = ByteArray(0)

    /**
     * Created on connecting rather than up front, because a scan produces one of these per panel in
     * range and a peripheral that is never connected to should not be holding anything open.
     */
    private var peripheral: Peripheral? = null
    private var watcher: Job? = null
    private var listener: Job? = null
    private var poller: Job? = null

    override suspend fun connect() {
        stopWorking()
        _state.value = PanelState.Connecting

        val peripheral = open().also { this.peripheral = it }

        // Kable reports the link going down; without watching it, a panel that hangs up looks to the
        // rest of the app exactly like one that is working but quiet.
        watcher = scope.launch {
            peripheral.state.collect { link ->
                when (link) {
                    is State.Connected -> publish()
                    is State.Connecting -> _state.value = PanelState.Connecting
                    is State.Disconnected, is State.Disconnecting -> {
                        status = null
                        buffer = ByteArray(0)
                        _state.value = PanelState.Disconnected
                    }
                }
            }
        }

        listener = scope.launch {
            runCatching { peripheral.observe(NOTIFY).collect(::consume) }
        }

        try {
            peripheral.connect()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            _state.value = PanelState.Failed(failure.message ?: "could not connect")
            return
        }

        send(Command.AUTHENTICATION, BuildConfig.AUTH_PASSWORD.toByteArray(Charsets.US_ASCII))
        publish()

        poller = scope.launch {
            while (isActive) {
                runCatching { send(Command.DEVICE_STATUS) }
                delay(STATUS_INTERVAL_MILLIS)
            }
        }
    }

    override suspend fun disconnect() {
        stopWorking()
        peripheral?.let { runCatching { it.disconnect() } }
        peripheral?.let { runCatching { it.close() } }
        peripheral = null
        status = null
        buffer = ByteArray(0)
        _state.value = PanelState.Disconnected
    }

    override suspend fun refresh() = send(Command.DEVICE_STATUS)

    override suspend fun setBrightness(channel: Int, percent: Int) {
        require(channel in 0 until capability.channelCount) {
            "channel $channel does not exist on a ${capability.channelCount}-channel panel"
        }

        // Six-channel panels also accept brightness through their light modes (0x54); addressing
        // channels one at a time works for both shapes, so only that path is implemented.
        send(Command.SET_DEVICE_CHANNEL_BRIGHTNESS, brightnessPayload(channel, percent, capability.minimumBrightness))
        requested[channel] = percent
        publish()
    }

    override suspend fun setCountdown(seconds: Int) {
        send(Command.SET_DEVICE_COUNTDOWN, countdownPayload(seconds))
        refresh()
    }

    override suspend fun start() {
        send(Command.CONTROL, controlPayload(Control.START))
        refresh()
    }

    override suspend fun stop() {
        send(Command.CONTROL, controlPayload(Control.STOP))
        refresh()
    }

    private fun stopWorking() {
        poller?.cancel()
        listener?.cancel()
        watcher?.cancel()
        poller = null
        listener = null
        watcher = null
    }

    private suspend fun send(command: Command, payload: ByteArray = ByteArray(0)) {
        val peripheral = peripheral ?: error("the panel is not connected")

        peripheral.write(WRITE, encode(command, payload), WriteType.WithResponse)
        delay(WRITE_GAP_MILLIS)
    }

    private fun consume(data: ByteArray) {
        buffer = if (buffer.size > MAX_BUFFER) data else buffer + data

        val decoded = decode(buffer)
        buffer = decoded.remainder

        for (frame in decoded.frames) {
            if (frame.command == Command.DEVICE_STATUS.code) {
                PanelStatus.parse(frame.payload)?.let { status = it }
            }
        }

        if (decoded.frames.isNotEmpty()) publish()
    }

    private fun publish() {
        _state.value = PanelState.Ready(status = status, requestedBrightness = requested.toMap())
    }

    companion object {
        fun found(advertisement: Advertisement, scope: CoroutineScope) = BluetoothPanel(
            open = { Peripheral(advertisement) },
            id = advertisement.identifier,
            name = advertisement.name ?: advertisement.identifier,
            capability = PanelCapability.ofName(advertisement.name),
            scope = scope,
        )

        /** No scan required: on Android a remembered id is the panel's Bluetooth address. */
        fun remembered(panel: RememberedPanel, scope: CoroutineScope) = BluetoothPanel(
            open = { Peripheral(panel.id) },
            id = panel.id,
            name = panel.name,
            capability = panel.capability,
            scope = scope,
        )

        /** The vendor app leaves 2 ms between writes; the Python driver used 50 ms without trouble. */
        private const val WRITE_GAP_MILLIS = 50L
        private const val STATUS_INTERVAL_MILLIS = 2_000L

        /** A stream that never yields a valid frame is garbage, not a very long frame. */
        private const val MAX_BUFFER = 256
    }
}
