package mitolight.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Consecutive days of use, which is the number that matters: red light therapy does nothing in one
 * heroic hour and something in ten unremarkable minutes a day, and nothing in the vendor's app helps
 * anybody see whether they are actually doing it.
 *
 * Only attributed sessions count. Leaving the panel on as ambient light is not therapy, so it does
 * not keep a streak alive — a streak that could be maintained by lighting a room would be worthless
 * as encouragement.
 */
data class Streak(val current: Int, val longest: Int)

fun streak(
    sessions: List<Session>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Streak {
    val days = sessions
        .filterNot { it.isUnattributed }
        .map { it.dateIn(zone) }
        .toSortedSet()

    if (days.isEmpty()) return Streak(current = 0, longest = 0)

    return Streak(current = currentRun(days, today), longest = longestRun(days))
}

/**
 * Counts back from today, and tolerates today being empty: at nine in the morning a run of thirty
 * days is not broken, it just has not been continued yet. Yesterday being empty does break it.
 */
private fun currentRun(days: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in days) today else today.minusDays(1)
    var run = 0

    while (day in days) {
        run++
        day = day.minusDays(1)
    }

    return run
}

private fun longestRun(days: Iterable<LocalDate>): Int {
    var longest = 0
    var run = 0
    var previous: LocalDate? = null

    for (day in days) {
        run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }

    return longest
}
