package mitolight.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * The most dose one area should receive in a day, taken from Mito Light's own manual rather than
 * from us: 10–20 minutes per area per day, an area lit once or twice, and no more than 30 minutes
 * in total if the same area is lit twice.
 *
 * Two numbers instead of one because they mean different things. Passing [recommendedMinutes] is
 * the user going beyond what is advised; passing [ceilingMinutes] is the user going beyond what the
 * manufacturer will defend at all.
 */
data class Limit(
    val recommendedMinutes: Int = 20,
    val ceilingMinutes: Int = 30,
    val warnAtPercent: Int = 80,
) {
    init {
        require(recommendedMinutes in 1..ceilingMinutes) { "the recommendation cannot exceed the ceiling" }
        require(warnAtPercent in 1..100) { "the warning threshold is a percentage" }
    }

    fun status(minutes: Int): DoseStatus = when {
        minutes > ceilingMinutes -> DoseStatus.OVER_CEILING
        minutes > recommendedMinutes -> DoseStatus.OVER_RECOMMENDED
        minutes >= recommendedMinutes * warnAtPercent / 100 -> DoseStatus.NEARING
        else -> DoseStatus.WITHIN
    }

    /** What is left before the recommendation, which is what a start decision turns on. */
    fun remainingMinutes(minutes: Int): Int = (recommendedMinutes - minutes).coerceAtLeast(0)

    companion object {
        /** The manual's numbers, and the default every area starts with. */
        val FromTheManual = Limit()
    }
}

enum class DoseStatus {
    WITHIN,
    NEARING,
    OVER_RECOMMENDED,
    OVER_CEILING,
    ;

    val isOver: Boolean get() = this == OVER_RECOMMENDED || this == OVER_CEILING
}

/** Minutes an area has received, always derived from exposures and never stored (ADR 0002). */
data class Dose(val area: Area, val minutes: Int, val limit: Limit = Limit.FromTheManual) {
    val status: DoseStatus get() = limit.status(minutes)
    val remainingMinutes: Int get() = limit.remainingMinutes(minutes)

    /** Clamped, because a bar that overflows its track says less than an amber one that does not. */
    val fractionOfLimit: Float
        get() = (minutes.toFloat() / limit.recommendedMinutes).coerceIn(0f, 1f)
}

/**
 * Minutes per area on one day. Sessions with no exposures contribute nothing by construction, which
 * is exactly what ADR 0008 asks for — no special case is needed here.
 */
fun dosesOn(
    date: LocalDate,
    sessions: List<Session>,
    zone: ZoneId = ZoneId.systemDefault(),
    limit: Limit = Limit.FromTheManual,
): Map<Area, Dose> =
    sessions
        .filter { it.dateIn(zone) == date }
        .flatMap { it.exposures }
        .groupBy { it.area }
        .mapValues { (area, exposures) -> Dose(area, exposures.sumOf { it.minutes }, limit) }

fun doseOn(
    area: Area,
    date: LocalDate,
    sessions: List<Session>,
    zone: ZoneId = ZoneId.systemDefault(),
    limit: Limit = Limit.FromTheManual,
): Dose = dosesOn(date, sessions, zone, limit)[area] ?: Dose(area, 0, limit)

/** Total minutes lit per day, whether attributed or not, which is what a heat map is about. */
fun minutesByDay(
    sessions: List<Session>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Int> =
    sessions
        .groupBy { it.dateIn(zone) }
        .mapValues { (_, onThatDay) -> onThatDay.sumOf { it.minutes } }

/** Minutes per area over any span, for the statistics screen. */
fun minutesByArea(
    sessions: List<Session>,
    from: LocalDate,
    to: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<Area, Int> =
    sessions
        .filter { it.dateIn(zone) in from..to }
        .flatMap { it.exposures }
        .groupBy { it.area }
        .mapValues { (_, exposures) -> exposures.sumOf { it.minutes } }
