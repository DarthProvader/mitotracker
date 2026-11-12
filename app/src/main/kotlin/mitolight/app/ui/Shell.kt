package mitolight.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mitolight.app.LocalePreferences
import mitolight.app.PanelViewModel
import mitolight.app.PresetPreferences
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.SessionTracker
import mitolight.app.ui.components.GlowDot
import mitolight.app.ui.components.RoundIconButton
import mitolight.app.ui.control.AimScreen
import mitolight.app.ui.control.ControlScreen
import mitolight.app.ui.control.SessionSummary
import mitolight.app.ui.control.SetupScreen
import mitolight.app.ui.history.HistoryScreen
import mitolight.app.ui.history.SessionDetailScreen
import mitolight.app.ui.panels.PanelDetailScreen
import mitolight.app.ui.panels.PanelsScreen
import mitolight.app.ui.settings.SettingsScreen
import mitolight.app.ui.stats.StatsScreen
import mitolight.app.ui.theme.MitoTheme
import mitolight.app.ui.theme.glass
import mitolight.app.ui.theme.roomLight
import mitolight.panel.PanelState

enum class Tab(val label: String, val icon: ImageVector) {
    PANELS("Panels", Icons.Outlined.Sensors),
    CONTROL("Control", Icons.Outlined.Timer),
    HISTORY("History", Icons.Outlined.History),
    STATS("Stats", Icons.Outlined.CalendarMonth),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

/** Screens that sit on top of a tab rather than beside it, and that back dismisses. */
sealed interface Pushed {
    data object Setup : Pushed

    /** Naming what the light is on. [turning] separates a correction from a turn mid-run. */
    data class Aim(val turning: Boolean) : Pushed

    data class SessionDetail(val id: String) : Pushed

    data class PanelDetail(val id: String) : Pushed
}

/**
 * Five destinations, landing on Control.
 *
 * It used to land on Panels and jump to Control if a remembered panel answered inside a grace window,
 * which meant the app opened on a list of hardware and then moved under the user's thumb. The app is
 * opened to turn a light on, so it opens on the screen that does that, and the state of the link is
 * reported by the switch itself rather than by which screen is showing.
 */
@Composable
fun Shell(
    model: PanelViewModel,
    log: SessionLog,
    presets: PresetPreferences,
    tracker: SessionTracker,
    locales: LocalePreferences,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(Tab.CONTROL) }
    val stack = remember { mutableStateListOf<Pushed>() }
    val finished by tracker.finished.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()

    BackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }

    Box(modifier = modifier.fillMaxSize().roomLight()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppBar(
                lit = (state as? PanelState.Ready)?.status?.running == true,
                connected = state is PanelState.Ready,
                onPanels = { tab = Tab.PANELS; stack.clear() },
                onSettings = { tab = Tab.SETTINGS; stack.clear() },
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val pushed = stack.lastOrNull()) {
                    null -> Destination(tab, model, log, tracker, locales, stack) { tab = it }

                    Pushed.Setup -> SetupScreen(
                        model = model,
                        presets = presets,
                        tracker = tracker,
                        onDone = { stack.pop() },
                    )

                    is Pushed.Aim -> AimScreen(
                        model = model,
                        log = log,
                        tracker = tracker,
                        turning = pushed.turning,
                        onDone = { stack.pop() },
                    )

                    is Pushed.SessionDetail -> SessionDetailScreen(
                        id = pushed.id,
                        log = log,
                        onBack = { stack.pop() },
                    )

                    is Pushed.PanelDetail -> PanelDetailScreen(
                        id = pushed.id,
                        model = model,
                        onBack = { stack.pop() },
                    )
                }
            }

            BottomBar(selected = tab, onSelect = { tab = it; stack.clear() })
        }

        // The session is what happened, not what was asked for, so the last word is the user's.
        finished?.let { session ->
            SessionSummary(
                session = session,
                onConfirm = tracker::dismissSummary,
                onCorrect = { areas ->
                    log.attribute(session.id, areas)
                    tracker.dismissSummary()
                },
                onDiscard = {
                    log.forget(session.id)
                    tracker.dismissSummary()
                },
            )
        }
    }
}

/**
 * The brand across the top, and either side of it the two things that are true of the whole app
 * rather than of one screen: what it is talking to, and how it behaves.
 */
@Composable
private fun AppBar(
    lit: Boolean,
    connected: Boolean,
    onPanels: () -> Unit,
    onSettings: () -> Unit,
) {
    val colours = MitoTheme.colours

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = MitoTheme.space.lg, vertical = MitoTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            RoundIconButton(
                icon = Icons.Outlined.Sensors,
                onClick = onPanels,
                contentDescription = stringResource(R.string.tab_panels),
            )
            // The one status worth carrying on every screen: a lit panel is a thing happening in the
            // room, and the app is the only place that says so once the phone is in a pocket.
            if (connected) {
                GlowDot(
                    colour = if (lit) colours.primary else colours.success,
                    size = 8.dp,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MitoTheme.type.headlineSm,
            color = colours.channelRed,
            modifier = Modifier.weight(1f).padding(horizontal = MitoTheme.space.sm),
        )

        RoundIconButton(
            icon = Icons.Outlined.Tune,
            onClick = onSettings,
            contentDescription = stringResource(R.string.tab_settings),
        )
    }
}

@Composable
private fun Destination(
    tab: Tab,
    model: PanelViewModel,
    log: SessionLog,
    tracker: SessionTracker,
    locales: LocalePreferences,
    stack: SnapshotStateList<Pushed>,
    onTab: (Tab) -> Unit,
) {
    when (tab) {
        Tab.PANELS -> PanelsScreen(
            model = model,
            onControl = { onTab(Tab.CONTROL) },
            onDetail = { id -> stack.add(Pushed.PanelDetail(id)) },
        )

        Tab.CONTROL -> ControlScreen(
            model = model,
            tracker = tracker,
            onSetup = { stack.add(Pushed.Setup) },
            onAim = { turning -> stack.add(Pushed.Aim(turning)) },
            onPanels = { onTab(Tab.PANELS) },
        )

        Tab.HISTORY -> HistoryScreen(
            log = log,
            onOpen = { id -> stack.add(Pushed.SessionDetail(id)) },
        )

        Tab.STATS -> StatsScreen(log = log, onLimits = { onTab(Tab.SETTINGS) })

        Tab.SETTINGS -> SettingsScreen(model = model, log = log, locales = locales)
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colours = MitoTheme.colours
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(shape = shape)
            .navigationBarsPadding()
            .padding(top = MitoTheme.space.md, bottom = MitoTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            val tint = if (active) colours.primaryBright else colours.onSurfaceMuted
            val label = tabLabel(tab)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = MitoTheme.space.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The selected destination gets a raised well under its icon, so the current place
                // is legible without relying on the red alone.
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .then(
                            if (active) {
                                Modifier
                                    .clip(MitoTheme.shape.md)
                                    .background(colours.hairlineBright)
                                    .border(1.dp, colours.hairline, MitoTheme.shape.md)
                                    .padding(horizontal = MitoTheme.space.md)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(text = label, style = MitoTheme.type.labelSm, color = tint)
            }
        }
    }
}

private fun SnapshotStateList<Pushed>.pop() {
    if (isNotEmpty()) removeAt(lastIndex)
}
