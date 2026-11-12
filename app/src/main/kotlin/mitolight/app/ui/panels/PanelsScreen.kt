package mitolight.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mitolight.app.PanelSource
import mitolight.app.PanelViewModel
import mitolight.app.R
import mitolight.app.ui.components.Chip
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.IconBubble
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.RoundIconButton
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.components.SecondaryAction
import mitolight.app.ui.components.StatusDot
import mitolight.app.ui.components.TextAction
import mitolight.app.ui.panelSourceLabel
import mitolight.app.ui.theme.MitoTheme
import mitolight.panel.PanelState
import mitolight.panel.RememberedPanel

/**
 * Choosing what to drive. A remembered panel is reached from its stored address with no scan at all,
 * which is worth the prominence: scanning only finds a panel while nothing else is connected to it.
 */
@Composable
fun PanelsScreen(
    model: PanelViewModel,
    onControl: () -> Unit,
    onDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    val source by model.source.collectAsStateWithLifecycle()
    val remembered by model.remembered.collectAsStateWithLifecycle()
    val found by model.found.collectAsStateWithLifecycle()
    val scanning by model.scanning.collectAsStateWithLifecycle()
    val scannedOnce by model.scannedOnce.collectAsStateWithLifecycle()
    val showingEverything by model.showingEverything.collectAsStateWithLifecycle()
    val failure by model.failure.collectAsStateWithLifecycle()
    val held by model.panel.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.screenMargin)
            .padding(top = space.sm, bottom = space.xl),
        verticalArrangement = Arrangement.spacedBy(space.md),
    ) {
        ScreenTitle(
            title = stringResource(R.string.tab_panels),
            subtitle = stringResource(R.string.panels_subtitle),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(space.sm),
            modifier = Modifier.padding(vertical = space.sm),
        ) {
            PanelSource.entries.forEach { option ->
                Chip(
                    label = panelSourceLabel(option),
                    selected = source == option,
                    onClick = { model.chooseSource(option) },
                )
            }
        }

        failure?.let { message ->
            MitoCard(glow = colours.glowRed) {
                Text(text = message, style = MitoTheme.type.bodySm, color = colours.error)
                TextAction(label = stringResource(R.string.action_dismiss), onClick = model::dismissFailure)
            }
        }

        // A first run, or a phone that has never met one of these. Saying why nothing is listed is
        // worth a card: the reason is a property of the hardware and nobody guesses it.
        if (remembered.isEmpty() && found.isEmpty() && !scanning) {
            MitoCard(glow = colours.vignette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.lg),
                ) {
                    IconBubble(
                        icon = Icons.Filled.Lightbulb,
                        tint = colours.primaryBright,
                        size = 52.dp,
                        fill = colours.primary.copy(alpha = 0.10f),
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.panels_none_title),
                            style = MitoTheme.type.titleMd,
                            color = colours.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.panels_scan_hint),
                            style = MitoTheme.type.bodySm,
                            color = colours.onSurfaceMuted,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.panels_advertise_note),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                    modifier = Modifier.padding(top = space.md),
                )
            }
        }

        if (remembered.isNotEmpty()) {
            Eyebrow(
                stringResource(R.string.panels_known),
                modifier = Modifier.padding(top = space.sm, bottom = space.xs),
            )

            remembered.forEach { panel ->
                val connected = held?.id == panel.id && state is PanelState.Ready

                KnownPanel(
                    panel = panel,
                    connected = connected,
                    onOpen = {
                        if (!connected) model.reconnect(panel)
                        onControl()
                    },
                    onDetail = { onDetail(panel.id) },
                )
            }
        }

        SecondaryAction(
            label = stringResource(
                if (scanning) R.string.status_scanning else R.string.action_scan,
            ),
            enabled = !scanning,
            onClick = { model.scan() },
            icon = Icons.Outlined.Radar,
            modifier = Modifier.padding(top = space.md),
        )

        if (scanning && found.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.md),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colours.primary)
                Text(
                    text = stringResource(R.string.panels_advertise_short),
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
            }
        }

        if (found.isNotEmpty()) {
            Eyebrow(
                stringResource(R.string.panels_found),
                modifier = Modifier.padding(top = space.sm, bottom = space.xs),
            )
        }

        found.forEach { panel ->
            // A simulated panel is never remembered, so the one being driven stays in this list and
            // has to be marked here too, or the app looks connected to nothing.
            val connected = held?.id == panel.id && state is PanelState.Ready

            MitoCard(
                onClick = {
                    if (!connected) model.select(panel)
                    onControl()
                },
                raised = connected,
                glow = if (connected) colours.glowRed else null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.lg),
                ) {
                    IconBubble(
                        icon = Icons.Filled.Lightbulb,
                        tint = if (connected) colours.successBright else colours.onSurfaceMuted,
                        size = 48.dp,
                        fill = if (connected) colours.hairlineBright else colours.hairline,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = panel.name,
                            style = MitoTheme.type.titleMd,
                            color = colours.onSurface,
                        )
                        if (connected) {
                            StatusDot(
                                colour = colours.successBright,
                                label = stringResource(R.string.status_connected),
                            )
                        } else {
                            Text(
                                text = panel.id,
                                style = MitoTheme.type.metricSm,
                                color = colours.onSurfaceMuted,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = if (connected) colours.onSurface else colours.onSurfaceMuted,
                    )
                }
            }
        }

        if (scannedOnce && !scanning && found.isEmpty() && !showingEverything) {
            TextAction(
                label = stringResource(R.string.action_list_every_device),
                onClick = { model.scan(onlyPanels = false) },
            )
        }
    }
}

@Composable
private fun KnownPanel(
    panel: RememberedPanel,
    connected: Boolean,
    onOpen: () -> Unit,
    onDetail: () -> Unit,
) {
    val colours = MitoTheme.colours

    MitoCard(
        onClick = onOpen,
        raised = connected,
        glow = if (connected) colours.glowRed else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.lg),
        ) {
            IconBubble(
                icon = Icons.Filled.Lightbulb,
                tint = if (connected) colours.successBright else colours.onSurfaceMuted,
                size = 52.dp,
                fill = colours.hairlineBright,
                contentDescription = null,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = panel.name, style = MitoTheme.type.titleMd, color = colours.onSurface)

                if (connected) {
                    StatusDot(
                        colour = colours.successBright,
                        label = stringResource(R.string.status_connected),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = colours.onSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.panels_channels_tap_reconnect_format,
                                panel.capability.channelCount,
                            ),
                            style = MitoTheme.type.bodySm,
                            color = colours.onSurfaceMuted,
                        )
                    }
                }
            }

            RoundIconButton(
                icon = Icons.Outlined.ChevronRight,
                onClick = onDetail,
                tint = if (connected) colours.onSurface else colours.onSurfaceMuted,
                contentDescription = stringResource(R.string.cd_panel_detail),
            )
        }
    }
}
