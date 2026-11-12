package mitolight.app.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import mitolight.app.R
import mitolight.app.ui.clock
import mitolight.app.ui.components.Chip
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.PrimaryAction
import mitolight.app.ui.components.TextAction
import mitolight.app.ui.joinedLabels
import mitolight.app.ui.label
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Area
import mitolight.domain.Session

/**
 * What actually happened, offered for correction.
 *
 * A session is not what was asked for: twenty minutes set and five minutes stood in front of it is a
 * five minute session, and this is where that gets confirmed. It is also the second chance to
 * attribute a run nobody claimed at the time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionSummary(
    session: Session,
    onConfirm: () -> Unit,
    onCorrect: (List<Area>) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val chosen = remember(session.id) {
        mutableStateListOf<Area>().also { it.addAll(session.exposures.map { exposure -> exposure.area }) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Heavier than the token scrim, which is sized for a sheet: this one has to stop the
            // screen behind it competing, because it is asking a question about the past.
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(space.screenMargin),
        contentAlignment = Alignment.Center,
    ) {
        MitoCard(
            raised = true,
            glow = colours.glowRed,
            // Opaque, unlike every other card: a lit control screen showing through the answer to
            // "what just happened" is two interfaces at once.
            opaque = true,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Eyebrow(stringResource(R.string.summary_session_recorded))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = space.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = clock(session.seconds),
                    style = MitoTheme.type.metricLg,
                    color = colours.primaryBright,
                )
                Text(
                    text = if (session.ranToCompletion) {
                        stringResource(R.string.summary_ran_to_end)
                    } else {
                        stringResource(R.string.summary_of_min_set_format, session.countdownSeconds / 60)
                    },
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
            }

            Text(
                text = if (chosen.isEmpty()) {
                    stringResource(R.string.summary_ambient_note)
                } else {
                    stringResource(R.string.summary_credited_to_format, chosen.toList().joinedLabels())
                },
                style = MitoTheme.type.bodyMd,
                color = colours.onSurface,
            )

            Eyebrow(text = stringResource(R.string.summary_correct_it), modifier = Modifier.padding(top = space.lg))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space.sm),
                verticalArrangement = Arrangement.spacedBy(space.sm),
                modifier = Modifier.padding(vertical = space.sm),
            ) {
                Area.entries.forEach { area ->
                    Chip(
                        label = area.label(),
                        selected = area in chosen,
                        onClick = { if (area in chosen) chosen.remove(area) else chosen.add(area) },
                    )
                }
            }

            PrimaryAction(
                label = stringResource(R.string.action_thats_right),
                onClick = {
                    val same = chosen.toList() == session.exposures.map { it.area }
                    if (same) onConfirm() else onCorrect(chosen.toList())
                },
                modifier = Modifier.padding(top = space.sm),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextAction(
                    label = stringResource(R.string.action_discard_session),
                    onClick = onDiscard,
                )
            }
        }
    }
}
