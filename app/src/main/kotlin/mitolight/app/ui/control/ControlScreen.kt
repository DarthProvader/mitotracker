package mitolight.app.ui.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mitolight.app.PanelViewModel
import mitolight.app.R
import mitolight.app.SessionTracker
import mitolight.app.ui.components.CardAction
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.Pill
import mitolight.app.ui.components.PowerButton
import mitolight.app.ui.components.SecondaryAction
import mitolight.app.ui.components.StatusPill
import mitolight.app.ui.components.TextAction
import mitolight.app.ui.clock
import mitolight.app.ui.joinedLabels
import mitolight.app.ui.presetName
import mitolight.app.ui.shortMix
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Preset
import mitolight.panel.Panel
import mitolight.panel.PanelState

/**
 * Where the app opens, and the only screen that turns the light on.
 *
 * It used to be a mixing desk with a start bar pinned under it, which meant the thing somebody
 * opened the app to do was the smallest object on the screen and cost a second screen to reach. Now
 * there is one round button, it starts the panel with whatever the panel is already set to, and the
 * settings live behind the row that reports them. The button is drawn even with no panel connected,
 * because "there is no panel yet" is a state of the switch and not a different screen.
 *
 * This also has to survive being opened onto a panel that is already lit and part way through a
 * countdown, which happens whenever somebody starts it from the panel's own touchscreen.
 *
 * While lit the round button pauses: ending the session is a separate, quieter control
 * under it, so the common mid-run action stays the one you can hit without looking.
 */
@Composable
fun ControlScreen(
    model: PanelViewModel,
    tracker: SessionTracker,
    onSetup: () -> Unit,
    onAim: (turning: Boolean) -> Unit,
    onPanels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    val panel by model.panel.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val recovering by model.recovering.collectAsStateWithLifecycle()
    val aim by tracker.aim.collectAsStateWithLifecycle()
    val unclaimed by tracker.unclaimed.collectAsStateWithLifecycle()
    val paused by tracker.paused.collectAsStateWithLifecycle()
    val pausedRemaining by tracker.pausedRemainingSeconds.collectAsStateWithLifecycle()

    val held = panel
    val ready = state as? PanelState.Ready
    val status = ready?.status
    val running = status?.running == true && !paused
    val fallback = if (running || paused) null else fallbackFor(held, ready)
    val shownRemaining = when {
        paused -> pausedRemaining ?: 0
        else -> status?.remainingSeconds ?: 0
    }
    val shownCountdown = fallback?.let { it.countdownMinutes * 60 }
        ?: status?.countdownSeconds
        ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = space.screenMargin)
            .padding(top = space.md, bottom = space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // With nothing connected there is no countdown to report and no output to describe, and a
        // clock reading "--:--" over a card reading "nothing to report" is three ways of saying the
        // same thing. The switch says it once.
        if (held != null) {
            Countdown(
                running = running,
                paused = paused,
                remainingSeconds = shownRemaining,
                // What pressing the button would run for, which is the fallback's countdown when the
                // panel has nothing of its own. A hero reading 0:00 above a card promising ten
                // minutes is the screen arguing with itself.
                countdownSeconds = shownCountdown,
                asking = ready == null || status == null,
            )
        }

        // This slot exists before and after Start. The aim appears when a preset starts, and letting
        // it add height here used to make the power button jump down when the panel stopped again.
        if (held != null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                aim?.let { current ->
                    Pill(
                        label = if (current.areas.isEmpty()) {
                            stringResource(R.string.control_unclaimed_ambient)
                        } else {
                            current.areas.joinedLabels()
                        },
                        tint = if (current.areas.isEmpty()) colours.onSurfaceMuted else colours.primaryBright,
                        icon = Icons.Outlined.MyLocation,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Switch(
                panel = held,
                state = state,
                recovering = recovering,
                running = running,
                paused = paused,
                remainingSeconds = shownRemaining,
                countdownSeconds = shownCountdown,
                onStart = {
                    // A panel that was never told anything sits at nothing per cent for no minutes,
                    // and a start button that visibly does nothing is worse than one that picks a
                    // sensible first run. What it picked is on the screen before it is pressed.
                    val plan = fallbackFor(held, ready)
                    plan?.let { preset ->
                        val mix = preset.brightnessFor(held?.capability?.channelCount ?: 2)
                        mix.forEach { (channel, percent) -> model.setBrightness(channel, percent) }
                        model.setCountdown(preset.countdownMinutes)
                        tracker.aimAt(preset.areas, mix)
                    }
                    tracker.expectStart()
                    model.start()
                },
                onPause = {
                    tracker.pause(ready?.status?.remainingSeconds ?: 0)
                    model.stop()
                },
                onResume = {
                    val plan = tracker.resumePlan() ?: return@Switch
                    plan.brightnessByChannel.forEach { (channel, percent) ->
                        model.setBrightness(channel, percent)
                    }
                    model.setCountdownSeconds(plan.remainingSeconds)
                    model.start()
                },
                onPanels = onPanels,
            )
        }

        // Somebody started this from the panel's own touchscreen, so the app has minutes it cannot
        // attribute to anybody. Asking is the only honest option (ADR 0003).
        if (running && unclaimed) {
            MitoCard(raised = true, glow = colours.glowAmber, modifier = Modifier.padding(bottom = space.md)) {
                Text(
                    text = stringResource(R.string.control_unclaimed_message),
                    style = MitoTheme.type.bodyMd,
                    color = colours.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(space.sm)) {
                    TextAction(
                        label = stringResource(R.string.action_say_aimed_at_unclaimed),
                        onClick = { onAim(false) },
                    )
                    TextAction(
                        label = stringResource(R.string.action_dont_record),
                        onClick = { tracker.aimAt(emptyList()) },
                    )
                }
            }
        }

        // End session is a real control, not a caption like "I turned around": a full-width secondary
        // under the round button. The slot stays reserved so Start ↔ Pause ↔ Resume never jumps.
        Box(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (running || paused) {
                SecondaryAction(
                    label = stringResource(R.string.action_end_session),
                    onClick = {
                        if (paused) tracker.endWhilePaused() else model.stop()
                    },
                    icon = Icons.Filled.Stop,
                )
            }
        }

        // Quiet bookkeeping stays below the session controls, in its own reserved slot.
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (running && !unclaimed) {
                val turning = aim?.areas?.isNotEmpty() == true

                TextAction(
                    label = stringResource(
                        if (turning) R.string.action_turned_around else R.string.action_say_aimed_at,
                    ),
                    onClick = { onAim(turning) },
                    icon = if (turning) Icons.Outlined.SwapHoriz else Icons.Outlined.MyLocation,
                )
            }
        }

        // What the button will do, and the way to change it. A start screen that reports its own
        // settings does not need to carry the controls for them.
        if (held != null) {
            Plan(panel = held, state = ready, fallback = fallback, onSetup = onSetup)
        }
    }
}

