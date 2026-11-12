package mitolight.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.SecondaryAction
import mitolight.app.ui.components.TextAction
import mitolight.app.ui.clock
import mitolight.app.ui.label
import mitolight.app.ui.mix
import mitolight.app.ui.spell
import mitolight.app.ui.theme.MitoTheme

/** One session in full: what was asked for, what happened, and which areas got the minutes. */
@Composable
fun SessionDetailScreen(
    id: String,
    log: SessionLog,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val sessions by log.sessions.collectAsStateWithLifecycle()
    val session = sessions.firstOrNull { it.id == id }
    val zone = remember { ZoneId.systemDefault() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.screenMargin, vertical = space.lg),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        TextAction(label = stringResource(R.string.action_back), onClick = onBack)

        if (session == null) {
            Text(
                text = stringResource(R.string.detail_session_gone),
                style = MitoTheme.type.bodyMd,
                color = colours.onSurfaceMuted,
            )
            return@Column
        }

        Text(
            text = dayLabel(session.dateIn(zone), LocalDate.now()),
            style = MitoTheme.type.headlineMd,
            color = colours.onSurface,
        )
        Text(
            text = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.getDefault())
                .withZone(zone)
                .format(session.startedAt),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
        )

        MitoCard {
            Fact(stringResource(R.string.fact_lit_for), clock(session.seconds))
            Fact(
                stringResource(R.string.label_countdown_set_to),
                stringResource(R.string.format_min, session.countdownSeconds / 60),
            )
            Fact(
                stringResource(R.string.fact_ended),
                stringResource(
                    if (session.ranToCompletion) R.string.detail_ended_zero else R.string.detail_ended_early,
                ),
            )
            Fact(stringResource(R.string.fact_panel), session.panelName)
        }

        if (session.isUnattributed) {
            MitoCard(raised = true) {
                Text(
                    text = stringResource(R.string.word_ambient_light),
                    style = MitoTheme.type.titleMd,
                    color = colours.onSurface,
                )
                Text(
                    text = stringResource(R.string.detail_ambient_note),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
            }
        } else {
            Eyebrow(stringResource(R.string.detail_exposures))
            session.exposures.forEach { exposure ->
                MitoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = exposure.area.label(),
                            style = MitoTheme.type.titleMd,
                            color = colours.onSurface,
                        )
                        Text(
                            text = spell(exposure.seconds),
                            style = MitoTheme.type.metricMd,
                            color = colours.onSurface,
                        )
                    }
                    Text(
                        text = mix(exposure.brightnessByChannel, exposure.brightnessByChannel.size),
                        style = MitoTheme.type.metricSm,
                        color = colours.onSurfaceMuted,
                    )
                    exposure.distanceCm?.let { distance ->
                        Text(
                            text = stringResource(R.string.format_distance_from_panel, distance),
                            style = MitoTheme.type.bodySm,
                            color = colours.onSurfaceMuted,
                        )
                    }
                }
            }
        }

        SecondaryAction(
            label = stringResource(R.string.action_delete_session),
            onClick = {
                log.forget(session.id)
                onBack()
            },
        )
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MitoTheme.space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MitoTheme.type.bodyMd, color = MitoTheme.colours.onSurfaceMuted)
        Text(text = value, style = MitoTheme.type.bodyMd, color = MitoTheme.colours.onSurface)
    }
}
