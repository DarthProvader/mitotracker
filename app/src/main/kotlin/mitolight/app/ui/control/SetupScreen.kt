package mitolight.app.ui.control

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import mitolight.app.PanelViewModel
import mitolight.app.PresetPreferences
import mitolight.app.R
import mitolight.app.SessionTracker
import mitolight.app.ui.channelIcon
import mitolight.app.ui.channelName
import mitolight.app.ui.components.AreaGrid
import mitolight.app.ui.components.CardAction
import mitolight.app.ui.components.ChannelFader
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.IconBubble
import mitolight.app.ui.components.MinuteFader
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.PrimaryAction
import mitolight.app.ui.components.RoundIconButton
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.components.SecondaryAction
import mitolight.app.ui.components.TabPair
import mitolight.app.ui.components.VerticalFader
import mitolight.app.ui.joinedLabels
import mitolight.app.ui.presetName
import mitolight.app.ui.shortMix
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Area
import mitolight.domain.Preset
import mitolight.panel.Panel
import mitolight.panel.PanelState

/**
 * What the panel will do, kept off the screen that does it.
 *
 * Everything here is written straight through to the hardware, because the panel is the source of
 * truth (ADR 0003): there is no "apply" and nothing to lose by leaving. A preset is the same thing in
 * one tap — it sets the channels, the countdown and what the run will be credited to, and hands back
 * to the start button rather than starting itself, so that the app has exactly one place where the
 * light comes on.
 */
@Composable
fun SetupScreen(
    model: PanelViewModel,
    presets: PresetPreferences,
    tracker: SessionTracker,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val space = MitoTheme.space
    val panel by model.panel.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val held = panel
    val ready = state as? PanelState.Ready
    var manual by remember { mutableStateOf(true) }
    var creating by remember { mutableStateOf(false) }

    BackHandler(enabled = creating) { creating = false }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.screenMargin)
            .padding(bottom = space.xl),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = { if (creating) creating = false else onDone() },
            contentDescription = stringResource(R.string.action_back),
        )

        ScreenTitle(
            title = stringResource(
                if (creating) R.string.setup_title_new_preset else R.string.setup_title_output,
            ),
            subtitle = stringResource(
                if (creating) R.string.setup_subtitle_new_preset else R.string.setup_subtitle_output,
            ),
        )

        if (!creating) {
            TabPair(
                first = stringResource(R.string.tab_manual),
                second = stringResource(R.string.tab_presets),
                firstSelected = manual,
                onSelect = { manual = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (held == null || ready == null) {
            MitoCard(glow = MitoTheme.colours.vignette) {
                Text(
                    text = stringResource(R.string.setup_nothing_connected),
                    style = MitoTheme.type.titleMd,
                    color = MitoTheme.colours.onSurface,
                )
            }
            return@Column
        }

        if (creating) {
            PresetEditor(
                channelCount = held.capability.channelCount,
                onCreate = { name, areas, brightness, minutes ->
                    presets.create(name, areas, brightness, minutes)
                    creating = false
                },
            )
        } else if (manual) {
            Channels(panel = held, state = ready, model = model)
        } else {
            Presets(
                presets = presets,
                channelCount = held.capability.channelCount,
                onNew = { creating = true },
                onLoad = { preset ->
                    val mix = preset.brightnessFor(held.capability.channelCount)
                    mix.forEach { (channel, percent) -> model.setBrightness(channel, percent) }
                    model.setCountdown(preset.countdownMinutes)
                    tracker.aimAt(preset.areas, mix)
                    onDone()
                },
            )
        }
    }
}

/**
 * The everyday manual control is Red and NIR on both panels. ML5's individual wavelengths belong to
 * the advanced section of a custom preset, not here; replacing the two readable columns
 * with six rails made the common case look like a mixing desk.
 */
@Composable
private fun Channels(panel: Panel, state: PanelState.Ready, model: PanelViewModel) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val channels = panel.capability.channelCount

    fun channelsIn(red: Boolean): List<Int> =
        (0 until channels).filter { Preset.isRedChannel(it, channels) == red }

    fun groupLevel(red: Boolean): Int {
        val values = channelsIn(red).map { state.requestedBrightness[it] ?: 0 }
        return values.average().roundToInt()
    }

    fun setGroup(red: Boolean, percent: Int) {
        channelsIn(red).forEach { channel -> model.setBrightness(channel, percent) }
    }

    // One column under each half of the Manual / Presets tab pair, centred on that label.
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            VerticalFader(
                label = stringResource(R.string.word_red),
                percent = groupLevel(red = true),
                tint = colours.channelRed,
                onSet = { percent -> setGroup(red = true, percent) },
                modifier = Modifier.fillMaxWidth(0.5f),
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            VerticalFader(
                label = stringResource(R.string.word_nir),
                percent = groupLevel(red = false),
                tint = colours.channelNir,
                onSet = { percent -> setGroup(red = false, percent) },
                modifier = Modifier.fillMaxWidth(0.5f),
            )
        }
    }

    MitoCard {
        MinuteFader(
            minutes = (state.status?.countdownSeconds ?: 0) / 60,
            onSet = model::setCountdown,
            icon = Icons.Outlined.Timer,
        )
        Text(
            text = stringResource(R.string.setup_zero_holds_light),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
            modifier = Modifier.padding(top = space.sm),
        )
    }
}

