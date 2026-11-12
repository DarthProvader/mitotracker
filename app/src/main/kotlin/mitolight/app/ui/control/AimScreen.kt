package mitolight.app.ui.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import mitolight.app.PanelViewModel
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.SessionTracker
import mitolight.app.ui.components.AreaGrid
import mitolight.app.ui.components.DoseBar
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.Pill
import mitolight.app.ui.components.PrimaryAction
import mitolight.app.ui.components.RoundIconButton
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.joinedLabels
import mitolight.app.ui.label
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Area
import mitolight.domain.Dose
import mitolight.domain.DoseStatus
import mitolight.domain.Limit
import mitolight.domain.dosesOn
import mitolight.panel.PanelState

/**
 * Where the light is pointed, asked while it is pointed there.
 *
 * This used to stand between the start button and the panel, which made naming a body part the price
 * of turning a lamp on. Attribution belongs at the end of a run, where the summary asks — so what is
 * left for this screen is the two cases that happen mid-session and cannot wait:
 *
 * - the app arrived at a panel that was already lit, and nobody has claimed those minutes;
 * - somebody turned around, which is one session with two exposures and the case the whole
 *   vocabulary was written for.
 *
 * [turning] is the difference between the two. Correcting the aim rewrites the run so far; turning
 * around keeps the minutes already spent where they were and starts counting the new direction from
 * now.
 */
@Composable
fun AimScreen(
    model: PanelViewModel,
    log: SessionLog,
    tracker: SessionTracker,
    turning: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    val sessions by log.sessions.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val aim by tracker.aim.collectAsStateWithLifecycle()
    val running = (state as? PanelState.Ready)?.status?.running == true

    val today = remember { LocalDate.now() }
    val doses = dosesOn(today, sessions)
    val chosen = remember {
        if (turning) mutableListOf<Area>().toMutableStateList()
        else (aim?.areas ?: emptyList()).toMutableStateList()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = space.screenMargin)
                .padding(bottom = space.lg),
            verticalArrangement = Arrangement.spacedBy(space.lg),
        ) {
            RoundIconButton(
                icon = Icons.Outlined.ArrowBack,
                onClick = onDone,
                contentDescription = stringResource(R.string.action_back),
            )

            ScreenTitle(
                title = stringResource(
                    if (turning) R.string.aim_title_turning else R.string.aim_title_target,
                ),
                subtitle = stringResource(
                    if (turning) R.string.aim_subtitle_turning else R.string.aim_subtitle_target,
                ),
            )

            if (turning) {
                aim?.takeIf { it.areas.isNotEmpty() }?.let { current ->
                    Pill(
                        label = stringResource(
                            R.string.aim_until_now_format,
                            current.areas.joinedLabels(),
                        ),
                        tint = colours.onSurfaceMuted,
                        icon = Icons.Outlined.MyLocation,
                    )
                }
            }

            AreaGrid(
                chosen = chosen,
                onToggle = { area -> if (area in chosen) chosen.remove(area) else chosen.add(area) },
            )

            chosen.forEach { area ->
                DoseCard(dose = doses[area] ?: Dose(area, 0))
            }

            if (chosen.isEmpty()) {
                Text(
                    text = stringResource(
                        if (turning) R.string.aim_empty_turning else R.string.aim_empty_target,
                    ),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
            }

            if (!running) {
                Text(
                    text = stringResource(R.string.aim_not_running),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
            }
        }

        // Pinned, because the grid is taller than the screen and the decision is not.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, colours.background)),
                )
                .padding(horizontal = space.screenMargin)
                .padding(top = space.xl, bottom = space.md),
        ) {
            PrimaryAction(
                label = stringResource(
                    when {
                        turning -> R.string.action_from_now_on
                        chosen.isEmpty() -> R.string.action_dont_record
                        else -> R.string.action_record_against_these
                    },
                ),
                onClick = {
                    if (turning) tracker.turnedTo(chosen.toList()) else tracker.aimAt(chosen.toList())
                    onDone()
                },
                icon = Icons.Outlined.MyLocation,
            )
        }
    }
}

/** The dose this area already carries today, and whether it is at the manual's limit. */
@Composable
private fun DoseCard(dose: Dose) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val limit = Limit.FromTheManual
    val nearing = dose.status == DoseStatus.NEARING

    MitoCard(glow = if (dose.status.isOver) colours.glowAmber else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Eyebrow(stringResource(R.string.dose_today_format, dose.area.label()))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = "${dose.minutes}",
                        style = MitoTheme.type.metricMd,
                        color = if (dose.status.isOver) colours.warning else colours.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.dose_of_limit_format, limit.recommendedMinutes),
                        style = MitoTheme.type.metricSm,
                        color = colours.onSurfaceMuted,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }

            when {
                dose.status.isOver -> Pill(
                    label = stringResource(R.string.status_over_limit),
                    tint = colours.warning,
                    icon = Icons.Filled.Warning,
                )

                nearing -> Pill(
                    label = stringResource(R.string.status_nearing_limit),
                    tint = colours.warning,
                    icon = Icons.Filled.Warning,
                )

                else -> Pill(
                    label = stringResource(R.string.dose_minutes_left_format, dose.remainingMinutes),
                    tint = colours.successBright,
                )
            }
        }

        DoseBar(
            fraction = dose.fractionOfLimit,
            over = dose.status.isOver,
            modifier = Modifier.padding(top = space.md),
        )

        if (dose.status.isOver) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = space.md),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = colours.warning,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.dose_over_limit_warning, limit.recommendedMinutes),
                    style = MitoTheme.type.bodySm,
                    color = colours.warning,
                )
            }
        }
    }
}
