package mitolight.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mitolight.app.AppLocale
import mitolight.app.LocalePreferences
import mitolight.app.PanelSource
import mitolight.app.PanelViewModel
import mitolight.app.R
import mitolight.app.SessionLog
import mitolight.app.ui.components.Chip
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.ScreenTitle
import mitolight.app.ui.panelSourceLabel
import mitolight.app.ui.theme.MitoTheme
import mitolight.domain.Area
import mitolight.domain.Limit
import mitolight.domain.Preset

/**
 * Where the borrowed numbers say who they were borrowed from. Every limit here is Mito Light's own,
 * which is what makes it hard to argue with — and the two toggles that look like plumbing are the two
 * the user actually meets: the notification that keeps a session alive, and the simulator that lets
 * the app be shown with no panel in the room.
 */
@Composable
fun SettingsScreen(
    model: PanelViewModel,
    log: SessionLog,
    locales: LocalePreferences,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    val source by model.source.collectAsStateWithLifecycle()
    val remembered by model.remembered.collectAsStateWithLifecycle()
    val sessions by log.sessions.collectAsStateWithLifecycle()
    val locale by locales.locale.collectAsStateWithLifecycle()

    var reconnect by remember { mutableStateOf(true) }
    var holdWhileOff by remember { mutableStateOf(true) }
    var askForArea by remember { mutableStateOf(true) }
    var warnNearLimit by remember { mutableStateOf(true) }
    var notifyOnEnd by remember { mutableStateOf(true) }
    var notifyOnDrop by remember { mutableStateOf(true) }
    var defaultMinutes by remember { mutableIntStateOf(10) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = space.screenMargin)
            .padding(top = space.sm, bottom = space.xl),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        ScreenTitle(
            title = stringResource(R.string.tab_settings),
            subtitle = stringResource(R.string.settings_subtitle),
        )

        Eyebrow(stringResource(R.string.settings_language))
        MitoCard {
            Text(
                text = stringResource(R.string.settings_language_title),
                style = MitoTheme.type.titleMd,
                color = colours.onSurface,
            )
            Column(
                modifier = Modifier.padding(top = space.sm),
                verticalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                Chip(
                    label = stringResource(R.string.language_system),
                    selected = locale == AppLocale.SYSTEM,
                    onClick = { locales.set(AppLocale.SYSTEM) },
                )
                Chip(
                    label = stringResource(R.string.language_english),
                    selected = locale == AppLocale.ENGLISH,
                    onClick = { locales.set(AppLocale.ENGLISH) },
                )
                Chip(
                    label = stringResource(R.string.language_czech),
                    selected = locale == AppLocale.CZECH,
                    onClick = { locales.set(AppLocale.CZECH) },
                )
            }
            Text(
                text = stringResource(R.string.settings_language_explanation),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        Eyebrow(stringResource(R.string.settings_panel))
        MitoCard {
            Fact(stringResource(R.string.fact_remembered_panels), "${remembered.size}")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.sm),
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                PanelSource.entries.forEach { option ->
                    Chip(
                        label = panelSourceLabel(option),
                        selected = source == option,
                        onClick = { model.chooseSource(option) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_simulator_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        Toggle(
            label = stringResource(R.string.settings_reconnect_label),
            explanation = stringResource(R.string.settings_reconnect_explanation),
            checked = reconnect,
            onChange = { reconnect = it },
        )

        Toggle(
            label = stringResource(R.string.settings_keep_control_label),
            explanation = stringResource(R.string.settings_keep_control_explanation),
            checked = holdWhileOff,
            onChange = { holdWhileOff = it },
        )

        Eyebrow(stringResource(R.string.settings_exposure))
        MitoCard {
            Text(
                text = stringResource(R.string.settings_manual_title),
                style = MitoTheme.type.titleMd,
                color = colours.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_manual_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(bottom = space.sm),
            )
            Fact(
                stringResource(R.string.fact_daily_limit),
                stringResource(R.string.format_min, Limit.FromTheManual.recommendedMinutes),
            )
            Fact(
                stringResource(R.string.fact_ceiling),
                stringResource(R.string.format_min, Limit.FromTheManual.ceilingMinutes),
            )
            Fact(
                stringResource(R.string.fact_warn_at),
                stringResource(R.string.settings_warn_at_format, Limit.FromTheManual.warnAtPercent),
            )
        }

        MitoCard {
            Text(
                text = stringResource(R.string.settings_default_countdown),
                style = MitoTheme.type.titleMd,
                color = colours.onSurface,
            )
            Row(
                modifier = Modifier.padding(top = space.sm),
                horizontalArrangement = Arrangement.spacedBy(space.sm),
            ) {
                listOf(5, 10, 15, 20).forEach { option ->
                    Chip(
                        label = stringResource(R.string.format_min, option),
                        selected = defaultMinutes == option,
                        onClick = { defaultMinutes = option },
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_panel_accepts_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        Toggle(
            label = stringResource(R.string.settings_ask_area_label),
            explanation = stringResource(R.string.settings_ask_area_explanation),
            checked = askForArea,
            onChange = { askForArea = it },
        )

        Toggle(
            label = stringResource(R.string.settings_warn_near_limit_label),
            explanation = stringResource(R.string.settings_warn_near_limit_explanation),
            checked = warnNearLimit,
            onChange = { warnNearLimit = it },
        )

        Eyebrow(stringResource(R.string.settings_areas_presets))
        MitoCard {
            Fact(
                stringResource(R.string.word_areas),
                stringResource(R.string.settings_areas_fixed_format, Area.entries.size),
            )
            Fact(stringResource(R.string.fact_default_presets), "${Preset.defaults.size}")
            Text(
                text = stringResource(R.string.settings_areas_no_overlap_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        Eyebrow(stringResource(R.string.tab_history))
        MitoCard {
            Fact(stringResource(R.string.fact_sessions), "${sessions.size}")
            Fact(stringResource(R.string.fact_unattributed), "${sessions.count { it.isUnattributed }}")
            Text(
                text = stringResource(R.string.settings_seeded_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }

        Eyebrow(stringResource(R.string.settings_notifications))
        Toggle(
            label = stringResource(R.string.settings_notify_end_label),
            explanation = null,
            checked = notifyOnEnd,
            onChange = { notifyOnEnd = it },
        )
        Toggle(
            label = stringResource(R.string.settings_notify_drop_label),
            explanation = stringResource(R.string.settings_notify_drop_explanation),
            checked = notifyOnDrop,
            onChange = { notifyOnDrop = it },
        )

        Eyebrow(stringResource(R.string.settings_about))
        MitoCard {
            Fact(stringResource(R.string.fact_version), stringResource(R.string.settings_version_value))
            Fact(stringResource(R.string.fact_protocol), stringResource(R.string.settings_protocol_value))
            Text(
                text = stringResource(R.string.settings_about_note),
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = space.sm),
            )
        }
    }
}

@Composable
private fun Toggle(
    label: String,
    explanation: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colours = MitoTheme.colours

    MitoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MitoTheme.type.titleMd,
                color = colours.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colours.onPrimary,
                    checkedTrackColor = colours.primary,
                    uncheckedThumbColor = colours.onSurfaceMuted,
                    uncheckedTrackColor = colours.surfaceHigh,
                ),
            )
        }
        explanation?.let {
            Text(
                text = it,
                style = MitoTheme.type.bodySm,
                color = colours.onSurfaceMuted,
                modifier = Modifier.padding(top = MitoTheme.space.xs),
            )
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MitoTheme.space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MitoTheme.type.bodyMd, color = MitoTheme.colours.onSurfaceMuted)
        Text(text = value, style = MitoTheme.type.metricSm, color = MitoTheme.colours.onSurface)
    }
}