/**
 * The hero, because it is the number somebody standing two metres away is trying to read. A stopped
 * panel keeps reporting the time left from its last run, so that figure is only live while the light
 * is on; repeating it as though it were counting down would be a lie. While paused the frozen
 * remainder is shown instead of whatever stop left on the panel.
 */
@Composable
private fun Countdown(
    running: Boolean,
    paused: Boolean,
    remainingSeconds: Int,
    countdownSeconds: Int,
    asking: Boolean,
) {
    val colours = MitoTheme.colours

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow(
            when {
                paused -> stringResource(R.string.control_eyebrow_paused)
                running -> stringResource(R.string.control_eyebrow_running)
                else -> stringResource(R.string.control_eyebrow_ready)
            },
        )

        Text(
            text = when {
                asking -> stringResource(R.string.control_asking_placeholder)
                running || paused -> clock(remainingSeconds)
                else -> clock(countdownSeconds)
            },
            style = MitoTheme.type.metricLg,
            color = when {
                asking -> colours.onSurfaceMuted
                paused -> colours.warning
                running -> colours.primaryBright
                else -> colours.onSurface
            },
            modifier = Modifier.padding(vertical = MitoTheme.space.sm),
        )

        StatusPill(
            label = when {
                asking -> stringResource(R.string.status_asking)
                paused -> stringResource(R.string.status_paused)
                running -> stringResource(R.string.status_lit)
                else -> stringResource(R.string.status_ready)
            },
            dot = when {
                asking -> colours.onSurfaceMuted
                paused -> colours.warning
                running -> colours.primary
                else -> colours.successBright
            },
        )
    }
}

/**
 * The switch, in whichever of its states applies. Every one of them is the same object in the same
 * place, so the screen never rearranges itself around what the Bluetooth link happens to be doing.
 * While lit it pauses; ending the run is the secondary control under it.
 */
