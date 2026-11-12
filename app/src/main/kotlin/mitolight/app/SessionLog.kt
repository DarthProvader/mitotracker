package mitolight.app

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mitolight.domain.Area
import mitolight.domain.DemoHistory
import mitolight.domain.Exposure
import mitolight.domain.Session

/**
 * Every session the app knows about, newest first.
 *
 * In memory for now, and seeded with generated history so that dose and streaks have something to
 * show before a single real session exists. Sessions recorded on the real panel land in the same list and are
 * indistinguishable from there on, so the screens built against generated data need no second
 * version once SQLDelight arrives (ADR 0005).
 */
class SessionLog(seedUntil: LocalDate = LocalDate.now()) {

    private val _sessions = MutableStateFlow(DemoHistory.generate(until = seedUntil))
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    fun record(session: Session) {
        _sessions.update { existing ->
            (existing + session).sortedByDescending { it.startedAtMillis }
        }
    }

    fun forget(id: String) {
        _sessions.update { existing -> existing.filterNot { it.id == id } }
    }

    /** Used by the summary after a run, where the areas can still be corrected. */
    fun attribute(id: String, areas: List<Area>) {
        _sessions.update { existing ->
            existing.map { session ->
                if (session.id != id) {
                    session
                } else {
                    // Splitting the run evenly is the only defensible guess after the fact.
                    val each = if (areas.isEmpty()) 0 else session.seconds / areas.size
                    val brightness = session.exposures.firstOrNull()?.brightnessByChannel ?: emptyMap()
                    session.copy(
                        exposures = areas.map { area -> Exposure(area, each, brightness) },
                    )
                }
            }
        }
    }
}
