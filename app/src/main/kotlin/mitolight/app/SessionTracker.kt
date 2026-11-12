package mitolight.app

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mitolight.domain.Area
import mitolight.domain.Exposure
import mitolight.domain.Session
import mitolight.panel.PanelConnection
import mitolight.panel.PanelState

/** What a run is aimed at, chosen before the light goes on. No areas means nobody is claiming it. */
data class Aim(val areas: List<Area>, val brightnessByChannel: Map<Int, Int> = emptyMap())

/**
 * Turns what the panel did into sessions.
 *
 * Application-scoped for the same reason the connection is (ADR 0007): the run being watched is
 * precisely the ten minutes during which somebody locks the phone, and a screen-scoped watcher would
 * miss the end of every session it was supposed to record.
 *
 * Pause is an app-level state: the protocol only starts and stops the LEDs, so stopping
 * for a pause must not look like a finished session. Dark wall-clock time is excluded from dose by
 * recording lit legs and pause markers separately. The paused snapshot lives in this process only —
 * if the app is killed while paused, the interrupted run is abandoned rather than guessed at.
 */
class SessionTracker(
    scope: CoroutineScope,
    private val connection: PanelConnection,
    private val log: SessionLog,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _aim = MutableStateFlow<Aim?>(null)
    val aim: StateFlow<Aim?> = _aim.asStateFlow()

    /** The session just written, which the summary asks the user to confirm or correct. */
    private val _finished = MutableStateFlow<Session?>(null)
    val finished: StateFlow<Session?> = _finished.asStateFlow()

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** Remaining planned seconds frozen at the pause; null while the panel is driving the clock. */
    private val _pausedRemainingSeconds = MutableStateFlow<Int?>(null)
    val pausedRemainingSeconds: StateFlow<Int?> = _pausedRemainingSeconds.asStateFlow()

    private var startedAtMillis: Long? = null
    private var countdownSeconds = 0

    /**
     * Where the light was pointed and from when. Lit legs carry areas; a pause inserts a dark marker
     * so wall-clock time with the LEDs off is never credited. A turn seven minutes into ten is seven
     * and three of lit time, not five and five.
     */
    private val legs = mutableListOf<Leg>()

    /** Set when the app is the one starting the panel, so a self-started run is not called unclaimed. */
    private var expecting = false

    /** Set when the app is about to stop the panel for a pause rather than to end the session. */
    private var expectingPause = false

    /** True when the panel is lit and we have no idea what at — somebody started it themselves. */
    private val _unclaimed = MutableStateFlow(false)
    val unclaimed: StateFlow<Boolean> = _unclaimed.asStateFlow()

    init {
        scope.launch { watch() }
    }

    /** Called as the app asks the panel to start, before the panel has said that it did. */
    fun expectStart() {
        expecting = true
    }

    /** What the run as a whole is aimed at: replaces everything, including a turn already recorded. */
    fun aimAt(areas: List<Area>, brightness: Map<Int, Int> = emptyMap()) {
        _aim.value = Aim(areas, brightness)
        _unclaimed.value = false
        legs.clear()
        startedAtMillis?.let { legs += Leg(areas, it, lit = true) }
    }

    /** Mid-session: from now on the light is on something else. The minutes so far stay where they are. */
    fun turnedTo(areas: List<Area>) {
        val begun = startedAtMillis
        if (begun == null) {
            aimAt(areas)
            return
        }

        _aim.value = Aim(areas, _aim.value?.brightnessByChannel ?: emptyMap())
        _unclaimed.value = false
        if (legs.none { it.lit }) legs += Leg(emptyList(), begun, lit = true)
        if (!_paused.value) legs += Leg(areas, now(), lit = true)
    }

    /**
     * The panel is about to be stopped without ending the session. Captures the remaining planned
     * time so Resume can put the same countdown back even though stop resets what the panel reports.
     */
    fun pause(remainingSeconds: Int) {
        if (startedAtMillis == null || _paused.value) return

        expectingPause = true
        _pausedRemainingSeconds.value = remainingSeconds.coerceAtLeast(0)
        _paused.value = true
        legs += Leg(emptyList(), now(), lit = false)
    }

    /**
     * What Resume must write to the panel before starting it again. Leaves [paused] true until the
     * panel reports running, so a failed start does not look like an ended session.
     */
    fun resumePlan(): ResumePlan? {
        if (!_paused.value) return null

        expecting = true
        expectingPause = false
        return ResumePlan(
            remainingSeconds = _pausedRemainingSeconds.value ?: 0,
            brightnessByChannel = _aim.value?.brightnessByChannel.orEmpty(),
        )
    }

    /** End the run while the LEDs are already off. Counts only the lit intervals. */
    fun endWhilePaused() {
        if (!_paused.value) return

        val begun = startedAtMillis ?: return
        val brightness = _aim.value?.brightnessByChannel.orEmpty()
            .ifEmpty { (connection.state.value as? PanelState.Ready)?.requestedBrightness.orEmpty() }
        val endedAt = legs.lastOrNull { !it.lit }?.fromMillis ?: now()

        clearPauseFlags()
        startedAtMillis = null
        _unclaimed.value = false
        record(begun, brightness, endedAtMillis = endedAt)
    }

    fun clearAim() {
        _aim.value = null
        legs.clear()
    }

    fun dismissSummary() {
        _finished.value = null
    }

    private suspend fun watch() {
        connection.state.collect { state ->
            val status = (state as? PanelState.Ready)?.status ?: return@collect
            val brightness = state.requestedBrightness

            if (status.running) {
                if (_paused.value) {
                    // Same session, light back on: close the pause gap and continue the aim.
                    _paused.value = false
                    _pausedRemainingSeconds.value = null
                    expectingPause = false
                    expecting = false
                    legs += Leg(_aim.value?.areas ?: emptyList(), now(), lit = true)
                    return@collect
                }

                if (startedAtMillis == null) {
                    // The panel knows how far in it is, which is what makes joining a run already
                    // in progress work at all: countdown minus remaining is elapsed (ADR 0003).
                    val elapsed = (status.countdownSeconds - status.remainingSeconds).coerceAtLeast(0)
                    val begun = now() - elapsed * 1_000L
                    startedAtMillis = begun
                    countdownSeconds = status.countdownSeconds
                    // Only a run the app did not start is unclaimed. Starting from this app with no
                    // area named is not a mystery to be solved mid-run: the summary asks at the end.
                    _unclaimed.value = !expecting && _aim.value == null
                    expecting = false
                    legs.clear()
                    legs += Leg(_aim.value?.areas ?: emptyList(), begun, lit = true)
                }
                return@collect
            }

            // A stop the app asked for as a pause must not become a finished session.
            if (expectingPause || _paused.value) {
                expectingPause = false
                return@collect
            }

            // A dropped link is not a stop, so only a panel that says it is idle ends a session.
            val begun = startedAtMillis ?: return@collect
            startedAtMillis = null
            _unclaimed.value = false
            record(begun, brightness)
        }
    }

    private fun clearPauseFlags() {
        expectingPause = false
        _paused.value = false
        _pausedRemainingSeconds.value = null
    }

    private fun record(begunAtMillis: Long, brightness: Map<Int, Int>, endedAtMillis: Long = now()) {
        val panel = connection.panel.value
        val elapsedSeconds = litSeconds(begunAtMillis, endedAtMillis)

        // Capped at what was asked for, because the gap between the panel stopping itself and us
        // noticing is ours, not the user's. Anything under half a minute was a misfire.
        val seconds = if (countdownSeconds > 0) minOf(elapsedSeconds, countdownSeconds) else elapsedSeconds
        if (seconds < MINIMUM_SECONDS) {
            _aim.value = null
            legs.clear()
            return
        }

        val aimed = _aim.value
        val mix = aimed?.brightnessByChannel?.takeIf { it.isNotEmpty() } ?: brightness

        val session = Session(
            id = UUID.randomUUID().toString(),
            panelId = panel?.id ?: "unknown",
            panelName = panel?.name ?: "unknown panel",
            startedAtMillis = begunAtMillis,
            seconds = seconds,
            countdownSeconds = countdownSeconds,
            exposures = exposures(begunAtMillis, endedAtMillis, mix),
        )

        log.record(session)
        _finished.value = session
        _aim.value = null
        legs.clear()
    }

    /** Wall-clock intervals where the LEDs were on. Pause markers cut those intervals short. */
    private fun litSeconds(begunAtMillis: Long, endedAtMillis: Long): Int {
        var total = 0
        legs.forEachIndexed { index, leg ->
            if (!leg.lit) return@forEachIndexed

            val from = leg.fromMillis.coerceIn(begunAtMillis, endedAtMillis)
            val until = (legs.getOrNull(index + 1)?.fromMillis ?: endedAtMillis)
                .coerceIn(begunAtMillis, endedAtMillis)
            total += ((until - from) / 1_000L).toInt().coerceAtLeast(0)
        }
        return total
    }

    /**
     * The lit legs turned into exposures, each lasting until the next leg begins. Within a lit leg
     * the time is split evenly, because two areas facing one panel at once really do share it.
     */
    private fun exposures(
        begunAtMillis: Long,
        endedAtMillis: Long,
        mix: Map<Int, Int>,
    ): List<Exposure> {
        val secondsByArea = linkedMapOf<Area, Int>()

        legs.forEachIndexed { index, leg ->
            if (!leg.lit || leg.areas.isEmpty()) return@forEachIndexed

            val from = leg.fromMillis.coerceIn(begunAtMillis, endedAtMillis)
            val until = (legs.getOrNull(index + 1)?.fromMillis ?: endedAtMillis)
                .coerceIn(begunAtMillis, endedAtMillis)
            val each = (((until - from) / 1_000L).toInt().coerceAtLeast(0)) / leg.areas.size

            leg.areas.forEach { area ->
                secondsByArea[area] = (secondsByArea[area] ?: 0) + each
            }
        }

        return secondsByArea
            .filterValues { it > 0 }
            .map { (area, spent) -> Exposure(area, spent, mix) }
    }

    private data class Leg(val areas: List<Area>, val fromMillis: Long, val lit: Boolean)

    data class ResumePlan(val remainingSeconds: Int, val brightnessByChannel: Map<Int, Int>)

    private companion object {
        const val MINIMUM_SECONDS = 30
    }
}
