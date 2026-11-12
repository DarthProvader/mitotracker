package mitolight.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.ui.components.CardAction
import mitolight.app.ui.components.Divider
import mitolight.app.ui.components.DoseBar
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.IconBubble
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.Pill
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.label
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Area
import mitolight.domain.Limit
import mitolight.domain.dosesOn
import mitolight.domain.minutesByArea
import mitolight.domain.minutesByDay
import mitolight.domain.streak

/**
 * The screen the vendor app has no equivalent of at all: whether the panel is actually being used,
 * and which parts of somebody have had what.
 */
@Composable
fun StatsScreen(
    log: SessionLog,
    onLimits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val sessions by log.sessions.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    val run = remember(sessions) { streak(sessions, today, zone) }
    val perDay = remember(sessions) { minutesByDay(sessions, zone) }
    val todaysDoses = dosesOn(today, sessions, zone)

    // What was lit today comes first, then the month behind it: the limit is a daily number, so an
    // area with minutes on it today is the one the screen has something to say about.
    val perArea = remember(sessions) {
        minutesByArea(sessions, from = today.minusDays(29), to = today, zone = zone)
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Area, Int>> { todaysDoses[it.key]?.minutes ?: 0 }
                    .thenByDescending { it.value },
            )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.screenMargin)
            .padding(top = space.sm, bottom = space.xl),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        ScreenTitle(
            title = stringResource(R.string.stats_title),
            subtitle = stringResource(R.string.stats_subtitle),
        )

        MitoCard(glow = colours.glowAmber) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.lg),
            ) {
                IconBubble(
                    icon = Icons.Filled.LocalFireDepartment,
                    tint = colours.warning,
                    size = 52.dp,
                    fill = colours.hairlineBright,
                )

                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(stringResource(R.string.stats_consistency))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(space.sm),
                    ) {
                        Text(
                            text = "${run.current}",
                            style = MitoTheme.type.metricLg,
                            color = colours.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (run.current == 1) R.string.stats_day_in_a_row else R.string.stats_days_in_a_row,
                            ),
                            style = MitoTheme.type.labelSm,
                            color = colours.onSurfaceMuted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }

                // Mito Light's own literature is built on daily use, so a run of days is the one
                // number worth congratulating. Below three days there is nothing to congratulate.
                if (run.current >= 3) {
                    Pill(
                        label = stringResource(R.string.status_optimal),
                        tint = colours.successBright,
                        icon = Icons.Outlined.TrendingUp,
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = space.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stats_longest_run),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
                Text(
                    text = stringResource(R.string.format_days, run.longest),
                    style = MitoTheme.type.metricMd,
                    color = colours.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.stats_ambient_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        MitoCard(glow = colours.vignette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarViewMonth,
                        contentDescription = null,
                        tint = colours.onSurfaceMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.stats_exposure_map),
                        style = MitoTheme.type.titleMd,
                        color = colours.onSurface,
                    )
                }
                HeatLegend()
            }

            HeatMap(
                until = today,
                weeks = 5,
                minutesByDay = perDay,
                modifier = Modifier.padding(top = space.lg),
            )

            Divider(modifier = Modifier.padding(vertical = space.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow(stringResource(R.string.stats_total_sessions))
                Text(
                    text = "${sessions.size}",
                    style = MitoTheme.type.metricMd,
                    color = colours.onSurface,
                )
            }
        }

        MitoCard(glow = colours.glowNir) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Accessibility,
                        contentDescription = null,
                        tint = colours.channelNirBright,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.stats_dose_per_area),
                        style = MitoTheme.type.titleMd,
                        color = colours.onSurface,
                    )
                }
                Pill(label = stringResource(R.string.word_today), tint = colours.onSurfaceMuted)
            }

            if (perArea.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_nothing_recorded),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                    modifier = Modifier.padding(top = space.lg),
                )
            }

            Column(
                modifier = Modifier.padding(top = space.lg),
                verticalArrangement = Arrangement.spacedBy(space.lg),
            ) {
                perArea.forEach { (area, minutes) ->
                    AreaDose(
                        area = area,
                        todaysMinutes = todaysDoses[area]?.minutes ?: 0,
                        fraction = todaysDoses[area]?.fractionOfLimit ?: 0f,
                        over = todaysDoses[area]?.status?.isOver == true,
                        monthMinutes = minutes,
                    )
                }
            }

            CardAction(
                label = stringResource(R.string.action_adjust_dose_limits),
                onClick = onLimits,
                icon = Icons.Outlined.Tune,
                modifier = Modifier.padding(top = space.lg),
            )
        }
    }
}