/** The named routines, each carrying what it is for as well as how bright and how long. */
@Composable
private fun Presets(
    presets: PresetPreferences,
    channelCount: Int,
    onNew: () -> Unit,
    onLoad: (Preset) -> Unit,
) {
    val space = MitoTheme.space
    val custom by presets.custom.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Preset?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(space.md)) {
        SecondaryAction(
            label = stringResource(R.string.action_new_preset),
            onClick = onNew,
            icon = Icons.Outlined.Add,
        )

        if (custom.isNotEmpty()) {
            Eyebrow(stringResource(R.string.setup_your_presets), modifier = Modifier.padding(top = space.sm))
            custom.forEach { preset ->
                PresetCard(
                    preset = preset,
                    channelCount = channelCount,
                    onLoad = onLoad,
                    onDeleteRequest = { pendingDelete = preset },
                )
            }
        }

        Eyebrow(stringResource(R.string.setup_included_routines), modifier = Modifier.padding(top = space.sm))
        Preset.defaults.forEach { preset ->
            PresetCard(preset, channelCount, onLoad = onLoad)
        }
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = {
                Text(stringResource(R.string.delete_preset_message, preset.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        presets.delete(preset.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.action_delete_preset))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    channelCount: Int,
    onLoad: (Preset) -> Unit,
    onDeleteRequest: (() -> Unit)? = null,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    val redWord = stringResource(R.string.word_red).lowercase()
    val nirWord = stringResource(R.string.word_nir)

    MitoCard(onClick = { onLoad(preset) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.md),
        ) {
            IconBubble(
                icon = Icons.Outlined.PlayCircleOutline,
                tint = colours.primaryBright,
                size = 44.dp,
                fill = colours.primary.copy(alpha = 0.10f),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = presetName(preset.id, preset.name),
                    style = MitoTheme.type.titleMd,
                    color = colours.onSurface,
                )
                Text(
                    text = if (preset.isUnattributed) {
                        stringResource(R.string.preset_no_area)
                    } else {
                        preset.areas.joinedLabels()
                    },
                    style = MitoTheme.type.bodySm,
                    color = colours.onSurfaceMuted,
                )
                Text(
                    text = shortMix(
                        preset.brightnessFor(channelCount),
                        channelCount,
                        redWord = redWord,
                        nirWord = nirWord,
                    ),
                    style = MitoTheme.type.metricSm,
                    color = colours.onSurfaceMuted,
                    modifier = Modifier.padding(top = space.sm),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (preset.countdownMinutes == 0) {
                        stringResource(R.string.word_hold)
                    } else {
                        stringResource(R.string.format_min, preset.countdownMinutes)
                    },
                    style = MitoTheme.type.metricMd,
                    color = colours.onSurface,
                )
                if (onDeleteRequest != null) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete_preset),
                        tint = colours.onSurfaceMuted,
                        modifier = Modifier
                            .padding(top = space.xs)
                            // Nudge under the "n" of "min" rather than the middle of the duration.
                            .offset(x = 6.dp)
                            .size(28.dp)
                            .clip(MitoTheme.shape.full)
                            .clickable(onClick = onDeleteRequest)
                            .padding(2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetEditor(
    channelCount: Int,
    onCreate: (name: String, areas: List<Area>, brightness: Map<Int, Int>, minutes: Int) -> Unit,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(10) }
    var individualWavelengths by remember(channelCount) { mutableStateOf(false) }
    val brightness = remember(channelCount) {
        mutableStateMapOf<Int, Int>().apply {
            repeat(channelCount) { channel -> put(channel, 100) }
        }
    }
    val chosen = remember { emptyList<Area>().toMutableStateList() }

    fun channelsIn(red: Boolean): List<Int> =
        (0 until channelCount).filter { Preset.isRedChannel(it, channelCount) == red }

    fun groupLevel(red: Boolean): Int =
        channelsIn(red).map { brightness[it] ?: 0 }.average().roundToInt()

    fun setGroup(red: Boolean, percent: Int) {
        channelsIn(red).forEach { channel -> brightness[channel] = percent }
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(40) },
        label = { Text(stringResource(R.string.label_preset_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Eyebrow(stringResource(R.string.word_areas))
    AreaGrid(
        chosen = chosen,
        onToggle = { area -> if (area in chosen) chosen.remove(area) else chosen.add(area) },
    )
    Text(
        text = if (chosen.isEmpty()) {
            stringResource(R.string.preset_no_area_selected_note)
        } else {
            chosen.toList().joinedLabels()
        },
        style = MitoTheme.type.bodySm,
        color = colours.onSurfaceMuted,
    )

    Eyebrow(stringResource(R.string.word_channels))
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            VerticalFader(
                label = stringResource(R.string.word_red),
                percent = groupLevel(red = true),
                tint = colours.channelRed,
                onSet = { percent -> setGroup(red = true, percent) },
                modifier = Modifier.fillMaxWidth(0.5f),
                height = 253.dp,
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            VerticalFader(
                label = stringResource(R.string.word_nir),
                percent = groupLevel(red = false),
                tint = colours.channelNir,
                onSet = { percent -> setGroup(red = false, percent) },
                modifier = Modifier.fillMaxWidth(0.5f),
                height = 253.dp,
            )
        }
    }

    if (channelCount > 2) {
        CardAction(
            label = stringResource(
                if (individualWavelengths) {
                    R.string.action_hide_individual_wavelengths
                } else {
                    R.string.action_individual_wavelengths
                },
            ),
            onClick = { individualWavelengths = !individualWavelengths },
            icon = if (individualWavelengths) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        )

        if (individualWavelengths) {
            MitoCard {
                Eyebrow(stringResource(R.string.setup_wavelength_mix))
                Column(
                    modifier = Modifier.padding(top = space.lg),
                    verticalArrangement = Arrangement.spacedBy(space.lg),
                ) {
                    // Wavelength labels are nanometre strings (unchanged by locale); Red/NIR only
                    // apply on a two-channel panel, which never opens this section.
                    repeat(channelCount) { channel ->
                        ChannelFader(
                            label = channelName(channelCount, channel),
                            tint = colours.channel(channel, channelCount),
                            icon = channelIcon(channelCount, channel),
                            percent = brightness[channel] ?: 0,
                            onSet = { percent -> brightness[channel] = percent },
                        )
                    }
                }
            }
        }
    }

    MitoCard {
        MinuteFader(minutes = minutes, onSet = { minutes = it }, icon = Icons.Outlined.Timer)
        Text(
            text = stringResource(R.string.setup_zero_holds_light),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
            modifier = Modifier.padding(top = space.sm),
        )
    }

    PrimaryAction(
        label = stringResource(R.string.action_save_preset),
        enabled = name.isNotBlank() && brightness.values.any { it > 0 },
        onClick = { onCreate(name, chosen.toList(), brightness.toMap(), minutes) },
    )
    if (name.isBlank() || brightness.values.none { it > 0 }) {
        Text(
            text = stringResource(R.string.setup_preset_validation_note),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
        )
    }
}
