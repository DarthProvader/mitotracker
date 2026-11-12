package mitolight.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One continuous run of a panel. Describes what the hardware did: [seconds] is how long it was
 * actually lit, while [countdownSeconds] is only what was asked for. Twenty minutes set and five
 * minutes stood in front of it is a five minute session.
 */
data class Session(
    val id: String,
    val panelId: String,
    val panelName: String,
    val startedAtMillis: Long,
    val seconds: Int,
    val countdownSeconds: Int,
    val exposures: List<Exposure> = emptyList(),
) {
    /**
     * The light was on but the app is not claiming it reached anybody — running the panel as
     * ambient light is the honest case. Contributes to no dose and trips no limit (ADR 0008).
     */
    val isUnattributed: Boolean get() = exposures.isEmpty()

    val startedAt: Instant get() = Instant.ofEpochMilli(startedAtMillis)

    val minutes: Int get() = seconds / 60

    fun dateIn(zone: ZoneId): LocalDate = startedAt.atZone(zone).toLocalDate()

    /** True when the panel stopped itself by reaching zero rather than being cut short. */
    val ranToCompletion: Boolean
        get() = countdownSeconds > 0 && seconds >= countdownSeconds - COMPLETION_SLACK_SECONDS

    private companion object {
        /** A status arrives every two seconds, so the last one lands slightly short of zero. */
        const val COMPLETION_SLACK_SECONDS = 5
    }
}

/**
 * A claim that one [area] received light for a span of a session. Records the facts that were true
 * while it happened rather than a computed dose, so history can be recomputed if the formula ever
 * improves (ADR 0002).
 */
data class Exposure(
    val area: Area,
    val seconds: Int,
    val brightnessByChannel: Map<Int, Int>,
    val distanceCm: Int? = null,
) {
    val minutes: Int get() = seconds / 60
}