/**
 * One area's day, against the manual's twenty minutes, with the month behind it for scale. An area
 * with nothing on it today gets no bar: a column of empty troughs says less than the month's figure
 * does, and the limit it would be measured against has not been touched.
 */
@Composable
private fun AreaDose(
    area: Area,
    todaysMinutes: Int,
    fraction: Float,
    over: Boolean,
    monthMinutes: Int,
) {
    val colours = MitoTheme.colours
    val limit = Limit.FromTheManual
    val tint = if (over) colours.warning else colours.onSurface

    if (todaysMinutes == 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = area.label(), style = MitoTheme.type.labelMd, color = colours.onSurfaceMuted)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$monthMinutes",
                    style = MitoTheme.type.metricMd,
                    color = colours.onSurfaceMuted,
                )
                Text(
                    text = stringResource(R.string.stats_min_per_30_days),
                    style = MitoTheme.type.labelSm,
                    color = colours.onSurfaceMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        return
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = area.label(), style = MitoTheme.type.labelMd, color = tint)
                if (over) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = colours.warning,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "$todaysMinutes", style = MitoTheme.type.metricMd, color = tint)
                Text(
                    text = stringResource(R.string.dose_of_limit_format, limit.recommendedMinutes),
                    style = MitoTheme.type.labelSm,
                    color = if (over) colours.warning.copy(alpha = 0.7f) else colours.onSurfaceMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }

        DoseBar(fraction = fraction, over = over, modifier = Modifier.padding(top = MitoTheme.space.sm))

        Text(
            text = stringResource(R.string.stats_month_minutes_format, monthMinutes),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The ramp, spelled out, because a heat map without a key is decoration. */
@Composable
private fun HeatLegend() {
    val colours = MitoTheme.colours

    Row(
        modifier = Modifier
            .clip(MitoTheme.shape.full)
            .background(colours.hairline)
            .border(1.dp, colours.hairline, MitoTheme.shape.full)
            .padding(horizontal = MitoTheme.space.sm, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_heat_low),
            style = MitoTheme.type.labelSm,
            color = colours.onSurfaceMuted,
            modifier = Modifier.padding(end = 2.dp),
        )
        listOf(colours.heatNone, colours.heatLight, colours.heatFull, colours.heatOver).forEach {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(it, MitoTheme.shape.xs)
                    .border(1.dp, colours.hairline, MitoTheme.shape.xs),
            )
        }
        Text(
            text = stringResource(R.string.stats_heat_max),
            style = MitoTheme.type.labelSm,
            color = colours.onSurfaceMuted,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/**
 * A day per cell on the four-step ramp, seven to a row, with no numbers in them: the point is the
 * shape of a month of use, and today is marked by a ring rather than by a date.
 */
@Composable
private fun HeatMap(
    until: LocalDate,
    weeks: Int,
    minutesByDay: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val weekdays = listOf(
        stringResource(R.string.stats_weekday_mon),
        stringResource(R.string.stats_weekday_tue),
        stringResource(R.string.stats_weekday_wed),
        stringResource(R.string.stats_weekday_thu),
        stringResource(R.string.stats_weekday_fri),
        stringResource(R.string.stats_weekday_sat),
        stringResource(R.string.stats_weekday_sun),
    )

    // Rows run Monday to Sunday, so the grid lines up with how people read a week.
    val end = until.plusDays((7 - until.dayOfWeek.value).toLong())
    val start = end.minusWeeks(weeks.toLong() - 1).minusDays(6)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
            weekdays.forEachIndexed { index, day ->
                Text(
                    text = day,
                    style = MitoTheme.type.metricSm,
                    color = if (index >= 5) colours.onSurfaceMuted.copy(alpha = 0.5f) else colours.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        repeat(weeks) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                repeat(7) { weekday ->
                    val date = start.plusDays((week * 7 + weekday).toLong())
                    val minutes = minutesByDay[date] ?: 0
                    val future = date.isAfter(until)
                    val isToday = date == until

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                color = if (future) Color.Transparent else colours.heat(minutes),
                                shape = MitoTheme.shape.xs,
                            )
                            .border(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) colours.primaryBright else colours.hairline,
                                shape = MitoTheme.shape.xs,
                            ),
                    )
                }
            }
        }
    }
}
