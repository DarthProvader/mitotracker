package mitolight.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mitolight.app.PanelSource
import mitolight.app.R
import mitolight.domain.Area

/**
 * Where an enum's own value stops being enough to show on screen.
 *
 * [Area] and [mitolight.domain.Preset] live in the domain module and carry an English name because
 * a domain module cannot depend on Android resources; [Tab] and [PanelSource] carry one
 * for the same reason it would be awkward to make them depend on Compose. Every translated label the
 * app shows for one of these comes from here, so a screen never has to know which enum's word needs
 * looking up and which is already the right one to print.
 */

/** [Area.label] is the English name kept for JSON and history; this is what a screen prints. */
@Composable
fun Area.label(): String = stringResource(
    when (this) {
        Area.SCALP -> R.string.area_scalp
        Area.FACE_AND_NECK -> R.string.area_face_and_neck
        Area.SHOULDERS -> R.string.area_shoulders
        Area.CHEST -> R.string.area_chest
        Area.ABDOMEN -> R.string.area_abdomen
        Area.UPPER_BACK -> R.string.area_upper_back
        Area.LOWER_BACK -> R.string.area_lower_back
        Area.ARMS -> R.string.area_arms
        Area.HIPS -> R.string.area_hips
        Area.THIGHS -> R.string.area_thighs
        Area.KNEES -> R.string.area_knees
        Area.FEET -> R.string.area_feet
    },
)

/** Joins [Area.label] lookups; a plain [Iterable.joinToString] cannot call a composable. */
@Composable
fun List<Area>.joinedLabels(separator: String = " · "): String {
    val labels = ArrayList<String>(size)
    for (area in this) labels.add(area.label())
    return labels.joinToString(separator)
}

/**
 * Looks a default routine up by its stable id and returns the translated name. [fallback] is the
 * preset's own [mitolight.domain.Preset.name] and is what carries a user's own preset through
 * untouched — only the ten included routines have a resource to translate into.
 */
@Composable
fun presetName(id: String, fallback: String): String {
    val resource = when (id) {
        "sleep" -> R.string.preset_sleep
        "joint-support" -> R.string.preset_joint_support
        "brain-cognition" -> R.string.preset_brain_cognition
        "beauty-skin" -> R.string.preset_beauty_skin
        "recovery" -> R.string.preset_recovery
        "vision" -> R.string.preset_vision
        "musculoskeletal-support" -> R.string.preset_musculoskeletal_support
        "feeling-under-the-weather" -> R.string.preset_feeling_under_the_weather
        "inflammation-support" -> R.string.preset_inflammation_support
        "mood-wellbeing" -> R.string.preset_mood_wellbeing
        else -> null
    }
    return resource?.let { stringResource(it) } ?: fallback
}

/** The five destinations in [Shell], as shown on the bottom bar and read by a screen reader. */
@Composable
fun tabLabel(tab: Tab): String = stringResource(
    when (tab) {
        Tab.PANELS -> R.string.tab_panels
        Tab.CONTROL -> R.string.tab_control
        Tab.HISTORY -> R.string.tab_history
        Tab.STATS -> R.string.tab_stats
        Tab.SETTINGS -> R.string.tab_settings
    },
)

/** Whether the app is driving real hardware or the in-memory stand-in (ADR 0006). */
@Composable
fun panelSourceLabel(source: PanelSource): String = stringResource(
    when (source) {
        PanelSource.BLUETOOTH -> R.string.source_bluetooth
        PanelSource.SIMULATOR -> R.string.source_simulator
    },
)
