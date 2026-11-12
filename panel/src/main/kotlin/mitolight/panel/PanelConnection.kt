package mitolight.panel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the one panel the app is driving, for as long as the app is running.
 *
 * A Bluetooth link outlives any single screen: a ten minute exposure is precisely the situation in
 * which somebody locks the phone and reads something else. So this is owned by the application and
 * not by a screen, and it is also what puts a dropped link back — the panel drops the connection on
 * its own, and until now nothing noticed (ADR 0007).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PanelConnection(
    private val scope: CoroutineScope,
    private val memory: PanelMemory,
    private val reopen: (RememberedPanel) -> Panel,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _panel = MutableStateFlow<Panel?>(null)
    val panel: StateFlow<Panel?> = _panel.asStateFlow()

    private val _remembered = MutableStateFlow<List<RememberedPanel>>(emptyList())
    val remembered: StateFlow<List<RememberedPanel>> = _remembered.asStateFlow()

    /** True while the link is down and we are still trying to get it back. */
    private val _recovering = MutableStateFlow(false)
    val recovering: StateFlow<Boolean> = _recovering.asStateFlow()

    /** The held panel's state, flattened so callers do not have to follow the swap themselves. */
    val state: StateFlow<PanelState> = _panel
        .flatMapLatest { it?.state ?: flowOf(PanelState.Disconnected) }
        .stateIn(scope, SharingStarted.Eagerly, PanelState.Disconnected)

    private var holder: Job? = null

    init {
        scope.launch { _remembered.value = memory.remembered() }
    }

    /** Takes the panel, keeps it connected, and remembers it unless it is only a simulation. */
    fun hold(panel: Panel, remember: Boolean = true) {
        val previous = _panel.value
        holder?.cancel()
        _panel.value = panel

        holder = scope.launch {
            if (previous != null && previous !== panel) runCatching { previous.disconnect() }
            if (remember) remember(panel)
            keepConnected(panel)
        }
    }

    /** Reaches a remembered panel directly, with no scan: its id is its Bluetooth address. */
    fun reconnect(panel: RememberedPanel) = hold(reopen(panel))

    fun release() {
        val panel = _panel.value ?: return

        holder?.cancel()
        holder = null
        _panel.value = null
        _recovering.value = false
        scope.launch { runCatching { panel.disconnect() } }
    }

    fun forget(id: String) {
        scope.launch {
            memory.forget(id)
            _remembered.value = memory.remembered()
        }
    }

    /**
     * Reconnects with a widening delay, then stops: a panel that has been switched off at the wall
     * is not going to answer, and scanning at it forever would cost the battery all day. The user
     * is left with an idle screen and a panel to tap, which is honest about what happened.
     */
    private suspend fun keepConnected(panel: Panel) {
        var attempt = 0

        while (true) {
            panel.connect()

            if (panel.state.value is PanelState.Ready) {
                attempt = 0
                _recovering.value = false
                panel.state.first { it !is PanelState.Ready }
            }

            attempt++
            if (attempt > ATTEMPTS_BEFORE_GIVING_UP) {
                _recovering.value = false
                return
            }

            _recovering.value = true
            delay(RETRY_DELAYS_MILLIS[minOf(attempt - 1, RETRY_DELAYS_MILLIS.lastIndex)])
        }
    }

    private suspend fun remember(panel: Panel) {
        memory.remember(
            RememberedPanel(
                id = panel.id,
                name = panel.name,
                capability = panel.capability,
                lastConnectedAtMillis = now(),
            ),
        )
        _remembered.value = memory.remembered()
    }

    private companion object {
        val RETRY_DELAYS_MILLIS = longArrayOf(1_000, 2_000, 5_000, 10_000, 15_000)
        const val ATTEMPTS_BEFORE_GIVING_UP = 8
    }
}