@Composable
private fun Switch(
    panel: Panel?,
    state: PanelState,
    recovering: Boolean,
    running: Boolean,
    paused: Boolean,
    remainingSeconds: Int,
    countdownSeconds: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onPanels: () -> Unit,
) {
    when {
        panel == null -> PowerButton(
            icon = Icons.Outlined.Sensors,
            caption = stringResource(R.string.control_tap_pick_panel),
            onClick = onPanels,
        )

        state is PanelState.Connecting -> PowerButton(
            icon = Icons.Outlined.Sync,
            caption = stringResource(R.string.control_connecting_to, panel.name),
            onClick = null,
        )

        state is PanelState.Ready && paused -> PowerButton(
            icon = Icons.Filled.PlayArrow,
            caption = stringResource(R.string.action_resume),
            onClick = onResume,
            progress = if (countdownSeconds > 0) {
                remainingSeconds.toFloat() / countdownSeconds
            } else {
                null
            },
        )

        state is PanelState.Ready && running -> PowerButton(
            icon = Icons.Filled.Pause,
            caption = stringResource(R.string.action_pause),
            onClick = onPause,
            lit = true,
            progress = if (countdownSeconds > 0) {
                remainingSeconds.toFloat() / countdownSeconds
            } else {
                null
            },
        )

        state is PanelState.Ready -> PowerButton(
            icon = Icons.Filled.PowerSettingsNew,
            caption = stringResource(R.string.action_start),
            onClick = onStart,
        )

        // Disconnected or failed. A retry is already running, and the way out is another panel.
        else -> PowerButton(
            icon = Icons.Outlined.Sync,
            caption = if (recovering) {
                stringResource(R.string.status_reconnecting)
            } else {
                stringResource(R.string.control_disconnected_tap_pick)
            },
            onClick = onPanels,
        )
    }
}

/** What the panel is set to, as one line, and the way through to change it. */
@Composable
private fun Plan(
    panel: Panel?,
    state: PanelState.Ready?,
    fallback: Preset?,
    onSetup: () -> Unit,
) {
    val colours = MitoTheme.colours
    val channels = panel?.capability?.channelCount ?: 2
    val redWord = stringResource(R.string.word_red).lowercase()
    val nirWord = stringResource(R.string.word_nir)

    MitoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(
                    if (fallback == null) {
                        stringResource(R.string.control_plan_output)
                    } else {
                        stringResource(
                            R.string.control_plan_will_start_with,
                            presetName(fallback.id, fallback.name),
                        )
                    },
                )
                Text(
                    text = when {
                        fallback != null -> shortMix(
                            fallback.brightnessFor(channels),
                            channels,
                            redWord = redWord,
                            nirWord = nirWord,
                        )
                        state != null -> shortMix(
                            state.requestedBrightness,
                            channels,
                            redWord = redWord,
                            nirWord = nirWord,
                        )
                        else -> stringResource(R.string.control_plan_nothing_to_report)
                    },
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Text(
                text = minutesLabel(fallback?.countdownMinutes ?: ((state?.status?.countdownSeconds ?: 0) / 60)),
                style = MitoTheme.type.metricMd,
                color = colours.onSurface,
            )
        }

        CardAction(
            label = stringResource(R.string.action_channels_countdown),
            onClick = onSetup,
            icon = Icons.Outlined.Tune,
            modifier = Modifier.padding(top = MitoTheme.space.md),
        )
    }
}

/**
 * The run to fall back on when every channel is at nothing, where starting would light nothing and a
 * button that visibly does nothing is worse than one that picks a first run. Null as soon as the
 * panel has a setting of its own, which is the normal case: a real panel remembers what it was last
 * doing, and overriding that would be the app talking over the hardware. A countdown of zero is not
 * a reason to intervene — holding the light on until it is stopped is a real setting with its own
 * word for it.
 */
private fun fallbackFor(panel: Panel?, state: PanelState.Ready?): Preset? {
    panel ?: return null
    state ?: return null

    val dark = state.requestedBrightness.values.none { it > 0 }
    return if (dark) Preset.firstRun else null
}

@Composable
private fun minutesLabel(minutes: Int): String =
    if (minutes <= 0) stringResource(R.string.word_hold) else stringResource(R.string.format_min, minutes)
