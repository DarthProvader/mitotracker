package mitolight.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mitolight.panel.BluetoothPanelFinder
import mitolight.panel.Panel
import mitolight.panel.PanelConnection
import mitolight.panel.RememberedPanel
import mitolight.panel.SimulatedPanelFinder

/** Whether we are driving real hardware or the in-memory stand-in (ADR 0006). */
enum class PanelSource(val label: String) {
    BLUETOOTH("Bluetooth"),
    SIMULATOR("Simulator"),
}

/**
 * Owns what belongs to the screen — the scan, and which source is being scanned — and nothing else.
 * The panel itself is held by [PanelConnection], which outlives every screen.
 */
class PanelViewModel(
    private val connection: PanelConnection,
    private val application: Application? = null,
) : ViewModel() {

    val panel = connection.panel
    val remembered = connection.remembered
    val recovering = connection.recovering

    /** Flattened by the connection, so a screen does not have to follow a panel being swapped. */
    val state = connection.state

    private val _source = MutableStateFlow(PanelSource.BLUETOOTH)
    val source: StateFlow<PanelSource> = _source.asStateFlow()

    private val _found = MutableStateFlow<List<Panel>>(emptyList())
    val found: StateFlow<List<Panel>> = _found.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _failure = MutableStateFlow<String?>(null)
    val failure: StateFlow<String?> = _failure.asStateFlow()

    private val _showingEverything = MutableStateFlow(false)
    val showingEverything: StateFlow<Boolean> = _showingEverything.asStateFlow()

    private val _scannedOnce = MutableStateFlow(false)
    val scannedOnce: StateFlow<Boolean> = _scannedOnce.asStateFlow()

    private var scanJob: Job? = null

    fun chooseSource(source: PanelSource) {
        if (_source.value == source) return

        _source.value = source
        _found.value = emptyList()
        _failure.value = null
        scanJob?.cancel()
        _scanning.value = false
    }

    /**
     * [onlyPanels] off is the escape hatch for a panel that does not announce its name: it lists
     * every Bluetooth device in range, which is unhelpful but occasionally the only way in.
     */
    fun scan(onlyPanels: Boolean = true) {
        scanJob?.cancel()
        _found.value = emptyList()
        _failure.value = null
        _scanning.value = true
        _showingEverything.value = !onlyPanels
        _scannedOnce.value = true

        val finder = when (_source.value) {
            PanelSource.BLUETOOTH -> BluetoothPanelFinder(viewModelScope, onlyPanels = onlyPanels)
            PanelSource.SIMULATOR -> SimulatedPanelFinder(viewModelScope)
        }

        scanJob = viewModelScope.launch {
            try {
                withTimeoutOrNull(SCAN_MILLIS) {
                    finder.panels.collect { panel -> _found.update { it + panel } }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _failure.value = failure.message
                    ?: application?.getString(R.string.error_scan_failed)
                    ?: "the scan could not be started"
            } finally {
                _scanning.value = false
            }
        }
    }

    fun select(panel: Panel) {
        scanJob?.cancel()
        _scanning.value = false
        // A simulated panel is not worth remembering: it will be offered again anyway.
        connection.hold(panel, remember = _source.value == PanelSource.BLUETOOTH)
    }

    fun reconnect(panel: RememberedPanel) {
        scanJob?.cancel()
        _scanning.value = false
        connection.reconnect(panel)
    }

    fun forget(id: String) = connection.forget(id)

    fun release() = connection.release()

    fun setBrightness(channel: Int, percent: Int) = onPanel { setBrightness(channel, percent) }

    fun setCountdown(minutes: Int) = onPanel { setCountdown(minutes * 60) }

    /** Second-accurate write used when resuming a paused run. */
    fun setCountdownSeconds(seconds: Int) = onPanel { setCountdown(seconds) }

    fun start() = onPanel { start() }

    fun stop() = onPanel { stop() }

    fun dismissFailure() {
        _failure.value = null
    }

    private fun onPanel(action: suspend Panel.() -> Unit) {
        val panel = connection.panel.value ?: return

        viewModelScope.launch {
            try {
                panel.action()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _failure.value = failure.message
                    ?: application?.getString(R.string.error_panel_refused)
                    ?: "the panel refused that"
            }
        }
    }

    private companion object {
        /** The panel advertises in short bursts, so a scan has to stay open for a while. */
        const val SCAN_MILLIS = 20_000L
    }
}
