package mitolight.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.GlowDot
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.label
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Session

/** Sessions newest first, grouped by day, with the day's total — the logbook the vendor app lacks. */
@Composable
fun HistoryScreen(
    log: SessionLog,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val sessions by log.sessions.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    val days = remember(sessions) {
        sessions.groupBy { it.dateIn(zone) }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = space.screenMargin),
        verticalArrangement = Arrangement.spacedBy(space.md),
        contentPadding = PaddingValues(top = space.sm, bottom = space.xl),
    ) {
        item {
            ScreenTitle(
                title = stringResource(R.string.history_title),
                subtitle = stringResource(R.string.history_subtitle_format, sessions.size),
                modifier = Modifier.padding(bottom = space.sm),
            )
        }

        days.forEach { (date, onThatDay) ->
            item(key = "day-$date") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = space.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eyebrow(dayLabel(date, today))
                    Text(
                        text = stringResource(R.string.format_min, onThatDay.sumOf { it.minutes }),
                        style = MitoTheme.type.metricSm,
                        color = colours.onSurfaceMuted,
                    )
                }
            }

            items(onThatDay, key = { it.id }) { session ->
                SessionRow(session = session, onOpen = { onOpen(session.id) })
            }
        }
    }
}

/**
 * A row reads left to right as an instrument log: how long, then what it was aimed at, then what it
 * ran. The lit edge carries which channels were used, so a month of history can be scanned for
 * near-infrared work without reading a word.
 */
@Composable
private fun SessionRow(session: Session, onOpen: () -> Unit) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    MitoCard(onClick = onOpen, accent = barColour(session)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Fixed width, so the rule beside it lands in the same place on every row and the column
            // of durations can be read down rather than one row at a time.
            Text(
                text = duration(session),
                style = MitoTheme.type.metricMd,
                color = colours.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.width(92.dp).padding(end = space.md),
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(colours.hairlineBright),
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = space.md)) {
                Text(
                    text = if (session.isUnattributed) {
                        stringResource(R.string.word_ambient_light)
                    } else {
                        session.exposures.map { it.area.label() }.joinToString(" · ")
                    },
                    style = MitoTheme.type.titleMd,
                    color = colours.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.sm),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    channelDots(session).forEach { GlowDot(it, size = 7.dp) }
                    Text(
                        text = caption(session),
                        style = MitoTheme.type.bodySm,
                        color = colours.onSurfaceMuted,
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colours.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun barColour(session: Session): Color = with(MitoTheme.colours) {
    val mix = session.exposures.firstOrNull()?.brightnessByChannel ?: emptyMap()
    val red = (mix[0] ?: 0) > 0
    val nearInfrared = (mix[1] ?: 0) > 0

    when {
        session.isUnattributed -> surfaceHigh
        red && nearInfrared -> primary
        nearInfrared -> channelNir
        red -> channelRed
        else -> surfaceHigh
    }
}

@Composable
private fun channelDots(session: Session): List<Color> = with(MitoTheme.colours) {
    val mix = session.exposures.firstOrNull()?.brightnessByChannel ?: emptyMap()

    buildList {
        if ((mix[0] ?: 0) > 0) add(channelRed)
        if ((mix[1] ?: 0) > 0) add(channelNir)
    }
}

/** When it ran, and at what — the dots beside this say which percentage belongs to which channel. */
@Composable
private fun caption(session: Session): String {
    val mix = session.exposures.firstOrNull()?.brightnessByChannel ?: emptyMap()
    val red = mix[0] ?: 0
    val nearInfrared = mix[1] ?: 0

    val what = when {
        session.isUnattributed -> stringResource(R.string.word_not_counted)
        red > 0 && nearInfrared > 0 ->
            "${stringResource(R.string.format_percent, red)} · ${stringResource(R.string.format_percent, nearInfrared)}"
        nearInfrared > 0 -> stringResource(R.string.format_percent_nir, nearInfrared)
        red > 0 -> stringResource(R.string.format_percent_red, red)
        else -> stringResource(R.string.word_no_output)
    }

    return "${startedAt(session)} · $what"
}

/** Minutes and seconds, because a session that was cut short is the interesting one. */
private fun duration(session: Session): String =
    "%d:%02d".format(session.seconds / 60, session.seconds % 60)

private fun startedAt(session: Session): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(session.startedAt)

@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.word_today)
    today.minusDays(1) -> stringResource(R.string.word_yesterday)
    else -> DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(date)
}
