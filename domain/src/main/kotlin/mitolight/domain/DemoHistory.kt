package mitolight.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Months of plausible history, so that the two screens built on history — dose and streaks — are
 * worth looking at before a single real session has been recorded.
 *
 * Deterministic for a given seed, so screenshots and tests stay the same between runs.
 *
 * The shape is deliberately imperfect, because nobody uses a panel every single day for four
 * months and a history with no gaps would not exercise the screens honestly. So there are holidays, a fortnight of enthusiasm, evenings that got away, one week
 * where the lower back was overdone, and a few evenings when the panel was just a warm lamp.
 */
object DemoHistory {

    fun generate(
        until: LocalDate,
        days: Int = 120,
        panelId: String = "AA:BB:CC:DD:EE:FF",
        panelName: String = "ML4 DEMO",
        zone: ZoneId = ZoneId.systemDefault(),
        seed: Int = 20260729,
    ): List<Session> {
        val random = Random(seed)
        val sessions = mutableListOf<Session>()

        // Two stretches of nothing: a week away, and a few days where it simply did not happen.
        val away = daysFrom(until.minusDays(38L), until.minusDays(31L))
        val lapse = daysFrom(until.minusDays(17L), until.minusDays(15L))
        val overdone = daysFrom(until.minusDays(9L), until.minusDays(5L))

        // The last week and a half always happened, so the current streak is never cut short by a
        // random draw that happened to skip yesterday.
        val keen = daysFrom(until.minusDays(9L), until)

        for (offset in days downTo 0) {
            val date = until.minusDays(offset.toLong())
            if (date in away || date in lapse) continue

            // Early on the habit is patchy; it firms up over time, which is what a streak is for.
            val settled = offset < days * 2 / 3
            val chance = if (settled) 0.88 else 0.55
            if (date !in keen && random.nextDouble() > chance) continue

            val overdoingIt = date in overdone

            sessions += therapySession(date, random, panelId, panelName, zone, overdoingIt)

            // A second run later the same day is what pushes an area towards its ceiling.
            if (overdoingIt || random.nextDouble() < 0.12) {
                sessions += therapySession(date, random, panelId, panelName, zone, overdoingIt, evening = true)
            }

            // The panel as a lamp: recorded, attributed to nobody, counting towards no dose.
            if (random.nextDouble() < 0.06) {
                sessions += ambientSession(date, random, panelId, panelName, zone)
            }
        }

        return sessions.sortedByDescending { it.startedAtMillis }
    }

    private fun therapySession(
        date: LocalDate,
        random: Random,
        panelId: String,
        panelName: String,
        zone: ZoneId,
        overdoingIt: Boolean,
        evening: Boolean = false,
    ): Session {
        val hour = if (evening) random.nextInt(19, 22) else random.nextInt(6, 10)
        val startedAt = date.atTime(LocalTime.of(hour, random.nextInt(0, 60)))
        val countdownMinutes = if (overdoingIt) random.nextInt(15, 21) else listOf(10, 10, 10, 12, 15).random(random)

        // Most runs finish; some get cut short, which is the difference between what was asked for
        // and what happened.
        val ranMinutes = if (random.nextDouble() < 0.15) {
            random.nextInt(3, countdownMinutes)
        } else {
            countdownMinutes
        }

        val areas = if (overdoingIt) {
            listOf(Area.LOWER_BACK)
        } else if (random.nextDouble() < 0.35) {
            listOf(Area.entries.random(random), Area.entries.random(random)).distinct()
        } else {
            listOf(favourites.random(random))
        }

        val brightness = if (evening) mapOf(0 to 0, 1 to 100) else mapOf(0 to 100, 1 to 100)

        // Turning around splits the run between the areas, which is what two exposures mean.
        val each = ranMinutes / areas.size

        return Session(
            id = "demo-${startedAt.atZone(zone).toEpochSecond()}",
            panelId = panelId,
            panelName = panelName,
            startedAtMillis = startedAt.atZone(zone).toInstant().toEpochMilli(),
            seconds = ranMinutes * 60,
            countdownSeconds = countdownMinutes * 60,
            exposures = areas.map { area ->
                Exposure(
                    area = area,
                    seconds = each * 60,
                    brightnessByChannel = brightness,
                    distanceCm = if (random.nextDouble() < 0.3) listOf(20, 25, 30, 40).random(random) else null,
                )
            },
        )
    }

    private fun ambientSession(
        date: LocalDate,
        random: Random,
        panelId: String,
        panelName: String,
        zone: ZoneId,
    ): Session {
        val startedAt = date.atTime(LocalTime.of(random.nextInt(20, 23), random.nextInt(0, 60)))
        val minutes = listOf(20, 25, 30).random(random)

        return Session(
            id = "demo-ambient-${startedAt.atZone(zone).toEpochSecond()}",
            panelId = panelId,
            panelName = panelName,
            startedAtMillis = startedAt.atZone(zone).toInstant().toEpochMilli(),
            seconds = minutes * 60,
            countdownSeconds = minutes * 60,
            exposures = emptyList(),
        )
    }

    /** The areas people actually keep coming back to, so the statistics have a shape. */
    private val favourites = listOf(
        Area.FACE_AND_NECK,
        Area.FACE_AND_NECK,
        Area.LOWER_BACK,
        Area.LOWER_BACK,
        Area.KNEES,
        Area.SHOULDERS,
        Area.UPPER_BACK,
        Area.THIGHS,
    )
}

private fun daysFrom(from: LocalDate, to: LocalDate): Set<LocalDate> =
    generateSequence(from) { day -> day.plusDays(1).takeIf { !it.isAfter(to) } }.toSet()
