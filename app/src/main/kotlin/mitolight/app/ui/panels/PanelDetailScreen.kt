package mitolight.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import mitolight.app.PanelViewModel
import mitolight.app.R
import mitolight.app.ui.components.Eyebrow
import mitolight.app.ui.components.MitoCard
import mitolight.app.ui.components.SecondaryAction
import mitolight.app.ui.components.TextAction
import mitolight.app.ui.theme.MitoTheme

/** What the app knows about one panel, and the two things it can do to it. */
@Composable
fun PanelDetailScreen(
    id: String,
    model: PanelViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val space = MitoTheme.space

    val remembered by model.remembered.collectAsStateWithLifecycle()
    val held by model.panel.collectAsStateWithLifecycle()
    val panel = remembered.firstOrNull { it.id == id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = space.screenMargin, vertical = space.lg),
        verticalArrangement = Arrangement.spacedBy(space.lg),
    ) {
        TextAction(label = stringResource(R.string.action_back), onClick = onBack)

        if (panel == null) {
            Text(
                text = stringResource(R.string.detail_not_remembered),
                style = MitoTheme.type.bodyMd,
                color = colours.onSurfaceMuted,
            )
            return@Column
        }

        Text(text = panel.name, style = MitoTheme.type.headlineMd, color = colours.onSurface)

        MitoCard {
            Fact(stringResource(R.string.fact_address), panel.id, monospaced = true)
            Fact(stringResource(R.string.word_channels), "${panel.capability.channelCount}")
            Fact(
                stringResource(R.string.fact_dims_to),
                stringResource(R.string.format_percent, panel.capability.minimumBrightness),
            )
            Fact(stringResource(R.string.fact_last_connected), stamp(panel.lastConnectedAtMillis))
        }

        Eyebrow(stringResource(R.string.detail_accepts_title))
        Text(
            text = stringResource(
                if (panel.capability.hasLightModes) R.string.detail_ml5_note else R.string.detail_ml4_note,
            ),
            style = MitoTheme.type.bodySm,
            color = colours.onSurfaceMuted,
        )

        if (held?.id == panel.id) {
            SecondaryAction(label = stringResource(R.string.action_disconnect), onClick = model::release)
        }

        SecondaryAction(
            label = stringResource(R.string.action_forget_panel),
            onClick = {
                model.forget(panel.id)
                onBack()
            },
        )
    }
}

@Composable
private fun Fact(label: String, value: String, monospaced: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MitoTheme.space.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MitoTheme.type.bodyMd, color = MitoTheme.colours.onSurfaceMuted)
        Text(
            text = value,
            style = if (monospaced) MitoTheme.type.metricSm else MitoTheme.type.bodyMd,
            color = MitoTheme.colours.onSurface,
        )
    }
}

private fun stamp(millis: Long): String =
    DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
